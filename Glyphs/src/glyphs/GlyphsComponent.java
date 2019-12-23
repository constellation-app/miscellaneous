package glyphs;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.image.BufferedImage;
import java.text.AttributedCharacterIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Convert text into images that can be passed to OpenGL.
 * <p>
 * TODO Reset everything when the font changes.
 *
 * @author algol
 */
public final class GlyphsComponent {

    // Where do we draw the text?
    //
    public static final int BASEX = 60;
    public static final int BASEY = 200;

    private final BufferedImage drawing;

    /**
     * This logical font is always present.
     */
    public static final String DEFAULT_FONT = Font.SANS_SERIF;
    public static final int DEFAULT_FONT_SIZE = 64;

    private Font[] fonts;
    private String line;

    // Which boundaries do we draw?
    //
    private boolean drawRuns, drawIndividual, drawCombined;

    private final GlyphsTextureBuffer textureBuffer;

    public GlyphsComponent(final String[] fontNames, final int style, final int fontSize, final int textureBufferSize) {

        // TODO Ensure that the BufferedImage is wide enough to draw into.
        //
        drawing = new BufferedImage(2048, 256, BufferedImage.TYPE_INT_ARGB);

        if(fontNames.length>0) {
            setFonts(fontNames, style, fontSize);
        } else {
            setFonts(new String[]{DEFAULT_FONT}, style, fontSize);
        }

        drawRuns = false;
        drawIndividual = false;
        drawCombined = false;

        textureBuffer = new GlyphsTextureBuffer(textureBufferSize, textureBufferSize);
    }

    public BufferedImage getImage() {
        return drawing;
    }

    /**
     * Remove codepoints that can ruin layouts.
     *
     * @param s
     * @return The string with the forbidden characters removed.
     */
    static String cleanString(final String s) {
        return s
            .trim()
            .codePoints()
            .filter(cp -> !((cp>=0x202a && cp<=0x202e) || (cp>=0x206a && cp<=0x206f)))
            .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
            .toString()
        ;
    }

    public final void setLine(final String line) {
        this.line = cleanString(line);
        drawMultiString(BASEX, BASEY);
    }

    public void setBoundaries(final boolean drawRuns, final boolean drawIndividual, final boolean drawCombined) {
        this.drawRuns = drawRuns;
        this.drawIndividual = drawIndividual;
        this.drawCombined = drawCombined;
        drawMultiString(BASEX, BASEY);
    }

    /**
     * Set the most specific font.
     *
     * @param fontNames
     * @param style
     * @param fontSize
     */
    public void setFonts(final String[] fontNames, final int style, final int fontSize) {
        fonts = Arrays.stream(fontNames).map(fn -> new Font(fn, style, fontSize)).toArray(Font[]::new);
        drawMultiString(BASEX, BASEY);
    }

    public String[] getFonts() {
        return Arrays.stream(fonts).map(f -> f.getFontName()).toArray(String[]::new);
    }

    /**
     * Merge bounding boxes that overlap on the x axis.
     * <p>
     * This code feels a bit ugly. Have another look later.
     *
     * @param boxes A List<Rectangle> representing possibly overlapping glyph bounding boxes.
     *
     * @return A List<Rectangle> of non-overlapping bounding boxes.
     */
    private static List<Rectangle> mergeBoxes(final List<Rectangle> boxes) {
        final List<Rectangle> merged = new ArrayList<>();
        for(int i=boxes.size()-1;i>=0;) {
            Rectangle curr = boxes.get(i--);
            if(i==-1) {
                merged.add(curr);
                break;
            }
            while(i>=0) {
                final Rectangle prev = boxes.get(i);
                if((prev.x + prev.width) < curr.x) {
                    merged.add(curr);
                    break;
                }
                final int y = Math.min(prev.y, curr.y);
                curr = new Rectangle(
                    prev.x,
                    y,
                    Math.max(prev.x+prev.width, curr.x+curr.width)-prev.x,
                    Math.max(prev.y+prev.height, curr.y+curr.height)-y
                );
                i--;
                if(i==-1) {
                    merged.add(curr);
                    break;
                }
            }
        }

        return merged;
    }

    /**
     * Draw a String that may contain multiple directions and scripts.
     * <p>
     * This is not a general purpose text drawer. Instead, it caters to the
     * kind of string that are likely to be found in a CONSTELLLATION label;
     * short, lacking punctuation,but possibly containing multi-language characters.
     * <p>
     * A String is first broken up into sub-strings that consist of codepoints
     * of the same direction. These sub-strings are further broken into
     * sub-sub-strings that contain the same font. Each sub-sub-string can then
     * be drawn using TextLayout.draw(), and the associated glyphs can be
     * determined using Font.layoutGlyphVector().
     * <p>
     * The glyph images can then be drawn into an image buffer for use by OpenGL
     * to draw node and connection labels. Some glyphs (such as those used in
     * cursive Arabic script) will overlap: any overlapping glyphs will be
     * treated as a unit.
     * <p>
     * Hashes of the glyph images are used to determine if the image has already
     * been
     * @param g2d
     * @param line
     * @param fonts
     * @param x0
     * @param y0
     */
    void drawMultiString(final int x0, final int y0) {
        final Graphics2D g2d = drawing.createGraphics();
        g2d.setBackground(new Color(0, 0, 0, 0));
        g2d.clearRect(0, 0, drawing.getWidth(), drawing.getHeight());
        g2d.setColor(Color.WHITE);

//        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        if(line==null){
            g2d.dispose();
            return;
        }

//        g2d.setColor(Color.ORANGE);
//        g2d.drawLine(BASEX, BASEY, BASEX+1000, BASEY);

        int x = x0;

        final FontRenderContext frc = g2d.getFontRenderContext();

        for(final DirectionRun drun : DirectionRun.getDirectionRuns(line)) {
            for(final FontRun frun : FontRun.getFontRuns(drun.run, fonts)) {
//                // Draw an indicator line to show where the font run starts.
//                //
//                g2d.setColor(Color.LIGHT_GRAY);
//                g2d.drawLine(x, y0-128, x, y0+64);

                final String spart = frun.string;
                final int flags = drun.getFontLayoutDirection() | Font.LAYOUT_NO_START_CONTEXT | Font.LAYOUT_NO_LIMIT_CONTEXT;
                final GlyphVector gv = frun.font.layoutGlyphVector(frc, spart.toCharArray(), 0, spart.length(), flags);
    //                final int ng = gv.getNumGlyphs();
    //                System.out.printf("* %s %s\n", gv.getClass(), gv);
    //                System.out.printf("* numGlyphs %d\n", gv.getNumGlyphs());

                // Some fonts are shaped such that the left edge of the pixel bounds is
                // to the left of the starting point, and the right edge of the pixel
                // bounds is to to the right of the pixel bounds (for example,
                // the word "Test" in font "Montez" from fonts.google.com).
                // Figure that out here.
                //
                final Rectangle pixelBounds = gv.getPixelBounds(null, x, y0);
                if(pixelBounds.x<x) {
                    System.out.printf("adjust %s %s %s\n", x, pixelBounds.x, x-pixelBounds.x);
                    x += x-pixelBounds.x;
                }

                System.out.printf("* font run %s %d->%s\n", frun, x, pixelBounds);
                g2d.setColor(Color.WHITE);
                g2d.setFont(frun.font);

                final Map<AttributedCharacterIterator.Attribute,Object> attrs = new HashMap<>();
                attrs.put(TextAttribute.RUN_DIRECTION, drun.direction);
                attrs.put(TextAttribute.FONT, frun.font);
                final TextLayout layout = new TextLayout(spart, attrs, frc);

//                System.out.printf("* isLTR %s\n", layout.isLeftToRight());
                layout.draw(g2d, x, y0);

//                // The font glyphs are drawn such that the reference point is at (x,y).
//                // (See the javadoc for FontMetrics.) However, the pixelBounds
//                // are where the glyphs are actually drawn. To place the glyphs
//                // accurately within the bounding box, we need to know the difference.
//                //
//                final int frontDiff = pixelBounds.x - x;
//                final int topDiff = pixelBounds.y - y0;
//                textureBuffer.drawGlyph(layout, pixelBounds, frun.font, frontDiff, topDiff);

                // Iterate through the glyphs to get the bounding boxes.
                //
                final List<Rectangle> boxes = new ArrayList<>();
                for(int glyphIx=0; glyphIx<gv.getNumGlyphs(); glyphIx++) {
                    final int gc = gv.getGlyphCode(glyphIx);
                    if(gc!=0) {
                        final Rectangle gr = gv.getGlyphPixelBounds(glyphIx, frc, x, y0);
                        if(gr.width>0) {
//                            System.out.printf("rec %s\n", gr);
                            boxes.add(gr);
                        }
                    }
                    else {
                        System.out.printf("glyphcode %d\n", gc);
                    }
                }

                // Sort them by x position.
                //
                Collections.sort(boxes, (Rectangle r0, Rectangle r1) -> r0.x - r1.x);

                final List<Rectangle> merged = mergeBoxes(boxes);
                System.out.printf("%s\n", merged);

                merged.forEach(r -> {textureBuffer.addSubImage(drawing.getSubimage(r.x, r.y, r.width, r.height));});

                if(drawRuns) {
                    g2d.setColor(Color.RED);
                    g2d.drawRect(pixelBounds.x, pixelBounds.y, pixelBounds.width, pixelBounds.height);
                }

                if(drawIndividual) {
                    for(int glyphIx=0; glyphIx<gv.getNumGlyphs(); glyphIx++) {
                        final int gc = gv.getGlyphCode(glyphIx);
                        if(gc!=0) {
                            final Rectangle gr = gv.getGlyphPixelBounds(glyphIx, frc, x, y0);
    //                        final Point2D pos = gv.getGlyphPosition(glyphIx);
    //                        System.out.printf("* GV  %d %s %s %s\n", glyphIx, gv.getGlyphCode(glyphIx), gr, spart);
                            g2d.setColor(Color.BLUE);
                            g2d.drawRect(gr.x, gr.y, gr.width, gr.height);

//                                final Shape shape = gv.getGlyphOutline(glyphIx, x, y0);
//                                g2d.setColor(Color.MAGENTA);
//                                g2d.fill(shape);
                        }
                        else {
                            System.out.printf("glyphcode %d\n", gc);
                        }
                    }
                }

                if(drawCombined) {
                    g2d.setColor(Color.MAGENTA);
                    merged.forEach(r -> {g2d.drawRect(r.x, r.y, r.width, r.height);});
                }

//                g2d.setColor(Color.LIGHT_GRAY);
//                g2d.drawLine(x, y0-2, (int)(x + layout.getAdvance()), y0+2);

                // Just like some fonts draw to the left of their start points (see above),
                // some fonts draw after their advance.
                // Figure that out here.
                //
                final int width = (int)Math.max(layout.getAdvance(), pixelBounds.width);
//                x += layout.getAdvance();
                x += width;
            }
        }

//        setPreferredSize(new Dimension(x, PREFERRED_HEIGHT));
//        revalidate();
        g2d.dispose();
    }

    BufferedImage getTextureBuffer() {
        return textureBuffer.get(textureBuffer.size()-1);
    }
}

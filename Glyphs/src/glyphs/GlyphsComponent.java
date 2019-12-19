package glyphs;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.text.AttributedCharacterIterator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JComponent;

/**
 *
 * @author algol
 */
public class GlyphsComponent extends JComponent {

    private static final int PREFERRED_HEIGHT = 400;

    public static final int BASEX = 60;
    public static final int BASEY = 300;

    public static final int FONT_SIZE = 100;

    private final String[] fontNames;
    private final Font[] fonts;
    private String line;

    // Which boundaries do we draw?
    //
    private boolean drawRuns, drawIndividual, drawCombined;

    private int fontSize;

    public GlyphsComponent(final String[] fontNames, final String initialLine) {

        fontSize = FONT_SIZE;

        this.fontNames = new String[fontNames.length];
        fonts = new Font[fontNames.length];
        for(int i=0; i<fontNames.length; i++) {
            this.fontNames[i] = fontNames[i];
            fonts[i] = new Font(fontNames[i], Font.PLAIN, fontSize);
        }

        line = initialLine;
        drawRuns = false;
        drawIndividual = false;
        drawCombined = false;

        setLine(line);
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
//            .replace(" ", "") // TODO fix up spaces
            .codePoints()
//            .filter(cp -> (cp<0x202a) || ((cp>0x202e) && (cp<0x206a)) || (cp>0x206f))
            .filter(cp -> !((cp>=0x202a && cp<=0x202e) || (cp>=0x206a && cp<=0x206f)))
            .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
            .toString()
        ;
    }

    public final void setLine(final String line) {
//        this.line = line;
        this.line = cleanString(line);
        repaint();
    }

    public void setBoundaries(final boolean drawRuns, final boolean drawIndividual, final boolean drawCombined) {
        this.drawRuns = drawRuns;
        this.drawIndividual = drawIndividual;
        this.drawCombined = drawCombined;
        repaint();
    }

    public void setFontSize(final int fontSize) {
        this.fontSize = fontSize;
        for(int i=0; i<fontNames.length; i++) {
            fonts[i] = new Font(fontNames[i], Font.PLAIN, fontSize);
        }
        repaint();
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
    void drawMultiString(final Graphics2D g2d, final String line, final Font[] fonts, final int x0, final int y0) {
        int x = x0;

        for(final DirectionRun drun : DirectionRun.getDirectionRuns(line)) {
            for(final FontRun frun : FontRun.getFontRuns(drun.run, fonts)) {
                final String spart = frun.string;
                System.out.printf("* font run %s\n", frun);
                g2d.setColor(Color.GRAY);
                g2d.setFont(frun.font);
                final FontRenderContext frc = g2d.getFontRenderContext();

                final Map<AttributedCharacterIterator.Attribute,Object> attrs = new HashMap<>();
                attrs.put(TextAttribute.RUN_DIRECTION, drun.direction);
                attrs.put(TextAttribute.FONT, frun.font);
                final TextLayout layout = new TextLayout(spart, attrs, frc);

//                System.out.printf("* isLTR %s\n", layout.isLeftToRight());
                layout.draw(g2d, x, y0);

                final int flags = drun.getFontLayoutDirection() | Font.LAYOUT_NO_START_CONTEXT | Font.LAYOUT_NO_LIMIT_CONTEXT;
                final GlyphVector gv = frun.font.layoutGlyphVector(frc, spart.toCharArray(), 0, spart.length(), flags);
    //                final int ng = gv.getNumGlyphs();
    //                System.out.printf("* %s %s\n", gv.getClass(), gv);
    //                System.out.printf("* numGlyphs %d\n", gv.getNumGlyphs());

                // Iterate through the glyphs to get the bounding boxes.
                //
                final List<Rectangle> boxes = new ArrayList<>();
                for(int glyphIx=0; glyphIx<gv.getNumGlyphs(); glyphIx++) {
                    final int gc = gv.getGlyphCode(glyphIx);
                    if(gc!=0) {
                        final Rectangle gr = gv.getGlyphPixelBounds(glyphIx, frc, x, y0);
                        if(gr.width>0) {
                        System.out.printf("rec %s\n", gr);
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

                if(drawRuns) {
                    final Rectangle r = gv.getPixelBounds(null, x, y0);
                    g2d.setColor(Color.RED);
                    g2d.drawRect(r.x, r.y, r.width, r.height);
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
                    for(final Rectangle r : merged) {
                            g2d.drawRect(r.x, r.y, r.width, r.height);
                    }
                }

                x += layout.getAdvance();
            }
        }

        setPreferredSize(new Dimension(x, PREFERRED_HEIGHT));
        revalidate();
    }

    @Override
    protected void paintComponent(final Graphics g) {
        final Graphics2D g2d = (Graphics2D) g;
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, getWidth(), getHeight());

//        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        drawMultiString(g2d, line, fonts, BASEX, BASEY);
    }
}

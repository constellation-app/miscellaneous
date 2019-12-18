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

    private static char codepointDirection(final int codepoint) {
//        if(codepoint>32 && codepoint<128) {
//            return 'L';
//        }
        final byte d = Character.getDirectionality(codepoint);

        switch (d) {
            case Character.DIRECTIONALITY_RIGHT_TO_LEFT:
            case Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC:
            case Character.DIRECTIONALITY_RIGHT_TO_LEFT_EMBEDDING:
            case Character.DIRECTIONALITY_RIGHT_TO_LEFT_OVERRIDE:
                return 'R';

            case Character.DIRECTIONALITY_LEFT_TO_RIGHT:
            case Character.DIRECTIONALITY_LEFT_TO_RIGHT_EMBEDDING:
            case Character.DIRECTIONALITY_LEFT_TO_RIGHT_OVERRIDE:
                return 'L';

            default:
                return 'U';
        }
    }

    private static final class DirectionRuns {
        final List<String> runs;
        final boolean firstIsLTR;

        DirectionRuns(final List<String> runs, final boolean firstIsLTR) {
            this.runs = runs;
            this.firstIsLTR = firstIsLTR;
        }
    }

    /**
     * Find the beginnings and ends of runs of codepoints that have the same direction.
     *
     * @param text
     * @return An int[] containing codepoint indexes of the beginning of each run.
     */
    private DirectionRuns getDirectionRuns(final String text) {
        final int length = text.length();

        char firstDir = ' ';
        char currDir = ' ';
        final ArrayList<String> runs = new ArrayList<>();

        int start = 0;
        for(int offset = 0; offset < length;) {
            final int codepoint = text.codePointAt(offset);
            final int cc = Character.charCount(codepoint);

            final char dir;
            final int cptype = Character.getType(codepoint);
            /*codepoint==32*/
            if((cptype==Character.SPACE_SEPARATOR || cptype==Character.NON_SPACING_MARK)) {
                dir = currDir;
            } else {
                final char d = codepointDirection(codepoint);
                dir = d!='R' ? 'L' : 'R';
            }
            System.out.printf("-- %d %d %c %d %s\n", offset, codepoint, dir, Character.getType(codepoint), Character.UnicodeBlock.of(codepoint));
            if(dir!=currDir) {
                if(firstDir==' ') {
                    firstDir = dir;
                } else {
                    System.out.printf("CHDIR %s %s\n", start, offset);
                    runs.add(text.substring(start, offset));
                }
                currDir = dir;
                start = offset;
            }

            offset += cc;
        }

        // Add the end of the final run.
        //
        runs.add(text.substring(start, length));

        if(firstDir=='R' || runs.size()>1) {
            Collections.reverse(runs);
            firstDir = currDir;
        }

        System.out.printf("* dir runs %d %c\n", runs.size(), firstDir);
        runs.stream().forEach(s -> System.out.printf("* dir  run  %d %d [%s]\n", s.length(), s.codePointAt(0), s));
//        System.out.printf("--\n");

        return new DirectionRuns(runs, firstDir=='L');
    }

    private static int whichFont(final Font[] fonts, final int codepoint) {
        for(int i=0; i<fonts.length; i++) {
            if(fonts[i].canDisplay(codepoint)) {
//                System.out.printf("-- %s %d\n", fonts[i].getName(), codepoint);
                return i;
            }
        }

        System.out.printf("**** Font not found for codepoint %d\n", codepoint);
        return -1;
    }

    private static final class FontRun {
        final String string;
        final Font font;

        FontRun(final String s, final Font f) {
            this.string = s;
            this.font = f;
        }

        @Override
        public String toString() {
            return String.format("[[%s],%s]", string, font.getName());
        }
    }

    /**
     * Find the beginnings and ends of runs of codepoints that have the same font.
     * <p>
     * We don't use Font.canDisplayUpTo().
     * Consider the string containing Chinese and English text "CCCEEECCC".
     * A font such as Noto Sans CJK SC Regular contains both Chinese and Latin
     * characters, so Font.CanDisplayUpTo() would consume the entire string.
     * This is no good if we want to use a different font style for Latin characters.
     * Therefore, we look at each individual character.
     * Obviously this requires that specific fonts appear first in the font list.
     *
     * @param text
     *
     * @return A List<FontRun> font runs.
     */
    private List<FontRun> getFontRuns(final String s, final Font[] fonts) {
        final int length = s.length();

        int currFontIx = -1;
        int start = 0;
        final ArrayList<FontRun> frs = new ArrayList<>();

        for(int offset = 0; offset < length;) {
            final int codepoint = s.codePointAt(offset);
            final int cc = Character.charCount(codepoint);

//            final int fontIx = whichFont(fonts, codepoint);
            // If this is a space, make it the same font as the previous codepoint.
            // This keeps words of the same font together.
            //
            final int fontIx = codepoint==32 && currFontIx!=-1 ? currFontIx : whichFont(fonts, codepoint);
            if(fontIx==-1) {
                final String t = new String(new int[]{fonts[0].getMissingGlyphCode()}, 0, 1);
                frs.add(new FontRun(t, fonts[0]));
//                currFontIx = -1;
            } else {
                if(fontIx!=currFontIx) {
                    if(currFontIx!=-1) {
                        final String t = s.substring(start, offset);
                        frs.add(new FontRun(t, fonts[currFontIx]));
                    }
                    start = offset;
                    currFontIx = fontIx;
                }
            }

            offset += cc;
        }

        // Add the end of the final run.
        //
        final String t = s.substring(start, length);
        frs.add(new FontRun(t, fonts[currFontIx]));
//        System.out.printf("%d %d - [%s]\n", runs.get(runs.size()-2), length, text.subSequence(runs.get(runs.size()-2), length));

        return frs;
    }

    void drawStringListParts(final Graphics2D g2d, final String line, final Font[] fonts, final int x0, final int y0) {
        final DirectionRuns parts = getDirectionRuns(line);
        int x = x0;
        int layoutDirection = parts.firstIsLTR ? Font.LAYOUT_LEFT_TO_RIGHT : Font.LAYOUT_RIGHT_TO_LEFT;
        boolean runDirection = parts.firstIsLTR ? TextAttribute.RUN_DIRECTION_LTR : TextAttribute.RUN_DIRECTION_RTL;

        for(final String part : parts.runs) {
            final List<FontRun> frs = getFontRuns(part, fonts);
            for(final FontRun fr : frs) {
//                final String spart = rtl && direction==Bidi.DIRECTION_LEFT_TO_RIGHT ? swapEndSpaces(fr.string) : fr.string;
                final String spart = fr.string;
                System.out.printf("* font run %s\n", fr);
                g2d.setColor(Color.GRAY);
                g2d.setFont(fr.font);
                final FontRenderContext frc = g2d.getFontRenderContext();

//                final TextLayout layout = new TextLayout(spart, fr.font, frc);

                final Map<AttributedCharacterIterator.Attribute,Object> attrs = new HashMap<>();
                attrs.put(TextAttribute.RUN_DIRECTION, runDirection);
                attrs.put(TextAttribute.FONT, fr.font);
                final TextLayout layout = new TextLayout(spart, attrs, frc);

//                System.out.printf("* isLTR %s\n", layout.isLeftToRight());
                layout.draw(g2d, x, y0);

                if(drawRuns || drawIndividual) {
                    final int flags = layoutDirection | Font.LAYOUT_NO_START_CONTEXT | Font.LAYOUT_NO_LIMIT_CONTEXT;
                    final GlyphVector gv = fr.font.layoutGlyphVector(frc, spart.toCharArray(), 0, spart.length(), flags);
    //                final int ng = gv.getNumGlyphs();
    //                System.out.printf("* %s %s\n", gv.getClass(), gv);
    //                System.out.printf("* numGlyphs %d\n", gv.getNumGlyphs());

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
                }

                x += layout.getAdvance();
            }

            layoutDirection = layoutDirection==Font.LAYOUT_LEFT_TO_RIGHT ? Font.LAYOUT_RIGHT_TO_LEFT : Font.LAYOUT_LEFT_TO_RIGHT;
            runDirection = !runDirection;
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

        drawStringListParts(g2d, line, fonts, BASEX, BASEY);
    }
}

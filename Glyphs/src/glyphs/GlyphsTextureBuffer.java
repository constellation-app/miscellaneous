package glyphs;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.font.TextLayout;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * This class encapsulates a BufferedImage that holds the glyph images.
 * <p>
 * Each new glyph is drawn at the current x,y position if there's enough room,
 * otherwise a new glyph line is started.
 *
 * @author algol
 */
final class GlyphsTextureBuffer {
    // The buffers that glyphs will be drawn to.
    //
    private final List<BufferedImage> glyphBuffers;
    private final int width;
    private final int height;

    // The current glyph buffer and its graphics.
    //
    private BufferedImage glyphBuffer;
    private Graphics2D g2d;

    // The next position to draw a glyph at.
    //
    private int x;
    private int y;

    // The maximum bounding box height in the current glyph line.
    // We need this so we know how much to add to y to go to the next line of glyphs.
    //
    private int maxHeight;

    GlyphsTextureBuffer(final int width, final int height) {
        this.width = width;
        this.height = height;
        glyphBuffers = new ArrayList<>();
        reset();
    }

    /**
     * Return the i'th buffer.
     *
     * @return
     */
    BufferedImage get() {
        return glyphBuffers.get(glyphBuffers.size()-1);
    }

    void reset() {
        glyphBuffers.clear();
        if(g2d!=null) {
            g2d.dispose();
        }
        g2d = null;
        newGlyphBuffer();
    }

    void drawGlyph(final TextLayout layout, final Rectangle boundingBox, final Font font, final int diff, final int topDiff) {
        if((x+boundingBox.width) > width) {
            newGlyphLine();
        }

        final FontMetrics fm = g2d.getFontMetrics(font);
        final int y_ = y+fm.getHeight()-fm.getMaxDescent();

        if((y_+boundingBox.height) > height) {
            newGlyphBuffer();
        }

        g2d.setColor(Color.GREEN);
        g2d.drawRect(x, y, boundingBox.width, boundingBox.height);
//        g2d.drawLine(x+boundingBox.x, y+boundingBox.y, x+boundingBox.width, y+boundingBox.height);
        g2d.setColor(Color.WHITE);

        System.out.printf("Draw image %s at %d,%d\n", boundingBox, x, y);
        layout.draw(g2d, x-diff, y-topDiff);


        x += boundingBox.width;

        maxHeight = Math.max(boundingBox.height, maxHeight);
    }

    private void newGlyphBuffer() {
        glyphBuffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        if(g2d!=null) {
            g2d.dispose();
        }
        g2d = glyphBuffer.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setBackground(Color.BLACK);
        g2d.setColor(Color.RED);
        g2d.drawRect(0, 0, width-1, height-1);
        g2d.setColor(Color.WHITE);

        glyphBuffers.add(glyphBuffer);

        x = 0;
        y = 0;
        maxHeight = 0;
    }

    private void newGlyphLine() {
        x = 0;
        y += maxHeight;
        maxHeight = 0;
    }
}

package glyphs;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    /**
     * Remember where we wrote each BufferedImage.
     * <p>
     * This stops us writing the same image twice.
     * <p>
     * The key is the hashcode of the BufferedImage.
     */
    private final Map<Integer, GlyphData> memory;

    GlyphsTextureBuffer(final int width, final int height) {
        this.width = width;
        this.height = height;
        glyphBuffers = new ArrayList<>();
        memory = new HashMap<>();
        reset();
    }

    int size() {
        return glyphBuffers.size();
    }

    /**
     * Return the i'th buffer.
     *
     * @return
     */
    BufferedImage get(final int i) {
        return glyphBuffers.get(i);
    }

    public void reset() {
        glyphBuffers.clear();
        memory.clear();
        if(g2d!=null) {
            g2d.dispose();
            g2d = null;
        }

        newGlyphBuffer();
    }

    void addSubImage(final BufferedImage img) {
        final int w = img.getWidth();
        final int h = img.getHeight();

        // Get the hashcode of the image.
        // BufferedImage doesn't have a hashCode() method, so we use the underlying pixels.
        //
        final int hashcode = Arrays.hashCode(img.getRGB(0, 0, w, h, null, 0, w));

        // Is there enough room for this image?
        //
        if(!memory.containsKey(hashcode)) {
            if((x+w) > width) {
                newGlyphLine();
            }
            if((y+h)>height) {
                newGlyphBuffer();
            }

            g2d.drawImage(img, x, y, null);

            memory.put(hashcode, new GlyphData(glyphBuffers.size()-1, new Rectangle(x, y, w, h)));

            x += w;
            maxHeight = Math.max(h, maxHeight);
        }
        else {
            System.out.printf("Texture buffer already contains %d\n", hashcode);
        }
    }

    private void newGlyphBuffer() {
        glyphBuffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        if(g2d!=null) {
            g2d.dispose();
        }
        g2d = glyphBuffer.createGraphics();

        // We don;t want to antialias images that are already antialiased.
        //
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
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

    private static final class GlyphData {
        final int page;
        final Rectangle r;

        GlyphData(final int page, final Rectangle r) {
            this.page = page;
            this.r = r;
        }

        @Override
        public String toString() {
            return String.format("[page %s %s]", page, r);
        }
    }
}

package glyphs;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class encapsulates a BufferedImage that holds rectangles containing
 * images of glyphs.
 * <p>
 * Each new rectangle is drawn at the current x,y position if there's enough room,
 * otherwise a new rectangle line is started.
 *
 * @author algol
 */
final class GlyphRectangleBuffer {
    // The buffers that glyphs will be drawn to.
    // Each one of these will eventually be copied to a texture buffer.
    //
    private final List<BufferedImage> rectBuffers;

    // The width and height of each BufferedImage.
    //
    private final int width;
    private final int height;

    // The current rectangle buffer and its graphics (a reference to
    // the end item in rectBuffers).
    //
    private BufferedImage rectBuffer;
    private Graphics2D g2d;

    // The next position to draw a rectangle at.
    //
    private int x;
    private int y;

    // The maximum bounding box height in the current rectangle line.
    // We need this so we know how much to add to y to go to the next line of rectangles.
    //
    private int maxHeight;

    // The coordinates (and other infornation) of each rectangle
    // as required by OpenGL.
    //
    private static final int FLOATS_PER_RECT = 4;
    private float[] rectTextureCoordinates;

    /**
     * Remember where we wrote each rectangle.
     * <p>
     * This stops us writing the same image twice.
     * <p>
     * The key is the hash code of the image in the rectangle.
     */
    private final Map<Integer, Integer> memory;

    GlyphRectangleBuffer(final int width, final int height) {
        this.width = width;
        this.height = height;
        rectBuffers = new ArrayList<>();
        memory = new HashMap<>();

        reset();
    }

    int size() {
        return rectBuffers.size();
    }

    /**
     * Return the i'th buffer.
     *
     * @return
     */
    BufferedImage get(final int i) {
        return rectBuffers.get(i);
    }

    public void reset() {
        rectBuffers.clear();
        memory.clear();
        if(g2d!=null) {
            g2d.dispose();
            g2d = null;
        }

        newRectBuffer();
    }

    /**
     * Add a rectangle containing an image to the buffers.
     * <p>
     * If the image has already been added (using the hash code of the int[]
     * of the image's pixels as the key), the index of the existing rectangle
     * is returned. Otherwise, the image is added and the index of the new
     * rectangle is returned.
     *
     * @param img A BufferedImage containing an image.
     * @return
     */
    int addRectImage(final BufferedImage img) {
        final int w = img.getWidth();
        final int h = img.getHeight();

        // Get the hash code of the image.
        // BufferedImage doesn't have a hashCode() method, so we use the underlying pixels.
        //
        final int hashCode = Arrays.hashCode(img.getRGB(0, 0, w, h, null, 0, w));

        final int rectIndex;
        if(memory.containsKey(hashCode)) {
            // We've seen this image before: return the index of the existing image.
            //
            rectIndex = memory.get(hashCode);
        } else {
            // This is a new image. Add it to the buffer, creating a new buffer if necessary.
            //
            if((x+w) > width) {
                newRectLine();
            }
            if((y+h)>height) {
                newRectBuffer();
            }

            // Copy the image to the current buffer.
            //
            g2d.drawImage(img, x, y, null);

            rectIndex = memory.size();
            memory.put(hashCode, rectIndex);

            final int ptr = rectIndex * FLOATS_PER_RECT;

            // Ensure that the coordinates array has enough room to add more coordinates.
            //
            if(ptr==rectTextureCoordinates.length) {
                rectTextureCoordinates = Arrays.copyOf(rectTextureCoordinates, rectTextureCoordinates.length*2);
            }

            // TODO fill in the right numbers (see the FX version of addTextureCoordinates).
            rectTextureCoordinates[ptr+0] = 0.0f;
            rectTextureCoordinates[ptr+1] = 0.0f;
            rectTextureCoordinates[ptr+2] = 0.0f;
            rectTextureCoordinates[ptr+3] = 0.0f;

            x += w;
            maxHeight = Math.max(h, maxHeight);
        }

        return rectIndex;
    }

//    private int addTextureCoordinates(float width, float height, float buffer) {
//        if (glyphCount * 4 == glyphTextureCoordinates.length) {
//            glyphTextureCoordinates = Arrays.copyOf(glyphTextureCoordinates, glyphTextureCoordinates.length * 2);
//        }
//
//        int glyphBoundsPointer = glyphCount << 2;
//        glyphTextureCoordinates[glyphBoundsPointer++] = currentGlyphPage + (currentGlyphLocationX - buffer) / textureWidth;
//        glyphTextureCoordinates[glyphBoundsPointer++] = (currentGlyphLocationY - buffer) / textureHeight;
//        glyphTextureCoordinates[glyphBoundsPointer++] = (width + buffer * 2) / textureWidth;
//        glyphTextureCoordinates[glyphBoundsPointer++] = (height + buffer * 2) / textureHeight;
//
//        return glyphCount++;
//    }

    private void newRectBuffer() {
        rectBuffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        if(g2d!=null) {
            g2d.dispose();
        }
        g2d = rectBuffer.createGraphics();

        // We don't want to antialias images that are already antialiased.
        //
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        g2d.setBackground(Color.BLACK);
        g2d.setColor(Color.RED);
        g2d.drawRect(0, 0, width-1, height-1);
        g2d.setColor(Color.WHITE);

        rectBuffers.add(rectBuffer);

        x = 0;
        y = 0;
        maxHeight = 0;

        // Start with room for an arbitrary number of rectangles
        // so we don't have to grow the array too quickly.
        //
        rectTextureCoordinates = new float[256 * FLOATS_PER_RECT];
    }

    private void newRectLine() {
        x = 0;
        y += maxHeight;
        maxHeight = 0;
    }

//    private static final class RectData {
//        final int page;
//        final Rectangle r;
//
//        RectData(final int page, final Rectangle r) {
//            this.page = page;
//            this.r = r;
//        }
//
//        @Override
//        public String toString() {
//            return String.format("[page %s %s]", page, r);
//        }
//    }
}

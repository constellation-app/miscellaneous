# CONSTELLATION and fontrendering - how it works

The rendering code is in the `Core OpenGL Display` module, in package `au.gov.asd.tac.constellation.visual.opengl.utilities.glyphs`. If you want to play with the font drawing part, the files in that package (and the interface `au.gov.asd.tac.constellation.visual.opengl.utilities.GlyphManager`) can be run as a standalone Java application, using `GlyphsFrame` as the main class.

## The simple version

CONSTELLATION uses a sequence of fonts, specified by the user in an options panel and retrieved using `LabelFontsPreferenceKeys.getFontNames()`. When a label is rendered, each character is drawn using the first font in the sequence that can draw it, with SansSerif being used as the font of last resort.

The fun happens in `GlyphManagerBI.renderTextAsLigatures()`. The string is drawn into a BufferedImage using a `TextLayout` instance, and `Font.layoutGlyphVector()` is used to get the bounding boxes of the glyphs. The bounding box sub-image contain each glyph is copied to a "texture buffer", which is subsequently copied to an OpenGL texture buffer. A hash of the image within each bounding box is used to determine if this glyph has been previously copied: given the word "eve", the first "e" is copied, the "v" is copied, but the second "e" is not copied, since it has the same image as the first "e". Therefore, the texture buffer only contains a single "e" image.

## The complicated (and more correct) version

Strings aren't as simple as we'd like. If you're using a "simple" font such as Arial, and your labels only contain Latin-1 characters, the simple version above is pretty much what happens. However:

- Because we want to be Unicode aware, we need to use code points, not characters.
- Text doesn't always go left to right.
- Depending on the font and/or the script, there is not necessarily a one-to-one mapping between a code point and a glyph.
- Different fonts can display the same code points.

## Definitions

- Code point: an integer representing a character in Unicode. There are a possible 1,114,112 code points in Unicode, but only 132,000+ are defined in version 12.1.
- Glyph: an image used to represent a sequence of one or more code points.
- Font: a collection of glyphs.
- LTR, RTL: left to right, right to left.
- Texture buffer: a 3 dimensional OpenGL structure used to send data to a GPU.

### Code points

Java's native char/Character type is defined as a 16-bit integer. This is sufficient to hold code points in Unicode's Basic Multilingual Plane (BMP), but cannot reference code points in astral planes. Therefore, iterating through a char[] (as obtained from `String.toCharArray()` for example) must not be done. Instead, the typical way to iterate through a text string `s` is:

```
    final int length = s.length();
    for(int offset = 0; offset < length;) {
        final int codepoint = s.codePointAt(offset);
        final int cc = Character.charCount(codepoint);

        // Do stuff with codepoint ...

        offset += cc;
    }
```

Note that methods such as `String.substring()` still work as long as the string is split at code

### Direction

Scripts such as English and Chinese read left to right, but scripts such as Arabic and Hebrew read right to left. Java isn't very good at drawing multi-directional text, so the first thing to do is split the text into mono-directional sections. This isn't as easy as it sounds.

The Java class `BiDi` implements the Unicode bidirectional algorithm, but don't get too excited.

A typical string in CONSTELLATION might be `AAAAA<LLLLL>`, where `A` is Arabic script and `L` is Latin script. (For example, `المستخدم عشرة<Person>`). According to someone who reads Arabic (not me), this should render as `<LLLLL>AAAAA`. However, the BiDi algorithm does this:

- the characters AAAAA are Arabic, and therefore RTL.
- the character `<` has no explicit direction, but in context with Arabic characters it is RTL.
- the characters LLLLL are Latin, and therefore LTR.
- the character '>' has no explicit direction, but in context with Latin characters it is LTR.
- the text is reordered RTL; because the '<' is RTL, it is reversed to be '>'.

The resulting text is `LLLLL>AAAAA>`, but in CONSTELLATION, we'd rather see `<LLLLL>AAAAA`. Therefore we can't use the built-in `BiDi` rules. The `DirectionRun` class implements its own bidrectional algorithm using the `Character.getDirectionality()` method and making some assumptions when dealing with directionless code points.

### Fonts

Text layout can only be done with a single font, so the next step is to split each mono-directional section into single font sections. The `FontRun` class uses the `Font.canDisplay()` method. In addition, the `Character.UnicodeScript.of()` method is used to determine the script that each code point belongs to. This allows the user to specify that a particular font can only be used for a character if that character belings (or does not belong) to a particular script).

### Glyphs

The original text has now ben divided into sections where each section has only one direction and one font. Each section is drawn into a local BufferedImage using a TextLayout, with a GlyphVector being used to find the bounding box of each glyph. The local BufferedImage is of type `BufferedImage.TYPE_BYTE_GRAY`, since our OpenGL shader only uses white (and multiplies that with the desired color). The size of the BufferedImage is arbitrary, but should be wide enough to handle most labels, and high enough to handle most glyphs. (Note that combining characters can extend glyph height indefinitely. You have to stop somewhere.)

The bounding boxes of some glyphs will overlap, depending on the characters and font, cursive fonts in particular. Overlapping bounding boxes (where the right edge of one bounding box is greater than the left edge of the next bounding box) are merged.

The glyphs have now been reduced to rectangles containing images. The rectangles are copied to the texture buffer as described in the simple version above.

## `GlyphsFrame` application

Run the `GlyphsFrame` standalone application to see the above in action. The application shows the various bounding boxes and the contents of the texture buffer.

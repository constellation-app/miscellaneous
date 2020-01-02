package au.gov.asd.tac.constellation.visual.opengl.utilities.glyphs;

import java.awt.Font;
import java.awt.FontFormatException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 *
 * @author algol
 */
public class FontInfo {
    private static final Logger LOGGER = Logger.getLogger(FontInfo.class.getName());

    final String fontName;
    final int fontStyle;
    final Set<Character.UnicodeScript> mustHave;
    final Set<Character.UnicodeScript> mustNotHave;
    Font font;

    public FontInfo(final String fontName, final int fontStyle, final Set<Character.UnicodeScript> mustHave, final Set<Character.UnicodeScript> mustNotHave) {
        this.fontName = fontName;
        this.fontStyle = fontStyle;
        this.mustHave = mustHave!=null ? mustHave : Collections.emptySet();
        this.mustNotHave = mustNotHave!=null ? mustNotHave : Collections.emptySet();
        this.font = null;
    }

    public void setFont(final int fontSize) {
        if(fontName.toLowerCase().endsWith(".otf")) {
            File otfFile = getOtfFont(fontName);
            if(otfFile!=null) {
                LOGGER.info(String.format("Reading OTF font from %s", otfFile));
                try {
                    final Font otf = Font.createFont(Font.TRUETYPE_FONT, otfFile);
                    font = otf.deriveFont(fontStyle, fontSize);
                }
                catch(final FontFormatException | IOException ex) {
                    LOGGER.log(Level.SEVERE, String.format("Can't load OTF font %s from %s", fontName, otfFile), ex);
                    font = null;
                }
            } else {
                LOGGER.info(String.format("OTF file %s not found", otfFile));
                font = null;
            }
        } else {
            font = new Font(fontName, fontStyle, fontSize);
        }
    }

    public boolean canDisplay(final int codepoint) {
        if(font.canDisplay(codepoint)) {
            final Character.UnicodeScript script = Character.UnicodeScript.of(codepoint);
            if(mustHave.contains(script) || !mustNotHave.contains(script)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Find the File specified by the given OTF font name.
     *
     * @param otfName An OTF font name ending with ".otf".
     *
     * @return A File specifying the font file, or null if it doesn't exist.
     */
    private static File getOtfFont(final String otfName) {
        File otfFile = new File(otfName);
        if(otfFile.isAbsolute()) {
            return otfFile.canRead() ? otfFile : null;
        } else {
            // If it is relative, look in operating system specific places for
            // the font file.
            //
            final String osName = System.getProperty("os.name");
            if(osName.toLowerCase().contains("win")) {
                // Look in the user's local profile, then the system font directory.
                //
                final String lap = System.getenv("LOCALAPPDATA");
                if(lap!=null) {
                    otfFile = new File(String.format("%s/Microsoft/Windows/Fonts/%s", lap, otfName));
                    if(otfFile.canRead()) {
                        return otfFile;
                    } else {
                        final String windir = System.getenv("windir");
                        otfFile = new File(String.format("%s/Fonts/%s", windir, otfName));
                        if(otfFile.canRead()) {
                            return otfFile;
                        }
                    }
                }
            } else {
                // Figure out something for Linux etc.
                //
                return null;
            }
        }

        return null;
    }

    public static class ParsedFontInfo {
        public final FontInfo[] fontsInfo;
        public final List<String> messages;

        private ParsedFontInfo(final List<FontInfo> fontInfoList, final List<String> messages) {
            fontsInfo = fontInfoList.stream().toArray(FontInfo[]::new);
            this.messages = messages;
        }

        public String getMessages() {
            return messages.stream().collect(Collectors.joining("\n"));
        }
    }

    /**
     * Parse lines of a string to find fontName[,bold|plain|block]... for each line.
     *
     * @param lines
     *
     * @return
     */
    public static ParsedFontInfo parseFontInfo(final String[] lines) {
        final List<FontInfo> fiList = new ArrayList<>();
        final List<String> messages = new ArrayList<>();

        int lineno = 0;
        for(String line : lines) {
            lineno++;
            int fontStyle = Font.PLAIN;
            final Set<Character.UnicodeScript> mustHave = new HashSet<>();
            final Set<Character.UnicodeScript> mustNotHave = new HashSet<>();
            boolean ok = true;
            line = line.trim();
            if(line.length()>0 && !line.startsWith("#")) {
                final String[] parts = line.trim().split("\\p{Zs}*,\\p{Zs}*");
                final String fontName = parts[0];//.trim();
                if(fontName.isEmpty()) {
                    ok = false;
                    messages.add(String.format("Line %d: Blank font name", lineno));
                } else {
                    for(int i=1; i<parts.length; i++) {
                        String part = parts[i].toUpperCase();
                        if(part.isEmpty()) {
                            ok = false;
                            messages.add(String.format("Line %d: Blank font description", lineno));
                        } else {
                            switch(part) {
                            case "BOLD":
                                fontStyle = Font.BOLD;
                                break;
                            case "PLAIN":
                                fontStyle = Font.PLAIN;
                                break;
                            default:
                                final boolean mustNot = part.startsWith("!");
                                if(mustNot) {
                                    part = part.substring(1);
                                }
                                try {
                                    final Character.UnicodeScript script = Character.UnicodeScript.forName(part);
                                    final Set<Character.UnicodeScript> s = mustNot ? mustNotHave : mustHave;
                                    s.add(script);
                                } catch(final IllegalArgumentException ex) {
                                    ok = false;
                                    messages.add(String.format("Line %d: Unicode script '%s' does not exist", lineno, part));
                                }
                                break;
                            }
                        }
                    }
                }

                if(ok) {
                    final FontInfo fi = new FontInfo(fontName, fontStyle, mustHave, mustNotHave);
                    fiList.add(fi);
                } else {
                    System.out.printf("!!!! %s\n", messages);
                }
            }
        }

        return new ParsedFontInfo(fiList, messages);
    }

    @Override
    public String toString() {
        return String.format("[FontInfo %s style:%d (%s) must:%s mustNot:%s]", fontName, fontStyle, font, mustHave, mustNotHave);
    }
}

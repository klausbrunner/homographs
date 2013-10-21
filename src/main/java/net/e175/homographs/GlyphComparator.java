package net.e175.homographs;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.List;

/**
 * Glyph comparator. Creates bitmap renderings of character glyphs and compares
 * them for equality to find homographs.
 *
 * @author Klaus A. Brunner 2005-02-10 (updated 2008-05-11)
 */
public class GlyphComparator {

    /**
     * Set this true to enable compareGlyphs().
     */
    public volatile boolean run = false;

    private Font font;
    private Dimension imageSize;
    private int baseline;
    private byte[][] glyphDigestCache;
    private final char targetEndChar;
    private MessageDigest digest = null;

    /**
     * Create a GlyphComparator.
     *
     * @param targetlimit The highest codepoint to check.
     */
    public GlyphComparator(char targetlimit) {
        this.targetEndChar = targetlimit;
        try {
            digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        reset();
    }

    private void reset() {
        this.glyphDigestCache = new byte[this.targetEndChar + 1][];
    }

    /**
     * Render the given character and return a digest value of the resulting
     * image.
     */
    private byte[] getGlyphDigest(char c) {
        byte[] hash = this.glyphDigestCache[c];
        if (hash == null) {
            BufferedImage glyph = this.renderImage(c);
            digest.reset();
            for (int x = glyph.getWidth() - 1; x >= 0; x--)
                for (int y = glyph.getHeight() - 1; y >= 0; y--) {
                    final int pix = glyph.getRGB(x, y);
                    byte b[] = new byte[4];
                    for (int i = 0, shift = 24; i < 4; i++, shift -= 8)
                        b[i] = (byte) (0xFF & (pix >> shift));
                    digest.update(b);
                }

            hash = digest.digest();
            this.glyphDigestCache[c] = hash;
        }
        return hash;
    }

    /**
     * Perform glyph comparison, returning a list of matching codepoint tuples.
     * Stops prematurely (or never starts) if run == false.
     *
     * @param sourceStartChar
     * @param sourceEndChar
     * @return List of 2-element char arrays, representing codepoints with
     *         identical rendering.
     */
    public List<char[]> compareGlyphs(char sourceStartChar, char sourceEndChar) {
        reset();

        List<char[]> matches = new LinkedList<char[]>();

        while (this.run && sourceStartChar < sourceEndChar) {
            byte[] sourceDigest = this.getGlyphDigest(sourceStartChar);
            if (Character.isLetterOrDigit(sourceStartChar)) {
                char ctarget = Character.MIN_CODE_POINT;
                while (this.run && ctarget < targetEndChar) {
                    if (Character.isLetterOrDigit(ctarget) && (ctarget != sourceStartChar)) {
                        byte[] targetDigest = this.getGlyphDigest(ctarget);

                        if (MessageDigest.isEqual(sourceDigest, targetDigest)
                                && imagesEqual(this.renderImage(sourceStartChar), this.renderImage(ctarget))) {

                            final char[] match = {sourceStartChar, ctarget};
                            matches.add(match);
                        }
                    }
                    ctarget++;
                }
            }
            sourceStartChar++;
        }

        return matches;
    }

    /**
     * Check whether two BufferedImages are equal in content.
     */
    private static boolean imagesEqual(final BufferedImage img1, final BufferedImage img2) {
        for (int x = img1.getWidth() - 1; x >= 0; x--) {
            for (int y = img1.getHeight() - 1; y >= 0; y--) {
                if (img1.getRGB(x, y) != img2.getRGB(x, y)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Render a BufferedImage containing the given character.
     */
    private BufferedImage renderImage(char toRender) {
        BufferedImage image = new BufferedImage(this.imageSize.width, this.imageSize.height,
                BufferedImage.TYPE_INT_ARGB);

        Graphics g = image.createGraphics();

        g.setColor(Color.black);
        g.setFont(this.font);

        String str = Character.valueOf(toRender).toString();
        g.drawString(str, 1, this.baseline);

        return image;
    }

    /**
     * @return Returns the font.
     */
    public Font getFont() {
        return font;
    }

    /**
     * Set font to use for glyph comparison.
     *
     * @param font The font to set.
     */
    public void setFont(Font font) {
        this.font = font;

        // get the required image size for rendering glyphs
        // there's probably a simpler way of doing this?
        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        Graphics g = image.getGraphics();
        g.setFont(font);
        FontMetrics metrics = g.getFontMetrics();
        this.imageSize = new Dimension(metrics.getMaxAdvance() + 3, metrics.getMaxAscent() + metrics.getMaxDescent()
                + 3);
        this.baseline = metrics.getMaxAscent() + 1;
    }
}

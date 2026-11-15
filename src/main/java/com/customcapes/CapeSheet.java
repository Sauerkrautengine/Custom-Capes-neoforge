package com.customcapes;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/** Macht aus einem beliebigen Bild eine Cape-Textur (Vanilla-Layout 64x32, hier in 16-facher Auflösung). */
public final class CapeSheet {

    private CapeSheet() {}

    public static boolean isSheet(BufferedImage src) {
        return src.getWidth() % 64 == 0 && src.getHeight() * 2 == src.getWidth();
    }

    public static BufferedImage build(BufferedImage src) {
        // Schon eine fertige Cape-Textur (64x32 oder ein Vielfaches davon)? Dann unverändert nehmen.
        if (isSheet(src)) return src;

        int s = 16;
        BufferedImage out = new BufferedImage(64 * s, 32 * s, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        Color avg = average(src);
        Color edge = new Color((int) (avg.getRed() * 0.7), (int) (avg.getGreen() * 0.7), (int) (avg.getBlue() * 0.7));
        g.setColor(edge);
        g.fillRect(1 * s, 0, 10 * s, s);        // oben
        g.fillRect(11 * s, 0, 10 * s, s);       // unten
        g.fillRect(0, s, s, 16 * s);            // links
        g.fillRect(11 * s, s, s, 16 * s);       // rechts
        g.fillRect(12 * s, s, 10 * s, 16 * s);  // Innenseite
        // Elytra-Flächen (nur als Notlösung, falls die Original-Elytra-Textur nicht geladen werden kann)
        g.fillRect(24 * s, 0, 10 * s, 2 * s);
        g.fillRect(34 * s, 0, 10 * s, 2 * s);
        g.fillRect(22 * s, 2 * s, 2 * s, 20 * s);
        g.fillRect(34 * s, 2 * s, 2 * s, 20 * s);

        drawCover(g, src, 1 * s, 1 * s, 10 * s, 16 * s);  // Außenseite: das Bild
        drawCover(g, src, 24 * s, 2 * s, 10 * s, 20 * s);
        drawCover(g, src, 36 * s, 2 * s, 10 * s, 20 * s);
        g.dispose();
        return out;
    }

    private static void drawCover(Graphics2D g, BufferedImage src, int dx, int dy, int dw, int dh) {
        int w = src.getWidth(), h = src.getHeight();
        double scale = Math.max((double) dw / w, (double) dh / h);
        int sw = (int) Math.round(dw / scale), sh = (int) Math.round(dh / scale);
        int sx = (w - sw) / 2, sy = (h - sh) / 2;
        g.drawImage(src, dx, dy, dx + dw, dy + dh, sx, sy, sx + sw, sy + sh, null);
    }

    /** Scharfe Vorschau der Cape-Rückseite im Seitenverhältnis 10:16. */
    public static BufferedImage previewImage(BufferedImage src, int w, int h) {
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        if (isSheet(src)) {
            int u = src.getWidth() / 64;
            g.drawImage(src, 0, 0, w, h, u, u, 11 * u, 17 * u, null);
        } else {
            drawCover(g, src, 0, 0, w, h);
        }
        g.dispose();
        return out;
    }

    private static Color average(BufferedImage img) {
        long r = 0, gr = 0, b = 0, n = 0;
        int stepX = Math.max(1, img.getWidth() / 64), stepY = Math.max(1, img.getHeight() / 64);
        for (int y = 0; y < img.getHeight(); y += stepY) {
            for (int x = 0; x < img.getWidth(); x += stepX) {
                int p = img.getRGB(x, y);
                if ((p >>> 24) < 32) continue;
                r += (p >> 16) & 255; gr += (p >> 8) & 255; b += p & 255; n++;
            }
        }
        if (n == 0) return new Color(90, 90, 90);
        return new Color((int) (r / n), (int) (gr / n), (int) (b / n));
    }
}

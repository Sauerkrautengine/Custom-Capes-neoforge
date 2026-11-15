package com.customcapes;

import java.awt.image.BufferedImage;
import java.util.Random;

/** Mitgelieferte Capes. Sie werden im Pixel-Stil per Code gezeichnet, es sind keine Bilddateien nötig. */
public final class Presets {

    public static final String[] IDS = {"sunset", "ocean", "night", "forest", "flame", "rainbow"};
    public static final String[] LABELS = {"Sonnenuntergang", "Ozean", "Nachthimmel", "Wald", "Flamme", "Regenbogen"};

    private static final int W = 20, H = 32, SCALE = 8;

    private Presets() {}

    public static boolean has(String id) {
        for (String s : IDS) if (s.equals(id)) return true;
        return false;
    }

    public static String label(String id) {
        for (int i = 0; i < IDS.length; i++) if (IDS[i].equals(id)) return LABELS[i];
        return id;
    }

    private static int rgb(double r, double g, double b) {
        return 0xFF000000 | (c(r) << 16) | (c(g) << 8) | c(b);
    }

    private static int c(double v) {
        return (int) Math.max(0, Math.min(255, Math.round(v)));
    }

    private static int mix(int a, int b, double t) {
        t = Math.max(0, Math.min(1, t));
        int r = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int g = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
        return rgb(r, g, bl);
    }

    private static void set(BufferedImage img, int x, int y, int argb) {
        if (x >= 0 && y >= 0 && x < W && y < H) img.setRGB(x, y, argb);
    }

    public static BufferedImage render(String id) {
        BufferedImage px = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        switch (id) {
            case "sunset" -> sunset(px);
            case "ocean" -> ocean(px);
            case "night" -> night(px);
            case "forest" -> forest(px);
            case "flame" -> flame(px);
            default -> rainbow(px);
        }
        BufferedImage out = new BufferedImage(W * SCALE, H * SCALE, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < out.getHeight(); y++) {
            for (int x = 0; x < out.getWidth(); x++) out.setRGB(x, y, px.getRGB(x / SCALE, y / SCALE));
        }
        return out;
    }

    private static void sunset(BufferedImage img) {
        int c0 = rgb(45, 25, 110), c1 = rgb(225, 70, 110), c2 = rgb(255, 170, 60);
        for (int y = 0; y < H; y++) {
            double t = y / (double) (H - 1);
            int col = t < 0.6 ? mix(c0, c1, t / 0.6) : mix(c1, c2, (t - 0.6) / 0.4);
            for (int x = 0; x < W; x++) set(img, x, y, col);
        }
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                double d = Math.hypot(x - 9.5, y - 23);
                if (d <= 6.5 && y <= 26) set(img, x, y, d > 5 ? rgb(255, 190, 90) : rgb(255, 235, 140));
            }
        }
        for (int y = 27; y < H; y++) for (int x = 0; x < W; x++) set(img, x, y, y == 27 ? rgb(70, 30, 70) : rgb(28, 14, 48));
    }

    private static void ocean(BufferedImage img) {
        int top = rgb(40, 140, 200), bottom = rgb(8, 28, 88);
        for (int y = 0; y < H; y++) {
            int col = mix(top, bottom, y / (double) (H - 1));
            for (int x = 0; x < W; x++) set(img, x, y, col);
        }
        for (int y = 3; y < H; y += 5) {
            for (int x = 0; x < W; x++) {
                int off = (int) Math.round(Math.sin((x + y) * 0.9) * 1.0);
                set(img, x, y + off, mix(rgb(130, 200, 240), rgb(40, 100, 170), y / (double) H));
            }
        }
        int[][] bubbles = {{3, 8}, {15, 12}, {7, 18}, {12, 24}, {4, 28}, {17, 5}};
        for (int[] b : bubbles) set(img, b[0], b[1], rgb(190, 235, 255));
    }

    private static void night(BufferedImage img) {
        int top = rgb(6, 8, 28), bottom = rgb(26, 24, 70);
        for (int y = 0; y < H; y++) {
            int col = mix(top, bottom, y / (double) (H - 1));
            for (int x = 0; x < W; x++) set(img, x, y, col);
        }
        Random r = new Random(3);
        for (int i = 0; i < 24; i++) {
            set(img, r.nextInt(W), r.nextInt(H), r.nextBoolean() ? rgb(255, 255, 255) : rgb(255, 235, 150));
        }
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                boolean inMoon = Math.hypot(x - 9.5, y - 10) <= 5;
                boolean cut = Math.hypot(x - 12, y - 8.5) <= 4.5;
                if (inMoon && !cut) set(img, x, y, rgb(245, 240, 205));
            }
        }
    }

    private static void forest(BufferedImage img) {
        int top = rgb(150, 210, 140), bottom = rgb(30, 100, 55);
        for (int y = 0; y < H; y++) {
            int col = mix(top, bottom, y / (double) (H - 1) * 1.1);
            for (int x = 0; x < W; x++) set(img, x, y, col);
        }
        int[][] trees = {{4, 12}, {10, 8}, {16, 13}, {7, 17}, {13, 18}};
        for (int[] t : trees) {
            int cx = t[0], y0 = t[1];
            for (int k = 0; k < 11; k++) {
                for (int x = cx - k / 2; x <= cx + k / 2; x++) {
                    set(img, x, y0 + k, k % 3 == 0 ? rgb(16, 70, 34) : rgb(10, 52, 26));
                }
            }
            set(img, cx, y0 + 11, rgb(70, 45, 20));
            set(img, cx, y0 + 12, rgb(70, 45, 20));
        }
        for (int y = 26; y < H; y++) for (int x = 0; x < W; x++) set(img, x, y, y == 26 ? rgb(70, 130, 60) : rgb(40, 28, 14));
    }

    private static void flame(BufferedImage img) {
        for (int y = 0; y < H; y++) {
            int col = mix(rgb(12, 4, 6), rgb(50, 8, 8), y / (double) (H - 1));
            for (int x = 0; x < W; x++) set(img, x, y, col);
        }
        for (int x = 0; x < W; x++) {
            int topY = 11 + (int) Math.round(4 * Math.sin(x * 1.3) + 3 * Math.sin(x * 0.55 + 1));
            for (int y = topY; y < H; y++) {
                double t = (y - topY) / (double) Math.max(1, H - 1 - topY);
                int col = t < 0.5 ? mix(rgb(210, 30, 10), rgb(255, 130, 20), t / 0.5) : mix(rgb(255, 130, 20), rgb(255, 225, 90), (t - 0.5) / 0.5);
                set(img, x, y, col);
            }
        }
        Random r = new Random(5);
        for (int i = 0; i < 10; i++) set(img, r.nextInt(W), r.nextInt(10), rgb(255, 140, 30));
    }

    private static void rainbow(BufferedImage img) {
        int[] cols = {rgb(230, 40, 40), rgb(245, 140, 30), rgb(250, 220, 40), rgb(60, 190, 70), rgb(50, 130, 230), rgb(80, 60, 200), rgb(160, 70, 200)};
        for (int y = 0; y < H; y++) {
            int col = cols[Math.min(6, y * 7 / H)];
            for (int x = 0; x < W; x++) set(img, x, y, col);
        }
    }
}

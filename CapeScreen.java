package com.customcapes;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class CapeScreen extends Screen {

    private static final int TEX_W = 320;
    private static final int TEX_H = 512;

    private record Entry(String key, String label) {}

    private final Screen parent;
    private List<Entry> entries = new ArrayList<>();
    private int selected = -1;
    private int scroll = 0;
    private String previewKey;
    private boolean hasPreview;
    private String message = "";

    public CapeScreen(Screen parent) {
        super(Component.literal("Custom Capes"));
        this.parent = parent;
    }

    private int listLeft() { return 20; }
    private int listWidth() { return this.width / 2 - 30; }
    private int listTop() { return 36; }
    private int rowsVisible() { return Math.max(1, (this.height - 36 - 70) / 22); }

    private void buildEntries() {
        entries = new ArrayList<>();
        for (int i = 0; i < Presets.IDS.length; i++) {
            entries.add(new Entry("preset:" + Presets.IDS[i], "Preset: " + Presets.LABELS[i]));
        }
        for (Path p : CapeManager.listImages()) {
            String n = p.getFileName().toString();
            entries.add(new Entry(n, n));
        }
    }

    @Override
    protected void init() {
        buildEntries();
        String active = CapeManager.activeKey();
        if (selected < 0 || selected >= entries.size()) {
            selected = -1;
            for (int i = 0; i < entries.size(); i++) {
                if (entries.get(i).key().equals(active)) selected = i;
            }
            if (selected < 0 && !entries.isEmpty()) selected = 0;
        }
        int vis = rowsVisible();
        scroll = Math.max(0, Math.min(scroll, Math.max(0, entries.size() - vis)));
        loadPreview();

        for (int r = 0; r < vis; r++) {
            int idx = scroll + r;
            if (idx >= entries.size()) break;
            final int fi = idx;
            Entry e = entries.get(idx);
            String label = (idx == selected ? "> " : "") + (e.key().equals(active) ? "* " : "") + shorten(e.label(), listWidth() - 36);
            this.addRenderableWidget(Button.builder(Component.literal(label), btn -> {
                selected = fi;
                this.rebuildWidgets();
            }).bounds(listLeft(), listTop() + r * 22, listWidth(), 20).build());
        }

        int gap = 4;
        int bw = Math.min(92, (this.width - 40 - 4 * gap) / 5);
        int total = 5 * bw + 4 * gap;
        int x0 = (this.width - total) / 2, y = this.height - 28;
        this.addRenderableWidget(Button.builder(Component.literal("Übernehmen"), b -> apply())
                .bounds(x0, y, bw, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Vanilla-Cape"), b -> {
            CapeManager.select(null);
            message = "Vanilla-Cape aktiv";
            this.rebuildWidgets();
        }).bounds(x0 + (bw + gap), y, bw, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Physik: " + CapePhysics.mode().label()), b -> {
            CapePhysics.Mode m = CapePhysics.cycle();
            message = "Cape-Physik: " + m.label();
            this.rebuildWidgets();
        }).bounds(x0 + 2 * (bw + gap), y, bw, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Ordner öffnen"), b -> CapeManager.openFolder())
                .bounds(x0 + 3 * (bw + gap), y, bw, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Fertig"), b -> this.onClose())
                .bounds(x0 + 4 * (bw + gap), y, bw, 20).build());
    }

    private String shorten(String s, int maxWidth) {
        if (this.font.width(s) <= maxWidth) return s;
        String t = s;
        while (t.length() > 1 && this.font.width(t + "...") > maxWidth) t = t.substring(0, t.length() - 1);
        return t + "...";
    }

    private void apply() {
        if (selected >= 0 && selected < entries.size()) {
            Entry e = entries.get(selected);
            CapeManager.select(e.key());
            message = "Cape übernommen: " + e.label();
            this.rebuildWidgets();
        }
    }

    private void loadPreview() {
        if (selected < 0 || selected >= entries.size()) {
            hasPreview = false;
            previewKey = null;
            return;
        }
        String key = entries.get(selected).key();
        if (key.equals(previewKey)) return;
        previewKey = key;
        hasPreview = false;
        try {
            BufferedImage src = CapeManager.load(key);
            if (src != null) {
                CapeManager.setPreview(CapeSheet.previewImage(src, TEX_W, TEX_H));
                hasPreview = true;
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int max = Math.max(0, entries.size() - rowsVisible());
        int ns = Math.max(0, Math.min(max, scroll - (int) Math.signum(scrollY)));
        if (ns != scroll) {
            scroll = ns;
            this.rebuildWidgets();
        }
        return true;
    }

    @Override
    public void onFilesDrop(List<Path> paths) {
        Path last = CapeManager.importFiles(paths);
        if (last != null) {
            CapeManager.select(last.getFileName().toString());
            message = "Hinzugefügt: " + last.getFileName();
            selected = -1;
            previewKey = null;
            this.rebuildWidgets();
        }
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(parent);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        String title = "Custom Capes";
        g.text(this.font, title, (this.width - this.font.width(title)) / 2, 12, 0xFFFFFFFF, true);

        int hintY = this.height - 52;
        g.text(this.font, "Bild ins Fenster ziehen = hinzufügen", listLeft(), hintY, 0xFF999999, true);
        if (entries.size() > rowsVisible()) {
            g.text(this.font, "Mausrad = scrollen", listLeft(), hintY - 11, 0xFF999999, true);
        }

        int ph = Math.max(40, this.height - 110);
        int pw = ph * 10 / 16;
        int maxW = this.width / 2 - 40;
        if (pw > maxW) {
            pw = maxW;
            ph = pw * 16 / 10;
        }
        int px = this.width * 3 / 4 - pw / 2, py = 40;
        g.fill(px - 2, py - 2, px + pw + 2, py + ph + 2, 0xFFFFFFFF);
        g.fill(px, py, px + pw, py + ph, 0xFF303030);
        if (hasPreview) {
            g.blit(RenderPipelines.GUI_TEXTURED, CapeManager.PREVIEW_ID, px, py, 0, 0, pw, ph, TEX_W, TEX_H, TEX_W, TEX_H);
        }
        int ty = py + ph + 6;
        g.text(this.font, "Vorschau (Rückseite)", px, ty, 0xFFCCCCCC, true);
        if (!message.isEmpty()) g.text(this.font, message, px, ty + 12, 0xFF88FF88, true);
    }
}

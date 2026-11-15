package com.customcapes;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import org.slf4j.Logger;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Locale;
import java.util.stream.Stream;

public final class CapeManager {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Identifier TEXTURE_ID =
            Identifier.fromNamespaceAndPath(CapesMod.MOD_ID, "own_cape");
    public static final Identifier PREVIEW_ID =
            Identifier.fromNamespaceAndPath(CapesMod.MOD_ID, "preview");

    private static final Identifier ELYTRA_ID =
            Identifier.fromNamespaceAndPath(CapesMod.MOD_ID, "vanilla_elytra");

    private static boolean elytraTried;
    private static ClientAsset.Texture elytraTex;

    private static Path dir;
    private static String signature = "";
    private static int ticks;
    private static ClientAsset.Texture current;
    private static String activeKey;

    private CapeManager() {}

    public static void init(Path gameDir) {
        dir = gameDir.resolve("customcapes");
        try {
            Files.createDirectories(dir);
        } catch (Exception e) {
            LOGGER.warn("[CustomCapes] could not create folder", e);
        }
    }

    public static Path dir() {
        return dir;
    }

    /** Das Cape-Textur-Objekt für den eigenen Spieler, oder null (= Vanilla-Cape). */
    public static ClientAsset.Texture current() {
        return current;
    }

    /** Schlüssel des aktiven Capes: Dateiname oder "preset:<id>", oder null. */
    public static String activeKey() {
        return activeKey;
    }

    /**
     * Die normale Elytra-Textur des Spiels. Ohne sie würde die Elytra die Cape-Textur benutzen
     * und (wegen deren leerer Flügel-Fläche) unsichtbar sein.
     */
    public static ClientAsset.Texture vanillaElytra() {
        if (elytraTried) return elytraTex;
        elytraTried = true;
        String[] paths = {"textures/entity/equipment/wings/elytra.png", "textures/entity/elytra.png"};
        for (String p : paths) {
            try {
                Optional<Resource> r = Minecraft.getInstance().getResourceManager()
                        .getResource(Identifier.withDefaultNamespace(p));
                if (r.isPresent()) {
                    try (InputStream in = r.get().open()) {
                        BufferedImage img = ImageIO.read(in);
                        if (img != null) {
                            register(ELYTRA_ID, img);
                            elytraTex = new ClientAsset.ResourceTexture(ELYTRA_ID, ELYTRA_ID);
                            LOGGER.info("[CustomCapes] vanilla elytra texture loaded from {}", p);
                            return elytraTex;
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.warn("[CustomCapes] could not read {}", p, e);
            }
        }
        LOGGER.warn("[CustomCapes] vanilla elytra texture not found, elytra will use the cape texture");
        return null;
    }

    public static void tick() {
        if (++ticks % 40 != 1) return;
        refresh();
    }

    private static boolean isImage(Path p) {
        String n = p.getFileName().toString().toLowerCase(Locale.ROOT);
        return n.endsWith(".png") || n.endsWith(".jpg") || n.endsWith(".jpeg");
    }

    public static List<Path> listImages() {
        if (dir == null) return new ArrayList<>();
        try (Stream<Path> s = Files.list(dir)) {
            return new ArrayList<>(s.filter(Files::isRegularFile).filter(CapeManager::isImage).sorted().toList());
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /** Welches Cape soll aktiv sein? null = Vanilla. */
    private static String effectiveKey() {
        try {
            Path sel = dir.resolve("selected.txt");
            if (Files.exists(sel)) {
                String v = Files.readString(sel).trim();
                if (v.isEmpty() || v.equalsIgnoreCase("none")) return null;
                if (v.startsWith("preset:")) {
                    if (Presets.has(v.substring(7))) return v;
                } else {
                    Path p = dir.resolve(v).normalize();
                    if (p.startsWith(dir) && Files.isRegularFile(p)) return v;
                }
            }
        } catch (Exception ignored) {
        }
        List<Path> files = listImages();
        for (Path p : files) {
            if (p.getFileName().toString().equalsIgnoreCase("cape.png")) return p.getFileName().toString();
        }
        return files.isEmpty() ? null : files.get(0).getFileName().toString();
    }

    /** Lädt das Quellbild zu einem Schlüssel (Preset oder Datei). */
    public static BufferedImage load(String key) throws Exception {
        if (key.startsWith("preset:")) return Presets.render(key.substring(7));
        return ImageIO.read(dir.resolve(key).toFile());
    }

    /** Wählt ein Cape aus; null = Vanilla-Cape. */
    public static void select(String key) {
        try {
            Files.writeString(dir.resolve("selected.txt"), key == null ? "none" : key);
        } catch (Exception e) {
            LOGGER.warn("[CustomCapes] could not save selection", e);
        }
        signature = "changed";
        refresh();
    }

    /** Kopiert Bilder in den Ordner (z. B. per Drag and Drop). Gibt die zuletzt kopierte Datei zurück. */
    public static Path importFiles(List<Path> paths) {
        Path last = null;
        for (Path p : paths) {
            try {
                if (!Files.isRegularFile(p) || !isImage(p)) continue;
                String name = p.getFileName().toString();
                int dot = name.lastIndexOf('.');
                String base = dot > 0 ? name.substring(0, dot) : name;
                String ext = dot > 0 ? name.substring(dot) : "";
                Path target = dir.resolve(name);
                int i = 1;
                while (Files.exists(target)) target = dir.resolve(base + "-" + (i++) + ext);
                Files.copy(p, target, StandardCopyOption.COPY_ATTRIBUTES);
                last = target;
            } catch (Exception e) {
                LOGGER.warn("[CustomCapes] could not import {}", p, e);
            }
        }
        return last;
    }

    /** Öffnet den Ordner im Datei-Explorer (ohne java.awt.Desktop, das im Spiel oft nicht verfügbar ist). */
    public static void openFolder() {
        if (dir == null) return;
        Thread t = new Thread(() -> {
            try {
                String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
                ProcessBuilder pb;
                if (os.contains("win")) pb = new ProcessBuilder("explorer.exe", dir.toAbsolutePath().toString());
                else if (os.contains("mac")) pb = new ProcessBuilder("open", dir.toAbsolutePath().toString());
                else pb = new ProcessBuilder("xdg-open", dir.toAbsolutePath().toString());
                pb.start();
            } catch (Throwable e) {
                LOGGER.warn("[CustomCapes] could not open folder", e);
            }
        }, "customcapes-open-folder");
        t.setDaemon(true);
        t.start();
    }

    private static void register(Identifier id, BufferedImage image) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        NativeImage ni = NativeImage.read(new ByteArrayInputStream(out.toByteArray()));
        Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(() -> "custom_capes_" + id.getPath(), ni));
    }

    /** Registriert die Vorschau-Textur für das Menü. */
    public static void setPreview(BufferedImage image) {
        try {
            register(PREVIEW_ID, image);
        } catch (Exception e) {
            LOGGER.warn("[CustomCapes] preview failed", e);
        }
    }

    private static void refresh() {
        if (dir == null) return;
        String key = effectiveKey();
        String sig = "";
        try {
            if (key != null) {
                sig = key;
                if (!key.startsWith("preset:")) {
                    Path f = dir.resolve(key);
                    sig += "|" + Files.getLastModifiedTime(f).toMillis() + "|" + Files.size(f);
                }
            }
        } catch (Exception e) {
            return;
        }
        if (sig.equals(signature)) return;
        signature = sig;

        if (key == null) {
            current = null;
            activeKey = null;
            return;
        }
        try {
            BufferedImage src = load(key);
            if (src == null) {
                LOGGER.warn("[CustomCapes] unsupported image: {}", key);
                current = null;
                activeKey = null;
                return;
            }
            register(TEXTURE_ID, CapeSheet.build(src));
            current = new ClientAsset.ResourceTexture(TEXTURE_ID, TEXTURE_ID);
            activeKey = key;
            LOGGER.info("[CustomCapes] loaded {}", key);
        } catch (Exception e) {
            LOGGER.warn("[CustomCapes] failed to load cape", e);
            current = null;
            activeKey = null;
        }
    }
}

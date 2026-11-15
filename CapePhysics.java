package com.customcapes;

import com.mojang.logging.LogUtils;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.slf4j.Logger;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Cape-Physik: Die Vanilla-Werte (Lehnen, Flattern, seitliches Lehnen) dienen als Ziel.
 * Das Cape folgt ihnen wie an einer weichen Feder (Trägheit, Nachschwingen), die Ausschläge
 * werden verstärkt, Drehungen schleudern das Cape zur Seite, beim Laufen flattert es.
 */
public final class CapePhysics {

    private static final Logger LOGGER = LogUtils.getLogger();

    public enum Mode {
        OFF("Aus", 1.0, 0, 0, 0),
        LIGHT("Leicht", 1.4, 10.0, 0.55, 1.5),
        NORMAL("Normal", 2.0, 7.5, 0.30, 3.0),
        STRONG("Stark", 2.8, 5.5, 0.20, 4.5);

        final String label;
        final double gain;
        final double omega;
        final double zeta;
        final double sway;

        Mode(String label, double gain, double omega, double zeta, double sway) {
            this.label = label;
            this.gain = gain;
            this.omega = omega;
            this.zeta = zeta;
            this.sway = sway;
        }

        public String label() {
            return label;
        }
    }

    private static final class Sim {
        final double[] x = new double[3];
        final double[] v = new double[3];
        long last;
        boolean init;
        float prevYaw;
        double yawRate;
    }

    private static final Map<Integer, Sim> SIMS = new HashMap<>();
    private static Mode mode = Mode.NORMAL;
    private static long lastPurge;

    private CapePhysics() {}

    public static Mode mode() {
        return mode;
    }

    public static void load() {
        try {
            Path f = CapeManager.dir().resolve("settings.properties");
            if (Files.exists(f)) {
                Properties p = new Properties();
                try (Reader r = Files.newBufferedReader(f)) {
                    p.load(r);
                }
                mode = Mode.valueOf(p.getProperty("physics", "NORMAL"));
            }
        } catch (Exception e) {
            mode = Mode.NORMAL;
        }
    }

    private static void save() {
        try {
            Properties p = new Properties();
            p.setProperty("physics", mode.name());
            try (Writer w = Files.newBufferedWriter(CapeManager.dir().resolve("settings.properties"))) {
                p.store(w, "Custom Capes");
            }
        } catch (Exception e) {
            LOGGER.warn("[CustomCapes] could not save settings", e);
        }
    }

    public static Mode cycle() {
        Mode[] all = Mode.values();
        mode = all[(mode.ordinal() + 1) % all.length];
        save();
        return mode;
    }

    /** Wird nach dem Berechnen des Render-Zustands aufgerufen und verändert die Cape-Werte. */
    public static void apply(int entityId, AvatarRenderState state) {
        if (mode == Mode.OFF) return;

        long now = System.nanoTime();
        if (now - lastPurge > 10_000_000_000L) {
            lastPurge = now;
            SIMS.values().removeIf(s -> now - s.last > 5_000_000_000L);
        }

        Sim m = SIMS.computeIfAbsent(entityId, k -> new Sim());
        double g = mode.gain;
        float yaw = state.bodyRot;

        if (!m.init) {
            m.init = true;
            m.x[0] = state.capeLean * g;
            m.x[1] = state.capeFlap * g;
            m.x[2] = state.capeLean2 * g;
            m.prevYaw = yaw;
            m.last = now;
        }

        double dt = Math.min(0.05, (now - m.last) / 1e9);
        m.last = now;

        // Drehgeschwindigkeit des Körpers (Grad pro Sekunde), geglättet
        if (dt > 0.0005) {
            double d = ((yaw - m.prevYaw + 540.0) % 360.0) - 180.0;
            double rate = Math.max(-720, Math.min(720, d / dt));
            m.yawRate += (rate - m.yawRate) * Math.min(1.0, dt * 12.0);
            m.prevYaw = yaw;
        }

        double speed = Math.max(0, Math.min(2.0, state.capeLean / 40.0));
        double[] target = {
                state.capeLean * g + Math.abs(m.yawRate) * 0.02 * g,
                state.capeFlap * g,
                state.capeLean2 * g - m.yawRate * 0.035 * g
        };

        if (dt > 0) {
            int steps = Math.max(1, (int) Math.ceil(dt / (1.0 / 120.0)));
            double h = dt / steps;
            double w = mode.omega, z = mode.zeta;
            for (int s = 0; s < steps; s++) {
                for (int i = 0; i < 3; i++) {
                    double a = w * w * (target[i] - m.x[i]) - 2 * z * w * m.v[i];
                    m.v[i] += a * h;
                    m.x[i] += m.v[i] * h;
                }
            }
        }

        double t = now / 1e9;
        double breeze = (Math.sin(t * 1.4 + entityId * 1.7) * 1.0 + Math.sin(t * 2.9 + entityId) * 0.5) * mode.sway;
        double flutter = Math.sin(t * 11.0 + entityId) * 1.6 * speed * g;
        double flutter2 = Math.sin(t * 7.3 + entityId * 2.0) * 2.2 * speed * g;

        state.capeLean = (float) clamp(m.x[0], -10, 150);
        state.capeFlap = (float) clamp(m.x[1] + breeze + flutter, -20, 80);
        state.capeLean2 = (float) clamp(m.x[2] + breeze * 0.8 + flutter2, -45, 45);
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}

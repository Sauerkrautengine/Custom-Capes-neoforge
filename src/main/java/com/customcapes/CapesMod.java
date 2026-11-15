package com.customcapes;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.slf4j.Logger;

import java.lang.reflect.Method;

public final class CapesMod {

    public static final String MOD_ID = "custom_capes";

    private static final Logger LOGGER = LogUtils.getLogger();

    private CapesMod() {}

    /**
     * Öffnet einen Bildschirm (null = schließen). Je nach Minecraft-Version liegt setScreen
     * entweder bei Minecraft selbst (26.1.x) oder bei Minecraft.gui (26.3). Darum per Reflection.
     */
    public static void openScreen(Screen screen) {
        Minecraft mc = Minecraft.getInstance();
        try {
            Method m = findSetScreen(mc.getClass());
            if (m != null) {
                m.invoke(mc, screen);
                return;
            }
            Object gui = mc.getClass().getField("gui").get(mc);
            Method m2 = findSetScreen(gui.getClass());
            if (m2 != null) {
                m2.invoke(gui, screen);
                return;
            }
            LOGGER.warn("[CustomCapes] no setScreen method found");
        } catch (Throwable t) {
            LOGGER.warn("[CustomCapes] could not open screen", t);
        }
    }

    private static Method findSetScreen(Class<?> c) {
        for (Method m : c.getMethods()) {
            if (m.getName().equals("setScreen") && m.getParameterCount() == 1
                    && m.getParameterTypes()[0].isAssignableFrom(Screen.class)) {
                return m;
            }
        }
        return null;
    }
}

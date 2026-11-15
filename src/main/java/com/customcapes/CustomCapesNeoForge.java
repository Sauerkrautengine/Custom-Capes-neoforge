package com.customcapes;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = CapesMod.MOD_ID, dist = Dist.CLIENT)
public class CustomCapesNeoForge {

    private static KeyMapping menuKey;

    public CustomCapesNeoForge(IEventBus modBus) {
        CapeManager.init(FMLPaths.GAMEDIR.get());
        CapePhysics.load();
        modBus.addListener(this::registerKeys);
        NeoForge.EVENT_BUS.addListener(this::onClientTick);
    }

    private void registerKeys(RegisterKeyMappingsEvent event) {
        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath(CapesMod.MOD_ID, "main"));
        menuKey = new KeyMapping("key.custom_capes.menu", InputConstants.KEY_K, category);
        event.register(menuKey);
    }

    private void onClientTick(ClientTickEvent.Post event) {
        CapeManager.tick();
        if (menuKey == null) return;
        Minecraft mc = Minecraft.getInstance();
        while (menuKey.consumeClick()) {
            CapesMod.openScreen(new CapeScreen(null));
        }
    }
}

package com.customcapes.mixin;

import com.customcapes.CapeManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.ClientAsset;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin {

    @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
    private void customcapes$replaceCape(CallbackInfoReturnable<PlayerSkin> cir) {
        if ((Object) this != Minecraft.getInstance().player) return;
        ClientAsset.Texture cape = CapeManager.current();
        if (cape == null) return;
        PlayerSkin s = cir.getReturnValue();
        // Elytra bleibt wie im Vanilla-Spiel: eigene Elytra-Textur, sonst die normale des Spiels,
        // nur im Notfall die Cape-Textur
        ClientAsset.Texture elytra = s.elytra() != null ? s.elytra() : CapeManager.vanillaElytra();
        if (elytra == null) elytra = cape;
        cir.setReturnValue(new PlayerSkin(s.body(), cape, elytra, s.model(), s.secure()));
    }
}

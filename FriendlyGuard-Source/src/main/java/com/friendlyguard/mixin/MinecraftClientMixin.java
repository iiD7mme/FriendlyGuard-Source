package com.friendlyguard.mixin;

import com.friendlyguard.config.FriendlyGuardConfig;
import com.friendlyguard.glow.FriendGlowManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @Inject(method = "hasOutline", at = @At("HEAD"), cancellable = true)
    private void onHasOutline(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof PlayerEntity player) {
            FriendlyGuardConfig config = FriendlyGuardConfig.getInstance();
            if (config.isProtectionEnabled() && config.isProtected(player.getGameProfile().getName())) {
                // If behind a wall, DO NOT glow under any circumstance ("ولا تخليه يبين من ورا الجدار")
                if (!FriendGlowManager.isVisibleNotBehindWall(MinecraftClient.getInstance().player, player)) {
                    cir.setReturnValue(false);
                    return;
                }
                // When friend glow is enabled and visible, glow with outline!
                if (config.isFriendGlowEnabled()) {
                    cir.setReturnValue(true);
                }
            }
        }
    }
}

package com.friendlyguard.mixin;

import com.friendlyguard.config.FriendlyGuardConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin {

    @Inject(method = "attackEntity", at = @At("HEAD"), cancellable = true)
    private void friendlyguard$preventAttack(PlayerEntity player, Entity target, CallbackInfo ci) {
        FriendlyGuardConfig config = FriendlyGuardConfig.getInstance();
        if (config.isProtectionEnabled() && target instanceof PlayerEntity targetPlayer) {
            String targetName = targetPlayer.getGameProfile().getName();
            if (config.isProtected(targetName)) {
                ci.cancel();

                if (config.isShowWarningMessage() && player == MinecraftClient.getInstance().player) {
                    player.sendMessage(
                        Text.literal("You cannot hit " + targetName + "! (Protected)")
                            .formatted(Formatting.RED),
                        true
                    );
                }
            }
        }
    }
}

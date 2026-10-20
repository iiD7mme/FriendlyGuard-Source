package com.friendlyguard.mixin;

import com.friendlyguard.config.FriendlyGuardConfig;
import com.friendlyguard.glow.FriendGlowManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Inject(method = "getTeamColorValue", at = @At("HEAD"), cancellable = true)
    private void onGetTeamColorValue(CallbackInfoReturnable<Integer> cir) {
        Entity self = (Entity) (Object) this;
        if (self instanceof PlayerEntity player) {
            FriendlyGuardConfig config = FriendlyGuardConfig.getInstance();
            if (config.isProtectionEnabled() && config.isFriendGlowEnabled() && config.isProtected(player.getGameProfile().getName())) {
                cir.setReturnValue(FriendGlowManager.getGlowColor(self));
            }
        }
    }
}

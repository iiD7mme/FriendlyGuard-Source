package com.friendlyguard.glow;

import com.friendlyguard.config.FriendlyGuardConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FriendGlowManager {
    private static final Map<UUID, VisibilityCache> CACHE = new ConcurrentHashMap<>();

    private static class VisibilityCache {
        final long time;
        final boolean visible;

        VisibilityCache(long time, boolean visible) {
            this.time = time;
            this.visible = visible;
        }
    }

    /**
     * Checks if the entity is an enabled protected friend and is directly visible (not behind a wall).
     */
    public static boolean shouldGlow(Entity entity) {
        if (!(entity instanceof PlayerEntity targetPlayer)) {
            return false;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || targetPlayer == client.player) {
            return false;
        }

        FriendlyGuardConfig config = FriendlyGuardConfig.getInstance();
        if (!config.isProtectionEnabled() || !config.isFriendGlowEnabled()) {
            return false;
        }

        String targetName = targetPlayer.getGameProfile().getName();
        if (!config.isProtected(targetName)) {
            return false;
        }

        // Friend is protected and enabled.
        // Wall check: ensure the glow is NOT visible through walls
        return isVisibleNotBehindWall(client.player, targetPlayer);
    }

    /**
     * Raycasts from observer eyes to target points (head, chest, feet).
     * If all points hit solid blocks, returns false (behind wall).
     */
    public static boolean isVisibleNotBehindWall(PlayerEntity observer, PlayerEntity target) {
        if (observer == null || target == null || observer.getWorld() != target.getWorld()) {
            return false;
        }

        // Beyond 64 blocks, do not outline
        double distSq = observer.squaredDistanceTo(target);
        if (distSq > 64.0 * 64.0) {
            return false;
        }

        long now = System.currentTimeMillis();
        UUID targetId = target.getUuid();
        VisibilityCache cached = CACHE.get(targetId);
        if (cached != null && (now - cached.time) < 40) {
            return cached.visible;
        }

        boolean visible = checkLineOfSight(observer, target);
        CACHE.put(targetId, new VisibilityCache(now, visible));
        return visible;
    }

    private static boolean checkLineOfSight(PlayerEntity observer, PlayerEntity target) {
        World world = observer.getWorld();
        Vec3d eyePos = observer.getEyePos();

        // 1. Check target eyes (head)
        Vec3d targetEyes = target.getEyePos();
        if (isRayClear(world, observer, eyePos, targetEyes)) {
            return true;
        }

        // 2. Check target chest (mid-body)
        Vec3d targetChest = new Vec3d(target.getX(), target.getY() + target.getHeight() * 0.5, target.getZ());
        if (isRayClear(world, observer, eyePos, targetChest)) {
            return true;
        }

        // 3. Check target feet
        Vec3d targetFeet = new Vec3d(target.getX(), target.getY() + 0.15, target.getZ());
        if (isRayClear(world, observer, eyePos, targetFeet)) {
            return true;
        }

        // All points blocked by blocks/walls
        return false;
    }

    private static boolean isRayClear(World world, PlayerEntity observer, Vec3d start, Vec3d end) {
        BlockHitResult hit = world.raycast(new RaycastContext(
            start,
            end,
            RaycastContext.ShapeType.VISUAL,
            RaycastContext.FluidHandling.NONE,
            observer
        ));
        return hit.getType() == HitResult.Type.MISS;
    }

    public static int getGlowColor(Entity entity) {
        FriendlyGuardConfig config = FriendlyGuardConfig.getInstance();
        if (entity instanceof PlayerEntity player) {
            String name = player.getGameProfile().getName();
            return config.getPlayerGlowColorValue(name);
        }
        return config.getGlowColorValue();
    }
}

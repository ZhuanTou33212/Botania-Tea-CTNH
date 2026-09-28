package com.qianxin.botaniatea.entity;

import com.qianxin.botaniatea.registry.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/** A short-lived, server-authoritative endpoint of a two-way shadow portal. */
public final class ShadowPortalEntity extends Entity {
    private static final String PORTAL_COOLDOWN = "BotaniaTeaShadowPortalCooldown";
    private Vec3 destination = Vec3.ZERO;
    private UUID ownerId;
    private int remainingTicks = 600;

    public ShadowPortalEntity(EntityType<? extends ShadowPortalEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public ShadowPortalEntity(Level level, Vec3 position, Vec3 destination, UUID ownerId, float yaw) {
        this(ModEntities.SHADOW_PORTAL.get(), level);
        this.destination = destination;
        this.ownerId = ownerId;
        setPos(position);
        setYRot(yaw);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel server)) return;
        if (--remainingTicks <= 0) {
            discard();
            return;
        }
        long now = server.getGameTime();
        for (ServerPlayer player : server.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(.2D),
                player -> player.isAlive() && !player.isSpectator())) {
            if (player.getPersistentData().getLong(PORTAL_COOLDOWN) > now || !safeDestination(player, destination)) continue;
            player.getPersistentData().putLong(PORTAL_COOLDOWN, now + 20);
            player.teleportTo(destination.x, destination.y, destination.z);
            player.fallDistance = 0;
        }
    }

    private static boolean safeDestination(ServerPlayer player, Vec3 destination) {
        var level = player.serverLevel();
        var pos = net.minecraft.core.BlockPos.containing(destination);
        if (!level.hasChunkAt(pos) || !level.getWorldBorder().isWithinBounds(pos)) return false;
        AABB moved = player.getBoundingBox().move(destination.subtract(player.position()));
        return moved.minY >= level.getMinBuildHeight() && moved.maxY <= level.getMaxBuildHeight()
                && level.getWorldBorder().isWithinBounds(moved) && level.noCollision(player, moved)
                && !level.containsAnyLiquid(moved);
    }

    @Override protected void defineSynchedData() {}

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        destination = new Vec3(tag.getDouble("TargetX"), tag.getDouble("TargetY"), tag.getDouble("TargetZ"));
        ownerId = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        remainingTicks = Math.max(0, Math.min(600, tag.getInt("Lifetime")));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putDouble("TargetX", destination.x); tag.putDouble("TargetY", destination.y); tag.putDouble("TargetZ", destination.z);
        if (ownerId != null) tag.putUUID("Owner", ownerId);
        tag.putInt("Lifetime", remainingTicks);
    }

    @Override public boolean isPickable() { return false; }
    @Override public boolean isAttackable() { return false; }
}

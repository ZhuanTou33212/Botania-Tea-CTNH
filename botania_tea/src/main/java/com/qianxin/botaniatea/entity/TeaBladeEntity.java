package com.qianxin.botaniatea.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** Visible, dodgeable tea blades stop at blocks; no fire or terrain destruction. */
public final class TeaBladeEntity extends ThrowableItemProjectile {
    private int remainingTicks=80;
    private float damage=10;
    public void setDamage(float amount) { damage=amount; }
    public TeaBladeEntity(EntityType<? extends TeaBladeEntity> type, Level level) { super(type, level); setNoGravity(true); }
    @Override protected Item getDefaultItem() { return com.qianxin.teaart.registry.ModItems.GREEN_TEA_LEAF.get(); }
    @Override protected void onHitEntity(EntityHitResult hit) {
        if (!level().isClientSide && hit.getEntity() instanceof net.minecraft.world.entity.player.Player player)
            player.hurt(damageSources().thrown(this, getOwner()), damage);
    }
    @Override protected void onHit(HitResult hit) { super.onHit(hit); if (!level().isClientSide) discard(); }
    @Override public void tick() { super.tick(); if (!level().isClientSide && --remainingTicks <= 0) discard(); }
    @Override public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.addAdditionalSaveData(tag);tag.putInt("TeaLifetime",remainingTicks);tag.putFloat("TeaDamage",damage);
    }
    @Override public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        remainingTicks=tag.contains("TeaLifetime")?Math.max(0,Math.min(80,tag.getInt("TeaLifetime"))):80;
        damage=tag.contains("TeaDamage")?Math.max(0,Math.min(1000,tag.getFloat("TeaDamage"))):10;
    }
}

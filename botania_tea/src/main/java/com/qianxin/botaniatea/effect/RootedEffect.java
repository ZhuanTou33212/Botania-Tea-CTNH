package com.qianxin.botaniatea.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/** Movement lock only; it deliberately leaves look direction and attacks untouched. */
public final class RootedEffect extends MobEffect {
    public RootedEffect() { super(MobEffectCategory.HARMFUL, 0x6C4A8A); }
    @Override public void applyEffectTick(LivingEntity entity, int amplifier) {
        entity.setDeltaMovement(0.0D, Math.min(0.0D, entity.getDeltaMovement().y), 0.0D);
        entity.hurtMarked = true;
    }
    @Override public boolean isDurationEffectTick(int duration, int amplifier) { return true; }
}

package com.qianxin.botaniatea.mixin;

import com.qianxin.botaniatea.registry.ModEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Entity.class)
public abstract class RootedMovementMixin {
    @ModifyVariable(method="move", at=@At("HEAD"), argsOnly=true)
    private Vec3 lockMovement(Vec3 motion) {
        if ((Object)this instanceof LivingEntity living && living.hasEffect(ModEffects.ROOTED.get())) {
            living.setDeltaMovement(0,Math.min(0,living.getDeltaMovement().y),0);
            return new Vec3(0,Math.min(0,motion.y),0);
        }
        return motion;
    }
}

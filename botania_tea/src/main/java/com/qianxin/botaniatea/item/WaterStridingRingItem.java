package com.qianxin.botaniatea.item;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/** Curios ring: keeps the wearer on the water surface and prevents fall distance accumulation. */
public final class WaterStridingRingItem extends Item implements ICurioItem {
    public WaterStridingRingItem(Properties properties) { super(properties); }

    @Override
    public void curioTick(SlotContext context, ItemStack stack) {
        LivingEntity entity = context.entity();
        catchWaterSurface(entity);
    }

    /** Sweeps the complete downward step, not just the block currently under the player's feet. */
    public static boolean catchWaterSurface(LivingEntity entity) {
        if (entity.level().isClientSide || entity.isShiftKeyDown() || entity.getDeltaMovement().y > 0) return false;
        var current = entity.position();
        if (entity.yOld > entity.getY() + .001 && catchSegment(entity,
                new net.minecraft.world.phys.Vec3(entity.xOld, entity.yOld, entity.zOld), current)) return true;
        return catchSegment(entity, current.add(0,.001,0), current.add(entity.getDeltaMovement()).add(0,-.08,0));
    }

    private static boolean catchSegment(LivingEntity entity, net.minecraft.world.phys.Vec3 start,
                                        net.minecraft.world.phys.Vec3 end) {
        var level = entity.level();
        if (end.y >= start.y || start.distanceToSqr(end) > 4096D*4096D
                || !com.qianxin.botaniatea.entity.BambooGrapplingHookEntity.loadedRay(level,start,end)) return false;
        var hit=level.clip(new net.minecraft.world.level.ClipContext(start,end,
                net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.ANY,entity));
        if(hit.getType()!=net.minecraft.world.phys.HitResult.Type.BLOCK)return false;
        var pos=hit.getBlockPos();var fluid=level.getFluidState(pos);
        if(!fluid.is(net.minecraft.tags.FluidTags.WATER) || !level.getBlockState(pos).getCollisionShape(level,pos).isEmpty())return false;
        double surface=pos.getY()+fluid.getHeight(level,pos);
        if(start.y+.001<surface || end.y>surface)return false;
        var destination=new net.minecraft.world.phys.Vec3(hit.getLocation().x,surface,hit.getLocation().z);
        if(!level.noCollision(entity,entity.getBoundingBox().move(destination.subtract(entity.position()))))return false;
        var motion=entity.getDeltaMovement();entity.setPos(destination);
        entity.setDeltaMovement(motion.x,0,motion.z);entity.setOnGround(true);entity.fallDistance=0;entity.hurtMarked=true;
        return true;
    }
}

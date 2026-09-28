package com.qianxin.botaniatea.item;

import com.qianxin.botaniatea.entity.WaterSparkEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class WaterSparkItem extends Item {
    public WaterSparkItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        return place(level, pos, context.getPlayer(), context.getItemInHand())
                ? InteractionResult.sidedSuccess(level.isClientSide)
                : InteractionResult.FAIL;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        Vec3 desired = player.getEyePosition().add(player.getLookAngle().scale(2.0));
        BlockPos anchor = BlockPos.containing(desired.x, desired.y - 1.15, desired.z);
        return place(level, anchor, player, stack)
                ? InteractionResultHolder.sidedSuccess(stack, level.isClientSide)
                : InteractionResultHolder.fail(stack);
    }

    private static boolean place(Level level, BlockPos pos, Player player, ItemStack stack) {
        boolean occupied = !level.getEntitiesOfClass(WaterSparkEntity.class, new AABB(pos).inflate(0.75),
                spark -> spark.attachedPos().equals(pos)).isEmpty();
        if (occupied) {
            return false;
        }
        if (!level.isClientSide) {
            WaterSparkEntity spark = new WaterSparkEntity(level, pos);
            if (!level.addFreshEntity(spark)) {
                return false;
            }
            if (player == null || !player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return true;
    }
}

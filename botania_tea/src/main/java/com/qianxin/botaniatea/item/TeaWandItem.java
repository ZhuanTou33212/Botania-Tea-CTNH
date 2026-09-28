package com.qianxin.botaniatea.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;

/** Marker base for server-authoritative tea wands. Behaviour is supplied by WandEvents. */
public abstract class TeaWandItem extends Item {
    protected TeaWandItem(Properties properties) { super(properties); }
    public abstract String wandId();
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if ("ember_beam".equals(wandId())) {
            if (!level.isClientSide && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer && !com.qianxin.botaniatea.server.WandService.startBeam(stack,serverPlayer)) return InteractionResultHolder.fail(stack);
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
        if ("aftertaste_guard".equals(wandId())) {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
        if (!level.isClientSide && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)
            return com.qianxin.botaniatea.server.WandService.tryUse(stack, serverPlayer) ? InteractionResultHolder.consume(stack) : InteractionResultHolder.fail(stack);
        return InteractionResultHolder.success(stack);
    }
    @Override public int getUseDuration(ItemStack stack) { return 72000; }
    @Override public net.minecraft.world.item.UseAnim getUseAnimation(ItemStack stack) { return net.minecraft.world.item.UseAnim.BOW; }
    @Override public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        if (!level.isClientSide && entity instanceof net.minecraft.server.level.ServerPlayer player && "ember_beam".equals(wandId()))
            com.qianxin.botaniatea.server.WandService.updateBeam(stack, player);
    }
    @Override public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeCharged) {
        if (level.isClientSide || !(entity instanceof net.minecraft.server.level.ServerPlayer player)) return;
        if ("ember_beam".equals(wandId())) com.qianxin.botaniatea.server.WandService.stopBeam(player);
        else if ("aftertaste_guard".equals(wandId()))
            com.qianxin.botaniatea.server.WandService.useAftertasteGuard(stack, player, getUseDuration(stack) - timeCharged >= 10);
    }
}

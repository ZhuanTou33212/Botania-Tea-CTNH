package com.qianxin.botaniatea.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;

/** Shield-like porcelain guard; mana billing and the 120 degree cone are enforced by the Forge event hook. */
public final class PorcelainGuardItem extends net.minecraft.world.item.Item {
    public PorcelainGuardItem(Properties properties) { super(properties); }
    @Override public int getUseDuration(ItemStack stack) { return 72000; }
    // BLOCK would activate vanilla's free full-damage shield before the mana-priced damage hook.
    @Override public net.minecraft.world.item.UseAnim getUseAnimation(ItemStack stack) { return net.minecraft.world.item.UseAnim.NONE; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }
}

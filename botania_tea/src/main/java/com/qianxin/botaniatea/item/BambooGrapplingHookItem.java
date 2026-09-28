package com.qianxin.botaniatea.item;

import com.qianxin.botaniatea.entity.BambooGrapplingHookEntity;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class BambooGrapplingHookItem extends Item {
    public BambooGrapplingHookItem(Properties properties) { super(properties); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, net.minecraft.world.InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof net.minecraft.server.level.ServerLevel server && player.getPersistentData().hasUUID("TeaHookEntity")) {
            var active = server.getEntity(player.getPersistentData().getUUID("TeaHookEntity"));
            if (active instanceof BambooGrapplingHookEntity hook) { hook.cancel(player); return InteractionResultHolder.success(stack); }
            player.getPersistentData().remove("TeaHookEntity");
        }
        if (!level.isClientSide) {
            if (!player.getAbilities().instabuild
                    && !vazkii.botania.api.mana.ManaItemHandler.instance().requestManaExactForTool(stack, player, 10, false)) {
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.botania_tea.grapple_no_mana"), true);
                return InteractionResultHolder.fail(stack);
            }
            BambooGrapplingHookEntity hook = new BambooGrapplingHookEntity(level, player, player.getLookAngle());
            hook.setTool(stack);
            if (level.addFreshEntity(hook)) {
                player.getPersistentData().putUUID("TeaHookEntity", hook.getUUID());
                stack.getOrCreateTag().putUUID("TeaHookEntity", hook.getUUID());
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}

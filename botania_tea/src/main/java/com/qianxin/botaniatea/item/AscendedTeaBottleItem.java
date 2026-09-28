package com.qianxin.botaniatea.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.util.List;

public final class AscendedTeaBottleItem extends Item {
    public AscendedTeaBottleItem(Properties properties) {
        super(properties);
    }

    /** Returns fresh effect instances for cleansing-flower integrations. */
    public List<MobEffectInstance> createEffects() {
        return List.of(
                new MobEffectInstance(MobEffects.REGENERATION, 20 * 20, 1),
                new MobEffectInstance(MobEffects.ABSORPTION, 2 * 60 * 20, 3),
                new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 5 * 60 * 20),
                new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 5 * 60 * 20));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide) {
            for (MobEffectInstance effect : createEffects()) entity.addEffect(effect);
        }
        if (entity instanceof Player player) {
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
                ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
                if (stack.isEmpty()) return bottle;
                if (!player.getInventory().add(bottle)) player.drop(bottle, false);
            }
            return stack;
        }
        stack.shrink(1);
        return stack;
    }

    @Override public int getUseDuration(ItemStack stack) { return 32; }
    @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.DRINK; }
}

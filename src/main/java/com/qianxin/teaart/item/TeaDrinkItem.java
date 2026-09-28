package com.qianxin.teaart.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import com.qianxin.teaart.registry.ModItems;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class TeaDrinkItem extends Item {

    private final TeaEffect[] effects;

    public TeaDrinkItem(Properties properties, TeaEffect... effects) {
        super(properties);
        this.effects = effects;
    }

    /** Returns fresh effect instances for integrations such as cleansing flowers. */
    public List<MobEffectInstance> createEffects() {
        List<MobEffectInstance> result = new ArrayList<>(effects.length);
        for (TeaEffect effect : effects) {
            result.add(new MobEffectInstance(effect.effect().get(), effect.durationTicks(), effect.amplifier()));
        }
        return result;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide) {
            for (TeaEffect effect : effects) {
                entity.addEffect(
                        new MobEffectInstance(effect.effect().get(), effect.durationTicks(), effect.amplifier()));
            }
        }

        if (entity instanceof Player player) {
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
                ItemStack cup = new ItemStack(ModItems.TEA_CUP.get());
                if (stack.isEmpty()) {
                    return cup;
                }
                if (!player.getInventory().add(cup)) {
                    player.drop(cup, false);
                }
            }
            return stack;
        }

        stack.shrink(1);
        return stack;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 32;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    public record TeaEffect(Supplier<MobEffect> effect, int durationTicks, int amplifier) {}
}

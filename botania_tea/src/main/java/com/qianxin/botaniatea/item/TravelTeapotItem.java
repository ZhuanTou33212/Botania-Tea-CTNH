package com.qianxin.botaniatea.item;

import com.qianxin.teaart.content.TeaLiquor;
import com.qianxin.teaart.registry.ModTeaFluids;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidHandlerItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** A one-fluid, 4000 mB travel container. It deliberately rejects water and mixed fluids. */
public final class TravelTeapotItem extends Item {
    public static final int CAPACITY = 4000;
    public static final int DRINK_AMOUNT = 250;

    public TravelTeapotItem(Properties properties) {
        super(properties);
    }

    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new FluidHandlerItemStack(stack, CAPACITY) {
            @Override
            public boolean canFillFluidType(FluidStack fluid) {
                return isTeaLiquor(fluid.getFluid());
            }

            @Override
            public boolean canDrainFluidType(FluidStack fluid) {
                return isTeaLiquor(fluid.getFluid());
            }

            @Override
            public int fill(FluidStack resource, IFluidHandler.FluidAction action) {
                return canFillFluidType(resource) ? super.fill(resource, action) : 0;
            }

            @Override
            public FluidStack drain(FluidStack resource, IFluidHandler.FluidAction action) {
                return canDrainFluidType(resource) ? super.drain(resource, action) : FluidStack.EMPTY;
            }
        };
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        FluidStack stored = stored(stack);
        return stored.getAmount() >= DRINK_AMOUNT
                ? ItemUtils.startUsingInstantly(level, player, hand)
                : InteractionResultHolder.fail(stack);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        FluidStack stored = stored(stack);
        TeaLiquor liquor = liquor(stored.getFluid());
        if (!level.isClientSide && liquor != null && stored.getAmount() >= DRINK_AMOUNT) {
            for (MobEffectInstance effect : effects(liquor)) entity.addEffect(effect);
            FluidStack remaining = stored.copy();
            remaining.shrink(DRINK_AMOUNT);
            write(stack, remaining);
        }
        return stack;
    }

    @Override public int getUseDuration(ItemStack stack) { return 32; }
    @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.DRINK; }

    private static FluidStack stored(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(FluidHandlerItemStack.FLUID_NBT_KEY, 10)) return FluidStack.EMPTY;
        return FluidStack.loadFluidStackFromNBT(tag.getCompound(FluidHandlerItemStack.FLUID_NBT_KEY));
    }

    private static void write(ItemStack stack, FluidStack fluid) {
        CompoundTag tag = stack.getOrCreateTag();
        if (fluid.isEmpty()) tag.remove(FluidHandlerItemStack.FLUID_NBT_KEY);
        else tag.put(FluidHandlerItemStack.FLUID_NBT_KEY, fluid.writeToNBT(new CompoundTag()));
    }

    private static boolean isTeaLiquor(net.minecraft.world.level.material.Fluid fluid) {
        return liquor(fluid) != null;
    }

    @Nullable
    private static TeaLiquor liquor(net.minecraft.world.level.material.Fluid fluid) {
        for (TeaLiquor liquor : TeaLiquor.values()) {
            if (fluid == ModTeaFluids.SOURCES.get(liquor).get()
                    || fluid == ModTeaFluids.FLOWING.get(liquor).get()) return liquor;
        }
        return null;
    }

    private static List<MobEffectInstance> effects(TeaLiquor liquor) {
        Item tea = switch (liquor) {
            case GREEN -> com.qianxin.teaart.registry.ModItems.GREEN_TEA.get();
            case WHITE -> com.qianxin.teaart.registry.ModItems.WHITE_TEA.get();
            case YELLOW -> com.qianxin.teaart.registry.ModItems.YELLOW_TEA.get();
            case RED -> com.qianxin.teaart.registry.ModItems.RED_TEA.get();
            case OOLONG -> com.qianxin.teaart.registry.ModItems.OOLONG_TEA.get();
            case BLACK -> com.qianxin.teaart.registry.ModItems.BLACK_TEA.get();
            case JASMINE_SCENTED -> com.qianxin.teaart.registry.ModItems.JASMINE_FLOWER_TEA.get();
            case GARDENIA_SCENTED -> com.qianxin.teaart.registry.ModItems.GARDENIA_TEA.get();
        };
        return tea instanceof com.qianxin.teaart.item.TeaDrinkItem drink ? drink.createEffects() : List.of();
    }
}

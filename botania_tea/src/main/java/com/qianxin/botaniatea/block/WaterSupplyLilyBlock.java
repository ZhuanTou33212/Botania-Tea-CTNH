package com.qianxin.botaniatea.block;

import com.qianxin.botaniatea.block.entity.RegisteredFunctionalFlowerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import vazkii.botania.forge.block.ForgeSpecialFlowerBlock;

public final class WaterSupplyLilyBlock extends TeaSpecialFlowerBlock {
    public WaterSupplyLilyBlock(Properties properties) {
        super(net.minecraft.world.effect.MobEffects.WATER_BREATHING, 5, properties,
                () -> com.qianxin.botaniatea.registry.ModBlockEntities.REGISTERED_FUNCTIONAL_FLOWER.get());
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof RegisteredFunctionalFlowerBlockEntity flower)
                || !(player instanceof ServerPlayer serverPlayer)) {
            return level.isClientSide ? InteractionResult.SUCCESS : super.use(state, level, pos, player, hand, hit);
        }
        if (!flower.isSealed()) return InteractionResult.CONSUME;
        ItemStack sample = player.getItemInHand(hand).copyWithCount(1);
        var container = FluidUtil.getFluidHandler(sample).resolve().orElse(null);
        if (container == null) return super.use(state, level, pos, player, hand, hit);
        FluidStack content = container.getTanks() == 0 ? FluidStack.EMPTY : container.getFluidInTank(0);
        FluidStack drained = container.drain(1000, IFluidHandler.FluidAction.SIMULATE);
        if (content.isEmpty() || drained.getAmount() != 1000 || !drained.isFluidEqual(content)) {
            return InteractionResult.CONSUME;
        }
        if (container.drain(1000, IFluidHandler.FluidAction.EXECUTE).getAmount() != 1000) return InteractionResult.CONSUME;
        if (!flower.setConfiguredFluid(content)) return InteractionResult.CONSUME;
        if (!player.getAbilities().instabuild) {
            ItemStack held = player.getItemInHand(hand);
            held.shrink(1);
            ItemStack remainder = container.getContainer();
            if (held.isEmpty()) player.setItemInHand(hand, remainder);
            else if (!player.addItem(remainder)) player.drop(remainder, false);
        }
        flower.sync();
        return InteractionResult.CONSUME;
    }
}

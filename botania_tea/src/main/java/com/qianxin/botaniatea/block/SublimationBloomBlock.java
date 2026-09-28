package com.qianxin.botaniatea.block;

import com.qianxin.botaniatea.block.entity.SublimationBloomBlockEntity;
import com.qianxin.botaniatea.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.fluids.FluidUtil;
import vazkii.botania.forge.block.ForgeSpecialFlowerBlock;

public final class SublimationBloomBlock extends TeaSpecialFlowerBlock {
    public static final BooleanProperty IDLE = BooleanProperty.create("idle");

    public SublimationBloomBlock(Properties properties) {
        super(MobEffects.REGENERATION, 5, properties,
                () -> com.qianxin.botaniatea.registry.ModBlockEntities.SUBLIMATION_BLOOM.get());
        registerDefaultState(stateDefinition.any().setValue(IDLE, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(IDLE);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof SublimationBloomBlockEntity flower)) return InteractionResult.PASS;
        ItemStack held = player.getItemInHand(hand);
        if (held.is(ModItems.WATER_SPARK.get())) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (held.is(Items.GLASS_BOTTLE)) {
            ItemStack result = flower.extractBottle();
            if (!result.isEmpty()) exchange(player, hand, result);
            return InteractionResult.CONSUME;
        }
        if (player instanceof ServerPlayer serverPlayer
                && FluidUtil.interactWithFluidHandler(serverPlayer, hand, flower.fluidHandler())) {
            return InteractionResult.CONSUME;
        }
        return super.use(state, level, pos, player, hand, hit);
    }

    private static void exchange(Player player, InteractionHand hand, ItemStack result) {
        ItemStack held = player.getItemInHand(hand);
        held.shrink(1);
        if (held.isEmpty()) player.setItemInHand(hand, result);
        else if (!player.addItem(result)) player.drop(result, false);
    }
}

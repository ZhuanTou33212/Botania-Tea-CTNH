package com.qianxin.botaniatea.block;

import com.qianxin.botaniatea.block.entity.TeaEssenceDistillerBlockEntity;
import com.qianxin.botaniatea.item.EssenceBottleItem;
import com.qianxin.botaniatea.item.AscendedTeaBottleItem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import org.jetbrains.annotations.Nullable;

public final class TeaEssenceDistillerBlock extends BaseEntityBlock {
    public static final BooleanProperty WORKING = BooleanProperty.create("working");

    public TeaEssenceDistillerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(WORKING, false));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(WORKING);
    }

    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Nullable @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TeaEssenceDistillerBlockEntity(pos, state);
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type,
                com.qianxin.botaniatea.registry.ModBlockEntities.TEA_ESSENCE_DISTILLER.get(),
                TeaEssenceDistillerBlockEntity::tick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof TeaEssenceDistillerBlockEntity tank)) return InteractionResult.PASS;
        ItemStack held = player.getItemInHand(hand);
        if (level.isClientSide) {
            boolean fluidContainer = FluidUtil.getFluidHandler(held).isPresent();
            return held.getItem() instanceof EssenceBottleItem || held.getItem() instanceof AscendedTeaBottleItem || held.is(Items.GLASS_BOTTLE) || held.isEmpty() || fluidContainer
                    ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }

        if (held.getItem() instanceof EssenceBottleItem bottle) {
            if (tank.insertBottle(bottle.essence())) consumeAndGive(player, hand, Items.GLASS_BOTTLE.getDefaultInstance());
            return InteractionResult.CONSUME;
        }
        if (held.getItem() instanceof AscendedTeaBottleItem) {
            if (tank.insertAscendedBottle()) consumeAndGive(player, hand, Items.GLASS_BOTTLE.getDefaultInstance());
            return InteractionResult.CONSUME;
        }
        if (held.is(Items.GLASS_BOTTLE)) {
            ItemStack filled = tank.extractBottle();
            if (!filled.isEmpty()) consumeAndGive(player, hand, filled);
            return InteractionResult.CONSUME;
        }
        if (held.isEmpty() && player.isShiftKeyDown()) {
            if (player instanceof ServerPlayer serverPlayer) net.minecraftforge.network.NetworkHooks.openScreen(serverPlayer, tank, pos);
            return InteractionResult.CONSUME;
        }
        if (player instanceof ServerPlayer serverPlayer && FluidUtil.interactWithFluidHandler(serverPlayer, hand, tank.fluidHandler())) {
            return InteractionResult.CONSUME;
        }
        if (held.isEmpty()) {
            ItemStack output = tank.takeOutput();
            if (!output.isEmpty() && !player.addItem(output)) player.drop(output, false);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    private static void consumeAndGive(Player player, InteractionHand hand, ItemStack result) {
        ItemStack held = player.getItemInHand(hand);
        held.shrink(1);
        if (held.isEmpty()) player.setItemInHand(hand, result);
        else if (!player.addItem(result)) player.drop(result, false);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
    }
}

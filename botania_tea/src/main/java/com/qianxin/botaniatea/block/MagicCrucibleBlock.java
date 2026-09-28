package com.qianxin.botaniatea.block;

import com.qianxin.botaniatea.block.entity.MagicCrucibleBlockEntity;
import com.qianxin.botaniatea.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

public final class MagicCrucibleBlock extends BaseEntityBlock {
    public MagicCrucibleBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)
                .setValue(BlockStateProperties.LIT, false));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.HORIZONTAL_FACING, BlockStateProperties.LIT);
    }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Nullable @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new MagicCrucibleBlockEntity(pos, state); }
    @Nullable @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.MAGIC_CRUCIBLE.get(), MagicCrucibleBlockEntity::tick);
    }
    @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        return defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, context.getHorizontalDirection().getOpposite());
    }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof MagicCrucibleBlockEntity crucible)) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;

        if (net.minecraftforge.fluids.FluidUtil.interactWithFluidHandler(player, hand, level, pos, hit.getDirection())) return InteractionResult.CONSUME;
        ItemStack held=player.getItemInHand(hand);
        if (held.isEmpty()) {
            var handler=crucible.getItemHandler();
            for(int slot=14;slot<handler.getSlots();slot++) {
                ItemStack result=handler.extractItem(slot,64,false);
                if(!result.isEmpty()) { if(!player.getInventory().add(result)) player.drop(result,false); break; }
            }
        } else {
            ItemStack remainder=held.copy();
            for(int slot=0;slot<14 && !remainder.isEmpty();slot++) remainder=crucible.getItemHandler().insertItem(slot,remainder,false);
            player.setItemInHand(hand,remainder);
        }
        return InteractionResult.CONSUME;
    }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
    }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean isMoving) {
        if (!state.is(replacement.getBlock()) && level.getBlockEntity(pos) instanceof MagicCrucibleBlockEntity crucible) crucible.dropInventory();
        super.onRemove(state, level, pos, replacement, isMoving);
    }
}

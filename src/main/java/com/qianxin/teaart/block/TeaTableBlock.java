package com.qianxin.teaart.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;

import com.qianxin.teaart.block.entity.TeaTableBlockEntity;
import com.qianxin.teaart.registry.ModBlockEntities;
import org.jetbrains.annotations.Nullable;

public class TeaTableBlock extends HorizontalDirectionalBlock implements EntityBlock {

    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(1.0D, 0.0D, 1.0D, 15.0D, 1.0D, 15.0D),
            Block.box(1.0D, 1.0D, 1.0D, 15.0D, 3.0D, 2.0D),
            Block.box(1.0D, 1.0D, 14.0D, 15.0D, 3.0D, 15.0D),
            Block.box(1.0D, 1.0D, 2.0D, 2.0D, 3.0D, 14.0D),
            Block.box(14.0D, 1.0D, 2.0D, 15.0D, 3.0D, 14.0D),
            Block.box(6.0D, 1.0D, 5.0D, 10.0D, 5.0D, 9.0D),
            Block.box(4.0D, 1.0D, 10.0D, 6.0D, 3.0D, 12.0D),
            Block.box(10.0D, 1.0D, 10.0D, 12.0D, 3.0D, 12.0D));

    public TeaTableBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TeaTableBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        return type == ModBlockEntities.TEA_TABLE.get() ?
                (tickingLevel, tickingPos, tickingState, blockEntity) -> TeaTableBlockEntity.tick(tickingLevel,
                        tickingPos, tickingState, (TeaTableBlockEntity) blockEntity) :
                null;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                 BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof TeaTableBlockEntity table)) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide) {
            NetworkHooks.openScreen((net.minecraft.server.level.ServerPlayer) player, table, pos);
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public void destroy(LevelAccessor level, BlockPos pos, BlockState state) {
        if (level instanceof ServerLevel serverLevel &&
                level.getBlockEntity(pos) instanceof TeaTableBlockEntity table) {
            table.dropContents(serverLevel);
        }
        super.destroy(level, pos, state);
    }
}

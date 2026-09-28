package com.qianxin.teaart.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;

import com.qianxin.teaart.block.entity.BambooPipeOutputPortBlockEntity;
import com.qianxin.teaart.registry.ModItems;
import org.jetbrains.annotations.Nullable;

public class BambooPipeOutputPortBlock extends DirectionalBlock implements EntityBlock {

    private static final VoxelShape DOWN_SHAPE = Shapes.or(
            Block.box(2.0D, 0.0D, 2.0D, 14.0D, 3.0D, 14.0D),
            Block.box(4.0D, 3.0D, 4.0D, 12.0D, 4.0D, 12.0D),
            Block.box(5.75D, 7.0D, 5.75D, 10.25D, 9.0D, 10.25D),
            Block.box(6.0D, 4.0D, 6.0D, 10.0D, 16.0D, 10.0D));
    private static final VoxelShape UP_SHAPE = Shapes.or(
            Block.box(2.0D, 13.0D, 2.0D, 14.0D, 16.0D, 14.0D),
            Block.box(4.0D, 12.0D, 4.0D, 12.0D, 13.0D, 12.0D),
            Block.box(5.75D, 7.0D, 5.75D, 10.25D, 9.0D, 10.25D),
            Block.box(6.0D, 0.0D, 6.0D, 10.0D, 12.0D, 10.0D));
    private static final VoxelShape NORTH_SHAPE = Shapes.or(
            Block.box(2.0D, 2.0D, 0.0D, 14.0D, 14.0D, 3.0D),
            Block.box(4.0D, 4.0D, 3.0D, 12.0D, 12.0D, 4.0D),
            Block.box(5.75D, 5.75D, 7.0D, 10.25D, 10.25D, 9.0D),
            Block.box(6.0D, 6.0D, 4.0D, 10.0D, 10.0D, 16.0D));
    private static final VoxelShape SOUTH_SHAPE = Shapes.or(
            Block.box(2.0D, 2.0D, 13.0D, 14.0D, 14.0D, 16.0D),
            Block.box(4.0D, 4.0D, 12.0D, 12.0D, 12.0D, 13.0D),
            Block.box(5.75D, 5.75D, 7.0D, 10.25D, 10.25D, 9.0D),
            Block.box(6.0D, 6.0D, 0.0D, 10.0D, 10.0D, 12.0D));
    private static final VoxelShape WEST_SHAPE = Shapes.or(
            Block.box(0.0D, 2.0D, 2.0D, 3.0D, 14.0D, 14.0D),
            Block.box(3.0D, 4.0D, 4.0D, 4.0D, 12.0D, 12.0D),
            Block.box(7.0D, 5.75D, 5.75D, 9.0D, 10.25D, 10.25D),
            Block.box(4.0D, 6.0D, 6.0D, 16.0D, 10.0D, 10.0D));
    private static final VoxelShape EAST_SHAPE = Shapes.or(
            Block.box(13.0D, 2.0D, 2.0D, 16.0D, 14.0D, 14.0D),
            Block.box(12.0D, 4.0D, 4.0D, 13.0D, 12.0D, 12.0D),
            Block.box(7.0D, 5.75D, 5.75D, 9.0D, 10.25D, 10.25D),
            Block.box(0.0D, 6.0D, 6.0D, 12.0D, 10.0D, 10.0D));

    public BambooPipeOutputPortBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return BambooPipePortConnections.align(
                defaultBlockState().setValue(FACING, context.getClickedFace().getOpposite()),
                context.getLevel(), context.getClickedPos());
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level,
                                  BlockPos currentPos, BlockPos neighborPos) {
        return BambooPipePortConnections.align(state, level, currentPos);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos,
                                boolean moving) {
        if (!level.isClientSide) {
            BlockState aligned = BambooPipePortConnections.align(state, level, pos);
            if (aligned != state) {
                level.setBlock(pos, aligned, Block.UPDATE_ALL);
            }
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                 BlockHitResult hit) {
        if (player.getItemInHand(hand).is(ModItems.BAMBOO_WRENCH.get())) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof BambooPipeOutputPortBlockEntity port) {
            NetworkHooks.openScreen((ServerPlayer) player, port, pos);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeFor(state.getValue(FACING));
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeFor(state.getValue(FACING));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BambooPipeOutputPortBlockEntity(pos, state);
    }

    private static VoxelShape shapeFor(Direction direction) {
        return switch (direction) {
            case DOWN -> DOWN_SHAPE;
            case UP -> UP_SHAPE;
            case NORTH -> NORTH_SHAPE;
            case SOUTH -> SOUTH_SHAPE;
            case WEST -> WEST_SHAPE;
            case EAST -> EAST_SHAPE;
        };
    }
}

package com.qianxin.teaart.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.qianxin.teaart.registry.ModBlocks;
import com.qianxin.teaart.registry.ModItems;
import org.jetbrains.annotations.Nullable;

public class BambooPipeRedstonePortBlock extends DirectionalBlock {

    public static final BooleanProperty POWERED = BooleanProperty.create("powered");
    public static final BooleanProperty INVERTED = BooleanProperty.create("inverted");

    private static final VoxelShape DOWN_SHAPE = Shapes.or(
            Block.box(4.0D, 0.0D, 4.0D, 12.0D, 1.0D, 12.0D),
            Block.box(6.0D, 1.0D, 6.0D, 10.0D, 16.0D, 10.0D),
            Block.box(5.75D, 7.0D, 5.75D, 10.25D, 9.0D, 10.25D),
            Block.box(7.0D, 7.0D, 3.0D, 9.0D, 8.0D, 6.0D));
    private static final VoxelShape UP_SHAPE = Shapes.or(
            Block.box(4.0D, 15.0D, 4.0D, 12.0D, 16.0D, 12.0D),
            Block.box(6.0D, 0.0D, 6.0D, 10.0D, 15.0D, 10.0D),
            Block.box(5.75D, 7.0D, 5.75D, 10.25D, 9.0D, 10.25D),
            Block.box(7.0D, 8.0D, 10.0D, 9.0D, 9.0D, 13.0D));
    private static final VoxelShape NORTH_SHAPE = Shapes.or(
            Block.box(4.0D, 4.0D, 0.0D, 12.0D, 12.0D, 1.0D),
            Block.box(6.0D, 6.0D, 1.0D, 10.0D, 10.0D, 16.0D),
            Block.box(5.75D, 5.75D, 7.0D, 10.25D, 10.25D, 9.0D),
            Block.box(7.0D, 10.0D, 7.0D, 9.0D, 13.0D, 8.0D));
    private static final VoxelShape SOUTH_SHAPE = Shapes.or(
            Block.box(4.0D, 4.0D, 15.0D, 12.0D, 12.0D, 16.0D),
            Block.box(6.0D, 6.0D, 0.0D, 10.0D, 10.0D, 15.0D),
            Block.box(5.75D, 5.75D, 7.0D, 10.25D, 10.25D, 9.0D),
            Block.box(7.0D, 10.0D, 8.0D, 9.0D, 13.0D, 9.0D));
    private static final VoxelShape WEST_SHAPE = Shapes.or(
            Block.box(0.0D, 4.0D, 4.0D, 1.0D, 12.0D, 12.0D),
            Block.box(1.0D, 6.0D, 6.0D, 16.0D, 10.0D, 10.0D),
            Block.box(7.0D, 5.75D, 5.75D, 9.0D, 10.25D, 10.25D),
            Block.box(7.0D, 10.0D, 7.0D, 8.0D, 13.0D, 9.0D));
    private static final VoxelShape EAST_SHAPE = Shapes.or(
            Block.box(15.0D, 4.0D, 4.0D, 16.0D, 12.0D, 12.0D),
            Block.box(0.0D, 6.0D, 6.0D, 15.0D, 10.0D, 10.0D),
            Block.box(7.0D, 5.75D, 5.75D, 9.0D, 10.25D, 10.25D),
            Block.box(8.0D, 10.0D, 7.0D, 9.0D, 13.0D, 9.0D));

    public BambooPipeRedstonePortBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(POWERED, false)
                .setValue(INVERTED, false));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getClickedFace().getOpposite();
        BlockPos pos = context.getClickedPos();
        BlockState state = defaultBlockState()
                .setValue(FACING, facing)
                .setValue(POWERED, context.getLevel().hasNeighborSignal(pos));
        return canSurvive(state, context.getLevel(), pos) ? state : null;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                 BlockHitResult hit) {
        if (!player.getItemInHand(hand).isEmpty() || player.getItemInHand(hand).is(ModItems.BAMBOO_WRENCH.get())) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            boolean inverted = !state.getValue(INVERTED);
            level.setBlock(pos, state.setValue(INVERTED, inverted), Block.UPDATE_ALL);
            player.displayClientMessage(Component.translatable(
                    inverted ? "message.tea_art.redstone_port_inverted" : "message.tea_art.redstone_port_normal"),
                    true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        BlockState support = level.getBlockState(pos.relative(facing));
        if (support.is(ModBlocks.BAMBOO_PIPE.get())) {
            return true;
        }
        return (support.is(ModBlocks.BAMBOO_PIPE_INPUT_PORT.get()) ||
                support.is(ModBlocks.BAMBOO_PIPE_OUTPUT_PORT.get())) && support.hasProperty(FACING) &&
                support.getValue(FACING) == facing;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level,
                                  BlockPos currentPos, BlockPos neighborPos) {
        return !canSurvive(state, level, currentPos) ? net.minecraft.world.level.block.Blocks.AIR.defaultBlockState() :
                super.updateShape(state, direction, neighborState, level, currentPos, neighborPos);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos,
                                boolean moving) {
        boolean powered = level.hasNeighborSignal(pos);
        if (powered != state.getValue(POWERED)) {
            level.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_ALL);
        }
    }

    public static boolean isClosed(BlockState state) {
        return state.getValue(POWERED) != state.getValue(INVERTED);
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
        builder.add(FACING, POWERED, INVERTED);
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

package com.qianxin.teaart.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.qianxin.teaart.block.entity.BambooPipeBlockEntity;
import com.qianxin.teaart.registry.ModBlockEntities;
import com.qianxin.teaart.registry.ModBlocks;
import com.qianxin.teaart.registry.ModItems;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public class BambooPipeBlock extends Block implements EntityBlock {

    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty WEST = BooleanProperty.create("west");
    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final BooleanProperty DOWN = BooleanProperty.create("down");
    public static final EnumProperty<PipeColor> COLOR = EnumProperty.create("color", PipeColor.class);

    private static final VoxelShape CORE = Shapes.or(
            Block.box(6.0D, 6.0D, 6.0D, 10.0D, 10.0D, 10.0D),
            Block.box(5.75D, 5.75D, 7.0D, 10.25D, 10.25D, 9.0D));
    private static final VoxelShape NORTH_SHAPE = Shapes.or(
            Block.box(6.0D, 6.0D, 0.0D, 10.0D, 10.0D, 6.0D),
            Block.box(5.75D, 5.75D, 2.0D, 10.25D, 10.25D, 4.0D));
    private static final VoxelShape EAST_SHAPE = Shapes.or(
            Block.box(10.0D, 6.0D, 6.0D, 16.0D, 10.0D, 10.0D),
            Block.box(12.0D, 5.75D, 5.75D, 14.0D, 10.25D, 10.25D));
    private static final VoxelShape SOUTH_SHAPE = Shapes.or(
            Block.box(6.0D, 6.0D, 10.0D, 10.0D, 10.0D, 16.0D),
            Block.box(5.75D, 5.75D, 12.0D, 10.25D, 10.25D, 14.0D));
    private static final VoxelShape WEST_SHAPE = Shapes.or(
            Block.box(0.0D, 6.0D, 6.0D, 6.0D, 10.0D, 10.0D),
            Block.box(2.0D, 5.75D, 5.75D, 4.0D, 10.25D, 10.25D));
    private static final VoxelShape UP_SHAPE = Shapes.or(
            Block.box(6.0D, 10.0D, 6.0D, 10.0D, 16.0D, 10.0D),
            Block.box(5.75D, 12.0D, 5.75D, 10.25D, 14.0D, 10.25D));
    private static final VoxelShape DOWN_SHAPE = Shapes.or(
            Block.box(6.0D, 0.0D, 6.0D, 10.0D, 6.0D, 10.0D),
            Block.box(5.75D, 2.0D, 5.75D, 10.25D, 4.0D, 10.25D));

    public BambooPipeBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(NORTH, false)
                .setValue(EAST, false)
                .setValue(SOUTH, false)
                .setValue(WEST, false)
                .setValue(UP, false)
                .setValue(DOWN, false)
                .setValue(COLOR, PipeColor.NATURAL));
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                 BlockHitResult hit) {
        ItemStack heldStack = player.getItemInHand(hand);
        if (heldStack.is(ModItems.BAMBOO_WRENCH.get())) {
            if (player.isShiftKeyDown()) {
                return InteractionResult.PASS;
            }
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof BambooPipeBlockEntity pipe) {
                Direction direction = hit.getDirection();
                pipe.toggleConnection(direction);
                BlockState updatedState = updateConnections(state, level, pos);
                level.setBlock(pos, updatedState, Block.UPDATE_ALL);
                level.updateNeighborsAt(pos.relative(direction), this);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (!(heldStack.getItem() instanceof DyeItem dyeItem)) {
            return InteractionResult.PASS;
        }

        PipeColor color = PipeColor.fromDyeColor(dyeItem.getDyeColor());
        if (state.getValue(COLOR) == color) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (!level.isClientSide) {
            BlockState coloredState = updateConnections(state.setValue(COLOR, color), level, pos);
            level.setBlock(pos, coloredState, Block.UPDATE_ALL);
            if (!player.getAbilities().instabuild) {
                heldStack.shrink(1);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return updateConnections(this.defaultBlockState(), context.getLevel(), context.getClickedPos());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BambooPipeBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        return type == ModBlockEntities.BAMBOO_PIPE.get() ?
                (tickingLevel, tickingPos, tickingState, blockEntity) -> BambooPipeBlockEntity.tick(tickingLevel,
                        tickingPos, tickingState, (BambooPipeBlockEntity) blockEntity) :
                null;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level,
                                  BlockPos currentPos, BlockPos neighborPos) {
        return state.setValue(propertyFor(direction),
                canConnectTo(state, level, currentPos, direction, neighborPos, neighborState));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = CORE;
        if (state.getValue(NORTH)) {
            shape = Shapes.or(shape, NORTH_SHAPE);
        }
        if (state.getValue(EAST)) {
            shape = Shapes.or(shape, EAST_SHAPE);
        }
        if (state.getValue(SOUTH)) {
            shape = Shapes.or(shape, SOUTH_SHAPE);
        }
        if (state.getValue(WEST)) {
            shape = Shapes.or(shape, WEST_SHAPE);
        }
        if (state.getValue(UP)) {
            shape = Shapes.or(shape, UP_SHAPE);
        }
        if (state.getValue(DOWN)) {
            shape = Shapes.or(shape, DOWN_SHAPE);
        }
        return shape;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN, COLOR);
    }

    public BlockState updateConnections(BlockState state, LevelAccessor level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.relative(direction);
            state = state.setValue(propertyFor(direction),
                    canConnectTo(state, level, pos, direction, neighborPos, level.getBlockState(neighborPos)));
        }
        return state;
    }

    private boolean canConnectTo(BlockState state, LevelAccessor level, BlockPos pos, Direction direction,
                                 BlockPos neighborPos, BlockState neighborState) {
        if (level.getBlockEntity(pos) instanceof BambooPipeBlockEntity pipe && pipe.isConnectionDisabled(direction)) {
            return false;
        }
        if (neighborState.is(ModBlocks.BAMBOO_PIPE.get())) {
            if (level.getBlockEntity(neighborPos) instanceof BambooPipeBlockEntity neighborPipe &&
                    neighborPipe.isConnectionDisabled(direction.getOpposite())) {
                return false;
            }
            return neighborState.hasProperty(COLOR) && neighborState.getValue(COLOR) == state.getValue(COLOR);
        }
        if (neighborState.is(ModBlocks.BAMBOO_PIPE_OUTPUT_PORT.get()) ||
                neighborState.is(ModBlocks.BAMBOO_PIPE_INPUT_PORT.get())) {
            return neighborState.hasProperty(BambooPipeOutputPortBlock.FACING) &&
                    neighborState.getValue(BambooPipeOutputPortBlock.FACING) == direction;
        }
        if (neighborState.is(ModBlocks.BAMBOO_PIPE_REDSTONE_PORT.get())) {
            return neighborState.hasProperty(BambooPipeRedstonePortBlock.FACING) &&
                    (neighborState.getValue(BambooPipeRedstonePortBlock.FACING) == direction ||
                            neighborState.getValue(BambooPipeRedstonePortBlock.FACING).getOpposite() == direction);
        }
        return level.getBlockEntity(neighborPos) != null;
    }

    private static BooleanProperty propertyFor(Direction direction) {
        return switch (direction) {
            case NORTH -> NORTH;
            case EAST -> EAST;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            case UP -> UP;
            case DOWN -> DOWN;
        };
    }

    public enum PipeColor implements StringRepresentable {

        NATURAL("natural", null, 0xFFFFFF),
        WHITE("white", DyeColor.WHITE, 0xF9FFFE),
        ORANGE("orange", DyeColor.ORANGE, 0xF9801D),
        MAGENTA("magenta", DyeColor.MAGENTA, 0xC74EBD),
        LIGHT_BLUE("light_blue", DyeColor.LIGHT_BLUE, 0x3AB3DA),
        YELLOW("yellow", DyeColor.YELLOW, 0xFED83D),
        LIME("lime", DyeColor.LIME, 0x80C71F),
        PINK("pink", DyeColor.PINK, 0xF38BAA),
        GRAY("gray", DyeColor.GRAY, 0x474F52),
        LIGHT_GRAY("light_gray", DyeColor.LIGHT_GRAY, 0x9D9D97),
        CYAN("cyan", DyeColor.CYAN, 0x169C9C),
        PURPLE("purple", DyeColor.PURPLE, 0x8932B8),
        BLUE("blue", DyeColor.BLUE, 0x3C44AA),
        BROWN("brown", DyeColor.BROWN, 0x835432),
        GREEN("green", DyeColor.GREEN, 0x5E7C16),
        RED("red", DyeColor.RED, 0xB02E26),
        BLACK("black", DyeColor.BLACK, 0x1D1D21);

        private final String name;
        private final DyeColor dyeColor;
        private final int tintColor;

        PipeColor(String name, DyeColor dyeColor, int tintColor) {
            this.name = name;
            this.dyeColor = dyeColor;
            this.tintColor = tintColor;
        }

        public static PipeColor fromDyeColor(DyeColor dyeColor) {
            for (PipeColor pipeColor : values()) {
                if (pipeColor.dyeColor == dyeColor) {
                    return pipeColor;
                }
            }
            return NATURAL;
        }

        public int getTintColor() {
            return tintColor;
        }

        @Override
        public String getSerializedName() {
            return name.toLowerCase(Locale.ROOT);
        }
    }
}

package com.qianxin.teaart.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;

import com.qianxin.teaart.block.entity.ScentingBoxBlockEntity;
import com.qianxin.teaart.registry.ModBlockEntities;
import org.jetbrains.annotations.Nullable;

public class ScentingBoxBlock extends Block implements EntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    private static final VoxelShape LOWER_SHAPE = Shapes.or(
            Block.box(0.0D, 0.0D, 0.0D, 16.0D, 14.0D, 16.0D),
            Block.box(-1.0D, 14.0D, -1.0D, 17.0D, 16.0D, 17.0D),
            Block.box(-1.0D, 0.0D, -1.0D, 17.0D, 1.0D, 17.0D));
    private static final VoxelShape UPPER_SHAPE = Shapes.or(
            Block.box(3.0D, 1.0D, 3.0D, 13.0D, 11.0D, 13.0D),
            Block.box(2.0D, 11.0D, 2.0D, 14.0D, 12.0D, 14.0D),
            Block.box(2.25D, 0.0D, 2.25D, 13.75D, 2.0D, 13.75D),
            Block.box(3.0D, 12.0D, 3.0D, 13.0D, 13.0D, 13.0D));

    public ScentingBoxBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(HALF, DoubleBlockHalf.LOWER)
                .setValue(LIT, false));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos above = context.getClickedPos().above();
        if (!context.getLevel().getBlockState(above).canBeReplaced(context)) {
            return null;
        }
        return defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(HALF, DoubleBlockHalf.LOWER)
                .setValue(LIT, false);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer,
                            ItemStack stack) {
        level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), 3);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level,
                                  BlockPos pos, BlockPos neighborPos) {
        DoubleBlockHalf half = state.getValue(HALF);
        if (direction.getAxis() == Direction.Axis.Y && half == DoubleBlockHalf.LOWER == (direction == Direction.UP) &&
                (!neighborState.is(this) || neighborState.getValue(HALF) == half)) {
            return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ScentingBoxBlockEntity(pos, state);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? LOWER_SHAPE : UPPER_SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER && type == ModBlockEntities.SCENTING_BOX.get() ?
                (tickingLevel, tickingPos, tickingState, blockEntity) -> ScentingBoxBlockEntity.tick(tickingLevel,
                        tickingPos, tickingState, (ScentingBoxBlockEntity) blockEntity) :
                null;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                 BlockHitResult hit) {
        BlockPos entityPos = state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
        if (!level.isClientSide && level.getBlockEntity(entityPos) instanceof ScentingBoxBlockEntity box &&
                player instanceof ServerPlayer serverPlayer) {
            NetworkHooks.openScreen(serverPlayer, box, entityPos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void destroy(LevelAccessor level, BlockPos pos, BlockState state) {
        if (state.getValue(HALF) == DoubleBlockHalf.LOWER && level instanceof ServerLevel serverLevel &&
                level.getBlockEntity(pos) instanceof ScentingBoxBlockEntity box) {
            box.dropContents(serverLevel);
        }
        super.destroy(level, pos, state);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT) || state.getValue(HALF) != DoubleBlockHalf.LOWER) {
            return;
        }

        Direction flameSide = state.getValue(FACING);
        double x = pos.getX() + 0.5D + flameSide.getStepX() * 0.53D;
        double z = pos.getZ() + 0.5D + flameSide.getStepZ() * 0.53D;
        double sideJitterX = flameSide.getClockWise().getStepX() * (random.nextDouble() - 0.5D) * 0.35D;
        double sideJitterZ = flameSide.getClockWise().getStepZ() * (random.nextDouble() - 0.5D) * 0.35D;

        if (random.nextFloat() < 0.55F) {
            level.addParticle(ParticleTypes.FLAME,
                    x + sideJitterX, pos.getY() + 0.62D + random.nextDouble() * 0.16D, z + sideJitterZ,
                    0.0D, 0.015D, 0.0D);
        }
        if (random.nextFloat() < 0.2F) {
            level.addParticle(ParticleTypes.SMOKE,
                    x + sideJitterX * 0.5D, pos.getY() + 0.78D, z + sideJitterZ * 0.5D,
                    0.0D, 0.025D, 0.0D);
        }
        if (random.nextFloat() < 0.35F) {
            level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    pos.getX() + 0.5D + (random.nextDouble() - 0.5D) * 0.35D,
                    pos.getY() + 1.78D,
                    pos.getZ() + 0.5D + (random.nextDouble() - 0.5D) * 0.35D,
                    0.0D, 0.035D, 0.0D);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HALF, LIT);
    }
}

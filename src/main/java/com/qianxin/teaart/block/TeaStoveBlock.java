package com.qianxin.teaart.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.qianxin.teaart.block.entity.TeaStoveBlockEntity;
import com.qianxin.teaart.registry.ModBlockEntities;
import org.jetbrains.annotations.Nullable;

public class TeaStoveBlock extends HorizontalDirectionalBlock implements EntityBlock {

    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(2.0D, 0.0D, 2.0D, 14.0D, 11.0D, 14.0D),
            Block.box(2.0D, 0.0D, 1.0D, 5.0D, 11.0D, 2.0D),
            Block.box(11.0D, 0.0D, 1.0D, 14.0D, 11.0D, 2.0D),
            Block.box(5.0D, 0.0D, 1.0D, 11.0D, 2.0D, 2.0D),
            Block.box(5.0D, 7.0D, 1.0D, 11.0D, 11.0D, 2.0D),
            Block.box(5.0D, 2.0D, 2.0D, 11.0D, 7.0D, 4.0D),
            Block.box(4.0D, 11.0D, 4.0D, 12.0D, 13.0D, 12.0D),
            Block.box(6.0D, 13.0D, 6.0D, 10.0D, 16.0D, 10.0D));

    public TeaStoveBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
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
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
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
        return new TeaStoveBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        return type == ModBlockEntities.TEA_STOVE.get() ?
                (tickingLevel, tickingPos, tickingState, blockEntity) -> TeaStoveBlockEntity.tick(tickingLevel,
                        tickingPos, tickingState, (TeaStoveBlockEntity) blockEntity) :
                null;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                 BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof TeaStoveBlockEntity stove)) {
            return InteractionResult.PASS;
        }

        ItemStack held = player.getItemInHand(hand);
        if (!held.isEmpty() && stove.canCook(held)) {
            if (!level.isClientSide) {
                ItemStack input = held.copy();
                input.setCount(1);
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                }
                stove.addItem(input);
                level.playSound(null, pos, SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS, 0.8F, 1.0F);
            }
            return InteractionResult.SUCCESS;
        }

        if (held.isEmpty()) {
            if (!level.isClientSide) {
                ItemStack cooked = stove.takeFinished();
                if (!cooked.isEmpty() && !player.getInventory().add(cooked)) {
                    player.drop(cooked, false);
                }
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, net.minecraft.util.RandomSource random) {
        if (random.nextFloat() < 0.45F) {
            level.addParticle(ParticleTypes.FLAME,
                    pos.getX() + 0.5D + (random.nextDouble() - 0.5D) * 0.2D,
                    pos.getY() + 0.35D,
                    pos.getZ() + 0.16D,
                    0.0D, 0.01D, 0.0D);
        }
        if (random.nextFloat() < 0.35F) {
            level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    pos.getX() + 0.5D, pos.getY() + 1.05D, pos.getZ() + 0.5D,
                    0.0D, 0.03D, 0.0D);
        }
    }

    @Override
    public void destroy(LevelAccessor level, BlockPos pos, BlockState state) {
        if (level instanceof ServerLevel serverLevel &&
                level.getBlockEntity(pos) instanceof TeaStoveBlockEntity stove) {
            stove.dropContents(serverLevel);
        }
        super.destroy(level, pos, state);
    }
}

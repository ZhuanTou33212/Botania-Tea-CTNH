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

import com.qianxin.teaart.block.entity.KneadingTableBlockEntity;
import com.qianxin.teaart.registry.ModBlockEntities;
import com.qianxin.teaart.registry.ModItems;
import org.jetbrains.annotations.Nullable;

public class KneadingTableBlock extends HorizontalDirectionalBlock implements EntityBlock {

    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(1.0D, 9.0D, 1.0D, 15.0D, 10.0D, 15.0D),
            Block.box(1.0D, 10.0D, 1.0D, 15.0D, 12.0D, 3.0D),
            Block.box(1.0D, 10.0D, 13.0D, 15.0D, 12.0D, 15.0D),
            Block.box(1.0D, 10.0D, 3.0D, 3.0D, 12.0D, 13.0D),
            Block.box(13.0D, 10.0D, 3.0D, 15.0D, 12.0D, 13.0D),
            Block.box(3.0D, 10.0D, 3.0D, 13.0D, 11.0D, 13.0D),
            Block.box(2.0D, 0.0D, 2.0D, 4.0D, 9.0D, 4.0D),
            Block.box(12.0D, 0.0D, 2.0D, 14.0D, 9.0D, 4.0D),
            Block.box(2.0D, 0.0D, 12.0D, 4.0D, 9.0D, 14.0D),
            Block.box(12.0D, 0.0D, 12.0D, 14.0D, 9.0D, 14.0D));

    public KneadingTableBlock(Properties properties) {
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
        return new KneadingTableBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        return type == ModBlockEntities.KNEADING_TABLE.get() ?
                (tickingLevel, tickingPos, tickingState, blockEntity) -> KneadingTableBlockEntity.tick(tickingLevel,
                        tickingPos, tickingState, (KneadingTableBlockEntity) blockEntity) :
                null;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                 BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof KneadingTableBlockEntity table)) {
            return InteractionResult.PASS;
        }

        ItemStack held = player.getItemInHand(hand);
        if (held.is(ModItems.TEA_LEAF.get()) && table.canPlaceTea(held)) {
            if (!level.isClientSide) {
                ItemStack input = held.copy();
                input.setCount(1);
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                }
                table.placeTea(input);
                level.playSound(null, pos, SoundEvents.GRASS_PLACE, SoundSource.BLOCKS, 0.8F, 0.9F);
            }
            return InteractionResult.SUCCESS;
        }

        if (held.isEmpty() && table.hasTea()) {
            if (!level.isClientSide) {
                table.knead();
                ((ServerLevel) level).sendParticles(ParticleTypes.COMPOSTER,
                        pos.getX() + 0.5D, pos.getY() + 1.05D, pos.getZ() + 0.5D,
                        8, 0.25D, 0.05D, 0.25D, 0.02D);
                level.playSound(null, pos, SoundEvents.WOOL_HIT, SoundSource.BLOCKS, 0.8F,
                        0.8F + level.random.nextFloat() * 0.3F);
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    @Override
    public void destroy(LevelAccessor level, BlockPos pos, BlockState state) {
        if (level instanceof ServerLevel serverLevel &&
                level.getBlockEntity(pos) instanceof KneadingTableBlockEntity table) {
            table.dropContents(serverLevel);
        }
        super.destroy(level, pos, state);
    }
}

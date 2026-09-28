package com.qianxin.teaart.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.qianxin.teaart.block.entity.TeaSieveRackBlockEntity;
import com.qianxin.teaart.registry.ModBlockEntities;
import com.qianxin.teaart.registry.ModItems;
import org.jetbrains.annotations.Nullable;

public class TeaSieveRackBlock extends HorizontalDirectionalBlock implements EntityBlock {

    public static final IntegerProperty SIEVES = IntegerProperty.create("sieves", 0, 3);
    private static final VoxelShape RACK_SHAPE = Shapes.or(
            Block.box(0.0D, 0.0D, 0.0D, 2.0D, 18.0D, 2.0D),
            Block.box(14.0D, 0.0D, 0.0D, 16.0D, 18.0D, 2.0D),
            Block.box(0.0D, 0.0D, 14.0D, 2.0D, 18.0D, 16.0D),
            Block.box(14.0D, 0.0D, 14.0D, 16.0D, 18.0D, 16.0D),
            Block.box(2.0D, 1.0D, 1.0D, 14.0D, 2.0D, 2.0D),
            Block.box(2.0D, 1.0D, 14.0D, 14.0D, 2.0D, 15.0D),
            Block.box(1.0D, 1.0D, 2.0D, 2.0D, 2.0D, 14.0D),
            Block.box(14.0D, 1.0D, 2.0D, 15.0D, 2.0D, 14.0D),
            Block.box(2.0D, 7.0D, 1.0D, 14.0D, 8.0D, 2.0D),
            Block.box(2.0D, 7.0D, 14.0D, 14.0D, 8.0D, 15.0D),
            Block.box(1.0D, 7.0D, 2.0D, 2.0D, 8.0D, 14.0D),
            Block.box(14.0D, 7.0D, 2.0D, 15.0D, 8.0D, 14.0D),
            Block.box(2.0D, 14.0D, 1.0D, 14.0D, 15.0D, 2.0D),
            Block.box(2.0D, 14.0D, 14.0D, 14.0D, 15.0D, 15.0D),
            Block.box(1.0D, 14.0D, 2.0D, 2.0D, 15.0D, 14.0D),
            Block.box(14.0D, 14.0D, 2.0D, 15.0D, 15.0D, 14.0D));

    public static VoxelShape shapeFor(int sieves) {
        if (sieves <= 0) {
            return RACK_SHAPE;
        }
        int y = switch (sieves) {
            case 1 -> 2;
            case 2 -> 8;
            default -> 15;
        };
        return Shapes.or(RACK_SHAPE,
                Block.box(1.0D, y, 1.0D, 15.0D, y + 2.0D, 2.0D),
                Block.box(2.0D, y, 2.0D, 14.0D, y + 1.0D, 14.0D),
                Block.box(1.0D, y, 2.0D, 2.0D, y + 2.0D, 14.0D),
                Block.box(1.0D, y, 14.0D, 15.0D, y + 2.0D, 15.0D),
                Block.box(14.0D, y, 2.0D, 15.0D, y + 2.0D, 14.0D));
    }

    public TeaSieveRackBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(SIEVES, 0));
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
        builder.add(FACING, SIEVES);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeFor(state.getValue(SIEVES));
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeFor(state.getValue(SIEVES));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TeaSieveRackBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        return type == ModBlockEntities.TEA_SIEVE_RACK.get() ?
                (tickingLevel, tickingPos, tickingState, blockEntity) -> TeaSieveRackBlockEntity.tick(tickingLevel,
                        tickingPos, tickingState, (TeaSieveRackBlockEntity) blockEntity) :
                null;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                 BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof TeaSieveRackBlockEntity rack)) {
            return InteractionResult.PASS;
        }

        ItemStack held = player.getItemInHand(hand);
        int slot = rack.slotFromHit(hit.getLocation().x - pos.getX());

        if (held.is(ModItems.TEA_SIEVE.get())) {
            if (!level.isClientSide && rack.addSieve(slot)) {
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                }
                level.playSound(null, pos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 0.7F, 1.1F);
            }
            return InteractionResult.SUCCESS;
        }

        if (held.isEmpty() && player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                ItemStack removed = rack.removeSieve(slot);
                if (!removed.isEmpty()) {
                    giveOrDrop(player, removed);
                    level.playSound(null, pos, SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 0.7F, 1.1F);
                }
            }
            return InteractionResult.SUCCESS;
        }

        if (rack.canStartDrying(slot, held)) {
            if (!level.isClientSide && rack.startDrying(slot, held)) {
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                }
                level.playSound(null, pos, SoundEvents.GRASS_PLACE, SoundSource.BLOCKS, 0.7F, 1.2F);
            }
            return InteractionResult.SUCCESS;
        }

        if (held.isEmpty()) {
            if (!level.isClientSide) {
                ItemStack tea = rack.takeTea(slot);
                if (!tea.isEmpty()) {
                    giveOrDrop(player, tea);
                }
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    @Override
    public void destroy(LevelAccessor level, BlockPos pos, BlockState state) {
        if (level instanceof ServerLevel serverLevel &&
                level.getBlockEntity(pos) instanceof TeaSieveRackBlockEntity rack) {
            rack.dropContents(serverLevel);
        }
        super.destroy(level, pos, state);
    }

    private static void giveOrDrop(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }
}

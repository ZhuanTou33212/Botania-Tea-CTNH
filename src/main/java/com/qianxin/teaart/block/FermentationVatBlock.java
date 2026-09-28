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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.qianxin.teaart.block.entity.FermentationVatBlockEntity;
import com.qianxin.teaart.registry.ModBlockEntities;
import org.jetbrains.annotations.Nullable;

public class FermentationVatBlock extends HorizontalDirectionalBlock implements EntityBlock {

    public static final BooleanProperty HAS_TEA = BooleanProperty.create("has_tea");

    private static final VoxelShape EMPTY_SHAPE = Shapes.or(
            Block.box(1.0D, 0.0D, 1.0D, 15.0D, 1.0D, 15.0D),
            Block.box(1.0D, 1.0D, 0.0D, 15.0D, 16.0D, 1.0D),
            Block.box(1.0D, 1.0D, 15.0D, 15.0D, 16.0D, 16.0D),
            Block.box(0.0D, 1.0D, 1.0D, 1.0D, 16.0D, 15.0D),
            Block.box(15.0D, 1.0D, 1.0D, 16.0D, 16.0D, 15.0D));
    private static final VoxelShape FULL_SHAPE = Shapes.or(
            EMPTY_SHAPE,
            Block.box(1.0D, 0.0D, 1.0D, 15.0D, 14.0D, 15.0D),
            Block.box(3.0D, 2.0D, 3.0D, 13.0D, 16.0D, 13.0D),
            Block.box(5.0D, 8.0D, 5.0D, 11.0D, 17.0D, 11.0D));

    public FermentationVatBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(HAS_TEA, false));
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
        builder.add(FACING, HAS_TEA);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(HAS_TEA) ? FULL_SHAPE : EMPTY_SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(HAS_TEA) ? FULL_SHAPE : EMPTY_SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FermentationVatBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        return type == ModBlockEntities.FERMENTATION_VAT.get() ?
                (tickingLevel, tickingPos, tickingState, blockEntity) -> FermentationVatBlockEntity.tick(tickingLevel,
                        tickingPos, tickingState, (FermentationVatBlockEntity) blockEntity) :
                null;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                 BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof FermentationVatBlockEntity vat)) {
            return InteractionResult.PASS;
        }

        ItemStack held = player.getItemInHand(hand);
        if (vat.canInsertTea(held)) {
            if (!level.isClientSide) {
                ItemStack input = held.copy();
                input.setCount(1);
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                }
                vat.insertTea(input);
                level.playSound(null, pos, SoundEvents.GRASS_PLACE, SoundSource.BLOCKS, 0.8F, 0.7F);
            }
            return InteractionResult.SUCCESS;
        }

        if (held.isEmpty()) {
            if (!level.isClientSide) {
                ItemStack tea = vat.takeTea();
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
                level.getBlockEntity(pos) instanceof FermentationVatBlockEntity vat) {
            vat.dropContents(serverLevel);
        }
        super.destroy(level, pos, state);
    }

    private static void giveOrDrop(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }
}

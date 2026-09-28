package com.qianxin.teaart.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.qianxin.teaart.registry.ModItems;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class TeaCupBlock extends Block {

    public static final IntegerProperty CUPS = IntegerProperty.create("cups", 1, 3);

    /** 单只茶杯的碰撞箱（4×4 底、4 高，杯口留空），按 x 偏移排列。 */
    private static VoxelShape cup(int ox) {
        return Shapes.or(
                Block.box(ox, 0.0D, 6.0D, ox + 4.0D, 1.0D, 10.0D),
                Block.box(ox, 1.0D, 6.0D, ox + 4.0D, 4.0D, 7.0D),
                Block.box(ox, 1.0D, 9.0D, ox + 4.0D, 4.0D, 10.0D),
                Block.box(ox, 1.0D, 7.0D, ox + 1.0D, 4.0D, 9.0D),
                Block.box(ox + 3.0D, 1.0D, 7.0D, ox + 4.0D, 4.0D, 9.0D));
    }

    private static final VoxelShape CUP_ONE = cup(6);
    private static final VoxelShape CUP_TWO = Shapes.or(cup(3), cup(9));
    private static final VoxelShape CUP_THREE = Shapes.or(cup(1), cup(6), cup(11));

    public TeaCupBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(CUPS, 1));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState clickedState = context.getLevel().getBlockState(context.getClickedPos());
        if (clickedState.is(this)) {
            return clickedState.setValue(CUPS, Math.min(3, clickedState.getValue(CUPS) + 1));
        }
        return context.getClickedFace() == Direction.UP &&
                canSurvive(defaultBlockState(), context.getLevel(), context.getClickedPos()) ? defaultBlockState() :
                        null;
    }

    @Override
    public boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        return context.getItemInHand().is(ModItems.TEA_CUP.get()) && state.getValue(CUPS) < 3;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (!player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            int cups = state.getValue(CUPS);
            if (cups > 1) {
                level.setBlock(pos, state.setValue(CUPS, cups - 1), Block.UPDATE_ALL);
            } else {
                level.removeBlock(pos, false);
            }
            ItemStack cup = new ItemStack(ModItems.TEA_CUP.get());
            if (!player.getInventory().add(cup)) {
                player.drop(cup, false);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return Block.canSupportCenter(level, pos.below(), Direction.UP);
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.DESTROY;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(CUPS)) {
            case 1 -> CUP_ONE;
            case 2 -> CUP_TWO;
            default -> CUP_THREE;
        };
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of(new ItemStack(ModItems.TEA_CUP.get(), state.getValue(CUPS)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CUPS);
    }
}

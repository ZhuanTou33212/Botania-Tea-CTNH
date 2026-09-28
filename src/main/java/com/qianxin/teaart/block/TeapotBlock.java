package com.qianxin.teaart.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.qianxin.teaart.block.entity.TeapotBlockEntity;
import com.qianxin.teaart.registry.ModBlockEntities;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

public class TeapotBlock extends Block implements EntityBlock {

    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(4.0D, 3.0D, 4.0D, 12.0D, 11.0D, 12.0D),
            Block.box(5.0D, 11.0D, 5.0D, 11.0D, 13.0D, 11.0D),
            Block.box(7.0D, 13.0D, 7.0D, 9.0D, 15.0D, 9.0D),
            Block.box(12.0D, 7.0D, 6.0D, 16.0D, 9.0D, 10.0D),
            Block.box(1.0D, 6.0D, 6.0D, 4.0D, 10.0D, 10.0D),
            Block.box(0.0D, 7.0D, 7.0D, 2.0D, 9.0D, 9.0D));

    private final Supplier<List<TeaBrew>> brews;

    public TeapotBlock(Properties properties, Supplier<List<TeaBrew>> brews) {
        super(properties);
        this.brews = brews;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TeapotBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> blockEntityType) {
        return blockEntityType == ModBlockEntities.TEAPOT.get() ?
                (tickingLevel, pos, tickingState, blockEntity) -> TeapotBlockEntity.tick(tickingLevel, pos,
                        tickingState, (TeapotBlockEntity) blockEntity) :
                null;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                 BlockHitResult hit) {
        return InteractionResult.PASS;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    private int getBrewIndex(ItemStack stack) {
        List<TeaBrew> teaBrews = brews.get();
        for (int i = 0; i < teaBrews.size(); i++) {
            if (stack.is(teaBrews.get(i).teaLeaf().get().asItem())) {
                return i;
            }
        }
        return -1;
    }

    private static void giveOrDrop(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    public record TeaBrew(Supplier<? extends ItemLike> teaLeaf, Supplier<? extends ItemLike> teaDrink) {}
}

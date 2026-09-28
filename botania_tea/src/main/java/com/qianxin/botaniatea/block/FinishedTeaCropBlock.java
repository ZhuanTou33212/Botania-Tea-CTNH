package com.qianxin.botaniatea.block;

import com.qianxin.teaart.block.TeaCropBlock;
import com.qianxin.teaart.content.FinishedTea;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import com.qianxin.botaniatea.registry.ModItems;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Uses the existing crop lifecycle; harvested products belong to tea_art. */
public final class FinishedTeaCropBlock extends TeaCropBlock {
    private final FinishedTea tea;

    public FinishedTeaCropBlock(FinishedTea tea, Properties properties) {
        super(properties);
        this.tea = tea;
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return tea.leaf();
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return new ItemStack(ModItems.FINISHED_TEA_TREES.get(tea).get());
    }

    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit) {
        if (!isMaxAge(state)) return InteractionResult.PASS;
        if (!level.isClientSide) {
            popResource(level, pos, new ItemStack(tea.leaf()));
            level.setBlock(pos, getStateForAge(0), 3);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}

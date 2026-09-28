package com.qianxin.botaniatea.block;

import com.qianxin.botaniatea.block.entity.TeaRitualTableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public final class TeaRitualTableBlock extends BaseEntityBlock {
    public TeaRitualTableBlock(Properties properties) {
        super(properties);
    }
    @Override public net.minecraft.world.level.block.RenderShape getRenderShape(BlockState state) {
        return net.minecraft.world.level.block.RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TeaRitualTableBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return null;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof TeaRitualTableBlockEntity table)) return InteractionResult.PASS;
        return player.getItemInHand(hand).isEmpty()
                ? (table.trySummon(player) ? InteractionResult.sidedSuccess(level.isClientSide) : InteractionResult.PASS)
                : (table.insertFromPlayer(player, hand) ? InteractionResult.sidedSuccess(level.isClientSide)
                : InteractionResult.PASS);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (state.getBlock() != replacement.getBlock() && level.getBlockEntity(pos) instanceof TeaRitualTableBlockEntity table) {
            table.dropContents();
        }
        super.onRemove(state, level, pos, replacement, moving);
    }
}

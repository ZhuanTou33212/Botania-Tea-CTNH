package com.qianxin.botaniatea.block;

import com.qianxin.botaniatea.block.entity.ManaOfferingBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class ManaOfferingBlock extends BaseEntityBlock {
    public ManaOfferingBlock(Properties properties) { super(properties); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new ManaOfferingBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(Level level, BlockState state, net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, com.qianxin.botaniatea.registry.ModBlockEntities.MANA_OFFERING.get(), com.qianxin.botaniatea.content.OfferingRitual::tick);
    }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof ManaOfferingBlockEntity table)) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        ItemStack held = player.getItemInHand(hand);
        boolean sneak = player.isShiftKeyDown();
        if (held.isEmpty()) {
            ItemStack removed = table.inventory.extractItem(0, sneak ? ManaOfferingBlockEntity.SLOT_LIMIT : 1, false);
            if (!removed.isEmpty() && !player.getInventory().add(removed)) player.drop(removed, false);
        } else {
            // Right click offers exactly one element; sneak-right-click offers the whole held stack.
            ItemStack toInsert = sneak ? held.copy() : held.copyWithCount(1);
            ItemStack remainder = table.inventory.insertItem(0, toInsert, false);
            if (sneak) player.setItemInHand(hand, remainder);
            else if (remainder.isEmpty()) {
                held.shrink(1);
                if (held.isEmpty()) player.setItemInHand(hand, ItemStack.EMPTY);
                else player.getInventory().setChanged();
            }
        }
        return InteractionResult.CONSUME;
    }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!state.is(replacement.getBlock()) && !level.isClientSide && level.getBlockEntity(pos) instanceof ManaOfferingBlockEntity table) {
            ItemStack stack = table.inventory.extractItem(0, 64, false);
            if (!stack.isEmpty()) level.addFreshEntity(new ItemEntity(level, pos.getX()+.5, pos.getY()+1, pos.getZ()+.5, stack));
        }
        super.onRemove(state, level, pos, replacement, moving);
    }
}

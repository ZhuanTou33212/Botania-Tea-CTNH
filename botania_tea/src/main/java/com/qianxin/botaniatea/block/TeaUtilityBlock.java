package com.qianxin.botaniatea.block;

import com.qianxin.botaniatea.block.entity.TeaUtilityBlockEntity;
import com.qianxin.botaniatea.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.fluids.FluidUtil;
import java.util.List;

public final class TeaUtilityBlock extends BaseEntityBlock {
    public TeaUtilityBlock(Properties properties) { super(properties); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new TeaUtilityBlockEntity(pos, state); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof TeaUtilityBlockEntity be)) return InteractionResult.PASS;
        if (state.is(ModBlocks.TEA_SEAT.get())) {
            return FluidUtil.interactWithFluidHandler(player, hand, be.tank()) ? InteractionResult.CONSUME : InteractionResult.PASS;
        }
        ItemStack held = player.getItemInHand(hand);
        if (held.isEmpty()) {
            player.setItemInHand(hand, be.inventory().extractItem(0, 1, false));
        } else if (be.inventory().insertItem(0, held.copyWithCount(1), true).isEmpty()) {
            be.inventory().insertItem(0, held.copyWithCount(1), false);
            if (!player.getAbilities().instabuild) held.shrink(1);
        }
        return InteractionResult.CONSUME;
    }
    @Override public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        var drops = super.getDrops(state, builder);
        var be = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (be != null) for (var drop : drops) if (drop.is(asItem())) {
            BlockItem.setBlockEntityData(drop, be.getType(), be.saveWithoutMetadata());
            break;
        }
        return drops;
    }
}

package com.qianxin.botaniatea.block;

import com.qianxin.botaniatea.block.entity.ElementTankBlockEntity;
import com.qianxin.botaniatea.content.ElementType;
import com.qianxin.botaniatea.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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

/** A single-fluid 8000 mB tank dedicated to the twenty elemental source essences. */
public final class ElementTankBlock extends BaseEntityBlock {
    public ElementTankBlock(Properties properties) { super(properties); }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ElementTankBlockEntity(pos,state);
    }

    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof ElementTankBlockEntity tank)) return InteractionResult.PASS;
        ItemStack held=player.getItemInHand(hand);
        ElementType crystal=ModItems.elementCrystalType(held);
        boolean fluidContainer=FluidUtil.getFluidHandler(held).isPresent();
        if (level.isClientSide) return crystal!=null || fluidContainer || held.isEmpty()
                ? InteractionResult.SUCCESS : InteractionResult.PASS;

        if (crystal!=null) {
            if (tank.insertCrystal(crystal) && !player.getAbilities().instabuild) held.shrink(1);
            showAmount(player,tank);
            return InteractionResult.CONSUME;
        }
        if (player instanceof ServerPlayer serverPlayer
                && FluidUtil.interactWithFluidHandler(serverPlayer,hand,tank.fluidHandler())) {
            showAmount(player,tank);
            return InteractionResult.CONSUME;
        }
        if (held.isEmpty()) {
            showAmount(player,tank);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    private static void showAmount(Player player,ElementTankBlockEntity tank) {
        if (tank.fluid().isEmpty()) {
            player.displayClientMessage(Component.translatable("message.botania_tea.element_tank.empty",tank.capacity()),true);
        } else {
            player.displayClientMessage(Component.translatable("message.botania_tea.element_tank.amount",
                    tank.fluid().getDisplayName(),tank.fluid().getAmount(),tank.capacity()),true);
        }
    }

    @Override public List<ItemStack> getDrops(BlockState state,LootParams.Builder builder) {
        List<ItemStack> drops=super.getDrops(state,builder);
        BlockEntity blockEntity=builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (blockEntity!=null) for (ItemStack drop:drops) if (drop.is(asItem())) {
            BlockItem.setBlockEntityData(drop,blockEntity.getType(),blockEntity.saveWithoutMetadata());
            break;
        }
        return drops;
    }
}

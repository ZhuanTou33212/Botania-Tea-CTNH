package com.qianxin.botaniatea.block;

import com.qianxin.botaniatea.block.entity.RegisteredFunctionalFlowerBlockEntity;
import com.qianxin.botaniatea.item.EssenceBottleItem;
import com.qianxin.botaniatea.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import com.qianxin.botaniatea.content.TeaEssence;
import vazkii.botania.forge.block.ForgeSpecialFlowerBlock;

public final class HeteromorphicCamelliaBlock extends TeaSpecialFlowerBlock {
    public HeteromorphicCamelliaBlock(Properties properties) {
        super(net.minecraft.world.effect.MobEffects.DIG_SPEED, 5, properties,
                () -> com.qianxin.botaniatea.registry.ModBlockEntities.REGISTERED_FUNCTIONAL_FLOWER.get());
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);
        if (held.isEmpty() && level.getBlockEntity(pos) instanceof RegisteredFunctionalFlowerBlockEntity flower) {
            if (!level.isClientSide) player.displayClientMessage(flower.camelliaStatus(), true);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!(held.getItem() instanceof EssenceBottleItem bottle)
                || !(level.getBlockEntity(pos) instanceof RegisteredFunctionalFlowerBlockEntity flower)) {
            return super.use(state, level, pos, player, hand, hit);
        }
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!flower.selectEssence(bottle.essence())) return InteractionResult.CONSUME;
        if (!player.getAbilities().instabuild) {
            held.shrink(1);
            if (held.isEmpty()) player.setItemInHand(hand, new ItemStack(Items.GLASS_BOTTLE));
            else if (!player.addItem(new ItemStack(Items.GLASS_BOTTLE))) player.drop(new ItemStack(Items.GLASS_BOTTLE), false);
        }
        flower.sync();
        return InteractionResult.CONSUME;
    }
}

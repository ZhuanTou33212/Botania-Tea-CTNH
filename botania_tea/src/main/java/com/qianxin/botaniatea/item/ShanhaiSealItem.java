package com.qianxin.botaniatea.item;

import com.qianxin.botaniatea.block.entity.RegisteredFunctionalFlowerBlockEntity;
import com.qianxin.botaniatea.block.entity.RegisteredGeneratingFlowerBlockEntity;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Permanently enables the bound flower's sealed-work mode. */
public final class ShanhaiSealItem extends Item {
    public ShanhaiSealItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        BlockEntity blockEntity = context.getLevel().getBlockEntity(context.getClickedPos());
        if (context.getLevel().isClientSide) {
            return blockEntity instanceof RegisteredFunctionalFlowerBlockEntity
                    || blockEntity instanceof RegisteredGeneratingFlowerBlockEntity
                    || blockEntity instanceof com.qianxin.botaniatea.block.entity.TeaEssenceStillbloomBlockEntity
                    || blockEntity instanceof com.qianxin.botaniatea.block.entity.SublimationBloomBlockEntity
                    ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }
        boolean sealed = false;
        if (blockEntity instanceof RegisteredFunctionalFlowerBlockEntity flower) {
            sealed = flower.seal();
        } else if (blockEntity instanceof RegisteredGeneratingFlowerBlockEntity flower) {
            sealed = flower.seal();
        } else if (blockEntity instanceof com.qianxin.botaniatea.block.entity.TeaEssenceStillbloomBlockEntity flower) {
            sealed = flower.seal();
        } else if (blockEntity instanceof com.qianxin.botaniatea.block.entity.SublimationBloomBlockEntity flower) {
            sealed = flower.seal();
        }
        if (!sealed) return InteractionResult.PASS;
        context.getLevel().playSound(null, context.getClickedPos(),
                com.qianxin.botaniatea.registry.ModSounds.SEAL_APPLIED.get(), net.minecraft.sounds.SoundSource.BLOCKS, .7F, 1F);
        if (context.getPlayer() != null && !context.getPlayer().getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }
        return InteractionResult.SUCCESS;
    }
}

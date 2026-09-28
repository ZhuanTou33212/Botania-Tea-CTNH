package com.qianxin.botaniatea.block;

import com.qianxin.botaniatea.block.entity.RegisteredGeneratingFlowerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.BlockHitResult;
import vazkii.botania.forge.block.ForgeSpecialFlowerBlock;

/** Adds the separate input-pool binding gesture required by butterfly_orchid. */
public final class ButterflyOrchidBlock extends TeaSpecialFlowerBlock {
    public ButterflyOrchidBlock(Properties properties) {
        super(net.minecraft.world.effect.MobEffects.DIG_SPEED, 5, properties,
                () -> com.qianxin.botaniatea.registry.ModBlockEntities.REGISTERED_GENERATING_FLOWER.get());
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (player.isCrouching() && player.getItemInHand(hand).isEmpty()
                && level.getBlockEntity(pos) instanceof RegisteredGeneratingFlowerBlockEntity flower) {
            if (level.isClientSide) return InteractionResult.SUCCESS;
            return flower.bindButterflyInputToNearestPool()
                    ? InteractionResult.CONSUME : InteractionResult.PASS;
        }
        return super.use(state, level, pos, player, hand, hit);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (level.getBlockEntity(pos) instanceof RegisteredGeneratingFlowerBlockEntity flower
                && flower.isButterflyActive() && random.nextFloat() < 0.8F) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double radius = 0.35D + random.nextDouble() * 0.45D;
            level.addParticle(ParticleTypes.ENCHANT,
                    pos.getX() + .5D + Math.cos(angle) * radius,
                    pos.getY() + .75D + random.nextDouble() * .35D,
                    pos.getZ() + .5D + Math.sin(angle) * radius,
                    -Math.sin(angle) * .03D, .02D, Math.cos(angle) * .03D);
        }
    }
}

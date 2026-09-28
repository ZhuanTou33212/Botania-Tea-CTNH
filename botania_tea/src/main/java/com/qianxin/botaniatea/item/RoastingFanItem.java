package com.qianxin.botaniatea.item;

import com.qianxin.teaart.api.TimedTeaDevice;
import com.qianxin.botaniatea.config.TeaGardenConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import vazkii.botania.api.mana.ManaItemHandler;

/** Ignites a 3x3 area in front of the user and still advances a clicked running tea device. */
public final class RoastingFanItem extends Item {
    public RoastingFanItem(Properties properties) { super(properties); }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        return activate(context.getLevel(), context.getPlayer(), context.getHand(), context.getClickedPos());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, net.minecraft.world.InteractionHand hand) {
        InteractionResult result = activate(level, player, hand, null);
        return new InteractionResultHolder<>(result, player.getItemInHand(hand));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return activate(context.getLevel(), context.getPlayer(), context.getHand(), context.getClickedPos());
    }

    private InteractionResult activate(Level level, Player player, net.minecraft.world.InteractionHand hand, BlockPos clickedPos) {
        if (player == null || player.getCooldowns().isOnCooldown(this)) return InteractionResult.FAIL;
        ItemStack stack = player.getItemInHand(hand);
        int cost = TeaGardenConfig.FAN_COST.get();
        if (!player.getAbilities().instabuild
                && !ManaItemHandler.instance().requestManaExactForTool(stack, player, cost, false)) return InteractionResult.FAIL;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!player.getAbilities().instabuild) ManaItemHandler.instance().requestManaExactForTool(stack, player, cost, true);

        Direction forward = player.getDirection();
        Direction right = forward.getClockWise();
        BlockPos base = player.blockPosition().relative(forward, 2);
        for (int width = -1; width <= 1; width++) for (int height = 0; height < 3; height++) {
            BlockPos pos = base.relative(right, width).above(height);
            if (!level.hasChunkAt(pos) || !level.mayInteract(player, pos)) continue;
            var state = level.getBlockState(pos);
            var fire = BaseFireBlock.getState(level, pos);
            if (state.canBeReplaced() && fire.canSurvive(level, pos)) level.setBlockAndUpdate(pos, fire);
            else if (state.isFlammable(level, pos, forward.getOpposite())) {
                BlockPos face = pos.relative(forward.getOpposite());
                var faceFire = BaseFireBlock.getState(level, face);
                if (level.getBlockState(face).canBeReplaced() && faceFire.canSurvive(level, face)) level.setBlockAndUpdate(face, faceFire);
            }
        }

        Vec3 center = player.position().add(forward.getStepX() * 2, 1.5, forward.getStepZ() * 2);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, AABB.ofSize(center, 3, 3, 3),
                target -> target != player && target.isAlive() && !target.fireImmune())) target.setSecondsOnFire(5);

        if (clickedPos != null && level.hasChunkAt(clickedPos) && level.mayInteract(player, clickedPos)
                && level.getBlockEntity(clickedPos) instanceof TimedTeaDevice processing && processing.canAdvanceProcessing())
            processing.tryAdvanceProcessing(TeaGardenConfig.FAN_ADVANCE.get());
        level.playSound(null, player.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1F, 1F);
        if (level instanceof net.minecraft.server.level.ServerLevel server)
            server.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME, center.x, center.y, center.z, 18, 1, 1, 1, .03);
        player.getCooldowns().addCooldown(this, TeaGardenConfig.FAN_COOLDOWN.get());
        return InteractionResult.SUCCESS;
    }
}

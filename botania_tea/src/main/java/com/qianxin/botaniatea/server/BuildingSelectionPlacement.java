package com.qianxin.botaniatea.server;

import com.qianxin.botaniatea.BotaniaTeaMod;
import com.qianxin.botaniatea.block.entity.RegisteredFunctionalFlowerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.List;

/** Keeps the actor's claim identity and lets Forge roll back denied placement snapshots. */
@Mod.EventBusSubscriber(modid = BotaniaTeaMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BuildingSelectionPlacement {
    private record Attempt(FakePlayer actor, ServerPlayer owner, BlockPos flower, BlockPos target) {}
    public record Result(boolean placed, ItemStack remainder) {}
    private static final ThreadLocal<Attempt> ATTEMPT = new ThreadLocal<>();
    private BuildingSelectionPlacement() {}

    public static boolean material(ItemStack stack) {
        return stack.getItem() instanceof BlockItem item && !(item.getBlock() instanceof FallingBlock);
    }

    public static Result place(RegisteredFunctionalFlowerBlockEntity flower, ServerPlayer owner, BlockPos pos, ItemStack reserved) {
        if (!(flower.getLevel() instanceof ServerLevel level) || !material(reserved)
                || !BuildingSelection.inRange(flower.getBlockPos(), pos) || pos.equals(flower.getBlockPos())
                || !BuildingSelectionService.canInteract(owner, pos) || !level.getBlockState(pos).isAir()
                || !level.getFluidState(pos).isEmpty() || ATTEMPT.get() != null) return new Result(false, reserved);
        // A dedicated actor prevents overwriting another mod's cached FakePlayer inventory/position.
        var actor = new FakePlayer(level, owner.getGameProfile());
        actor.getAbilities().instabuild = false;
        actor.getAbilities().mayBuild = owner.getAbilities().mayBuild;
        actor.setPos(Vec3.atCenterOf(flower.getBlockPos()));
        actor.setYRot(owner.getYRot()); actor.setXRot(owner.getXRot());
        actor.setItemInHand(InteractionHand.MAIN_HAND, reserved);
        ATTEMPT.set(new Attempt(actor, owner, flower.getBlockPos(), pos));
        try {
            var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
            var result = reserved.useOn(new UseOnContext(actor, InteractionHand.MAIN_HAND, hit));
            boolean placed = result.consumesAction() && !level.getBlockState(pos).isAir() && reserved.isEmpty();
            return new Result(placed, actor.getMainHandItem().copy());
        } finally {
            actor.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            ATTEMPT.remove();
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void guard(BlockEvent.EntityPlaceEvent event) {
        Attempt attempt = ATTEMPT.get();
        if (attempt == null || event.getEntity() != attempt.actor()) return;
        var snapshots = event instanceof BlockEvent.EntityMultiPlaceEvent multi
                ? multi.getReplacedBlockSnapshots() : List.of(event.getBlockSnapshot());
        var seen = new HashSet<BlockPos>();
        for (var snapshot : snapshots) {
            BlockPos pos = snapshot.getPos();
            if (!pos.equals(attempt.target()) || pos.equals(attempt.flower())
                    || !BuildingSelection.inRange(attempt.flower(), pos)
                    || !BuildingSelectionService.canInteract(attempt.owner(), pos)
                    || (seen.add(pos) && !snapshot.getReplacedBlock().isAir())) {
                event.setCanceled(true); return;
            }
        }
    }
}

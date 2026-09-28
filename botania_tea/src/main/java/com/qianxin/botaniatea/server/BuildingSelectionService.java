package com.qianxin.botaniatea.server;

import com.qianxin.botaniatea.block.entity.RegisteredFunctionalFlowerBlockEntity;
import com.qianxin.botaniatea.content.TeaFlower;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Server-owned sessions; only existing block-interaction packets are required. */
public final class BuildingSelectionService {
    private static final Map<UUID, RegisteredFunctionalFlowerBlockEntity> SESSIONS = new HashMap<>();
    private static final ThreadLocal<Boolean> PERMISSION_CHECK = ThreadLocal.withInitial(() -> false);
    private static final DustParticleOptions RANGE = new DustParticleOptions(new Vector3f(.35F, .8F, .65F), .7F);
    private BuildingSelectionService() {}

    public static boolean checkingPermission() { return PERMISSION_CHECK.get(); }
    public static boolean canInteract(Player player, BlockPos pos) {
        var level = player.level();
        return player.isAlive() && !player.isSpectator() && player.getAbilities().mayBuild
                && level.hasChunkAt(pos) && !level.isOutsideBuildHeight(pos)
                && level.getWorldBorder().isWithinBounds(pos) && level.mayInteract(player, pos)
                && player.mayUseItemAt(pos, Direction.UP, ItemStack.EMPTY);
    }

    /** Ask the same claim/interaction listeners as a manual container interaction. */
    public static boolean canUseContainer(Player player, BlockPos pos, Direction face) {
        if (!canInteract(player, pos)) return false;
        if (player.level().isClientSide) return true;
        if (checkingPermission()) return false;
        PERMISSION_CHECK.set(true);
        try {
            var event = new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, pos,
                    new BlockHitResult(Vec3.atCenterOf(pos), face, pos, false));
            MinecraftForge.EVENT_BUS.post(event);
            return !event.isCanceled() && event.getUseBlock() != Event.Result.DENY && event.getUseItem() != Event.Result.DENY;
        } finally { PERMISSION_CHECK.remove(); }
    }

    /** Hook for TeaSpecialFlowerBlock.use, before its generic shift-empty-hand mode handler. */
    public static InteractionResult useFlower(RegisteredFunctionalFlowerBlockEntity flower, Player player, InteractionHand hand) {
        if (flower.flower() != TeaFlower.BUILDING_BEGONIA || hand != InteractionHand.MAIN_HAND
                || !player.getMainHandItem().isEmpty() || !player.getOffhandItem().isEmpty()) return InteractionResult.PASS;
        if (player.level().isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer server) || !canInteract(player, flower.getBlockPos())
                || player.distanceToSqr(Vec3.atCenterOf(flower.getBlockPos())) > 64) return InteractionResult.FAIL;
        var selection = flower.extraWork().buildingSelection();
        if (selection.active() && !selection.ownedBy(player.getUUID())) {
            var owner = player.level().getPlayerByUUID(selection.owner());
            if (owner != null && owner.isAlive() && owner.distanceToSqr(Vec3.atCenterOf(flower.getBlockPos())) <= 24 * 24) {
                message(player, "busy"); return InteractionResult.CONSUME;
            }
            selection.stop();
        }
        if (player.isShiftKeyDown()) {
            flower.extraWork().toggleMode();
            message(player, "mode", Component.translatable(selection.mode().key()));
        } else if (!selection.active()) {
            if (!flower.extraWork().buildingSourceUsable(server)) {
                message(player, "invalid_container"); return InteractionResult.CONSUME;
            }
            close(player.getUUID());
            selection.begin(player.getUUID(), flower.getBlockPos(), player.level().dimension().location().toString());
            SESSIONS.put(player.getUUID(), flower);
            message(player, "start", Component.translatable(selection.mode().key()));
        } else if (selection.running() || !selection.ready()) {
            close(player.getUUID()); selection.stop(); message(player, "cancelled");
        } else if (flower.extraWork().buildingSourceUsable(server) && !selection.targets(flower.getBlockPos()).isEmpty()) {
            message(player, "waiting_redstone"); // Right-click configures only; redstone starts the work.
        } else message(player, "invalid_container");
        flower.setChanged(); flower.sync();
        return InteractionResult.CONSUME;
    }

    public static RegisteredFunctionalFlowerBlockEntity session(UUID player) { return SESSIONS.get(player); }

    /** Called after protection listeners and only for actual right-clicks on blocks. */
    public static boolean select(ServerPlayer player, BlockHitResult hit) {
        var flower = SESSIONS.get(player.getUUID());
        if (flower == null) return false;
        var selection = flower.extraWork().buildingSelection();
        if (!validSession(player, flower)) { close(player.getUUID()); return false; }
        if (selection.running()) { message(player, "running"); return true; }
        if (player.isShiftKeyDown()) {
            flower.extraWork().toggleMode();
            message(player, "mode", Component.translatable(selection.mode().key()));
            flower.sync(); return true;
        }
        BlockPos selected = hit.getBlockPos().relative(hit.getDirection());
        if (player.distanceToSqr(hit.getLocation()) > 64 || !canInteract(player, hit.getBlockPos())
                || !canInteract(player, selected) || !BuildingSelection.inRange(flower.getBlockPos(), selected)) {
            message(player, "out_of_range"); return true;
        }
        if (selection.mode() == BuildingSelection.Mode.PLATFORM && selection.first() != null && !selection.ready()
                && selection.first().getY() != selected.getY()) {
            message(player, "same_height"); return true;
        }
        selection.select(selected); flower.setChanged(); flower.sync();
        message(player, selection.ready() ? "ready" : "first", selected.getX(), selected.getY(), selected.getZ());
        return true;
    }

    private static boolean validSession(ServerPlayer player, RegisteredFunctionalFlowerBlockEntity flower) {
        return !flower.isRemoved() && player.level() == flower.getLevel()
                && flower.getLevel().hasChunkAt(flower.getBlockPos()) && flower.getLevel().getBlockEntity(flower.getBlockPos()) == flower
                && canInteract(player, flower.getBlockPos()) && player.distanceToSqr(Vec3.atCenterOf(flower.getBlockPos())) <= 24 * 24
                && flower.extraWork().buildingSelection().ownedBy(player.getUUID());
    }

    public static void tick(ServerPlayer player) {
        var flower = SESSIONS.get(player.getUUID());
        if (flower == null) return;
        if (!validSession(player, flower)) { close(player.getUUID()); return; }
        if (player.tickCount % 10 != 0) return;
        // Fixed-density edges of the exact 17x7x17 build box, only for its owner.
        var level = player.serverLevel();
        var origin = flower.getBlockPos();
        for (int i = 0; i <= 8; i++) for (int a = 0; a < 2; a++) for (int b = 0; b < 2; b++) {
            double t = i / 8.0;
            edge(level, player, origin, -8 + 17 * t, -3 + 7 * a, -8 + 17 * b);
            edge(level, player, origin, -8 + 17 * a, -3 + 7 * t, -8 + 17 * b);
            edge(level, player, origin, -8 + 17 * a, -3 + 7 * b, -8 + 17 * t);
        }
        var selection = flower.extraWork().buildingSelection();
        if (selection.first() != null) particle(level, player, selection.first());
        if (selection.second() != null) particle(level, player, selection.second());
    }

    private static void particle(ServerLevel level, ServerPlayer player, BlockPos pos) {
        level.sendParticles(player, RANGE, true, pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, 1, 0, 0, 0, 0);
    }
    private static void edge(ServerLevel level, ServerPlayer player, BlockPos origin, double x, double y, double z) {
        level.sendParticles(player, RANGE, true, origin.getX() + x, origin.getY() + y, origin.getZ() + z, 1, 0, 0, 0, 0);
    }
    public static void message(Player player, String key, Object... args) {
        player.displayClientMessage(Component.translatable("message.botania_tea.building." + key, args), true);
    }
    public static void close(UUID player) {
        var flower = SESSIONS.remove(player);
        if (flower != null && flower.extraWork().buildingSelection().ownedBy(player)) {
            flower.extraWork().buildingSelection().stop(); flower.setChanged();
            if (!flower.isRemoved()) flower.sync();
        }
    }
    public static void clear() { SESSIONS.clear(); }
}

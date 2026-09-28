package com.qianxin.botaniatea.entity;

import com.qianxin.botaniatea.registry.ModEntities;
import com.qianxin.botaniatea.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

/** Server-side grappling state: free-flight hook, optional block anchor and explicit cancellation. */
public final class BambooGrapplingHookEntity extends Entity implements ItemSupplier {
    public static final double MAX_RANGE = 64.0D;
    private static final double FLIGHT_SPEED = 2.4D;
    public static final double PULL_SPEED = 0.8D;
    private UUID ownerId;
    private BlockPos anchor = BlockPos.ZERO;
    private Vec3 launchPos = Vec3.ZERO;
    private Vec3 flightTarget = Vec3.ZERO;
    private ItemStack tool = ItemStack.EMPTY;
    private boolean attached;
    public void setTool(ItemStack value) { tool = value.copyWithCount(1); }
    public static boolean loadedRay(Level level, Vec3 start, Vec3 end) {
        int steps = Math.max(1, (int) Math.ceil(start.distanceTo(end) * 2));
        for (int i = 0; i <= steps; i++) if (!level.hasChunkAt(BlockPos.containing(start.lerp(end, (double) i / steps)))) return false;
        return true;
    }

    public BambooGrapplingHookEntity(EntityType<? extends BambooGrapplingHookEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public BambooGrapplingHookEntity(Level level, Player owner, BlockPos anchor) {
        this(ModEntities.BAMBOO_GRAPPLING_HOOK.get(), level);
        ownerId = owner.getUUID();
        this.anchor = anchor.immutable();
        launchPos = owner.getEyePosition();
        flightTarget = Vec3.atCenterOf(anchor);
        setPos(launchPos);
    }

    public BambooGrapplingHookEntity(Level level, Player owner, Vec3 direction) {
        this(ModEntities.BAMBOO_GRAPPLING_HOOK.get(), level);
        ownerId = owner.getUUID();
        launchPos = owner.getEyePosition();
        flightTarget = launchPos.add(direction.normalize().scale(MAX_RANGE));
        setPos(launchPos);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof net.minecraft.server.level.ServerLevel server)) return;
        Player owner = ownerId == null ? null : server.getPlayerByUUID(ownerId);
        if (owner == null || !owner.isAlive() || owner.isShiftKeyDown()) {
            cancel(owner);
            return;
        }
        ItemStack held = owner.getMainHandItem();
        if (!ownsHook(held)) held = owner.getOffhandItem();
        if (!ownsHook(held)) { cancel(owner); return; }
        if (!attached) {
            Vec3 flight = flightTarget.subtract(position());
            Vec3 next = flight.length() <= FLIGHT_SPEED ? flightTarget : position().add(flight.normalize().scale(FLIGHT_SPEED));
            if (!server.hasChunkAt(BlockPos.containing(next))) { cancel(owner); return; }
            var hit = server.clip(new net.minecraft.world.level.ClipContext(position(), next,
                    net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, owner));
            if (hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK
                    && server.mayInteract(owner, hit.getBlockPos())) {
                anchor = hit.getBlockPos().immutable();
                flightTarget = hit.getLocation();
                setPos(flightTarget);
                attached = true;
            } else if (next.equals(flightTarget) || launchPos.distanceToSqr(next) >= MAX_RANGE * MAX_RANGE) {
                setPos(next);
                cancel(owner);
            } else setPos(next);
            return;
        }
        if (!server.hasChunkAt(anchor) || server.getBlockState(anchor).getCollisionShape(server, anchor).isEmpty()
                || owner.getEyePosition().distanceToSqr(flightTarget) > MAX_RANGE * MAX_RANGE
                || !loadedRay(server, owner.getEyePosition(), flightTarget)) { cancel(owner); return; }
        var hit = server.clip(new net.minecraft.world.level.ClipContext(owner.getEyePosition(), flightTarget,
                net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, owner));
        if (!hit.getBlockPos().equals(anchor)) { cancel(owner); return; }
        Vec3 target = flightTarget;
        Vec3 delta = target.subtract(owner.position());
        if (delta.lengthSqr() < 4.0D) {
            cancel(owner);
            return;
        }
        if (!owner.getAbilities().instabuild
                && !vazkii.botania.api.mana.ManaItemHandler.instance().requestManaExactForTool(held, owner, 10, true)) {
            owner.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.botania_tea.grapple_no_mana"), true);
            cancel(owner);
            return;
        }
        owner.setDeltaMovement(delta.normalize().scale(PULL_SPEED));
        owner.hurtMarked = true;
        setPos(target);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (!level().isClientSide && ownerId != null && ownerId.equals(player.getUUID())) cancel(player);
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        if (!level().isClientSide) cancel(owner());
        return true;
    }

    private boolean ownsHook(ItemStack stack) {
        return stack.is(ModItems.BAMBOO_GRAPPLING_HOOK.get()) && stack.hasTag() && stack.getTag().hasUUID("TeaHookEntity")
                && stack.getTag().getUUID("TeaHookEntity").equals(getUUID());
    }
    public void cancel(@Nullable Player owner) {
        if (isRemoved()) return;
        if (owner != null) {
            if (owner.getPersistentData().hasUUID("TeaHookEntity") && owner.getPersistentData().getUUID("TeaHookEntity").equals(getUUID()))
                owner.getPersistentData().remove("TeaHookEntity");
            for (int i = 0; i < owner.getInventory().getContainerSize(); i++) {
                ItemStack stack = owner.getInventory().getItem(i);
                if (ownsHook(stack)) stack.getTag().remove("TeaHookEntity");
            }
        }
        // The tool stays in the player's inventory: no replacement stack, NBT loss, or full-inventory drop.
        discard();
    }

    @Nullable
    private Player owner() {
        return level() instanceof net.minecraft.server.level.ServerLevel server && ownerId != null
                ? server.getPlayerByUUID(ownerId) : null;
    }

    @Override protected void defineSynchedData() {}
    @Override protected void readAdditionalSaveData(CompoundTag tag) {
        ownerId = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        anchor = BlockPos.of(tag.getLong("Anchor"));
        launchPos = new Vec3(tag.getDouble("LaunchX"), tag.getDouble("LaunchY"), tag.getDouble("LaunchZ"));
        flightTarget = new Vec3(tag.getDouble("TargetX"), tag.getDouble("TargetY"), tag.getDouble("TargetZ"));
        tool = ItemStack.of(tag.getCompound("Tool")); attached = tag.getBoolean("Attached");
    }
    @Override protected void addAdditionalSaveData(CompoundTag tag) {
        if (ownerId != null) tag.putUUID("Owner", ownerId);
        tag.putLong("Anchor", anchor.asLong());
        tag.putDouble("LaunchX", launchPos.x); tag.putDouble("LaunchY", launchPos.y); tag.putDouble("LaunchZ", launchPos.z);
        tag.putDouble("TargetX", flightTarget.x); tag.putDouble("TargetY", flightTarget.y); tag.putDouble("TargetZ", flightTarget.z);
        tag.put("Tool", tool.save(new CompoundTag())); tag.putBoolean("Attached", attached);
    }
    @Override public ItemStack getPickResult() { return new ItemStack(ModItems.BAMBOO_GRAPPLING_HOOK.get()); }
    @Override public ItemStack getItem() { return tool.isEmpty() ? new ItemStack(ModItems.BAMBOO_GRAPPLING_HOOK.get()) : tool.copy(); }
}

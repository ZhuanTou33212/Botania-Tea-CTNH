package com.qianxin.botaniatea.entity;

import com.qianxin.botaniatea.content.TeaEssence;
import com.qianxin.botaniatea.registry.ModEntities;
import com.qianxin.botaniatea.registry.ModItems;
import com.qianxin.botaniatea.registry.ModFluids;
import com.qianxin.botaniatea.registry.ModFluidTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import vazkii.botania.common.item.WandOfTheForestItem;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class WaterSparkEntity extends Entity implements ItemSupplier {
    private static final EntityDataAccessor<BlockPos> ATTACHED_POS =
            SynchedEntityData.defineId(WaterSparkEntity.class, EntityDataSerializers.BLOCK_POS);
    private static final EntityDataAccessor<Optional<UUID>> TARGET_ID =
            SynchedEntityData.defineId(WaterSparkEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final String TAG_TARGET = "Target";
    private static final String WAND_SELECTION_TAG = "BotaniaTeaWaterSparkSelection";
    public static final int RANGE = 8;
    public static final int TRANSFER_INTERVAL = 20;
    public static final int TRANSFER_AMOUNT = 50;
    private static final double BURST_SPEED = 0.35;

    private int transferCooldown;
    private int burstTick = -1;
    private int burstDuration;
    private UUID burstTargetId;
    private Fluid burstFluid;
    private int burstColor;

    public WaterSparkEntity(EntityType<? extends WaterSparkEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public WaterSparkEntity(Level level, BlockPos attachedPos) {
        this(ModEntities.WATER_SPARK.get(), level);
        entityData.set(ATTACHED_POS, attachedPos.immutable());
        moveToAnchor();
    }

    public BlockPos attachedPos() {
        return entityData.get(ATTACHED_POS);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(ATTACHED_POS, BlockPos.ZERO);
        entityData.define(TARGET_ID, Optional.empty());
    }

    @Override
    public void tick() {
        super.tick();
        moveToAnchor();
        if (level().isClientSide) return;
        if (burstTick >= 0) {
            tickBurst();
            return;
        }
        if (transferCooldown > 0) {
            transferCooldown--;
            return;
        }
        tryStartBurst();
    }

    private void moveToAnchor() {
        BlockPos pos = attachedPos();
        setPos(pos.getX() + 0.5, pos.getY() + 1.15, pos.getZ() + 0.5);
    }

    private void tryStartBurst() {
        WaterSparkEntity target = targetSpark();
        if (target == null || distanceToSqr(target) > RANGE * RANGE) return;
        Endpoint source = findSource(null);
        if (source == null) return;
        FluidStack available = firstTransportableFluid(source.handler);
        if (available.isEmpty() || available.getAmount() < TRANSFER_AMOUNT) return;
        FluidStack offered = new FluidStack(available.getFluid(), TRANSFER_AMOUNT, available.getTag());
        Endpoint destination = target.findDestination(offered);
        if (destination == null || destination.handler == source.handler
                || source.handler.drain(offered, IFluidHandler.FluidAction.SIMULATE).getAmount() < TRANSFER_AMOUNT) return;

        burstTargetId = target.getUUID();
        burstFluid = available.getFluid();
        burstColor = transportColor(burstFluid);
        burstTick = 0;
        burstDuration = Math.max(4, (int) Math.ceil(distanceTo(target) / BURST_SPEED));
    }

    private void tickBurst() {
        if (!(level() instanceof ServerLevel serverLevel) || burstFluid == null || burstTargetId == null) {
            cancelBurst();
            return;
        }
        Entity entity = serverLevel.getEntity(burstTargetId);
        if (!(entity instanceof WaterSparkEntity target) || distanceToSqr(target) > RANGE * RANGE) {
            cancelBurst();
            return;
        }

        burstTick++;
        double progress = Math.min(1.0, burstTick / (double) burstDuration);
        Vec3 start = position();
        Vec3 end = target.position();
        Vec3 point = start.add(end.subtract(start).scale(progress));
        spawnBurstParticles(serverLevel, point, burstColor, progress >= 1.0);
        if (progress < 1.0) return;

        Endpoint source = findSource(burstFluid);
        if (source != null) {
            FluidStack available = firstTransportableFluid(source.handler);
            if (available.getFluid() == burstFluid) {
                FluidStack offered = new FluidStack(available.getFluid(), TRANSFER_AMOUNT, available.getTag());
                Endpoint destination = target.findDestination(offered);
                if (destination != null && destination.handler != source.handler) {
                    transfer(source.handler, destination.handler, offered);
                }
            }
        }
        cancelBurst();
        transferCooldown = TRANSFER_INTERVAL;
    }

    private static void transfer(IFluidHandler source, IFluidHandler destination, FluidStack offered) {
        int accepted = destination.fill(offered, IFluidHandler.FluidAction.SIMULATE);
        if (accepted <= 0) return;
        FluidStack drained = source.drain(new FluidStack(offered.getFluid(), accepted, offered.getTag()),
                IFluidHandler.FluidAction.EXECUTE);
        if (!drained.isEmpty()) destination.fill(drained, IFluidHandler.FluidAction.EXECUTE);
    }

    private void cancelBurst() {
        burstTick = -1;
        burstDuration = 0;
        burstTargetId = null;
        burstFluid = null;
        burstColor = 0;
    }

    @Nullable
    private Endpoint findSource(@Nullable Fluid required) {
        for (Endpoint endpoint : localEndpoints()) {
            FluidStack stack = firstTransportableFluid(endpoint.handler);
            if (!stack.isEmpty() && (required == null || required == stack.getFluid())) return endpoint;
        }
        return null;
    }

    @Nullable
    private Endpoint findDestination(FluidStack offered) {
        for (Endpoint endpoint : localEndpoints()) {
            if (endpoint.handler.fill(offered, IFluidHandler.FluidAction.SIMULATE) > 0) return endpoint;
        }
        return null;
    }

    private List<Endpoint> localEndpoints() {
        List<Endpoint> endpoints = new ArrayList<>();
        BlockPos anchor = attachedPos();
        for (BlockPos pos : BlockPos.betweenClosed(anchor.offset(-1, -1, -1), anchor.offset(1, 1, 1))) {
            IFluidHandler handler = handler(level().getBlockEntity(pos));
            if (handler != null) endpoints.add(new Endpoint(pos.immutable(), handler));
        }
        endpoints.sort(Comparator.comparingDouble(endpoint -> endpoint.pos.distSqr(anchor)));
        return endpoints;
    }

    @Nullable
    private WaterSparkEntity targetSpark() {
        if (!(level() instanceof ServerLevel serverLevel)) return null;
        Optional<UUID> targetId = entityData.get(TARGET_ID);
        if (targetId.isEmpty()) return null;
        Entity entity = serverLevel.getEntity(targetId.get());
        return entity instanceof WaterSparkEntity spark ? spark : null;
    }

    private void setTarget(@Nullable UUID targetId) {
        entityData.set(TARGET_ID, Optional.ofNullable(targetId));
        cancelBurst();
        transferCooldown = 0;
    }

    private static void spawnBurstParticles(ServerLevel level, Vec3 point, int color, boolean arrived) {
        int rgb = color & 0xFFFFFF;
        DustParticleOptions particle = new DustParticleOptions(new Vector3f(
                ((rgb >> 16) & 255) / 255F,
                ((rgb >> 8) & 255) / 255F,
                (rgb & 255) / 255F), 1.0F);
        level.sendParticles(particle, point.x, point.y, point.z, arrived ? 8 : 2,
                arrived ? 0.14 : 0.035, arrived ? 0.14 : 0.035, arrived ? 0.14 : 0.035, 0.01);
    }

    private static FluidStack firstTransportableFluid(IFluidHandler handler) {
        for (int tank = 0; tank < handler.getTanks(); tank++) {
            FluidStack stack = handler.getFluidInTank(tank);
            if (!stack.isEmpty() && isTransportable(stack.getFluid())) return stack.copy();
        }
        return FluidStack.EMPTY;
    }

    private static boolean isTransportable(Fluid fluid) {
        return TeaEssence.fromEssenceFluid(fluid) != null
                || fluid == ModFluids.ASCENDED_TEA_LIQUOR.get()
                || fluid == ModFluids.FLOWING_ASCENDED_TEA_LIQUOR.get();
    }

    private static int transportColor(Fluid fluid) {
        TeaEssence essence = TeaEssence.fromEssenceFluid(fluid);
        return essence == null ? ModFluidTypes.ASCENDED_TEA_TINT : essence.tint;
    }

    private static IFluidHandler handler(BlockEntity blockEntity) {
        return blockEntity == null ? null
                : blockEntity.getCapability(ForgeCapabilities.FLUID_HANDLER).resolve().orElse(null);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        entityData.set(ATTACHED_POS, BlockPos.of(tag.getLong("AttachedPos")));
        entityData.set(TARGET_ID, tag.hasUUID(TAG_TARGET) ? Optional.of(tag.getUUID(TAG_TARGET)) : Optional.empty());
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putLong("AttachedPos", attachedPos().asLong());
        entityData.get(TARGET_ID).ifPresent(target -> tag.putUUID(TAG_TARGET, target));
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof Player player && isForestWand(player.getMainHandItem())) {
            if (!level().isClientSide) {
                player.getMainHandItem().getOrCreateTag().putUUID(WAND_SELECTION_TAG, getUUID());
                player.displayClientMessage(Component.translatable("message.botania_tea.water_spark.selected"), true);
            }
            return true;
        }
        if (!level().isClientSide) {
            spawnAtLocation(ModItems.WATER_SPARK.get());
            discard();
        }
        return true;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (isForestWand(held)) {
            if (!level().isClientSide) completeBinding(player, held);
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (!level().isClientSide) {
            if (!player.getAbilities().instabuild) player.addItem(new ItemStack(ModItems.WATER_SPARK.get()));
            discard();
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    private void completeBinding(Player player, ItemStack wand) {
        CompoundTag tag = wand.getTag();
        if (tag == null || !tag.hasUUID(WAND_SELECTION_TAG)) {
            player.displayClientMessage(Component.translatable("message.botania_tea.water_spark.select_first"), true);
            return;
        }
        UUID selectedId = tag.getUUID(WAND_SELECTION_TAG);
        tag.remove(WAND_SELECTION_TAG);
        if (!(level() instanceof ServerLevel serverLevel)
                || !(serverLevel.getEntity(selectedId) instanceof WaterSparkEntity source)) {
            player.displayClientMessage(Component.translatable("message.botania_tea.water_spark.selection_lost"), true);
            return;
        }
        if (source == this) {
            player.displayClientMessage(Component.translatable("message.botania_tea.water_spark.select_other"), true);
            return;
        }
        if (source.distanceToSqr(this) > RANGE * RANGE) {
            player.displayClientMessage(Component.translatable("message.botania_tea.water_spark.too_far"), true);
            return;
        }
        if (source.entityData.get(TARGET_ID).filter(getUUID()::equals).isPresent()) {
            source.setTarget(null);
            player.displayClientMessage(Component.translatable("message.botania_tea.water_spark.unbound"), true);
        } else {
            source.setTarget(getUUID());
            player.displayClientMessage(Component.translatable("message.botania_tea.water_spark.bound"), true);
        }
        bindingBeam(serverLevel, source.position(), position());
    }

    private static void bindingBeam(ServerLevel level, Vec3 start, Vec3 end) {
        DustParticleOptions particle = new DustParticleOptions(new Vector3f(0.15F, 0.85F, 0.95F), 0.8F);
        Vec3 delta = end.subtract(start);
        for (int step = 0; step <= 16; step++) {
            Vec3 point = start.add(delta.scale(step / 16.0));
            level.sendParticles(particle, point.x, point.y, point.z, 1, 0, 0, 0, 0);
        }
    }

    private static boolean isForestWand(ItemStack stack) {
        return stack.getItem() instanceof WandOfTheForestItem;
    }

    @Override
    public @NotNull ItemStack getItem() {
        return new ItemStack(ModItems.WATER_SPARK.get());
    }

    private record Endpoint(BlockPos pos, IFluidHandler handler) {}
}

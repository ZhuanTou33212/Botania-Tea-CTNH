package com.qianxin.botaniatea.block.entity;

import com.qianxin.botaniatea.config.TeaGardenConfig;
import com.qianxin.botaniatea.content.TeaFlower;
import com.qianxin.botaniatea.registry.ModBlockEntities;
import com.qianxin.botaniatea.world.TeaWorldEventData;
import com.qianxin.teaart.content.TeaLiquor;
import com.qianxin.teaart.registry.ModItemTags;
import com.qianxin.teaart.registry.ModBlockTags;
import com.qianxin.teaart.registry.ModTeaFluids;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Fireball;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;
import vazkii.botania.api.block_entity.GeneratingFlowerBlockEntity;
import vazkii.botania.api.block_entity.RadiusDescriptor;

import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import vazkii.botania.api.BotaniaForgeCapabilities;
import vazkii.botania.api.mana.ManaPool;

/** Shared generating flower entity with server-side fuel and production rules. */
public final class RegisteredGeneratingFlowerBlockEntity extends GeneratingFlowerBlockEntity {
    private static final int NBT_VERSION = 1;
    // Snapshot economics per cycle so changing server settings cannot mint refunds or change an active reward.
    private int butterflyVisitTicks = 40, butterflyUnitCost = 300, butterflyUnitReward = 2500;
    private double butterflyDiversityBonus = .25D;
    private static final int BUTTERFLY_UNREST_LIMIT = 15;
    private static final TeaLiquor[] SIX_HARMONY_SEQUENCE = {
            TeaLiquor.WHITE, TeaLiquor.GREEN, TeaLiquor.YELLOW,
            TeaLiquor.OOLONG, TeaLiquor.RED, TeaLiquor.BLACK
    };
    private int burnRemaining;
    private int pendingMana;
    private int harmonyIndex;
    private int harmonyLegacyPaid;
    private boolean sealed;
    private int workCredit;
    private BlockPos residueSource;
    private String residueDimension = "";
    private Direction residueSide = Direction.UP;
    private BlockPos butterflyInputBinding;
    private final List<ButterflyTarget> butterflyTargets = new ArrayList<>();
    private final Set<String> butterflyConsumedTypes = new LinkedHashSet<>();
    private UUID butterflyCycleId;
    private int butterflyTargetIndex;
    private int butterflyVisitRemaining;
    private int butterflyMissingTicks;
    private int butterflyInputReserve;
    private int butterflyRefundMana;
    private int butterflyConsumedCount;
    private String butterflyLastTea;
    private int butterflyUnrest;
    private int butterflyCooldown;
    private boolean butterflyActive;
    private boolean butterflyEventPending;
    private int butterflyEventType = -1;
    private String butterflyInputDimension = "";
    private BlockPos butterflyVisualTarget;
    private int butterflyVisualRemaining, butterflyVisualReceivedAt;

    @Nullable public BlockPos butterflyVisualTarget() { return butterflyActive ? butterflyVisualTarget : null; }
    public float butterflyVisualProgress(float partial) {
        return net.minecraft.util.Mth.clamp(1F - (butterflyVisualRemaining - (ticksExisted - butterflyVisualReceivedAt + partial)
                * (sealed ? TeaGardenConfig.SEALED_WORK_RATE.get() : 1)) / butterflyVisitTicks, 0F, 1F);
    }
    @Override public AABB getRenderBoundingBox() { return new AABB(worldPosition).inflate(8); }

    public boolean isButterflyActive() {
        return butterflyActive;
    }

    public RegisteredGeneratingFlowerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.REGISTERED_GENERATING_FLOWER.get(), pos, state);
    }

    @Override public int getMaxMana() { return TeaGardenConfig.GENERATING_MANA_CAPACITY.get(); }
    @Override public int getColor() { return 0xC49C54; }
    @Override public RadiusDescriptor getRadius() {
        return new RadiusDescriptor.Circle(worldPosition, TeaGardenConfig.WORK_RADIUS.get());
    }

    @Override public void tickFlower() {
        super.tickFlower();
        if (level == null || level.isClientSide) return;
        TeaFlower flower = TeaFlower.fromBlock(getBlockState().getBlock());
        if (flower == TeaFlower.AFTERTASTE_DAISY) {
            for (int step = 0; step < (sealed ? TeaGardenConfig.SEALED_WORK_RATE.get() : 1); step++) tickAftertaste();
            setChanged();
        } else if (flower == TeaFlower.BUTTERFLY_ORCHID) {
            tickButterfly();
        } else if (flower == TeaFlower.SIX_HARMONY_CHRYSANTHEMUM) {
            workCredit = Math.min(40, workCredit) + (sealed ? TeaGardenConfig.SEALED_WORK_RATE.get() : 1);
            for (int actions = 0; workCredit >= 40 && actions < 11; actions++) {
                if (!processHarmony()) { workCredit = 40; break; }
                workCredit -= 40;
            }
            setChanged();
        } else if (flower == TeaFlower.PROJECTILE_BELLFLOWER) {
            catchProjectile();
        }
    }

    private void tickButterfly() {
        if (!(level instanceof ServerLevel server)) return;
        setChanged(); // Visit, cooldown and refund progress must survive ordinary chunk saves.
        refundButterflyMana();
        if (pendingMana > 0) {
            int accepted = Math.min(pendingMana, Math.max(0, getMaxMana() - getMana()));
            addMana(accepted);
            pendingMana -= accepted;
        }
        if (butterflyEventPending) {
            // Recover older saves that stopped between triggering a disaster and settling a cycle.
            if (butterflyActive) settleButterflyCycle();
            if (butterflyCycleId != null) TeaWorldEventData.trigger(server, worldPosition, butterflyCycleId, butterflyEventType);
            butterflyEventPending = false;
            butterflyEventType = -1;
            butterflyUnrest = 0;
            return;
        }
        if (butterflyCooldown > 0) {
            butterflyCooldown--;
            return;
        }
        if (butterflyActive) {
            if (ticksExisted % 5 == 0) sync();
            if (butterflyTargetIndex < butterflyTargets.size() && !level.hasChunkAt(butterflyTargets.get(butterflyTargetIndex).pos)) {
                tickButterflyVisit(); // Loaded-target grace is 100 real ticks, not 100 sealed work units.
                return;
            }
            int steps = sealed ? TeaGardenConfig.SEALED_WORK_RATE.get() : 1;
            for (int step = 0; step < steps && butterflyActive && !butterflyEventPending; step++) tickButterflyVisit();
        } else if (pendingMana == 0 && level.hasNeighborSignal(worldPosition)) {
            startButterflyCycle();
        }
    }

    private void startButterflyCycle() {
        butterflyCooldown = 20; // Retry failed input/space sampling at a bounded cadence, not every tick.
        sampleButterflyTargets();
        if (butterflyTargets.isEmpty()) return;
        butterflyVisitTicks = TeaGardenConfig.BUTTERFLY_VISIT.get();
        butterflyUnitCost = 0; // New cycles are redstone-only; old saved reserves still refund normally.
        butterflyUnitReward = TeaGardenConfig.BUTTERFLY_REWARD.get();
        butterflyDiversityBonus = TeaGardenConfig.BUTTERFLY_DIVERSITY.get();
        int worstReward = (int) Math.ceil(butterflyTargets.size() * (double) butterflyUnitReward * (1D + butterflyDiversityBonus * 7D));
        if (getMaxMana() - getMana() < worstReward) return;
        butterflyCycleId = UUID.randomUUID();
        butterflyTargetIndex = 0;
        butterflyVisitRemaining = butterflyVisitTicks;
        butterflyMissingTicks = 0;
        butterflyInputReserve = 0;
        butterflyConsumedCount = 0;
        butterflyConsumedTypes.clear();
        butterflyCooldown = 0;
        butterflyActive = true;
        setChanged();
        sync();
    }

    private void sampleButterflyTargets() {
        butterflyTargets.clear();
        int radius = 7;
        for (BlockPos pos : BlockPos.betweenClosed(worldPosition.offset(-radius, -2, -radius),
                worldPosition.offset(radius, 2, radius))) {
            if (!level.hasChunkAt(pos)) continue;
            BlockState state = level.getBlockState(pos);
            if (state.is(ModBlockTags.FINISHED_TEA_BLOCKS) && !state.hasBlockEntity()) {
                butterflyTargets.add(new ButterflyTarget(pos.immutable(),
                        BuiltInRegistries.BLOCK.getKey(state.getBlock())));
            }
        }
        butterflyTargets.sort(Comparator
                .comparingDouble((ButterflyTarget target) -> target.pos.distSqr(worldPosition))
                .thenComparingInt(target -> target.pos.getX())
                .thenComparingInt(target -> target.pos.getY())
                .thenComparingInt(target -> target.pos.getZ()));
        if (butterflyTargets.size() > TeaGardenConfig.BUTTERFLY_TARGETS.get()) {
            butterflyTargets.subList(TeaGardenConfig.BUTTERFLY_TARGETS.get(), butterflyTargets.size()).clear();
        }
    }

    private void tickButterflyVisit() {
        if (butterflyTargetIndex >= butterflyTargets.size()) {
            settleButterflyCycle();
            return;
        }
        ButterflyTarget target = butterflyTargets.get(butterflyTargetIndex);
        if (!level.hasChunkAt(target.pos)) {
            if (++butterflyMissingTicks < 100) return;
            refundButterflyTarget();
            advanceButterflyTarget();
            return;
        }
        butterflyMissingTicks = 0;
        if (--butterflyVisitRemaining > 0) return;
        BlockState state = level.getBlockState(target.pos);
        ResourceLocation actual = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (actual.equals(target.expected) && state.is(ModBlockTags.FINISHED_TEA_BLOCKS) && !state.hasBlockEntity()
                && level.getWorldBorder().isWithinBounds(target.pos) && level.destroyBlock(target.pos, false)) {
            butterflyInputReserve -= butterflyUnitCost;
            butterflyConsumedCount++;
            String type = target.expected.toString();
            butterflyConsumedTypes.add(type);
            if (type.equals(butterflyLastTea) && level.random.nextFloat() < 0.25F) butterflyUnrest++;
            butterflyLastTea = type;
            if (butterflyUnrest >= BUTTERFLY_UNREST_LIMIT && !butterflyEventPending) {
                butterflyEventPending = true;
                butterflyEventType = level.random.nextInt(4);
                butterflyUnrest = 0;
            }
        } else {
            refundButterflyTarget();
        }
        advanceButterflyTarget();
    }

    private void refundButterflyTarget() {
        butterflyInputReserve -= butterflyUnitCost;
        butterflyRefundMana += butterflyUnitCost;
    }

    private void advanceButterflyTarget() {
        butterflyTargetIndex++;
        butterflyVisitRemaining = butterflyVisitTicks;
        butterflyMissingTicks = 0;
        if (butterflyTargetIndex >= butterflyTargets.size() || butterflyEventPending || !level.hasNeighborSignal(worldPosition)) settleButterflyCycle();
    }

    private void settleButterflyCycle() {
        int types = butterflyConsumedTypes.size();
        int reward = butterflyConsumedCount == 0 ? 0
                : (int) Math.floor(butterflyConsumedCount * (double) butterflyUnitReward * (1D + butterflyDiversityBonus * (types - 1)));
        pendingMana += reward;
        int emitted = Math.min(pendingMana, Math.max(0, getMaxMana() - getMana()));
        addMana(emitted);
        pendingMana -= emitted;
        butterflyActive = false;
        butterflyTargets.clear();
        butterflyRefundMana += butterflyInputReserve;
        butterflyInputReserve = 0;
        butterflyTargetIndex = 0;
        butterflyConsumedCount = 0;
        butterflyConsumedTypes.clear();
        butterflyCooldown = 20;
        setChanged();
        sync();
    }

    private void refundButterflyMana() {
        if (butterflyRefundMana <= 0) return;
        ManaPool input = findButterflyInput();
        if (input == null) return;
        int accepted = Math.min(butterflyRefundMana, Math.max(0, input.getMaxMana() - input.getCurrentMana()));
        if (accepted > 0) {
            input.receiveMana(accepted);
            butterflyRefundMana -= accepted;
        }
    }

    @Nullable
    private ManaPool findButterflyInput() {
        if (butterflyInputBinding == null || level == null || !level.hasChunkAt(butterflyInputBinding)
                || (!butterflyInputDimension.isEmpty() && !butterflyInputDimension.equals(level.dimension().location().toString()))
                || butterflyInputBinding.equals(getBinding())
                || butterflyInputBinding.distSqr(worldPosition) > 64) return null;
        BlockEntity blockEntity = level.getBlockEntity(butterflyInputBinding);
        if (blockEntity == null) return null;
        return blockEntity.getCapability(BotaniaForgeCapabilities.MANA_RECEIVER).resolve()
                .filter(ManaPool.class::isInstance).map(ManaPool.class::cast).orElse(null);
    }

    public void setButterflyInputBinding(@Nullable BlockPos pos) {
        if (butterflyActive || butterflyRefundMana > 0 || butterflyInputReserve > 0) return;
        if (pos != null && pos.equals(getBinding())) return;
        butterflyInputBinding = pos == null ? null : pos.immutable();
        butterflyInputDimension = level == null ? "" : level.dimension().location().toString();
        setChanged();
        sync();
    }

    @Nullable public BlockPos getButterflyInputBinding() { return butterflyInputBinding; }

    public boolean bindButterflyInputToNearestPool() {
        if (level == null || butterflyActive || butterflyRefundMana > 0 || butterflyInputReserve > 0) return false;
        BlockPos closest = null;
        double closestDistance = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(worldPosition.offset(-8, -2, -8),
                worldPosition.offset(8, 2, 8))) {
            if (!level.hasChunkAt(pos) || pos.equals(worldPosition) || pos.equals(getBinding())) continue;
            if (pos.distSqr(worldPosition) > 64) continue;
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity == null || blockEntity.getCapability(BotaniaForgeCapabilities.MANA_RECEIVER)
                    .resolve().filter(ManaPool.class::isInstance).isEmpty()) continue;
            double distance = pos.distSqr(worldPosition);
            if (distance < closestDistance) {
                closest = pos.immutable();
                closestDistance = distance;
            }
        }
        if (closest == null) return false;
        setButterflyInputBinding(closest);
        return true;
    }

    private void tickAftertaste() {
        if (burnRemaining == 0 && pendingMana == 0 && getMana() <= getMaxMana() - 400 && takeResidue()) burnRemaining = 40;
        if (burnRemaining > 0) {
            burnRemaining--;
            if (burnRemaining == 0) pendingMana += 400;
        }
        if (pendingMana > 0) {
            int accepted = Math.min(pendingMana, getMaxMana() - getMana());
            if (accepted > 0) {
                addMana(accepted);
                pendingMana -= accepted;
            }
        }
    }

    public boolean bindResidue(BlockPos pos, Direction side, String dimension) {
        if (level == null || !level.dimension().location().toString().equals(dimension) || !level.hasChunkAt(pos)
                || pos.distSqr(worldPosition) > 64) return false;
        var be = level.getBlockEntity(pos);
        if (be == null || !be.getCapability(ForgeCapabilities.ITEM_HANDLER, side).isPresent()) return false;
        residueSource = pos.immutable(); residueDimension = dimension; residueSide = side; setChanged(); return true;
    }

    private boolean takeResidue() {
        if (residueSource != null && residueDimension.equals(level.dimension().location().toString())
                && residueSource.distSqr(worldPosition) <= 64 && level.hasChunkAt(residueSource)) {
            var source = level.getBlockEntity(residueSource);
            var inventory = source == null ? null : source.getCapability(ForgeCapabilities.ITEM_HANDLER, residueSide).orElse(null);
            if (inventory != null) for (int slot = 0; slot < inventory.getSlots(); slot++) {
                if (inventory.extractItem(slot, 1, true).is(ModItemTags.TEA_RESIDUES)
                        && inventory.extractItem(slot, 1, false).is(ModItemTags.TEA_RESIDUES)) return true;
            }
        }
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class,
                new AABB(worldPosition).inflate(2))) {
            if (!item.getItem().is(ModItemTags.TEA_RESIDUES)) continue;
            item.getItem().shrink(1);
            setChanged();
            return true;
        }
        return false;
    }


    public net.minecraft.network.chat.Component harmonyStatus() {
        var next = new FluidStack(ModTeaFluids.SOURCES.get(SIX_HARMONY_SEQUENCE[harmonyIndex]).get(), 1000);
        return net.minecraft.network.chat.Component.translatable("message.botania_tea.harmony_next",
                next.getDisplayName(), harmonyIndex);
    }

    private boolean processHarmony() {
        TeaLiquor liquor = SIX_HARMONY_SEQUENCE[harmonyIndex];
        int reward = harmonyIndex == SIX_HARMONY_SEQUENCE.length - 1 ? 76800 - harmonyLegacyPaid : 0;
        if (getMana() + reward > getMaxMana()) return false;
        FluidStack request = new FluidStack(ModTeaFluids.SOURCES.get(liquor).get(), 1000);
        Source source = findSource(request);
        if (source == null) return false;
        FluidStack drained = source.handler.drain(request, IFluidHandler.FluidAction.EXECUTE);
        if (drained.getAmount() != 1000 || !drained.isFluidEqual(request)) return false;
        if (reward > 0) { addMana(reward); harmonyLegacyPaid = 0; }
        harmonyIndex = (harmonyIndex + 1) % SIX_HARMONY_SEQUENCE.length;
        setChanged();
        sync();
        return true;
    }

    public boolean isSealed() { return sealed; }

    public boolean seal() {
        if (sealed) return false;
        sealed = true;
        setChanged();
        sync();
        return true;
    }

    @Nullable private Source findSource(FluidStack request) {
        for (BlockPos pos : BlockPos.betweenClosed(worldPosition.offset(-1, -2, -1),
                worldPosition.offset(1, 2, 1))) {
            if (pos.equals(worldPosition) || !level.hasChunkAt(pos)) continue;
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity == null) continue;
            IFluidHandler handler = blockEntity.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.UP)
                    .resolve().orElse(null);
            if (handler == null) continue;
            FluidStack simulated = handler.drain(request, IFluidHandler.FluidAction.SIMULATE);
            if (simulated.getAmount() == request.getAmount() && simulated.isFluidEqual(request)) {
                return new Source(handler);
            }
        }
        return null;
    }

    private void catchProjectile() {
        if (getMana() >= getMaxMana()) return;
        List<Projectile> projectiles = level.getEntitiesOfClass(Projectile.class,
                new AABB(worldPosition).inflate(2),
                entity -> entity instanceof net.minecraft.world.entity.projectile.Arrow || entity instanceof SmallFireball || entity instanceof Fireball);
        for (Projectile projectile : projectiles) {
            double speed = projectile.getDeltaMovement().length();
            int reward = (int) Math.ceil(500.0D * speed * speed);
            if (!Double.isFinite(speed) || reward <= 0 || reward > getMaxMana() - getMana()) continue;
            addMana(reward);
            projectile.discard();
            AABB damageArea = new AABB(worldPosition.offset(-7, -2, -7), worldPosition.offset(8, 3, 8));
            var damage = new net.minecraft.world.damagesource.DamageSource(level.registryAccess()
                    .registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE).getHolderOrThrow(
                            net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE,
                                    ResourceLocation.fromNamespaceAndPath("botania_tea", "projectile_capture"))));
            for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, damageArea,
                    living -> !(living instanceof net.minecraft.world.entity.player.Player))) {
                entity.hurt(damage, 5.0F);
            }
            setChanged();
            return;
        }
    }

    @Override public void writeToPacketNBT(CompoundTag tag) {
        super.writeToPacketNBT(tag);
        CompoundTag shanhai = tag.getCompound("Shanhai");
        shanhai.putInt("Version", NBT_VERSION);
        shanhai.putBoolean("Sealed", sealed);
        CompoundTag runtime = shanhai.getCompound("Runtime");
        runtime.putInt("WorkCredit", workCredit);
        if (residueSource != null) {
            var config = shanhai.getCompound("Config");
            config.putLong("ResidueSource", residueSource.asLong()); config.putString("ResidueDimension", residueDimension);
            config.putInt("ResidueSide", residueSide.ordinal()); shanhai.put("Config", config);
        }
        runtime.putInt("BurnRemaining", burnRemaining);
        runtime.putInt("PendingMana", pendingMana);
        runtime.putInt("HarmonyIndex", harmonyIndex);
        runtime.putInt("HarmonyVersion", 2);
        runtime.putInt("HarmonyLegacyPaid", harmonyLegacyPaid);
        runtime.putBoolean("ButterflyActive", butterflyActive);
        runtime.putInt("ButterflyCooldown", butterflyCooldown);
        runtime.putBoolean("ButterflyEventPending", butterflyEventPending);
        runtime.putInt("ButterflyUnrest", butterflyUnrest);
        runtime.putInt("ButterflyEventType", butterflyEventType);
        runtime.putString("ButterflyInputDimension", butterflyInputDimension);
        runtime.putInt("ButterflyRefundMana", butterflyRefundMana);
        if (butterflyInputBinding != null) runtime.putLong("ButterflyInputBinding", butterflyInputBinding.asLong());
        runtime.putInt("ButterflyTargetIndex", butterflyTargetIndex);
        runtime.putInt("ButterflyVisitRemaining", butterflyVisitRemaining);
        runtime.putInt("VisitTicks",butterflyVisitTicks);
        runtime.putInt("UnitCost",butterflyUnitCost);
        runtime.putInt("UnitReward",butterflyUnitReward);
        runtime.putDouble("DiversityBonus",butterflyDiversityBonus);
        if (butterflyActive && butterflyTargetIndex < butterflyTargets.size()) {
            runtime.putLong("VisualTarget", butterflyTargets.get(butterflyTargetIndex).pos.asLong());
            runtime.putInt("VisualRemaining", butterflyVisitRemaining);
        }
        runtime.putInt("ButterflyMissingTicks", butterflyMissingTicks);
        runtime.putInt("ButterflyInputReserve", butterflyInputReserve);
        runtime.putInt("ButterflyConsumedCount", butterflyConsumedCount);
        if (butterflyLastTea != null) runtime.putString("ButterflyLastTea", butterflyLastTea);
        ListTag targetList = new ListTag();
        for (ButterflyTarget target : butterflyTargets) {
            CompoundTag entry = new CompoundTag();
            entry.putLong("Pos", target.pos.asLong());
            entry.putString("Expected", target.expected.toString());
            targetList.add(entry);
        }
        runtime.put("ButterflyTargets", targetList);
        ListTag typeList = new ListTag();
        for (String type : butterflyConsumedTypes) typeList.add(net.minecraft.nbt.StringTag.valueOf(type));
        runtime.put("ButterflyConsumedTypes", typeList);
        if (butterflyCycleId != null) runtime.putUUID("CycleId", butterflyCycleId);
        shanhai.put("Runtime", runtime);
        tag.put("Shanhai", shanhai);
    }

    @Override public void readFromPacketNBT(CompoundTag tag) {
        super.readFromPacketNBT(tag);
        CompoundTag shanhai = tag.getCompound("Shanhai");
        sealed = shanhai.getBoolean("Sealed") || tag.getBoolean("ShanhaiSealed");
        CompoundTag runtime = shanhai.getCompound("Runtime");
        workCredit = Math.max(0, runtime.getInt("WorkCredit"));
        var config = shanhai.getCompound("Config");
        residueSource = config.contains("ResidueSource") ? BlockPos.of(config.getLong("ResidueSource")) : null;
        residueDimension = config.getString("ResidueDimension"); residueSide = Direction.from3DDataValue(config.getInt("ResidueSide"));
        burnRemaining = Math.max(0, runtime.getInt("BurnRemaining"));
        pendingMana = Math.max(0, runtime.getInt("PendingMana"));
        harmonyIndex = Math.max(0, Math.min(SIX_HARMONY_SEQUENCE.length - 1, runtime.getInt("HarmonyIndex")));
        harmonyLegacyPaid = runtime.getInt("HarmonyVersion") < 2 ? harmonyIndex * 800
                : Math.max(0, Math.min(4000, runtime.getInt("HarmonyLegacyPaid")));
        butterflyActive = runtime.getBoolean("ButterflyActive");
        butterflyCooldown = Math.max(0, runtime.getInt("ButterflyCooldown"));
        butterflyEventPending = runtime.getBoolean("ButterflyEventPending");
        butterflyUnrest = Math.max(0, runtime.getInt("ButterflyUnrest"));
        butterflyEventType = runtime.contains("ButterflyEventType") ? runtime.getInt("ButterflyEventType") : -1;
        butterflyInputDimension = runtime.getString("ButterflyInputDimension");
        butterflyRefundMana = Math.max(0, runtime.getInt("ButterflyRefundMana"));
        butterflyInputBinding = runtime.contains("ButterflyInputBinding")
                ? BlockPos.of(runtime.getLong("ButterflyInputBinding")) : null;
        butterflyTargetIndex = Math.max(0, runtime.getInt("ButterflyTargetIndex"));
        butterflyVisitRemaining = Math.max(0, runtime.getInt("ButterflyVisitRemaining"));
        butterflyVisitTicks = runtime.contains("VisitTicks") ? Math.max(1,runtime.getInt("VisitTicks")) : 40;
        butterflyUnitCost = runtime.contains("UnitCost") ? Math.max(0,runtime.getInt("UnitCost")) : 300;
        butterflyUnitReward = runtime.contains("UnitReward") ? Math.max(1,runtime.getInt("UnitReward")) : 2500;
        butterflyDiversityBonus = runtime.contains("DiversityBonus") ? Math.max(0,runtime.getDouble("DiversityBonus")) : .25D;
        butterflyVisualTarget = runtime.contains("VisualTarget") ? BlockPos.of(runtime.getLong("VisualTarget")) : null;
        butterflyVisualRemaining = Math.max(0, runtime.getInt("VisualRemaining"));
        butterflyVisualReceivedAt = ticksExisted;
        butterflyMissingTicks = Math.max(0, runtime.getInt("ButterflyMissingTicks"));
        butterflyInputReserve = Math.max(0, runtime.getInt("ButterflyInputReserve"));
        butterflyConsumedCount = Math.max(0, runtime.getInt("ButterflyConsumedCount"));
        butterflyLastTea = runtime.contains("ButterflyLastTea") ? runtime.getString("ButterflyLastTea") : null;
        butterflyTargets.clear();
        ListTag targetList = runtime.getList("ButterflyTargets", Tag.TAG_COMPOUND);
        for (int i = 0; i < targetList.size() && i < 64; i++) {
            CompoundTag entry = targetList.getCompound(i);
            ResourceLocation expected = ResourceLocation.tryParse(entry.getString("Expected"));
            if (expected != null) butterflyTargets.add(new ButterflyTarget(BlockPos.of(entry.getLong("Pos")), expected));
        }
        butterflyConsumedTypes.clear();
        ListTag typeList = runtime.getList("ButterflyConsumedTypes", Tag.TAG_STRING);
        for (int i = 0; i < typeList.size(); i++) butterflyConsumedTypes.add(typeList.getString(i));
        butterflyCycleId = runtime.hasUUID("CycleId") ? runtime.getUUID("CycleId") : null;
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        CompoundTag shanhai = tag.getCompound("Shanhai");
        CompoundTag runtime = shanhai.getCompound("Runtime");
        runtime.remove("ButterflyEventPending");
        runtime.remove("ButterflyEventType");
        runtime.remove("ButterflyInputDimension");
        runtime.remove("ButterflyUnrest");
        runtime.remove("ButterflyRefundMana");
        runtime.remove("ButterflyInputBinding");
        runtime.remove("ButterflyTargetIndex");
        runtime.remove("ButterflyVisitRemaining");
        runtime.remove("ButterflyMissingTicks");
        runtime.remove("ButterflyInputReserve");
        runtime.remove("ButterflyConsumedCount");
        runtime.remove("ButterflyLastTea");
        runtime.remove("ButterflyTargets");
        runtime.remove("ButterflyConsumedTypes");
        runtime.remove("CycleId");
        shanhai.put("Runtime", runtime);
        tag.put("Shanhai", shanhai);
        return tag;
    }

    private record ButterflyTarget(BlockPos pos, ResourceLocation expected) {}
    private record Source(IFluidHandler handler) {}
}

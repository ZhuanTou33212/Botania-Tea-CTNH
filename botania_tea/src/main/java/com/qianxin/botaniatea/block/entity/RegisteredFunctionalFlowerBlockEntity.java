package com.qianxin.botaniatea.block.entity;

import com.qianxin.botaniatea.registry.ModBlockEntities;
import com.qianxin.botaniatea.config.TeaGardenConfig;
import com.qianxin.botaniatea.content.TeaFlower;
import com.qianxin.botaniatea.content.TeaEssence;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.world.ForgeChunkManager;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.level.block.state.BlockState;
import vazkii.botania.api.block_entity.FunctionalFlowerBlockEntity;
import vazkii.botania.api.block_entity.RadiusDescriptor;

import java.util.List;

/** Shared bindable functional flower entity; concrete work is dispatched by its flower identity. */
public final class RegisteredFunctionalFlowerBlockEntity extends FunctionalFlowerBlockEntity {
    private static final int NBT_VERSION = 1;
    private boolean sealed;
    private int workCredit;
    private TeaEssence selectedEssence;
    private boolean domainActive;
    private boolean chunkForced;
    private int supplyCursor;
    private int leaseRemaining;
    private int instantRemaining;
    private final FlowerWork extraWork = new FlowerWork(this);
    private final java.util.List<net.minecraft.world.effect.MobEffectInstance> effects = new java.util.ArrayList<>();
    public FlowerWork extraWork() { return extraWork; }
    private final FluidTank configuredFluid = new FluidTank(1000, stack -> !stack.isEmpty()) {
        @Override public boolean isFluidValid(FluidStack stack) {
            return !stack.isEmpty() && (flower() != TeaFlower.HETEROMORPHIC_CAMELLIA
                    || TeaEssence.fromEssenceFluid(stack.getFluid()) != null);
        }
        @Override protected void onContentsChanged() {
            if (flower() == TeaFlower.HETEROMORPHIC_CAMELLIA && !isEmpty())
                selectedEssence = TeaEssence.fromEssenceFluid(getFluid().getFluid());
            setChanged();
        }
    };
    private LazyOptional<IFluidHandler> fluidCap = LazyOptional.of(() -> configuredFluid);

    public RegisteredFunctionalFlowerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.REGISTERED_FUNCTIONAL_FLOWER.get(), pos, state);
    }

    @Override public int getMaxMana() { return TeaGardenConfig.FUNCTIONAL_MANA_CAPACITY.get(); }
    @Override public void drawManaFromPool() {
        TeaFlower kind = flower();
        if (kind == TeaFlower.WATER_SUPPLY_LILY || kind == TeaFlower.GAUGING_HYDRANGEA
                || kind == TeaFlower.GUARDIAN_LOTUS || kind == TeaFlower.NIGHT_EPIPHYLLUM) return;
        super.drawManaFromPool();
    }
    @Override public int getColor() { return 0x82B45F; }
    @Override public RadiusDescriptor getRadius() {
        return new RadiusDescriptor.Circle(worldPosition, effectiveRadius(TeaGardenConfig.WORK_RADIUS.get()));
    }

    public boolean isChibi() { return TeaFlower.isChibi(getBlockState().getBlock()); }
    public int effectiveRadius(int normalRadius) {
        TeaFlower kind = flower();
        return kind == null ? normalRadius : kind.effectiveRadius(normalRadius, isChibi());
    }

    @Override public void tickFlower() {
        super.tickFlower();
        if (level == null || level.isClientSide) return;
        TeaFlower flower = TeaFlower.fromBlock(getBlockState().getBlock());
        if (flower == null) return;
        if (flower == TeaFlower.CHUNK_DANDELION) {
            if (leaseRemaining > 0) leaseRemaining--;
            if (leaseRemaining == 0 && (chunkForced || ticksExisted % 20 == 0)) maintainChunkLease();
            setChanged();
            return;
        }
        if (instantRemaining > 0) instantRemaining--;
        int period = switch (flower) {
            case HETEROMORPHIC_CAMELLIA -> TeaGardenConfig.WORK_PERIOD.get();
            case WATER_SUPPLY_LILY -> TeaGardenConfig.SUPPLY_PERIOD.get();
            case GAUGING_HYDRANGEA -> TeaGardenConfig.MEASURE_PERIOD.get();
            default -> TeaGardenConfig.COMMON_PERIOD.get();
        };
        workCredit = Math.min(period, workCredit) + (sealed ? TeaGardenConfig.SEALED_WORK_RATE.get() : 1);
        for (int actions = 0; workCredit >= period && actions < 11; actions++) {
            if (performWork(flower)) {
                workCredit -= period;
            } else {
                workCredit = period;
                break;
            }
        }
        setChanged();
    }

    private boolean performWork(TeaFlower flower) {
        if (flower == TeaFlower.HETEROMORPHIC_CAMELLIA) {
            if (!configuredFluid.isEmpty()) selectedEssence = TeaEssence.fromEssenceFluid(configuredFluid.getFluid().getFluid());
            if (selectedEssence == null || configuredFluid.getFluidAmount() < 250 || getMana() < TeaGardenConfig.CONVERSION_COST.get()) return false;
            if (configuredFluid.getFluid().getFluid() != com.qianxin.botaniatea.registry.ModFluids.SOURCES.get(selectedEssence).get()) return false;
            BlockPos target = findTeaTarget();
            if (target == null) return false;
            BlockState original = level.getBlockState(target);
            var crop = com.qianxin.botaniatea.registry.ModBlocks.FINISHED_TEA_TREES
                    .get(com.qianxin.teaart.content.FinishedTea.valueOf(selectedEssence.name())).get();
            int age = original.getBlock() instanceof net.minecraft.world.level.block.CropBlock sourceCrop
                    ? sourceCrop.getAge(original) : 0;
            if (!level.setBlock(target, crop.getStateForAge(Math.min(age, crop.getMaxAge())), 3)) return false;
            configuredFluid.drain(250, IFluidHandler.FluidAction.EXECUTE);
            addMana(-TeaGardenConfig.CONVERSION_COST.get());
            if (level instanceof net.minecraft.server.level.ServerLevel server)
                server.sendParticles(net.minecraft.core.particles.ParticleTypes.COMPOSTER,
                        target.getX() + .5, target.getY() + .5, target.getZ() + .5, 18, .35, .4, .35, .02);
            return true;
        }
        if (flower == TeaFlower.WATER_SUPPLY_LILY) return supplyFluid();
        if (flower == TeaFlower.CLEANSING_ANGELICA) { maintainAngelica(); return domainActive; }
        if (flower == TeaFlower.GUARDIAN_LOTUS || flower == TeaFlower.NIGHT_EPIPHYLLUM) {
            setDomainActive(true);
            return domainActive;
        }
        return extraWork.work();
    }

    @Nullable
    private BlockPos findTeaTarget() {
        int radius = effectiveRadius(TeaGardenConfig.WORK_RADIUS.get());
        BlockPos min = worldPosition.offset(-radius, -TeaGardenConfig.WORK_HEIGHT.get() / 2, -radius);
        BlockPos max = worldPosition.offset(radius, TeaGardenConfig.WORK_HEIGHT.get() / 2, radius);
        BlockPos closest = null;
        double closestDistance = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            if (!level.hasChunkAt(pos)) continue;
            BlockState state = level.getBlockState(pos);
            boolean teaCrop = state.getBlock() instanceof com.qianxin.teaart.block.TeaCropBlock
                    || state.getBlock() instanceof com.qianxin.teaart.block.TeaPlantBlock;
            boolean farmlandCrop = state.getBlock() instanceof net.minecraft.world.level.block.CropBlock
                    && level.getBlockState(pos.below()).getBlock() instanceof net.minecraft.world.level.block.FarmBlock;
            if ((!teaCrop && !farmlandCrop) || state.hasBlockEntity()) continue;
            if (state.is(com.qianxin.botaniatea.registry.ModBlocks.FINISHED_TEA_TREES
                    .get(com.qianxin.teaart.content.FinishedTea.valueOf(selectedEssence.name())).get())) continue;
            double distance = pos.distSqr(worldPosition);
            if (distance < closestDistance) {
                closest = pos.immutable();
                closestDistance = distance;
            }
        }
        return closest;
    }

    private boolean supplyFluid() {
        if (!(level instanceof ServerLevel server)) return false;
        Fluid selected = configuredFluid.isEmpty() ? Fluids.WATER : configuredFluid.getFluid().getFluid();
        FluidStack fluid = selected.defaultFluidState().isSource()
                ? new FluidStack(selected, TeaGardenConfig.SUPPLY_AMOUNT.get()) : FluidStack.EMPTY;
        if (fluid.isEmpty()) return false;
        int accepted = 0;
        int radius = effectiveRadius(TeaGardenConfig.WORK_RADIUS.get());
        BlockPos min = worldPosition.offset(-radius, -2, -radius);
        BlockPos max = worldPosition.offset(radius, 2, radius);
        var positions = new java.util.ArrayList<BlockPos>();
        BlockPos.betweenClosed(min, max).forEach(pos -> positions.add(pos.immutable()));
        for (int n = 0; n < positions.size(); n++) {
            int index = Math.floorMod(supplyCursor + n, positions.size());
            BlockPos pos = positions.get(index);
            if (pos.equals(worldPosition) || !level.hasChunkAt(pos)) continue;
            net.minecraft.world.level.block.entity.BlockEntity target = level.getBlockEntity(pos);
            if (target == null) continue;
            if (target instanceof vazkii.botania.api.block.PetalApothecary altar) {
                // Botania 452 stores a basin state, not mB: one refill is an entire basin.
                if (selected != Fluids.WATER || altar.getFluid() != vazkii.botania.api.block.PetalApothecary.State.EMPTY) continue;
                altar.setFluid(vazkii.botania.api.block.PetalApothecary.State.WATER);
                if (altar.getFluid() != vazkii.botania.api.block.PetalApothecary.State.WATER) continue;
                accepted = 1000;
            } else {
                IFluidHandler handler = target.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.UP)
                        .resolve().orElse(null);
                if (handler == null) continue;
                int simulated = handler.fill(fluid, IFluidHandler.FluidAction.SIMULATE);
                if (simulated <= 0) continue;
                accepted = handler.fill(new FluidStack(fluid.getFluid(), simulated), IFluidHandler.FluidAction.EXECUTE);
            }
            if (accepted > 0) {
                supplyCursor = (index + 1) % positions.size();
                var particle = selected == Fluids.WATER ? net.minecraft.core.particles.ParticleTypes.SPLASH
                        : selected == Fluids.LAVA ? net.minecraft.core.particles.ParticleTypes.DRIPPING_LAVA
                        : net.minecraft.core.particles.ParticleTypes.END_ROD;
                server.sendParticles(particle, pos.getX() + .5, pos.getY() + 1.05, pos.getZ() + .5,
                        8, .25, .08, .25, .02);
                break;
            }
        }
        if (accepted <= 0) return false;
        return true;
    }

    private void maintainAngelica() {
        int radius = effectiveRadius(5);
        List<Player> players = level.getEntitiesOfClass(Player.class,
                new AABB(worldPosition.offset(-radius, -2, -radius), worldPosition.offset(radius + 1, 3, radius + 1)),
                player -> player.isAlive() && !player.isSpectator());
        var selected = effects.isEmpty() ? java.util.List.of(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.DIG_SPEED, 60, 1)) : effects;
        int cost = TeaGardenConfig.ANGELICA_BASE_COST.get() + players.size() * selected.size() * TeaGardenConfig.ANGELICA_EFFECT_COST.get();
        if (!spendMana(cost)) {
            setDomainActive(false);
            return;
        }
        setDomainActive(true);
        boolean instant = instantRemaining == 0;
        for (Player player : players) {
            for (var effect : selected) {
                if (effect.getEffect().isInstantenous()) {
                    if (instant && spendMana(TeaGardenConfig.INSTANT_COST.get())) effect.getEffect().applyInstantenousEffect(null, null, player, effect.getAmplifier(), 1.0);
                } else player.addEffect(new net.minecraft.world.effect.MobEffectInstance(effect.getEffect(), 60, effect.getAmplifier()));
            }
        }
        if (instant) instantRemaining = TeaGardenConfig.INSTANT_PERIOD.get();
        if (level instanceof ServerLevel server) com.qianxin.botaniatea.world.TeaWorldEventData.get(server)
                .clearFog(server, new AABB(worldPosition.offset(-radius, -2, -radius), worldPosition.offset(radius + 1, 3, radius + 1)));
    }

    public void configureEffects(java.util.List<net.minecraft.world.effect.MobEffectInstance> values) {
        effects.clear();
        values.stream().limit(32).forEach(effect -> effects.add(new net.minecraft.world.effect.MobEffectInstance(effect)));
        instantRemaining = TeaGardenConfig.INSTANT_PERIOD.get();
        setChanged(); sync();
    }
    private void setDomainActive(boolean value) {
        domainActive = value;
        com.qianxin.botaniatea.server.TeaAreaIndex.update(this);
        if (flower() == TeaFlower.NIGHT_EPIPHYLLUM && getBlockState().hasProperty(com.qianxin.botaniatea.block.TeaSpecialFlowerBlock.LIT)
                && getBlockState().getValue(com.qianxin.botaniatea.block.TeaSpecialFlowerBlock.LIT) != value) {
            level.setBlock(worldPosition, getBlockState().setValue(com.qianxin.botaniatea.block.TeaSpecialFlowerBlock.LIT, value), 3);
        }
        setChanged();
    }
    @Override public void onLoad() {
        super.onLoad();
        com.qianxin.botaniatea.server.TeaAreaIndex.update(this);
        com.qianxin.botaniatea.server.HomingService.register(this);
    }
    @Override public void setRemoved() { com.qianxin.botaniatea.server.TeaAreaIndex.remove(this); super.setRemoved(); }
    @Override public void onChunkUnloaded() { com.qianxin.botaniatea.server.TeaAreaIndex.remove(this); super.onChunkUnloaded(); }

    private boolean spendMana(int amount) {
        if (getMana() < amount) return false;
        addMana(-amount);
        return true;
    }

    private void maintainChunkLease() {
        if (!(level instanceof ServerLevel serverLevel)) return;
        int cost = TeaGardenConfig.CHUNK_COST.get() * (sealed ? TeaGardenConfig.SEALED_WORK_RATE.get() : 1);
        if (!chunkForced && getMana() < cost) return;
        if (getMana() >= cost) {
            addMana(-cost);
            ForgeChunkManager.forceChunk(serverLevel, com.qianxin.botaniatea.BotaniaTeaMod.MOD_ID,
                    worldPosition, worldPosition.getX() >> 4, worldPosition.getZ() >> 4, true, true);
            chunkForced = true;
            leaseRemaining = 20;
        } else if (chunkForced) {
            ForgeChunkManager.forceChunk(serverLevel, com.qianxin.botaniatea.BotaniaTeaMod.MOD_ID,
                    worldPosition, worldPosition.getX() >> 4, worldPosition.getZ() >> 4, false, true);
            chunkForced = false;
        }
    }

    public boolean isDomainActive() { return domainActive; }
    public void releaseChunkLease() {
        if (level instanceof ServerLevel serverLevel) {
            ForgeChunkManager.forceChunk(serverLevel, com.qianxin.botaniatea.BotaniaTeaMod.MOD_ID,
                    worldPosition, worldPosition.getX() >> 4, worldPosition.getZ() >> 4, false, true);
        }
        chunkForced = false;
        leaseRemaining = 0;
        setChanged();
    }
    public TeaFlower flower() { return TeaFlower.fromBlock(getBlockState().getBlock()); }

    public boolean selectEssence(TeaEssence essence) {
        if (!configuredFluid.isEmpty() && selectedEssence != null && selectedEssence != essence) return false;
        FluidStack source = new FluidStack(com.qianxin.botaniatea.registry.ModFluids.SOURCES.get(essence).get(), 250);
        if (configuredFluid.fill(source, IFluidHandler.FluidAction.SIMULATE) != 250) return false;
        if (configuredFluid.fill(source, IFluidHandler.FluidAction.EXECUTE) != 250) return false;
        selectedEssence = essence;
        setChanged();
        return true;
    }

    public net.minecraft.network.chat.Component camelliaStatus() {
        String reason = configuredFluid.getFluidAmount() < 250 ? "essence"
                : TeaEssence.fromEssenceFluid(configuredFluid.getFluid().getFluid()) == null ? "wrong_fluid"
                : getMana() < TeaGardenConfig.CONVERSION_COST.get() ? "mana"
                : selectedEssence == null || findTeaTarget() == null ? "crop" : "ready";
        return net.minecraft.network.chat.Component.translatable("message.botania_tea.camellia." + reason);
    }

    public boolean setConfiguredFluid(FluidStack stack) {
        if (stack.isEmpty()) return false;
        configuredFluid.drain(1000, IFluidHandler.FluidAction.EXECUTE);
        return configuredFluid.fill(new FluidStack(stack.getFluid(), 1000), IFluidHandler.FluidAction.EXECUTE) == 1000;
    }

    public boolean isSealed() { return sealed; }

    public boolean seal() {
        if (sealed) return false;
        sealed = true;
        setChanged();
        sync();
        return true;
    }

    public int workCredit() { return workCredit; }

    @Override public void writeToPacketNBT(CompoundTag tag) {
        super.writeToPacketNBT(tag);
        CompoundTag shanhai = new CompoundTag();
        shanhai.putInt("Version", NBT_VERSION);
        shanhai.putBoolean("Sealed", sealed);
        CompoundTag config = new CompoundTag();
        if (selectedEssence != null) config.putString("TeaType", selectedEssence.id);
        config.put("Fluid", configuredFluid.writeToNBT(new CompoundTag()));
        shanhai.put("Config", config);
        CompoundTag runtime = new CompoundTag();
        runtime.putInt("WorkCredit", workCredit);
        runtime.putBoolean("DomainActive", domainActive);
        runtime.putBoolean("ChunkForced", chunkForced);
        runtime.putInt("SupplyCursor", supplyCursor);
        runtime.putInt("LeaseRemaining", leaseRemaining);
        runtime.putInt("InstantRemaining", instantRemaining);
        extraWork.save(config, runtime);
        net.minecraft.nbt.ListTag effectTags = new net.minecraft.nbt.ListTag();
        for (var effect : effects) effectTags.add(effect.save(new CompoundTag()));
        config.put("Effects", effectTags);
        shanhai.put("Config", config);
        shanhai.put("Runtime", runtime);
        tag.put("Shanhai", shanhai);
    }

    @Override public void readFromPacketNBT(CompoundTag tag) {
        super.readFromPacketNBT(tag);
        CompoundTag shanhai = tag.getCompound("Shanhai");
        sealed = shanhai.getBoolean("Sealed") || tag.getBoolean("ShanhaiSealed");
        CompoundTag config = shanhai.getCompound("Config");
        selectedEssence = TeaEssence.byId(config.getString("TeaType"));
        configuredFluid.readFromNBT(config.getCompound("Fluid"));
        CompoundTag runtime = shanhai.getCompound("Runtime");
        workCredit = Math.max(0, runtime.contains("WorkCredit")
                ? runtime.getInt("WorkCredit") : tag.getInt("ShanhaiWorkCredit"));
        domainActive = runtime.getBoolean("DomainActive");
        chunkForced = runtime.getBoolean("ChunkForced");
        supplyCursor = runtime.getInt("SupplyCursor");
        leaseRemaining = Math.max(0, Math.min(20, runtime.getInt("LeaseRemaining")));
        instantRemaining = Math.max(0, Math.min(TeaGardenConfig.INSTANT_PERIOD.get(), runtime.getInt("InstantRemaining")));
        extraWork.load(config, runtime);
        effects.clear();
        for (var entry : config.getList("Effects", 10)) {
            var effect = net.minecraft.world.effect.MobEffectInstance.load((CompoundTag) entry);
            if (effect != null && effects.size() < 32) effects.add(effect);
        }
    }

    @Override public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> capability,
                                                                 @Nullable Direction side) {
        // Only camellia consumes this tank; other flowers' saved fluid is configuration.
        if (capability == ForgeCapabilities.FLUID_HANDLER && flower() == TeaFlower.HETEROMORPHIC_CAMELLIA) return fluidCap.cast();
        return super.getCapability(capability, side);
    }

    @Override public void invalidateCaps() { super.invalidateCaps(); fluidCap.invalidate(); }
    @Override public void reviveCaps() { super.reviveCaps(); fluidCap = LazyOptional.of(() -> configuredFluid); }
}

package com.qianxin.botaniatea.block.entity;

import com.qianxin.botaniatea.content.TeaFlower;
import com.qianxin.botaniatea.config.TeaGardenConfig;
import com.qianxin.botaniatea.registry.ModBlocks;
import com.qianxin.botaniatea.server.BuildingSelection;
import com.qianxin.botaniatea.server.BuildingSelectionService;
import com.qianxin.botaniatea.server.BuildingSelectionPlacement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.IItemHandler;

/** Extra material bindings are separate from Botania's mana-pool binding. No cached capabilities. */
public final class FlowerWork {
    private final RegisteredFunctionalFlowerBlockEntity flower;
    private Endpoint source, target;
    private boolean line, fluids, less;
    private int threshold, cursor, signal;
    private net.minecraft.world.level.material.Fluid measuredFluid = net.minecraft.world.level.material.Fluids.WATER;
    private ItemStack pendingReturn = ItemStack.EMPTY;
    private final BuildingSelection buildingSelection = new BuildingSelection();

    public FlowerWork(RegisteredFunctionalFlowerBlockEntity flower) { this.flower = flower; }
    public int signal() { return signal; }
    public BuildingSelection buildingSelection() { return buildingSelection; }
    public void configureLine(boolean value) {
        line = value; cursor = 0;
        if (flower.flower() == TeaFlower.BUILDING_BEGONIA) buildingSelection.setMode(value ? BuildingSelection.Mode.LINE : BuildingSelection.Mode.SINGLE);
        flower.setChanged();
    }
    public void configureMeasure(boolean fluid, boolean below, int value) {
        fluids = fluid; less = below; threshold = Math.max(0, value); flower.setChanged();
    }
    public void selectMeasuredFluid(net.minecraft.world.level.material.Fluid fluid) {
        measuredFluid = fluid; fluids = true; flower.setChanged(); flower.sync();
    }
    public String summary() { return "line=" + line + ", measure=" + (fluids ? net.minecraftforge.registries.ForgeRegistries.FLUIDS.getKey(measuredFluid) + " mB" : "items") + (less ? " < " : " > ") + threshold; }
    public void toggleMode() {
        if (flower.flower() == TeaFlower.BUILDING_BEGONIA) {
            buildingSelection.setMode(buildingSelection.mode().next());
            line = buildingSelection.mode() == BuildingSelection.Mode.LINE;
            flower.setChanged(); flower.sync();
        }
        else configureMeasure(fluids, !less, threshold);
    }

    public boolean bind(BlockPos pos, Direction face, String dimension) {
        if (flower.getLevel() == null || !dimension.equals(flower.getLevel().dimension().location().toString())) return false;
        if (flower.flower() == TeaFlower.HOMING_MORNING_GLORY) {
            return flower.getLevel() instanceof ServerLevel level
                    && com.qianxin.botaniatea.server.HomingService.selectDestination(level, flower.getBlockPos(), pos);
        }
        if (flower.flower() == TeaFlower.BUILDING_BEGONIA) {
            if (!BuildingSelection.nearContainer(flower.getBlockPos(), pos) || !flower.getLevel().hasChunkAt(pos)
                    || !flower.getLevel().getWorldBorder().isWithinBounds(pos)) return false;
            var container = flower.getLevel().getBlockEntity(pos);
            if (container == null || !container.getCapability(ForgeCapabilities.ITEM_HANDLER, face).isPresent()) return false;
            source = new Endpoint(dimension, pos.immutable(), face);
            buildingSelection.stop(); flower.setChanged(); flower.sync(); return true;
        }
        int range = flower.effectiveRadius(8);
        if (!flower.getLevel().hasChunkAt(pos) || pos.distSqr(flower.getBlockPos()) > range * range) return false;
        Endpoint endpoint = new Endpoint(dimension, pos.immutable(), face);
        source = endpoint;
        flower.setChanged();
        return true;
    }

    public boolean work() {
        return switch (flower.flower()) {
            case BUILDING_BEGONIA -> build();
            case HOMING_MORNING_GLORY -> teleport();
            case GAUGING_HYDRANGEA -> measure();
            case MENDING_HIBISCUS -> mend();
            default -> false;
        };
    }

    private BlockEntity sourceEntity() {
        return valid(source, flower.effectiveRadius(8)) ? flower.getLevel().getBlockEntity(source.pos) : null;
    }
    private boolean valid(Endpoint endpoint, int range) {
        var level = flower.getLevel();
        return endpoint != null && level != null && endpoint.dim.equals(level.dimension().location().toString())
                && endpoint.pos.distSqr(flower.getBlockPos()) <= range * range && level.hasChunkAt(endpoint.pos)
                && level.getWorldBorder().isWithinBounds(endpoint.pos);
    }
    private IItemHandler sourceItems() {
        BlockEntity be = sourceEntity();
        return be == null ? null : be.getCapability(ForgeCapabilities.ITEM_HANDLER, source.face).orElse(null);
    }

    public boolean buildingSourceUsable(ServerPlayer player) {
        return source != null && valid(source, 3) && BuildingSelection.nearContainer(flower.getBlockPos(), source.pos)
                && player.level() == flower.getLevel() && BuildingSelectionService.canUseContainer(player, source.pos, source.face)
                && sourceItems() != null;
    }

    private boolean build() {
        if (!(flower.getLevel() instanceof ServerLevel level)) return false;
        // Refund escrow even after cancellation. Capability is always resolved afresh.
        IItemHandler inventory = source != null && BuildingSelection.nearContainer(flower.getBlockPos(), source.pos) ? sourceItems() : null;
        if (inventory != null && !pendingReturn.isEmpty()) {
            pendingReturn = ItemHandlerHelper.insertItemStacked(inventory, pendingReturn, false); flower.setChanged();
        }
        if (!level.hasNeighborSignal(flower.getBlockPos()) || !buildingSelection.active()
                || !buildingSelection.ready()) return false;
        var player = level.getPlayerByUUID(buildingSelection.owner());
        if (!(player instanceof ServerPlayer owner) || !owner.isAlive() || owner.isSpectator()
                || !buildingSelection.at(flower.getBlockPos(), level.dimension().location().toString())
                || owner.distanceToSqr(Vec3.atCenterOf(flower.getBlockPos())) > 24 * 24
                || !BuildingSelectionService.canInteract(owner, flower.getBlockPos()) || !buildingSourceUsable(owner)) {
            stopBuilding(); return false;
        }
        if (!pendingReturn.isEmpty() || inventory == null) return false;
        var targets = buildingSelection.targets(flower.getBlockPos());
        if (targets.isEmpty()) return false;
        if (!buildingSelection.running()) {
            buildingSelection.start(); flower.setChanged(); flower.sync();
            BuildingSelectionService.message(owner, "started", targets.size());
        }
        // Bounded scan only skips occupied cells; one successful placement per work action.
        while (buildingSelection.cursor() < targets.size()) {
            BlockPos pos = targets.get(buildingSelection.cursor());
            if (!level.hasChunkAt(pos) || !BuildingSelectionService.canInteract(owner, pos)
                    || pos.equals(flower.getBlockPos()) || !level.getBlockState(pos).isAir() || !level.getFluidState(pos).isEmpty()) {
                buildingSelection.advance(); continue;
            }
            if (flower.getMana() < TeaGardenConfig.BUILD_COST.get()) return false;
            for (int slot = 0; slot < inventory.getSlots(); slot++) {
                if (!BuildingSelectionPlacement.material(inventory.extractItem(slot, 1, true))) continue;
                ItemStack reserved = inventory.extractItem(slot, 1, false);
                if (reserved.isEmpty()) continue;
                // Escrow is persistent and survives a return handler that temporarily refuses insertion.
                pendingReturn = reserved.copy(); flower.setChanged();
                var result = BuildingSelectionPlacement.place(flower, owner, pos, reserved);
                pendingReturn = ItemHandlerHelper.insertItemStacked(inventory, result.remainder(), false);
                buildingSelection.advance(); flower.setChanged();
                if (result.placed()) flower.addMana(-TeaGardenConfig.BUILD_COST.get());
                else BuildingSelectionService.message(owner, "rejected");
                if (buildingSelection.cursor() >= targets.size()) finishBuilding(owner);
                else flower.sync();
                return result.placed();
            }
            return false; // Wait for materials; never repeatedly bill an occupied/completed cell.
        }
        finishBuilding(owner); return false;
    }

    private void stopBuilding() {
        var owner = buildingSelection.owner();
        if (owner != null) BuildingSelectionService.close(owner);
        buildingSelection.stop(); flower.setChanged(); flower.sync();
    }
    private void finishBuilding(ServerPlayer player) {
        stopBuilding(); BuildingSelectionService.message(player, "finished");
    }

    public boolean safeDestination() {
        return com.qianxin.botaniatea.server.HomingService.safeDestination(flower);
    }
    private boolean teleport() {
        return com.qianxin.botaniatea.server.HomingService.teleport(flower);
    }

    private boolean measure() {
        BlockEntity be = sourceEntity();
        if (be == null) { setSignal(0); return false; }
        long count = 0;
        if (fluids) {
            var handler = be.getCapability(ForgeCapabilities.FLUID_HANDLER, source.face).orElse(null);
            if (handler == null) { setSignal(0); return false; }
            for (int i = 0; i < handler.getTanks(); i++) {
                var fluid = handler.getFluidInTank(i);
                if (fluid.getFluid() == measuredFluid) count += fluid.getAmount();
            }
        } else {
            var handler = sourceItems();
            if (handler == null) { setSignal(0); return false; }
            for (int i = 0; i < handler.getSlots(); i++) count += handler.getStackInSlot(i).getCount();
        }
        setSignal((less ? count < threshold : count > threshold) ? 15 : 0);
        return true;
    }
    private void setSignal(int value) {
        if (signal == value) return;
        signal = value;
        flower.setChanged();
        flower.sync();
        flower.getLevel().updateNeighborsAt(flower.getBlockPos(), flower.getBlockState().getBlock());
    }
    private boolean mend() {
        if (!(flower.getLevel() instanceof ServerLevel) || flower.getMana() < TeaGardenConfig.MEND_COST.get()) return false;
        var inventory = mendingInventory(mendingPedestal());
        if (inventory == null) return false;
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            // Forge isRepairable is a crafting/grindstone rule, not a durability-recovery rule.
            if (!stack.isDamageableItem() || stack.getDamageValue() <= 0) continue;
            ItemStack repaired = stack.copy();
            repaired.setDamageValue(stack.getDamageValue() - 1);
            inventory.setStackInSlot(slot, repaired);
            flower.addMana(-TeaGardenConfig.MEND_COST.get());
            return true;
        }
        return false;
    }

    private BlockEntity mendingPedestal() {
        // An explicit binding stays authoritative, including when invalid/unloaded.
        if (source != null) return sourceEntity();
        var level = flower.getLevel();
        BlockPos origin = flower.getBlockPos();
        int range = flower.effectiveRadius(8);
        BlockEntity closest = null;
        double distance = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-range, -range, -range), origin.offset(range, range, range))) {
            double candidateDistance = pos.distSqr(origin);
            if (candidateDistance > range * range || candidateDistance >= distance || !level.hasChunkAt(pos)
                    || !level.getWorldBorder().isWithinBounds(pos)) continue;
            BlockEntity candidate = level.getBlockEntity(pos);
            var inventory = mendingInventory(candidate);
            if (inventory == null) continue;
            for (int slot = 0; slot < inventory.getSlots(); slot++) {
                ItemStack stack = inventory.getStackInSlot(slot);
                if (stack.isDamageableItem() && stack.getDamageValue() > 0) {
                    closest = candidate;
                    distance = candidateDistance;
                    break;
                }
            }
        }
        return closest;
    }

    private net.minecraftforge.items.IItemHandlerModifiable mendingInventory(BlockEntity be) {
        if (be instanceof TeaUtilityBlockEntity pedestal && be.getBlockState().is(ModBlocks.EQUIPMENT_PEDESTAL.get()))
            return pedestal.inventory();
        // Botania's documented item display has six real slots; do not accept arbitrary machines.
        if (be instanceof vazkii.botania.common.block.block_entity.TinyPotatoBlockEntity display)
            return new net.minecraftforge.items.wrapper.InvWrapper(display.getItemHandler());
        return null;
    }
    public void save(CompoundTag config, CompoundTag runtime) {
        buildingSelection.save(config, runtime);
        if (source != null) config.put("Source", source.save());
        // Homing links live in the per-dimension HomingNetworkData, not the old arbitrary endpoint.
        if (target != null && flower.flower() != TeaFlower.HOMING_MORNING_GLORY) config.put("Target", target.save());
        else if (flower.flower() == TeaFlower.HOMING_MORNING_GLORY) config.remove("Target");
        config.putBoolean("Line", line); config.putBoolean("MeasureFluid", fluids);
        config.putString("MeasuredFluid", net.minecraftforge.registries.ForgeRegistries.FLUIDS.getKey(measuredFluid).toString());
        config.putBoolean("Less", less); config.putInt("Threshold", threshold);
        runtime.putInt("Cursor", cursor); runtime.putInt("Signal", signal);
        runtime.put("PendingReturn", pendingReturn.save(new CompoundTag()));
    }
    public void load(CompoundTag config, CompoundTag runtime) {
        buildingSelection.load(config, runtime);
        source = Endpoint.load(config.getCompound("Source")); target = Endpoint.load(config.getCompound("Target"));
        if (flower.flower() == TeaFlower.HOMING_MORNING_GLORY) target = null;
        line = config.getBoolean("Line"); fluids = config.getBoolean("MeasureFluid"); less = config.getBoolean("Less");
        var fluidId = net.minecraft.resources.ResourceLocation.tryParse(config.getString("MeasuredFluid"));
        var fluid = fluidId == null ? null : net.minecraftforge.registries.ForgeRegistries.FLUIDS.getValue(fluidId);
        measuredFluid = fluid == null || fluid == net.minecraft.world.level.material.Fluids.EMPTY ? net.minecraft.world.level.material.Fluids.WATER : fluid;
        threshold = Math.max(0, config.getInt("Threshold")); cursor = Math.floorMod(runtime.getInt("Cursor"), 8);
        signal = runtime.getInt("Signal") == 15 ? 15 : 0; pendingReturn = ItemStack.of(runtime.getCompound("PendingReturn"));
    }
    private record Endpoint(String dim, BlockPos pos, Direction face) {
        CompoundTag save() {
            var tag = new CompoundTag(); tag.putString("Dimension", dim); tag.putLong("Pos", pos.asLong()); tag.putInt("Face", face.ordinal()); return tag;
        }
        static Endpoint load(CompoundTag tag) {
            return tag.contains("Pos") && tag.contains("Dimension") ? new Endpoint(tag.getString("Dimension"),
                    BlockPos.of(tag.getLong("Pos")), Direction.from3DDataValue(tag.getInt("Face"))) : null;
        }
    }
}

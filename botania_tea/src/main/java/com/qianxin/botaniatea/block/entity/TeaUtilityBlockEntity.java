package com.qianxin.botaniatea.block.entity;

import com.qianxin.botaniatea.registry.ModBlockEntities;
import com.qianxin.botaniatea.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

/** Two simple devices share serialization, but expose only the capability their block owns. */
public final class TeaUtilityBlockEntity extends BlockEntity {
    private final FluidTank tank = new FluidTank(8000) {
        @Override protected void onContentsChanged() { setChanged(); }
    };
    private final ItemStackHandler inventory = new ItemStackHandler(1) {
        @Override public int getSlotLimit(int slot) { return 1; }
        @Override protected void onContentsChanged(int slot) { setChanged(); }
    };
    private LazyOptional<IFluidHandler> fluidCap = LazyOptional.of(() -> tank);
    private LazyOptional<IItemHandler> itemCap = LazyOptional.of(() -> inventory);

    public TeaUtilityBlockEntity(BlockPos pos, BlockState state) {
        super(state.is(ModBlocks.TEA_SEAT.get()) ? ModBlockEntities.TEA_SEAT.get()
                : ModBlockEntities.EQUIPMENT_PEDESTAL.get(), pos, state);
    }

    public FluidTank tank() { return tank; }
    public ItemStackHandler inventory() { return inventory; }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Fluid", tank.writeToNBT(new CompoundTag()));
        tag.put("Inventory", inventory.serializeNBT());
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        tank.readFromNBT(tag.getCompound("Fluid"));
        inventory.deserializeNBT(tag.getCompound("Inventory"));
    }
    @Override public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER && getBlockState().is(ModBlocks.TEA_SEAT.get())) return fluidCap.cast();
        if (cap == ForgeCapabilities.ITEM_HANDLER && getBlockState().is(ModBlocks.EQUIPMENT_PEDESTAL.get())) return itemCap.cast();
        return super.getCapability(cap, side);
    }
    @Override public void invalidateCaps() { super.invalidateCaps(); fluidCap.invalidate(); itemCap.invalidate(); }
    @Override public void reviveCaps() {
        super.reviveCaps();
        fluidCap = LazyOptional.of(() -> tank);
        itemCap = LazyOptional.of(() -> inventory);
    }
}

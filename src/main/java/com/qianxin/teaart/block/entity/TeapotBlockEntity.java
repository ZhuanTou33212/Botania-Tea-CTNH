package com.qianxin.teaart.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;

import com.qianxin.teaart.lib.TeaArtProcessingTimes;
import com.qianxin.teaart.registry.ModBlockEntities;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TeapotBlockEntity extends BlockEntity {

    public static final int BREW_TIME_TICKS = TeaArtProcessingTimes.BREW_TIME_TICKS;

    private boolean hasWater;
    private int brewIndex = -1;
    private int brewTicks;
    private final FluidTank waterTank = new FluidTank(1000, stack -> stack.getFluid() == Fluids.WATER) {

        @Override
        protected void onContentsChanged() {
            hasWater = !isEmpty();
            setChanged();
        }
    };
    private LazyOptional<IFluidHandler> fluidCap = LazyOptional.of(() -> waterTank);

    public TeapotBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TEAPOT.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TeapotBlockEntity teapot) {
        if (level.isClientSide || teapot.brewTicks <= 0) {
            return;
        }

        teapot.brewTicks--;
        if (teapot.brewTicks == 0) {
            teapot.setChanged();
            level.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.7F, 1.25F);
        }
    }

    public boolean isEmpty() {
        return waterTank.isEmpty();
    }

    public boolean hasWaterOnly() {
        return hasWater && brewIndex < 0;
    }

    public boolean isSteeping() {
        return hasWater && brewIndex >= 0 && brewTicks > 0;
    }

    public boolean isReady() {
        return hasWater && brewIndex >= 0 && brewTicks <= 0;
    }

    public int getBrewIndex() {
        return brewIndex;
    }

    public int getSecondsRemaining() {
        return Math.max(1, (brewTicks + 19) / 20);
    }

    public void fillWater() {
        waterTank.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
        brewIndex = -1;
        brewTicks = 0;
        setChanged();
    }

    public void startBrew(int brewIndex) {
        this.brewIndex = brewIndex;
        brewTicks = BREW_TIME_TICKS;
        setChanged();
    }

    public void clear() {
        waterTank.drain(1000, IFluidHandler.FluidAction.EXECUTE);
        brewIndex = -1;
        brewTicks = 0;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean("HasWater", hasWater);
        tag.put("WaterTank", waterTank.writeToNBT(new CompoundTag()));
        tag.putInt("BrewIndex", brewIndex);
        tag.putInt("BrewTicks", brewTicks);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        hasWater = tag.getBoolean("HasWater");
        waterTank.readFromNBT(tag.getCompound("WaterTank"));
        if (hasWater && waterTank.isEmpty()) {
            waterTank.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
        }
        hasWater = !waterTank.isEmpty();
        brewIndex = tag.getInt("BrewIndex");
        brewTicks = tag.getInt("BrewTicks");
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> capability,
                                                      @Nullable net.minecraft.core.Direction side) {
        if (capability == ForgeCapabilities.FLUID_HANDLER) return fluidCap.cast();
        return super.getCapability(capability, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        fluidCap.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        fluidCap = LazyOptional.of(() -> waterTank);
    }
}

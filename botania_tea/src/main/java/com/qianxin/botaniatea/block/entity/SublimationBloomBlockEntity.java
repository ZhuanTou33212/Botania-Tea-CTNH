package com.qianxin.botaniatea.block.entity;

import com.qianxin.botaniatea.block.SublimationBloomBlock;
import com.qianxin.botaniatea.content.TeaEssence;
import com.qianxin.botaniatea.registry.ModBlockEntities;
import com.qianxin.botaniatea.registry.ModFluids;
import com.qianxin.botaniatea.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import vazkii.botania.api.block_entity.FunctionalFlowerBlockEntity;
import vazkii.botania.api.block_entity.RadiusDescriptor;

public final class SublimationBloomBlockEntity extends FunctionalFlowerBlockEntity {
    public static final int ACTIVE_INTERVAL = 150;
    public static final int IDLE_INTERVAL = 600;
    public static final int ESSENCE_PER_CYCLE = 250;
    public static final int MANA_COST = 1000;
    public static final int RANGE = 3;

    private int checkDelay;
    private boolean sealed;
    public boolean seal() { if (sealed) return false; sealed = true; setChanged(); sync(); return true; }
    private final FluidTank outputTank = new FluidTank(8000,
            stack -> stack.getFluid() == ModFluids.ASCENDED_TEA_LIQUOR.get()) {
        @Override protected void onContentsChanged() { setChanged(); }
    };
    private LazyOptional<IFluidHandler> fluidCap = LazyOptional.of(() -> outputTank);

    public SublimationBloomBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SUBLIMATION_BLOOM.get(), pos, state);
    }

    @Override
    public void tickFlower() {
        super.tickFlower();
        if (level == null || level.isClientSide) return;
        for (int i = 0; i < (sealed ? com.qianxin.botaniatea.config.TeaGardenConfig.SEALED_WORK_RATE.get() : 1); i++) processStep();
        setChanged();
    }

    private void processStep() {
        if (checkDelay-- > 0) return;
        Source source = findSource();
        boolean idle = source == null;
        setIdle(idle);
        checkDelay = idle ? IDLE_INTERVAL - 1 : ACTIVE_INTERVAL - 1;
        if (source == null || getMana() < MANA_COST) return;

        int yield = source.essence.ascendedYield();
        FluidStack result = new FluidStack(ModFluids.ASCENDED_TEA_LIQUOR.get(), yield);
        if (outputTank.fill(result, IFluidHandler.FluidAction.SIMULATE) != yield) return;
        FluidStack drained = source.handler.drain(
                new FluidStack(source.fluid, ESSENCE_PER_CYCLE),
                IFluidHandler.FluidAction.EXECUTE);
        if (drained.getAmount() != ESSENCE_PER_CYCLE) return;
        outputTank.fill(result, IFluidHandler.FluidAction.EXECUTE);
        addMana(-MANA_COST);
        sync();
    }

    @Nullable
    private Source findSource() {
        BlockPos min = worldPosition.offset(-RANGE, -RANGE, -RANGE);
        BlockPos max = worldPosition.offset(RANGE, RANGE, RANGE);
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            if (pos.equals(worldPosition) || !level.hasChunkAt(pos)) continue;
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity == null) continue;
            IFluidHandler handler = blockEntity.getCapability(ForgeCapabilities.FLUID_HANDLER).resolve().orElse(null);
            if (handler == null) continue;
            for (int tank = 0; tank < handler.getTanks(); tank++) {
                FluidStack stored = handler.getFluidInTank(tank);
                TeaEssence essence = TeaEssence.fromEssenceFluid(stored.getFluid());
                if (essence == null || stored.getAmount() < ESSENCE_PER_CYCLE) continue;
                FluidStack simulated = handler.drain(
                        new FluidStack(stored.getFluid(), ESSENCE_PER_CYCLE),
                        IFluidHandler.FluidAction.SIMULATE);
                int yield = essence.ascendedYield();
                FluidStack result = new FluidStack(ModFluids.ASCENDED_TEA_LIQUOR.get(), yield);
                if (simulated.getAmount() == ESSENCE_PER_CYCLE
                        && TeaEssence.fromEssenceFluid(simulated.getFluid()) == essence
                        && outputTank.fill(result, IFluidHandler.FluidAction.SIMULATE) == yield) {
                    return new Source(handler, essence, stored.getFluid());
                }
            }
        }
        return null;
    }

    private void setIdle(boolean idle) {
        BlockState state = getBlockState();
        if (state.hasProperty(SublimationBloomBlock.IDLE) && state.getValue(SublimationBloomBlock.IDLE) != idle) {
            level.setBlock(worldPosition, state.setValue(SublimationBloomBlock.IDLE, idle), 3);
        }
    }

    public ItemStack extractBottle() {
        if (outputTank.getFluidAmount() < 250) return ItemStack.EMPTY;
        outputTank.drain(250, IFluidHandler.FluidAction.EXECUTE);
        sync();
        return new ItemStack(ModItems.ASCENDED_TEA_LIQUOR_BOTTLE.get());
    }

    public IFluidHandler fluidHandler() { return outputTank; }

    @Override
    public void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override public RadiusDescriptor getRadius() { return new RadiusDescriptor.Circle(worldPosition, RANGE); }
    @Override public int getMaxMana() { return 10000; }
    @Override public int getColor() { return 0xFF9DBF; }

    @Override
    public void writeToPacketNBT(CompoundTag tag) {
        super.writeToPacketNBT(tag);
        tag.putInt("CheckDelay", checkDelay);
        var shanhai = tag.getCompound("Shanhai"); shanhai.putBoolean("Sealed", sealed); shanhai.putInt("Version", 1); tag.put("Shanhai", shanhai);
        tag.put("OutputTank", outputTank.writeToNBT(new CompoundTag()));
    }

    @Override
    public void readFromPacketNBT(CompoundTag tag) {
        super.readFromPacketNBT(tag);
        checkDelay = tag.getInt("CheckDelay");
        sealed = tag.getCompound("Shanhai").getBoolean("Sealed");
        outputTank.readFromNBT(tag.getCompound("OutputTank"));
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction side) {
        if (capability == ForgeCapabilities.FLUID_HANDLER) return fluidCap.cast();
        return super.getCapability(capability, side);
    }

    @Override public void invalidateCaps() { super.invalidateCaps(); fluidCap.invalidate(); }
    @Override public void reviveCaps() { super.reviveCaps(); fluidCap = LazyOptional.of(() -> outputTank); }

    private record Source(IFluidHandler handler, TeaEssence essence, net.minecraft.world.level.material.Fluid fluid) {}
}

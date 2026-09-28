package com.qianxin.botaniatea.block.entity;

import com.qianxin.botaniatea.content.ElementLiquidVector;
import com.qianxin.botaniatea.content.ElementType;
import com.qianxin.botaniatea.registry.ModBlockEntities;
import com.qianxin.botaniatea.registry.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;

public final class ElementTankBlockEntity extends BlockEntity {
    public static final int CAPACITY=8000;
    private final FluidTank tank=new FluidTank(CAPACITY,stack -> ModFluids.isElementEssence(stack.getFluid())) {
        @Override protected void onContentsChanged() {
            setChanged();
            if (level!=null && !level.isClientSide)
                level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);
        }
    };
    private LazyOptional<IFluidHandler> fluidCap=LazyOptional.of(() -> tank);

    public ElementTankBlockEntity(BlockPos pos,BlockState state) {
        super(ModBlockEntities.ELEMENT_TANK.get(),pos,state);
    }

    public FluidTank fluidHandler() { return tank; }
    public FluidStack fluid() { return tank.getFluid(); }
    public int capacity() { return tank.getCapacity(); }

    /** One source crystal always becomes exactly 100 mB of its matching liquid. */
    public boolean insertCrystal(ElementType type) {
        FluidStack offered=new FluidStack(ModFluids.ELEMENT_SOURCES.get(type).get(),ElementLiquidVector.MB_PER_ESSENCE);
        if (tank.fill(offered,IFluidHandler.FluidAction.SIMULATE)!=ElementLiquidVector.MB_PER_ESSENCE) return false;
        return tank.fill(offered,IFluidHandler.FluidAction.EXECUTE)==ElementLiquidVector.MB_PER_ESSENCE;
    }

    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Fluid",tank.writeToNBT(new CompoundTag()));
    }

    @Override public void load(CompoundTag tag) {
        super.load(tag);
        tank.readFromNBT(tag.getCompound("Fluid"));
    }

    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }

    @Override public <T> LazyOptional<T> getCapability(Capability<T> capability,@Nullable Direction side) {
        return capability==ForgeCapabilities.FLUID_HANDLER?fluidCap.cast():super.getCapability(capability,side);
    }

    @Override public void invalidateCaps() { super.invalidateCaps(); fluidCap.invalidate(); }
    @Override public void reviveCaps() { super.reviveCaps(); fluidCap=LazyOptional.of(() -> tank); }
}

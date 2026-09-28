package com.qianxin.botaniatea.block.entity;

import com.qianxin.botaniatea.block.TeaEssenceStillbloomBlock;
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

public final class TeaEssenceStillbloomBlockEntity extends FunctionalFlowerBlockEntity {
    public static final int ACTIVE_INTERVAL = 150;
    public static final int IDLE_INTERVAL = 600;
    public static final int TRANSFER_AMOUNT = 250;
    public static final int MANA_COST = 500;
    public static final int RANGE = 3;
    private int checkDelay;
    private boolean sealed;
    public boolean seal() { if (sealed) return false; sealed = true; setChanged(); sync(); return true; }
    private final FluidTank outputTank = new FluidTank(8000, stack -> essenceOf(stack) != null) {
        @Override protected void onContentsChanged() { setChanged(); }
    };
    private LazyOptional<IFluidHandler> fluidCap = LazyOptional.of(() -> outputTank);

    public TeaEssenceStillbloomBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TEA_ESSENCE_STILLBLOOM.get(), pos, state);
    }

    @Override public void tickFlower() {
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

        FluidStack essence = new FluidStack(ModFluids.SOURCES.get(source.essence).get(), TRANSFER_AMOUNT);
        if (outputTank.fill(essence, IFluidHandler.FluidAction.SIMULATE) != TRANSFER_AMOUNT) return;
        FluidStack drained = source.handler.drain(new FluidStack(source.essence.liquor(), TRANSFER_AMOUNT),
                IFluidHandler.FluidAction.EXECUTE);
        if (drained.getAmount() != TRANSFER_AMOUNT) return;
        outputTank.fill(essence, IFluidHandler.FluidAction.EXECUTE);
        addMana(-MANA_COST);
        sync();
    }

    @Nullable private Source findSource() {
        BlockPos min = worldPosition.offset(-RANGE, -RANGE, -RANGE);
        BlockPos max = worldPosition.offset(RANGE, RANGE, RANGE);
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            if (pos.equals(worldPosition) || !level.hasChunkAt(pos)) continue;
            BlockEntity be = level.getBlockEntity(pos);
            if (be == null) continue;
            LazyOptional<IFluidHandler> capability = be.getCapability(ForgeCapabilities.FLUID_HANDLER);
            if (!capability.isPresent()) continue;
            IFluidHandler handler = capability.orElseThrow(IllegalStateException::new);
            for (TeaEssence essence : TeaEssence.values()) {
                FluidStack simulated = handler.drain(new FluidStack(essence.liquor(), TRANSFER_AMOUNT),
                        IFluidHandler.FluidAction.SIMULATE);
                FluidStack result = new FluidStack(ModFluids.SOURCES.get(essence).get(), TRANSFER_AMOUNT);
                if (simulated.getAmount() == TRANSFER_AMOUNT
                        && outputTank.fill(result, IFluidHandler.FluidAction.SIMULATE) == TRANSFER_AMOUNT) {
                    return new Source(handler, essence);
                }
            }
        }
        return null;
    }

    private void setIdle(boolean idle) {
        BlockState state = getBlockState();
        if (state.hasProperty(TeaEssenceStillbloomBlock.IDLE) && state.getValue(TeaEssenceStillbloomBlock.IDLE) != idle) {
            level.setBlock(worldPosition, state.setValue(TeaEssenceStillbloomBlock.IDLE, idle), 3);
        }
    }

    public ItemStack extractBottle() {
        TeaEssence essence = essenceOf(outputTank.getFluid());
        if (essence == null || outputTank.getFluidAmount() < 250) return ItemStack.EMPTY;
        outputTank.drain(250, IFluidHandler.FluidAction.EXECUTE);
        return new ItemStack(ModItems.ESSENCE_BOTTLES.get(essence).get());
    }

    public IFluidHandler fluidHandler() { return outputTank; }

    @Nullable private static TeaEssence essenceOf(FluidStack stack) {
        if (stack.isEmpty()) return null;
        for (TeaEssence essence : TeaEssence.values()) if (stack.getFluid() == ModFluids.SOURCES.get(essence).get()) return essence;
        return null;
    }

    @Override public RadiusDescriptor getRadius() { return new RadiusDescriptor.Circle(worldPosition, RANGE); }
    @Override public int getMaxMana() { return 5000; }
    @Override public int getColor() { return 0x82B45F; }

    @Override public void writeToPacketNBT(CompoundTag tag) {
        super.writeToPacketNBT(tag);
        tag.putInt("CheckDelay", checkDelay);
        var shanhai = tag.getCompound("Shanhai"); shanhai.putBoolean("Sealed", sealed); shanhai.putInt("Version", 1); tag.put("Shanhai", shanhai);
        tag.put("OutputTank", outputTank.writeToNBT(new CompoundTag()));
    }

    @Override public void readFromPacketNBT(CompoundTag tag) {
        super.readFromPacketNBT(tag);
        checkDelay = tag.getInt("CheckDelay");
        sealed = tag.getCompound("Shanhai").getBoolean("Sealed");
        outputTank.readFromNBT(tag.getCompound("OutputTank"));
    }

    @Override public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction side) {
        if (capability == ForgeCapabilities.FLUID_HANDLER) return fluidCap.cast();
        return super.getCapability(capability, side);
    }

    @Override public void invalidateCaps() { super.invalidateCaps(); fluidCap.invalidate(); }
    @Override public void reviveCaps() { super.reviveCaps(); fluidCap = LazyOptional.of(() -> outputTank); }

    private record Source(IFluidHandler handler, TeaEssence essence) {}
}

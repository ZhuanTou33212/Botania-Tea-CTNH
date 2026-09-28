package com.qianxin.teaart.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;

import com.qianxin.teaart.item.TeaProcessingData;
import com.qianxin.teaart.registry.ModBlockEntities;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TeaSieveBlockEntity extends BlockEntity implements com.qianxin.teaart.api.TimedTeaDevice {

    @Override
    public boolean canAdvanceProcessing() {
        return !tea.isEmpty() && !finished && ticksRemaining > 0 && TeaProcessingData.canDry(tea);
    }

    @Override
    public boolean tryAdvanceProcessing(int ticks) {
        if (level == null || level.isClientSide || ticks <= 0 || !canAdvanceProcessing()) return false;
        for (int i = 0; i < Math.min(ticks, 12000) && canAdvanceProcessing(); i++)
            tick(level, worldPosition, getBlockState(), this);
        setChanged();
        return true;
    }

    public static final int DRYING_TICKS = com.qianxin.teaart.lib.TeaArtProcessingTimes.DRYING_TICKS;

    private ItemStack tea = ItemStack.EMPTY;
    private int ticksRemaining;
    private boolean finished;
    private final LazyOptional<IItemHandler> itemHandler = LazyOptional.of(() -> new Handler());

    public TeaSieveBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TEA_SIEVE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TeaSieveBlockEntity sieve) {
        if (level.isClientSide || sieve.tea.isEmpty() || sieve.finished) {
            return;
        }

        sieve.ticksRemaining--;
        if (sieve.ticksRemaining <= 0) {
            sieve.tea = TeaProcessingData.dry(sieve.tea);
            sieve.finished = true;
            sieve.sync();
            ItemStack result = sieve.takeTea();
            ItemStack remainder = BambooPipeBlockEntity.insertIntoAdjacentPipeNetwork(level, pos, result);
            if (!remainder.isEmpty()) {
                level.addFreshEntity(
                        new ItemEntity(level, pos.getX() + 0.5D, pos.getY() + 0.25D, pos.getZ() + 0.5D, remainder));
            }
        } else {
            sieve.setChanged();
        }
    }

    public boolean canStart(ItemStack stack) {
        return tea.isEmpty() && TeaProcessingData.canDry(stack);
    }

    public void startDrying(ItemStack stack) {
        tea = stack.copy();
        tea.setCount(1);
        ticksRemaining = DRYING_TICKS;
        finished = false;
        sync();
    }

    public boolean hasFinishedTea() {
        return finished && !tea.isEmpty();
    }

    public ItemStack takeTea() {
        ItemStack result = tea.copy();
        tea = ItemStack.EMPTY;
        ticksRemaining = 0;
        finished = false;
        sync();
        return result;
    }

    public void dropContents(Level level) {
        if (!tea.isEmpty()) {
            level.addFreshEntity(new ItemEntity(level, worldPosition.getX() + 0.5D, worldPosition.getY() + 0.25D,
                    worldPosition.getZ() + 0.5D, tea.copy()));
            tea = ItemStack.EMPTY;
            ticksRemaining = 0;
            finished = false;
        }
    }

    private void sync() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemHandler.invalidate();
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return itemHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (!tea.isEmpty()) {
            tag.put("Tea", tea.save(new CompoundTag()));
        }
        tag.putInt("TicksRemaining", ticksRemaining);
        tag.putBoolean("Finished", finished);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        tea = tag.contains("Tea") ? ItemStack.of(tag.getCompound("Tea")) : ItemStack.EMPTY;
        ticksRemaining = tag.getInt("TicksRemaining");
        finished = tag.getBoolean("Finished");
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    private class Handler implements IItemHandler {

        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            return tea;
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (!canStart(stack)) {
                return stack;
            }
            ItemStack remainder = stack.copy();
            ItemStack input = stack.copy();
            input.setCount(1);
            if (!simulate) {
                startDrying(input);
            }
            remainder.shrink(1);
            return remainder;
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (!hasFinishedTea() || amount <= 0) {
                return ItemStack.EMPTY;
            }
            ItemStack result = tea.copy();
            if (!simulate) {
                takeTea();
            }
            return result;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return canStart(stack);
        }
    }
}

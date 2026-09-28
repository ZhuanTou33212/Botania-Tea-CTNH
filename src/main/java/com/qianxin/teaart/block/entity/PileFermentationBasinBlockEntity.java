package com.qianxin.teaart.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
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
import com.qianxin.teaart.registry.ModItems;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PileFermentationBasinBlockEntity extends BlockEntity implements com.qianxin.teaart.api.TimedTeaDevice {

    @Override
    public boolean canAdvanceProcessing() {
        return !tea.isEmpty() && !done && ticksRemaining > 0 &&
                TeaProcessingData.canApply(tea, TeaProcessingData.PILED);
    }

    @Override
    public boolean tryAdvanceProcessing(int ticks) {
        if (level == null || level.isClientSide || ticks <= 0 || !canAdvanceProcessing()) return false;
        for (int i = 0; i < Math.min(ticks, 12000) && canAdvanceProcessing(); i++)
            tick(level, worldPosition, getBlockState(), this);
        setChanged();
        return true;
    }

    private static final int PILE_TICKS = com.qianxin.teaart.lib.TeaArtProcessingTimes.PILE_TICKS;

    private ItemStack tea = ItemStack.EMPTY;
    private boolean done;
    private int ticksRemaining;
    private final LazyOptional<IItemHandler> itemHandler = LazyOptional.of(() -> new Handler());

    public PileFermentationBasinBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PILE_FERMENTATION_BASIN.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, PileFermentationBasinBlockEntity basin) {
        if (!level.isClientSide && !basin.tea.isEmpty() && !basin.done && basin.ticksRemaining > 0) {
            basin.ticksRemaining--;
            if (basin.ticksRemaining <= 0) {
                basin.tea = TeaProcessingData.apply(basin.tea, TeaProcessingData.PILED);
                basin.done = true;
                basin.sync();
            } else {
                basin.setChanged();
            }
        }
        if (level instanceof ServerLevel serverLevel && !basin.tea.isEmpty() && !basin.done &&
                level.random.nextFloat() < 0.08F) {
            serverLevel.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5D, pos.getY() + 0.55D, pos.getZ() + 0.5D,
                    1, 0.2D, 0.05D, 0.2D, 0.01D);
        }
    }

    public boolean canInsertTea(ItemStack stack) {
        return tea.isEmpty() && stack.is(ModItems.TEA_LEAF.get()) &&
                TeaProcessingData.canApply(stack, TeaProcessingData.PILED);
    }

    public void insertTea(ItemStack stack) {
        tea = stack.copy();
        tea.setCount(1);
        done = false;
        ticksRemaining = PILE_TICKS;
        sync();
    }

    public ItemStack takeTea() {
        if (tea.isEmpty() || !done) {
            return ItemStack.EMPTY;
        }
        ItemStack result = tea.copy();
        tea = ItemStack.EMPTY;
        done = false;
        ticksRemaining = 0;
        sync();
        return result;
    }

    public void dropContents(Level level) {
        if (!tea.isEmpty()) {
            level.addFreshEntity(new ItemEntity(level, worldPosition.getX() + 0.5D, worldPosition.getY() + 0.6D,
                    worldPosition.getZ() + 0.5D, tea.copy()));
        }
        tea = ItemStack.EMPTY;
        done = false;
        ticksRemaining = 0;
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
        tag.putBoolean("Done", done);
        tag.putInt("TicksRemaining", ticksRemaining);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        tea = tag.contains("Tea") ? ItemStack.of(tag.getCompound("Tea")) : ItemStack.EMPTY;
        done = tag.getBoolean("Done");
        ticksRemaining = tag.getInt("TicksRemaining");
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
            if (!canInsertTea(stack)) {
                return stack;
            }
            ItemStack remainder = stack.copy();
            ItemStack input = stack.copy();
            input.setCount(1);
            if (!simulate) {
                insertTea(input);
            }
            remainder.shrink(1);
            return remainder;
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (amount <= 0 || tea.isEmpty() || !done) {
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
            return canInsertTea(stack);
        }
    }
}

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

import com.qianxin.teaart.block.TeaSieveRackBlock;
import com.qianxin.teaart.item.TeaProcessingData;
import com.qianxin.teaart.registry.ModBlockEntities;
import com.qianxin.teaart.registry.ModItems;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public class TeaSieveRackBlockEntity extends BlockEntity implements com.qianxin.teaart.api.TimedTeaDevice {

    @Override
    public boolean canAdvanceProcessing() {
        for (int i = 0; i < SLOT_COUNT; i++) if (hasSieve[i] && !tea[i].isEmpty() && !finished[i] &&
                ticksRemaining[i] > 0 && TeaProcessingData.canDry(tea[i]))
            return true;
        return false;
    }

    @Override
    public boolean tryAdvanceProcessing(int ticks) {
        if (level == null || level.isClientSide || ticks <= 0 || !canAdvanceProcessing()) return false;
        for (int i = 0; i < Math.min(ticks, 12000) && canAdvanceProcessing(); i++)
            tick(level, worldPosition, getBlockState(), this);
        setChanged();
        return true;
    }

    private static final int SLOT_COUNT = 3;

    private final boolean[] hasSieve = new boolean[SLOT_COUNT];
    private final ItemStack[] tea = new ItemStack[] { ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY };
    private final int[] ticksRemaining = new int[SLOT_COUNT];
    private final boolean[] finished = new boolean[SLOT_COUNT];
    private final LazyOptional<IItemHandler> itemHandler = LazyOptional.of(() -> new Handler());

    public TeaSieveRackBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TEA_SIEVE_RACK.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TeaSieveRackBlockEntity rack) {
        if (level.isClientSide) {
            return;
        }

        boolean changed = false;
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (!rack.hasSieve[i] || rack.tea[i].isEmpty() || rack.finished[i]) {
                continue;
            }
            rack.ticksRemaining[i]--;
            if (rack.ticksRemaining[i] <= 0) {
                rack.tea[i] = TeaProcessingData.dry(rack.tea[i]);
                rack.finished[i] = true;
                changed = true;
            }
        }
        if (changed) {
            rack.sync();
        }
    }

    public int slotFromHit(double localX) {
        if (localX < 5.33D) {
            return 0;
        }
        if (localX < 10.66D) {
            return 1;
        }
        return 2;
    }

    public boolean addSieve(int preferredSlot) {
        int slot = findSlot(preferredSlot, true);
        if (slot < 0) {
            return false;
        }
        hasSieve[slot] = true;
        sync();
        return true;
    }

    public ItemStack removeSieve(int preferredSlot) {
        int slot = findSlot(preferredSlot, false);
        if (slot < 0 || !tea[slot].isEmpty()) {
            return ItemStack.EMPTY;
        }
        hasSieve[slot] = false;
        sync();
        return new ItemStack(ModItems.TEA_SIEVE.get());
    }

    public boolean startDrying(int preferredSlot, ItemStack stack) {
        int slot = findDryingSlot(preferredSlot, stack);
        if (slot < 0) {
            return false;
        }
        tea[slot] = stack.copy();
        tea[slot].setCount(1);
        ticksRemaining[slot] = TeaSieveBlockEntity.DRYING_TICKS;
        finished[slot] = false;
        sync();
        return true;
    }

    public boolean canStartDrying(int preferredSlot, ItemStack stack) {
        return findDryingSlot(preferredSlot, stack) >= 0;
    }

    public ItemStack takeTea(int preferredSlot) {
        int slot = findFinishedSlot(preferredSlot);
        if (slot < 0) {
            return ItemStack.EMPTY;
        }
        ItemStack result = tea[slot].copy();
        tea[slot] = ItemStack.EMPTY;
        ticksRemaining[slot] = 0;
        finished[slot] = false;
        sync();
        return result;
    }

    public void dropContents(Level level) {
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (hasSieve[i]) {
                level.addFreshEntity(new ItemEntity(level, worldPosition.getX() + 0.5D, worldPosition.getY() + 0.7D,
                        worldPosition.getZ() + 0.5D, new ItemStack(ModItems.TEA_SIEVE.get())));
            }
            if (!tea[i].isEmpty()) {
                level.addFreshEntity(new ItemEntity(level, worldPosition.getX() + 0.5D, worldPosition.getY() + 0.8D,
                        worldPosition.getZ() + 0.5D, tea[i].copy()));
            }
        }
        Arrays.fill(hasSieve, false);
        Arrays.fill(tea, ItemStack.EMPTY);
        Arrays.fill(ticksRemaining, 0);
        Arrays.fill(finished, false);
    }

    private int findSlot(int preferredSlot, boolean empty) {
        if (preferredSlot >= 0 && preferredSlot < SLOT_COUNT && hasSieve[preferredSlot] != empty) {
            return preferredSlot;
        }
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (hasSieve[i] != empty) {
                return i;
            }
        }
        return -1;
    }

    private int findDryingSlot(int preferredSlot, ItemStack stack) {
        if (isDryingSlot(preferredSlot, stack)) {
            return preferredSlot;
        }
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (isDryingSlot(i, stack)) {
                return i;
            }
        }
        return -1;
    }

    private boolean isDryingSlot(int slot, ItemStack stack) {
        return slot >= 0 && slot < SLOT_COUNT && hasSieve[slot] && tea[slot].isEmpty() &&
                TeaProcessingData.canDry(stack);
    }

    private int findFinishedSlot(int preferredSlot) {
        if (isFinishedSlot(preferredSlot)) {
            return preferredSlot;
        }
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (isFinishedSlot(i)) {
                return i;
            }
        }
        return -1;
    }

    private boolean isFinishedSlot(int slot) {
        return slot >= 0 && slot < SLOT_COUNT && hasSieve[slot] && finished[slot] && !tea[slot].isEmpty();
    }

    private int countSieves() {
        int count = 0;
        for (boolean has : hasSieve) {
            if (has) count++;
        }
        return count;
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            int count = countSieves();
            if (state.hasProperty(TeaSieveRackBlock.SIEVES) && state.getValue(TeaSieveRackBlock.SIEVES) != count) {
                level.setBlock(worldPosition, state.setValue(TeaSieveRackBlock.SIEVES, count), Block.UPDATE_ALL);
            } else {
                level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
            }
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
        for (int i = 0; i < SLOT_COUNT; i++) {
            tag.putBoolean("HasSieve" + i, hasSieve[i]);
            if (!tea[i].isEmpty()) {
                tag.put("Tea" + i, tea[i].save(new CompoundTag()));
            }
            tag.putInt("TicksRemaining" + i, ticksRemaining[i]);
            tag.putBoolean("Finished" + i, finished[i]);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        for (int i = 0; i < SLOT_COUNT; i++) {
            hasSieve[i] = tag.getBoolean("HasSieve" + i);
            tea[i] = tag.contains("Tea" + i) ? ItemStack.of(tag.getCompound("Tea" + i)) : ItemStack.EMPTY;
            ticksRemaining[i] = tag.getInt("TicksRemaining" + i);
            finished[i] = tag.getBoolean("Finished" + i);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            int count = countSieves();
            if (state.hasProperty(TeaSieveRackBlock.SIEVES) && state.getValue(TeaSieveRackBlock.SIEVES) != count) {
                level.setBlock(worldPosition, state.setValue(TeaSieveRackBlock.SIEVES, count), Block.UPDATE_ALL);
            }
        }
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
            return SLOT_COUNT;
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            return tea[slot];
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (!isDryingSlot(slot, stack)) {
                return stack;
            }
            ItemStack remainder = stack.copy();
            ItemStack input = stack.copy();
            input.setCount(1);
            if (!simulate) {
                startDrying(slot, input);
            }
            remainder.shrink(1);
            return remainder;
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (!isFinishedSlot(slot) || amount <= 0) {
                return ItemStack.EMPTY;
            }
            ItemStack result = tea[slot].copy();
            if (!simulate) {
                takeTea(slot);
            }
            return result;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return isDryingSlot(slot, stack);
        }
    }
}

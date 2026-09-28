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
import com.qianxin.teaart.registry.ModItems;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class KneadingTableBlockEntity extends BlockEntity {

    private static final int REQUIRED_KNEADS = com.qianxin.teaart.lib.TeaArtProcessingTimes.REQUIRED_KNEADS;

    private ItemStack tea = ItemStack.EMPTY;
    private int progress;
    private final LazyOptional<IItemHandler> itemHandler = LazyOptional.of(() -> new Handler());

    public KneadingTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.KNEADING_TABLE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, KneadingTableBlockEntity table) {}

    public boolean canPlaceTea(ItemStack stack) {
        return tea.isEmpty() && stack.is(ModItems.TEA_LEAF.get()) &&
                TeaProcessingData.canApply(stack, TeaProcessingData.ROLLED);
    }

    public void placeTea(ItemStack stack) {
        tea = stack.copy();
        tea.setCount(1);
        progress = 0;
        sync();
    }

    public boolean hasTea() {
        return !tea.isEmpty();
    }

    public void knead() {
        if (tea.isEmpty()) {
            return;
        }
        progress++;
        if (progress >= REQUIRED_KNEADS) {
            ItemStack result = TeaProcessingData.apply(tea, TeaProcessingData.ROLLED);
            if (level != null) {
                ItemStack remainder = BambooPipeBlockEntity.insertIntoAdjacentPipeNetwork(level, worldPosition, result);
                if (!remainder.isEmpty()) {
                    level.addFreshEntity(new ItemEntity(level, worldPosition.getX() + 0.5D, worldPosition.getY() + 1.1D,
                            worldPosition.getZ() + 0.5D, remainder));
                }
            }
            tea = ItemStack.EMPTY;
            progress = 0;
        }
        sync();
    }

    public void dropContents(Level level) {
        if (!tea.isEmpty()) {
            level.addFreshEntity(new ItemEntity(level, worldPosition.getX() + 0.5D, worldPosition.getY() + 1.1D,
                    worldPosition.getZ() + 0.5D, tea.copy()));
            tea = ItemStack.EMPTY;
            progress = 0;
        }
    }

    private void sync() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (!tea.isEmpty()) {
            tag.put("Tea", tea.save(new CompoundTag()));
        }
        tag.putInt("Progress", progress);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        tea = tag.contains("Tea") ? ItemStack.of(tag.getCompound("Tea")) : ItemStack.EMPTY;
        progress = tag.getInt("Progress");
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
            if (!canPlaceTea(stack)) {
                return stack;
            }
            ItemStack remainder = stack.copy();
            ItemStack input = stack.copy();
            input.setCount(1);
            if (!simulate) {
                placeTea(input);
            }
            remainder.shrink(1);
            return remainder;
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return canPlaceTea(stack);
        }
    }
}

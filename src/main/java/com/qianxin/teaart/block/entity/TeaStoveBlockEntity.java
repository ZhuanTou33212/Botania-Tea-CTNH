package com.qianxin.teaart.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;

import com.qianxin.teaart.lib.TeaArtProcessingTimes;
import com.qianxin.teaart.lib.TeaArtSlotCounts;
import com.qianxin.teaart.registry.ModBlockEntities;
import com.qianxin.teaart.registry.ModItems;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class TeaStoveBlockEntity extends BlockEntity {

    private static final int SLOT_COUNT = TeaArtSlotCounts.TEA_STOVE_SLOTS;
    private static final int DEFAULT_COOK_TICKS = TeaArtProcessingTimes.DEFAULT_COOK_TICKS;

    private final ItemStack[] items = new ItemStack[] { ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
            ItemStack.EMPTY };
    private final int[] cookTicks = new int[SLOT_COUNT];
    private final boolean[] finished = new boolean[SLOT_COUNT];
    private final LazyOptional<IItemHandler> itemHandler = LazyOptional.of(() -> new Handler());

    public TeaStoveBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TEA_STOVE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TeaStoveBlockEntity stove) {
        if (level.isClientSide) {
            return;
        }

        boolean changed = false;
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (stove.items[i].isEmpty() || stove.finished[i]) {
                continue;
            }
            stove.cookTicks[i]--;
            if (stove.cookTicks[i] <= 0) {
                ItemStack result = stove.getCookResult(stove.items[i]);
                ItemStack remainder = BambooPipeBlockEntity.insertIntoAdjacentPipeNetwork(level, pos, result);
                if (!remainder.isEmpty()) {
                    level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5D, pos.getY() + 1.0D,
                            pos.getZ() + 0.5D, remainder));
                }
                stove.items[i] = ItemStack.EMPTY;
                stove.finished[i] = false;
                changed = true;
            }
        }
        if (changed) {
            stove.sync();
        }
    }

    public boolean canCook(ItemStack stack) {
        return findEmptySlot() >= 0 &&
                (getRecipeResult(stack).isPresent() || stack.isEdible() || stack.is(ModItems.TEA_LEAF.get()));
    }

    public void addItem(ItemStack stack) {
        int slot = findEmptySlot();
        if (slot < 0) {
            return;
        }
        items[slot] = stack.copy();
        items[slot].setCount(1);
        cookTicks[slot] = DEFAULT_COOK_TICKS;
        finished[slot] = false;
        sync();
    }

    public ItemStack takeFinished() {
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (finished[i] && !items[i].isEmpty()) {
                ItemStack result = items[i].copy();
                items[i] = ItemStack.EMPTY;
                cookTicks[i] = 0;
                finished[i] = false;
                sync();
                return result;
            }
        }
        return ItemStack.EMPTY;
    }

    public void dropContents(Level level) {
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (!items[i].isEmpty()) {
                level.addFreshEntity(new ItemEntity(level, worldPosition.getX() + 0.5D, worldPosition.getY() + 1.0D,
                        worldPosition.getZ() + 0.5D, items[i].copy()));
                items[i] = ItemStack.EMPTY;
            }
        }
    }

    private int findEmptySlot() {
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (items[i].isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    private ItemStack getCookResult(ItemStack input) {
        return getRecipeResult(input).orElseGet(() -> input.copy());
    }

    private Optional<ItemStack> getRecipeResult(ItemStack input) {
        if (level == null) {
            return Optional.empty();
        }
        SimpleContainer container = new SimpleContainer(input.copy());
        return level.getRecipeManager().getRecipeFor(RecipeType.CAMPFIRE_COOKING, container, level)
                .map(recipe -> recipe.getResultItem(level.registryAccess()))
                .map(stack -> stack.copy());
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
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (!items[i].isEmpty()) {
                tag.put("Item" + i, items[i].save(new CompoundTag()));
            }
            tag.putInt("CookTicks" + i, cookTicks[i]);
            tag.putBoolean("Finished" + i, finished[i]);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        for (int i = 0; i < SLOT_COUNT; i++) {
            items[i] = tag.contains("Item" + i) ? ItemStack.of(tag.getCompound("Item" + i)) : ItemStack.EMPTY;
            cookTicks[i] = tag.getInt("CookTicks" + i);
            finished[i] = tag.getBoolean("Finished" + i);
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
            return slot >= 0 && slot < SLOT_COUNT ? items[slot] : ItemStack.EMPTY;
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (slot < 0 || slot >= SLOT_COUNT || !items[slot].isEmpty() || !canCook(stack)) {
                return stack;
            }
            ItemStack remainder = stack.copy();
            if (!simulate) {
                items[slot] = stack.copy();
                items[slot].setCount(1);
                cookTicks[slot] = DEFAULT_COOK_TICKS;
                finished[slot] = false;
                sync();
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
            return canCook(stack);
        }
    }
}

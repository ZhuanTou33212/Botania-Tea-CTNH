package com.qianxin.teaart.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;

import com.qianxin.teaart.block.ScentingBoxBlock;
import com.qianxin.teaart.item.TeaProcessingData;
import com.qianxin.teaart.lib.TeaArtProcessingTimes;
import com.qianxin.teaart.lib.TeaArtSlotCounts;
import com.qianxin.teaart.menu.ScentingBoxMenu;
import com.qianxin.teaart.registry.ModBlockEntities;
import com.qianxin.teaart.registry.ModItems;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ScentingBoxBlockEntity extends BlockEntity implements MenuProvider, com.qianxin.teaart.api.TimedTeaDevice {

    @Override
    public boolean canAdvanceProcessing() {
        return canProcess(getResult()) && (burnTime > 1 || ForgeHooks.getBurnTime(items[FUEL_SLOT], null) > 0);
    }

    @Override
    public boolean tryAdvanceProcessing(int ticks) {
        if (level == null || level.isClientSide || ticks <= 0 || !canAdvanceProcessing()) return false;
        for (int i = 0; i < Math.min(ticks, 12000) && canAdvanceProcessing(); i++)
            tick(level, worldPosition, getBlockState(), this);
        setChanged();
        return true;
    }

    public static final int TEA_SLOT = 0;
    public static final int FLOWER_SLOT = 1;
    public static final int FUEL_SLOT = 2;
    public static final int OUTPUT_SLOT = 3;
    public static final int SLOT_COUNT = TeaArtSlotCounts.SCENTING_BOX_SLOTS;
    public static final int SCENTING_TICKS = TeaArtProcessingTimes.SCENTING_TICKS;

    private final ItemStack[] items = new ItemStack[] { ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
            ItemStack.EMPTY };
    private int progress;
    private int burnTime;
    private int burnDuration;
    private final LazyOptional<IItemHandler> itemHandler = LazyOptional.of(() -> new Handler());

    public ScentingBoxBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SCENTING_BOX.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ScentingBoxBlockEntity box) {
        if (level.isClientSide) {
            return;
        }
        boolean changed = false;
        if (box.isBurning()) {
            box.burnTime--;
            changed = true;
        }

        ItemStack result = box.getResult();
        boolean canProcess = box.canProcess(result);
        if (!box.isBurning() && canProcess && ForgeHooks.getBurnTime(box.items[FUEL_SLOT], null) > 0) {
            box.burnDuration = ForgeHooks.getBurnTime(box.items[FUEL_SLOT], null);
            box.burnTime = box.burnDuration;
            box.items[FUEL_SLOT].shrink(1);
            if (box.items[FUEL_SLOT].isEmpty()) {
                box.items[FUEL_SLOT] = ItemStack.EMPTY;
            }
            changed = true;
        }

        if (box.isBurning() && canProcess) {
            box.progress++;
            if (box.progress >= SCENTING_TICKS) {
                box.process(result);
                box.progress = 0;
                box.updateLitState();
                box.sync();
                return;
            }
            changed = true;
        } else {
            if (box.progress != 0) {
                box.progress = 0;
                changed = true;
            }
        }

        if (changed) {
            box.setChanged();
        }
        box.updateLitState();
    }

    public IItemHandler getItemHandler() {
        return itemHandler.orElseThrow(IllegalStateException::new);
    }

    public int getProgress() {
        return progress;
    }

    public int getProgressScale(int pixels) {
        return progress * pixels / SCENTING_TICKS;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public int getBurnScale(int pixels) {
        if (burnDuration <= 0) {
            return 0;
        }
        return burnTime * pixels / burnDuration;
    }

    public int getBurnTime() {
        return burnTime;
    }

    public void setBurnTime(int burnTime) {
        this.burnTime = burnTime;
    }

    public int getBurnDuration() {
        return burnDuration;
    }

    public void setBurnDuration(int burnDuration) {
        this.burnDuration = burnDuration;
    }

    public boolean mayPlace(int slot, ItemStack stack) {
        return switch (slot) {
            case TEA_SLOT -> isTeaInput(stack);
            case FLOWER_SLOT -> isFlower(stack);
            case FUEL_SLOT -> ForgeHooks.getBurnTime(stack, null) > 0;
            default -> false;
        };
    }

    public boolean mayTake(int slot) {
        return slot >= 0 && slot < SLOT_COUNT;
    }

    public ItemStack getStack(int slot) {
        return items[slot];
    }

    public void setStack(int slot, ItemStack stack) {
        items[slot] = stack;
        sync();
    }

    public ItemStack removeStack(int slot, int amount) {
        if (items[slot].isEmpty() || amount <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack result = items[slot].split(amount);
        if (items[slot].isEmpty()) {
            items[slot] = ItemStack.EMPTY;
        }
        sync();
        return result;
    }

    public void dropContents(Level level) {
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (!items[i].isEmpty()) {
                level.addFreshEntity(new ItemEntity(level, worldPosition.getX() + 0.5D, worldPosition.getY() + 0.8D,
                        worldPosition.getZ() + 0.5D, items[i].copy()));
                items[i] = ItemStack.EMPTY;
            }
        }
    }

    private boolean isBurning() {
        return burnTime > 0;
    }

    private boolean canProcess(ItemStack result) {
        if (result.isEmpty() || !isTeaInput(items[TEA_SLOT]) || !isFlower(items[FLOWER_SLOT])) {
            return false;
        }
        ItemStack output = items[OUTPUT_SLOT];
        if (output.isEmpty()) {
            return true;
        }
        return ItemStack.isSameItemSameTags(output, result) &&
                output.getCount() + result.getCount() <= Math.min(output.getMaxStackSize(), getSlotLimit(OUTPUT_SLOT));
    }

    private void process(ItemStack result) {
        if (items[OUTPUT_SLOT].isEmpty()) {
            items[OUTPUT_SLOT] = result.copy();
        } else {
            items[OUTPUT_SLOT].grow(result.getCount());
        }
        items[TEA_SLOT].shrink(1);
        items[FLOWER_SLOT].shrink(1);
        if (items[TEA_SLOT].isEmpty()) items[TEA_SLOT] = ItemStack.EMPTY;
        if (items[FLOWER_SLOT].isEmpty()) items[FLOWER_SLOT] = ItemStack.EMPTY;
    }

    private ItemStack getResult() {
        if (items[FLOWER_SLOT].is(ModItems.JASMINE_FLOWER.get())) {
            return TeaProcessingData.scent(items[TEA_SLOT], TeaProcessingData.JASMINE_SCENTED);
        }
        if (items[FLOWER_SLOT].is(ModItems.GARDENIA_FLOWER.get())) {
            return TeaProcessingData.scent(items[TEA_SLOT], TeaProcessingData.GARDENIA_SCENTED);
        }
        return ItemStack.EMPTY;
    }

    private static boolean isTeaInput(ItemStack stack) {
        return TeaProcessingData.canScent(stack);
    }

    private static boolean isFlower(ItemStack stack) {
        return stack.is(ModItems.JASMINE_FLOWER.get()) || stack.is(ModItems.GARDENIA_FLOWER.get());
    }

    private static boolean isTeaOutput(ItemStack stack) {
        return TeaProcessingData.isScentedTeaLeaf(stack);
    }

    private static int getSlotLimit(int slot) {
        return 64;
    }

    private void sync() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private void updateLitState() {
        if (level == null || level.isClientSide || !getBlockState().hasProperty(ScentingBoxBlock.LIT)) {
            return;
        }
        setLit(worldPosition, getBlockState(), isBurning());
        BlockPos upperPos = worldPosition.above();
        BlockState upperState = level.getBlockState(upperPos);
        if (upperState.is(getBlockState().getBlock()) && upperState.hasProperty(ScentingBoxBlock.LIT)) {
            setLit(upperPos, upperState, isBurning());
        }
    }

    private void setLit(BlockPos pos, BlockState state, boolean lit) {
        if (state.getValue(ScentingBoxBlock.LIT) != lit) {
            level.setBlock(pos, state.setValue(ScentingBoxBlock.LIT, lit), Block.UPDATE_ALL);
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.tea_art.scenting_box");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new ScentingBoxMenu(containerId, inventory, this);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemHandler.invalidate();
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            if (side == null) {
                return itemHandler.cast();
            }
            return LazyOptional.<IItemHandler>of(() -> new SidedHandler(side)).cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (!items[i].isEmpty()) {
                tag.put("Item" + i, items[i].save(new CompoundTag()));
            }
        }
        tag.putInt("Progress", progress);
        tag.putInt("BurnTime", burnTime);
        tag.putInt("BurnDuration", burnDuration);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        for (int i = 0; i < SLOT_COUNT; i++) {
            items[i] = tag.contains("Item" + i) ? ItemStack.of(tag.getCompound("Item" + i)) : ItemStack.EMPTY;
        }
        progress = tag.getInt("Progress");
        burnTime = tag.getInt("BurnTime");
        burnDuration = tag.getInt("BurnDuration");
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

    private class Handler implements IItemHandlerModifiable {

        @Override
        public int getSlots() {
            return SLOT_COUNT;
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            return items[slot];
        }

        @Override
        public void setStackInSlot(int slot, @NotNull ItemStack stack) {
            if (slot < 0 || slot >= SLOT_COUNT) {
                return;
            }
            items[slot] = stack.copy();
            sync();
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (!mayPlace(slot, stack)) {
                return stack;
            }
            int limit = Math.min(getSlotLimit(slot), stack.getMaxStackSize());
            if (!items[slot].isEmpty() && !ItemStack.isSameItemSameTags(items[slot], stack)) {
                return stack;
            }
            int insertedCount = Math.min(stack.getCount(), limit - items[slot].getCount());
            if (insertedCount <= 0) {
                return stack;
            }
            ItemStack remainder = stack.copy();
            ItemStack inserted = stack.copy();
            inserted.setCount(insertedCount);
            if (!simulate) {
                if (items[slot].isEmpty()) {
                    setStack(slot, inserted);
                } else {
                    items[slot].grow(insertedCount);
                    sync();
                }
            }
            remainder.shrink(insertedCount);
            return remainder;
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (!mayTake(slot) || amount <= 0 || items[slot].isEmpty()) {
                return ItemStack.EMPTY;
            }
            int extractedCount = Math.min(amount, items[slot].getCount());
            ItemStack result = items[slot].copy();
            result.setCount(extractedCount);
            if (!simulate) {
                items[slot].shrink(extractedCount);
                if (items[slot].isEmpty()) {
                    items[slot] = ItemStack.EMPTY;
                }
                sync();
            }
            return result;
        }

        @Override
        public int getSlotLimit(int slot) {
            return ScentingBoxBlockEntity.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return mayPlace(slot, stack);
        }
    }

    private class SidedHandler extends Handler {

        private final Direction side;

        SidedHandler(Direction side) {
            this.side = side;
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            ScentingBoxBlockEntity target = target();
            return target == ScentingBoxBlockEntity.this ? super.getStackInSlot(slot) : target.getStack(slot);
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            int targetSlot = insertionSlot();
            if (slot != targetSlot || targetSlot < 0) {
                return stack;
            }
            ScentingBoxBlockEntity target = target();
            return target == ScentingBoxBlockEntity.this ? super.insertItem(slot, stack, simulate) :
                    target.insertFromAutomation(slot, stack, simulate);
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot != OUTPUT_SLOT) {
                return ItemStack.EMPTY;
            }
            ScentingBoxBlockEntity target = target();
            return target == ScentingBoxBlockEntity.this ? super.extractItem(slot, amount, simulate) :
                    target.extractFromAutomation(slot, amount, simulate);
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return slot == insertionSlot() && target().mayPlace(slot, stack);
        }

        private int insertionSlot() {
            if (side == Direction.UP) {
                return TEA_SLOT;
            }
            if (side == Direction.DOWN) {
                return FUEL_SLOT;
            }
            if (getBlockState().hasProperty(
                    net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF) &&
                    getBlockState().getValue(
                            net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF) ==
                            net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER) {
                return FLOWER_SLOT;
            }
            return FUEL_SLOT;
        }

        private ScentingBoxBlockEntity target() {
            if (getBlockState().hasProperty(
                    net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF) &&
                    getBlockState().getValue(
                            net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF) ==
                            net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER &&
                    level != null &&
                    level.getBlockEntity(worldPosition.below()) instanceof ScentingBoxBlockEntity lower) {
                return lower;
            }
            return ScentingBoxBlockEntity.this;
        }
    }

    private ItemStack insertFromAutomation(int slot, ItemStack stack, boolean simulate) {
        return new Handler().insertItem(slot, stack, simulate);
    }

    private ItemStack extractFromAutomation(int slot, int amount, boolean simulate) {
        return new Handler().extractItem(slot, amount, simulate);
    }
}

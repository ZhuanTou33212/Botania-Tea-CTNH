package com.qianxin.teaart.menu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.items.SlotItemHandler;

import com.qianxin.teaart.block.entity.ScentingBoxBlockEntity;
import com.qianxin.teaart.item.TeaProcessingData;
import com.qianxin.teaart.registry.ModBlocks;
import com.qianxin.teaart.registry.ModItems;
import com.qianxin.teaart.registry.ModMenus;

public class ScentingBoxMenu extends AbstractContainerMenu {

    private static final int INV_START = ScentingBoxBlockEntity.SLOT_COUNT;
    private static final int INV_END = INV_START + 27;
    private static final int HOTBAR_START = INV_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final ScentingBoxBlockEntity box;
    private int progress;
    private int burnTime;
    private int burnDuration;

    public ScentingBoxMenu(int containerId, Inventory inventory, FriendlyByteBuf data) {
        this(containerId, inventory, inventory.player.level().getBlockEntity(data.readBlockPos()));
    }

    public ScentingBoxMenu(int containerId, Inventory inventory, BlockEntity blockEntity) {
        super(ModMenus.SCENTING_BOX.get(), containerId);
        this.box = (ScentingBoxBlockEntity) blockEntity;

        addSlot(new BoxSlot(ScentingBoxBlockEntity.TEA_SLOT, 56, 17));
        addSlot(new BoxSlot(ScentingBoxBlockEntity.FLOWER_SLOT, 80, 17));
        addSlot(new BoxSlot(ScentingBoxBlockEntity.FUEL_SLOT, 56, 53));
        addSlot(new BoxSlot(ScentingBoxBlockEntity.OUTPUT_SLOT, 116, 35));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        }

        addDataSlot(new DataSlot() {

            @Override
            public int get() {
                return box.getProgress();
            }

            @Override
            public void set(int value) {
                progress = value;
                box.setProgress(value);
            }
        });
        addDataSlot(new DataSlot() {

            @Override
            public int get() {
                return box.getBurnTime();
            }

            @Override
            public void set(int value) {
                burnTime = value;
                box.setBurnTime(value);
            }
        });
        addDataSlot(new DataSlot() {

            @Override
            public int get() {
                return box.getBurnDuration();
            }

            @Override
            public void set(int value) {
                burnDuration = value;
                box.setBurnDuration(value);
            }
        });
    }

    public int getProgressScale(int pixels) {
        return progress * pixels / ScentingBoxBlockEntity.SCENTING_TICKS;
    }

    public int getBurnScale(int pixels) {
        if (burnDuration <= 0) {
            return 0;
        }
        return burnTime * pixels / burnDuration;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack original = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            original = stack.copy();
            if (index == ScentingBoxBlockEntity.OUTPUT_SLOT) {
                if (!moveItemStackTo(stack, INV_START, HOTBAR_END, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (index < ScentingBoxBlockEntity.SLOT_COUNT) {
                if (!moveItemStackTo(stack, INV_START, HOTBAR_END, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (TeaProcessingData.canScent(stack)) {
                if (!moveItemStackTo(stack, ScentingBoxBlockEntity.TEA_SLOT, ScentingBoxBlockEntity.TEA_SLOT + 1,
                        false)) {
                    return ItemStack.EMPTY;
                }
            } else if (isFlower(stack)) {
                if (!moveItemStackTo(stack, ScentingBoxBlockEntity.FLOWER_SLOT, ScentingBoxBlockEntity.FLOWER_SLOT + 1,
                        false)) {
                    return ItemStack.EMPTY;
                }
            } else if (ForgeHooks.getBurnTime(stack, null) > 0) {
                if (!moveItemStackTo(stack, ScentingBoxBlockEntity.FUEL_SLOT, ScentingBoxBlockEntity.FUEL_SLOT + 1,
                        false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index >= INV_START && index < INV_END) {
                if (!moveItemStackTo(stack, HOTBAR_START, HOTBAR_END, false)) {
                    return ItemStack.EMPTY;
                }
            } else
                if (index >= HOTBAR_START && index < HOTBAR_END && !moveItemStackTo(stack, INV_START, INV_END, false)) {
                    return ItemStack.EMPTY;
                }
            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(net.minecraft.world.inventory.ContainerLevelAccess.create(box.getLevel(), box.getBlockPos()),
                player, ModBlocks.SCENTING_BOX.get());
    }

    private static boolean isFlower(ItemStack stack) {
        return stack.is(ModItems.JASMINE_FLOWER.get()) || stack.is(ModItems.GARDENIA_FLOWER.get());
    }

    private class BoxSlot extends SlotItemHandler {

        BoxSlot(int slot, int x, int y) {
            super(box.getItemHandler(), slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return box.mayPlace(getSlotIndex(), stack);
        }
    }
}

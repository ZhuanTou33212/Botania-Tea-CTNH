package com.qianxin.teaart.menu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.SlotItemHandler;

import com.qianxin.teaart.block.entity.TeaTableBlockEntity;
import com.qianxin.teaart.registry.ModBlocks;
import com.qianxin.teaart.registry.ModMenus;

public class TeaTableMenu extends AbstractContainerMenu {

    private static final int INV_START = TeaTableBlockEntity.SLOT_COUNT;
    private static final int INV_END = INV_START + 27;
    private static final int HOTBAR_START = INV_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final TeaTableBlockEntity table;
    private int water;
    private int brewTicks;
    private int burnTime;
    private int burnDuration;

    public TeaTableMenu(int containerId, Inventory inventory, FriendlyByteBuf data) {
        this(containerId, inventory, inventory.player.level().getBlockEntity(data.readBlockPos()));
    }

    public TeaTableMenu(int containerId, Inventory inventory, BlockEntity blockEntity) {
        super(ModMenus.TEA_TABLE.get(), containerId);
        this.table = (TeaTableBlockEntity) blockEntity;
        addSlot(new TeaTableSlot(TeaTableBlockEntity.LEAF_SLOT, 70, 18));
        addSlot(new TeaTableSlot(TeaTableBlockEntity.CUP_SLOT, 52, 72));
        addSlot(new TeaTableSlot(TeaTableBlockEntity.WATER_SLOT, 17, 122));
        addSlot(new TeaTableSlot(TeaTableBlockEntity.FUEL_SLOT, 88, 72));
        addSlot(new TeaTableSlot(TeaTableBlockEntity.OUTPUT_SLOT, 150, 72));
        addSlot(new TeaTableSlot(TeaTableBlockEntity.RESIDUE_SLOT, 150, 122));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 154 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 212));
        }

        addDataSlot(new DataSlot() {

            @Override
            public int get() {
                return table.getWater();
            }

            @Override
            public void set(int value) {
                water = value;
                table.setWater(value);
            }
        });
        addDataSlot(new DataSlot() {

            @Override
            public int get() {
                return table.getBrewTicks();
            }

            @Override
            public void set(int value) {
                brewTicks = value;
                table.setBrewTicks(value);
            }
        });
        addDataSlot(new DataSlot() {

            @Override
            public int get() {
                return table.getBurnTime();
            }

            @Override
            public void set(int value) {
                burnTime = value;
                table.setBurnTime(value);
            }
        });
        addDataSlot(new DataSlot() {

            @Override
            public int get() {
                return table.getBurnDuration();
            }

            @Override
            public void set(int value) {
                burnDuration = value;
                table.setBurnDuration(value);
            }
        });
    }

    public int getWaterScale(int pixels) {
        return water * pixels / TeaTableBlockEntity.CAPACITY;
    }

    public int getBrewScale(int pixels) {
        return brewTicks * pixels / table.getRequiredBrewTicks();
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
            if (index == TeaTableBlockEntity.OUTPUT_SLOT || index == TeaTableBlockEntity.RESIDUE_SLOT) {
                if (!moveItemStackTo(stack, INV_START, HOTBAR_END, true)) return ItemStack.EMPTY;
            } else if (index < TeaTableBlockEntity.SLOT_COUNT) {
                if (!moveItemStackTo(stack, INV_START, HOTBAR_END, false)) return ItemStack.EMPTY;
            } else {
                boolean moved = false;
                for (int i = 0; i < TeaTableBlockEntity.SLOT_COUNT - 1; i++) {
                    if (table.mayPlace(i, stack) && moveItemStackTo(stack, i, i + 1, false)) {
                        moved = true;
                        break;
                    }
                }
                if (!moved) {
                    if (index >= INV_START && index < INV_END) {
                        if (!moveItemStackTo(stack, HOTBAR_START, HOTBAR_END, false)) return ItemStack.EMPTY;
                    } else if (index >= HOTBAR_START && index < HOTBAR_END &&
                            !moveItemStackTo(stack, INV_START, INV_END, false)) {
                                return ItemStack.EMPTY;
                            }
                }
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
        return stillValid(ContainerLevelAccess.create(table.getLevel(), table.getBlockPos()),
                player, ModBlocks.TEA_TABLE.get());
    }

    private class TeaTableSlot extends SlotItemHandler {

        TeaTableSlot(int slot, int x, int y) {
            super(table.getItemHandler(), slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return table.mayPlace(getSlotIndex(), stack);
        }
    }
}

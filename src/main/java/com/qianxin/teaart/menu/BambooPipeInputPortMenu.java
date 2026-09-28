package com.qianxin.teaart.menu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.SlotItemHandler;

import com.qianxin.teaart.block.entity.FilteredPipePort;
import com.qianxin.teaart.registry.ModBlocks;
import com.qianxin.teaart.registry.ModMenus;

public class BambooPipeInputPortMenu extends AbstractContainerMenu {

    private static final int FILTER_END = FilteredPipePort.FILTER_SLOTS;
    private static final int INV_START = FILTER_END;
    private static final int INV_END = INV_START + 27;
    private static final int HOTBAR_START = INV_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final BlockEntity blockEntity;
    private final FilteredPipePort port;
    private int whitelist;

    public BambooPipeInputPortMenu(int containerId, Inventory inventory, FriendlyByteBuf data) {
        this(containerId, inventory, inventory.player.level().getBlockEntity(data.readBlockPos()));
    }

    public BambooPipeInputPortMenu(int containerId, Inventory inventory, BlockEntity blockEntity) {
        super(ModMenus.BAMBOO_PIPE_INPUT_PORT.get(), containerId);
        this.blockEntity = blockEntity;
        this.port = (FilteredPipePort) blockEntity;
        this.whitelist = port.isWhitelist() ? 1 : 0;

        for (int i = 0; i < FilteredPipePort.FILTER_SLOTS; i++) {
            addSlot(new FilterSlot(i, 26 + (i % 5) * 18, 20 + (i / 5) * 18));
        }

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
                return port.isWhitelist() ? 1 : 0;
            }

            @Override
            public void set(int value) {
                whitelist = value;
            }
        });
    }

    public boolean isWhitelist() {
        return whitelist != 0;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == 0) {
            port.toggleMode();
            return true;
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack original = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            original = stack.copy();
            if (index < FILTER_END) {
                setFilter(index, ItemStack.EMPTY);
            } else if (setFirstAvailableFilter(stack)) {
                // Ghost filters do not consume the source stack. A nonempty result
                // makes AbstractContainerMenu retry QUICK_MOVE indefinitely.
                return ItemStack.EMPTY;
            } else {
                if (index >= INV_START && index < INV_END) {
                    if (!moveItemStackTo(stack, HOTBAR_START, HOTBAR_END, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (index >= HOTBAR_START && index < HOTBAR_END &&
                        !moveItemStackTo(stack, INV_START, INV_END, false)) {
                            return ItemStack.EMPTY;
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
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId >= 0 && slotId < FILTER_END) {
            if (clickType == ClickType.QUICK_MOVE || button == 1) {
                setFilter(slotId, ItemStack.EMPTY);
                return;
            }
            ItemStack carried = getCarried();
            setFilter(slotId, carried.isEmpty() ? ItemStack.EMPTY : carried);
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()),
                player, ModBlocks.BAMBOO_PIPE_INPUT_PORT.get()) ||
                stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()),
                        player, ModBlocks.BAMBOO_PIPE_OUTPUT_PORT.get());
    }

    private void setFilter(int slot, ItemStack stack) {
        ItemStack filter = stack.copy();
        if (!filter.isEmpty()) {
            filter.setCount(1);
        }
        port.getFilters().setStackInSlot(slot, filter);
    }

    private boolean setFirstAvailableFilter(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        int fallback = 0;
        for (int i = 0; i < FILTER_END; i++) {
            if (port.getFilters().getStackInSlot(i).isEmpty()) {
                setFilter(i, stack);
                return true;
            }
        }
        setFilter(fallback, stack);
        return true;
    }

    private class FilterSlot extends SlotItemHandler {

        private FilterSlot(int index, int x, int y) {
            super(port.getFilters(), index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }
    }
}

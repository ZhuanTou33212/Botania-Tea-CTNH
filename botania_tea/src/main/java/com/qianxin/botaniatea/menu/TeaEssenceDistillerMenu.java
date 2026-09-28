package com.qianxin.botaniatea.menu;

import com.qianxin.botaniatea.block.entity.TeaEssenceDistillerBlockEntity;
import com.qianxin.botaniatea.registry.ModBlocks;
import com.qianxin.botaniatea.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

public final class TeaEssenceDistillerMenu extends AbstractContainerMenu {
    private final TeaEssenceDistillerBlockEntity be;
    private final ContainerData data;
    public TeaEssenceDistillerMenu(int id, Inventory inventory, FriendlyByteBuf buf) {
        this(id, inventory, (TeaEssenceDistillerBlockEntity) inventory.player.level().getBlockEntity(buf.readBlockPos()));
    }
    public TeaEssenceDistillerMenu(int id, Inventory inventory, TeaEssenceDistillerBlockEntity be) {
        super(ModMenus.TEA_ESSENCE_DISTILLER.get(), id); this.be = be;
        data = new ContainerData() {
            private final int[] values = new int[4];
            public int get(int i) {
                if (inventory.player.level().isClientSide) return values[i];
                return switch (i) { case 0 -> switch(be.mode()) { case "ELEMENT" -> 1; case "ASCENDED" -> 2; default -> 0; };
                    case 1 -> be.selectedOutputId(); case 2 -> be.fluidHandler().getFluidInTank(0).getAmount(); default -> be.progress(); };
            }
            public void set(int i, int value) { values[i] = value & 65535; }
            public int getCount() { return 4; }
        };
        addDataSlots(data);
        for (int i=0; i<4; i++) addSlot(new SlotItemHandler(be.menuInventory(), i, 44+i*22, 56));
        for (int row=0; row<3; row++) for (int col=0; col<9; col++) addSlot(new Slot(inventory, 9+row*9+col, 8+col*18, 94+row*18));
        for (int col=0; col<9; col++) addSlot(new Slot(inventory, col, 8+col*18, 152));
    }
    public int value(int i) { return data.get(i); }
    @Override public boolean clickMenuButton(Player player, int button) {
        if (player.level().isClientSide || !stillValid(player)) return false;
        if (button==0) be.cycleMode(); else if (button==1) be.cycleRecipe(); else return false;
        broadcastChanges(); return true;
    }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        Slot slot=slots.get(index); if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack=slot.getItem(), original=stack.copy();
        if (!(index<4 ? moveItemStackTo(stack,4,slots.size(),true) : moveItemStackTo(stack,0,2,false))) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player,stack); return original;
    }
    @Override public boolean stillValid(Player player) { return stillValid(ContainerLevelAccess.create(be.getLevel(),be.getBlockPos()),player,ModBlocks.TEA_ESSENCE_DISTILLER.get()); }
}

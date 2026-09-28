package com.qianxin.botaniatea.menu;

import com.qianxin.botaniatea.block.entity.MagicCrucibleBlockEntity;
import com.qianxin.botaniatea.registry.ModBlocks;
import com.qianxin.botaniatea.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

public final class MagicCrucibleMenu extends AbstractContainerMenu {
    private final MagicCrucibleBlockEntity crucible;
    private final net.minecraft.world.inventory.ContainerData data;
    public MagicCrucibleMenu(int id, Inventory inventory, FriendlyByteBuf data) { this(id, inventory, (MagicCrucibleBlockEntity) inventory.player.level().getBlockEntity(data.readBlockPos())); }
    public MagicCrucibleMenu(int id, Inventory inventory, MagicCrucibleBlockEntity crucible) {
        super(ModMenus.MAGIC_CRUCIBLE.get(), id); this.crucible = crucible;
        data = new net.minecraft.world.inventory.ContainerData() {
            private final int[] synced = new int[5];
            public int get(int index) {
                if (inventory.player.level().isClientSide) return synced[index];
                var recipe=index>=3?crucible.selectedEnchantment():null;
                return switch (index) { case 0 -> crucible.mana() & 65535; case 1 -> crucible.mana() >>> 16; case 2 -> crucible.progress(); case 3 -> recipe==null?65535:net.minecraft.core.registries.BuiltInRegistries.ENCHANTMENT.getId(net.minecraftforge.registries.ForgeRegistries.ENCHANTMENTS.getValue(recipe.enchantmentId())); default -> recipe==null?0:recipe.level(); };
            }
            public void set(int index, int value) { synced[index] = value & 65535; }
            public int getCount() { return 5; }
        };
        addDataSlots(data);
        for (int slot = 0; slot < 16; slot++) addSlot(new SlotItemHandler(crucible.menuInventory(), slot, 8 + (slot % 8) * 18, 18 + (slot / 8) * 18));
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 86 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, 144));
    }
    public int mana() { return data.get(0) | (data.get(1) << 16); }
    public int progress() { return data.get(2); }
    public net.minecraft.network.chat.Component enchantmentDescription() {
        if(data.get(3)==65535 || data.get(4)==0) return net.minecraft.network.chat.Component.translatable("gui.botania_tea.choose_enchantment");
        var enchantment=net.minecraft.core.registries.BuiltInRegistries.ENCHANTMENT.byId(data.get(3));
        return enchantment==null?net.minecraft.network.chat.Component.empty():enchantment.getFullname(data.get(4));
    }
    @Override public boolean clickMenuButton(Player player,int button) {
        if(!stillValid(player) || !(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) return false;
        return switch(button) { case 0 -> crucible.authorizeEnchanting(serverPlayer); case 1 -> crucible.selectNextEnchanting(serverPlayer); default -> false; };
    }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), original = stack.copy();
        if (!(index < 16 ? moveItemStackTo(stack,16,slots.size(),true) : moveItemStackTo(stack,0,14,false))) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player,stack); return original;
    }
    @Override public boolean stillValid(Player player) { return stillValid(ContainerLevelAccess.create(crucible.getLevel(), crucible.getBlockPos()), player, ModBlocks.MAGIC_CRUCIBLE.get()); }
}

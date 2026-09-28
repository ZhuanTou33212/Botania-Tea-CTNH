package com.qianxin.botaniatea.block.entity;

import com.qianxin.botaniatea.BotaniaTeaMod;
import com.qianxin.botaniatea.recipe.OfferingCraftingRecipe;
import com.qianxin.botaniatea.recipe.OfferingEnchantingRecipe;
import com.qianxin.botaniatea.registry.ModBlockEntities;
import com.qianxin.botaniatea.registry.ModBlocks;
import com.qianxin.botaniatea.registry.ModRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
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
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import vazkii.botania.api.mana.ManaReceiver;

import java.util.Map;

public final class MagicCrucibleBlockEntity extends BlockEntity implements Container, MenuProvider, ManaReceiver {
    public static final int SLOT_COUNT = 16;
    public static final int MANA_CAPACITY = 100_000;
    private final ItemStackHandler inventory = new ItemStackHandler(SLOT_COUNT) {
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) { return validSlot(slot, stack); }
        @Override public int getSlotLimit(int slot) { return 64; }
        @Override protected void onContentsChanged(int slot) { changed(); }
    };
    private LazyOptional<IItemHandler> itemCapability = LazyOptional.of(() -> new AutomationHandler());
    private int mana;
    public final com.qianxin.botaniatea.content.CrucibleRefining refining = new com.qianxin.botaniatea.content.CrucibleRefining(this);
    private LazyOptional<net.minecraftforge.fluids.capability.IFluidHandler> fluidCapability = LazyOptional.of(() -> refining.tank);
    private int progress;
    private String selectedRecipe = "";
    private String authorizedEnchanting = "";
    private String requestedEnchanting = "";
    private java.util.List<OfferingEnchantingRecipe> matchingEnchantments() {
        if(level==null || inventory.getStackInSlot(12).isEmpty() || inventory.getStackInSlot(13).isEmpty()) return java.util.List.of();
        return level.getRecipeManager().getAllRecipesFor(ModRecipes.OFFERING_ENCHANTING.get()).stream().filter(r->r.matchesContainer(this,level))
                .sorted(java.util.Comparator.comparing(r->r.getId().toString())).toList();
    }
    public boolean selectNextEnchanting(net.minecraft.server.level.ServerPlayer player) {
        if(level==null || level.isClientSide || !stillValid(player)) return false;
        var choices=matchingEnchantments(); if(choices.isEmpty()) return false;
        int current=-1; for(int i=0;i<choices.size();i++) if(choices.get(i).getId().toString().equals(requestedEnchanting)) current=i;
        requestedEnchanting=choices.get((current+1)%choices.size()).getId().toString();
        progress=0; selectedRecipe=""; changed(); return true;
    }
    public OfferingEnchantingRecipe selectedEnchantment() { return findEnchantingRecipe(); }
    public boolean authorizeEnchanting(net.minecraft.server.level.ServerPlayer player) {
        if(level==null || level.isClientSide || !stillValid(player)) return false;
        var recipe=findEnchantingRecipe();
        if(recipe==null) return false;
        var requirement=recipe.requiredAdvancement();
        if(requirement!=null) {
            var advancement=player.server.getAdvancements().getAdvancement(requirement);
            if(advancement==null || !player.getAdvancements().getOrStartProgress(advancement).isDone()) {
                player.displayClientMessage(Component.translatable("message.botania_tea.enchant_locked",requirement.toString()),true); return false;
            }
        }
        authorizedEnchanting=recipeKey(recipe); changed(); return true;
    }
    private net.minecraft.world.item.crafting.Recipe<?> digestRecipe;
    private String digestKey;
    private String recipeKey(net.minecraft.world.item.crafting.Recipe<?> recipe) {
        if (digestRecipe!=recipe) {
            var buffer=new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
            try {
                if (recipe instanceof OfferingCraftingRecipe crafting) new OfferingCraftingRecipe.Serializer().toNetwork(buffer,crafting);
                else new OfferingEnchantingRecipe.Serializer().toNetwork(buffer,(OfferingEnchantingRecipe)recipe);
                byte[] bytes=new byte[buffer.readableBytes()]; buffer.getBytes(0,bytes);
                digestKey=recipe.getId()+"|"+java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes));
                digestRecipe=recipe;
            } catch (java.security.NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
            finally { buffer.release(); }
        }
        return digestKey;
    }

    public MagicCrucibleBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.MAGIC_CRUCIBLE.get(), pos, state); }

    public static void tick(Level level, BlockPos pos, BlockState state, MagicCrucibleBlockEntity crucible) {
        // All crafting and enchanting belongs to the runic-altar multiblock, never to the crucible.
        crucible.refining.tick();
    }


    private OfferingEnchantingRecipe findEnchantingRecipe() {
        var choices=matchingEnchantments();
        if(!requestedEnchanting.isEmpty()) return choices.stream().filter(r->r.getId().toString().equals(requestedEnchanting)).findFirst().orElse(null);
        return choices.size()==1?choices.get(0):null;
    }

    public IItemHandler getItemHandler() { return itemCapability.orElseThrow(IllegalStateException::new); }
    public ItemStackHandler menuInventory() { return inventory; }
    public int progress() { return progress; }
    public int mana() { return mana; }
    public void dropInventory() {
        if (level == null || level.isClientSide) return;
        for (int i = 0; i < SLOT_COUNT; i++) { ItemStack stack = inventory.getStackInSlot(i); if (!stack.isEmpty()) level.addFreshEntity(new ItemEntity(level, worldPosition.getX() + .5, worldPosition.getY() + .8, worldPosition.getZ() + .5, stack.copy())); }
        clearContent();
        for (int i=0;i<refining.products.getSlots();i++) {
            ItemStack stack=refining.products.extractItem(i,64,false);
            if(!stack.isEmpty()) level.addFreshEntity(new ItemEntity(level,worldPosition.getX()+.5,worldPosition.getY()+.8,worldPosition.getZ()+.5,stack));
        }
    }
    private static boolean validSlot(int slot, ItemStack stack) {
        return slot < 14 && com.qianxin.botaniatea.content.CrucibleRefining.accepts(stack);
    }
    private void changed() { setChanged(); if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3); }

    @Override public Component getDisplayName() { return Component.translatable("block.botania_tea.magic_crucible"); }
    @Nullable @Override public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) { return null; }
    @Override public void receiveMana(int mana) { this.mana = Math.max(0, Math.min(MANA_CAPACITY, this.mana + Math.max(0, mana))); changed(); }
    @Override public int getCurrentMana() { return mana; }
    public int getMaxMana() { return MANA_CAPACITY; }
    @Override public boolean isFull() { return mana >= MANA_CAPACITY; }
    @Override public boolean canReceiveManaFromBursts() { return false; }
    @Override public BlockPos getManaReceiverPos() { return worldPosition; }
    @Override public Level getManaReceiverLevel() { return level; }

    @Override public int getContainerSize() { return SLOT_COUNT; }
    @Override public boolean isEmpty() { for (int i = 0; i < SLOT_COUNT; i++) if (!getItem(i).isEmpty()) return false; return true; }
    @Override public ItemStack getItem(int slot) { return inventory.getStackInSlot(slot); }
    @Override public ItemStack removeItem(int slot, int amount) { return inventory.extractItem(slot, amount, false); }
    @Override public ItemStack removeItemNoUpdate(int slot) { ItemStack result = inventory.getStackInSlot(slot).copy(); inventory.setStackInSlot(slot, ItemStack.EMPTY); return result; }
    @Override public void setItem(int slot, ItemStack stack) { inventory.setStackInSlot(slot, stack); }
    @Override public boolean stillValid(Player player) { return Container.stillValidBlockEntity(this, player); }
    @Override public void clearContent() { for (int i = 0; i < SLOT_COUNT; i++) inventory.setStackInSlot(i, ItemStack.EMPTY); }
    @Override public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction side) {
        if (capability == ForgeCapabilities.ITEM_HANDLER) return itemCapability.cast();
        if (capability == ForgeCapabilities.FLUID_HANDLER) return fluidCapability.cast();
        return super.getCapability(capability, side);
    }
    @Override public void invalidateCaps() { super.invalidateCaps(); itemCapability.invalidate(); fluidCapability.invalidate(); }
    @Override public void reviveCaps() { super.reviveCaps(); itemCapability = LazyOptional.of(() -> new AutomationHandler()); fluidCapability=LazyOptional.of(() -> refining.tank); }
    @Override protected void saveAdditional(CompoundTag tag) { super.saveAdditional(tag); tag.put("Inventory", inventory.serializeNBT()); tag.put("Refining",refining.save()); tag.putInt("Mana", mana); tag.putInt("Progress", progress); tag.putString("SelectedRecipe", selectedRecipe); tag.putString("AuthorizedEnchanting",authorizedEnchanting); tag.putString("RequestedEnchanting",requestedEnchanting); }
    @Override public void load(CompoundTag tag) { super.load(tag); inventory.deserializeNBT(tag.getCompound("Inventory")); refining.load(tag.getCompound("Refining")); mana = Math.max(0, Math.min(MANA_CAPACITY, tag.getInt("Mana"))); progress = tag.getInt("Progress"); selectedRecipe = tag.getString("SelectedRecipe"); authorizedEnchanting=tag.getString("AuthorizedEnchanting"); requestedEnchanting=tag.getString("RequestedEnchanting"); }

    private final class AutomationHandler implements IItemHandler {
        @Override public int getSlots() { return SLOT_COUNT + refining.products.getSlots(); }
        @Override public ItemStack getStackInSlot(int slot) { return slot<SLOT_COUNT ? inventory.getStackInSlot(slot) : refining.products.getStackInSlot(slot-SLOT_COUNT); }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) { return slot < 14 && inventory.isItemValid(slot, stack) ? inventory.insertItem(slot, stack, simulate) : stack; }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) { return slot>=SLOT_COUNT ? refining.products.extractItem(slot-SLOT_COUNT,amount,simulate) : slot >= 14 ? inventory.extractItem(slot, amount, simulate) : ItemStack.EMPTY; }
        @Override public int getSlotLimit(int slot) { return 64; }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return slot < 14 && inventory.isItemValid(slot, stack); }
    }
}
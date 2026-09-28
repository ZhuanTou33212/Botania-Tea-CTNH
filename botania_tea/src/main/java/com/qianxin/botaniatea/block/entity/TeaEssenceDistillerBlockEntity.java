package com.qianxin.botaniatea.block.entity;

import com.qianxin.botaniatea.block.TeaEssenceDistillerBlock;
import com.qianxin.botaniatea.content.TeaEssence;
import com.qianxin.botaniatea.registry.ModBlockEntities;
import com.qianxin.botaniatea.registry.ModFluids;
import com.qianxin.botaniatea.registry.ModItems;
import com.qianxin.botaniatea.registry.ModRecipes;
import com.qianxin.botaniatea.recipe.ElementDistillingRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class TeaEssenceDistillerBlockEntity extends BlockEntity implements net.minecraft.world.MenuProvider {
    public static final int CAPACITY = 8000;
    public static final int ESSENCE_PER_CYCLE = 250;
    public static final int CYCLE_TICKS = 100;
    private static final ResourceLocation MANA_POWDER = ResourceLocation.fromNamespaceAndPath("botania", "mana_powder");
    private int progress;
    private String mode = "LEGACY";
    private String selectedRecipe = "";

    private final FluidTank tank = new FluidTank(CAPACITY, stack -> essenceOf(stack) != null || stack.getFluid() == ModFluids.ASCENDED_TEA_LIQUOR.get()) {
        @Override protected void onContentsChanged() { changed(); }
    };
    private final ItemStackHandler inventory = new ItemStackHandler(4) {
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            if (slot == 0) {
                if (isAscendedMode()) return false;
                TeaEssence liquid = currentEssence();
                return liquid != null && stack.is(liquid.leaf());
            }
            if (slot == 1) return MANA_POWDER.equals(ForgeRegistries.ITEMS.getKey(stack.getItem()));
            if (slot == 3) return false;
            return false;
        }
        @Override public int getSlotLimit(int slot) { return 64; }
        @Override protected void onContentsChanged(int slot) { changed(); }
    };
    private LazyOptional<IFluidHandler> fluidCap = LazyOptional.of(() -> tank);
    private LazyOptional<IItemHandler> itemCap = LazyOptional.of(AutomationItemHandler::new);

    public TeaEssenceDistillerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TEA_ESSENCE_DISTILLER.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TeaEssenceDistillerBlockEntity be) {
        if (!"LEGACY".equals(be.mode)) { be.tickElement(level, pos, state); return; }
        TeaEssence essence = be.currentEssence();
        ItemStack result = essence == null ? ItemStack.EMPTY : essence.output();
        boolean canWork = essence != null && be.tank.getFluidAmount() >= ESSENCE_PER_CYCLE
                && be.inventory.getStackInSlot(0).is(essence.leaf())
                && !be.inventory.getStackInSlot(1).isEmpty()
                && be.canAcceptOutput(result);
        if (state.getValue(TeaEssenceDistillerBlock.WORKING) != canWork) {
            level.setBlock(pos, state.setValue(TeaEssenceDistillerBlock.WORKING, canWork), 3);
        }
        if (!canWork) {
            if (be.progress != 0) { be.progress = 0; be.changed(); }
            return;
        }
        if (++be.progress >= CYCLE_TICKS) {
            be.progress = 0;
            be.tank.drain(ESSENCE_PER_CYCLE, IFluidHandler.FluidAction.EXECUTE);
            be.inventory.extractItem(0, 1, false);
            be.inventory.extractItem(1, 1, false);
            ItemStack output = be.inventory.getStackInSlot(2);
            if (output.isEmpty()) be.inventory.setStackInSlot(2, result);
            else output.grow(result.getCount());
            be.changed();
        }
    }

    private void tickElement(Level level, BlockPos pos, BlockState state) {
        ElementDistillingRecipe recipe = findElementRecipe();
        ItemStack result = recipe == null ? ItemStack.EMPTY : recipe.getResultItem(level.registryAccess());
        boolean canWork = recipe != null && recipe.matchesInput(tank.getFluid(), inventory, level) && canAcceptOutput(result);
        if (state.getValue(TeaEssenceDistillerBlock.WORKING) != canWork)
            level.setBlock(pos, state.setValue(TeaEssenceDistillerBlock.WORKING, canWork), 3);
        if (!canWork) { if (progress != 0) { progress = 0; changed(); } return; }
        if (++progress < recipe.duration()) return;
        progress = 0;
        if (!recipe.matchesInput(tank.getFluid(), inventory, level) || !canAcceptOutput(result)) { changed(); return; }
        tank.drain(recipe.fluidAmount(), IFluidHandler.FluidAction.EXECUTE);
        if (recipe.mode() == ElementDistillingRecipe.Mode.ELEMENT) inventory.extractItem(0, 1, false);
        inventory.extractItem(1, 1, false);
        if (inventory.getStackInSlot(2).isEmpty()) inventory.setStackInSlot(2, result);
        else inventory.getStackInSlot(2).grow(result.getCount());
        changed();
    }

    @Nullable private ElementDistillingRecipe findElementRecipe() {
        if (level == null) return null;
        return level.getRecipeManager().getAllRecipesFor(ModRecipes.ELEMENT_DISTILLING.get()).stream()
                .filter(recipe -> recipe.getId().toString().equals(selectedRecipe))
                .filter(recipe -> recipe.mode().name().equals(mode)).filter(recipe -> recipe.matchesInput(tank.getFluid(), inventory, level)).findFirst().orElse(null);
    }

    public boolean isAscendedMode() { return "ASCENDED".equals(mode); }
    public String mode() { return mode; }
    public void cycleMode() { mode = switch (mode) { case "LEGACY" -> "ELEMENT"; case "ELEMENT" -> "ASCENDED"; default -> "LEGACY"; }; selectedRecipe = ""; progress = 0; cycleRecipe(); changed(); }
    public java.util.List<ElementDistillingRecipe> availableRecipes() {
        if (level == null) return java.util.List.of();
        return level.getRecipeManager().getAllRecipesFor(ModRecipes.ELEMENT_DISTILLING.get()).stream()
                .filter(r -> r.mode().name().equals(mode)).sorted(java.util.Comparator.comparing(r -> r.getId().toString())).toList();
    }
    public void cycleRecipe() {
        var recipes = availableRecipes();
        if (recipes.isEmpty()) { selectedRecipe = ""; return; }
        int index = -1;
        for (int i = 0; i < recipes.size(); i++) if (recipes.get(i).getId().toString().equals(selectedRecipe)) index = i;
        selectedRecipe = recipes.get((index + 1) % recipes.size()).getId().toString(); progress = 0; changed();
    }
    public String selectedRecipe() { return selectedRecipe; }
    public int progress() { return progress; }
    public int selectedOutputId() {
        return availableRecipes().stream().filter(r -> r.getId().toString().equals(selectedRecipe))
                .mapToInt(r -> net.minecraft.world.item.Item.getId(r.getResultItem(level.registryAccess()).getItem())).findFirst().orElse(0);
    }
    public ItemStackHandler menuInventory() { return inventory; }
    @Override public net.minecraft.network.chat.Component getDisplayName() { return net.minecraft.network.chat.Component.translatable("block.botania_tea.tea_essence_distiller"); }
    @Override public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id, net.minecraft.world.entity.player.Inventory playerInventory, net.minecraft.world.entity.player.Player player) {
        return new com.qianxin.botaniatea.menu.TeaEssenceDistillerMenu(id, playerInventory, this);
    }

    private boolean canAcceptOutput(ItemStack result) {
        if (result.isEmpty()) return false;
        ItemStack output = inventory.getStackInSlot(2);
        return output.isEmpty() || (ItemStack.isSameItemSameTags(output, result) && output.getCount() + result.getCount() <= 64);
    }

    public ItemStack takeOutput() { return inventory.extractItem(2, 64, false); }

    public IFluidHandler fluidHandler() { return tank; }

    public boolean insertBottle(TeaEssence essence) {
        FluidStack stack = new FluidStack(ModFluids.SOURCES.get(essence).get(), 250);
        return tank.fill(stack, IFluidHandler.FluidAction.SIMULATE) == 250
                && tank.fill(stack, IFluidHandler.FluidAction.EXECUTE) == 250;
    }

    public boolean insertAscendedBottle() {
        FluidStack stack = new FluidStack(ModFluids.ASCENDED_TEA_LIQUOR.get(), 250);
        return tank.fill(stack, IFluidHandler.FluidAction.SIMULATE) == 250 && tank.fill(stack, IFluidHandler.FluidAction.EXECUTE) == 250;
    }

    public ItemStack extractBottle() {
        TeaEssence essence = currentEssence();
        if (essence == null && tank.getFluid().getFluid() == ModFluids.ASCENDED_TEA_LIQUOR.get() && tank.getFluidAmount() >= 250) {
            tank.drain(250, IFluidHandler.FluidAction.EXECUTE);
            return new ItemStack(ModItems.ASCENDED_TEA_LIQUOR_BOTTLE.get());
        }
        if (essence == null || tank.getFluidAmount() < 250) return ItemStack.EMPTY;
        tank.drain(250, IFluidHandler.FluidAction.EXECUTE);
        return new ItemStack(ModItems.ESSENCE_BOTTLES.get(essence).get());
    }

    @Nullable public TeaEssence currentEssence() { return essenceOf(tank.getFluid()); }

    @Nullable private static TeaEssence essenceOf(FluidStack stack) {
        if (stack.isEmpty()) return null;
        for (TeaEssence essence : TeaEssence.values()) if (stack.getFluid() == ModFluids.SOURCES.get(essence).get()) return essence;
        return null;
    }

    private void changed() {
        setChanged();
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Tank", tank.writeToNBT(new CompoundTag()));
        tag.put("Inventory", inventory.serializeNBT());
        tag.putInt("Progress", progress);
        tag.putString("Mode", mode);
        tag.putString("SelectedRecipe", selectedRecipe);
        tag.putInt("DataVersion", 1);
    }

    @Override public void load(CompoundTag tag) {
        super.load(tag);
        tank.readFromNBT(tag.getCompound("Tank"));
        CompoundTag migratedInventory = tag.getCompound("Inventory").copy();
        migratedInventory.putInt("Size", 4);
        inventory.deserializeNBT(migratedInventory);
        progress = tag.getInt("Progress");
        mode = tag.contains("Mode") ? tag.getString("Mode") : "LEGACY";
        selectedRecipe = tag.getString("SelectedRecipe");
    }

    @Override public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction side) {
        if (capability == ForgeCapabilities.FLUID_HANDLER) return fluidCap.cast();
        if (capability == ForgeCapabilities.ITEM_HANDLER) return itemCap.cast();
        return super.getCapability(capability, side);
    }

    @Override public void invalidateCaps() {
        super.invalidateCaps();
        fluidCap.invalidate();
        itemCap.invalidate();
    }

    @Override public void reviveCaps() {
        super.reviveCaps();
        fluidCap = LazyOptional.of(() -> tank);
        itemCap = LazyOptional.of(AutomationItemHandler::new);
    }

    private final class AutomationItemHandler implements IItemHandler {
        @Override public int getSlots() { return inventory.getSlots(); }

        @Override public @NotNull ItemStack getStackInSlot(int slot) {
            return inventory.getStackInSlot(slot);
        }

        @Override public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            return slot == 2 ? stack : inventory.insertItem(slot, stack, simulate);
        }

        @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot >= 2 ? inventory.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }

        @Override public int getSlotLimit(int slot) { return inventory.getSlotLimit(slot); }

        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return slot != 2 && inventory.isItemValid(slot, stack);
        }
    }
}

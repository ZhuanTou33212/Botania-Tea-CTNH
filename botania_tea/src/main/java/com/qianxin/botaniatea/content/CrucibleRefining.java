package com.qianxin.botaniatea.content;

import com.qianxin.botaniatea.block.entity.MagicCrucibleBlockEntity;
import com.qianxin.botaniatea.item.EssenceBottleItem;
import com.qianxin.botaniatea.registry.ModFluids;
import com.qianxin.botaniatea.registry.ModItems;
import com.qianxin.botaniatea.registry.ModRecipes;
import com.qianxin.botaniatea.recipe.ElementDistillingRecipe;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.ItemStackHandler;
import java.util.*;

/** Separate raw input and product buffers. Only complete batches may commit. */
public final class CrucibleRefining {
    public static final int BATCH_MB=1000, BATCH_MANA=1000, BATCH_TICKS=100;
    private final MagicCrucibleBlockEntity owner;
    public final ItemStackHandler products;
    public final FluidTank tank;
    private int progress;
    private String recipeKey="";
    public CrucibleRefining(MagicCrucibleBlockEntity owner) {
        this.owner=owner;
        products=new ItemStackHandler(24) {
            @Override protected void onContentsChanged(int slot) { owner.setChanged(); }
        };
        tank=new FluidTank(8000, fluid -> TeaEssence.fromEssenceFluid(fluid.getFluid())!=null) {
            @Override protected void onContentsChanged() { owner.setChanged(); }
            // External fluid extraction cannot take unprocessed source material.
            @Override public FluidStack drain(int amount, IFluidHandler.FluidAction action) { return FluidStack.EMPTY; }
            @Override public FluidStack drain(FluidStack fluid, IFluidHandler.FluidAction action) { return FluidStack.EMPTY; }
        };
    }
    public static boolean accepts(ItemStack stack) {
        if(stack.getItem() instanceof EssenceBottleItem) return true;
        return ModItems.ESSENCE_BUCKETS.values().stream().anyMatch(bucket -> stack.is(bucket.get()));
    }
    private List<ItemStack> batch() {
        var level=owner.getLevel();
        if(level==null || tank.getFluidAmount()<BATCH_MB) return List.of();
        var unique=new TreeMap<String,ItemStack>();
        for(var recipe:level.getRecipeManager().getAllRecipesFor(ModRecipes.ELEMENT_DISTILLING.get())) {
            if(recipe.mode()!=ElementDistillingRecipe.Mode.ELEMENT || !recipe.fluidId().equals(net.minecraftforge.registries.ForgeRegistries.FLUIDS.getKey(tank.getFluid().getFluid()))) continue;
            ItemStack result=recipe.getResultItem(level.registryAccess());
            if(!ModItems.isElementCrystal(result)) continue;
            result.setCount(1); unique.put(net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(result.getItem()).toString(),result);
        }
        return List.copyOf(unique.values());
    }
    private ItemStackHandler preview(List<ItemStack> additions) {
        ItemStackHandler copy=new ItemStackHandler(24); copy.deserializeNBT(products.serializeNBT());
        for(ItemStack addition:additions) {
            ItemStack remainder=addition.copy();
            for(int i=0;i<copy.getSlots() && !remainder.isEmpty();i++) remainder=copy.insertItem(i,remainder,false);
            if(!remainder.isEmpty()) return null;
        }
        return copy;
    }
    public void tick() {
        var level=owner.getLevel(); if(level==null || level.isClientSide) return;
        // A bottle is 250 mB; a source bucket is 1000 mB. Return containers are products.
        for(int slot=0;slot<14;slot++) {
            ItemStack stack=owner.menuInventory().getStackInSlot(slot);
            TeaEssence source=null; int amount=1000; ItemStack returned=new ItemStack(Items.BUCKET);
            if(stack.getItem() instanceof EssenceBottleItem bottle) { source=bottle.essence(); amount=250; returned=new ItemStack(Items.GLASS_BOTTLE); }
            else for(var entry:ModItems.ESSENCE_BUCKETS.entrySet()) if(stack.is(entry.getValue().get())) { source=entry.getKey(); break; }
            if(source==null) continue;
            FluidStack fluid=new FluidStack(ModFluids.SOURCES.get(source).get(),amount);
            ItemStackHandler next=preview(List.of(returned));
            if(next!=null && tank.fill(fluid,IFluidHandler.FluidAction.SIMULATE)==amount) {
                tank.fill(fluid,IFluidHandler.FluidAction.EXECUTE);
                owner.menuInventory().extractItem(slot,1,false);
                products.deserializeNBT(next.serializeNBT()); owner.setChanged();
            }
            break;
        }
        List<ItemStack> results=batch();
        String key=tank.getFluid().getFluid().toString()+results.toString();
        if(!key.equals(recipeKey)) { recipeKey=key; progress=0; }
        boolean working=!results.isEmpty() && preview(results)!=null && NearbyPoolMana.take(level,owner.getBlockPos(),BATCH_MANA,true);
        var state=owner.getBlockState();
        if(state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT)!=working)
            level.setBlock(owner.getBlockPos(),state.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT,working),3);
        if(!working) { progress=0; return; }
        if(++progress<BATCH_TICKS) return;
        ItemStackHandler next=preview(results);
        if(next==null || !NearbyPoolMana.take(level,owner.getBlockPos(),BATCH_MANA,false)) return;
        FluidStack remaining=tank.getFluid().copy(); remaining.shrink(BATCH_MB); tank.setFluid(remaining);
        products.deserializeNBT(next.serializeNBT()); progress=0; owner.setChanged();
    }
    public CompoundTag save() { var tag=new CompoundTag(); tag.put("Tank",tank.writeToNBT(new CompoundTag())); tag.put("Products",products.serializeNBT()); return tag; }
    public void load(CompoundTag tag) {
        tank.readFromNBT(tag.getCompound("Tank"));
        if(tag.contains("Products")) { var saved=tag.getCompound("Products").copy(); saved.putInt("Size",24); products.deserializeNBT(saved); }
        progress=0; recipeKey="";
    }
}

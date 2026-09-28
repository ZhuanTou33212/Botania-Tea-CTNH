package com.qianxin.botaniatea.block.entity;

import com.qianxin.botaniatea.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

/** One stack means one item type, including its NBT. */
public final class ManaOfferingBlockEntity extends BlockEntity {
    public int nextFilter;
    public int ritualProgress;
    public String ritualRecipe = "";
    /** Seed for the enchantment roll; redrawn whenever a new craft starts so price and result agree. */
    public long ritualSeed;
    public boolean foundationFormed;
    /** Set by using any wand of the forest on the altar; cleared when the livingrock base breaks. */
    public boolean circleActive;
    public BlockPos boundPool;
    public void resetRitual() {
        if (ritualProgress != 0 || !ritualRecipe.isEmpty()) {
            ritualProgress=0; ritualRecipe=""; setChanged();
        }
    }
    /** One stack means one item type, including its NBT. Storage is capped at Short.MAX_VALUE. */
    public static final int SLOT_LIMIT = Short.MAX_VALUE;
    public final ItemStackHandler inventory = new ItemStackHandler(1) {
        @Override public int getSlotLimit(int slot) { return SLOT_LIMIT; }
        @Override protected void onContentsChanged(int slot) {
            setChanged();
            if (level != null && !level.isClientSide)
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    };
    private LazyOptional<IItemHandler> items = LazyOptional.of(() -> inventory);
    public ManaOfferingBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MANA_OFFERING.get(), pos, state);
    }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag); tag.put("Offering", inventory.serializeNBT());
        tag.putInt("NextFilter", nextFilter);
        tag.putBoolean("FoundationFormed", foundationFormed); tag.putBoolean("CircleActive", circleActive);
        if (boundPool != null) tag.putLong("BoundPool", boundPool.asLong());
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        nextFilter=Math.max(0,tag.getInt("NextFilter"));
        foundationFormed=tag.getBoolean("FoundationFormed"); circleActive=tag.getBoolean("CircleActive");
        boundPool=tag.contains("BoundPool") ? BlockPos.of(tag.getLong("BoundPool")) : null;
        ritualProgress=0; ritualRecipe="";
        if (tag.contains("Offering")) {
            CompoundTag saved = tag.getCompound("Offering").copy();
            saved.putInt("Size", 1);
            inventory.deserializeNBT(saved);
        }
    }
    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        return cap == ForgeCapabilities.ITEM_HANDLER ? items.cast() : super.getCapability(cap, side);
    }
    @Override public void invalidateCaps() { super.invalidateCaps(); items.invalidate(); }
    @Override public void reviveCaps() { super.reviveCaps(); items = LazyOptional.of(() -> inventory); }
}

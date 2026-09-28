package com.qianxin.teaart.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.ItemStackHandler;

import com.qianxin.teaart.menu.BambooPipeInputPortMenu;
import com.qianxin.teaart.registry.ModBlockEntities;
import org.jetbrains.annotations.Nullable;

public class BambooPipeOutputPortBlockEntity extends BlockEntity implements MenuProvider, FilteredPipePort {

    private final ItemStackHandler filters = new ItemStackHandler(FILTER_SLOTS) {

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private boolean whitelist = true;

    public BambooPipeOutputPortBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BAMBOO_PIPE_OUTPUT_PORT.get(), pos, state);
    }

    @Override
    public ItemStackHandler getFilters() {
        return filters;
    }

    @Override
    public boolean isWhitelist() {
        return whitelist;
    }

    @Override
    public void toggleMode() {
        whitelist = !whitelist;
        setChanged();
    }

    public boolean allows(ItemStack stack) {
        return BambooPipeInputPortBlockEntity.allows(filters, whitelist, stack);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.tea_art.bamboo_pipe_output_port");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new BambooPipeInputPortMenu(containerId, inventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Filters", filters.serializeNBT());
        tag.putBoolean("Whitelist", whitelist);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        filters.deserializeNBT(tag.getCompound("Filters"));
        whitelist = !tag.contains("Whitelist") || tag.getBoolean("Whitelist");
    }
}

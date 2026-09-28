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

import com.qianxin.teaart.item.TeaProcessingData;
import com.qianxin.teaart.menu.BambooPipeInputPortMenu;
import com.qianxin.teaart.registry.ModBlockEntities;
import org.jetbrains.annotations.Nullable;

public class BambooPipeInputPortBlockEntity extends BlockEntity implements MenuProvider, FilteredPipePort {

    private final ItemStackHandler filters = new ItemStackHandler(FILTER_SLOTS) {

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private boolean whitelist = true;

    public BambooPipeInputPortBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BAMBOO_PIPE_INPUT_PORT.get(), pos, state);
    }

    public ItemStackHandler getFilters() {
        return filters;
    }

    public boolean isWhitelist() {
        return whitelist;
    }

    public void toggleMode() {
        whitelist = !whitelist;
        setChanged();
    }

    public boolean allows(ItemStack stack) {
        return allows(filters, whitelist, stack);
    }

    public static boolean allows(ItemStackHandler filters, boolean whitelist, ItemStack stack) {
        boolean hasFilter = false;
        boolean matched = false;
        for (int i = 0; i < filters.getSlots(); i++) {
            ItemStack filter = filters.getStackInSlot(i);
            if (filter.isEmpty()) {
                continue;
            }
            hasFilter = true;
            if (TeaProcessingData.matchesFilter(filter, stack)) {
                matched = true;
                break;
            }
        }
        if (!hasFilter) {
            return true;
        }
        return whitelist == matched;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.tea_art.bamboo_pipe_input_port");
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

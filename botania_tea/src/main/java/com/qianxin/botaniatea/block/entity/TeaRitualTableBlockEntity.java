package com.qianxin.botaniatea.block.entity;

import com.qianxin.botaniatea.entity.TeaMountainSpiritEntity;
import com.qianxin.botaniatea.item.AscendedTeaBottleItem;
import com.qianxin.botaniatea.item.EssenceBottleItem;
import com.qianxin.botaniatea.registry.ModBlockEntities;
import com.qianxin.botaniatea.registry.ModEntities;
import com.qianxin.botaniatea.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class TeaRitualTableBlockEntity extends BlockEntity {
    private final ItemStackHandler inventory = new ItemStackHandler(8) {
        @Override public int getSlotLimit(int slot) { return 1; }
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return validSlot(slot, stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    };
    private LazyOptional<ItemStackHandler> itemHandler = LazyOptional.of(() -> inventory);

    public TeaRitualTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TEA_RITUAL_TABLE.get(), pos, state);
    }

    public boolean insertFromPlayer(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        int slot = slotFor(held);
        if (slot < 0 || !validSlot(slot, held) || !inventory.getStackInSlot(slot).isEmpty()) return false;
        if (!level.isClientSide) {
            ItemStack one = held.copy();
            one.setCount(1);
            inventory.setStackInSlot(slot, one);
            if (!player.getAbilities().instabuild) held.shrink(1);
        }
        return true;
    }

    public boolean trySummon(Player player) {
        if (level == null || level.isClientSide || !(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) return true;
        if (!validRitual()) return false;
        if (!level.getEntitiesOfClass(TeaMountainSpiritEntity.class, new AABB(worldPosition).inflate(32)).isEmpty()) return false;
        TeaMountainSpiritEntity spirit = ModEntities.TEA_MOUNTAIN_SPIRIT.get().create(level);
        if (spirit == null) return false;
        spirit.moveTo(worldPosition.getX() + 0.5D, worldPosition.getY() + 1.0D, worldPosition.getZ() + 0.5D,
                player.getYRot(), 0.0F);
        if (!level.getWorldBorder().isWithinBounds(spirit.getBoundingBox()) || !level.noCollision(spirit)
                || !level.getFluidState(worldPosition.above()).isEmpty()
                || !level.getFluidState(worldPosition.above(2)).isEmpty()
                || !level.getFluidState(worldPosition.above(3)).isEmpty()) return false;
        spirit.setSummoner(player.getUUID());
        if (!level.addFreshEntity(spirit)) return false;
        for (int i = 0; i < 8; i++) inventory.extractItem(i, 1, false);
        ItemStack bottles = new ItemStack(Items.GLASS_BOTTLE, 7);
        if (!player.getInventory().add(bottles)) player.drop(bottles, false);
        level.playSound(null, worldPosition, com.qianxin.botaniatea.registry.ModSounds.RITUAL_SUMMON.get(),
                net.minecraft.sounds.SoundSource.BLOCKS, 1F, .8F);
        return true;
    }

    private boolean validRitual() {
        for (int i = 0; i < 8; i++) if (inventory.getStackInSlot(i).isEmpty() || !validSlot(i, inventory.getStackInSlot(i))) return false;
        return true;
    }

    private static boolean validSlot(int slot, ItemStack stack) {
        return slot < 6 ? stack.getItem() instanceof EssenceBottleItem essence && essence.essence().ordinal() == slot
                : slot == 6 ? stack.getItem() instanceof AscendedTeaBottleItem
                : slot == 7 && stack.is(ModItems.MOUNTAIN_INVITATION.get());
    }

    private static int slotFor(ItemStack stack) {
        if (stack.getItem() instanceof EssenceBottleItem essence) return essence.essence().ordinal();
        if (stack.getItem() instanceof AscendedTeaBottleItem) return 6;
        if (stack.is(ModItems.MOUNTAIN_INVITATION.get())) return 7;
        return -1;
    }

    public void dropContents() {
        if (level == null || level.isClientSide) return;
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.extractItem(i, inventory.getStackInSlot(i).getCount(), false);
            if (!stack.isEmpty()) level.addFreshEntity(new ItemEntity(level, worldPosition.getX() + .5D,
                    worldPosition.getY() + .8D, worldPosition.getZ() + .5D, stack));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", inventory.serializeNBT());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Inventory")) inventory.deserializeNBT(tag.getCompound("Inventory"));
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemHandler.invalidate();
    }

    @Override public void reviveCaps() { super.reviveCaps(); itemHandler = LazyOptional.of(() -> inventory); }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable net.minecraft.core.Direction side) {
        return cap == ForgeCapabilities.ITEM_HANDLER ? itemHandler.cast() : super.getCapability(cap, side);
    }
}

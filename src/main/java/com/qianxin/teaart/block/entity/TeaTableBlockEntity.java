package com.qianxin.teaart.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;

import com.qianxin.teaart.block.TeaTableBlock;
import com.qianxin.teaart.content.TeaLiquor;
import com.qianxin.teaart.menu.TeaTableMenu;
import com.qianxin.teaart.registry.ModBlockEntities;
import com.qianxin.teaart.registry.ModItems;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TeaTableBlockEntity extends BlockEntity implements MenuProvider {

    // 输入：茶叶 / 茶杯 / 水桶 / 燃料；输出：茶汤 / 茶渣
    public static final int LEAF_SLOT = 0;
    public static final int CUP_SLOT = 1;
    public static final int WATER_SLOT = 2;
    public static final int FUEL_SLOT = 3;
    public static final int OUTPUT_SLOT = 4;
    public static final int RESIDUE_SLOT = 5;
    public static final int SLOT_COUNT = 6;
    public static final int CAPACITY = 1000;
    public static final int WATER_PER_BREW = 250;
    /** 无燃料时的兜底泡茶时长（tick）；实际时长等于燃料的燃烧时间。 */
    public static final int FALLBACK_BREW_TICKS = 10 * 20;
    private static final int BUCKET_BREW_AMOUNT = 4;

    private final ItemStack[] items = new ItemStack[] {
            ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
            ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY
    };
    private int water;
    private int brewTicks;
    private int burnTime;
    private int burnDuration;
    private final LazyOptional<IItemHandler> itemHandler = LazyOptional.of(() -> new Handler());

    public TeaTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TEA_TABLE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TeaTableBlockEntity table) {
        if (level.isClientSide) {
            return;
        }
        boolean changed = false;
        // 水槽：水桶 → 注入 1000mB 并返还空桶
        if (table.items[WATER_SLOT].is(Items.WATER_BUCKET) && table.water <= CAPACITY - 1000) {
            table.water = Math.min(CAPACITY, table.water + 1000);
            table.items[WATER_SLOT] = new ItemStack(Items.BUCKET);
            changed = true;
        }
        // 点火：消耗 1 个燃料；其燃烧时间决定本次泡茶时长
        if (!table.isBurning() && table.canLight()) {
            table.burnDuration = ForgeHooks.getBurnTime(table.items[FUEL_SLOT], null);
            table.burnTime = table.burnDuration;
            table.items[FUEL_SLOT].shrink(1);
            if (table.items[FUEL_SLOT].isEmpty()) {
                table.items[FUEL_SLOT] = ItemStack.EMPTY;
            }
            changed = true;
        }
        // 泡茶进度与燃料燃烧时间同步
        if (table.isBurning() && table.canBrew()) {
            table.brewTicks++;
            changed = true;
            if (table.brewTicks >= table.getRequiredBrewTicks()) {
                table.finishBrew();
                table.brewTicks = 0;
                // 1 份燃料 = 1 次泡茶，结束时烧尽剩余燃烧值
                table.burnTime = 0;
                table.burnDuration = 0;
                table.sync();
                return;
            }
            // 泡茶时少量烟雾粒子
            if (level instanceof net.minecraft.server.level.ServerLevel server && level.getGameTime() % 8 == 0) {
                server.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE,
                        pos.getX() + 0.5D, pos.getY() + 1.05D, pos.getZ() + 0.5D,
                        1, 0.16D, 0.05D, 0.16D, 0.008D);
            }
        } else if (table.brewTicks != 0) {
            table.brewTicks = 0;
            changed = true;
        }
        if (table.isBurning()) {
            table.burnTime--;
            changed = true;
        }
        if (changed) {
            table.setChanged();
        }
    }

    public IItemHandler getItemHandler() {
        return itemHandler.orElseThrow(IllegalStateException::new);
    }

    public int getWater() {
        return water;
    }

    public void setWater(int water) {
        this.water = water;
    }

    public int getBrewTicks() {
        return brewTicks;
    }

    public void setBrewTicks(int brewTicks) {
        this.brewTicks = brewTicks;
    }

    /** 本次泡茶时长 = 当前燃料的燃烧时间；没有燃料时用兜底值。 */
    public int getRequiredBrewTicks() {
        return burnDuration > 0 ? burnDuration : FALLBACK_BREW_TICKS;
    }

    public int getBurnTime() {
        return burnTime;
    }

    public void setBurnTime(int burnTime) {
        this.burnTime = burnTime;
    }

    public int getBurnDuration() {
        return burnDuration;
    }

    public void setBurnDuration(int burnDuration) {
        this.burnDuration = burnDuration;
    }

    public boolean mayPlace(int slot, ItemStack stack) {
        return switch (slot) {
            case LEAF_SLOT -> isTeaLeaf(stack);
            case CUP_SLOT -> stack.is(ModItems.TEA_CUP.get());
            case WATER_SLOT -> stack.is(Items.WATER_BUCKET) && water <= CAPACITY - 1000 || stack.is(Items.BUCKET);
            case FUEL_SLOT -> ForgeHooks.getBurnTime(stack, null) > 0;
            default -> false;
        };
    }

    public boolean mayTake(int slot) {
        return slot >= 0 && slot < SLOT_COUNT;
    }

    public void dropContents(Level level) {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                level.addFreshEntity(new ItemEntity(level, worldPosition.getX() + 0.5D, worldPosition.getY() + 1.0D,
                        worldPosition.getZ() + 0.5D, stack.copy()));
            }
        }
    }

    private boolean canBrew() {
        boolean bucketBrew = isBucketBrew();
        int amount = bucketBrew ? BUCKET_BREW_AMOUNT : 1;
        int waterCost = WATER_PER_BREW * amount;
        ItemStack result = bucketBrew ? getTeaLiquorBucket(items[LEAF_SLOT]) : getTeaDrink(items[LEAF_SLOT]);
        if (result.isEmpty() || water < waterCost || items[LEAF_SLOT].getCount() < amount ||
                (!bucketBrew && items[CUP_SLOT].isEmpty()) || !canAcceptResidue(amount)) {
            return false;
        }
        ItemStack output = items[OUTPUT_SLOT];
        return output.isEmpty() || ItemStack.isSameItemSameTags(output, result) &&
                output.getCount() + result.getCount() <= output.getMaxStackSize();
    }

    private boolean canLight() {
        return canBrew() && ForgeHooks.getBurnTime(items[FUEL_SLOT], null) > 0;
    }

    private boolean isBurning() {
        return burnTime > 0;
    }

    private void finishBrew() {
        boolean bucketBrew = isBucketBrew();
        int amount = bucketBrew ? BUCKET_BREW_AMOUNT : 1;
        ItemStack result = bucketBrew ? getTeaLiquorBucket(items[LEAF_SLOT]) : getTeaDrink(items[LEAF_SLOT]);
        if (items[OUTPUT_SLOT].isEmpty()) {
            items[OUTPUT_SLOT] = result.copy();
        } else {
            items[OUTPUT_SLOT].grow(result.getCount());
        }
        // 茶 + 茶渣：每消耗 1 片茶叶产出 1 份茶渣
        addResidue(amount);
        water -= WATER_PER_BREW * amount;
        items[LEAF_SLOT].shrink(amount);
        if (bucketBrew) {
            items[WATER_SLOT].shrink(1);
        } else {
            items[CUP_SLOT].shrink(1);
        }
        if (items[LEAF_SLOT].isEmpty()) items[LEAF_SLOT] = ItemStack.EMPTY;
        if (items[CUP_SLOT].isEmpty()) items[CUP_SLOT] = ItemStack.EMPTY;
        if (items[WATER_SLOT].isEmpty()) items[WATER_SLOT] = ItemStack.EMPTY;
    }

    /** 把茶渣放入输出槽；槽满（64）时 canBrew() 会阻止继续泡茶。 */
    private void addResidue(int amount) {
        ItemStack residue = items[RESIDUE_SLOT];
        if (residue.isEmpty()) {
            items[RESIDUE_SLOT] = new ItemStack(ModItems.TEA_RESIDUE.get(), amount);
        } else {
            residue.grow(amount);
        }
    }

    private boolean canAcceptResidue(int amount) {
        ItemStack residue = items[RESIDUE_SLOT];
        return residue.isEmpty() ||
                (residue.is(ModItems.TEA_RESIDUE.get()) && residue.getCount() + amount <= residue.getMaxStackSize());
    }

    private boolean isBucketBrew() {
        return items[CUP_SLOT].isEmpty() && items[WATER_SLOT].is(Items.BUCKET);
    }

    private static boolean isTeaLeaf(ItemStack stack) {
        return stack.is(ModItems.GREEN_TEA_LEAF.get()) || stack.is(ModItems.WHITE_TEA_LEAF.get()) ||
                stack.is(ModItems.YELLOW_TEA_LEAF.get()) || stack.is(ModItems.RED_TEA_LEAF.get()) ||
                stack.is(ModItems.BLACK_TEA_LEAF.get()) || stack.is(ModItems.OOLONG_TEA_LEAF.get()) ||
                stack.is(ModItems.JASMINE_FLOWER_TEA_LEAF.get()) || stack.is(ModItems.GARDENIA_TEA_LEAF.get());
    }

    private static ItemStack getTeaDrink(ItemStack leaf) {
        if (leaf.is(ModItems.GREEN_TEA_LEAF.get())) return new ItemStack(ModItems.GREEN_TEA.get());
        if (leaf.is(ModItems.WHITE_TEA_LEAF.get())) return new ItemStack(ModItems.WHITE_TEA.get());
        if (leaf.is(ModItems.YELLOW_TEA_LEAF.get())) return new ItemStack(ModItems.YELLOW_TEA.get());
        if (leaf.is(ModItems.RED_TEA_LEAF.get())) return new ItemStack(ModItems.RED_TEA.get());
        if (leaf.is(ModItems.BLACK_TEA_LEAF.get())) return new ItemStack(ModItems.BLACK_TEA.get());
        if (leaf.is(ModItems.OOLONG_TEA_LEAF.get())) {
            return new ItemStack(ModItems.OOLONG_TEA.get());
        }
        if (leaf.is(ModItems.JASMINE_FLOWER_TEA_LEAF.get())) return new ItemStack(ModItems.JASMINE_FLOWER_TEA.get());
        if (leaf.is(ModItems.GARDENIA_TEA_LEAF.get())) return new ItemStack(ModItems.GARDENIA_TEA.get());
        return ItemStack.EMPTY;
    }

    private static ItemStack getTeaLiquorBucket(ItemStack leaf) {
        TeaLiquor liquor = null;
        if (leaf.is(ModItems.GREEN_TEA_LEAF.get())) liquor = TeaLiquor.GREEN;
        else if (leaf.is(ModItems.WHITE_TEA_LEAF.get())) liquor = TeaLiquor.WHITE;
        else if (leaf.is(ModItems.YELLOW_TEA_LEAF.get())) liquor = TeaLiquor.YELLOW;
        else if (leaf.is(ModItems.RED_TEA_LEAF.get())) liquor = TeaLiquor.RED;
        else if (leaf.is(ModItems.BLACK_TEA_LEAF.get())) liquor = TeaLiquor.BLACK;
        else if (leaf.is(ModItems.OOLONG_TEA_LEAF.get())) liquor = TeaLiquor.OOLONG;
        else if (leaf.is(ModItems.JASMINE_FLOWER_TEA_LEAF.get())) liquor = TeaLiquor.JASMINE_SCENTED;
        else if (leaf.is(ModItems.GARDENIA_TEA_LEAF.get())) liquor = TeaLiquor.GARDENIA_SCENTED;
        return liquor == null ? ItemStack.EMPTY : new ItemStack(ModItems.TEA_LIQUOR_BUCKETS.get(liquor).get());
    }

    private void sync() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.tea_art.tea_table");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new TeaTableMenu(containerId, inventory, this);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemHandler.invalidate();
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            if (side == null) {
                return itemHandler.cast();
            }
            return LazyOptional.<IItemHandler>of(() -> new SidedHandler(side)).cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Water", water);
        tag.putInt("BrewTicks", brewTicks);
        tag.putInt("BurnTime", burnTime);
        tag.putInt("BurnDuration", burnDuration);
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (!items[i].isEmpty()) {
                tag.put("Item" + i, items[i].save(new CompoundTag()));
            }
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        water = tag.getInt("Water");
        brewTicks = tag.getInt("BrewTicks");
        burnTime = tag.getInt("BurnTime");
        burnDuration = tag.getInt("BurnDuration");
        for (int i = 0; i < SLOT_COUNT; i++) {
            items[i] = tag.contains("Item" + i) ? ItemStack.of(tag.getCompound("Item" + i)) : ItemStack.EMPTY;
        }
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    private class Handler implements IItemHandlerModifiable {

        @Override
        public int getSlots() {
            return SLOT_COUNT;
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            return items[slot];
        }

        @Override
        public void setStackInSlot(int slot, @NotNull ItemStack stack) {
            if (slot < 0 || slot >= SLOT_COUNT) {
                return;
            }
            items[slot] = stack.copy();
            sync();
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (!mayPlace(slot, stack)) {
                return stack;
            }
            if (!items[slot].isEmpty() && !ItemStack.isSameItemSameTags(items[slot], stack)) {
                return stack;
            }
            int limit = 64;
            int inserted = Math.min(stack.getCount(), limit - items[slot].getCount());
            if (inserted <= 0) {
                return stack;
            }
            ItemStack remainder = stack.copy();
            if (!simulate) {
                if (items[slot].isEmpty()) {
                    items[slot] = stack.copy();
                    items[slot].setCount(inserted);
                } else {
                    items[slot].grow(inserted);
                }
                sync();
            }
            remainder.shrink(inserted);
            return remainder;
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (!mayTake(slot) || amount <= 0 || items[slot].isEmpty()) {
                return ItemStack.EMPTY;
            }
            int extracted = Math.min(amount, items[slot].getCount());
            ItemStack result = items[slot].copy();
            result.setCount(extracted);
            if (!simulate) {
                items[slot].shrink(extracted);
                if (items[slot].isEmpty()) {
                    items[slot] = ItemStack.EMPTY;
                }
                sync();
            }
            return result;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return mayPlace(slot, stack);
        }
    }

    private class SidedHandler extends Handler {

        private final Direction side;

        SidedHandler(Direction side) {
            this.side = side;
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (!canSideInsert(slot)) {
                return stack;
            }
            return super.insertItem(slot, stack, simulate);
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (side != Direction.DOWN || (slot != OUTPUT_SLOT && slot != RESIDUE_SLOT)) {
                return ItemStack.EMPTY;
            }
            return super.extractItem(slot, amount, simulate);
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return canSideInsert(slot) && super.isItemValid(slot, stack);
        }

        private boolean canSideInsert(int slot) {
            if (side == Direction.DOWN) {
                return false;
            }
            Direction back = getBlockState().hasProperty(TeaTableBlock.FACING) ?
                    getBlockState().getValue(TeaTableBlock.FACING).getOpposite() : Direction.SOUTH;
            if (side == back) {
                return slot == FUEL_SLOT || slot == WATER_SLOT;
            }
            return side.getAxis().isHorizontal() && (slot == CUP_SLOT || slot == LEAF_SLOT);
        }
    }
}

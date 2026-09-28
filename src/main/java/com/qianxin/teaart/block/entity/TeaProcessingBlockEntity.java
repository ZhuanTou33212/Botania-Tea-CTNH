package com.qianxin.teaart.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;

import com.qianxin.teaart.block.TeaProcessingBlock;
import com.qianxin.teaart.registry.ModBlockEntities;
import com.qianxin.teaart.registry.ModBlocks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class TeaProcessingBlockEntity extends BlockEntity implements com.qianxin.teaart.api.TimedTeaDevice {

    @Override
    public boolean canAdvanceProcessing() {
        return isProcessing() && !inputStack.isEmpty();
    }

    private int processIndex = -1;
    private int processTicks;
    private int totalProcessTicks;
    private ItemStack inputStack = ItemStack.EMPTY;
    private final LazyOptional<IItemHandler> itemHandler = LazyOptional.of(() -> new Handler());

    public TeaProcessingBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TEA_PROCESSING.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TeaProcessingBlockEntity processing) {
        if (!processing.isProcessing()) {
            return;
        }

        if (level.isClientSide) {
            spawnParticles(level, pos, state);
            processing.processTicks--;
            return;
        }

        processing.processTicks--;
        playWorkingSound(level, pos, state, processing.processTicks);
        if (processing.processTicks == 0) {
            processing.sync();
            level.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.8F, 1.2F);
        }
    }

    public boolean isIdle() {
        return processIndex < 0;
    }

    public boolean isProcessing() {
        return processIndex >= 0 && processTicks > 0;
    }

    public boolean isReady() {
        return processIndex >= 0 && processTicks <= 0;
    }

    public int getProcessIndex() {
        return processIndex;
    }

    public int getSecondsRemaining() {
        return Math.max(1, (processTicks + 19) / 20);
    }

    /** Server-side tool hook; it never creates output directly, preserving the normal completion path. */
    public boolean tryAdvanceProcessing(int ticks) {
        if (level == null || level.isClientSide || !canAdvanceProcessing() || ticks <= 0) return false;
        processTicks = Math.max(0, processTicks - ticks);
        sync();
        return true;
    }

    public void startProcess(int processIndex, int durationTicks, ItemStack inputStack) {
        this.processIndex = processIndex;
        this.processTicks = durationTicks;
        this.totalProcessTicks = durationTicks;
        this.inputStack = inputStack.copy();
        this.inputStack.setCount(1);
        sync();
    }

    public ItemStack getInputStack() {
        return inputStack.copy();
    }

    public void clear() {
        processIndex = -1;
        processTicks = 0;
        totalProcessTicks = 0;
        inputStack = ItemStack.EMPTY;
        sync();
    }

    public boolean canInsert(ItemStack stack) {
        return isIdle() && findProcess(stack) >= 0;
    }

    public boolean hasOutput() {
        return isReady() && !inputStack.isEmpty() && getResult().isEmpty() == false;
    }

    public ItemStack getResult() {
        if (!(getBlockState().getBlock() instanceof TeaProcessingBlock block)) {
            return ItemStack.EMPTY;
        }
        List<TeaProcessingBlock.TeaProcess> processes = block.getProcesses();
        if (processIndex < 0 || processIndex >= processes.size()) {
            return ItemStack.EMPTY;
        }
        return processes.get(processIndex).apply(inputStack);
    }

    private int findProcess(ItemStack stack) {
        if (!(getBlockState().getBlock() instanceof TeaProcessingBlock block)) {
            return -1;
        }
        List<TeaProcessingBlock.TeaProcess> processes = block.getProcesses();
        for (int i = 0; i < processes.size(); i++) {
            TeaProcessingBlock.TeaProcess process = processes.get(i);
            if (!process.requiresTool() && process.canApply(stack)) {
                return i;
            }
        }
        return -1;
    }

    private void sync() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private static void spawnParticles(Level level, BlockPos pos, BlockState state) {
        RandomSource random = level.random;
        double x = pos.getX() + 0.5D + (random.nextDouble() - 0.5D) * 0.45D;
        double y = pos.getY() + 0.82D;
        double z = pos.getZ() + 0.5D + (random.nextDouble() - 0.5D) * 0.45D;
        Block block = state.getBlock();

        if (block == ModBlocks.CHARCOAL_ROASTER.get()) {
            if (random.nextFloat() < 0.25F) {
                level.addParticle(ParticleTypes.FLAME, x, y, z, 0.0D, 0.015D, 0.0D);
            }
            if (random.nextFloat() < 0.08F) {
                level.addParticle(ParticleTypes.LAVA, x, y + 0.05D, z, 0.0D, 0.0D, 0.0D);
            }
            return;
        }

        if (block == ModBlocks.TEA_ROASTING_PAN.get() || block == ModBlocks.TEA_STOVE.get()) {
            if (random.nextFloat() < 0.25F) {
                level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y, z, 0.0D, 0.025D, 0.0D);
            }
            if (random.nextFloat() < 0.08F) {
                level.addParticle(ParticleTypes.FLAME, x, y - 0.08D, z, 0.0D, 0.015D, 0.0D);
            }
            return;
        }

        if (block == ModBlocks.FERMENTATION_VAT.get()) {
            level.addParticle(ParticleTypes.CLOUD, x, y, z, 0.0D, 0.025D, 0.0D);
            if (random.nextFloat() < 0.45F) {
                level.addParticle(ParticleTypes.BUBBLE_POP, x, y - 0.04D, z, 0.0D, 0.01D, 0.0D);
            }
            return;
        }

        if (block == ModBlocks.WITHERING_TRAY.get()) {
            if (random.nextFloat() < 0.25F) {
                level.addParticle(ParticleTypes.HAPPY_VILLAGER, x, y, z, 0.0D, 0.015D, 0.0D);
            }
            return;
        }

        if (block == ModBlocks.TEA_SIEVE_RACK.get()) {
            level.addParticle(ParticleTypes.HAPPY_VILLAGER, x, y, z, 0.0D, 0.015D, 0.0D);
            return;
        }

        if (block == ModBlocks.ROLLING_TABLE.get()) {
            level.addParticle(ParticleTypes.COMPOSTER, x, y, z, 0.0D, 0.02D, 0.0D);
        }
    }

    private static void playWorkingSound(Level level, BlockPos pos, BlockState state, int ticksRemaining) {
        if (ticksRemaining % 40 != 0) {
            return;
        }

        Block block = state.getBlock();
        if (block == ModBlocks.CHARCOAL_ROASTER.get() || block == ModBlocks.TEA_ROASTING_PAN.get() ||
                block == ModBlocks.TEA_STOVE.get()) {
            level.playSound(null, pos, SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS, 0.35F, 1.0F);
            return;
        }

        if (block == ModBlocks.FERMENTATION_VAT.get()) {
            level.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.35F, 0.75F);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("ProcessIndex", processIndex);
        tag.putInt("ProcessTicks", processTicks);
        tag.putInt("TotalProcessTicks", totalProcessTicks);
        if (!inputStack.isEmpty()) {
            tag.put("InputStack", inputStack.save(new CompoundTag()));
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        processIndex = tag.getInt("ProcessIndex");
        processTicks = tag.getInt("ProcessTicks");
        totalProcessTicks = tag.getInt("TotalProcessTicks");
        inputStack = tag.contains("InputStack") ? ItemStack.of(tag.getCompound("InputStack")) : ItemStack.EMPTY;
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemHandler.invalidate();
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return itemHandler.cast();
        }
        return super.getCapability(cap, side);
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

    private class Handler implements IItemHandler {

        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            return isReady() ? getResult() : inputStack;
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            int process = findProcess(stack);
            if (!isIdle() || process < 0) {
                return stack;
            }
            ItemStack remainder = stack.copy();
            if (!simulate) {
                startProcess(process, getBlockState().getBlock() instanceof TeaProcessingBlock block ?
                        block.getProcesses().get(process).durationTicks() : 6 * 20, stack);
            }
            remainder.shrink(1);
            return remainder;
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (!isReady() || amount <= 0) {
                return ItemStack.EMPTY;
            }
            ItemStack result = getResult();
            if (result.isEmpty()) {
                return ItemStack.EMPTY;
            }
            result.setCount(Math.min(amount, result.getCount()));
            if (!simulate) {
                clear();
            }
            return result;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return canInsert(stack);
        }
    }
}

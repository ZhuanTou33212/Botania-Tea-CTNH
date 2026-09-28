package com.qianxin.teaart.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;

import com.qianxin.teaart.api.BambooPipeDefaultOutput;
import com.qianxin.teaart.block.BambooPipeBlock;
import com.qianxin.teaart.block.BambooPipeInputPortBlock;
import com.qianxin.teaart.block.BambooPipeOutputPortBlock;
import com.qianxin.teaart.block.BambooPipeRedstonePortBlock;
import com.qianxin.teaart.registry.ModBlockEntities;
import com.qianxin.teaart.registry.ModBlocks;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;

public class BambooPipeBlockEntity extends BlockEntity {

    private static final int TRANSFER_INTERVAL = 5;
    private static final int TRANSFER_AMOUNT = 1;
    private static final int MAX_NETWORK_PIPES = 32;

    private int tickCounter;
    private int nextSourceIndex;
    private int nextTargetIndex;
    private int disabledConnections;

    public BambooPipeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BAMBOO_PIPE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, BambooPipeBlockEntity pipe) {
        if (level.isClientSide || !(state.getBlock() instanceof BambooPipeBlock)) {
            return;
        }

        pipe.tickCounter++;
        if (pipe.tickCounter % TRANSFER_INTERVAL != 0) {
            return;
        }

        Network network = collectNetwork(level, pos, state.getValue(BambooPipeBlock.COLOR));
        if (!network.controller.equals(pos)) {
            return;
        }

        pipe.transfer(network);
    }

    public static ItemStack insertIntoAdjacentPipeNetwork(Level level, BlockPos sourcePos, ItemStack stack) {
        if (level.isClientSide || stack.isEmpty()) {
            return stack;
        }

        for (Direction direction : Direction.values()) {
            BlockPos networkPos = sourcePos.relative(direction);
            BlockState networkState = level.getBlockState(networkPos);
            Optional<BambooPipeBlock.PipeColor> color = networkColorForAdjacentSource(level, sourcePos, networkPos,
                    networkState);
            if (color.isEmpty()) {
                continue;
            }

            Network network = collectNetwork(level, networkPos, color.get());
            ItemStack remainder = insertIntoNetwork(network, sourcePos, stack);
            if (remainder.getCount() != stack.getCount()) {
                return remainder;
            }
        }
        return stack;
    }

    private static Optional<BambooPipeBlock.PipeColor> networkColorForAdjacentSource(Level level, BlockPos sourcePos,
                                                                                     BlockPos networkPos,
                                                                                     BlockState networkState) {
        if (networkState.is(ModBlocks.BAMBOO_PIPE.get()) && networkState.hasProperty(BambooPipeBlock.COLOR)) {
            return Optional.of(networkState.getValue(BambooPipeBlock.COLOR));
        }
        if (!isOutputPort(networkState) && !isInputPort(networkState)) {
            return Optional.empty();
        }
        Direction facing = networkState.getValue(BambooPipeOutputPortBlock.FACING);
        if (!networkPos.relative(facing).equals(sourcePos)) {
            return Optional.empty();
        }
        BlockPos frontPipePos = networkPos.relative(facing.getOpposite());
        BlockState frontPipeState = level.getBlockState(frontPipePos);
        if (frontPipeState.is(ModBlocks.BAMBOO_PIPE.get()) && frontPipeState.hasProperty(BambooPipeBlock.COLOR)) {
            return Optional.of(frontPipeState.getValue(BambooPipeBlock.COLOR));
        }
        return Optional.empty();
    }

    public boolean toggleConnection(Direction direction) {
        int mask = 1 << direction.ordinal();
        disabledConnections ^= mask;
        setChanged();
        return (disabledConnections & mask) == 0;
    }

    public boolean isConnectionDisabled(Direction direction) {
        return (disabledConnections & (1 << direction.ordinal())) != 0;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("DisabledConnections", disabledConnections);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        disabledConnections = tag.getInt("DisabledConnections");
    }

    private void transfer(Network network) {
        if (network.endpoints.size() < 2) {
            return;
        }

        List<Endpoint> outputPortSources = network.endpoints.stream()
                .filter(Endpoint::canOutputPortExtract)
                .toList();
        List<Endpoint> sources = outputPortSources.isEmpty() ?
                network.endpoints.stream().filter(Endpoint::canDefaultExtract).toList() : outputPortSources;
        List<Endpoint> normalTargets = network.endpoints.stream()
                .filter(endpoint -> !endpoint.outputPortSource() && !endpoint.inputPortTarget())
                .toList();
        List<Endpoint> inputPortTargets = network.endpoints.stream()
                .filter(Endpoint::inputPortTarget)
                .toList();
        if (sources.isEmpty() || (normalTargets.isEmpty() && inputPortTargets.isEmpty())) {
            return;
        }

        nextSourceIndex = Math.floorMod(nextSourceIndex, sources.size());

        for (int sourceAttempt = 0; sourceAttempt < sources.size(); sourceAttempt++) {
            Endpoint source = sources.get((nextSourceIndex + sourceAttempt) % sources.size());
            ItemStack extracted = source.extract(TRANSFER_AMOUNT, true);
            if (extracted.isEmpty()) {
                continue;
            }

            if (transferToAnyTarget(source, extracted, normalTargets, sources.size(), sourceAttempt) ||
                    transferToAnyTarget(source, extracted, inputPortTargets, sources.size(), sourceAttempt)) {
                return;
            }
        }
    }

    private boolean transferToAnyTarget(Endpoint source, ItemStack extracted, List<Endpoint> targets,
                                        int sourceCount, int sourceAttempt) {
        if (targets.isEmpty()) {
            return false;
        }

        int startTargetIndex = Math.floorMod(nextTargetIndex, targets.size());
        for (int targetAttempt = 0; targetAttempt < targets.size(); targetAttempt++) {
            Endpoint target = targets.get((startTargetIndex + targetAttempt) % targets.size());
            if (source.sameBlock(target)) {
                continue;
            }

            int accepted = target.acceptedCount(extracted);
            if (accepted <= 0) {
                continue;
            }

            ItemStack moved = source.extract(accepted, false);
            if (moved.isEmpty()) {
                return true;
            }
            ItemStack remainder = target.insert(moved, false);
            if (!remainder.isEmpty()) {
                source.insert(remainder, false);
            }

            nextSourceIndex = (sourceAttempt + nextSourceIndex + 1) % sourceCount;
            nextTargetIndex = (targetAttempt + startTargetIndex + 1) % targets.size();
            setChanged();
            return true;
        }
        return false;
    }

    private static ItemStack insertIntoNetwork(Network network, BlockPos sourcePos, ItemStack stack) {
        ItemStack remainder = stack.copy();
        List<Endpoint> targets = new ArrayList<>(network.endpoints.stream()
                .filter(endpoint -> !endpoint.outputPortSource() && !endpoint.inputPortTarget())
                .toList());
        targets.addAll(network.endpoints.stream()
                .filter(Endpoint::inputPortTarget)
                .toList());
        for (Endpoint target : targets) {
            if (target.pos.equals(sourcePos)) {
                continue;
            }
            remainder = target.insert(remainder, false);
            if (remainder.isEmpty()) {
                return ItemStack.EMPTY;
            }
        }
        return remainder;
    }

    private static Network collectNetwork(Level level, BlockPos start, BambooPipeBlock.PipeColor color) {
        Set<BlockPos> nodes = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);

        while (!queue.isEmpty() && nodes.size() < MAX_NETWORK_PIPES) {
            BlockPos pos = queue.remove();
            if (!nodes.add(pos)) {
                continue;
            }

            for (Direction direction : Direction.values()) {
                BlockPos nextPos = pos.relative(direction);
                if (!nodes.contains(nextPos) && canTraverseNetwork(level, pos, nextPos, direction, color)) {
                    queue.add(nextPos);
                }
            }
        }

        List<Endpoint> endpoints = new ArrayList<>();
        for (BlockPos nodePos : nodes) {
            BlockState nodeState = level.getBlockState(nodePos);
            if (isOutputPort(nodeState)) {
                outputPortEndpoint(level, nodePos).ifPresent(endpoints::add);
                continue;
            }
            if (isInputPort(nodeState)) {
                inputPortEndpoint(level, nodePos).ifPresent(endpoints::add);
                continue;
            }

            for (Direction direction : Direction.values()) {
                BlockPos endpointPos = nodePos.relative(direction);
                BlockState endpointState = level.getBlockState(endpointPos);
                if (nodes.contains(endpointPos) || isNetworkBlock(endpointState, color)) {
                    continue;
                }
                endpoint(level, endpointPos, direction.getOpposite()).ifPresent(endpoints::add);
            }
        }

        BlockPos controller = nodes.stream()
                .filter(nodePos -> isSameChannelPipe(level.getBlockState(nodePos), color))
                .min(Comparator.<BlockPos>comparingInt(BlockPos::getY)
                        .thenComparingInt(BlockPos::getX)
                        .thenComparingInt(BlockPos::getZ))
                .orElse(start);
        return new Network(controller, endpoints);
    }

    private static boolean isSameChannelPipe(BlockState state, BambooPipeBlock.PipeColor color) {
        return state.is(ModBlocks.BAMBOO_PIPE.get()) && state.hasProperty(BambooPipeBlock.COLOR) &&
                state.getValue(BambooPipeBlock.COLOR) == color;
    }

    private static boolean isOutputPort(BlockState state) {
        return state.is(ModBlocks.BAMBOO_PIPE_OUTPUT_PORT.get()) && state.hasProperty(BambooPipeOutputPortBlock.FACING);
    }

    private static boolean isInputPort(BlockState state) {
        return state.is(ModBlocks.BAMBOO_PIPE_INPUT_PORT.get()) && state.hasProperty(BambooPipeInputPortBlock.FACING);
    }

    private static boolean isNetworkBlock(BlockState state, BambooPipeBlock.PipeColor color) {
        return isSameChannelPipe(state, color) || isOutputPort(state) || isInputPort(state) || isRedstonePort(state);
    }

    private static boolean canTraverseNetwork(Level level, BlockPos from, BlockPos to, Direction direction,
                                              BambooPipeBlock.PipeColor color) {
        BlockState fromState = level.getBlockState(from);
        BlockState toState = level.getBlockState(to);
        if (isSameChannelPipe(fromState, color)) {
            if (level.getBlockEntity(from) instanceof BambooPipeBlockEntity pipe &&
                    pipe.isConnectionDisabled(direction)) {
                return false;
            }
            if (isSameChannelPipe(toState, color)) {
                return !(level.getBlockEntity(to) instanceof BambooPipeBlockEntity toPipe) ||
                        !toPipe.isConnectionDisabled(direction.getOpposite());
            }
            if (isOutputPort(toState) || isInputPort(toState)) {
                return toState.getValue(BambooPipeOutputPortBlock.FACING) == direction;
            }
            if (isRedstonePort(toState)) {
                return !BambooPipeRedstonePortBlock.isClosed(toState) &&
                        (toState.getValue(BambooPipeRedstonePortBlock.FACING) == direction ||
                                toState.getValue(BambooPipeRedstonePortBlock.FACING).getOpposite() == direction);
            }
            return false;
        }
        if (isOutputPort(fromState) || isInputPort(fromState)) {
            Direction front = fromState.getValue(BambooPipeOutputPortBlock.FACING).getOpposite();
            if (direction != front) {
                return false;
            }
            if (isSameChannelPipe(toState, color)) {
                return !(level.getBlockEntity(to) instanceof BambooPipeBlockEntity toPipe) ||
                        !toPipe.isConnectionDisabled(direction.getOpposite());
            }
            return isRedstonePort(toState) && !BambooPipeRedstonePortBlock.isClosed(toState) &&
                    toState.getValue(BambooPipeRedstonePortBlock.FACING) ==
                            fromState.getValue(BambooPipeOutputPortBlock.FACING);
        }
        if (isRedstonePort(fromState)) {
            if (BambooPipeRedstonePortBlock.isClosed(fromState)) {
                return false;
            }
            Direction facing = fromState.getValue(BambooPipeRedstonePortBlock.FACING);
            if (direction == facing) {
                if (isSameChannelPipe(toState, color)) {
                    return !(level.getBlockEntity(to) instanceof BambooPipeBlockEntity toPipe) ||
                            !toPipe.isConnectionDisabled(direction.getOpposite());
                }
                return (isOutputPort(toState) || isInputPort(toState)) &&
                        toState.getValue(BambooPipeOutputPortBlock.FACING) == facing;
            }
            if (direction == facing.getOpposite()) {
                return isSameChannelPipe(toState, color) &&
                        (!(level.getBlockEntity(to) instanceof BambooPipeBlockEntity toPipe) ||
                                !toPipe.isConnectionDisabled(direction.getOpposite()));
            }
        }
        return false;
    }

    private static boolean isRedstonePort(BlockState state) {
        return state.is(ModBlocks.BAMBOO_PIPE_REDSTONE_PORT.get()) &&
                state.hasProperty(BambooPipeRedstonePortBlock.FACING);
    }

    private static boolean canTraversePipeConnection(Level level, BlockPos from, BlockPos to, Direction direction) {
        if (level.getBlockEntity(from) instanceof BambooPipeBlockEntity fromPipe &&
                fromPipe.isConnectionDisabled(direction)) {
            return false;
        }
        return !(level.getBlockEntity(to) instanceof BambooPipeBlockEntity toPipe) ||
                !toPipe.isConnectionDisabled(direction.getOpposite());
    }

    private static Optional<Endpoint> endpoint(Level level, BlockPos pos, Direction side) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null) {
            return Optional.empty();
        }
        return blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, side)
                .map(handler -> new Endpoint(pos, side, handler, isDefaultOutputSource(blockEntity), false, false));
    }

    private static Optional<Endpoint> outputPortEndpoint(Level level, BlockPos portPos) {
        BlockState portState = level.getBlockState(portPos);
        if (!portState.is(ModBlocks.BAMBOO_PIPE_OUTPUT_PORT.get()) ||
                !portState.hasProperty(BambooPipeOutputPortBlock.FACING)) {
            return Optional.empty();
        }

        Direction facing = portState.getValue(BambooPipeOutputPortBlock.FACING);
        BlockPos sourcePos = portPos.relative(facing);
        BlockEntity blockEntity = level.getBlockEntity(sourcePos);
        if (blockEntity == null) {
            return Optional.empty();
        }
        Direction sourceSide = facing.getOpposite();
        return blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, sourceSide)
                .map(handler -> new Endpoint(sourcePos, sourceSide,
                        level.getBlockEntity(portPos) instanceof BambooPipeOutputPortBlockEntity port ?
                                new FilteredExtractHandler(handler, port) : handler,
                        true, true, false));
    }

    private static Optional<Endpoint> inputPortEndpoint(Level level, BlockPos portPos) {
        BlockState portState = level.getBlockState(portPos);
        if (!portState.is(ModBlocks.BAMBOO_PIPE_INPUT_PORT.get()) ||
                !portState.hasProperty(BambooPipeInputPortBlock.FACING) ||
                !(level.getBlockEntity(portPos) instanceof BambooPipeInputPortBlockEntity port)) {
            return Optional.empty();
        }

        Direction facing = portState.getValue(BambooPipeInputPortBlock.FACING);
        BlockPos targetPos = portPos.relative(facing);
        BlockEntity blockEntity = level.getBlockEntity(targetPos);
        if (blockEntity == null) {
            return Optional.empty();
        }
        Direction targetSide = facing.getOpposite();
        return blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, targetSide)
                .map(handler -> new Endpoint(targetPos, targetSide, new FilteredInsertHandler(handler, port), false,
                        false, true));
    }

    private static boolean isDefaultOutputSource(BlockEntity blockEntity) {
        return blockEntity instanceof BambooPipeDefaultOutput ||
                blockEntity.getType() == ModBlockEntities.TEA_TABLE.get() ||
                blockEntity.getType() == ModBlockEntities.TEA_PROCESSING.get() ||
                blockEntity.getType() == ModBlockEntities.SCENTING_BOX.get() ||
                blockEntity.getType() == ModBlockEntities.TEA_SIEVE.get() ||
                blockEntity.getType() == ModBlockEntities.TEA_SIEVE_RACK.get() ||
                blockEntity.getType() == ModBlockEntities.FERMENTATION_VAT.get() ||
                blockEntity.getType() == ModBlockEntities.PILE_FERMENTATION_BASIN.get() ||
                blockEntity.getType() == ModBlockEntities.TEA_STOVE.get();
    }

    private record Network(BlockPos controller, List<Endpoint> endpoints) {}

    private record Endpoint(BlockPos pos, Direction side, IItemHandler handler, boolean defaultOutputSource,
                            boolean outputPortSource, boolean inputPortTarget) {

        boolean sameBlock(Endpoint other) {
            return pos.equals(other.pos);
        }

        boolean canDefaultExtract() {
            return defaultOutputSource && !extract(TRANSFER_AMOUNT, true).isEmpty();
        }

        boolean canOutputPortExtract() {
            return outputPortSource && !extract(TRANSFER_AMOUNT, true).isEmpty();
        }

        ItemStack extract(int amount, boolean simulate) {
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                ItemStack extracted = handler.extractItem(slot, amount, simulate);
                if (!extracted.isEmpty()) {
                    return extracted;
                }
            }
            return ItemStack.EMPTY;
        }

        ItemStack insert(ItemStack stack, boolean simulate) {
            ItemStack remainder = stack.copy();
            for (int slot = 0; slot < handler.getSlots() && !remainder.isEmpty(); slot++) {
                remainder = handler.insertItem(slot, remainder, simulate);
            }
            return remainder;
        }

        int acceptedCount(ItemStack stack) {
            ItemStack offered = stack.copy();
            ItemStack remainder = insert(offered, true);
            return offered.getCount() - remainder.getCount();
        }
    }

    private record FilteredInsertHandler(IItemHandler delegate, BambooPipeInputPortBlockEntity port)
            implements IItemHandler {

        @Override
        public int getSlots() {
            return delegate.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return delegate.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return port.allows(stack) ? delegate.insertItem(slot, stack, simulate) : stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return delegate.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return port.allows(stack) && delegate.isItemValid(slot, stack);
        }
    }

    private record FilteredExtractHandler(IItemHandler delegate, BambooPipeOutputPortBlockEntity port)
            implements IItemHandler {

        @Override
        public int getSlots() {
            return delegate.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return delegate.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return delegate.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            ItemStack extracted = delegate.extractItem(slot, amount, true);
            if (extracted.isEmpty() || !port.allows(extracted)) {
                return ItemStack.EMPTY;
            }
            return delegate.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return delegate.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return delegate.isItemValid(slot, stack);
        }
    }
}

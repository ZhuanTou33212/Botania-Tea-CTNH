package com.qianxin.teaart.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.qianxin.teaart.block.entity.TeaProcessingBlockEntity;
import com.qianxin.teaart.item.TeaProcessingData;
import com.qianxin.teaart.registry.ModBlockEntities;
import com.qianxin.teaart.registry.ModBlocks;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

public class TeaProcessingBlock extends HorizontalDirectionalBlock implements EntityBlock {

    private static final VoxelShape WITHERING_TRAY_SHAPE = Shapes.or(
            Block.box(1.0D, 3.0D, 1.0D, 15.0D, 4.0D, 15.0D),
            Block.box(1.0D, 4.0D, 1.0D, 15.0D, 6.0D, 3.0D),
            Block.box(1.0D, 4.0D, 13.0D, 15.0D, 6.0D, 15.0D),
            Block.box(1.0D, 4.0D, 3.0D, 3.0D, 6.0D, 13.0D),
            Block.box(13.0D, 4.0D, 3.0D, 15.0D, 6.0D, 13.0D),
            Block.box(2.0D, 0.0D, 2.0D, 4.0D, 3.0D, 4.0D),
            Block.box(12.0D, 0.0D, 2.0D, 14.0D, 3.0D, 4.0D),
            Block.box(2.0D, 0.0D, 12.0D, 4.0D, 3.0D, 14.0D),
            Block.box(12.0D, 0.0D, 12.0D, 14.0D, 3.0D, 14.0D));
    private static final VoxelShape CHARCOAL_ROASTER_SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 14.0D, 16.0D);

    private final Supplier<List<TeaProcess>> processes;

    public TeaProcessingBlock(Properties properties, Supplier<List<TeaProcess>> processes) {
        super(properties);
        this.processes = processes;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeFor(state);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeFor(state);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TeaProcessingBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> blockEntityType) {
        return blockEntityType == ModBlockEntities.TEA_PROCESSING.get() ?
                (tickingLevel, pos, tickingState, blockEntity) -> TeaProcessingBlockEntity.tick(tickingLevel, pos,
                        tickingState, (TeaProcessingBlockEntity) blockEntity) :
                null;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                 BlockHitResult hit) {
        ItemStack heldStack = player.getItemInHand(hand);
        List<TeaProcess> teaProcesses = processes.get();

        if (level.isClientSide) {
            if (level.getBlockEntity(pos) instanceof TeaProcessingBlockEntity processing &&
                    (processing.isProcessing() || processing.isReady())) {
                return InteractionResult.SUCCESS;
            }
            for (TeaProcess process : teaProcesses) {
                if (process.canApply(heldStack)) {
                    return InteractionResult.SUCCESS;
                }
            }
            return InteractionResult.PASS;
        }

        if (!(level.getBlockEntity(pos) instanceof TeaProcessingBlockEntity processing)) {
            return InteractionResult.PASS;
        }

        if (processing.isReady()) {
            int processIndex = processing.getProcessIndex();
            if (processIndex < 0 || processIndex >= teaProcesses.size()) {
                processing.clear();
                player.displayClientMessage(Component.translatable("message.tea_art.processing_failed"), true);
                return InteractionResult.SUCCESS;
            }

            TeaProcess process = teaProcesses.get(processIndex);
            ItemStack result = process.apply(processing.getInputStack());
            if (!player.getInventory().add(result)) {
                player.drop(result, false);
            }
            processing.clear();
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.8F, 1.1F);
            player.displayClientMessage(Component.translatable("message.tea_art.processed", result.getHoverName()),
                    true);
            return InteractionResult.SUCCESS;
        }

        if (processing.isProcessing()) {
            player.displayClientMessage(
                    Component.translatable("message.tea_art.processing_busy", processing.getSecondsRemaining()),
                    true);
            return InteractionResult.SUCCESS;
        }

        for (int i = 0; i < teaProcesses.size(); i++) {
            TeaProcess process = teaProcesses.get(i);
            if (process.canApply(heldStack)) {
                if (process.requiresTool() &&
                        !player.getInventory().contains(new ItemStack(process.tool().get().asItem()))) {
                    Component toolName = Component.translatable(process.tool().get().asItem().getDescriptionId());
                    player.displayClientMessage(Component.translatable("message.tea_art.need_tool", toolName), true);
                    return InteractionResult.SUCCESS;
                }

                ItemStack processInput = heldStack.copy();
                processInput.setCount(1);
                if (!player.getAbilities().instabuild) {
                    heldStack.shrink(1);
                }

                processing.startProcess(i, process.durationTicks(), processInput);
                level.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.8F, 0.9F);
                player.displayClientMessage(
                        Component.translatable("message.tea_art.processing_started",
                                Component.translatable(process.translationKey()),
                                process.durationTicks() / 20),
                        true);
                return InteractionResult.SUCCESS;
            }
        }

        return InteractionResult.PASS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.is(ModBlocks.CHARCOAL_ROASTER.get())) {
            return;
        }
        if (random.nextFloat() < 0.45F) {
            level.addParticle(ParticleTypes.FLAME,
                    pos.getX() + 0.5D + (random.nextDouble() - 0.5D) * 0.25D,
                    pos.getY() + 0.42D,
                    pos.getZ() + 0.12D,
                    0.0D, 0.01D, 0.0D);
        }
        if (random.nextFloat() < 0.25F) {
            level.addParticle(ParticleTypes.SMOKE,
                    pos.getX() + 0.5D,
                    pos.getY() + 0.9D,
                    pos.getZ() + 0.5D,
                    0.0D, 0.025D, 0.0D);
        }
    }

    private static VoxelShape shapeFor(BlockState state) {
        if (state.is(ModBlocks.WITHERING_TRAY.get())) {
            return WITHERING_TRAY_SHAPE;
        }
        if (state.is(ModBlocks.CHARCOAL_ROASTER.get())) {
            return CHARCOAL_ROASTER_SHAPE;
        }
        return Shapes.block();
    }

    public List<TeaProcess> getProcesses() {
        return processes.get();
    }

    public record TeaProcess(String process, Supplier<? extends ItemLike> input, Supplier<? extends ItemLike> output,
                             Supplier<? extends ItemLike> tool, int durationTicks) {

        private static final int DEFAULT_DURATION_TICKS = 6 * 20;

        public TeaProcess(String process, Supplier<? extends ItemLike> tool) {
            this(process, null, null, tool, DEFAULT_DURATION_TICKS);
        }

        public TeaProcess(String process) {
            this(process, null, null, null, DEFAULT_DURATION_TICKS);
        }

        public TeaProcess(String process, Supplier<? extends ItemLike> tool, int durationTicks) {
            this(process, null, null, tool, durationTicks);
        }

        public TeaProcess(String process, int durationTicks) {
            this(process, null, null, null, durationTicks);
        }

        public TeaProcess(Supplier<? extends ItemLike> input, Supplier<? extends ItemLike> output,
                          Supplier<? extends ItemLike> tool, int durationTicks) {
            this(null, input, output, tool, durationTicks);
        }

        public TeaProcess(Supplier<? extends ItemLike> input, Supplier<? extends ItemLike> output) {
            this(null, input, output, null, DEFAULT_DURATION_TICKS);
        }

        public boolean requiresTool() {
            return tool != null;
        }

        public boolean canApply(ItemStack stack) {
            if (input != null) {
                return stack.is(input.get().asItem());
            }
            return TeaProcessingData.canApply(stack, process);
        }

        public ItemStack apply(ItemStack stack) {
            if (output != null) {
                return new ItemStack(output.get());
            }
            return TeaProcessingData.apply(stack, process);
        }

        public String translationKey() {
            if (output != null) {
                return output.get().asItem().getDescriptionId();
            }
            return "process.tea_art." + process;
        }
    }
}

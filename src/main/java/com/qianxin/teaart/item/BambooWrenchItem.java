package com.qianxin.teaart.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import com.qianxin.teaart.block.BambooPipeBlock;
import com.qianxin.teaart.block.entity.BambooPipeBlockEntity;
import com.qianxin.teaart.registry.ModBlocks;

public class BambooWrenchItem extends Item {

    public BambooWrenchItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);

        if (context.getPlayer() != null && context.getPlayer().isShiftKeyDown()) {
            if (!level.isClientSide && canWrenchBreak(state)) {
                level.destroyBlock(pos, true, context.getPlayer());
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (state.getBlock() instanceof BambooPipeBlock &&
                level.getBlockEntity(pos) instanceof BambooPipeBlockEntity pipe) {
            if (!level.isClientSide) {
                Direction direction = context.getClickedFace();
                pipe.toggleConnection(direction);
                BlockState updatedState = ((BambooPipeBlock) state.getBlock()).updateConnections(state, level, pos);
                level.setBlock(pos, updatedState, Block.UPDATE_ALL);
                level.updateNeighborsAt(pos.relative(direction), state.getBlock());
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            if (!level.isClientSide) {
                level.setBlock(pos, state.rotate(Rotation.CLOCKWISE_90), Block.UPDATE_ALL);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        return InteractionResult.PASS;
    }

    private static boolean canWrenchBreak(BlockState state) {
        return state.getBlock() instanceof BambooPipeBlock || state.is(ModBlocks.BAMBOO_PIPE_OUTPUT_PORT.get()) ||
                state.is(ModBlocks.BAMBOO_PIPE_INPUT_PORT.get()) ||
                state.hasProperty(BlockStateProperties.HORIZONTAL_FACING);
    }
}

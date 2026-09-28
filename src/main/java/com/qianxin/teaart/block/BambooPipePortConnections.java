package com.qianxin.teaart.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

import com.qianxin.teaart.block.entity.BambooPipeBlockEntity;

final class BambooPipePortConnections {

    private BambooPipePortConnections() {}

    static BlockState align(BlockState state, LevelAccessor level, BlockPos pos) {
        Direction best = state.getValue(DirectionalBlock.FACING);
        int bestScore = score(level, pos, best);
        for (Direction direction : Direction.values()) {
            int score = score(level, pos, direction);
            // Keep the current orientation when multiple connections are equally suitable.
            if (score > bestScore) {
                best = direction;
                bestScore = score;
            }
        }
        return state.setValue(DirectionalBlock.FACING, best);
    }

    private static int score(LevelAccessor level, BlockPos pos, Direction facing) {
        BlockEntity target = level.getBlockEntity(pos.relative(facing));
        boolean inventory = target != null &&
                target.getCapability(ForgeCapabilities.ITEM_HANDLER, facing.getOpposite()).isPresent();
        BlockPos pipePos = pos.relative(facing.getOpposite());
        BlockState pipeState = level.getBlockState(pipePos);
        boolean pipe = pipeState.getBlock() instanceof BambooPipeBlock &&
                (!(level.getBlockEntity(pipePos) instanceof BambooPipeBlockEntity entity) ||
                        !entity.isConnectionDisabled(facing));
        boolean valve = pipeState.getBlock() instanceof BambooPipeRedstonePortBlock &&
                pipeState.getValue(DirectionalBlock.FACING) == facing;
        return (inventory ? 2 : 0) + (pipe || valve ? 1 : 0);
    }
}

package com.qianxin.teaart.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.qianxin.teaart.registry.ModItems;

public class TeaCropBlock extends CropBlock {

    private static final VoxelShape[] GROWTH_SHAPES = new VoxelShape[8];
    static {
        for (int age = 0; age < GROWTH_SHAPES.length; age++) {
            double inset = Math.max(1, 6 - age);
            GROWTH_SHAPES[age] = Block.box(inset, 0, inset, 16 - inset, 2 + age * 2, 16 - inset);
        }
    }

    public TeaCropBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return super.mayPlaceOn(state, level, pos) || state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) ||
                state.is(Blocks.COARSE_DIRT) || state.is(Blocks.ROOTED_DIRT) || state.is(Blocks.PODZOL) ||
                state.is(Blocks.MOSS_BLOCK);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return GROWTH_SHAPES[getAge(state)];
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return ModItems.TEA_SEED.get();
    }
}

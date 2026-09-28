package com.qianxin.teaart.block;

import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

import com.qianxin.teaart.registry.ModItems;

public class TeaPlantBlock extends CropBlock {

    /** 茶苗只有 0~6 共 7 个阶段（原版 CropBlock 固定 AGE_7，这里换成 0~6）。 */
    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, 6);

    public TeaPlantBlock(Properties properties) {
        super(properties);
    }

    @Override
    public IntegerProperty getAgeProperty() {
        return AGE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    /** 长到第 6 阶段即成熟。 */
    @Override
    public int getMaxAge() {
        return 6;
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return ModItems.TEA_SEED.get();
    }
}

package com.qianxin.teaart.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 茶块的连接边框模型。
 *
 * <p>
 * 每个面用 8 位上下文（4 个正方向邻居 + 4 个斜角邻居）算出一个索引，
 * 再把该面所有顶点的 UV 位移到 128x128 图集里对应的 16x16 格子上，
 * 于是"外沿画边框、内部无缝、凹角补点"全部由贴图本身完成，不需要任何 blockstate 属性。
 *
 * <p>
 * 实现思路与索引公式参考 Create 的
 * {@code ConnectedTextureBehaviour} / {@code CTModel} / {@code AllCTTypes.OMNIDIRECTIONAL}。
 */
public class TeaBlockCTModel extends BakedModelWrapper<BakedModel> {

    /** 图集是 8x8 个 16x16 格子。 */
    private static final int SHEET_SIZE = 8;
    /** DefaultVertexFormat.BLOCK 每个顶点 8 个 int，UV 在第 4、5 个。 */
    private static final int VERTEX_STRIDE = 8;
    private static final int U_OFFSET = 4;
    private static final int V_OFFSET = 5;

    private static final ModelProperty<int[]> FACE_INDICES = new ModelProperty<>();

    /** 这一份茶块用的面板贴图与图集贴图（每种茶块各一份）。 */
    private final ResourceLocation panelTexture;
    private final ResourceLocation atlasTexture;

    private TextureAtlasSprite panelSprite;
    private TextureAtlasSprite atlasSprite;
    private boolean spritesResolved;

    public TeaBlockCTModel(BakedModel originalModel, ResourceLocation panelTexture, ResourceLocation atlasTexture) {
        super(originalModel);
        this.panelTexture = panelTexture;
        this.atlasTexture = atlasTexture;
    }

    /** 每个方块位置算一次：6 个面各自的图集索引。被遮住的面不用算。 */
    @Override
    public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData modelData) {
        int[] indices = new int[6];
        Arrays.fill(indices, -1);
        BlockPos.MutableBlockPos neighborPos = new BlockPos.MutableBlockPos();
        for (Direction face : Direction.values()) {
            if (!Block.shouldRenderFace(state, level, pos, face, neighborPos.setWithOffset(pos, face))) {
                continue;
            }
            indices[face.get3DDataValue()] = tileIndex(level, pos, state, face);
        }
        return ModelData.builder().with(FACE_INDICES, indices).build();
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource rand, ModelData modelData,
                                    RenderType renderType) {
        List<BakedQuad> quads = super.getQuads(state, side, rand, modelData, renderType);
        if (side == null || !modelData.has(FACE_INDICES)) {
            return quads;
        }
        int index = modelData.get(FACE_INDICES)[side.get3DDataValue()];
        if (index < 0 || !resolveSprites()) {
            return quads;
        }

        int column = index % SHEET_SIZE;
        int row = index / SHEET_SIZE;
        List<BakedQuad> result = new ArrayList<>(quads.size());
        for (BakedQuad quad : quads) {
            if (quad.getSprite() != panelSprite) {
                result.add(quad);
                continue;
            }
            int[] vertexData = Arrays.copyOf(quad.getVertices(), quad.getVertices().length);
            for (int vertex = 0; vertex < 4; vertex++) {
                int uIndex = vertex * VERTEX_STRIDE + U_OFFSET;
                int vIndex = vertex * VERTEX_STRIDE + V_OFFSET;
                float localU = toLocal(Float.intBitsToFloat(vertexData[uIndex]), panelSprite.getU0(),
                        panelSprite.getU1());
                float localV = toLocal(Float.intBitsToFloat(vertexData[vIndex]), panelSprite.getV0(),
                        panelSprite.getV1());
                vertexData[uIndex] = Float.floatToRawIntBits(
                        toAtlas((localU + column) / SHEET_SIZE, atlasSprite.getU0(), atlasSprite.getU1()));
                vertexData[vIndex] = Float.floatToRawIntBits(
                        toAtlas((localV + row) / SHEET_SIZE, atlasSprite.getV0(), atlasSprite.getV1()));
            }
            result.add(new BakedQuad(vertexData, quad.getTintIndex(), quad.getDirection(), quad.getSprite(),
                    quad.isShade()));
        }
        return result;
    }

    private static float toLocal(float atlasCoord, float min, float max) {
        return (atlasCoord - min) / (max - min);
    }

    private static float toAtlas(float local, float min, float max) {
        return min + (max - min) * local;
    }

    private boolean resolveSprites() {
        if (spritesResolved) {
            return panelSprite != null && atlasSprite != null;
        }
        spritesResolved = true;
        if (Minecraft.getInstance() == null) {
            return false;
        }
        var atlas = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS);
        panelSprite = atlas.apply(panelTexture);
        atlasSprite = atlas.apply(atlasTexture);
        return true;
    }

    // ------------------------------------------------------------------
    // 上下文 -> 图集索引
    // ------------------------------------------------------------------

    private static int tileIndex(BlockAndTintGetter level, BlockPos pos, BlockState state, Direction face) {
        Direction h = face.getAxis() == Direction.Axis.X ? Direction.SOUTH : Direction.WEST;
        Direction v = face.getAxis().isHorizontal() ? Direction.UP : Direction.NORTH;
        h = face.getAxisDirection() == Direction.AxisDirection.POSITIVE ? h.getOpposite() : h;
        if (face == Direction.DOWN) {
            v = v.getOpposite();
            h = h.getOpposite();
        }

        boolean up = connects(level, pos, state, v);
        boolean down = connects(level, pos, state, v.getOpposite());
        boolean left = connects(level, pos, state, h.getOpposite());
        boolean right = connects(level, pos, state, h);
        boolean topLeft = up && left && connects(level, pos, state, h.getOpposite(), v);
        boolean topRight = up && right && connects(level, pos, state, h, v);
        boolean bottomLeft = down && left && connects(level, pos, state, h.getOpposite(), v.getOpposite());
        boolean bottomRight = down && right && connects(level, pos, state, h, v.getOpposite());
        return tileIndex(up, down, left, right, topLeft, topRight, bottomLeft, bottomRight);
    }

    private static boolean connects(BlockAndTintGetter level, BlockPos pos, BlockState state, Direction dir) {
        return level.getBlockState(pos.relative(dir)).is(state.getBlock());
    }

    private static boolean connects(BlockAndTintGetter level, BlockPos pos, BlockState state, Direction first,
                                    Direction second) {
        return level.getBlockState(pos.relative(first).relative(second)).is(state.getBlock());
    }

    /**
     * 索引公式与 Create 的 AllCTTypes.OMNIDIRECTIONAL 完全一致，
     * 保证与脚本生成的 xxx_tea_block_connected.png 图集布局一一对应。
     *
     * @param up/down/left/right 该方向邻居是否相连
     * @param topLeft            等 4 个斜角是否相连（仅在两正交边都相连时才有意义）
     */
    private static int tileIndex(boolean up, boolean down, boolean left, boolean right,
                                 boolean topLeft, boolean topRight, boolean bottomLeft, boolean bottomRight) {
        int tileX = 0;
        int tileY = 0;
        int borders = (!up ? 1 : 0) + (!down ? 1 : 0) + (!left ? 1 : 0) + (!right ? 1 : 0);
        if (up) {
            tileX++;
        }
        if (down) {
            tileX += 2;
        }
        if (left) {
            tileY++;
        }
        if (right) {
            tileY += 2;
        }

        if (borders == 0) {
            if (topRight) {
                tileX++;
            }
            if (topLeft) {
                tileX += 2;
            }
            if (bottomRight) {
                tileY += 2;
            }
            if (bottomLeft) {
                tileY++;
            }
        }

        if (borders == 1) {
            if (!right && (topLeft || bottomLeft)) {
                tileY = 4;
                tileX = -1 + (bottomLeft ? 1 : 0) + (topLeft ? 1 : 0) * 2;
            }
            if (!left && (topRight || bottomRight)) {
                tileY = 5;
                tileX = -1 + (bottomRight ? 1 : 0) + (topRight ? 1 : 0) * 2;
            }
            if (!down && (topLeft || topRight)) {
                tileY = 6;
                tileX = -1 + (topLeft ? 1 : 0) + (topRight ? 1 : 0) * 2;
            }
            if (!up && (bottomLeft || bottomRight)) {
                tileY = 7;
                tileX = -1 + (bottomLeft ? 1 : 0) + (bottomRight ? 1 : 0) * 2;
            }
        }

        if (borders == 2 && ((up && left && topLeft) || (down && left && bottomLeft) || (up && right && topRight) ||
                (down && right && bottomRight))) {
            tileX += 3;
        }
        return tileX + SHEET_SIZE * tileY;
    }
}

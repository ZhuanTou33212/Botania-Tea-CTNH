package com.qianxin.botaniatea.content;

import com.qianxin.botaniatea.registry.ModBlocks;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

/** Shared identities for registered flowers and their server-side behaviour dispatch. */
public enum TeaFlower {
    HETEROMORPHIC_CAMELLIA("heteromorphic_camellia", false),
    WATER_SUPPLY_LILY("water_supply_lily", false),
    CLEANSING_ANGELICA("cleansing_angelica", false),
    CHUNK_DANDELION("chunk_dandelion", false),
    GUARDIAN_LOTUS("guardian_lotus", false),
    BUILDING_BEGONIA("building_begonia", false),
    HOMING_MORNING_GLORY("homing_morning_glory", false),
    GAUGING_HYDRANGEA("gauging_hydrangea", false),
    MENDING_HIBISCUS("mending_hibiscus", false),
    NIGHT_EPIPHYLLUM("night_epiphyllum", false),
    AFTERTASTE_DAISY("aftertaste_daisy", true),
    SIX_HARMONY_CHRYSANTHEMUM("six_harmony_chrysanthemum", true),
    PROJECTILE_BELLFLOWER("projectile_bellflower", true),
    BUTTERFLY_ORCHID("butterfly_orchid", true);

    public final String id;
    public final boolean generating;

    TeaFlower(String id, boolean generating) {
        this.id = id;
        this.generating = generating;
    }

    /** The Tea Garden has petite variants only for flowers with a meaningful work area. */
    public boolean supportsChibi() {
        return switch (this) {
            case HETEROMORPHIC_CAMELLIA, WATER_SUPPLY_LILY, CLEANSING_ANGELICA,
                    GUARDIAN_LOTUS, HOMING_MORNING_GLORY, MENDING_HIBISCUS,
                    NIGHT_EPIPHYLLUM -> true;
            default -> false;
        };
    }

    public int effectiveRadius(int normalRadius, boolean chibi) {
        return chibi && supportsChibi() ? Math.max(1, normalRadius / 2) : normalRadius;
    }

    @Nullable
    public static TeaFlower fromBlock(Block block) {
        for (TeaFlower flower : values()) {
            if (ModBlocks.isFlowerBlock(flower, block)) {
                return flower;
            }
        }
        return null;
    }

    public static boolean isChibi(Block block) {
        for (TeaFlower flower : values()) {
            if (flower.supportsChibi() && ModBlocks.CHIBI_FLOWERS.containsKey(flower)
                    && ModBlocks.CHIBI_FLOWERS.get(flower).get() == block) return true;
            if (flower.supportsChibi() && ModBlocks.FLOATING_CHIBI_FLOWERS.containsKey(flower)
                    && ModBlocks.FLOATING_CHIBI_FLOWERS.get(flower).get() == block) return true;
        }
        return false;
    }

    public static boolean isFloating(Block block) {
        for (TeaFlower flower : values()) {
            if (ModBlocks.FLOATING_FLOWERS.containsKey(flower)
                    && ModBlocks.FLOATING_FLOWERS.get(flower).get() == block) return true;
            if (flower.supportsChibi() && ModBlocks.FLOATING_CHIBI_FLOWERS.containsKey(flower)
                    && ModBlocks.FLOATING_CHIBI_FLOWERS.get(flower).get() == block) return true;
        }
        return false;
    }
}

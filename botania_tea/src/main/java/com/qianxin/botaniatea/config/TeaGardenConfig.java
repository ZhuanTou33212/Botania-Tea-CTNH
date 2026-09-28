package com.qianxin.botaniatea.config;

import net.minecraftforge.common.ForgeConfigSpec;

/** Server-authoritative first-pass tuning for the tea garden. Suggested values remain configurable. */
public final class TeaGardenConfig {
    public static final ForgeConfigSpec.IntValue FUNCTIONAL_MANA_CAPACITY;
    public static final ForgeConfigSpec.IntValue GENERATING_MANA_CAPACITY;
    public static final ForgeConfigSpec.IntValue WORK_PERIOD;
    public static final ForgeConfigSpec.IntValue SEALED_WORK_RATE;
    public static final ForgeConfigSpec.IntValue WORK_RADIUS;
    public static final ForgeConfigSpec.IntValue WORK_HEIGHT;
    public static final ForgeConfigSpec.IntValue SUPPLY_PERIOD, SUPPLY_AMOUNT, WATER_COST, FLUID_COST, CONVERSION_COST;
    public static final ForgeConfigSpec.IntValue BUILD_COST, TELEPORT_COST, MEASURE_COST, MEND_COST;
    public static final ForgeConfigSpec.IntValue FAN_COST, FAN_ADVANCE, FAN_COOLDOWN, FAN_RANGE, GUARD_COST;
    public static final ForgeConfigSpec.IntValue MEASURE_PERIOD, COMMON_PERIOD, DOMAIN_COST, CHUNK_COST;
    public static final ForgeConfigSpec.IntValue ANGELICA_BASE_COST, ANGELICA_EFFECT_COST, INSTANT_COST, INSTANT_PERIOD;
    public static final ForgeConfigSpec.IntValue BUTTERFLY_TARGETS, BUTTERFLY_VISIT, BUTTERFLY_COST, BUTTERFLY_REWARD;
    public static final ForgeConfigSpec.DoubleValue BUTTERFLY_DIVERSITY, BOSS_HEALTH, BOSS_ARMOR, BOSS_DAMAGE;
    public static final ForgeConfigSpec SERVER_SPEC;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("tea_garden");
        FUNCTIONAL_MANA_CAPACITY = builder.comment("Suggested first-pass mana cache for functional flowers.")
                .defineInRange("functional_mana_capacity", 100000, 0, 10000000);
        GENERATING_MANA_CAPACITY = builder.comment("Suggested first-pass mana cache for generating flowers.")
                .defineInRange("generating_mana_capacity", 200000, 0, 10000000);
        WORK_PERIOD = builder.comment("Base work period in server ticks.")
                .defineInRange("work_period", 200, 1, 12000);
        SEALED_WORK_RATE = builder.comment("Work-credit units gained per tick by a sealed flower.")
                .defineInRange("sealed_work_rate", 11, 1, 64);
        WORK_RADIUS = builder.comment("Suggested default horizontal work radius.")
                .defineInRange("work_radius", 4, 0, 32);
        WORK_HEIGHT = builder.comment("Suggested default vertical work height.")
                .defineInRange("work_height", 5, 1, 32);
        SUPPLY_PERIOD = builder.defineInRange("supply_period", 20, 1, 12000);
        SUPPLY_AMOUNT = builder.defineInRange("supply_amount", 100, 1, 10000);
        WATER_COST = builder.comment("Legacy compatibility key; water supply is now free.")
                .defineInRange("water_mana_per_mb", 1, 1, 10000);
        FLUID_COST = builder.comment("Legacy compatibility key; selected-fluid supply is now free.")
                .defineInRange("fluid_mana_per_mb", 8, 1, 10000);
        CONVERSION_COST = builder.defineInRange("conversion_mana", 5000, 1, 10000000);
        BUILD_COST = builder.defineInRange("building_mana", 20, 1, 1000000);
        TELEPORT_COST = builder.defineInRange("teleport_mana", 10, 1, 1000000);
        MEASURE_COST = builder.comment("Legacy compatibility key; gauging is now free.")
                .defineInRange("measure_mana", 5, 1, 1000000);
        MEND_COST = builder.defineInRange("mending_mana", 50, 1, 1000000);
        FAN_COST = builder.defineInRange("fan_mana", 100, 1, 1000000);
        FAN_ADVANCE = builder.defineInRange("fan_advance_ticks", 20, 1, 12000);
        FAN_COOLDOWN = builder.defineInRange("fan_cooldown_ticks", 20, 1, 12000);
        FAN_RANGE = builder.defineInRange("fan_range", 8, 1, 32);
        GUARD_COST = builder.defineInRange("guard_mana_per_damage", 20, 1, 1000000);
        MEASURE_PERIOD = builder.defineInRange("measure_period", 10, 1, 12000);
        COMMON_PERIOD = builder.defineInRange("common_flower_period", 20, 1, 12000);
        DOMAIN_COST = builder.comment("Legacy compatibility key; guardian and night domains are now free.")
                .defineInRange("domain_mana_per_cycle", 40, 1, 1000000);
        CHUNK_COST = builder.defineInRange("chunk_mana_per_lease", 100, 1, 1000000);
        ANGELICA_BASE_COST = builder.defineInRange("angelica_base_mana", 10, 1, 1000000);
        ANGELICA_EFFECT_COST = builder.defineInRange("angelica_mana_per_player_effect", 5, 1, 1000000);
        INSTANT_COST = builder.defineInRange("angelica_instant_mana", 100, 1, 1000000);
        INSTANT_PERIOD = builder.defineInRange("angelica_instant_period", 100, 1, 12000);
        builder.push("butterfly_suggested");
        BUTTERFLY_TARGETS = builder.defineInRange("maximum_targets",16,1,64);
        BUTTERFLY_VISIT = builder.defineInRange("visit_ticks",40,1,12000);
        BUTTERFLY_COST = builder.comment("Legacy compatibility key; new butterfly cycles require only redstone and tea blocks.")
                .defineInRange("mana_per_target",300,1,100000);
        BUTTERFLY_REWARD = builder.defineInRange("base_reward",3750,1,20000);
        BUTTERFLY_DIVERSITY = builder.defineInRange("extra_type_bonus",.25D,0D,1D);
        builder.pop();
        builder.push("boss_suggested");
        BOSS_HEALTH = builder.defineInRange("health",600D,1D,100000D);
        BOSS_ARMOR = builder.defineInRange("armor",8D,0D,30D);
        BOSS_DAMAGE = builder.defineInRange("base_damage",10D,0D,1000D);
        builder.pop();
        builder.pop();
        SERVER_SPEC = builder.build();
    }

    private TeaGardenConfig() {}
}

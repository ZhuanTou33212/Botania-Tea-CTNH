package com.qianxin.botaniatea.registry;

import com.qianxin.botaniatea.BotaniaTeaMod;
import com.qianxin.botaniatea.entity.WaterSparkEntity;
import com.qianxin.botaniatea.entity.TeaMountainSpiritEntity;
import com.qianxin.botaniatea.entity.BambooGrapplingHookEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, BotaniaTeaMod.MOD_ID);
    public static final RegistryObject<EntityType<com.qianxin.botaniatea.entity.TeaBladeEntity>> TEA_BLADE = ENTITY_TYPES.register("tea_blade",
            () -> EntityType.Builder.<com.qianxin.botaniatea.entity.TeaBladeEntity>of(com.qianxin.botaniatea.entity.TeaBladeEntity::new, MobCategory.MISC)
                    .sized(.25F,.25F).clientTrackingRange(48).updateInterval(1).build("botania_tea:tea_blade"));
    public static final RegistryObject<EntityType<WaterSparkEntity>> WATER_SPARK = ENTITY_TYPES.register("water_spark",
            () -> EntityType.Builder.<WaterSparkEntity>of(WaterSparkEntity::new, MobCategory.MISC)
                    .sized(0.3F, 0.3F).clientTrackingRange(10).updateInterval(10)
                    .build(BotaniaTeaMod.MOD_ID + ":water_spark"));
    public static final RegistryObject<EntityType<TeaMountainSpiritEntity>> TEA_MOUNTAIN_SPIRIT = ENTITY_TYPES.register(
            "tea_mountain_spirit", () -> EntityType.Builder.<TeaMountainSpiritEntity>of(
                            TeaMountainSpiritEntity::new, MobCategory.MONSTER)
                    .sized(0.9F, 2.6F).clientTrackingRange(64).updateInterval(2)
                    .build(BotaniaTeaMod.MOD_ID + ":tea_mountain_spirit"));
    public static final RegistryObject<EntityType<BambooGrapplingHookEntity>> BAMBOO_GRAPPLING_HOOK = ENTITY_TYPES.register(
                    "bamboo_grappling_hook", () -> EntityType.Builder.<BambooGrapplingHookEntity>of(
                            BambooGrapplingHookEntity::new, MobCategory.MISC)
                    .sized(0.2F, 0.2F).clientTrackingRange(64).updateInterval(1)
                    .build(BotaniaTeaMod.MOD_ID + ":bamboo_grappling_hook"));
    public static final RegistryObject<EntityType<com.qianxin.botaniatea.entity.ShadowPortalEntity>> SHADOW_PORTAL = ENTITY_TYPES.register(
            "shadow_portal", () -> EntityType.Builder.<com.qianxin.botaniatea.entity.ShadowPortalEntity>of(
                            com.qianxin.botaniatea.entity.ShadowPortalEntity::new, MobCategory.MISC)
                    .sized(1.25F, 2.25F).clientTrackingRange(64).updateInterval(1).noSave()
                    .build(BotaniaTeaMod.MOD_ID + ":shadow_portal"));

    public static void register(IEventBus bus) {
        ENTITY_TYPES.register(bus);
    }

    private ModEntities() {}
}

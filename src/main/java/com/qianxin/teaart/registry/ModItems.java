package com.qianxin.teaart.registry;

import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.Foods;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import com.qianxin.teaart.TeaArtMod;
import com.qianxin.teaart.content.TeaLiquor;
import com.qianxin.teaart.item.BambooWrenchItem;
import com.qianxin.teaart.item.ReusableToolItem;
import com.qianxin.teaart.item.TeaDrinkItem;
import com.qianxin.teaart.item.TeaDrinkItem.TeaEffect;
import com.qianxin.teaart.item.TeaLeafItem;

import java.util.EnumMap;
import java.util.Map;

public class ModItems {

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, TeaArtMod.MOD_ID);

    public static final Map<TeaLiquor, RegistryObject<Item>> TEA_LIQUOR_BUCKETS = new EnumMap<>(TeaLiquor.class);

    public static final RegistryObject<Item> TEA_SEED = ITEMS.register("tea_seed",
            () -> new ItemNameBlockItem(ModBlocks.TEA_CROP.get(), new Item.Properties()));
    public static final RegistryObject<Item> TEA_LEAF = ITEMS.register("tea_leaf",
            () -> new TeaLeafItem(new Item.Properties()));
    public static final RegistryObject<Item> TEA_FRUIT = ITEMS.register("tea_fruit",
            () -> new Item(new Item.Properties().food(Foods.APPLE)));
    public static final RegistryObject<Item> JASMINE_SEED = ITEMS.register("jasmine_seed",
            () -> new ItemNameBlockItem(ModBlocks.JASMINE_BUSH.get(), new Item.Properties()));
    public static final RegistryObject<Item> GARDENIA_SEED = ITEMS.register("gardenia_seed",
            () -> new ItemNameBlockItem(ModBlocks.GARDENIA_BUSH.get(), new Item.Properties()));
    public static final RegistryObject<Item> JASMINE_FLOWER = item("jasmine_flower");
    public static final RegistryObject<Item> GARDENIA_FLOWER = item("gardenia_flower");
    public static final RegistryObject<Item> TEA_RESIDUE = item("tea_residue");

    public static final RegistryObject<Item> GREEN_TEA_LEAF = item("green_tea_leaf");
    public static final RegistryObject<Item> WHITE_TEA_LEAF = ITEMS.register("white_tea_leaf",
            () -> new TeaLeafItem(new Item.Properties()));
    public static final RegistryObject<Item> YELLOW_TEA_LEAF = item("yellow_tea_leaf");
    public static final RegistryObject<Item> RED_TEA_LEAF = item("red_tea_leaf");
    public static final RegistryObject<Item> BLACK_TEA_LEAF = item("black_tea_leaf");
    public static final RegistryObject<Item> OOLONG_TEA_LEAF = item("oolong_tea_leaf");
    public static final RegistryObject<Item> JASMINE_FLOWER_TEA_LEAF = item("jasmine_flower_tea_leaf");
    public static final RegistryObject<Item> GARDENIA_TEA_LEAF = item("gardenia_tea_leaf");

    public static final RegistryObject<Item> TEA_CUP = ITEMS.register("tea_cup",
            () -> new BlockItem(ModBlocks.TEA_CUP.get(), new Item.Properties()));
    public static final RegistryObject<Item> TEA_SIEVE = ITEMS.register("tea_sieve",
            () -> new BlockItem(ModBlocks.TEA_SIEVE.get(), new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> SPATULA = ITEMS.register("spatula",
            () -> new ReusableToolItem(new Item.Properties().stacksTo(1).durability(96)));
    public static final RegistryObject<Item> BAMBOO_WRENCH = ITEMS.register("bamboo_wrench",
            () -> new BambooWrenchItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> GREEN_TEA = ITEMS.register("green_tea",
            () -> new TeaDrinkItem(new Item.Properties().stacksTo(16),
                    effect(() -> MobEffects.DIG_SPEED, 60 * 20, 1),
                    effect(() -> MobEffects.NIGHT_VISION, 5 * 60 * 20, 0)));
    public static final RegistryObject<Item> WHITE_TEA = ITEMS.register("white_tea",
            () -> new TeaDrinkItem(new Item.Properties().stacksTo(16),
                    effect(() -> MobEffects.DIG_SPEED, 60 * 20, 0),
                    effect(() -> MobEffects.NIGHT_VISION, 5 * 60 * 20, 0)));
    public static final RegistryObject<Item> YELLOW_TEA = ITEMS.register("yellow_tea",
            () -> new TeaDrinkItem(new Item.Properties().stacksTo(16),
                    effect(() -> MobEffects.DIG_SPEED, 3 * 60 * 20, 1),
                    effect(() -> MobEffects.MOVEMENT_SPEED, 3 * 60 * 20, 1)));
    public static final RegistryObject<Item> RED_TEA = ITEMS.register("red_tea",
            () -> new TeaDrinkItem(new Item.Properties().stacksTo(16),
                    effect(() -> MobEffects.DIG_SPEED, 3 * 60 * 20, 1),
                    effect(() -> MobEffects.MOVEMENT_SPEED, 3 * 60 * 20, 1)));
    public static final RegistryObject<Item> BLACK_TEA = ITEMS.register("black_tea",
            () -> new TeaDrinkItem(new Item.Properties().stacksTo(16),
                    effect(() -> MobEffects.DAMAGE_RESISTANCE, 3 * 60 * 20, 1),
                    effect(() -> MobEffects.ABSORPTION, 3 * 60 * 20, 0),
                    effect(() -> MobEffects.REGENERATION, 10 * 20, 0)));
    public static final RegistryObject<Item> OOLONG_TEA = ITEMS.register("oolong_tea",
            () -> new TeaDrinkItem(new Item.Properties().stacksTo(16),
                    effect(() -> MobEffects.DIG_SPEED, 5 * 60 * 20, 1),
                    effect(() -> MobEffects.MOVEMENT_SPEED, 5 * 60 * 20, 1)));
    public static final RegistryObject<Item> JASMINE_FLOWER_TEA = ITEMS.register("jasmine_flower_tea",
            () -> new TeaDrinkItem(new Item.Properties().stacksTo(16),
                    effect(() -> MobEffects.REGENERATION, 20 * 20, 0),
                    effect(() -> MobEffects.NIGHT_VISION, 5 * 60 * 20, 0)));
    public static final RegistryObject<Item> GARDENIA_TEA = ITEMS.register("gardenia_tea",
            () -> new TeaDrinkItem(new Item.Properties().stacksTo(16),
                    effect(() -> MobEffects.DIG_SPEED, 3 * 60 * 20, 0),
                    effect(() -> MobEffects.REGENERATION, 20 * 20, 0)));

    private static RegistryObject<Item> item(String name) {
        return ITEMS.register(name, () -> new Item(new Item.Properties()));
    }

    private static TeaEffect effect(java.util.function.Supplier<net.minecraft.world.effect.MobEffect> effect,
                                    int durationTicks, int amplifier) {
        return new TeaEffect(effect, durationTicks, amplifier);
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}

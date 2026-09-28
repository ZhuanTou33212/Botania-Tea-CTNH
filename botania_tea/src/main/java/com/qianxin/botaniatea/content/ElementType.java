package com.qianxin.botaniatea.content;

import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/** Stable, data-facing element identifiers. Element values are never stored as floating point. */
public enum ElementType {
    AIR("air", "气流", 0xFFDDEEFF), WATER("water", "水流", 0xFF4A9FE8), FIRE("fire", "火焰", 0xFFFF6B35),
    EARTH("earth", "土地", 0xFF9A6A42), STONE("stone", "岩石", 0xFF8C8C8C), METAL("metal", "金属", 0xFFB8C2CC),
    CRYSTAL("crystal", "晶体", 0xFFE79DFF), PLANT("plant", "草木", 0xFF5CAA45), LIFE("life", "生机", 0xFF8BE06D),
    BEAST("beast", "兽性", 0xFFB66C4B), SOUL("soul", "灵魂", 0xFF8D6BC7), MOTION("motion", "动力", 0xFFFFC857),
    MECHANISM("mechanism", "机关", 0xFF777777), TOOL("tool", "工具", 0xFFE2D4AA), PROTECTION("protection", "守护", 0xFF65C6D6),
    SPACE("space", "空间", 0xFF7C83FF), SENSE("sense", "感知", 0xFFFFE275), ORDER("order", "秩序", 0xFFB7D7A8),
    DECAY("decay", "分解", 0xFF6D7650), MAGIC("magic", "魔法", 0xFFCC72E8);

    public final String id;
    public final String chineseName;
    public final int color;

    ElementType(String id, String chineseName, int color) {
        this.id = id;
        this.chineseName = chineseName;
        this.color = color;
    }

    public Component displayName() {
        return Component.translatable("element.botania_tea." + id);
    }

    /**
     * Item tag describing spell affinity, not the source-crystal crafting cost.
     * Keeping this relation in data lets datapacks add or remove spell users
     * without changing the material conservation rules.
     */
    public TagKey<Item> spellTag() {
        return TagKey.create(Registries.ITEM,
                ResourceLocation.fromNamespaceAndPath("botania_tea", "spell_elements/" + id));
    }

    public static ElementType byId(String id) {
        for (ElementType type : values()) if (type.id.equals(id)) return type;
        return null;
    }
}

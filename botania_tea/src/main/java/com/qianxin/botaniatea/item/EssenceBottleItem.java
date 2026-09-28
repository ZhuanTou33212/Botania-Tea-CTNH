package com.qianxin.botaniatea.item;

import com.qianxin.botaniatea.content.TeaEssence;
import net.minecraft.world.item.Item;

public final class EssenceBottleItem extends Item {
    private final TeaEssence essence;

    public EssenceBottleItem(TeaEssence essence, Properties properties) {
        super(properties);
        this.essence = essence;
    }

    public TeaEssence essence() { return essence; }
}

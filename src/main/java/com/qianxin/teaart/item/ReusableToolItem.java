package com.qianxin.teaart.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 可复用工具：像铲子/锄头一样消耗耐久，耐久用尽后可通过合成修复（锅铲 SPATULA 等）。
 * 基础实现继承 Item，仅处理耐久消耗；如需特殊行为可覆盖 useOn / mineBlock 等。
 */
public class ReusableToolItem extends Item {

    public ReusableToolItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isDamageable(ItemStack stack) {
        return true;
    }

    @Override
    public boolean canBeDepleted() {
        return true;
    }
}

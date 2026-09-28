package com.qianxin.botaniatea.registry;

import com.qianxin.botaniatea.BotaniaTeaMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, BotaniaTeaMod.MOD_ID);
    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register("botania_tea", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.botania_tea"))
            .icon(() -> new ItemStack(ModItems.TEA_ESSENCE_STILLBLOOM.get()))
            .displayItems((parameters, output) -> {
                output.accept(ModItems.MAGIC_CRUCIBLE.get());
                ModItems.ITEMS.getEntries().stream().filter(item -> item != ModItems.MAGIC_CRUCIBLE)
                        .forEach(item -> output.accept(item.get()));
            })
            .build());

    private ModCreativeTabs() {}
}

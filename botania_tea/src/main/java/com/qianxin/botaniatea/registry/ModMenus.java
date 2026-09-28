package com.qianxin.botaniatea.registry;

import com.qianxin.botaniatea.BotaniaTeaMod;
import com.qianxin.botaniatea.menu.MagicCrucibleMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, BotaniaTeaMod.MOD_ID);
    public static final RegistryObject<MenuType<MagicCrucibleMenu>> MAGIC_CRUCIBLE =
            MENUS.register("magic_crucible", () -> IForgeMenuType.create(MagicCrucibleMenu::new));
    private ModMenus() {}
    public static final RegistryObject<MenuType<com.qianxin.botaniatea.menu.TeaEssenceDistillerMenu>> TEA_ESSENCE_DISTILLER =
            MENUS.register("tea_essence_distiller", () -> IForgeMenuType.create(com.qianxin.botaniatea.menu.TeaEssenceDistillerMenu::new));
}

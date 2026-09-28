package com.qianxin.teaart.registry;

import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import com.qianxin.teaart.TeaArtMod;
import com.qianxin.teaart.menu.BambooPipeInputPortMenu;
import com.qianxin.teaart.menu.ScentingBoxMenu;
import com.qianxin.teaart.menu.TeaTableMenu;

public class ModMenus {

    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES,
            TeaArtMod.MOD_ID);

    public static final RegistryObject<MenuType<ScentingBoxMenu>> SCENTING_BOX = MENUS.register("scenting_box",
            () -> IForgeMenuType.create(ScentingBoxMenu::new));
    public static final RegistryObject<MenuType<TeaTableMenu>> TEA_TABLE = MENUS.register("tea_table",
            () -> IForgeMenuType.create(TeaTableMenu::new));
    public static final RegistryObject<MenuType<BambooPipeInputPortMenu>> BAMBOO_PIPE_INPUT_PORT = MENUS
            .register("bamboo_pipe_input_port", () -> IForgeMenuType.create(BambooPipeInputPortMenu::new));

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}

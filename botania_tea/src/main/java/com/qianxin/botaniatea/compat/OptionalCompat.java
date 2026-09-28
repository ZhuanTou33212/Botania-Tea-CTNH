package com.qianxin.botaniatea.compat;

import net.minecraftforge.fml.ModList;

/** Optional-module boundary. No optional class is referenced from the common registration path. */
public final class OptionalCompat {
    private OptionalCompat() {}
    public static boolean createLoaded() { return ModList.get().isLoaded("create"); }
    public static boolean farmersDelightLoaded() { return ModList.get().isLoaded("farmersdelight"); }
    public static String status() { return "create=" + (createLoaded() ? "supported" : "absent") + ", farmersdelight=" + (farmersDelightLoaded() ? "supported" : "absent"); }
}

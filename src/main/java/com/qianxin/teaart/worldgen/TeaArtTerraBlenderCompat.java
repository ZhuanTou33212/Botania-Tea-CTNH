package com.qianxin.teaart.worldgen;

import terrablender.api.Regions;

public class TeaArtTerraBlenderCompat {

    private TeaArtTerraBlenderCompat() {}

    public static void registerRegions() {
        Regions.register(new TeaArtOverworldRegion(2));
    }
}

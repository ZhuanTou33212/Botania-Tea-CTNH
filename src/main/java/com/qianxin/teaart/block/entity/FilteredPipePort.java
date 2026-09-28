package com.qianxin.teaart.block.entity;

import net.minecraftforge.items.ItemStackHandler;

public interface FilteredPipePort {

    int FILTER_SLOTS = 10;

    ItemStackHandler getFilters();

    boolean isWhitelist();

    void toggleMode();
}

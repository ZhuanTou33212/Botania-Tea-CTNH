package com.qianxin.teaart.content;

import org.jetbrains.annotations.Nullable;

public enum TeaLiquor {

    WHITE("white", 0xFFF5E6C8),
    GREEN("green", 0xFFC8E6C9),
    YELLOW("yellow", 0xFFE8D5A3),
    RED("red", 0xFFD4A574),
    OOLONG("oolong", 0xFFC9A96E),
    BLACK("black", 0xFF8B5E3C),
    JASMINE_SCENTED("jasmine_scented", 0xFFFFF0B8),
    GARDENIA_SCENTED("gardenia_scented", 0xFFFFE28A);

    private final String id;
    private final int tint;

    TeaLiquor(String id, int tint) {
        this.id = id;
        this.tint = tint;
    }

    public String id() {
        return id;
    }

    public String fluidName() {
        return id + "_tea_liquor";
    }

    public int tint() {
        return tint;
    }

    @Nullable
    public static TeaLiquor byId(String id) {
        for (TeaLiquor liquor : values()) {
            if (liquor.id.equals(id)) return liquor;
        }
        return null;
    }
}

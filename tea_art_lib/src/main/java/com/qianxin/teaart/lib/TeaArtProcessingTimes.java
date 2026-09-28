package com.qianxin.teaart.lib;

public final class TeaArtProcessingTimes {
    private TeaArtProcessingTimes() {}
    public static final int FERMENT_TICKS        = 30 * 20;   // 600
    public static final int PILE_TICKS           = 30 * 20;   // 600
    public static final int SCENTING_TICKS       = 20 * 20;   // 400
    public static final int BREW_TIME_TICKS      =  8 * 20;   // 160
    public static final int DRYING_TICKS         = 20 * 20;   // 400
    public static final int DEFAULT_COOK_TICKS   = 30 * 20;   // 600
    public static final int REQUIRED_KNEADS      = 10;
    public static final int REQUIRED_STIRS       = 10;
}

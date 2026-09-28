package com.qianxin.teaart.api;

/** Explicit opt-in for advancing valid timed processing; manual actions are excluded. */
public interface TimedTeaDevice {

    boolean canAdvanceProcessing();

    boolean tryAdvanceProcessing(int ticks);
}

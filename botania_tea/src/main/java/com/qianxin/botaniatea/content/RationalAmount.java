package com.qianxin.botaniatea.content;

import java.math.BigInteger;

/** Exact non-negative rational used by the element analyzer. */
public record RationalAmount(BigInteger numerator, BigInteger denominator) {
    public static final RationalAmount ZERO = new RationalAmount(BigInteger.ZERO, BigInteger.ONE);

    public RationalAmount {
        if (denominator.signum() <= 0 || numerator.signum() < 0) throw new IllegalArgumentException("invalid rational");
        BigInteger gcd = numerator.gcd(denominator);
        numerator = numerator.divide(gcd);
        denominator = denominator.divide(gcd);
    }

    public static RationalAmount of(long value) { return new RationalAmount(BigInteger.valueOf(value), BigInteger.ONE); }
    public RationalAmount add(RationalAmount other) {
        return new RationalAmount(numerator.multiply(other.denominator).add(other.numerator.multiply(denominator)), denominator.multiply(other.denominator));
    }
    public RationalAmount multiply(long value) { return new RationalAmount(numerator.multiply(BigInteger.valueOf(value)), denominator); }
    public RationalAmount divide(long value) { return new RationalAmount(numerator, denominator.multiply(BigInteger.valueOf(value))); }
    public boolean isZero() { return numerator.signum() == 0; }
    public boolean isInteger() { return denominator.equals(BigInteger.ONE); }
    public long ceilLong() {
        return numerator.add(denominator).subtract(BigInteger.ONE).divide(denominator).longValueExact();
    }
    @Override public String toString() { return denominator.equals(BigInteger.ONE) ? numerator.toString() : numerator + "/" + denominator; }
}

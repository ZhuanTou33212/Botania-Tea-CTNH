package com.qianxin.botaniatea.content;

import java.math.BigInteger;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Recoverable elemental liquid stored in milliBuckets.  This is deliberately separate from
 * {@link ElementVector}: ritual/crystal costs use exact abstract units, while machines and tanks
 * need concrete integer mB values.
 */
public final class ElementLiquidVector {
    public static final int MB_PER_ESSENCE = 100;
    private final Map<ElementType, Long> values;

    public ElementLiquidVector() {
        values = Collections.emptyMap();
    }

    public ElementLiquidVector(Map<ElementType, Long> values) {
        EnumMap<ElementType, Long> copy = new EnumMap<>(ElementType.class);
        values.forEach((type, amount) -> {
            if (amount != null && amount > 0) copy.put(type, amount);
        });
        this.values = Collections.unmodifiableMap(copy);
    }

    public static ElementLiquidVector of(ElementType type, long milliBuckets) {
        return new ElementLiquidVector(Map.of(type, milliBuckets));
    }

    /** Converts one abstract essence unit to 100 mB, rounding fractional mB down. */
    public static ElementLiquidVector from(ElementVector vector) {
        if (vector == null) return new ElementLiquidVector();
        EnumMap<ElementType, Long> result = new EnumMap<>(ElementType.class);
        vector.values().forEach((type, amount) -> {
            BigInteger scaled = amount.numerator().multiply(BigInteger.valueOf(MB_PER_ESSENCE));
            long milliBuckets = scaled.divide(amount.denominator()).longValueExact();
            if (milliBuckets > 0) result.put(type, milliBuckets);
        });
        return new ElementLiquidVector(result);
    }

    public Map<ElementType, Long> values() { return values; }
    public long get(ElementType type) { return values.getOrDefault(type, 0L); }
    public boolean isEmpty() { return values.isEmpty(); }

    public ElementLiquidVector add(ElementLiquidVector other) {
        EnumMap<ElementType, Long> result = new EnumMap<>(ElementType.class);
        values.forEach(result::put);
        other.values.forEach((type, amount) -> result.merge(type, amount, Math::addExact));
        return new ElementLiquidVector(result);
    }

    public ElementLiquidVector multiply(long count) {
        EnumMap<ElementType, Long> result = new EnumMap<>(ElementType.class);
        values.forEach((type, amount) -> result.put(type, Math.multiplyExact(amount, count)));
        return new ElementLiquidVector(result);
    }

    /** Component-wise conservative subtraction; a crafting remainder can never create liquid. */
    public ElementLiquidVector subtractFloor(ElementLiquidVector other) {
        EnumMap<ElementType, Long> result = new EnumMap<>(ElementType.class);
        for (ElementType type : ElementType.values()) {
            long amount = Math.max(0L, get(type) - other.get(type));
            if (amount > 0) result.put(type, amount);
        }
        return new ElementLiquidVector(result);
    }

    /** Component-wise minimum used to cap an output to the liquid present in its ingredients. */
    public ElementLiquidVector clampTo(ElementLiquidVector ceiling) {
        EnumMap<ElementType, Long> result = new EnumMap<>(ElementType.class);
        values.forEach((type, amount) -> {
            long capped = Math.min(amount, ceiling.get(type));
            if (capped > 0) result.put(type, capped);
        });
        return new ElementLiquidVector(result);
    }

    /** Minimum across alternative ingredient choices; missing components count as zero. */
    public ElementLiquidVector componentMin(ElementLiquidVector other) {
        EnumMap<ElementType, Long> result = new EnumMap<>(ElementType.class);
        for (ElementType type : ElementType.values()) {
            long amount = Math.min(get(type), other.get(type));
            if (amount > 0) result.put(type, amount);
        }
        return new ElementLiquidVector(result);
    }

    @Override public boolean equals(Object other) {
        return other instanceof ElementLiquidVector vector && values.equals(vector.values);
    }
    @Override public int hashCode() { return values.hashCode(); }
    @Override public String toString() { return values + " mB"; }
}

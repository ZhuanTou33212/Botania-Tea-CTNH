package com.qianxin.botaniatea.content;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/** Immutable exact vector. Functional categories are intentionally not represented here. */
public final class ElementVector {
    private final Map<ElementType, RationalAmount> values;

    public ElementVector() { this.values = Collections.emptyMap(); }
    public ElementVector(Map<ElementType, RationalAmount> values) {
        EnumMap<ElementType, RationalAmount> copy = new EnumMap<>(ElementType.class);
        values.forEach((type, amount) -> { if (!amount.isZero()) copy.put(type, amount); });
        this.values = Collections.unmodifiableMap(copy);
    }
    public static ElementVector of(ElementType type, long amount) {
        return new ElementVector(Map.of(type, RationalAmount.of(amount)));
    }
    public Map<ElementType, RationalAmount> values() { return values; }
    public boolean isIntegral() { return values.values().stream().allMatch(RationalAmount::isInteger); }
    public RationalAmount get(ElementType type) { return values.getOrDefault(type, RationalAmount.ZERO); }
    public ElementVector add(ElementVector other) {
        EnumMap<ElementType, RationalAmount> result = new EnumMap<>(ElementType.class);
        values.forEach(result::put);
        other.values.forEach((type, amount) -> result.merge(type, amount, RationalAmount::add));
        return new ElementVector(result);
    }
    public ElementVector multiply(long count) {
        EnumMap<ElementType, RationalAmount> result = new EnumMap<>(ElementType.class);
        values.forEach((type, amount) -> result.put(type, amount.multiply(count)));
        return new ElementVector(result);
    }
    public boolean isEmpty() { return values.isEmpty(); }
    public ElementVector divide(long count) {
        EnumMap<ElementType,RationalAmount> result=new EnumMap<>(ElementType.class);
        values.forEach((type,amount)->result.put(type,amount.divide(count)));
        return new ElementVector(result);
    }
    public ElementVector subtract(ElementVector other) {
        EnumMap<ElementType,RationalAmount> result=new EnumMap<>(ElementType.class);
        for (ElementType type:ElementType.values()) {
            RationalAmount a=get(type), b=other.get(type);
            result.put(type,new RationalAmount(a.numerator().multiply(b.denominator()).subtract(b.numerator().multiply(a.denominator())),a.denominator().multiply(b.denominator())));
        }
        return new ElementVector(result);
    }
    @Override public boolean equals(Object other) { return other instanceof ElementVector v && values.equals(v.values); }
    @Override public int hashCode() { return values.hashCode(); }
    @Override public String toString() { return values.toString(); }
}

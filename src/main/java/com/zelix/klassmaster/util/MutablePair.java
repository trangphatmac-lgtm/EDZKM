package com.zelix.klassmaster.util;

public class MutablePair {
    public final Object key;
    public Object value;

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof MutablePair)) {
            return false;
        }

        MutablePair mutablePair1 = (MutablePair) other;
        return this.key.equals(mutablePair1.key)
                && (this.value == null && mutablePair1.value == null || this.value != null && mutablePair1.value != null && this.value.equals(mutablePair1.value));
    }

    public Object setValue(Object valueArgument) {
        Object valueValue = this.value;
        this.value = valueArgument;
        return valueValue;
    }

    @Override
    public int hashCode() {
        return this.value == null ? this.key.hashCode() : this.key.hashCode() ^ this.value.hashCode();
    }

    public MutablePair(Object keyArgument, Object valueArgument) {
        if (keyArgument == null) {
            throw new IllegalArgumentException(this.getClass().getName());
        }

        this.key = keyArgument;
        this.value = valueArgument;
    }

    public Object getValue() {
        return this.value;
    }

    public Object getKey() {
        return this.key;
    }

    public MutablePair(Object keyArgument) {
        if (keyArgument == null) {
            throw new IllegalArgumentException(this.getClass().getName());
        }

        this.key = keyArgument;
    }
}

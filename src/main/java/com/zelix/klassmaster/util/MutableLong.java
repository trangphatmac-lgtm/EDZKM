package com.zelix.klassmaster.util;

import java.io.Serializable;

public class MutableLong implements Comparable, Serializable {
    /** value; the field name is retained for serialized settings compatibility. */
    public long f = 0L;

    public long getValue() {
        return this.f;
    }

    public int compareValue(MutableLong mutableLong1) {
        if (this.f < mutableLong1.f) {
            return -1;
        } else {
            return this.f == mutableLong1.f ? 0 : 1;
        }
    }

    @Override
    public boolean equals(Object other) {
        return ((other != null && other instanceof MutableLong) && (this.f == ((MutableLong) other).f));
    }

    @Override
    public int hashCode() {
        return (int) this.f;
    }

    @Override
    public int compareTo(Object other) {
        return this.compareValue((MutableLong) other);
    }

    public MutableLong(long ba) {
    }

    public void setValue(long value) {
        this.f = value;
    }

    public MutableLong() {
        this(0L);
    }
}

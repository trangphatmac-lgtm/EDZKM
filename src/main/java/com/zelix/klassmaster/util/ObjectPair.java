package com.zelix.klassmaster.util;

import java.io.Serializable;

public class ObjectPair implements Serializable {
    /** first; the field name is retained for serialized settings compatibility. */
    private Object E;
    /** second; the field name is retained for serialized settings compatibility. */
    private Object w;

    public Object setSecond(Object second) {
        Object secondValue = this.w;
        this.w = second;
        return secondValue;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ObjectPair)) {
            return false;
        }

        ObjectPair objectPair1 = (ObjectPair) other;
        return this.E.equals(objectPair1.E)
                && (this.w == null && objectPair1.w == null || this.w != null && objectPair1.w != null && this.w.equals(objectPair1.w));
    }

    public Object setFirst(Object first) {
        if (first == null) {
            throw new IllegalArgumentException(this.getClass().getName());
        }

        Object firstValue = this.E;
        this.E = first;
        return firstValue;
    }

    public Object getFirst() {
        return this.E;
    }

    public Object getSecond() {
        return this.w;
    }

    public ObjectPair(Object first, Object second) {
        if (first == null) {
            throw new IllegalArgumentException(this.getClass().getName());
        }

        this.E = first;
        this.w = second;
    }

    @Override
    public int hashCode() {
        return this.w == null ? this.E.hashCode() : this.E.hashCode() ^ this.w.hashCode();
    }
}

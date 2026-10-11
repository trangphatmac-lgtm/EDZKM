package com.zelix.klassmaster.util;

import com.zelix.klassmaster.classfile.insn.IntCounter;

import java.io.Serializable;

public class MutableInt implements IntCounter, Comparable, Serializable {
    /** value; the field name is retained for serialized settings compatibility. */
    private int Y;

    public int decrementAndGet() {
        this.Y--;
        return this.Y;
    }

    public int compareValue(MutableInt mutableInt1) {
        if (this.Y < mutableInt1.Y) {
            return -1;
        } else {
            return this.Y == mutableInt1.Y ? 0 : 1;
        }
    }

    public void setValue(int value) {
        this.Y = value;
    }

    @Override
    public boolean equals(Object other) {
        return ((other != null && other instanceof MutableInt) && (this.Y == ((MutableInt) other).Y));
    }

    @Override
    public int getValue() {
        return this.Y;
    }

    @Override
    public int addAndGet(int ba) {
        this.Y += ba;
        return this.Y;
    }

    public MutableInt(int value) {
        this.Y = value;
    }

    @Override
    public int getAndIncrement() {
        return this.Y++;
    }

    @Override
    public int hashCode() {
        return this.Y;
    }

    public MutableInt() {
        this(0);
    }

    @Override
    public int incrementAndGet() {
        return ++this.Y;
    }

    @Override
    public int compareTo(Object other) {
        return this.compareValue((MutableInt) other);
    }
}

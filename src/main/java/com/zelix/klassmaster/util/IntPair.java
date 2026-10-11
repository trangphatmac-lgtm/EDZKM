package com.zelix.klassmaster.util;

import java.io.Serializable;

public class IntPair implements Serializable {
    /** first; the field name is retained for serialized settings compatibility. */
    public int a;
    /** second; the field name is retained for serialized settings compatibility. */
    public int b;

    public int getSecond() {
        return this.b;
    }

    public int getFirst() {
        return this.a;
    }

    public IntPair(int first, int second) {
        this.a = first;
        this.b = second;
    }
}

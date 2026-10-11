package com.zelix.klassmaster.util;

import java.io.Serializable;

public class Quadruple implements Serializable {
    /** first; the field name is retained for serialized settings compatibility. */
    public Object G;
    /** second; the field name is retained for serialized settings compatibility. */
    public Object i;
    /** third; the field name is retained for serialized settings compatibility. */
    public Object m;
    /** fourth; the field name is retained for serialized settings compatibility. */
    public Object V;

    public Object getThird() {
        return this.m;
    }

    @Override
    public int hashCode() {
        int ba = 0;
        if (this.G != null) {
            ba = 0 ^ this.G.hashCode();
        }

        if (this.i != null) {
            ba ^= this.i.hashCode();
        }

        Object fourth;
        if (this.m != null) {
            ba ^= this.m.hashCode();
            fourth = this.V;
        } else {
            fourth = this.V;
        }

        if (fourth != null) {
            ba ^= this.V.hashCode();
        }

        return ba;
    }

    public Quadruple(Object first, Object second, Object third, Object fourth) {
        this.G = first;
        this.i = second;
        this.m = third;
        this.V = fourth;
    }

    public Object getFirst() {
        return this.G;
    }

    public Object getFourth() {
        return this.V;
    }

    public Object getSecond() {
        return this.i;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Quadruple)) {
            return false;
        }

        Quadruple quadruple1 = (Quadruple) other;
        return (this.G == null && quadruple1.G == null || this.G != null && quadruple1.G != null && this.G.equals(quadruple1.G))
                && (this.i == null && quadruple1.i == null || this.i != null && quadruple1.i != null && this.i.equals(quadruple1.i))
                && (this.m == null && quadruple1.m == null || this.m != null && quadruple1.m != null && this.m.equals(quadruple1.m))
                && (this.V == null && quadruple1.V == null || this.V != null && quadruple1.V != null && this.V.equals(quadruple1.V));
    }
}

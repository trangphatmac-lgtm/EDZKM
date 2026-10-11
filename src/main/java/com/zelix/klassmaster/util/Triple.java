package com.zelix.klassmaster.util;

import java.io.Serializable;

public class Triple implements Serializable {
    /** first; the field name is retained for serialized settings compatibility. */
    public Object N;
    /** second; the field name is retained for serialized settings compatibility. */
    public Object n;
    /** third; the field name is retained for serialized settings compatibility. */
    public Object z;

    public Object getFirst() {
        return this.N;
    }

    @Override
    public int hashCode() {
        int ba = 0;
        if (this.N != null) {
            ba = 0 ^ this.N.hashCode();
        }

        Object third;
        if (this.n != null) {
            ba ^= this.n.hashCode();
            third = this.z;
        } else {
            third = this.z;
        }

        if (third != null) {
            ba ^= this.z.hashCode();
        }

        return ba;
    }

    public Triple(Object first, Object second, Object third) {
        this.N = first;
        this.n = second;
        this.z = third;
    }

    public Object getThird() {
        return this.z;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Triple)) {
            return false;
        }

        Triple triple1 = (Triple) other;
        return (this.N == null && triple1.N == null || this.N != null && triple1.N != null && this.N.equals(triple1.N))
                && (this.n == null && triple1.n == null || this.n != null && triple1.n != null && this.n.equals(triple1.n))
                && (this.z == null && triple1.z == null || this.z != null && triple1.z != null && this.z.equals(triple1.z));
    }

    public Object getSecond() {
        return this.n;
    }
}

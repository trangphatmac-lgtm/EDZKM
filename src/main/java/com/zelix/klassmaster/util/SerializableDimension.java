package com.zelix.klassmaster.util;

import java.io.Serializable;

public class SerializableDimension implements Serializable {
    /** width; the field name is retained for serialized settings compatibility. */
    public int a;
    /** height; the field name is retained for serialized settings compatibility. */
    public int b;

    public int getHeight() {
        return this.b;
    }

    public int getWidth() {
        return this.a;
    }

    public SerializableDimension(int width, int height) {
        this.a = width;
        this.b = height;
    }
}

package com.zelix.klassmaster.proguard.config.parser;

import com.zelix.klassmaster.util.ZkmAssert;

import java.io.Serializable;

public class ProGuardConfigToken implements Serializable {
    public ProGuardConfigToken w;
    public int s;
    public ProGuardConfigToken Y;
    public int R;
    public int F;
    public int c;
    public int d;
    public String M;
    private static final String NULL_IMAGE_SUFFIX = " NULL >";

    public static ProGuardConfigToken newToken(int ba) {
        return newToken(ba, (String) null);
    }

    public ProGuardConfigToken() {
    }

    @Override
    public String toString() {
        return this.M == null ? "<" + ZkmAssert.getSimpleClassName(this) + NULL_IMAGE_SUFFIX : this.M;
    }

    public ProGuardConfigToken(int ba, String string) {
        this.s = ba;
        this.M = string;
    }

    public static ProGuardConfigToken newToken(int ba, String string) {
        switch (ba) {
            default:
                return new ProGuardConfigToken(ba, string);
        }
    }
}

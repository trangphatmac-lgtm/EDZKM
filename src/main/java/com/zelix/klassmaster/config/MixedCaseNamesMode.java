package com.zelix.klassmaster.config;

import java.io.Serializable;

public class MixedCaseNamesMode implements Serializable {
    // Preserve the existing serialized identity while recovering non-persistent member names.
    private static final long serialVersionUID = 8252886617585476780L;

    public static final MixedCaseNamesMode ALWAYS = new MixedCaseNamesMode(0);
    public static final MixedCaseNamesMode NEVER = new MixedCaseNamesMode(1);
    public static final MixedCaseNamesMode IF_IN_ARCHIVE = new MixedCaseNamesMode(2);
    public static final MixedCaseNamesMode[] VALUES = new MixedCaseNamesMode[]{ALWAYS, NEVER, IF_IN_ARCHIVE};
    /** value; the field name is retained for serialized settings compatibility. */
    public final int c;

    public int getValue() {
        return this.c;
    }

    public static MixedCaseNamesMode fromValue(int valueArgument) {
        if (valueArgument >= 0 && valueArgument < VALUES.length) {
            return VALUES[valueArgument];
        } else {
            throw new IllegalArgumentException();
        }
    }

    private MixedCaseNamesMode(int value) {
        this.c = value;
    }

    @Override
    public String toString() {
        switch (this.c) {
            case 0:
                return "ALWAYS";
            case 1:
                return "NEVER";
            case 2:
                return "IF_IN_ARCHIVE";
            default:
                return "ERROR";
        }
    }
}

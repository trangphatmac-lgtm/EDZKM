package com.zelix.klassmaster.util;

public class IntegerCache {
    private static volatile IntegerCache instance;
    public static final Integer MINUS_ONE = -1;
    private Integer[] cache = new Integer[1000];
    private int capacity = 1000;

    public Integer valueOf(int integerArgument) {
        if (integerArgument == -1) {
            return MINUS_ONE;
        }

        if (integerArgument > 32767) {
            return integerArgument;
        }

        synchronized (this) {
            if (integerArgument >= this.capacity) {
                int capacityValue = integerArgument * 2;
                Integer[] integers = new Integer[capacityValue];
                System.arraycopy(this.cache, 0, integers, 0, this.capacity);
                this.cache = integers;
                this.capacity = capacityValue;
            }

            Integer integer = this.cache[integerArgument];
            if (integer == null) {
                integer = integerArgument;
                this.cache[integerArgument] = integer;
            }

            return integer;
        }
    }

    private IntegerCache() {
    }

    public static IntegerCache getInstance() {
        if (instance == null) {
            instance = new IntegerCache();
        }

        return instance;
    }
}

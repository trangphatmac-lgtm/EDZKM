package com.zelix.klassmaster.util;

import java.util.Enumeration;
import java.util.Map;

public class ReadOnlyMultiMap {
    public TwoKeyMap map;
    private static final String NULL_ARGUMENT_MESSAGE = "Null argument not allowed";

    public final int getKeyCount() {
        return this.map.getKeyCount();
    }

    @Override
    public final Object clone() {
        return new ReadOnlyMultiMap(ZkmUtils.copyTwoKeyMap(this.map));
    }

    public EnumerableMap getInnerMap(Object key) {
        Map map1 = this.map.getInnerMap(key);
        return map1 != null ? new EnumerableMap(map1) : null;
    }

    public final synchronized Enumeration keys() {
        return this.map.keys();
    }

    public ReadOnlyMultiMap(TwoKeyMap twoKeyMap) {
        if (twoKeyMap == null) {
            throw new IllegalArgumentException(NULL_ARGUMENT_MESSAGE);
        }

        this.map = twoKeyMap;
    }

    public final Object getValue(Object key, Object keyArgument) {
        return this.map.getValue(key, keyArgument);
    }
}

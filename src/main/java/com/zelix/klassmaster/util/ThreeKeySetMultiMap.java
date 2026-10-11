package com.zelix.klassmaster.util;

import java.util.Map;

public class ThreeKeySetMultiMap {
    public final int nestedCapacity = 10;
    public final int innerCapacity = 16;
    public final int valueSetCapacity = 16;
    public Map map = ZkmUtils.createHashMap(75);

    public void clear() {
        this.map.clear();
    }

    public SetMultiMap getSetMultiMap(Object key, Object keyArgument) {
        TwoKeySetMultiMap twoKeySetMultiMap = (TwoKeySetMultiMap) this.map.get(key);
        return twoKeySetMultiMap == null ? null : twoKeySetMultiMap.getSetMultiMap(keyArgument);
    }

    public boolean addValue(Object key, Object keyArgument, Object keyArgument2, Object object3) {
        TwoKeySetMultiMap twoKeySetMultiMap = (TwoKeySetMultiMap) this.map.get(key);
        if (twoKeySetMultiMap == null) {
            twoKeySetMultiMap = new TwoKeySetMultiMap(this.nestedCapacity, this.innerCapacity, this.valueSetCapacity);
            boolean bl = twoKeySetMultiMap.addValue(keyArgument, keyArgument2, object3);
            this.map.put(key, twoKeySetMultiMap);
            return bl;
        } else {
            return twoKeySetMultiMap.addValue(keyArgument, keyArgument2, object3);
        }
    }

    public ThreeKeySetMultiMap(int ba, int bb, int bc, int bd) {
    }

    public ThreeKeySetMultiMap(int ba, int bb) {
        this(75, 10, 16, 16);
    }

    public ThreeKeySetMultiMap() {
        this(75, 10);
    }
}

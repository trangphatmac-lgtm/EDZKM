package com.zelix.klassmaster.util;

import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ThreeKeyConcurrentMap {
    public int outerCapacity;
    public int innerCapacity;
    public ConcurrentHashMap map;

    public TwoKeyMap getTwoKeyMap(Object key) {
        return (TwoKeyMap) this.map.get(key);
    }

    public boolean containsKeys(Object key, Object keyArgument, Object keyArgument2) {
        TwoKeyMap twoKeyMap = (TwoKeyMap) this.map.get(key);
        return (!(twoKeyMap == null) && (twoKeyMap.containsKeys(keyArgument, keyArgument2)));
    }

    public ThreeKeyConcurrentMap(int ba, int outerCapacity, int bc) {
        this.outerCapacity = outerCapacity;
        this.innerCapacity = 5;
        this.map = new ConcurrentHashMap(ZkmUtils.getPrimeCapacity(ba));
    }

    public TwoKeyMap removeTwoKeyMap(Object key) {
        return (TwoKeyMap) this.map.remove(key);
    }

    public Object getValue(Object key, Object keyArgument2, Object keyArgument) {
        TwoKeyMap twoKeyMap = (TwoKeyMap) this.map.get(key);
        if (twoKeyMap == null) {
            return null;
        }

        Map map1 = twoKeyMap.getInnerMap(keyArgument2);
        return map1 == null ? null : map1.get(keyArgument);
    }

    public Object removeValue(Object key, Object keyArgument, Object keyArgument2) {
        TwoKeyMap twoKeyMap = (TwoKeyMap) this.map.get(key);
        if (twoKeyMap != null) {
            Object object3 = twoKeyMap.removeValue(keyArgument, keyArgument2);
            if (twoKeyMap.getKeyCount() == 0) {
                this.map.remove(key);
            }

            return object3;
        } else {
            return null;
        }
    }

    public Enumeration keys() {
        return this.map.keys();
    }

    public ThreeKeyConcurrentMap(int ba) {
        this(ba, 10, 5);
    }

    public boolean containsKeyPair(Object key, Object keyArgument) {
        TwoKeyMap twoKeyMap = (TwoKeyMap) this.map.get(key);
        return (!(twoKeyMap == null) && (twoKeyMap.containsKey(keyArgument)));
    }

    public ThreeKeyConcurrentMap(int ba, int bb) {
        this(ba, 5, 5);
    }

    public Map getInnerMap(Object key, Object keyArgument) {
        TwoKeyMap twoKeyMap = (TwoKeyMap) this.map.get(key);
        return twoKeyMap == null ? null : twoKeyMap.getInnerMap(keyArgument);
    }

    public Object putValue(Object key, Object keyArgument, Object keyArgument2, Object value) {
        TwoKeyMap twoKeyMap = (TwoKeyMap) this.map.get(key);
        if (twoKeyMap == null) {
            twoKeyMap = new TwoKeyMap(this.outerCapacity, this.innerCapacity);
            Object object4 = twoKeyMap.putValue(keyArgument, keyArgument2, value);
            this.map.put(key, twoKeyMap);
            return object4;
        } else {
            return twoKeyMap.putValue(keyArgument, keyArgument2, value);
        }
    }

    static {
        try {
            staticInit();
        } catch (Exception exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private static void staticInit() {
    }
}

package com.zelix.klassmaster.util;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public class TwoKeyMap {
    public boolean linked;
    private boolean concurrent;
    private int outerCapacity;
    private int innerCapacity;
    private Map map;

    public TwoKeyMap deepCopy() {
        int mapSize = this.map.size();
        TwoKeyMap twoKeyMap1 = new TwoKeyMap(this.linked, mapSize * 2 + 1, this.innerCapacity, this.concurrent);
        Iterator iterator = this.map.entrySet().iterator();

        while (iterator.hasNext()) {
            Entry entry = (Entry) iterator.next();
            Map map1 = (Map) entry.getValue();
            Map map2 = this.copyInnerMap(map1);
            twoKeyMap1.putInnerMap(entry.getKey(), map2);
        }

        return twoKeyMap1;
    }

    public int getValueCount() {
        int valueCount = 0;
        Iterator iterator = this.map.values().iterator();

        while (iterator.hasNext()) {
            Map map1 = (Map) iterator.next();
            if (map1 != null) {
                valueCount += map1.size();
            }
        }

        return valueCount;
    }

    public Object getValue(Object key, Object keyArgument) {
        Map map1 = (Map) this.map.get(key);
        return map1 == null ? null : map1.get(keyArgument);
    }

    public boolean isEmpty() {
        return this.map.isEmpty();
    }

    public Enumeration innerMaps() {
        return Collections.enumeration(this.map.values());
    }

    public TwoKeyMap(boolean concurrentArgument) {
        this(75, 15, concurrentArgument);
    }

    public Map getInnerMap(Object key) {
        return (Map) this.map.get(key);
    }

    public int getKeyCount() {
        return this.map.size();
    }

    public TwoKeyMap(boolean bl, boolean bl1) {
        this(true, 75, 15, false);
    }

    private Map createInnerMap() {
        if (this.linked) {
            return new LinkedHashMap(this.innerCapacity);
        } else {
            return this.concurrent ? new ConcurrentHashMap(this.innerCapacity) : ZkmUtils.createHashMap(this.innerCapacity);
        }
    }

    public Set entrySet() {
        return this.map.entrySet();
    }

    public TwoKeyMap() {
        this(75, 15, false);
    }

    public TwoKeyMap(int ba, int bb, boolean concurrentArgument) {
        this(false, ba, bb, concurrentArgument);
    }

    public TwoKeyMap(int ba, int bb) {
        this(ba, bb, false);
    }

    public TwoKeyMap(int ba, boolean bl) {
        this(ba, 15, false);
    }

    public Map copyOf(Map map1) {
        return this.copyInnerMap(map1);
    }

    public Object removeValue(Object key, Object keyArgument) {
        Object object2 = null;
        Map map1 = (Map) this.map.get(key);
        if (map1 != null) {
            object2 = map1.remove(keyArgument);
            if (map1.size() == 0) {
                this.map.remove(key);
            }
        }

        return object2;
    }

    public boolean containsKeys(Object key, Object keyArgument) {
        Map map1 = (Map) this.map.get(key);
        return (!(map1 == null) && (map1.containsKey(keyArgument)));
    }

    public Map removeInnerMap(Object key) {
        return (Map) this.map.remove(key);
    }

    public Enumeration keys() {
        return Collections.enumeration(this.map.keySet());
    }

    public Enumeration distinctValues() {
        HashSet hashSet = ZkmUtils.createHashSet();
        Iterator iterator = this.map.values().iterator();

        while (iterator.hasNext()) {
            Iterator iterator1 = ((Map) iterator.next()).values().iterator();

            while (iterator1.hasNext()) {
                hashSet.add(iterator1.next());
            }
        }

        return Collections.enumeration(hashSet);
    }

    public TwoKeyMap(boolean linked, int ba, int bb, boolean concurrent) {
        this.linked = linked;
        this.concurrent = concurrent;
        this.outerCapacity = ZkmUtils.getPrimeCapacity(ba);
        this.innerCapacity = ZkmUtils.getPrimeCapacity(bb);
        if (linked) {
            this.map = new LinkedHashMap(this.outerCapacity);
        } else if (concurrent) {
            this.map = new ConcurrentHashMap(this.outerCapacity);
        } else {
            this.map = ZkmUtils.createHashMap(this.outerCapacity);
        }
    }

    public Map copyInnerMap(Map map1) {
        if (this.linked) {
            return new LinkedHashMap(map1);
        } else {
            return this.concurrent ? new ConcurrentHashMap(map1) : ZkmUtils.copyToHashMap(map1);
        }
    }

    public Set keySet() {
        return this.map.keySet();
    }

    public TwoKeyMap(int ba) {
        this(ba, 15, false);
    }

    public void clear() {
        Iterator iterator = this.map.values().iterator();

        while (iterator.hasNext()) {
            ((Map) iterator.next()).clear();
        }

        this.map.clear();
    }

    public boolean containsKey(Object key) {
        return this.map.containsKey(key);
    }

    public Map putInnerMap(Object key, Map map1) {
        return ((java.util.Map) (this.map.put(key, this.copyInnerMap(map1))));
    }

    public Object putValue(Object key, Object keyArgument, Object value) {
        Object object3 = null;
        Map map1 = (Map) this.map.get(key);
        if (map1 == null) {
            map1 = this.createInnerMap();
            map1.put(keyArgument, value);
            this.map.put(key, map1);
        } else {
            object3 = map1.put(keyArgument, value);
        }

        return object3;
    }
}

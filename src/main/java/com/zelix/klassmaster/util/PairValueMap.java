package com.zelix.klassmaster.util;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class PairValueMap implements Map {
    public Map map = ZkmUtils.createHashMap();

    @Override
    public Collection values() {
        return this.map.values();
    }

    @Override
    public boolean containsKey(Object key) {
        return this.map.containsKey(key);
    }

    @Override
    public Object get(Object key) {
        return this.getPair(key);
    }

    @Override
    public Object put(Object key, Object object1) {
        return this.putPairValue(key, (ObjectPair) object1);
    }

    @Override
    public boolean isEmpty() {
        return this.map.isEmpty();
    }

    @Override
    public Object remove(Object key) {
        return this.removePair(key);
    }

    @Override
    public Set entrySet() {
        return this.map.entrySet();
    }

    public ObjectPair putPair(Object key, Object first, Object second) {
        ObjectPair objectPair = new ObjectPair(first, second);
        return ((com.zelix.klassmaster.util.ObjectPair) (this.map.put(key, objectPair)));
    }

    @Override
    public Set keySet() {
        return this.map.keySet();
    }

    @Override
    public int size() {
        return this.map.size();
    }

    @Override
    public void putAll(Map map1) {
        this.map.putAll(map1);
    }

    @Override
    public boolean containsValue(Object object) {
        return this.map.containsValue(object);
    }

    public ObjectPair putPairValue(Object key, Object value) {
        return (ObjectPair) this.map.put(key, value);
    }

    public ObjectPair removePair(Object key) {
        return (ObjectPair) this.map.remove(key);
    }

    public ObjectPair getPair(Object key) {
        return (ObjectPair) this.map.get(key);
    }

    @Override
    public void clear() {
        this.map.clear();
    }
}

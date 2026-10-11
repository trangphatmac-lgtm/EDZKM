package com.zelix.klassmaster.util;

import java.util.IdentityHashMap;
import java.util.Set;

public class IdentityMapWrapper {
    private final IdentityHashMap map;

    public IdentityMapWrapper() {
        this.map = new IdentityHashMap();
    }

    public Object remove(Object key) {
        return this.map.remove(key);
    }

    public int size() {
        return this.map.size();
    }

    public Set keySet() {
        return this.map.keySet();
    }

    public void clear() {
        this.map.clear();
    }

    public IdentityMapWrapper(int ba) {
        this.map = new IdentityHashMap(ZkmUtils.getPrimeCapacity(ba));
    }

    public boolean isEmpty() {
        return this.map.isEmpty();
    }

    public boolean containsKey(Object key) {
        return this.map.containsKey(key);
    }

    public Object put(Object key, Object value) {
        return this.map.put(key, value);
    }

    public Object get(Object key) {
        return this.map.get(key);
    }
}

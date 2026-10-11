package com.zelix.klassmaster.util;

import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class NestedMultiMap {
    public int innerInitialCapacity;
    public int innerListCapacity;
    public boolean concurrent;
    public Map map;

    public NestedMultiMap() {
        this(75, 10, 5, false);
    }

    public NestedMultiMap(int ba, boolean concurrentArgument) {
        this(ba, 5, 5, concurrentArgument);
    }

    public Enumeration keys() {
        return new CollectionSnapshotEnumeration(this.map.keySet());
    }

    public synchronized void clearAll() {
        Iterator iterator = this.map.values().iterator();

        while (iterator.hasNext()) {
            ((ListMultimap) iterator.next()).clear();
        }

        this.map.clear();
    }

    public ListMultimap putMultimap(Object key, Object value) {
        return (ListMultimap) this.map.put(key, value);
    }

    public ListMultimap getMultimap(Object key) {
        return (ListMultimap) this.map.get(key);
    }

    public ListMultimap removeMultimap(Object key) {
        return (ListMultimap) this.map.remove(key);
    }

    public Set entrySet() {
        return this.map.entrySet();
    }

    public synchronized void addValues(Object key, Object keyArgument, Collection collection1) {
        ListMultimap listMultimap = (ListMultimap) this.map.get(key);
        if (listMultimap == null) {
            listMultimap = new ListMultimap(false, this.innerInitialCapacity, collection1.size(), this.concurrent);
            this.map.put(key, listMultimap);
        }

        for (Object object2 : collection1) {
            listMultimap.addValue(keyArgument, object2);
        }
    }

    public boolean isEmpty() {
        return this.map.isEmpty();
    }

    public NestedMultiMap(int ba, int innerInitialCapacityArgument, int innerListCapacityArgument) {
        this(ba, innerInitialCapacityArgument, innerListCapacityArgument, false);
    }

    public int getKeyCount() {
        return this.map.size();
    }

    public NestedMultiMap(boolean concurrentArgument) {
        this(75, 10, 5, concurrentArgument);
    }

    public synchronized boolean containsValue(Object key, Object keyArgument, Object object2) {
        ListMultimap listMultimap = (ListMultimap) this.map.get(key);
        return (!(listMultimap == null) && (listMultimap.containsValue(keyArgument, object2)));
    }

    public NestedMultiMap(int ba) {
        this(ba, 10, 5, false);
    }

    public synchronized List getValues(Object key, Object keyArgument) {
        ListMultimap listMultimap = (ListMultimap) this.map.get(key);
        return listMultimap == null ? null : listMultimap.getValues(keyArgument);
    }

    public synchronized void addValue(Object key, Object keyArgument, Object object2) {
        ListMultimap listMultimap = (ListMultimap) this.map.get(key);
        if (listMultimap == null) {
            listMultimap = new ListMultimap(false, this.innerInitialCapacity, this.innerListCapacity, this.concurrent);
            listMultimap.addValue(keyArgument, object2);
            this.map.put(key, listMultimap);
        } else {
            listMultimap.addValue(keyArgument, object2);
        }
    }

    public boolean containsKey(Object key) {
        return this.map.containsKey(key);
    }

    public NestedMultiMap(int ba, int innerInitialCapacity, int innerListCapacity, boolean concurrent) {
        this.innerInitialCapacity = innerInitialCapacity;
        this.innerListCapacity = innerListCapacity;
        this.concurrent = concurrent;
        if (concurrent) {
            this.map = new ConcurrentHashMap(ZkmUtils.getPrimeCapacity(ba));
        } else {
            this.map = ZkmUtils.createHashMap(ZkmUtils.getPrimeCapacity(ba));
        }
    }

    public Set keySet() {
        return this.map.keySet();
    }

    public synchronized List removeInnerKey(Object key, Object keyArgument) {
        ListMultimap listMultimap = (ListMultimap) this.map.get(key);
        return listMultimap != null ? listMultimap.removeKey(keyArgument) : null;
    }

    public synchronized boolean containsInnerKey(Object key, Object keyArgument) {
        ListMultimap listMultimap = (ListMultimap) this.map.get(key);
        return (!(listMultimap == null) && (listMultimap.containsKey(keyArgument)));
    }

    public synchronized boolean removeValue(Object key, Object keyArgument, Object object2) {
        ListMultimap listMultimap = (ListMultimap) this.map.get(key);
        return ((listMultimap != null) && (listMultimap.removeValue(keyArgument, object2)));
    }
}

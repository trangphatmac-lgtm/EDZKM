package com.zelix.klassmaster.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class OrderedIndexedMap implements Serializable, Map {
    public Map Z;
    public List B;

    @Override
    public boolean isEmpty() {
        return this.Z.isEmpty();
    }

    public OrderedIndexedMap(int ba) {
        this.Z = ZkmUtils.createHashMap(ZkmUtils.getPrimeCapacity(ba));
        this.B = new ArrayList(ba);
    }

    @Override
    public synchronized Collection values() {
        return this.getValueList();
    }

    public OrderedIndexedMap() {
        this.Z = ZkmUtils.createHashMap();
        this.B = new ArrayList();
    }

    public List getKeyList() {
        int ba = this.B.size();
        ArrayList arrayList = new ArrayList();

        for (int elementIndex = 0; elementIndex < ba; elementIndex++) {
            ObjectPair objectPair = (ObjectPair) this.B.get(elementIndex);
            arrayList.add(objectPair.getFirst());
        }

        return arrayList;
    }

    public synchronized Object getKeyAt(int elementIndex) {
        if (elementIndex >= 0 && elementIndex < this.B.size()) {
            return ((ObjectPair) this.B.get(elementIndex)).getFirst();
        } else {
            throw new IllegalArgumentException("Index " + elementIndex + " out of range : " + this.B.size());
        }
    }

    @Override
    public synchronized Object remove(Object keyArgument) {
        Object second = null;
        ObjectPair objectPair = (ObjectPair) this.Z.remove(keyArgument);
        if (objectPair != null) {
            int ba = this.B.size();

            for (int elementIndex = 0; elementIndex < ba; elementIndex++) {
                if (((ObjectPair) this.B.get(elementIndex)).getFirst().equals(keyArgument)) {
                    this.B.remove(elementIndex);
                    break;
                }
            }

            second = objectPair.getSecond();
        }

        return second;
    }

    @Override
    public synchronized void putAll(Map map1) {
        throw new UnsupportedOperationException();
    }

    @Override
    public synchronized Object clone() {
        return new OrderedIndexedMap(this);
    }

    public int indexOfKey(Object keyArgument) {
        if (this.Z.containsKey(keyArgument)) {
            int ba = this.B.size();

            for (int elementIndex = 0; elementIndex < ba; elementIndex++) {
                if (((ObjectPair) this.B.get(elementIndex)).getFirst().equals(keyArgument)) {
                    return elementIndex;
                }
            }
        }

        return -1;
    }

    @Override
    public synchronized Set keySet() {
        return Collections.unmodifiableSet(this.Z.keySet());
    }

    @Override
    public synchronized void clear() {
        this.Z.clear();
        this.B.clear();
    }

    @Override
    public synchronized Set entrySet() {
        ArrayList arrayList = new ArrayList();
        Iterator iterator = this.B.iterator();

        while (iterator.hasNext()) {
            ObjectPair objectPair = (ObjectPair) iterator.next();
            arrayList.add(new SimpleMapEntry(objectPair));
        }

        return new NonNullSet(new SyncIndexedSet(arrayList));
    }

    @Override
    public int size() {
        return this.B.size();
    }

    @Override
    public synchronized boolean containsValue(Object object) {
        int ba = this.B.size();

        for (int elementIndex = 0; elementIndex < ba; elementIndex++) {
            if (((ObjectPair) this.B.get(elementIndex)).getSecond().equals(object)) {
                return true;
            }
        }

        return false;
    }

    public synchronized Enumeration keyEnumeration() {
        return Collections.enumeration(this.getKeyList());
    }

    public List getValueList() {
        int ba = this.B.size();
        ArrayList arrayList = new ArrayList();

        for (int elementIndex = 0; elementIndex < ba; elementIndex++) {
            ObjectPair objectPair = (ObjectPair) this.B.get(elementIndex);
            arrayList.add(objectPair.getSecond());
        }

        return arrayList;
    }

    public synchronized Object removeAt(int elementIndex) {
        if (elementIndex >= 0 && elementIndex < this.B.size()) {
            ObjectPair objectPair = (ObjectPair) this.B.remove(elementIndex);
            return ((ObjectPair) this.Z.remove(objectPair.getFirst())).getSecond();
        } else {
            throw new IllegalArgumentException("Index " + elementIndex + " out of range : " + this.B.size());
        }
    }

    public synchronized Object replaceEntryAt(int elementIndex, Object keyArgument, Object second) {
        if (elementIndex < 0 || elementIndex >= this.B.size()) {
            throw new IllegalArgumentException("Index " + elementIndex + " out of range : " + this.B.size());
        }

        if (this.Z.containsKey(keyArgument)) {
            if (((ObjectPair) this.Z.get(keyArgument)).equals(this.B.get(elementIndex))) {
                return ((ObjectPair) this.B.get(elementIndex)).getSecond();
            } else {
                throw new IllegalArgumentException("'" + keyArgument + "' is not a unique key");
            }
        } else {
            ObjectPair objectPair = new ObjectPair(keyArgument, second);
            ObjectPair objectPair1 = ((com.zelix.klassmaster.util.ObjectPair) (this.B.set(elementIndex, objectPair)));
            ObjectPair objectPair2 = (ObjectPair) this.Z.remove(objectPair1.getFirst());
            this.Z.put(keyArgument, objectPair);
            return objectPair2.getSecond();
        }
    }

    @Override
    public synchronized Object get(Object keyArgument) {
        ObjectPair objectPair = (ObjectPair) this.Z.get(keyArgument);
        return objectPair == null ? null : objectPair.getSecond();
    }

    @Override
    public synchronized Object put(Object keyArgument, Object second) {
        Object object2 = null;
        ObjectPair objectPair = (ObjectPair) this.Z.get(keyArgument);
        if (objectPair == null) {
            objectPair = new ObjectPair(keyArgument, second);
            this.Z.put(keyArgument, objectPair);
            this.B.add(objectPair);
        } else {
            object2 = objectPair.setSecond(second);
        }

        return object2;
    }

    public OrderedIndexedMap(OrderedIndexedMap orderedIndexedMap1) {
        this(orderedIndexedMap1.size());
        synchronized (orderedIndexedMap1) {
            int orderedIndexedMap1Size = orderedIndexedMap1.size();

            for (int elementIndex = 0; elementIndex < orderedIndexedMap1Size; elementIndex++) {
                ObjectPair objectPair = (ObjectPair) orderedIndexedMap1.B.get(elementIndex);
                Object first = objectPair.getFirst();
                Object second = objectPair.getSecond();
                ObjectPair objectPair1 = new ObjectPair(first, second);
                this.Z.put(first, objectPair1);
                this.B.add(objectPair1);
            }
        }
    }

    @Override
    public synchronized boolean containsKey(Object keyArgument) {
        return this.Z.containsKey(keyArgument);
    }
}

package com.zelix.klassmaster.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public class ListMultimap implements MultiMap {
    private static final String TRIM_TO_SIZE_METHOD = "trimToSize";
    private int initialListCapacity = 5;
    private MultiMapValueEnumeration cachedValueEnumeration = null;
    public final boolean identityKeys;
    private final boolean concurrent;
    public Map map;

    public List putValues(Object key, Object value) {
        if (this.cachedValueEnumeration != null) {
            MultiMapValueEnumeration.invalidateEnumeration(this.cachedValueEnumeration);
            this.cachedValueEnumeration = null;
        }

        return (List) this.map.put(key, value);
    }

    @Override
    public void clear() {
        Iterator iterator = this.map.values().iterator();

        while (iterator.hasNext()) {
            ((List) iterator.next()).clear();
        }

        this.map.clear();
        if (this.cachedValueEnumeration != null) {
            MultiMapValueEnumeration.invalidateEnumeration(this.cachedValueEnumeration);
            this.cachedValueEnumeration = null;
        }
    }

    public ListMultimap() {
        this(false, 75, 5, false);
    }

    public List createDefaultList() {
        return this.concurrent ? new Vector(this.initialListCapacity) : new ArrayList(this.initialListCapacity);
    }

    @Override
    public List removeKey(Object key) {
        if (this.cachedValueEnumeration != null) {
            MultiMapValueEnumeration.invalidateEnumeration(this.cachedValueEnumeration);
            this.cachedValueEnumeration = null;
        }

        return (List) this.map.remove(key);
    }

    @Override
    public synchronized Enumeration keys() {
        return Collections.enumeration(this.map.keySet());
    }

    public ListMultimap(int ba, int initialListCapacityArgument) {
        this(false, ba, initialListCapacityArgument, false);
    }

    public boolean isConcurrent() {
        return this.concurrent;
    }

    public void addAllFrom(ListMultimap listMultimap1) {
        if (this.cachedValueEnumeration != null) {
            MultiMapValueEnumeration.invalidateEnumeration(this.cachedValueEnumeration);
            this.cachedValueEnumeration = null;
        }

        Iterator iterator = listMultimap1.map.entrySet().iterator();

        while (iterator.hasNext()) {
            Entry entry = (Entry) iterator.next();
            Object key = entry.getKey();
            List list1 = (List) entry.getValue();
            List list2 = (List) this.map.get(key);
            if (list2 == null) {
                list2 = this.createDefaultList();
                list2.addAll(list1);
                this.map.put(key, list2);
            } else {
                list2.addAll(list1);
            }
        }
    }

    public ListMultimap(int ba) {
        this(false, ba, 5, false);
    }

    public void prependValues(Object key, Collection collection1) {
        this.insertValues(key, collection1, true);
    }

    private void insertValues(Object key, Collection collection1, boolean bl) {
        if (this.cachedValueEnumeration != null) {
            MultiMapValueEnumeration.invalidateEnumeration(this.cachedValueEnumeration);
            this.cachedValueEnumeration = null;
        }

        List list1 = (List) this.map.get(key);
        if (list1 == null) {
            list1 = this.createList(Math.max(this.initialListCapacity, collection1.size()));
            this.map.put(key, list1);
        }

        if (bl) {
            list1.addAll(0, collection1);
        } else {
            list1.addAll(collection1);
        }
    }

    @Override
    public Enumeration allValues() {
        if (this.concurrent) {
            synchronized (this.map) {
                if (this.cachedValueEnumeration == null) {
                    this.cachedValueEnumeration = new MultiMapValueEnumeration(this, 87429089775110L, null);
                } else {
                    MultiMapValueEnumeration.resetEnumeration(this.cachedValueEnumeration);
                }
            }
        } else if (this.cachedValueEnumeration == null) {
            this.cachedValueEnumeration = new MultiMapValueEnumeration(this, 87429089775110L, null);
        } else {
            MultiMapValueEnumeration.resetEnumeration(this.cachedValueEnumeration);
        }

        return this.cachedValueEnumeration;
    }

    @Override
    public int getKeyCount() {
        return this.map.size();
    }

    @Override
    public boolean isEmpty() {
        return this.map.isEmpty();
    }

    @Override
    public Enumeration valuesOf(Object key) {
        List list1;
        return (list1 = (List) this.map.get(key)) == null ? null : Collections.enumeration(list1);
    }

    @Override
    public boolean containsKey(Object key) {
        return this.map.containsKey(key);
    }

    public ListMultimap(int ba, int bb, boolean concurrentArgument) {
        this(false, ba, 5, concurrentArgument);
    }

    public void appendValues(Object key, Collection collection1) {
        this.insertValues(key, collection1, false);
    }

    @Override
    public void addValue(Object key, Object object1) {
        if (this.cachedValueEnumeration != null) {
            MultiMapValueEnumeration.invalidateEnumeration(this.cachedValueEnumeration);
            this.cachedValueEnumeration = null;
        }

        List list1 = (List) this.map.get(key);
        if (list1 == null) {
            list1 = this.createList(this.initialListCapacity);
            this.map.put(key, list1);
        }

        list1.add(object1);
    }

    public ListMultimap(boolean concurrentArgument) {
        this(false, 75, 5, concurrentArgument);
    }

    @Override
    public Set keySet() {
        return this.map.keySet();
    }

    public void trimToSize() {
        Iterator iterator = this.map.values().iterator();

        while (iterator.hasNext()) {
            List list1 = (List) iterator.next();
            if (list1 instanceof Vector) {
                ((Vector) list1).trimToSize();
            } else if (list1 instanceof ArrayList) {
                ((ArrayList) list1).trimToSize();
            } else {
                try {
                    Method method1 = list1.getClass().getMethod(TRIM_TO_SIZE_METHOD);
                    if (method1 != null) {
                        method1.invoke(list1);
                    }
                } catch (NoSuchMethodException noSuchMethodException) {
                } catch (InvocationTargetException invocationTargetException) {
                } catch (IllegalAccessException illegalAccessException) {
                }
            }
        }
    }

    public ListMultimap(boolean identityKeys, int ba, int initialListCapacity, boolean concurrent) {
        this.initialListCapacity = initialListCapacity;
        this.identityKeys = identityKeys;
        this.concurrent = concurrent;
        int primeCapacity = ZkmUtils.getPrimeCapacity(ba);
        if (concurrent) {
            this.map = new ConcurrentHashMap(primeCapacity);
        } else if (identityKeys) {
            this.map = new IdentityHashMap(primeCapacity);
        } else {
            this.map = ZkmUtils.createHashMap(primeCapacity);
        }
    }

    public List createList(int ba) {
        return this.concurrent ? new Vector(ba) : new ArrayList(ba);
    }

    public ListMultimap(ListMultimap listMultimap1) {
        this.concurrent = listMultimap1.concurrent;
        this.identityKeys = listMultimap1.identityKeys;
        this.initialListCapacity = listMultimap1.initialListCapacity;
        int primeCapacity = ZkmUtils.getPrimeCapacity(listMultimap1.map.size());
        if (this.concurrent) {
            this.map = new ConcurrentHashMap(primeCapacity);
        } else if (this.identityKeys) {
            this.map = new IdentityHashMap(primeCapacity);
        } else {
            this.map = ZkmUtils.createHashMap(primeCapacity);
        }

        Iterator iterator = listMultimap1.map.entrySet().iterator();

        while (iterator.hasNext()) {
            Entry entry = (Entry) iterator.next();
            List list1 = (List) entry.getValue();
            AbstractList abstractList;
            if (this.concurrent) {
                abstractList = new Vector((Collection) entry.getValue());
            } else {
                abstractList = new ArrayList(list1);
            }

            this.map.put(entry.getKey(), abstractList);
        }
    }

    @Override
    public boolean containsValue(Object key, Object object1) {
        List list1 = (List) this.map.get(key);
        return (!(list1 == null) && (list1.contains(object1)));
    }

    @Override
    public Set entrySet() {
        return this.map.entrySet();
    }

    public ListMultimap(boolean bl, int ba) {
        this(true, ba, 5, false);
    }

    @Override
    public List getValues(Object key) {
        return (List) this.map.get(key);
    }

    @Override
    public int getValueCount() {
        int valueCount = 0;
        Iterator iterator = this.map.values().iterator();

        while (iterator.hasNext()) {
            List list1 = (List) iterator.next();
            valueCount += list1.size();
        }

        return valueCount;
    }

    @Override
    public boolean removeValue(Object key, Object object1) {
        if (this.cachedValueEnumeration != null) {
            MultiMapValueEnumeration.invalidateEnumeration(this.cachedValueEnumeration);
            this.cachedValueEnumeration = null;
        }

        List list1 = (List) this.map.get(key);
        if (list1 == null) {
            return false;
        }

        boolean bl = false;
        Iterator iterator = list1.iterator();

        while (iterator.hasNext()) {
            if (iterator.next().equals(object1)) {
                iterator.remove();
                bl = true;
            }
        }

        if (list1.size() == 0) {
            this.map.remove(key);
        }

        return bl;
    }

    public ListMultimap(int ba, boolean concurrentArgument) {
        this(false, ba, 5, concurrentArgument);
    }
}

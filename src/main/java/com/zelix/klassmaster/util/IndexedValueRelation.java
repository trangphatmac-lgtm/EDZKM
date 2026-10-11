package com.zelix.klassmaster.util;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

public class IndexedValueRelation implements SetValuedMap {
    public static IntegerCache integerCache = IntegerCache.getInstance();
    public Object[] values;
    public Map valueBitsByKey;
    public Map indexByValue;
    private int valueCount;
    public final boolean ownsValueArray;

    @Override
    public Set keySet() {
        return ZkmUtils.createHashSetFrom(this.valueBitsByKey.keySet());
    }

    @Override
    public void clear() {
        this.valueBitsByKey.clear();
    }

    @Override
    public boolean containsKey(Object key) {
        return this.valueBitsByKey.containsKey(key);
    }

    @Override
    public boolean containsValue(Object key, Object keyArgument) {
        Integer integer = (Integer) this.indexByValue.get(keyArgument);
        if (integer == null) {
            throw new IllegalArgumentException("Value '" + keyArgument + "' doesn't exist in value array (C)");
        }

        BitSet bitSet = (BitSet) this.valueBitsByKey.get(key);
        return (!(bitSet == null) && (bitSet.get(integer)));
    }

    @Override
    public List getValueList(Object key) {
        BitSet bitSet = (BitSet) this.valueBitsByKey.get(key);
        if (bitSet == null) {
            return null;
        }

        ArrayList arrayList = new ArrayList(bitSet.cardinality());
        this.collectValues(bitSet, arrayList);
        return arrayList;
    }

    public Collection collectValues(BitSet bitSet, Collection collection1) {
        int ba = bitSet.cardinality();
        int bb = 0;

        for (int valueIndex = 0; valueIndex < this.valueCount; valueIndex++) {
            if (bitSet.get(valueIndex)) {
                collection1.add(this.values[valueIndex]);
                bb++;
            }

            if (bb >= ba) {
                break;
            }
        }

        return collection1;
    }

    @Override
    public boolean removeValue(Object key, Object keyArgument) {
        Integer integer = (Integer) this.indexByValue.get(keyArgument);
        if (integer == null) {
            throw new IllegalArgumentException("Value '" + keyArgument + "' doesn't exist in value array (B)");
        }

        BitSet bitSet = (BitSet) this.valueBitsByKey.get(key);
        if (bitSet == null) {
            return false;
        }

        int integerValue = integer;
        boolean bl = bitSet.get(integerValue);
        bitSet.clear(integerValue);
        if (bitSet.cardinality() == 0) {
            this.valueBitsByKey.remove(key);
        }

        return bl;
    }

    @Override
    public Set removeKey(Object key) {
        BitSet bitSet = (BitSet) this.valueBitsByKey.remove(key);
        if (bitSet == null) {
            return null;
        }

        HashSet hashSet = ZkmUtils.createHashSet(ZkmUtils.getPrimeCapacity(bitSet.cardinality()));
        this.collectValues(bitSet, hashSet);
        return hashSet;
    }

    public IndexedValueRelation(IndexedValueRelation indexedValueRelation1, int ba) {
        this(indexedValueRelation1.values, ba, false);
        this.mergeFrom(indexedValueRelation1);
    }

    @Override
    public void retainValues(Object[] valuesArgument) {
        int lengthValue = valuesArgument.length;
        HashMap hashMap = ZkmUtils.createHashMap(ZkmUtils.getPrimeCapacity(lengthValue));

        for (int integerValue = 0; integerValue < lengthValue; integerValue++) {
            Object key = valuesArgument[integerValue];
            hashMap.put(key, integerCache.valueOf(integerValue));
        }

        HashMap hashMap1 = ZkmUtils.createHashMap(ZkmUtils.getPrimeCapacity(this.valueBitsByKey.size()));
        int lengthValue2 = this.values.length;
        Iterator iterator = this.valueBitsByKey.entrySet().iterator();

        while (iterator.hasNext()) {
            Entry entry = (Entry) iterator.next();
            Object keyValue = entry.getKey();
            BitSet bitSet = (BitSet) entry.getValue();
            BitSet bitSet1 = new BitSet(valuesArgument.length);
            hashMap1.put(keyValue, bitSet1);

            for (int valueIndex = 0; valueIndex < lengthValue2; valueIndex++) {
                if (bitSet.get(valueIndex)) {
                    Object valueValue = this.values[valueIndex];
                    Integer integer = (Integer) hashMap.get(valueValue);
                    if (integer != null) {
                        bitSet1.set(integer);
                    }
                }
            }
        }

        if (this.ownsValueArray) {
            this.values = new Object[lengthValue];
            System.arraycopy(valuesArgument, 0, this.values, 0, lengthValue);
            this.valueBitsByKey = hashMap1;
        } else {
            this.values = valuesArgument;
            this.valueBitsByKey = hashMap1;
        }

        this.indexByValue = hashMap;
        this.valueCount = this.values.length;
    }

    @Override
    public int getKeyCount() {
        return this.valueBitsByKey.size();
    }

    public void mergeFrom(IndexedValueRelation indexedValueRelation1) {
        if (indexedValueRelation1.values != this.values) {
            throw new IllegalArgumentException(
                    "Must be based upon the exact same array. : " + System.identityHashCode(this.values) + "!=" + System.identityHashCode(indexedValueRelation1.values)
            );
        }

        Iterator iterator = indexedValueRelation1.valueBitsByKey.entrySet().iterator();

        while (iterator.hasNext()) {
            Entry entry = (Entry) iterator.next();
            Object key = entry.getKey();
            BitSet bitSet = (BitSet) entry.getValue();
            BitSet bitSet1 = (BitSet) this.valueBitsByKey.get(key);
            if (bitSet1 == null) {
                this.valueBitsByKey.put(key, (BitSet) bitSet.clone());
            } else {
                bitSet1.or(bitSet);
            }
        }
    }

    @Override
    public Set getValues(Object key) {
        BitSet bitSet = (BitSet) this.valueBitsByKey.get(key);
        if (bitSet == null) {
            return null;
        }

        HashSet hashSet = ZkmUtils.createHashSet(ZkmUtils.getPrimeCapacity(bitSet.cardinality()));
        this.collectValues(bitSet, hashSet);
        return hashSet;
    }

    public IndexedValueRelation(Object[] valuesArgument, int ba) {
        this(valuesArgument, ba, true);
    }

    @Override
    public boolean isEmpty() {
        return this.valueBitsByKey.isEmpty();
    }

    @Override
    public Enumeration keys() {
        return Collections.enumeration(this.valueBitsByKey.keySet());
    }

    public IndexedValueRelation(Object[] valuesArgument, int ba, boolean ownsValueArray) {
        if (ownsValueArray) {
            this.values = new Object[valuesArgument.length];
            System.arraycopy(valuesArgument, 0, this.values, 0, valuesArgument.length);
        } else {
            this.values = valuesArgument;
        }

        this.valueBitsByKey = ZkmUtils.createHashMap(ZkmUtils.getPrimeCapacity(ba));
        this.indexByValue = ZkmUtils.createHashMap(ZkmUtils.getPrimeCapacity(this.values.length));
        this.valueCount = this.values.length;

        for (int integerValue = 0; integerValue < this.valueCount; integerValue++) {
            this.indexByValue.put(this.values[integerValue], integerCache.valueOf(integerValue));
        }

        this.ownsValueArray = ownsValueArray;
    }

    @Override
    public boolean addValue(Object key, Object keyArgument) {
        Integer integer = (Integer) this.indexByValue.get(keyArgument);
        if (integer == null) {
            throw new IllegalArgumentException("Value '" + keyArgument + "' doesn't exist in value array (A)");
        }

        BitSet bitSet = (BitSet) this.valueBitsByKey.get(key);
        if (bitSet == null) {
            bitSet = new BitSet(this.valueCount);
            this.valueBitsByKey.put(key, bitSet);
        }

        int integerValue = integer;
        boolean bl = bitSet.get(integerValue);
        bitSet.set(integerValue);
        return bl;
    }
}

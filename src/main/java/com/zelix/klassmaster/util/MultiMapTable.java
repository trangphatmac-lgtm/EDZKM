package com.zelix.klassmaster.util;

import java.util.Enumeration;
import java.util.Map;
import java.util.Set;

public class MultiMapTable {
    public Map map;

    public int getKeyCount() {
        return this.map.size();
    }

    public void clear() {
        this.map.clear();
    }

    public PairMultiMap getPairMultiMap(Object key) {
        return (PairMultiMap) this.map.get(key);
    }

    public Set entrySet() {
        return this.map.entrySet();
    }

    public void addEntry(Object key, Object keyArgument, Object first, Object second) {
        PairMultiMap pairMultiMap = (PairMultiMap) this.map.get(key);
        if (pairMultiMap == null) {
            pairMultiMap = new PairMultiMap();
            this.map.put(key, pairMultiMap);
        }

        pairMultiMap.addPair(keyArgument, first, second);
    }

    public Enumeration keys() {
        long ba = 137000840253142L;
        ba = 91899041683623L ^ ba;
        int bb = (int) ((ba ^ 3561773033513L) >>> 48);
        Set set2 = this.map.keySet();
        char bc = (char) bb;
        Set set1 = set2;
        return new CollectionSnapshotEnumeration(set1);
    }

    public MultiMapTable(int ba) {
        this.map = ZkmUtils.createHashMap(ba);
    }
}

package com.zelix.klassmaster.util;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

public class EquivalenceGroups {
    public final Map groupByElement = ZkmUtils.createHashMap();

    public Set getGroups() {
        HashSet hashSet = ZkmUtils.createHashSet();
        Iterator iterator = this.groupByElement.entrySet().iterator();

        while (iterator.hasNext()) {
            Entry entry = (Entry) iterator.next();
            hashSet.add(entry.getValue());
        }

        return hashSet;
    }

    public Set getElements() {
        return Collections.unmodifiableSet(this.groupByElement.keySet());
    }

    public void makeEquivalent(Object key, Object keyArgument) {
        EquivalenceGroup equivalenceGroup = (EquivalenceGroup) this.groupByElement.get(key);
        EquivalenceGroup equivalenceGroup1 = (EquivalenceGroup) this.groupByElement.get(keyArgument);
        if (equivalenceGroup == null && equivalenceGroup1 == null) {
            EquivalenceGroup equivalenceGroup2 = new EquivalenceGroup();
            equivalenceGroup2.add(key);
            equivalenceGroup2.add(keyArgument);
            this.groupByElement.put(key, equivalenceGroup2);
            this.groupByElement.put(keyArgument, equivalenceGroup2);
        } else if (equivalenceGroup != null && equivalenceGroup1 != null) {
            for (Object keyValue : equivalenceGroup1) {
                this.groupByElement.remove(keyValue);
                this.groupByElement.put(keyValue, equivalenceGroup);
            }

            equivalenceGroup.addAll(equivalenceGroup1);
        } else if (equivalenceGroup != null) {
            equivalenceGroup.add(keyArgument);
            this.groupByElement.put(keyArgument, equivalenceGroup);
        } else {
            equivalenceGroup1.add(key);
            this.groupByElement.put(key, equivalenceGroup1);
        }
    }

    public boolean containsElement(Object key) {
        return this.groupByElement.containsKey(key);
    }

    public Set getEntries() {
        return Collections.unmodifiableSet(this.groupByElement.entrySet());
    }

    public EquivalenceGroup getGroup(Object key) {
        return (EquivalenceGroup) this.groupByElement.get(key);
    }
}

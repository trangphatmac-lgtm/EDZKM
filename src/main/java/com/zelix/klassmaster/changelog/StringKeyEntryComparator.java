package com.zelix.klassmaster.changelog;

import java.util.Comparator;
import java.util.Map.Entry;

public class StringKeyEntryComparator implements Comparator {
    public final ChangeLogMapping changeLogMapping;

    @Override
    public int compare(Object object, Object object1) {
        return this.compareEntries((Entry) object, (Entry) object1);
    }

    public int compareEntries(Entry entry, Entry entry1) {
        String key = (String) entry.getKey();
        String keyValue = (String) entry1.getKey();
        return key.compareTo(keyValue);
    }

    public StringKeyEntryComparator(ChangeLogMapping changeLogMapping1) {
        this.changeLogMapping = changeLogMapping1;
    }
}

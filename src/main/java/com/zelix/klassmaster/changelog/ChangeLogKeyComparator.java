package com.zelix.klassmaster.changelog;

import com.zelix.klassmaster.obfuscator.flow.OpaquePredicateField;
import com.zelix.klassmaster.util.ObjectPair;

import java.util.Comparator;

public class ChangeLogKeyComparator implements Comparator {
    public final ChangeLogWriter writer;

    @Override
    public int compare(Object object, Object object1) {
        return this.compareFlowEntries((ObjectPair) object, (ObjectPair) object1);
    }

    public int compareFlowEntries(ObjectPair objectPair, ObjectPair objectPair1) {
        String ownerClassName = ((OpaquePredicateField) objectPair.getSecond()).getOwnerClassName();
        String ownerClassNameValue = ((OpaquePredicateField) objectPair1.getSecond()).getOwnerClassName();
        int ba = ownerClassName.compareTo(ownerClassNameValue);
        if (ba == 0) {
            String first = (String) objectPair.getFirst();
            String firstValue = (String) objectPair1.getFirst();
            return first.compareTo(firstValue);
        } else {
            return ba;
        }
    }

    public ChangeLogKeyComparator(ChangeLogWriter changeLogWriter1) {
        this.writer = changeLogWriter1;
    }
}

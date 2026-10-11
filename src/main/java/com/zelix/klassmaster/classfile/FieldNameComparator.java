package com.zelix.klassmaster.classfile;

import java.util.Comparator;

public class FieldNameComparator implements Comparator {
    @Override
    public int compare(Object object, Object object1) {
        return this.compareByName((FieldInfo) object, (FieldInfo) object1);
    }

    public int compareByName(FieldInfo fieldInfo, FieldInfo fieldInfo1) {
        String sourceName = fieldInfo.getSourceName();
        String sourceNameValue = fieldInfo1.getSourceName();
        return sourceName.compareTo(sourceNameValue);
    }
}

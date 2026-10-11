package com.zelix.klassmaster.util;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

public class NamedHashSet extends HashSet implements NamedSet {
    public String name;

    @Override
    public String getSetName() {
        return this.name;
    }

    public NamedHashSet(String nameArgument, Collection collection1) {
        super(collection1);
        this.name = nameArgument;
    }

    public NamedHashSet(String nameArgument, Map map1) {
        this(nameArgument, map1.keySet());
    }

    public NamedHashSet() {
    }

    public NamedHashSet(Collection collection1) {
        super(collection1);
    }

    @Override
    public Object clone() {
        return super.clone();
    }
}

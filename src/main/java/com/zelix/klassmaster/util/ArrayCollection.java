package com.zelix.klassmaster.util;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;

public class ArrayCollection implements Collection {
    public Object[] elements;

    @Override
    public boolean retainAll(Collection collection1) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(Collection collection1) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean contains(Object object) {
        int elementIndex = 0;
        int bb = 0;

        for (Object[] elementsValue = this.elements; bb < elementsValue.length; elementsValue = this.elements) {
            if (this.elements[elementIndex].equals(object)) {
                return true;
            }

            bb = ++elementIndex;
        }

        return false;
    }

    @Override
    public boolean addAll(Collection collection1) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isEmpty() {
        return this.elements.length == 0;
    }

    @Override
    public boolean containsAll(Collection collection1) {
        Iterator iterator = collection1.iterator();

        while (iterator.hasNext()) {
            if (!this.contains(iterator.next())) {
                return false;
            }
        }

        return true;
    }

    @Override
    public Iterator iterator() {
        return new ArrayCollectionIterator(this);
    }

    @Override
    public Object[] toArray() {
        return ZkmUtils.copyArray(this.elements);
    }

    @Override
    public boolean remove(Object object) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int size() {
        return this.elements.length;
    }

    @Override
    public boolean add(Object object) {
        throw new UnsupportedOperationException();
    }

    public static Object[] getElements(ArrayCollection arrayCollection) {
        return arrayCollection.elements;
    }

    public ArrayCollection(Object[] elementsArgument) {
        if (elementsArgument == null) {
            throw new IllegalArgumentException();
        }

        this.elements = elementsArgument;
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object[] toArray(Object[] destinationArray) {
        if (destinationArray.length < this.elements.length) {
            destinationArray = (Object[]) Array.newInstance(destinationArray.getClass().getComponentType(), this.elements.length);
        }

        System.arraycopy(this.elements, 0, destinationArray, 0, this.elements.length);
        if (destinationArray.length > this.elements.length) {
            destinationArray[this.elements.length] = null;
        }

        return destinationArray;
    }
}

package com.zelix.klassmaster.util;

import com.zelix.klassmaster.exceptions.ZkmRuntimeException;

import java.util.Collection;
import java.util.Enumeration;
import java.util.Set;

public class UniqueWorkQueue {
    private int head = 0;
    private int tail = 0;
    private int count = 0;
    private Object[] buffer;
    private Set members;

    public boolean contains(Object object) {
        return this.members.contains(object);
    }

    public boolean isEmpty() {
        return this.count == 0;
    }

    public boolean enqueue(Object bufferElement) {
        if (!this.members.add(bufferElement)) {
            return false;
        }

        int lengthValue = this.buffer.length;
        if (this.count == lengthValue) {
            this.grow();
            lengthValue = this.buffer.length;
        }

        this.buffer[this.tail] = bufferElement;
        this.tail = (this.tail + 1) % lengthValue;
        this.count++;
        return true;
    }

    public UniqueWorkQueue() {
        this(10);
    }

    public void enqueueAllElements(Enumeration enumeration) {
        while (enumeration.hasMoreElements()) {
            this.enqueue(enumeration.nextElement());
        }
    }

    public void grow() {
        int lengthValue = this.buffer.length;
        Object[] bufferValue = new Object[lengthValue * 2];

        for (int bufferValueElementIndex = 0; bufferValueElementIndex < this.count; bufferValueElementIndex++) {
            int bufferElementIndex = (this.head + bufferValueElementIndex) % lengthValue;
            bufferValue[bufferValueElementIndex] = this.buffer[bufferElementIndex];
        }

        this.head = 0;
        this.tail = this.count;
        this.buffer = bufferValue;
    }

    @Override
    public int hashCode() {
        int ba = 1;
        int lengthValue = this.buffer.length;

        for (int i = 0; i < this.count; i++) {
            int bufferElementIndex = (this.head + i) % lengthValue;
            ba = 31 * ba + this.buffer[bufferElementIndex].hashCode();
        }

        return ba;
    }

    public void enqueueAll(Collection collection1) {
        for (Object bufferElement : collection1) {
            this.enqueue(bufferElement);
        }
    }

    public UniqueWorkQueue(int ba) {
        if (ba <= 1) {
            throw new IllegalArgumentException("Queue size must be greater than 1 : " + ba);
        }

        this.buffer = new Object[ba];
        this.members = ZkmUtils.createHashSet(ZkmUtils.getPrimeCapacity(ba));
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        if (other instanceof UniqueWorkQueue) {
            UniqueWorkQueue uniqueWorkQueue1 = (UniqueWorkQueue) other;
            if (this.count != uniqueWorkQueue1.count) {
                return false;
            }

            int lengthValue = this.buffer.length;
            int lengthValue2 = uniqueWorkQueue1.buffer.length;

            for (int i = 0; i < this.count; i++) {
                int bufferElementIndex = (this.head + i) % lengthValue;
                int bufferElementIndexValue = (uniqueWorkQueue1.head + i) % lengthValue2;
                if (!this.buffer[bufferElementIndex].equals(uniqueWorkQueue1.buffer[bufferElementIndexValue])) {
                    return false;
                }
            }

            return true;
        } else {
            return false;
        }
    }

    public Object dequeue() {
        if (this.count == 0) {
            throw new ZkmRuntimeException("Empty Queue");
        }

        int lengthValue = this.buffer.length;
        Object bufferElement = this.buffer[this.head];
        this.head = (this.head + 1) % lengthValue;
        this.count--;
        this.members.remove(bufferElement);
        return bufferElement;
    }

    public int size() {
        return this.count;
    }
}

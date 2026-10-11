package com.zelix.klassmaster.classfile.constpool;

import com.zelix.klassmaster.exceptions.ZkmException;
import com.zelix.klassmaster.util.NodeVisitor;
import com.zelix.klassmaster.util.ObservableModel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class NumericConstantEntry extends ConstantPoolEntry implements LoadableConstant, ObservableModel {
    public List observers;

    @Override
    public void notifyObservers(Object object, Object object1, Object object2) throws ZkmException, IOException {
        int observersSize;
        NodeVisitor[] nodeVisitors;
        synchronized (this) {
            if (this.observers == null) {
                return;
            }

            observersSize = this.observers.size();
            nodeVisitors = new NodeVisitor[observersSize];
            nodeVisitors = ((com.zelix.klassmaster.util.NodeVisitor[]) (this.observers.toArray(nodeVisitors)));
        }

        for (int nodeVisitorIndex = observersSize - 1; nodeVisitorIndex >= 0; nodeVisitorIndex += -1) {
            if (nodeVisitors[nodeVisitorIndex] != null) {
                nodeVisitors[nodeVisitorIndex].handleObservedChange(this, object, object1, object2);
            }
        }
    }

    @Override
    public final void notifyObserverWithoutArgs(NodeVisitor nodeVisitor) throws ZkmException, IOException {
        this.notifyObserver(nodeVisitor);
    }

    @Override
    public final void setChangedAndNotify() throws ZkmException, IOException {
        this.setChanged();
        this.notifyObservers();
    }

    @Override
    public synchronized void removeObserver(Object object) {
        if (this.observers != null) {
            this.observers.remove(object);
        }
    }

    @Override
    public void notifyObservers() throws ZkmException, IOException {
        this.notifyObservers(null, null, null);
    }

    @Override
    public final void notifyObserver(NodeVisitor nodeVisitor) throws ZkmException, IOException {
        nodeVisitor.handleObservedChange(this, null, null, null);
    }

    public NumericConstantEntry(int index, AbstractConstantPool abstractConstantPool) {
        super(index, abstractConstantPool);
    }

    @Override
    public final synchronized void addObserver(NodeVisitor nodeVisitor) {
        if (this.observers == null) {
            this.observers = new ArrayList(2);
        }

        if (!this.observers.contains(nodeVisitor)) {
            this.observers.add(nodeVisitor);
        }
    }

    @Override
    public synchronized void setChanged() {
    }
}

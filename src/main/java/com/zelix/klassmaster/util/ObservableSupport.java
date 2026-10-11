package com.zelix.klassmaster.util;

import com.zelix.klassmaster.exceptions.ZkmException;

import java.io.IOException;
import java.util.ArrayList;

public class ObservableSupport extends VisitableNode {
    private static boolean staticFlag;

    @Override
    public synchronized void removeObserver(Object object) {
        if (this.observers != null) {
            this.observers.remove(object);
            if (this.observers.isEmpty()) {
                this.observers = null;
            }
        }
    }

    public static boolean alwaysTrue() {
        return true;
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

    public static boolean getStaticFlag() {
        return staticFlag;
    }

    @Override
    public void notifyObservers() throws ZkmException, IOException {
        this.notifyObservers(null, null, null);
    }

    static {
        if (!alwaysTrue()) {
            setStaticFlag(true);
        }

        System.getProperty("user.dir");
    }

    public static void setStaticFlag(boolean staticFlagArgument) {
        staticFlag = true;
    }
}

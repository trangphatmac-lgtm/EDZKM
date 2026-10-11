package com.zelix.klassmaster.util;

import java.util.Iterator;
import java.util.Map;

public class ListenerRegistry {
    public final TwoKeyMap observersByKey;
    private static final String DEFAULT_KEY = "DefaultKey__";

    public void removeAllObservers() {
        Iterator iterator = new EnumerationBackedList(this.observersByKey.keys()).iterator();

        while (iterator.hasNext()) {
            this.removeObserversForKey(iterator.next());
        }
    }

    public ListenerRegistry() {
        this(false);
    }

    public NodeVisitor removeObserver(ObservableModel observableModel1, NodeVisitor nodeVisitor, Object key) {
        observableModel1.removeObserver(nodeVisitor);
        return (NodeVisitor) this.observersByKey.removeValue(key, observableModel1);
    }

    public void removeObserversForKey(Object key) {
        Map map1 = this.observersByKey.getInnerMap(key);
        if (map1 != null) {
            Iterator iterator = ZkmUtils.createHashSetFrom(map1.keySet()).iterator();

            while (iterator.hasNext()) {
                ObservableModel observableModel1 = (ObservableModel) iterator.next();
                NodeVisitor nodeVisitor = (NodeVisitor) map1.get(observableModel1);
                this.removeObserver(observableModel1, nodeVisitor, key);
            }
        }
    }

    public ListenerRegistry(boolean concurrent) {
        this.observersByKey = new TwoKeyMap(concurrent);
    }

    public void addDefaultObserver(VisitableNode visitableNode, NodeVisitor nodeVisitor) {
        this.addObserver(visitableNode, nodeVisitor, DEFAULT_KEY);
    }

    public void addObserver(ObservableModel observableModel1, NodeVisitor nodeVisitor, Object key) {
        observableModel1.addObserver(nodeVisitor);
        this.observersByKey.putValue(key, observableModel1, nodeVisitor);
    }
}

package com.zelix.klassmaster.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;

public class ObjectTreeNode {
    public final GenericTree tree;
    private ArrayList children;
    private Object value;
    private ObjectTreeNode parent;

    public static ObjectTreeNode accessParent(ObjectTreeNode objectTreeNode) {
        return objectTreeNode.parent;
    }

    public static boolean removeChildOf(ObjectTreeNode objectTreeNode, ObjectTreeNode objectTreeNode1) {
        return objectTreeNode.removeChild(objectTreeNode1);
    }

    public ObjectTreeNode(GenericTree genericTree, Object valueArgument) {
        this(genericTree, valueArgument, null);
    }

    public static ObjectTreeNode addChildTo(ObjectTreeNode objectTreeNode, Object valueArgument) {
        return objectTreeNode.addChild(valueArgument);
    }

    private ObjectTreeNode addChild(Object valueArgument) {
        ObjectTreeNode objectTreeNode1 = new ObjectTreeNode(this.tree, valueArgument, this);
        this.children.add(objectTreeNode1);
        return objectTreeNode1;
    }

    public Enumeration childNodes() {
        return Collections.enumeration(this.children);
    }

    public boolean removeChild(Object object) {
        return this.children.remove(object);
    }

    private ObjectTreeNode(GenericTree genericTree, Object valueArgument, ObjectTreeNode objectTreeNode1) {
        this.tree = genericTree;
        this.children = new ArrayList();
        this.value = valueArgument;
        this.parent = objectTreeNode1;
    }

    public static ObjectTreeNode getParentOf(ObjectTreeNode objectTreeNode) {
        return objectTreeNode.getParent();
    }

    public ObjectTreeNode getParent() {
        return this.parent;
    }

    public static ArrayList getChildrenOf(ObjectTreeNode objectTreeNode) {
        return objectTreeNode.children;
    }

    public Object getValue() {
        return this.value;
    }
}

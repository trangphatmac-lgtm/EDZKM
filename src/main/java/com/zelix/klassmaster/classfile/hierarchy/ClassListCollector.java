package com.zelix.klassmaster.classfile.hierarchy;

import java.util.ArrayList;

public class ClassListCollector extends HierarchyNodeVisitor {
    @Override
    public void visitNode(ClassHierarchyNode classHierarchyNode, int depth, ArrayList arrayList) {
        classHierarchyNode.setDepth(depth);
        arrayList.add(classHierarchyNode);
    }
}

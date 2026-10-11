package com.zelix.klassmaster.proguard;

import com.zelix.klassmaster.proguard.mapping.parser.ProGuardMappingNode;

import java.util.ArrayList;
import java.util.List;

public class ProGuardMappingTreeState {
    public boolean nodeCreated;
    public List nodes = new ArrayList();
    public List marks = new ArrayList();
    public int stackPointer = 0;
    public int scopeMark = 0;

    public void reset() {
        this.nodes.clear();
        this.marks.clear();
        this.stackPointer = 0;
        this.scopeMark = 0;
    }

    public ProGuardMappingNode popNode() {
        if (--this.stackPointer < this.scopeMark) {
            this.scopeMark = (Integer) this.marks.remove(this.marks.size() - 1);
        }

        return (ProGuardMappingNode) this.nodes.remove(this.nodes.size() - 1);
    }

    public int nodeArity() {
        return this.stackPointer - this.scopeMark;
    }

    public void openNodeScope(ProGuardMappingNode proGuardMappingNode) {
        this.marks.add(this.scopeMark);
        this.scopeMark = this.stackPointer;
        proGuardMappingNode.jjtOpen();
    }

    public void pushNode(Object object) {
        this.nodes.add(object);
        this.stackPointer++;
    }

    public void clearNodeScope() {
        for (int stackPointerValue = this.stackPointer; stackPointerValue > this.scopeMark; stackPointerValue = this.stackPointer) {
            this.popNode();
        }

        this.scopeMark = (Integer) this.marks.remove(this.marks.size() - 1);
    }

    public void closeNodeScope(ProGuardMappingNode parentNode) {
        int remainingChildren = this.nodeArity();
        this.scopeMark = (Integer) this.marks.remove(this.marks.size() - 1);

        while (remainingChildren > 0) {
            remainingChildren--;
            ProGuardMappingNode childNode = this.popNode();
            childNode.jjtSetParent(parentNode);
            parentNode.jjtAddChild(childNode, remainingChildren);
        }

        parentNode.jjtClose();
        this.pushNode(parentNode);
        this.nodeCreated = true;
    }
}

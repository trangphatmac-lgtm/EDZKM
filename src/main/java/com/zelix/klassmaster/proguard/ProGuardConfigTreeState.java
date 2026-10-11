package com.zelix.klassmaster.proguard;

import com.zelix.klassmaster.proguard.config.parser.ProGuardConfigNode;

import java.util.ArrayList;
import java.util.List;

public class ProGuardConfigTreeState {
    public boolean nodeCreated;
    public List nodes = new ArrayList();
    public List marks = new ArrayList();
    public int stackPointer = 0;
    public int scopeMark = 0;

    public void closeNodeScope(ProGuardConfigNode parentNode) {
        int remainingChildren = this.nodeArity();
        this.scopeMark = (Integer) this.marks.remove(this.marks.size() - 1);

        while (remainingChildren > 0) {
            remainingChildren--;
            ProGuardConfigNode childNode = this.popNode();
            childNode.jjtSetParent(parentNode);
            parentNode.jjtAddChild(childNode, remainingChildren);
        }

        parentNode.jjtOpen();
        this.pushNode(parentNode);
        this.nodeCreated = true;
    }

    public void clearNodeScope() {
        for (int stackPointerValue = this.stackPointer; stackPointerValue > this.scopeMark; stackPointerValue = this.stackPointer) {
            this.popNode();
        }

        this.scopeMark = (Integer) this.marks.remove(this.marks.size() - 1);
    }

    public ProGuardConfigNode popNode() {
        if (--this.stackPointer < this.scopeMark) {
            this.scopeMark = (Integer) this.marks.remove(this.marks.size() - 1);
        }

        return (ProGuardConfigNode) this.nodes.remove(this.nodes.size() - 1);
    }

    public int nodeArity() {
        return this.stackPointer - this.scopeMark;
    }

    public void openNodeScope(ProGuardConfigNode proGuardConfigNode) {
        this.marks.add(this.scopeMark);
        this.scopeMark = this.stackPointer;
        proGuardConfigNode.jjtClose();
    }

    public void pushNode(Object object) {
        this.nodes.add(object);
        this.stackPointer++;
    }
}

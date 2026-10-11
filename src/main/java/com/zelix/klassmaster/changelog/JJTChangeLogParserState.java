package com.zelix.klassmaster.changelog;

import com.zelix.klassmaster.changelog.parser.ChangeLogNode;

import java.util.ArrayList;
import java.util.List;

public class JJTChangeLogParserState {
    private boolean nodeCreated;
    private List nodes = new ArrayList();
    private List marks = new ArrayList();
    private int stackPointer = 0;
    private int scopeMark = 0;

    public void closeNodeScope(ChangeLogNode parentNode) {
        int remainingChildren = this.nodeArity();
        this.scopeMark = (Integer) this.marks.remove(this.marks.size() - 1);

        while (remainingChildren > 0) {
            remainingChildren--;
            ChangeLogNode childNode = this.popNode();
            childNode.jjtSetParent(parentNode);
            parentNode.jjtAddChild(childNode, remainingChildren);
        }

        parentNode.jjtOpen();
        this.pushNode(parentNode);
        this.nodeCreated = true;
    }

    public void pushNode(ChangeLogNode changeLogNode) {
        this.nodes.add(changeLogNode);
        this.stackPointer++;
    }

    public ChangeLogNode popNode() {
        if (--this.stackPointer < this.scopeMark) {
            this.scopeMark = (Integer) this.marks.remove(this.marks.size() - 1);
        }

        return (ChangeLogNode) this.nodes.remove(this.nodes.size() - 1);
    }

    public int nodeArity() {
        return this.stackPointer - this.scopeMark;
    }

    public void clearNodeScope() {
        while (this.stackPointer > this.scopeMark) {
            this.popNode();
        }

        this.scopeMark = (Integer) this.marks.remove(this.marks.size() - 1);
    }

    public void openNodeScope(ChangeLogNode changeLogNode) {
        this.marks.add(this.scopeMark);
        this.scopeMark = this.stackPointer;
        changeLogNode.jjtClose();
    }

    public void reset() {
        this.nodes.clear();
        this.marks.clear();
        this.stackPointer = 0;
        this.scopeMark = 0;
    }
}

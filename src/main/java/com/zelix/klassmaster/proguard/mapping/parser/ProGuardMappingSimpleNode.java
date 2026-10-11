package com.zelix.klassmaster.proguard.mapping.parser;

import com.zelix.klassmaster.proguard.ProGuardMappingTranslator;

public abstract class ProGuardMappingSimpleNode implements ProGuardMappingNode {
    public ProGuardMappingNode parent;
    public ProGuardMappingNode[] children;
    private static int[] flowState;
    public int id;

    @Override
    public void translate(Object object, ProGuardMappingTranslator proGuardMappingTranslator) {
        int ba = this.jjtGetNumChildren();

        for (int childrenElementIndex = 0; childrenElementIndex < ba; childrenElementIndex++) {
            this.jjtGetChild(childrenElementIndex).translate(this, proGuardMappingTranslator);
        }
    }

    @Override
    public ProGuardMappingNode jjtGetChild(int childrenElementIndex) {
        return this.children[childrenElementIndex];
    }

    @Override
    public void jjtOpen() {
    }

    @Override
    public void jjtSetParent(ProGuardMappingNode proGuardMappingNode) {
        this.parent = proGuardMappingNode;
    }

    @Override
    public void jjtAddChild(ProGuardMappingNode proGuardMappingNode, int proGuardMappingNodes1ElementIndex) {
        ProGuardMappingNode[] proGuardMappingNodes1;
        if (this.children == null) {
            this.children = new ProGuardMappingNode[proGuardMappingNodes1ElementIndex + 1];
            proGuardMappingNodes1 = this.children;
        } else if (proGuardMappingNodes1ElementIndex >= this.children.length) {
            ProGuardMappingNode[] proGuardMappingNodes = new ProGuardMappingNode[proGuardMappingNodes1ElementIndex + 1];
            System.arraycopy(this.children, 0, proGuardMappingNodes, 0, this.children.length);
            this.children = proGuardMappingNodes;
            proGuardMappingNodes1 = this.children;
        } else {
            proGuardMappingNodes1 = this.children;
        }

        proGuardMappingNodes1[proGuardMappingNodes1ElementIndex] = proGuardMappingNode;
    }

    @Override
    public String toString() {
        return ProGuardMappingTreeConstants.jjtNodeName[this.id];
    }

    public static void setFlowState(int[] flowStateArgument) {
        flowState = flowStateArgument;
    }

    @Override
    public void jjtClose() {
    }

    @Override
    public int jjtGetNumChildren() {
        return this.children == null ? 0 : this.children.length;
    }

    public static int[] getFlowState() {
        return flowState;
    }

    public ProGuardMappingSimpleNode(int id) {
        this.id = id;
    }

    static {
        if (getFlowState() != null) {
            setFlowState(new int[3]);
        }
    }
}

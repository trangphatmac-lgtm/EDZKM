package com.zelix.klassmaster.changelog.parser.ast;

import com.zelix.klassmaster.changelog.AbstractChangeLog;
import com.zelix.klassmaster.changelog.parser.ChangeLogNode;
import com.zelix.klassmaster.changelog.parser.ChangeLogSimpleNode;
import com.zelix.klassmaster.exceptions.ZkmException;

import java.io.IOException;

public abstract class ChangeLogValueListNode extends ChangeLogSimpleNode {
    @Override
    public final void interpret(ChangeLogNode changeLogNode, int bc, int bd, int be, AbstractChangeLog abstractChangeLog) throws ZkmException, IOException {
        FlowObfuscationDataNode flowObfuscationDataNode = (FlowObfuscationDataNode) changeLogNode;
        int ba = this.jjtGetNumChildren();

        for (int childrenElementIndex = 0; childrenElementIndex < ba; childrenElementIndex++) {
            String value = ((ChangeLogValueNode) this.jjtGetChild(childrenElementIndex)).getValue();
            flowObfuscationDataNode.addValue(value);
        }
    }

    public ChangeLogValueListNode(int ba) {
        super(ba);
    }
}

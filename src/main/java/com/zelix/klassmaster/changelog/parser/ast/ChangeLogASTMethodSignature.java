package com.zelix.klassmaster.changelog.parser.ast;

import com.zelix.klassmaster.changelog.AbstractChangeLog;
import com.zelix.klassmaster.changelog.parser.ChangeLogNode;
import com.zelix.klassmaster.exceptions.ZkmException;

import java.io.IOException;

public class ChangeLogASTMethodSignature extends ChangeLogMethodSignatureNode {
    @Override
    public void interpret(ChangeLogNode changeLogNode, int ba, int bd, int be, AbstractChangeLog abstractChangeLog) throws ZkmException, IOException {
        int bb = this.jjtGetNumChildren();
        int childrenElementIndex = 0;

        while (childrenElementIndex < bb) {
            this.jjtGetChild(childrenElementIndex).interpret(this, 30872, 34067, 41973, abstractChangeLog);
            if (ba >= 0) {
                childrenElementIndex++;
            }
        }

        ((ChangeLogASTMethodNameChange) changeLogNode).setOldSignature(super.methodName, super.argumentTypes);
    }

    public ChangeLogASTMethodSignature() {
        super(26);
    }
}

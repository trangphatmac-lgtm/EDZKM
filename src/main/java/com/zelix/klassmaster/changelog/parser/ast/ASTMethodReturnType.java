package com.zelix.klassmaster.changelog.parser.ast;

import com.zelix.klassmaster.changelog.AbstractChangeLog;
import com.zelix.klassmaster.changelog.TypeNameSetter;
import com.zelix.klassmaster.changelog.parser.ChangeLogNode;
import com.zelix.klassmaster.changelog.parser.ChangeLogSimpleNode;
import com.zelix.klassmaster.exceptions.ZkmException;

import java.io.IOException;

public class ASTMethodReturnType extends ChangeLogSimpleNode implements TypeNameSetter {
    public String returnType;

    public ASTMethodReturnType() {
        super(29);
    }

    @Override
    public void interpret(ChangeLogNode changeLogNode, int bd, int ba, int be, AbstractChangeLog abstractChangeLog) throws ZkmException, IOException {
        int bb = this.jjtGetNumChildren();
        ChangeLogSimpleNode.getOpaqueStrings();
        int childrenElementIndex = 0;

        while (childrenElementIndex < bb) {
            if (ba > 0) {
                this.jjtGetChild(childrenElementIndex).interpret(this, 30872, 34067, 41973, abstractChangeLog);
                childrenElementIndex++;
            }
        }

        ((ChangeLogASTMethodNameChange) changeLogNode).setReturnType(this.returnType);
    }

    @Override
    public void setTypeName(Object typeName) {
        this.returnType = (String) typeName;
    }
}

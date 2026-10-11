package com.zelix.klassmaster.changelog.parser.ast;

import com.zelix.klassmaster.changelog.AbstractChangeLog;
import com.zelix.klassmaster.changelog.TypeNameSetter;
import com.zelix.klassmaster.changelog.parser.ChangeLogNode;
import com.zelix.klassmaster.changelog.parser.ChangeLogSimpleNode;
import com.zelix.klassmaster.exceptions.ZkmException;

import java.io.IOException;

public class ASTFieldType extends ChangeLogSimpleNode implements TypeNameSetter {
    public String typeName;

    @Override
    public void interpret(ChangeLogNode changeLogNode, int bd, int ba, int be, AbstractChangeLog abstractChangeLog) throws ZkmException, IOException {
        int bb = this.jjtGetNumChildren();
        int childrenElementIndex = 0;

        while (childrenElementIndex < bb) {
            this.jjtGetChild(childrenElementIndex).interpret(this, 30872, 34067, 41973, abstractChangeLog);
            if (ba > 0) {
                childrenElementIndex++;
            }
        }

        ((ChangeLogASTFieldNameChange) changeLogNode).setFieldType(this.typeName);
    }

    public ASTFieldType() {
        super(28);
    }

    @Override
    public void setTypeName(Object typeNameArgument) {
        this.typeName = (String) typeNameArgument;
    }
}

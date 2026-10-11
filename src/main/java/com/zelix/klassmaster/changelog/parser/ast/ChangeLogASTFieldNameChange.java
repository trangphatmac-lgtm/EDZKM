package com.zelix.klassmaster.changelog.parser.ast;

import com.zelix.klassmaster.changelog.AbstractChangeLog;
import com.zelix.klassmaster.changelog.parser.ChangeLogNode;
import com.zelix.klassmaster.exceptions.ZkmException;

import java.io.IOException;

public class ChangeLogASTFieldNameChange extends ChangeLogModifierNode {
    public String fieldType;
    public String oldFieldName;
    public String newFieldName;

    @Override
    public void interpret(ChangeLogNode changeLogNode, int bd, int be, int ba, AbstractChangeLog abstractChangeLog) throws ZkmException, IOException {
        if (abstractChangeLog.acceptsFieldChanges()) {
            int bb = this.jjtGetNumChildren();
            int childrenElementIndex = 0;

            while (childrenElementIndex < bb) {
                ChangeLogNode changeLogNode1 = this.jjtGetChild(childrenElementIndex);
                if (ba > 0) {
                    changeLogNode1.interpret(this, 30872, 34067, 41973, abstractChangeLog);
                    childrenElementIndex++;
                }
            }

            if (this.newFieldName == null) {
                this.newFieldName = this.oldFieldName;
            }

            ((ASTClassChange) changeLogNode).addFieldChange(this.newFieldName, this.oldFieldName, this.fieldType, this.getModifiers());
        }
    }

    public void setFieldType(String fieldTypeArgument) {
        this.fieldType = fieldTypeArgument;
    }

    public void setOldFieldName(String oldFieldNameArgument) {
        this.oldFieldName = oldFieldNameArgument;
    }

    public ChangeLogASTFieldNameChange() {
        super(14);
    }

    public void setNewFieldName(String newFieldNameArgument) {
        this.newFieldName = newFieldNameArgument;
    }
}

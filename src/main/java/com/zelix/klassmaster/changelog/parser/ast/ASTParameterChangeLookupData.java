package com.zelix.klassmaster.changelog.parser.ast;

import com.zelix.klassmaster.changelog.AbstractChangeLog;
import com.zelix.klassmaster.changelog.ChangeLogNameHolder;
import com.zelix.klassmaster.changelog.parser.ChangeLogNode;
import com.zelix.klassmaster.changelog.parser.ChangeLogSimpleNode;
import com.zelix.klassmaster.exceptions.ZkmException;

import java.io.IOException;

public class ASTParameterChangeLookupData extends ChangeLogSimpleNode implements ChangeLogNameHolder {
    public String parameterChangeLookupData;

    @Override
    public void setParsedName(Object parsedName) {
        this.parameterChangeLookupData = (String) parsedName;
    }

    @Override
    public void interpret(ChangeLogNode changeLogNode, int bd, int ba, int be, AbstractChangeLog abstractChangeLog) throws ZkmException, IOException {
        ChangeLogSimpleNode.getOpaqueStrings();
        int bb = this.jjtGetNumChildren();
        int childrenElementIndex = 0;

        while (childrenElementIndex < bb) {
            if (ba > 0) {
                this.jjtGetChild(childrenElementIndex).interpret(this, 30872, 34067, 41973, abstractChangeLog);
                childrenElementIndex++;
            }
        }

        ((ChangeLogASTMethodNameChange) changeLogNode).setParameterChangeLookupData(this.parameterChangeLookupData);
    }

    public ASTParameterChangeLookupData() {
        super(20);
    }
}

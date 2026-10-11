package com.zelix.klassmaster.changelog.parser.ast;

import com.zelix.klassmaster.changelog.AbstractChangeLog;
import com.zelix.klassmaster.changelog.TypeNameSetter;
import com.zelix.klassmaster.changelog.parser.ChangeLogNode;
import com.zelix.klassmaster.changelog.parser.ChangeLogSimpleNode;
import com.zelix.klassmaster.exceptions.ZkmException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ChangeLogASTMethodArguments extends ChangeLogSimpleNode implements TypeNameSetter {
    public List argumentTypes = new ArrayList();
    private static final String ARGUMENT_SEPARATOR = ", ";

    public String buildArgumentList() {
        StringBuilder stringBuilder = new StringBuilder();
        if (this.argumentTypes != null) {
            int argumentTypesSize = this.argumentTypes.size();

            for (int elementIndex = 0; elementIndex < argumentTypesSize; elementIndex++) {
                String string = (String) this.argumentTypes.get(elementIndex);
                stringBuilder.append(string);
                if (elementIndex < this.argumentTypes.size() - 1) {
                    stringBuilder.append(ARGUMENT_SEPARATOR);
                }
            }
        }

        return stringBuilder.toString();
    }

    @Override
    public void interpret(ChangeLogNode changeLogNode, int bd, int be, int ba, AbstractChangeLog abstractChangeLog) throws ZkmException, IOException {
        int bb = this.jjtGetNumChildren();

        for (int childrenElementIndex = 0; childrenElementIndex < bb; childrenElementIndex++) {
            ChangeLogNode changeLogNode1 = this;
            if (ba > 0) {
                changeLogNode1 = this.jjtGetChild(childrenElementIndex);
            }

            changeLogNode1.interpret(this, 30872, 34067, 41973, abstractChangeLog);
        }

        ChangeLogMethodSignatureNode changeLogMethodSignatureNode = (ChangeLogMethodSignatureNode) changeLogNode;
        changeLogMethodSignatureNode.setTypeName(this.buildArgumentList());
    }

    public ChangeLogASTMethodArguments() {
        super(30);
    }

    @Override
    public void setTypeName(Object typeName) {
        this.argumentTypes.add(typeName);
    }
}

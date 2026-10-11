package com.zelix.klassmaster.script.parser.ast;

import com.zelix.klassmaster.util.ArrayEnumeration;

import java.util.Enumeration;

public abstract class ParameterListStatement extends ParameterListStatementBase {
    public ParameterListStatement(int nodeTypeId) {
        super(nodeTypeId);
    }

    public final Enumeration getRenameFilterParameters() {
        int parametersSize = super.parameters.size();
        ASTRenameFilterParameter[] aSTRenameFilterParameters = new ASTRenameFilterParameter[parametersSize];

        for (int elementIndex = 0; elementIndex < parametersSize; elementIndex++) {
            aSTRenameFilterParameters[elementIndex] = (ASTRenameFilterParameter) super.parameters.get(elementIndex);
        }

        return new ArrayEnumeration(aSTRenameFilterParameters);
    }
}

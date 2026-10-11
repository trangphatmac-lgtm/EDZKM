package com.zelix.klassmaster.changelog.parser.ast;

import com.zelix.klassmaster.changelog.AbstractChangeLog;
import com.zelix.klassmaster.changelog.parser.ChangeLogNode;
import com.zelix.klassmaster.exceptions.ZkmException;

import java.io.IOException;

public class ChangeLogASTMethodNameChange extends ChangeLogModifierNode {
    public String oldArgumentTypes;
    public String parameterChangeLookupData;
    public String returnType;
    public boolean parametersObfuscated;
    public String newMethodName;
    public String parameterChangeData;
    public String newArgumentTypes;
    public String oldMethodName;

    public void setParameterChangeData(String parameterChangeDataArgument) {
        this.parameterChangeData = parameterChangeDataArgument;
    }

    public void setParameterChangeLookupData(String parameterChangeLookupDataArgument) {
        this.parameterChangeLookupData = parameterChangeLookupDataArgument;
    }

    @Override
    public void interpret(ChangeLogNode changeLogNode, int bd, int be, int bf, AbstractChangeLog abstractChangeLog) throws ZkmException, IOException {
        int ba = this.jjtGetNumChildren();

        for (int childrenElementIndex = 0; childrenElementIndex < ba; childrenElementIndex++) {
            this.jjtGetChild(childrenElementIndex).interpret(this, 30872, 34067, 41973, abstractChangeLog);
        }

        if (this.newMethodName == null) {
            this.newMethodName = this.oldMethodName;
        }

        ASTClassChange aSTClassChange = (ASTClassChange) changeLogNode;
        String oldMethodNameValue = this.oldMethodName;
        String oldArgumentTypesValue = this.oldArgumentTypes;
        String returnTypeValue = this.returnType;
        int modifiers = this.getModifiers();
        String newMethodNameValue = this.newMethodName;
        String newArgumentTypesValue = this.newArgumentTypes;
        boolean parametersObfuscated = this.parametersObfuscated;
        String parameterChangeDataValue = this.parameterChangeData;
        String parameterChangeLookupDataValue = this.parameterChangeLookupData;
        Boolean boolean2 = this.isManufactured();
        String parameterChangeLookupDataValue2 = parameterChangeLookupDataValue;
        String parameterChangeDataValue2 = parameterChangeDataValue;
        Boolean boolean1 = parametersObfuscated;
        String newArgumentTypesValue2 = newArgumentTypesValue;
        String newMethodNameValue2 = newMethodNameValue;
        aSTClassChange.addMethodChange(oldMethodNameValue, oldArgumentTypesValue, returnTypeValue, modifiers, newMethodNameValue2, newArgumentTypesValue2, boolean1, parameterChangeDataValue2, parameterChangeLookupDataValue2, boolean2);
    }

    public void setNewMethodName(String newMethodNameArgument) {
        this.newMethodName = newMethodNameArgument;
    }

    public void setNewSignature(String newMethodNameArgument, String newArgumentTypesArgument, boolean parametersObfuscated) {
        if (this.newMethodName == null) {
            this.newMethodName = newMethodNameArgument;
            this.newArgumentTypes = newArgumentTypesArgument;
            this.parametersObfuscated = parametersObfuscated;
        } else {
            this.parametersObfuscated = parametersObfuscated;
        }
    }

    public void setOldSignature(String oldMethodNameArgument, String oldArgumentTypesArgument) {
        this.oldMethodName = oldMethodNameArgument;
        this.oldArgumentTypes = oldArgumentTypesArgument;
    }

    public void setReturnType(String returnTypeArgument) {
        this.returnType = returnTypeArgument;
    }

    public ChangeLogASTMethodNameChange() {
        super(17);
    }
}

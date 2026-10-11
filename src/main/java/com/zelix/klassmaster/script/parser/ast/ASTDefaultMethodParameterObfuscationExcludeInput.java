package com.zelix.klassmaster.script.parser.ast;

import com.zelix.klassmaster.exceptions.ZkmException;
import com.zelix.klassmaster.script.ScriptEnvironment;
import com.zelix.klassmaster.script.parser.ZkmScriptSimpleNode;

import java.io.IOException;

public class ASTDefaultMethodParameterObfuscationExcludeInput extends ZkmScriptSimpleNode {
    public ParameterListStatement getParameterListStatement() {
        return this.jjtGetNumChildren() > 0 ? (ParameterListStatement) this.jjtGetChild(0) : null;
    }

    public ASTDefaultMethodParameterObfuscationExcludeInput() {
        super(49);
    }

    @Override
    public void execute(Object object1, Object object) throws ZkmException, IOException {
        ScriptEnvironment scriptEnvironment1 = (ScriptEnvironment) object;
        int ba = this.jjtGetNumChildren();

        for (int childrenElementIndex = 0; childrenElementIndex < ba; childrenElementIndex++) {
            this.jjtGetChild(childrenElementIndex).execute(this, scriptEnvironment1);
        }
    }
}

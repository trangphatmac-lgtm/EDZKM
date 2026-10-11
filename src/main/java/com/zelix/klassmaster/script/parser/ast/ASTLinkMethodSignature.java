package com.zelix.klassmaster.script.parser.ast;

import com.zelix.klassmaster.exceptions.ZkmException;
import com.zelix.klassmaster.script.ScriptEnvironment;
import com.zelix.klassmaster.script.parser.ZkmScriptNode;

import java.io.IOException;

public class ASTLinkMethodSignature extends MethodSignatureNodeBase {
    public String linkMethodName;

    public void setLinkMethodName(String linkMethodNameArgument) {
        this.linkMethodName = linkMethodNameArgument;
    }

    @Override
    public void execute(Object object, Object object1) throws ZkmException, IOException {
        ScriptEnvironment scriptEnvironment1 = (ScriptEnvironment) object1;
        ZkmScriptNode zkmScriptNode = (ZkmScriptNode) object;
        int ba = this.jjtGetNumChildren();

        for (int childrenElementIndex = 0; childrenElementIndex < ba; childrenElementIndex++) {
            this.jjtGetChild(childrenElementIndex).execute(this, scriptEnvironment1);
        }

        ASTRenameFilterParameter aSTRenameFilterParameter = (ASTRenameFilterParameter) zkmScriptNode;
        aSTRenameFilterParameter.setLinkMethodSignature(this.linkMethodName);
        aSTRenameFilterParameter.setParameterAnnotations(super.parameterAnnotations);
        aSTRenameFilterParameter.setArgsPattern(super.argsPattern);
    }

    public ASTLinkMethodSignature() {
        super(207);
    }
}

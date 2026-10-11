package com.zelix.klassmaster.script.parser.ast;

import com.zelix.klassmaster.exceptions.ZkmException;
import com.zelix.klassmaster.script.parser.ZkmScriptSimpleNode;

import java.io.IOException;

public class ASTLinkPackageName extends ZkmScriptSimpleNode {
    public ASTLinkPackageName() {
        super(193);
    }

    @Override
    public void execute(Object object, Object object1) throws ZkmException, IOException {
        ASTRenameFilterParameter aSTRenameFilterParameter = (ASTRenameFilterParameter) object;
        int ba = this.jjtGetNumChildren();
        StringBuffer stringBuffer = new StringBuffer();

        for (int childrenElementIndex = 0; childrenElementIndex < ba; childrenElementIndex++) {
            stringBuffer.append(((ScriptValueNode) this.jjtGetChild(childrenElementIndex)).getValue());
            if (childrenElementIndex < ba - 1) {
                stringBuffer.append("/");
            }
        }

        aSTRenameFilterParameter.addLinkPackageName(stringBuffer.toString());
    }
}

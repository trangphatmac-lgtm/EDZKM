package com.zelix.klassmaster.script.parser.ast;

import com.zelix.klassmaster.exceptions.ZkmException;
import com.zelix.klassmaster.script.ScriptEnvironment;
import com.zelix.klassmaster.script.parser.ZkmScriptSimpleNode;

import java.io.IOException;

public class ZkmScriptASTInput extends ZkmScriptSimpleNode {
    public ZkmScriptASTInput() {
        super(0);
    }

    public int countSaveStatements() {
        int ba = 0;
        int bb = this.jjtGetNumChildren();

        for (int childrenElementIndex = 0; childrenElementIndex < bb; childrenElementIndex++) {
            if ((ScriptStatementNode) ((ASTNTStatement) this.jjtGetChild(childrenElementIndex)).jjtGetChild(0) instanceof SaveStatementNode) {
                ba++;
            }
        }

        return ba;
    }

    public int countLoadStatements() {
        int ba = 0;
        int bb = this.jjtGetNumChildren();

        for (int childrenElementIndex = 0; childrenElementIndex < bb; childrenElementIndex++) {
            if ((ScriptStatementNode) ((ASTNTStatement) this.jjtGetChild(childrenElementIndex)).jjtGetChild(0) instanceof ASTLoadStatement) {
                ba++;
            }
        }

        return ba;
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

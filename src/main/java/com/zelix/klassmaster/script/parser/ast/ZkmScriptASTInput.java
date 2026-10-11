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

        com.zelix.klassmaster.script.ExecutionObserver initialObserver = com.zelix.klassmaster.script.ExecutionObserver.CURRENT.get();
        if (initialObserver != null) initialObserver.start(ba);
        for (int childrenElementIndex = 0; childrenElementIndex < ba; childrenElementIndex++) {
            com.zelix.klassmaster.script.ExecutionObserver observer = com.zelix.klassmaster.script.ExecutionObserver.CURRENT.get();
            if (observer != null) observer.before(this.jjtGetChild(childrenElementIndex), scriptEnvironment1);
            this.jjtGetChild(childrenElementIndex).execute(this, scriptEnvironment1);
            if (observer != null) observer.after(this.jjtGetChild(childrenElementIndex), scriptEnvironment1);
        }
    }
}

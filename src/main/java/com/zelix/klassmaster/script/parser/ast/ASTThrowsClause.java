package com.zelix.klassmaster.script.parser.ast;

import com.zelix.klassmaster.exceptions.ZkmException;
import com.zelix.klassmaster.script.ScriptEnvironment;
import com.zelix.klassmaster.script.TypeTextHolder;
import com.zelix.klassmaster.script.parser.ZkmScriptNode;
import com.zelix.klassmaster.script.parser.ZkmScriptSimpleNode;

import java.io.IOException;
import java.util.Vector;

public class ASTThrowsClause extends ZkmScriptSimpleNode implements TypeTextHolder {
    public Vector exceptionTypeNames = new Vector();

    @Override
    public void execute(Object object, Object object1) throws ZkmException, IOException {
        ZkmScriptNode zkmScriptNode = (ZkmScriptNode) object;
        ScriptEnvironment scriptEnvironment1 = (ScriptEnvironment) object1;
        int ba = this.jjtGetNumChildren();

        for (int childrenElementIndex = 0; childrenElementIndex < ba; childrenElementIndex++) {
            this.jjtGetChild(childrenElementIndex).execute(this, scriptEnvironment1);
        }

        MethodSpecifierChildNode methodSpecifierChildNode = (MethodSpecifierChildNode) zkmScriptNode;
        int elementIndex = 0;
        int bd = 0;

        for (Vector vector = this.exceptionTypeNames; bd < vector.size(); vector = this.exceptionTypeNames) {
            methodSpecifierChildNode.addThrownException((String) this.exceptionTypeNames.elementAt(elementIndex));
            bd = ++elementIndex;
        }
    }

    public ASTThrowsClause() {
        super(155);
    }

    @Override
    public void setTypeText(Object typeText) {
        this.exceptionTypeNames.addElement(typeText);
    }
}

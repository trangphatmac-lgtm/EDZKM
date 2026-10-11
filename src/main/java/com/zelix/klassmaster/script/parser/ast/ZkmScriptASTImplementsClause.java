package com.zelix.klassmaster.script.parser.ast;

import com.zelix.klassmaster.exceptions.ZkmException;
import com.zelix.klassmaster.script.ScriptEnvironment;
import com.zelix.klassmaster.script.TypeTextHolder;
import com.zelix.klassmaster.script.parser.ZkmScriptNode;
import com.zelix.klassmaster.script.parser.ZkmScriptSimpleNode;

import java.io.IOException;
import java.util.ArrayList;

public class ZkmScriptASTImplementsClause extends ZkmScriptSimpleNode implements TypeTextHolder {
    public ArrayList interfaceNames = new ArrayList();

    @Override
    public void setTypeText(Object typeText) {
        this.interfaceNames.add(typeText);
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
        int elementIndex = 0;
        int be = 0;

        for (ArrayList arrayList = this.interfaceNames; be < arrayList.size(); arrayList = this.interfaceNames) {
            aSTRenameFilterParameter.addImplementsName((String) this.interfaceNames.get(elementIndex));
            be = ++elementIndex;
        }

        boolean bl = false;

        for (int childrenElementIndexValue = 0; childrenElementIndexValue < ba; childrenElementIndexValue++) {
            ZkmScriptNode zkmScriptNode1 = this.jjtGetChild(childrenElementIndexValue);
            if (zkmScriptNode1 instanceof ASTComplexAnnotationSpecifier) {
                aSTRenameFilterParameter.addImplementsAnnotation((ASTComplexAnnotationSpecifier) zkmScriptNode1);
                bl = true;
            } else if (zkmScriptNode1 instanceof ASTQualifiedClassName) {
                if (!bl) {
                    aSTRenameFilterParameter.addImplementsAnnotation(null);
                }

                bl = false;
            }
        }
    }

    public ZkmScriptASTImplementsClause() {
        super(154);
    }
}

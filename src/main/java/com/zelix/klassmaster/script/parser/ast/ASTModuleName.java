package com.zelix.klassmaster.script.parser.ast;

import com.zelix.klassmaster.exceptions.ZkmException;
import com.zelix.klassmaster.obfuscator.exclude.QualifiedNameMatcher;
import com.zelix.klassmaster.script.parser.ZkmScriptSimpleNode;
import com.zelix.klassmaster.util.ZkmStringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ASTModuleName extends ZkmScriptSimpleNode implements QualifiedNameMatcher {
    public String moduleName;
    public List nameComponents = new ArrayList();

    @Override
    public boolean matchesName(String string) {
        return ZkmStringUtils.matchesWildcard(string, this.moduleName);
    }

    public ASTModuleName() {
        super(189);
    }

    @Override
    public void execute(Object object, Object object1) throws ZkmException, IOException {
        int ba = this.jjtGetNumChildren();
        StringBuilder stringBuilder = new StringBuilder();

        for (int childrenElementIndex = 0; childrenElementIndex < ba; childrenElementIndex++) {
            String value = ((ScriptValueNode) this.jjtGetChild(childrenElementIndex)).getValue();
            this.nameComponents.add(value);
            stringBuilder.append(value);
            if (childrenElementIndex < ba - 1) {
                stringBuilder.append(".");
            }
        }

        this.moduleName = stringBuilder.toString();
    }

    public List getNameComponents() {
        int ba = this.jjtGetNumChildren();
        ArrayList arrayList = new ArrayList(ba);

        for (int childrenElementIndex = 0; childrenElementIndex < ba; childrenElementIndex++) {
            ASTModuleNameComponent aSTModuleNameComponent = (ASTModuleNameComponent) this.jjtGetChild(childrenElementIndex);
            arrayList.add(aSTModuleNameComponent.getValue());
        }

        return arrayList;
    }

    @Override
    public String getSpecText() {
        return this.moduleName + "/";
    }
}

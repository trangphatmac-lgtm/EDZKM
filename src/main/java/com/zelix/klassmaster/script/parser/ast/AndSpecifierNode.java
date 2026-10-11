package com.zelix.klassmaster.script.parser.ast;

import com.zelix.klassmaster.exceptions.ZkmException;
import com.zelix.klassmaster.obfuscator.exclude.DescribableSpec;
import com.zelix.klassmaster.script.ScriptEnvironment;
import com.zelix.klassmaster.script.parser.ZkmScriptSimpleNode;

import java.io.IOException;

public abstract class AndSpecifierNode extends ZkmScriptSimpleNode {
    private static final String AND_SEPARATOR = " && ";

    @Override
    public void execute(Object object1, Object object) throws ZkmException, IOException {
        ScriptEnvironment scriptEnvironment1 = (ScriptEnvironment) object;
        int ba = this.jjtGetNumChildren();

        for (int childrenElementIndex = 0; childrenElementIndex < ba; childrenElementIndex++) {
            this.jjtGetChild(childrenElementIndex).execute(this, scriptEnvironment1);
        }
    }

    public String getSpecText() {
        StringBuffer stringBuffer = new StringBuffer();
        int lengthValue = this.children.length;
        if (lengthValue > 1) {
            stringBuffer.append('(');
        }

        for (int childrenElementIndex = 0; childrenElementIndex < lengthValue; childrenElementIndex++) {
            DescribableSpec describableSpec = (DescribableSpec) this.children[childrenElementIndex];
            stringBuffer.append(describableSpec.getSpecText());
            if (childrenElementIndex < lengthValue - 1) {
                stringBuffer.append(AND_SEPARATOR);
            }
        }

        if (lengthValue > 1) {
            stringBuffer.append(')');
        }

        return stringBuffer.toString();
    }

    public AndSpecifierNode(int nodeTypeId) {
        super(nodeTypeId);
    }
}

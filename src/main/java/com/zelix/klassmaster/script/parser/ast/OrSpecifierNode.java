package com.zelix.klassmaster.script.parser.ast;

import com.zelix.klassmaster.exceptions.ZkmException;
import com.zelix.klassmaster.obfuscator.exclude.DescribableSpec;
import com.zelix.klassmaster.script.ScriptEnvironment;
import com.zelix.klassmaster.script.parser.ZkmScriptSimpleNode;

import java.io.IOException;

public abstract class OrSpecifierNode extends ZkmScriptSimpleNode {
    public boolean negated = false;
    private static final String OR_SEPARATOR = " || ";

    @Override
    public void execute(Object object1, Object object) throws ZkmException, IOException {
        ScriptEnvironment scriptEnvironment1 = (ScriptEnvironment) object;
        int ba = this.jjtGetNumChildren();

        for (int childrenElementIndex = 0; childrenElementIndex < ba; childrenElementIndex++) {
            this.jjtGetChild(childrenElementIndex).execute(this, scriptEnvironment1);
        }
    }

    public String getSpecText() {
        StringBuilder result = new StringBuilder();
        int childCount = this.children.length;
        if (this.negated || childCount > 1) {
            result.append('(');
        }
        for (int childIndex = 0; childIndex < childCount; childIndex++) {
            DescribableSpec child = (DescribableSpec) this.children[childIndex];
            result.append(child.getSpecText());
            if (childIndex < childCount - 1) {
                result.append(OR_SEPARATOR);
            }
        }
        if (this.negated || childCount > 1) {
            result.append(')');
        }
        return (this.negated ? "!" : "") + result.toString();
    }

    public void setNegated() {
        this.negated = true;
    }

    public OrSpecifierNode(int nodeTypeId) {
        super(nodeTypeId);
    }
}

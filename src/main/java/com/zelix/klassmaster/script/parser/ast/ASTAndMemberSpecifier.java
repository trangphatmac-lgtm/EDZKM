package com.zelix.klassmaster.script.parser.ast;

import com.zelix.klassmaster.classfile.ClassFileBase;
import com.zelix.klassmaster.classfile.hierarchy.ClassHierarchyQuery;
import com.zelix.klassmaster.exceptions.ZkmException;
import com.zelix.klassmaster.obfuscator.exclude.DescribableSpec;
import com.zelix.klassmaster.obfuscator.exclude.MemberSpecifierMatcher;
import com.zelix.klassmaster.script.ScriptEnvironment;
import com.zelix.klassmaster.script.parser.ZkmScriptSimpleNode;

import java.io.IOException;

public class ASTAndMemberSpecifier extends ZkmScriptSimpleNode implements MemberSpecifierMatcher {
    public boolean negated = false;
    private static final String AND_SEPARATOR = " && ";

    @Override
    public boolean hasMatchingMember(ClassFileBase classFileBase, ClassHierarchyQuery classHierarchyQuery) throws ZkmException, IOException {
        int lengthValue = this.children.length;

        for (int childrenElementIndex = 0; childrenElementIndex < lengthValue; childrenElementIndex++) {
            if (!((MemberSpecifierMatcher) this.children[childrenElementIndex]).hasMatchingMember(classFileBase, classHierarchyQuery)) {
                return false;
            }
        }

        return true;
    }

    public ASTAndMemberSpecifier() {
        super(151);
    }

    @Override
    public String getSpecText() {
        int flowPredicate = ZkmScriptSimpleNode.getFlowPredicate();
        StringBuffer result = new StringBuffer();
        int childCount = this.children.length;
        if (flowPredicate != 0) {
            int childIndex = this.negated ? 1 : 0;
            if (childIndex < childCount) {
                // Preserve the original checked cast even when this path does not render the child.
                DescribableSpec child = (DescribableSpec) this.children[childIndex];
            }
            return (this.negated ? "!" : "") + result.toString();
        }
        if (this.negated || childCount > 1) {
            result.append('(');
        }
        for (int childIndex = 0; childIndex < childCount; childIndex++) {
            DescribableSpec child = (DescribableSpec) this.children[childIndex];
            result.append(child.getSpecText());
            if (childIndex < childCount - 1) {
                result.append(AND_SEPARATOR);
            }
        }
        if (this.negated || childCount > 1) {
            result.append(')');
        }
        return (this.negated ? "!" : "") + result.toString();
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

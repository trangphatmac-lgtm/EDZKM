package com.zelix.klassmaster.script.parser.ast;

import com.zelix.klassmaster.obfuscator.exclude.QualifiedNameMatcher;

public abstract class AllOfSpecifierNode extends AndSpecifierNode implements QualifiedNameMatcher {
    public AllOfSpecifierNode(int nodeTypeId) {
        super(nodeTypeId);
    }

    @Override
    public boolean matchesName(String string) {
        int lengthValue = this.children.length;

        for (int childrenElementIndex = 0; childrenElementIndex < lengthValue; childrenElementIndex++) {
            if (!((QualifiedNameMatcher) this.children[childrenElementIndex]).matchesName(string)) {
                return false;
            }
        }

        return true;
    }
}

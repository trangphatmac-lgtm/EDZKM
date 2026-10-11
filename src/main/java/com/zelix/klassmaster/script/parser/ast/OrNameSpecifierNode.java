package com.zelix.klassmaster.script.parser.ast;

import com.zelix.klassmaster.obfuscator.exclude.QualifiedNameMatcher;

public abstract class OrNameSpecifierNode extends OrSpecifierNode implements QualifiedNameMatcher {
    public OrNameSpecifierNode(int nodeTypeId) {
        super(nodeTypeId);
    }

    @Override
    public boolean matchesName(String string) {
        boolean bl = false;
        int lengthValue = this.children.length;

        for (int childrenElementIndex = 0; childrenElementIndex < lengthValue; childrenElementIndex++) {
            if (((QualifiedNameMatcher) this.children[childrenElementIndex]).matchesName(string)) {
                bl = true;
                break;
            }
        }

        return super.negated ^ bl;
    }
}

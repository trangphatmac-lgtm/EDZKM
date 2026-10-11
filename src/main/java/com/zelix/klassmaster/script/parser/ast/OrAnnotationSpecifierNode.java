package com.zelix.klassmaster.script.parser.ast;

import com.zelix.klassmaster.obfuscator.exclude.AnnotationMatcher;

import java.util.Set;

public abstract class OrAnnotationSpecifierNode extends OrSpecifierNode implements AnnotationMatcher {
    @Override
    public boolean matchesAnyAnnotation(Set set1) {
        boolean bl = false;
        int lengthValue = this.children.length;

        for (int childrenElementIndex = 0; childrenElementIndex < lengthValue; childrenElementIndex++) {
            if (((AnnotationMatcher) this.children[childrenElementIndex]).matchesAnyAnnotation(set1)) {
                bl = true;
                break;
            }
        }

        return super.negated ^ bl;
    }

    public OrAnnotationSpecifierNode() {
        super(176);
    }
}

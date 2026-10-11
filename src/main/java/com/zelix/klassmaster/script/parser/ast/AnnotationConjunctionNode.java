package com.zelix.klassmaster.script.parser.ast;

import com.zelix.klassmaster.obfuscator.exclude.AnnotationMatcher;

import java.util.Set;

public abstract class AnnotationConjunctionNode extends AndSpecifierNode implements AnnotationMatcher {
    @Override
    public boolean matchesAnyAnnotation(Set set1) {
        int lengthValue = this.children.length;

        for (int childrenElementIndex = 0; childrenElementIndex < lengthValue; childrenElementIndex++) {
            if (!((AnnotationMatcher) this.children[childrenElementIndex]).matchesAnyAnnotation(set1)) {
                return false;
            }
        }

        return true;
    }

    public AnnotationConjunctionNode() {
        super(177);
    }
}

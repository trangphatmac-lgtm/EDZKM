package com.zelix.klassmaster.proguard.config.parser.ast;

import com.zelix.klassmaster.exceptions.ZkmException;
import com.zelix.klassmaster.proguard.NamePatternEntry;
import com.zelix.klassmaster.proguard.ProGuardClassNameListOwner;
import com.zelix.klassmaster.proguard.ProGuardConfigTranslator;
import com.zelix.klassmaster.proguard.config.parser.ProGuardConfigSimpleNode;

import java.io.IOException;

public class ASTProduction2 extends ProGuardConfigSimpleNode {
    @Override
    public void translate(Object object1, Object object) throws ZkmException, IOException {
        ProGuardConfigTranslator proGuardConfigTranslator = (ProGuardConfigTranslator) object;
        int ba = this.jjtGetNumChildren();

        for (int childrenElementIndex = 0; childrenElementIndex < this.jjtGetNumChildren(); childrenElementIndex++) {
            this.jjtGetChild(childrenElementIndex).translate(this, proGuardConfigTranslator);
        }

        ProGuardClassNameListOwner proGuardClassNameListOwner = (ProGuardClassNameListOwner) this.jjtGetParent();

        for (int childrenElementIndexValue = 0; childrenElementIndexValue < ba; childrenElementIndexValue++) {
            ASTStarClause2 aSTStarClause2 = (ASTStarClause2) this.jjtGetChild(childrenElementIndexValue);
            String value = aSTStarClause2.getValue();
            boolean negated = aSTStarClause2.isNegated();
            boolean inList = ba > 1;
            boolean negatedValue = negated;
            String valueValue = value;
            proGuardClassNameListOwner.addClassNamePattern(new NamePatternEntry(valueValue, negatedValue, inList));
        }
    }

    public ASTProduction2() {
        super(88);
    }
}

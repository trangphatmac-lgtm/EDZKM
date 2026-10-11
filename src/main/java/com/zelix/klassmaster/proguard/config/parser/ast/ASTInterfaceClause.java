package com.zelix.klassmaster.proguard.config.parser.ast;

import com.zelix.klassmaster.exceptions.ZkmException;
import com.zelix.klassmaster.proguard.NegatableClause;
import com.zelix.klassmaster.proguard.ProGuardConfigTranslator;
import com.zelix.klassmaster.proguard.config.parser.ProGuardConfigNode;

import java.io.IOException;

public class ASTInterfaceClause extends ProGuardValueNode implements NegatableClause {
    public boolean negated;

    public ASTInterfaceClause() {
        super(93);
    }

    @Override
    public void setNegated() {
        this.negated = true;
    }

    @Override
    public void translate(Object object, Object object1) throws ZkmException, IOException {
        ProGuardConfigNode proGuardConfigNode = (ProGuardConfigNode) object;
        ProGuardConfigTranslator proGuardConfigTranslator = (ProGuardConfigTranslator) object1;
        super.translate(proGuardConfigNode, proGuardConfigTranslator);
        ASTLbraceClause aSTLbraceClause = (ASTLbraceClause) this.jjtGetParent();
        String value = this.getValue();
        if (value.equals("interface") || value.equals("enum")) {
            aSTLbraceClause.addModifier((this.negated ? "!" : "") + value);
        } else if (value.equals("@interface")) {
            aSTLbraceClause.addModifier((this.negated ? "!" : "") + "annotation");
        } else if (this.negated) {
            proGuardConfigTranslator.logWarning("'!" + this.getValue() + "' is not directly supported");
        }
    }
}

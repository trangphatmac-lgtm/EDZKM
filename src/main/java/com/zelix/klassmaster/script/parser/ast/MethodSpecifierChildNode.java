package com.zelix.klassmaster.script.parser.ast;

import com.zelix.klassmaster.obfuscator.exclude.MemberSpecCollector;

public abstract class MethodSpecifierChildNode extends MethodSignatureNodeBase {
    public ASTComplexMethodSpecifier methodSpecifier;

    public MethodSpecifierChildNode(int nodeTypeId) {
        super(nodeTypeId);
    }

    public void addThrownException(String string) {
        ((MemberSpecCollector) this.jjtGetParent()).addParameterType(string);
    }

    public void setMethodSpecifier(ASTComplexMethodSpecifier aSTComplexMethodSpecifier) {
        this.methodSpecifier = aSTComplexMethodSpecifier;
    }
}

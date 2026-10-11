package com.zelix.klassmaster.obfuscator.flow;

public class FlowFieldPair {
    public final OpaquePredicateField groupField;
    public final OpaquePredicateField packageField;
    public final String packageName;

    public String getPackageName() {
        return this.packageName;
    }

    public OpaquePredicateField getPackageField() {
        return this.packageField;
    }

    public FlowFieldPair(OpaquePredicateField opaquePredicateField, OpaquePredicateField opaquePredicateField1, String packageNameArgument) {
        this.groupField = opaquePredicateField;
        this.packageField = opaquePredicateField1;
        this.packageName = packageNameArgument;
    }

    public OpaquePredicateField getGroupField() {
        return this.groupField;
    }
}

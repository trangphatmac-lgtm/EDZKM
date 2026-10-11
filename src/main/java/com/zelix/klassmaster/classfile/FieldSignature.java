package com.zelix.klassmaster.classfile;

import java.util.Map;

public class FieldSignature {
    private final String name;
    private final String descriptor;
    private final int hash;

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof FieldSignature)) {
            return false;
        }

        FieldSignature fieldSignature1 = (FieldSignature) other;
        return this.hash == fieldSignature1.hash && this.name.equals(fieldSignature1.name) && this.descriptor.equals(fieldSignature1.descriptor);
    }

    public String getName() {
        return this.name;
    }

    public FieldSignature(String nameArgument, String descriptorArgument) {
        this.name = nameArgument;
        this.descriptor = descriptorArgument;
        this.hash = nameArgument.hashCode() ^ descriptorArgument.hashCode();
    }

    @Override
    public Object clone() {
        return new FieldSignature(this.name, this.descriptor, this.hash);
    }

    public String getDescriptor() {
        return this.descriptor;
    }

    @Override
    public int hashCode() {
        return this.hash;
    }

    public String formatDeclaration(Map map1) {
        return MethodSignature.descriptorToJavaType(this.descriptor, map1) + " " + this.name;
    }

    private FieldSignature(String nameArgument, String descriptorArgument, int hash) {
        this.name = nameArgument;
        this.descriptor = descriptorArgument;
        this.hash = hash;
    }

    static {
        new FieldSignature("serialVersionUID", "J");
    }
}

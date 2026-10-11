package com.zelix.klassmaster.classfile.insn;

public class UninitializedType extends UninitializedThisValueType {
    public TypeInstruction newInstruction;

    public TypeInstruction getNewInstruction() {
        return this.newInstruction;
    }

    public UninitializedType(String descriptor, TypeInstruction typeInstruction) {
        super(descriptor);
        this.newInstruction = typeInstruction;
    }

    @Override
    public boolean isUninitializedThis() {
        return false;
    }
}

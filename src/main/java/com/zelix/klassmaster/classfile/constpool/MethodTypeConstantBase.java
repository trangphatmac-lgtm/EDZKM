package com.zelix.klassmaster.classfile.constpool;

public abstract class MethodTypeConstantBase extends ConstantPoolEntry {
    public static final ConstantPoolTag TAG = ConstantPoolTag.METHOD_TYPE;

    @Override
    public ConstantPoolTag getTag() {
        return TAG;
    }

    public MethodTypeConstantBase(int index, AbstractConstantPool abstractConstantPool) {
        super(index, abstractConstantPool);
    }
}

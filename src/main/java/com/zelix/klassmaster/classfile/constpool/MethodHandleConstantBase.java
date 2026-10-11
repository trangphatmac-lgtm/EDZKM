package com.zelix.klassmaster.classfile.constpool;

public abstract class MethodHandleConstantBase extends ConstantPoolEntry {
    public static final ConstantPoolTag TAG = ConstantPoolTag.METHOD_HANDLE;

    public MethodHandleConstantBase(int index, AbstractConstantPool abstractConstantPool) {
        super(index, abstractConstantPool);
    }

    @Override
    public final ConstantPoolTag getTag() {
        return TAG;
    }
}

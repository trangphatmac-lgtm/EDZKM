package com.zelix.klassmaster.classfile.constpool;

public abstract class ResolvedDynamicConstantBase extends ConstantPoolEntry {
    public ResolvedDynamicConstantBase(int index, AbstractConstantPool abstractConstantPool) {
        super(index, abstractConstantPool);
    }
}

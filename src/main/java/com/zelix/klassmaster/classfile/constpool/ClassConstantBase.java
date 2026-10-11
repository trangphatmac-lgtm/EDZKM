package com.zelix.klassmaster.classfile.constpool;

public abstract class ClassConstantBase extends ConstantPoolEntry {
    public static final ConstantPoolTag CLASS_TAG = ConstantPoolTag.CLASS;

    public abstract void setClassName(String className);

    @Override
    public ConstantPoolTag getTag() {
        return CLASS_TAG;
    }

    public ClassConstantBase(int index, AbstractConstantPool abstractConstantPool) {
        super(index, abstractConstantPool);
    }

    public abstract String getDottedClassName();

    public abstract String getClassName();
}

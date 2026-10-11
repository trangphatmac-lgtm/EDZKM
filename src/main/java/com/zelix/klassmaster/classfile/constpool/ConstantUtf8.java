package com.zelix.klassmaster.classfile.constpool;

import com.zelix.klassmaster.classfile.ClassFileInputStream;
import com.zelix.klassmaster.util.ZkmAssert;
import com.zelix.klassmaster.util.ZkmUtils;

import java.io.DataOutputStream;
import java.io.IOException;

public class ConstantUtf8 extends ConstantPoolEntry implements ConstantReferenceVisitable, ConstantKeyProvider {
    public static final ConstantPoolTag TAG = ConstantPoolTag.UTF8;
    public String value;

    public String getValue() {
        return this.value;
    }

    @Override
    public ConstantPoolTag getTag() {
        return TAG;
    }

    public ConstantUtf8(int index, ClassFileInputStream classFileInputStream, AbstractConstantPool abstractConstantPool) throws IOException {
        super(index, abstractConstantPool);
        byte[] byteArray = new byte[classFileInputStream.readUnsignedShort()];
        classFileInputStream.read(byteArray);
        this.value = ZkmUtils.fromModifiedUtf8(byteArray);
        this.value = this.value.intern();
    }

    public ConstantUtf8 copyWithIndex(int index) {
        return new ConstantUtf8(index, this.constantPool, this.value);
    }

    @Override
    public String getAbbreviatedValue() {
        String abbreviatedValue = ZkmUtils.escapeJavaString(this.getValueString());
        if (abbreviatedValue.length() > 200) {
            abbreviatedValue = abbreviatedValue.substring(0, 50) + "...<" + (abbreviatedValue.length() - 100) + " CHARS>..." + abbreviatedValue.substring(abbreviatedValue.length() - 50);
        }

        return abbreviatedValue;
    }

    @Override
    public boolean registerUsage(UsedConstantsCollector usedConstantsCollector, Object object, Object object1) {
        return usedConstantsCollector.markUsed(this, object, object1);
    }

    public final int getLength() {
        return this.value.length();
    }

    @Override
    public String getValueString() {
        return this.getValue();
    }

    @Override
    public String getConstantKey() {
        return this.value;
    }

    public ConstantUtf8(int index, AbstractConstantPool abstractConstantPool, String valueArgument) {
        super(index, abstractConstantPool);
        this.value = valueArgument;
    }

    @Override
    public void writeTo(DataOutputStream dataOutputStream) throws IOException {
        dataOutputStream.writeByte(TAG.getTagValue());
        byte[] byteArray = ZkmUtils.toModifiedUtf8(this.value);
        ZkmAssert.assertTrue(
                byteArray.length <= 65535, new String[]{"String too long in class '" + this.getOwnerLocationDescription() + "' : " + byteArray.length + " : " + this.value.length()}
        );
        dataOutputStream.writeShort(byteArray.length);
        dataOutputStream.write(byteArray);
    }

    public void setValue(String valueArgument) {
        this.value = valueArgument;
        this.value = this.value.intern();
    }
}

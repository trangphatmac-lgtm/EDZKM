package com.zelix.klassmaster.classfile.constpool;

import com.zelix.klassmaster.util.ListMultimap;
import com.zelix.klassmaster.util.ZkmUtils;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;

public class ResolvedPackageConstant extends ConstantPoolEntry implements Utf8ConstantReplaceable, ConstantReferenceVisitable {
    public ConstantUtf8 nameUtf8;
    public static final ConstantPoolTag TAG = ConstantPoolTag.PACKAGE;

    @Override
    public void writeRemappedTo(DataOutputStream dataOutputStream, Map map1) throws IOException {
        dataOutputStream.writeByte(TAG.getTagValue());
        ConstantUtf8 constantUtf8 = (ConstantUtf8) map1.get(this.nameUtf8);
        if (constantUtf8 != null) {
            dataOutputStream.writeShort(constantUtf8.getIndex());
        } else {
            dataOutputStream.writeShort(this.nameUtf8.getIndex());
        }
    }

    @Override
    public String getValueString() {
        return this.nameUtf8.getValue();
    }

    @Override
    public boolean registerUsage(UsedConstantsCollector usedConstantsCollector, Object object, Object object1) {
        return usedConstantsCollector.markUsed(this, object, object1);
    }

    @Override
    public String getDisplayString() {
        return "\"" + this.getValueString() + "\"";
    }

    @Override
    public ConstantPoolTag getTag() {
        return TAG;
    }

    public ConstantUtf8 getNameUtf8() {
        return this.nameUtf8;
    }

    public void remapName(Map map1) {
        String value = this.nameUtf8.getValue();
        String valueValue = (String) ZkmUtils.mapOrSelf(value, map1);
        if (!value.equals(valueValue)) {
            this.nameUtf8.setValue(valueValue);
        }
    }

    @Override
    public void replaceUtf8Constant(ConstantUtf8 constantUtf8, ConstantUtf8 constantUtf81) {
        if (this.nameUtf8 == constantUtf8) {
            this.nameUtf8 = constantUtf81;
        }
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
    public void writeTo(DataOutputStream dataOutputStream) throws IOException {
        dataOutputStream.writeByte(TAG.getTagValue());
        dataOutputStream.writeShort(this.nameUtf8.getIndex());
    }

    public ResolvedPackageConstant(ConstantPackage constantPackage, ConstantUtf8 constantUtf8, ListMultimap listMultimap) {
        super(constantPackage.index, constantPackage.constantPool);
        this.nameUtf8 = constantUtf8;
        listMultimap.addValue(constantUtf8, this);
    }
}

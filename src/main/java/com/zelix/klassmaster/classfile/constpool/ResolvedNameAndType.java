package com.zelix.klassmaster.classfile.constpool;

import com.zelix.klassmaster.util.ListMultimap;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResolvedNameAndType extends ConstantPoolEntry implements ConstantReferenceVisitable, Utf8ConstantReplaceable, ConstantKeyProvider {
    public ConstantUtf8 nameUtf8;
    public ConstantUtf8 descriptorUtf8;
    public static final ConstantPoolTag TAG = ConstantPoolTag.NAME_AND_TYPE;

    public ConstantUtf8 getDescriptorUtf8() {
        return this.descriptorUtf8;
    }

    public List getDescriptorClasses() {
        return ConstantPoolEntry.getReferencedClasses(this.getDescriptor());
    }

    public ConstantUtf8 getNameUtf8() {
        return this.nameUtf8;
    }

    @Override
    public void writeRemappedTo(DataOutputStream dataOutputStream, Map map1) throws IOException {
        dataOutputStream.writeByte(TAG.getTagValue());
        ConstantUtf8 constantUtf8 = (ConstantUtf8) map1.get(this.nameUtf8);
        if (constantUtf8 != null) {
            dataOutputStream.writeShort(constantUtf8.getIndex());
        } else {
            dataOutputStream.writeShort(this.nameUtf8.getIndex());
        }

        ConstantUtf8 constantUtf81 = (ConstantUtf8) map1.get(this.descriptorUtf8);
        if (constantUtf81 != null) {
            dataOutputStream.writeShort(constantUtf81.getIndex());
        } else {
            dataOutputStream.writeShort(this.descriptorUtf8.getIndex());
        }
    }

    public String getName() {
        return this.nameUtf8.getValue();
    }

    @Override
    public boolean registerUsage(UsedConstantsCollector usedConstantsCollector, Object object, Object object1) {
        return usedConstantsCollector.markUsed(this, object, object1);
    }

    @Override
    public String getValueString() {
        String valueString = this.descriptorUtf8.getValueString();
        String valueStringValue = this.nameUtf8.getValueString();
        String valueStringValue2 = ConstantPoolEntry.descriptorToJavaType(valueString) + " " + valueStringValue;
        if (valueString.indexOf(40) == 0) {
            valueStringValue2 = valueStringValue2 + "(";
            List list1 = ConstantPoolEntry.getParameterTypes(valueString);

            for (int elementIndex = 0; elementIndex < list1.size(); elementIndex++) {
                valueStringValue2 = valueStringValue2 + ConstantPoolEntry.descriptorToJavaType((String) list1.get(elementIndex)) + (elementIndex < list1.size() - 1 ? ", " : "");
            }

            valueStringValue2 = valueStringValue2 + ")";
        }

        return valueStringValue2;
    }

    @Override
    public void replaceUtf8Constant(ConstantUtf8 constantUtf8, ConstantUtf8 constantUtf81) {
        if (this.nameUtf8 == constantUtf8) {
            this.nameUtf8 = constantUtf81;
        } else if (this.descriptorUtf8 == constantUtf8) {
            this.descriptorUtf8 = constantUtf81;
        }
    }

    public ResolvedNameAndType(ConstantNameAndType constantNameAndType, ConstantUtf8 constantUtf8, ConstantUtf8 constantUtf81) {
        super(constantNameAndType.index, constantNameAndType.constantPool);
        this.nameUtf8 = constantUtf8;
        this.descriptorUtf8 = constantUtf81;
    }

    public List getDescriptorProgramClasses() {
        return ConstantPoolEntry.getReferencedProgramClasses(this.getDescriptor());
    }

    @Override
    public String getConstantKey() {
        return this.nameUtf8.getValueString() + '~' + this.descriptorUtf8.getValueString();
    }

    @Override
    public String getDisplayString() {
        String valueString = this.nameUtf8.getValueString();
        String valueStringValue = this.descriptorUtf8.getValueString();
        String displayString = valueString;
        if (valueStringValue.indexOf(40) == 0) {
            displayString = displayString + "(";
            List list1 = ConstantPoolEntry.getParameterTypes(valueStringValue);

            for (int elementIndex = 0; elementIndex < list1.size(); elementIndex++) {
                displayString = displayString + ConstantPoolEntry.descriptorToJavaType((String) list1.get(elementIndex)) + (elementIndex < list1.size() - 1 ? ", " : "");
            }

            displayString = displayString + ")";
        }

        return displayString;
    }

    public String getDescriptor() {
        return this.descriptorUtf8.getValue();
    }

    public void remapDescriptor(HashMap hashMap) {
        String value = this.descriptorUtf8.getValue();
        String valueValue = ConstantPoolEntry.remapDescriptorClassNames(value, hashMap);
        if (valueValue != value) {
            this.descriptorUtf8.setValue(valueValue);
        }
    }

    public ResolvedNameAndType(
            int indexArgument, AbstractConstantPool abstractConstantPool, ConstantUtf8 constantUtf8, ConstantUtf8 constantUtf81, ListMultimap listMultimap
    ) {
        super(indexArgument, abstractConstantPool);
        this.nameUtf8 = constantUtf8;
        this.descriptorUtf8 = constantUtf81;
        if (listMultimap != null) {
            listMultimap.addValue(constantUtf8, this);
            listMultimap.addValue(constantUtf81, this);
        }
    }

    public final void setDescriptor(String descriptor) {
        this.descriptorUtf8.setValue(descriptor);
    }

    public final void setName(String name) {
        this.nameUtf8.setValue(name);
    }

    @Override
    public ConstantPoolTag getTag() {
        return TAG;
    }

    @Override
    public void writeTo(DataOutputStream dataOutputStream) throws IOException {
        dataOutputStream.writeByte(TAG.getTagValue());
        dataOutputStream.writeShort(this.nameUtf8.getIndex());
        dataOutputStream.writeShort(this.descriptorUtf8.getIndex());
    }
}

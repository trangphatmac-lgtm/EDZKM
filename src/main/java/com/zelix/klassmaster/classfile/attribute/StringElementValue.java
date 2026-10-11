package com.zelix.klassmaster.classfile.attribute;

import com.zelix.klassmaster.classfile.AbstractFieldInfo;
import com.zelix.klassmaster.classfile.ClassFileBase;
import com.zelix.klassmaster.classfile.ClassFileComponent;
import com.zelix.klassmaster.classfile.ClassFileInputStream;
import com.zelix.klassmaster.classfile.FieldInfo;
import com.zelix.klassmaster.classfile.constpool.ConstantPoolEntry;
import com.zelix.klassmaster.classfile.constpool.ConstantUtf8;
import com.zelix.klassmaster.classfile.constpool.UsedConstantsCollector;
import com.zelix.klassmaster.classfile.hierarchy.ClassHierarchyNode;
import com.zelix.klassmaster.exceptions.ZkmException;
import com.zelix.klassmaster.util.BooleanFlag;
import com.zelix.klassmaster.util.ListMultimap;
import com.zelix.klassmaster.util.TwoKeyMap;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class StringElementValue extends AnnotationElementValue {
    public static TwoKeyMap specialElementKinds = new TwoKeyMap();
    public FieldInfo referencedField;
    public ConstantUtf8 stringConstant;

    @Override
    public void collectReferencedClasses(Object object) {
        if (this.isValid()) {
            String key = ConstantPoolEntry.parseClassNameCandidate(this.stringConstant.getValue());
            if (key != null) {
                ClassFileBase classFileBase = ClassHierarchyNode.findClassFile(key);
                if (classFileBase != null) {
                    ((Set) object).add(classFileBase);
                }
            }
        }
    }

    @Override
    public void updateAfterClassRename(Object object, Object object1) {
        HashMap hashMap = (HashMap) object1;
        HashMap hashMap1 = (HashMap) object;
        AnnotationNameKind annotationNameKind = (AnnotationNameKind) specialElementKinds.getValue(this.getAnnotationTypeName(), this.getElementName());
        boolean bl = annotationNameKind != null && annotationNameKind.equals(AnnotationNameKind.PACKAGE);
        String valueValue = this.stringConstant.getValue();
        if (bl) {
            String valueValue2 = ConstantPoolEntry.remapQualifiedName(valueValue, hashMap1, true);
            if (!valueValue2.equals(valueValue)) {
                this.stringConstant.setValue(valueValue2);
            }
        } else if (annotationNameKind != AnnotationNameKind.FIELD) {
            BooleanFlag booleanFlag = new BooleanFlag();
            String valueValue3 = ConstantPoolEntry.remapClassNameString(valueValue, hashMap, booleanFlag);
            if (!valueValue3.equals(valueValue)) {
                this.stringConstant.setValue(valueValue3);
            } else if (!booleanFlag.getValue()) {
                valueValue3 = ConstantPoolEntry.remapQualifiedName(valueValue, hashMap1, false);
                if (!valueValue3.equals(valueValue)) {
                    this.stringConstant.setValue(valueValue3);
                }
            }
        }
    }

    @Override
    public void writeRemapped(DataOutputStream dataOutputStream, Object object, Object object1) throws IOException {
        dataOutputStream.writeByte(this.getTag());
        ConstantUtf8 constantUtf8 = (ConstantUtf8) ((Map) object).get(this.stringConstant);
        if (constantUtf8 != null) {
            dataOutputStream.writeShort(constantUtf8.getIndex());
        } else {
            dataOutputStream.writeShort(this.stringConstant.getIndex());
        }
    }

    @Override
    public void replaceUtf8Constant(ConstantUtf8 constantUtf8, ConstantUtf8 constantUtf81) {
        if (this.stringConstant == constantUtf8) {
            this.stringConstant = constantUtf81;
        }
    }

    @Override
    public String getValueTypeDescriptor() {
        return "Ljava/lang/String;";
    }

    static {
        specialElementKinds.putValue("org/springframework/context/annotation/ComponentScan", "basePackages", AnnotationNameKind.PACKAGE);
        specialElementKinds.putValue("org/springframework/context/annotation/ComponentScan", "value", AnnotationNameKind.PACKAGE);
        specialElementKinds.putValue("javax/xml/bind/annotation/XmlType", "propOrder", AnnotationNameKind.FIELD);
    }

    @Override
    public void resolveReferences(Object object, Object object1, Object object2) throws ZkmException, IOException {
        AnnotationNameKind annotationNameKind = (AnnotationNameKind) specialElementKinds.getValue(this.getAnnotationTypeName(), this.getElementName());
        if (annotationNameKind != null && annotationNameKind == AnnotationNameKind.FIELD) {
            FieldInfo fieldInfo = this.findReferencedField();
            if (fieldInfo != null) {
                this.referencedField = fieldInfo;
            }
        }
    }

    @Override
    public int getByteLength() {
        return 3;
    }

    public String getStringValue() {
        return this.stringConstant.getValueString();
    }

    @Override
    public boolean isArrayOrClassValue() {
        return false;
    }

    @Override
    public boolean hasClassValue() {
        return false;
    }

    public FieldInfo findReferencedField() {
        ClassFileBase classFileBase = this.getOwningClass();
        if (classFileBase.isProgramClass()) {
            AbstractFieldInfo[] abstractFieldInfos = classFileBase.findFieldsByName(this.stringConstant.getValueString());
            if (abstractFieldInfos.length == 1) {
                return (FieldInfo) abstractFieldInfos[0];
            }
        }

        return null;
    }

    @Override
    public String getClassValueName() {
        return null;
    }

    @Override
    public void collectReferencedMembers(Object object, Object object2, Object object1, Object object3) {
        String key = ConstantPoolEntry.parseClassNameCandidate(this.stringConstant.getValue());
        TwoKeyMap twoKeyMap;
        if (key != null) {
            if (ClassHierarchyNode.isProgramClassName(key)) {
                ((Set) object).add(ClassHierarchyNode.findProgramClass(key));
                twoKeyMap = specialElementKinds;
            } else {
                twoKeyMap = specialElementKinds;
            }
        } else {
            twoKeyMap = specialElementKinds;
        }

        if ((AnnotationNameKind) twoKeyMap.getValue(this.getAnnotationTypeName(), this.getElementName()) == AnnotationNameKind.FIELD) {
            FieldInfo fieldInfo = this.findReferencedField();
            if (fieldInfo != null) {
                ((Set) object1).add(fieldInfo);
            }
        }
    }

    public void setStringValue(String stringValue) {
        this.stringConstant.setValue(stringValue);
    }

    @Override
    public void updateAfterFieldRename() {
        if (this.referencedField != null && !this.referencedField.getSourceName().equals(this.stringConstant.getValue())) {
            this.stringConstant.setValue(this.referencedField.getSourceName());
        }
    }

    public StringElementValue(ClassFileComponent classFileComponent, int tagArgument, ClassFileInputStream classFileInputStream, ListMultimap listMultimap) throws IOException {
        super(classFileComponent, tagArgument);
        int constantPoolIndex = classFileInputStream.readUnsignedShort();
        ConstantPoolEntry constantPoolEntry = this.getConstantPoolEntry(constantPoolIndex);
        if (constantPoolEntry != null && constantPoolEntry instanceof ConstantUtf8) {
            this.stringConstant = (ConstantUtf8) constantPoolEntry;
            listMultimap.addValue(this.stringConstant, this);
        } else {
            this.setValid();
            this.setErrorMessage("Invalid component string constant index : " + constantPoolIndex + (constantPoolEntry != null ? " : tag=" + constantPoolEntry.getTag() : ""));
        }
    }

    @Override
    public void updateAfterMethodRename() {
    }

    @Override
    public void collectUsedConstants(char ba, int bb, UsedConstantsCollector usedConstantsCollector, char bc) {
        this.stringConstant.registerUsage(usedConstantsCollector, this, this.getParent());
    }

    @Override
    public void write(int ba, DataOutputStream dataOutputStream, int bb, int bc) throws IOException {
        dataOutputStream.writeByte(this.getTag());
        dataOutputStream.writeShort(this.stringConstant.getIndex());
    }
}

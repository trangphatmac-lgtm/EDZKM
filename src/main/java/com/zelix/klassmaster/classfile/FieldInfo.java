package com.zelix.klassmaster.classfile;

import com.zelix.klassmaster.classfile.attribute.Attribute;
import com.zelix.klassmaster.classfile.attribute.ConstantValueAttribute;
import com.zelix.klassmaster.classfile.attribute.SignatureAttribute;
import com.zelix.klassmaster.classfile.attribute.TypeAnnotationsAttribute;
import com.zelix.klassmaster.classfile.constpool.ConstantPoolEntry;
import com.zelix.klassmaster.classfile.constpool.ConstantUtf8;
import com.zelix.klassmaster.classfile.hierarchy.ClassHierarchyNode;
import com.zelix.klassmaster.classfile.hierarchy.ClassRepository;
import com.zelix.klassmaster.exceptions.ZkmException;
import com.zelix.klassmaster.exceptions.ZkmProcessingException;
import com.zelix.klassmaster.obfuscator.exclude.NameExclusionSet;
import com.zelix.klassmaster.obfuscator.rename.FieldNameAssigner;
import com.zelix.klassmaster.util.ListMultimap;
import com.zelix.klassmaster.util.SetMultiMap;
import com.zelix.klassmaster.util.ThreeKeyMultiMap;
import com.zelix.klassmaster.util.TwoKeyMap;
import com.zelix.klassmaster.util.ZkmAssert;
import com.zelix.klassmaster.util.ZkmStringUtils;
import com.zelix.klassmaster.util.ZkmUtils;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class FieldInfo extends AbstractFieldInfo {
    public boolean isIntCompatibleType() {
        String value = this.descriptorConstant.getValue();
        if (value.length() == 1) {
            switch (value.charAt(0)) {
                case 'B':
                case 'C':
                case 'I':
                case 'S':
                    return true;
                default:
                    return false;
            }
        } else {
            return false;
        }
    }

    @Override
    public boolean isStringType() {
        return this.descriptorConstant.getValue().equals("Ljava/lang/String;");
    }

    public boolean isLongType() {
        return this.descriptorConstant.getValue().equals("J");
    }

    public void stripGenericInfo(int ba) {
        if (ba == 1) {
            ArrayList arrayList = new ArrayList(this.attributes.length);

            for (int attributeIndex = 0; attributeIndex < this.attributes.length; attributeIndex++) {
                if (!(this.attributes[attributeIndex] instanceof SignatureAttribute)) {
                    if (this.attributes[attributeIndex] instanceof TypeAnnotationsAttribute) {
                        TypeAnnotationsAttribute typeAnnotationsAttribute = (TypeAnnotationsAttribute) this.attributes[attributeIndex];
                        typeAnnotationsAttribute.removeSignatureDependentAnnotations();
                        if (!typeAnnotationsAttribute.isEmpty()) {
                            arrayList.add(this.attributes[attributeIndex]);
                        }
                    } else {
                        arrayList.add(this.attributes[attributeIndex]);
                    }
                }
            }

            if (arrayList.size() < this.attributes.length) {
                Attribute[] attributes1 = new Attribute[arrayList.size()];
                this.attributes = ((com.zelix.klassmaster.classfile.attribute.Attribute[]) (arrayList.toArray(attributes1)));
                this.attributeCount = this.attributes.length;
            }

            if (this.isEnum()) {
                this.setEnum();
            }
        }
    }

    public void assignNewName(
            ClassHierarchyNode classHierarchyNode,
            FieldNameAssigner fieldNameAssigner,
            TwoKeyMap twoKeyMap,
            TwoKeyMap twoKeyMap1,
            Map map1,
            ListMultimap listMultimap,
            ListMultimap listMultimap1,
            Map map2,
            Map map3,
            String key,
            NameExclusionSet nameExclusionSet,
            Map map4,
            HashMap hashMap,
            HashMap hashMap1,
            SetMultiMap setMultiMap,
            TwoKeyMap twoKeyMap2,
            Map map5
    ) throws ZkmException, IOException {
        String sourceName = this.getSourceName();
        this.getTypeName();
        String nameValue;
        if (nameExclusionSet.isFieldExcluded(this)) {
            nameValue = sourceName;
        } else if (map3 != null && map3.containsKey(this)) {
            nameValue = (String) map3.get(this);
        } else if (map5.containsKey(this)) {
            nameValue = (String) map5.get(this);
            map1.put(nameValue, this);
            map2.put(nameValue, this);
            listMultimap1.addValue(this.getDescriptorKey(), nameValue);
        } else {
            String descriptorKey = this.getDescriptorKey();
            nameValue = fieldNameAssigner.assignFieldName(
                    classHierarchyNode, this, sourceName, descriptorKey, map1, listMultimap, listMultimap1, map2, this.isSynthetic(), map4, hashMap, setMultiMap, twoKeyMap2
            );
        }

        if (nameValue == null) {
            nameValue = sourceName;
        }

        FieldSignature fieldSignature2 = new FieldSignature(sourceName, this.getDescriptor());
        FieldSignature fieldSignature = new FieldSignature(nameValue, this.getDescriptor());
        FieldSignature fieldSignature3 = (FieldSignature) twoKeyMap.putValue(key, fieldSignature2, fieldSignature);
        FieldSignature fieldSignature1 = (FieldSignature) twoKeyMap1.putValue(key, fieldSignature, fieldSignature2);
        if (fieldSignature1 != null) {
            ZkmAssert.assertTrue(
                    false,
                    new String[]{
                            "Duplicate field name in class "
                                    + ZkmUtils.slashesToDots((String) ZkmUtils.mapOrSelf(key, hashMap1))
                                    + ". Both "
                                    + fieldSignature2.getName()
                                    + " and "
                                    + fieldSignature1.getName()
                                    + " renamed to '"
                                    + fieldSignature.getName()
                                    + "'."
                    }
            );
        }

        hashMap.put(this, nameValue);
        if (!nameValue.equals(sourceName)) {
            this.setName(nameValue);
        }
    }

    public static String toIndefiniteArticleName(String className) {
        int packageSeparator = className.lastIndexOf("/");
        String simpleName = packageSeparator != -1 ? className.substring(packageSeparator + 1) : className;
        String descriptiveName = ZkmStringUtils.replaceAll(simpleName, "[]", "Array");
        StringBuilder name = new StringBuilder();
        char initial = Character.toUpperCase(descriptiveName.charAt(0));
        if (initial == 'A' || initial == 'E' || initial == 'I' || initial == 'O' || initial == 'U') {
            name.append("an");
        } else {
            name.append("a");
        }
        name.append(initial + descriptiveName.substring(1));
        return name.toString();
    }

    public void replaceNamePrefix(String string, String string1, TwoKeyMap twoKeyMap, ProgramClass programClass1, ClassRepository classRepository1) throws ZkmException, IOException {
        String classNameValue = this.getClassName();
        String sourceName = this.getSourceName();
        String string4 = sourceName.substring(string.length());
        String nameValue = string1 + string4;
        if (classRepository1.hasFieldNamed(classNameValue, nameValue)) {
            StringBuffer stringBuffer = new StringBuffer();
            if (string4.length() > 0) {
                for (int characterIndex = string4.length() - 1; characterIndex >= 0; characterIndex += -1) {
                    char bb = string4.charAt(characterIndex);
                    if (Character.isDigit(bb)) {
                        char bc = bb;
                        stringBuffer.insert(0, bc);
                    }
                }
            }

            String string6 = nameValue.substring(0, nameValue.length() - stringBuffer.length());
            int bd = -1;
            if (stringBuffer.length() > 0) {
                try {
                    bd = Integer.parseInt(stringBuffer.toString());
                } catch (NumberFormatException numberFormatException) {
                }
            }

            do {
                nameValue = string6 + ++bd;
            } while (classRepository1.hasFieldNamed(classNameValue, nameValue));
        }

        this.setName(nameValue);
        classRepository1.reindexRenamedField(programClass1, this, sourceName);
        twoKeyMap.putValue(classNameValue, sourceName, nameValue);
    }

    public FieldInfo(
            ClassFileComponent classFileComponent,
            ClassFileInputStream classFileInputStream,
            ListMultimap listMultimap,
            ListMultimap listMultimap1,
            ListMultimap listMultimap2,
            ListMultimap listMultimap3,
            ListMultimap listMultimap4,
            ListMultimap listMultimap5,
            ListMultimap listMultimap6,
            PrintWriter printWriter,
            ThreeKeyMultiMap threeKeyMultiMap
    ) throws ZkmProcessingException, IOException {
        super(classFileComponent, classFileInputStream, listMultimap, printWriter);
        this.attributes = new Attribute[this.attributeCount];

        for (int attributeIndex = 0; attributeIndex < this.attributes.length; attributeIndex++) {
            Object object = null;
            PrintWriter printWriter1 = printWriter;
            this.attributes[attributeIndex] = Attribute.readAttribute(
                    this,
                    classFileInputStream,
                    listMultimap,
                    listMultimap1,
                    listMultimap2,
                    listMultimap3,
                    listMultimap4,
                    listMultimap5,
                    listMultimap6,
                    (ListMultimap) null,
                    printWriter1,
                    (ListMultimap) object,
                    threeKeyMultiMap
            );
        }

        if (ConstantPoolEntry.descriptorToJavaType(this.getDescriptor(), true) == null) {
            printWriter.println("ERROR: " + this.getLocationName() + " : " + "Invalid Field Descriptor");
            this.setValid(false);
        } else {
            this.setValid(true);
        }
    }

    @Override
    public ClassFileBase getOwningClass() {
        return this.getProgramClass();
    }

    public ProgramClass getProgramClass() {
        return (ProgramClass) this.getParent();
    }

    public FieldInfo(ClassFileBase classFileBase, ConstantUtf8 constantUtf8, ConstantUtf8 constantUtf81, Attribute[] attributes1, int creationKind) {
        super(classFileBase, constantUtf8, constantUtf81, attributes1, creationKind);

        for (int attributes1ElementIndex = 0; attributes1ElementIndex < attributes1.length; attributes1ElementIndex++) {
            attributes1[attributes1ElementIndex].setParent(this);
        }
    }

    public ProgramClass getTypeProgramClass() {
        String key = ClassFileBase.extractClassName(this.getDescriptor());
        return key != null ? ClassHierarchyNode.findProgramClass(key) : null;
    }

    @Override
    public boolean isProgramMember() {
        return true;
    }

    public boolean removeConstantValueAttribute() {
        ConstantValueAttribute constantValueAttribute = null;
        Attribute[] attributes1 = new Attribute[this.attributes.length - 1];
        int ba = 0;

        for (int attributeIndex = 0; attributeIndex < this.attributes.length; attributeIndex++) {
            if (this.attributes[attributeIndex] instanceof ConstantValueAttribute) {
                constantValueAttribute = (ConstantValueAttribute) this.attributes[attributeIndex];
            } else if (ba < attributes1.length) {
                attributes1[ba++] = this.attributes[attributeIndex];
            }
        }

        if (constantValueAttribute != null) {
            this.attributes = attributes1;
            this.attributeCount = this.attributes.length;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public ConstantValueAttribute getConstantValueAttribute() {
        for (int attributeIndex = 0; attributeIndex < this.attributes.length; attributeIndex++) {
            if (this.attributes[attributeIndex] instanceof ConstantValueAttribute) {
                return (ConstantValueAttribute) this.attributes[attributeIndex];
            }
        }

        return null;
    }
}

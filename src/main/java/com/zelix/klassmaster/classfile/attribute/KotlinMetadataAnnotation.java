package com.zelix.klassmaster.classfile.attribute;

import com.zelix.klassmaster.classfile.AbstractFieldInfo;
import com.zelix.klassmaster.classfile.AbstractMethodInfo;
import com.zelix.klassmaster.classfile.ClassFileBase;
import com.zelix.klassmaster.classfile.ClassFileComponent;
import com.zelix.klassmaster.classfile.ClassFileInputStream;
import com.zelix.klassmaster.classfile.MethodSignature;
import com.zelix.klassmaster.classfile.ProgramClass;
import com.zelix.klassmaster.classfile.constpool.ConstantPoolEntry;
import com.zelix.klassmaster.classfile.constpool.ConstantUtf8;
import com.zelix.klassmaster.classfile.constpool.UsedConstantsCollector;
import com.zelix.klassmaster.classfile.hierarchy.ClassResolver;
import com.zelix.klassmaster.config.HiddenOptionFlags;
import com.zelix.klassmaster.config.IgnoreMissingReferencesSpec;
import com.zelix.klassmaster.exceptions.ZkmException;
import com.zelix.klassmaster.script.ScriptEnvironment;
import com.zelix.klassmaster.util.BooleanFlag;
import com.zelix.klassmaster.util.ListMultimap;
import com.zelix.klassmaster.util.MutableInt;
import com.zelix.klassmaster.util.ZkmStringUtils;
import com.zelix.klassmaster.util.ZkmUtils;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class KotlinMetadataAnnotation extends AnnotationEntry {
    public int kind;
    public String[] data1;
    public String[] data2;
    public String extraString;
    public String packageName;
    public int[] metadataVersion;
    public int[] bytecodeVersion;
    public Integer extraInt;

    @Override
    public void collectReferencedClasses(Object object) {
        Set set1 = (Set) object;
        if (super.valid) {
            if (super.annotationClass != null) {
                set1.add(super.annotationClass);
            }

            int elementIndex = 0;
            int bb = 0;

            for (int elementCountValue = super.elementCount; bb < elementCountValue; elementCountValue = super.elementCount) {
                super.elements[elementIndex].collectReferencedClasses(set1);
                bb = ++elementIndex;
            }
        }
    }

    @Override
    public void writeRemapped(DataOutputStream dataOutputStream, Object object, Object object1) throws IOException {
        ScriptEnvironment scriptEnvironment1 = (ScriptEnvironment) object1;
        Map map1 = (Map) object;
        ConstantUtf8 constantUtf8 = (ConstantUtf8) map1.get(super.typeConstant);
        DataOutputStream dataOutputStream1;
        int elementCountValue;
        if (constantUtf8 != null) {
            dataOutputStream.writeShort(constantUtf8.getIndex());
            dataOutputStream1 = dataOutputStream;
            elementCountValue = super.elementCount;
        } else {
            dataOutputStream.writeShort(super.typeConstant.getIndex());
            dataOutputStream1 = dataOutputStream;
            elementCountValue = super.elementCount;
        }

        dataOutputStream1.writeShort(elementCountValue);
        int elementIndex = 0;
        int bb = 0;

        for (int elementCountValue2 = super.elementCount; bb < elementCountValue2; elementCountValue2 = super.elementCount) {
            super.elements[elementIndex].writeRemapped(dataOutputStream, map1, scriptEnvironment1);
            bb = ++elementIndex;
        }
    }

    @Override
    public int getByteLength() {
        return super.getByteLength();
    }

    @Override
    public void replaceUtf8Constant(ConstantUtf8 constantUtf8, ConstantUtf8 constantUtf81) {
        if (super.typeConstant == constantUtf8) {
            super.typeConstant = constantUtf81;
        }
    }

    @Override
    public ClassFileBase getAnnotationClass() {
        return super.annotationClass;
    }

    public KotlinMetadataAnnotation(
            ClassFileComponent classFileComponent, int ba, ConstantPoolEntry constantPoolEntry, ClassFileInputStream classFileInputStream, ListMultimap listMultimap
    ) throws IOException {
        super(classFileComponent, ba, constantPoolEntry, classFileInputStream, listMultimap);
        int elementIndex = 0;
        int bg = 0;

        for (int elementCountValue = super.elementCount; bg < elementCountValue; elementCountValue = super.elementCount) {
            AnnotationElementPair annotationElementPair = super.elements[elementIndex];
            AnnotationElementValue annotationElementValue = annotationElementPair.getValue();
            switch (annotationElementPair.getElementName()) {
                case "k":
                    this.kind = ((IntElementValue) annotationElementValue).getIntValue();
                    break;
                case "d1":
                    if (!annotationElementValue.isArrayOrClassValue()) {
                        break;
                    }

                    AnnotationElementValue[] annotationElementValues3 = ((NestedAnnotationElementValue) annotationElementValue).getElementValues();
                    this.data1 = new String[annotationElementValues3.length];

                    for (int j = 0; j < annotationElementValues3.length; j++) {
                        AnnotationElementValue annotationElementValue4 = annotationElementValues3[j];
                        if (annotationElementValue4 instanceof StringElementValue) {
                            StringElementValue stringElementValue3 = (StringElementValue) annotationElementValue4;
                            this.data1[j] = stringElementValue3.getStringValue();
                        }
                    }
                    break;
                case "d2":
                    if (!annotationElementValue.isArrayOrClassValue()) {
                        break;
                    }

                    AnnotationElementValue[] annotationElementValues2 = ((NestedAnnotationElementValue) annotationElementValue).getElementValues();
                    this.data2 = new String[annotationElementValues2.length];

                    for (int j = 0; j < annotationElementValues2.length; j++) {
                        AnnotationElementValue annotationElementValue3 = annotationElementValues2[j];
                        if (annotationElementValue3 instanceof StringElementValue) {
                            StringElementValue stringElementValue2 = (StringElementValue) annotationElementValue3;
                            this.data2[j] = stringElementValue2.getStringValue();
                        }
                    }
                    break;
                case "xs":
                    if (annotationElementValue instanceof StringElementValue) {
                        StringElementValue stringElementValue1 = (StringElementValue) annotationElementValue;
                        this.extraString = stringElementValue1.getStringValue();
                    }
                    break;
                case "pn":
                    if (annotationElementValue instanceof StringElementValue) {
                        StringElementValue stringElementValue = (StringElementValue) annotationElementValue;
                        this.packageName = stringElementValue.getStringValue();
                    }
                    break;
                case "mv":
                    if (!annotationElementValue.isArrayOrClassValue()) {
                        break;
                    }

                    AnnotationElementValue[] annotationElementValues1 = ((NestedAnnotationElementValue) annotationElementValue).getElementValues();
                    this.metadataVersion = new int[annotationElementValues1.length];

                    for (int j = 0; j < annotationElementValues1.length; j++) {
                        AnnotationElementValue annotationElementValue2 = annotationElementValues1[j];
                        if (annotationElementValue2 instanceof IntElementValue) {
                            IntElementValue intElementValue2 = (IntElementValue) annotationElementValue2;
                            this.metadataVersion[j] = intElementValue2.getIntValue();
                        }
                    }
                    break;
                case "bv":
                    if (!annotationElementValue.isArrayOrClassValue()) {
                        break;
                    }

                    AnnotationElementValue[] annotationElementValues = ((NestedAnnotationElementValue) annotationElementValue).getElementValues();
                    this.bytecodeVersion = new int[annotationElementValues.length];

                    for (int j = 0; j < annotationElementValues.length; j++) {
                        AnnotationElementValue annotationElementValue1 = annotationElementValues[j];
                        if (annotationElementValue1 instanceof IntElementValue) {
                            IntElementValue intElementValue1 = (IntElementValue) annotationElementValue1;
                            this.bytecodeVersion[j] = intElementValue1.getIntValue();
                        }
                    }
                    break;
                case "xi":
                    if (annotationElementValue instanceof IntElementValue) {
                        IntElementValue intElementValue = (IntElementValue) annotationElementValue;
                        this.extraInt = intElementValue.getIntValue();
                    }
            }

            bg = ++elementIndex;
        }
    }

    @Override
    public boolean isValid() {
        return super.isValid();
    }

    @Override
    public void collectReferencedMembers(Set set1, Set set2, Set set3, Set set4) {
        if (super.annotationClass != null && super.annotationClass.isProgramClass()) {
            set1.add((ProgramClass) super.annotationClass);
        }

        int elementIndex = 0;
        int elementIndexValue = elementIndex;

        for (int elementCountValue = super.elementCount; elementIndexValue < elementCountValue; elementCountValue = super.elementCount) {
            super.elements[elementIndex].collectReferencedMembers(super.annotationClass, set1, set2, set3, set4);
            elementIndexValue = ++elementIndex;
        }
    }

    @Override
    public String getElementClassValue(String string, boolean bl) {
        int elementIndex = 0;
        int bb = 0;

        for (int elementCountValue = super.elementCount; bb < elementCountValue; elementCountValue = super.elementCount) {
            String string1;
            if (bl) {
                string1 = super.elements[elementIndex].getResolvedElementName();
            } else {
                string1 = super.elements[elementIndex].getElementName();
            }

            if (string.equals(string1)) {
                return super.elements[elementIndex].getClassValueName();
            }

            bb = ++elementIndex;
        }

        return null;
    }

    @Override
    public String getTypeName() {
        if (super.valid) {
            String value = super.typeConstant.getValue();
            return value.substring(1, value.length() - 1);
        } else {
            return null;
        }
    }

    @Override
    public String getErrorMessage(Object object, Object object1, Object object2) {
        int ba = (Integer) object1;
        int bb = (Integer) object2;
        long bg = (long) ((Integer) object).intValue() << 48 | (long) ba << 32 >>> 16;
        long bh = (bg | (long) bb << 48 >>> 48) ^ 0L;
        int bc = (int) (((bg | (long) bb << 48 >>> 48) ^ 0L) >>> 48);
        int bd = (int) (((bg | (long) bb << 48 >>> 48) ^ 0L) << 16 >>> 32);
        int be = (int) (bh << 48 >>> 48);
        char bf = (char) bc;
        Integer integer1 = Integer.valueOf((char) be);
        Integer integer = bd;
        return super.getErrorMessage(Integer.valueOf(bf), integer, integer1);
    }

    @Override
    public String getTypeDescriptor() {
        return super.typeConstant.getValue();
    }

    @Override
    public void updateAfterClassRename(Object object1, Object object) {
        HashMap hashMap = (HashMap) object;
        if (super.annotationClass != null) {
            String string = ClassFileBase.extractClassName(super.typeConstant.getValue());
            if (!super.annotationClass.getClassName().equals(string)) {
                String value = ClassFileBase.toTypeDescriptor(super.annotationClass.getClassName(), super.arrayDimensions);
                super.typeConstant.setValue(value);
            }

            this.getOwningClass();
            int intValueValue = 0;
            int elementIndex = 0;
            int be = 0;

            for (int elementCountValue = super.elementCount; be < elementCountValue; elementCountValue = super.elementCount) {
                AnnotationElementPair annotationElementPair = super.elements[elementIndex];
                AnnotationElementValue annotationElementValue = annotationElementPair.getValue();
                switch (annotationElementPair.getElementName()) {
                    case "k":
                        intValueValue = ((IntElementValue) annotationElementValue).getIntValue();
                        break;
                    case "d1":
                        if (intValueValue != 4 || !annotationElementValue.isArrayOrClassValue()) {
                            break;
                        }

                        AnnotationElementValue[] annotationElementValues1 = ((NestedAnnotationElementValue) annotationElementValue).getElementValues();

                        for (int annotationElementValues1ElementIndex = 0; annotationElementValues1ElementIndex < annotationElementValues1.length; annotationElementValues1ElementIndex++) {
                            AnnotationElementValue annotationElementValue2 = annotationElementValues1[annotationElementValues1ElementIndex];
                            if (annotationElementValue2 instanceof StringElementValue) {
                                StringElementValue stringElementValue2 = (StringElementValue) annotationElementValue2;
                                String stringValue = stringElementValue2.getStringValue();
                                if (ZkmStringUtils.countChar(stringValue, '/') >= 1) {
                                    BooleanFlag booleanFlag2 = new BooleanFlag();
                                    String stringValueValue = ConstantPoolEntry.remapClassNameString(stringValue, hashMap, booleanFlag2);
                                    if (booleanFlag2.getValue() && !stringValueValue.equals(stringValue)) {
                                        stringElementValue2.setStringValue(stringValueValue);
                                    }
                                }
                            }
                        }
                        break;
                    case "d2":
                        if (!annotationElementValue.isArrayOrClassValue()) {
                            break;
                        }

                        AnnotationElementValue[] annotationElementValues = ((NestedAnnotationElementValue) annotationElementValue).getElementValues();
                        int annotationElementValueIndex = 0;

                        for (; annotationElementValueIndex < annotationElementValues.length; annotationElementValueIndex++) {
                            AnnotationElementValue annotationElementValue1 = annotationElementValues[annotationElementValueIndex];
                            if (annotationElementValue1 instanceof StringElementValue) {
                                StringElementValue stringElementValue1 = (StringElementValue) annotationElementValue1;
                                String stringValueValue2 = stringElementValue1.getStringValue();
                                if (stringValueValue2.startsWith("(") && stringValueValue2.indexOf(41) < stringValueValue2.length() && ZkmStringUtils.countChar(stringValueValue2, ')') == 1) {
                                    String parameterPart = MethodSignature.getParameterPart(stringValueValue2);
                                    String returnPart = MethodSignature.getReturnPart(stringValueValue2);
                                    String string6 = '(' + ConstantPoolEntry.remapDescriptorClassNames(parameterPart.substring(1, parameterPart.length() - 1), hashMap) + ')';
                                    String string7 = ConstantPoolEntry.remapClassNameString(returnPart, hashMap, new BooleanFlag());
                                    if (!string6.equals(parameterPart) || !string7.equals(returnPart)) {
                                        stringElementValue1.setStringValue(string6 + string7);
                                    }
                                } else if (stringValueValue2.startsWith("L") && stringValueValue2.endsWith(";") || ZkmStringUtils.countChar(stringValueValue2, '/') >= 1) {
                                    BooleanFlag booleanFlag1 = new BooleanFlag();
                                    String stringValueValue3 = ConstantPoolEntry.remapClassNameString(stringValueValue2, hashMap, booleanFlag1);
                                    if (booleanFlag1.getValue() && !stringValueValue3.equals(stringValueValue2)) {
                                        stringElementValue1.setStringValue(stringValueValue3);
                                    }
                                }
                            }
                        }
                        break;
                    case "xs":
                        if (annotationElementValue instanceof StringElementValue) {
                            StringElementValue stringElementValue = (StringElementValue) annotationElementValue;
                            String stringValueValue4 = stringElementValue.getStringValue();
                            if (ZkmStringUtils.countChar(stringValueValue4, '/') >= 1) {
                                BooleanFlag booleanFlag = new BooleanFlag();
                                String stringValueValue5 = ConstantPoolEntry.remapClassNameString(stringValueValue4, hashMap, booleanFlag);
                                if (booleanFlag.getValue() && !stringValueValue5.equals(stringValueValue4)) {
                                    stringElementValue.setStringValue(stringValueValue5);
                                }
                            }
                        }
                    case "mv":
                    case "bv":
                    case "pn":
                    case "xi":
                }

                be = ++elementIndex;
            }
        }
    }

    @Override
    public void updateAfterFieldRename() {
        if (super.annotationClass != null && super.annotationClass.getOriginalClassName().equals("kotlin/Metadata")) {
            ClassFileBase classFileBase = this.getOwningClass();
            AbstractMethodInfo[] abstractMethodInfos = classFileBase.getDeclaredMethods();
            AbstractFieldInfo[] abstractFieldInfos = classFileBase.getFields();
            HashSet hashSet = ZkmUtils.createHashSet(ZkmUtils.getPrimeCapacity(abstractFieldInfos.length));

            for (AbstractMethodInfo abstractMethodInfo : abstractMethodInfos) {
                hashSet.add(abstractMethodInfo.getOriginalMemberName());
            }

            ListMultimap listMultimap = new ListMultimap(abstractFieldInfos.length);

            for (AbstractFieldInfo abstractFieldInfo : abstractFieldInfos) {
                listMultimap.addValue(abstractFieldInfo.getOriginalMemberName(), abstractFieldInfo);
            }

            int elementIndex = 0;
            int bc = 0;

            for (int elementCountValue = super.elementCount; bc < elementCountValue; elementCountValue = super.elementCount) {
                AnnotationElementPair annotationElementPair = super.elements[elementIndex];
                if (annotationElementPair.getElementName().equals("d2")) {
                    NestedAnnotationElementValue nestedAnnotationElementValue = (NestedAnnotationElementValue) annotationElementPair.getValue();

                    for (AnnotationElementValue annotationElementValue : nestedAnnotationElementValue.getElementValues()) {
                        if (annotationElementValue instanceof StringElementValue) {
                            StringElementValue stringElementValue = (StringElementValue) annotationElementValue;
                            String stringValue = stringElementValue.getStringValue();
                            List list1 = listMultimap.getValues(stringValue);
                            if (list1 != null && !hashSet.contains(stringValue)) {
                                boolean bl = true;
                                String sourceName = null;
                                Iterator iterator = list1.iterator();

                                while (iterator.hasNext()) {
                                    AbstractFieldInfo abstractFieldInfo1 = (AbstractFieldInfo) iterator.next();
                                    if (sourceName == null) {
                                        sourceName = abstractFieldInfo1.getSourceName();
                                    } else if (!sourceName.equals(abstractFieldInfo1.getSourceName())) {
                                        bl = false;
                                        break;
                                    }
                                }

                                if (bl && !stringValue.equals(sourceName)) {
                                    stringElementValue.setStringValue(sourceName);
                                }
                            }
                        }
                    }
                }

                bc = ++elementIndex;
            }
        } else {
            int elementIndexValue = 0;
            int elementIndexValue2 = elementIndexValue;

            for (int elementCountValue2 = super.elementCount; elementIndexValue2 < elementCountValue2; elementCountValue2 = super.elementCount) {
                super.elements[elementIndexValue].updateAfterFieldRename();
                elementIndexValue2 = ++elementIndexValue;
            }
        }
    }

    @Override
    public String getResolvedTypeName() {
        if (super.valid) {
            return super.annotationClass != null ? super.annotationClass.getOriginalClassName() : this.getTypeName();
        } else {
            return null;
        }
    }

    @Override
    public void updateAfterMethodRename() {
        if ((HiddenOptionFlags.TRANSLATE_KOTLIN || HiddenOptionFlags.TRANSLATE_KOTLIN_METADATA)
                && super.annotationClass != null
                && super.annotationClass.getOriginalClassName().equals("kotlin/Metadata")) {
            ClassFileBase classFileBase = this.getOwningClass();
            AbstractFieldInfo[] abstractFieldInfos = classFileBase.getFields();
            HashSet hashSet = ZkmUtils.createHashSet(ZkmUtils.getPrimeCapacity(abstractFieldInfos.length));

            for (AbstractFieldInfo abstractFieldInfo : abstractFieldInfos) {
                hashSet.add(abstractFieldInfo.getOriginalMemberName());
            }

            AbstractMethodInfo[] abstractMethodInfos = classFileBase.getDeclaredMethods();
            ListMultimap listMultimap = new ListMultimap(abstractMethodInfos.length);

            for (AbstractMethodInfo abstractMethodInfo : abstractMethodInfos) {
                listMultimap.addValue(abstractMethodInfo.getOriginalMemberName(), abstractMethodInfo);
            }

            int elementIndex = 0;
            int bc = 0;

            for (int elementCountValue = super.elementCount; bc < elementCountValue; elementCountValue = super.elementCount) {
                AnnotationElementPair annotationElementPair = super.elements[elementIndex];
                if (annotationElementPair.getElementName().equals("d2")) {
                    AnnotationElementValue annotationElementValue1 = annotationElementPair.getValue();
                    if (annotationElementValue1.isArrayOrClassValue()) {
                        NestedAnnotationElementValue nestedAnnotationElementValue = (NestedAnnotationElementValue) annotationElementValue1;

                        for (AnnotationElementValue annotationElementValue : nestedAnnotationElementValue.getElementValues()) {
                            if (annotationElementValue instanceof StringElementValue) {
                                StringElementValue stringElementValue = (StringElementValue) annotationElementValue;
                                String stringValue = stringElementValue.getStringValue();
                                List list1 = listMultimap.getValues(stringValue);
                                if (list1 != null && !hashSet.contains(stringValue)) {
                                    boolean bl = true;
                                    String sourceName = null;
                                    Iterator iterator = list1.iterator();

                                    while (iterator.hasNext()) {
                                        AbstractMethodInfo abstractMethodInfo1 = (AbstractMethodInfo) iterator.next();
                                        if (sourceName == null) {
                                            sourceName = abstractMethodInfo1.getSourceName();
                                        } else if (!sourceName.equals(abstractMethodInfo1.getSourceName())) {
                                            bl = false;
                                            break;
                                        }
                                    }

                                    if (bl && !stringValue.equals(sourceName)) {
                                        stringElementValue.setStringValue(sourceName);
                                    }
                                }
                            }
                        }
                    }
                }

                bc = ++elementIndex;
            }
        } else {
            int elementIndexValue = 0;
            int elementIndexValue2 = elementIndexValue;

            for (int elementCountValue2 = super.elementCount; elementIndexValue2 < elementCountValue2; elementCountValue2 = super.elementCount) {
                super.elements[elementIndexValue].updateAfterMethodRename();
                elementIndexValue2 = ++elementIndexValue;
            }
        }
    }

    @Override
    public void resolveReferences(ClassResolver classResolver1, IgnoreMissingReferencesSpec ignoreMissingReferencesSpec1, ScriptEnvironment scriptEnvironment1) throws ZkmException, IOException {
        MutableInt mutableInt = new MutableInt(0);
        String key = ClassFileBase.extractClassName(super.typeConstant.getValue(), mutableInt);
        super.arrayDimensions = mutableInt.getValue();
        if (key != null) {
            super.annotationClass = classResolver1.getClassFile(
                    key, "analyzing annotations in class '" + this.getDisplayLocationName() + "' (C)", ignoreMissingReferencesSpec1
            );
        }

        int elementIndex = 0;
        int bb = 0;

        for (int elementCountValue = super.elementCount; bb < elementCountValue; elementCountValue = super.elementCount) {
            super.elements[elementIndex].resolveReferences(super.annotationClass, classResolver1, ignoreMissingReferencesSpec1, scriptEnvironment1);
            bb = ++elementIndex;
        }
    }

    @Override
    public void collectUsedConstants(char bd, int be, UsedConstantsCollector usedConstantsCollector, char bf) {
        if (super.valid) {
            super.typeConstant.registerUsage(usedConstantsCollector, this, this.getParent());
            int elementIndex = 0;
            int bb = 0;

            for (int elementCountValue = super.elementCount; bb < elementCountValue; elementCountValue = super.elementCount) {
                super.elements[elementIndex].collectUsedConstants('\u0000', 1103830477, usedConstantsCollector, '鞍');
                bb = ++elementIndex;
            }
        }
    }

    @Override
    public void write(int ba, DataOutputStream dataOutputStream, int bb, int bc) throws IOException {
        long bj = ((long) ba << 32 | (long) bb << 48 >>> 32 | (long) bc << 48 >>> 48) ^ 0L;
        int bd = (int) ((((long) ba << 32 | (long) bb << 48 >>> 32 | (long) bc << 48 >>> 48) ^ 0L) >>> 32);
        int be = (int) ((((long) ba << 32 | (long) bb << 48 >>> 32 | (long) bc << 48 >>> 48) ^ 0L) << 32 >>> 48);
        int bf = (int) (bj << 48 >>> 48);
        dataOutputStream.writeShort(super.typeConstant.getIndex());
        dataOutputStream.writeShort(super.elementCount);
        int elementIndex = 0;
        int bi = 0;

        for (int elementCountValue = super.elementCount; bi < elementCountValue; elementCountValue = super.elementCount) {
            super.elements[elementIndex].write(bd, dataOutputStream, (char) be, (char) bf);
            bi = ++elementIndex;
        }
    }
}

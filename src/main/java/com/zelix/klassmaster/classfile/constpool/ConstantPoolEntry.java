package com.zelix.klassmaster.classfile.constpool;

import com.zelix.klassmaster.classfile.ClassFileBase;
import com.zelix.klassmaster.classfile.ClassFileInputStream;
import com.zelix.klassmaster.classfile.MethodSignature;
import com.zelix.klassmaster.classfile.ProgramClass;
import com.zelix.klassmaster.classfile.hierarchy.ClassHierarchyNode;
import com.zelix.klassmaster.exceptions.ClassFileFormatException;
import com.zelix.klassmaster.util.BooleanFlag;
import com.zelix.klassmaster.util.ZkmAssert;
import com.zelix.klassmaster.util.ZkmStringUtils;
import com.zelix.klassmaster.util.ZkmUtils;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

public abstract class ConstantPoolEntry implements Comparable {
    private static int flowKey;
    public static final Map PRIMITIVE_TYPE_NAMES;
    public static final Map PRIMITIVE_WRAPPER_CLASSES;
    public static final Map WRAPPER_TO_PRIMITIVE;
    public int index;
    public AbstractConstantPool constantPool;

    public boolean isLongOrDouble() {
        ConstantPoolTag constantPoolTag = this.getTag();
        return constantPoolTag.equals(ConstantPoolTag.LONG) || constantPoolTag.equals(ConstantPoolTag.DOUBLE);
    }

    @Override
    public int compareTo(Object other) {
        return this.compareIndex((ConstantPoolEntry) other);
    }

    @Override
    public final boolean equals(Object other) {
        return super.equals(other);
    }

    public static ConstantPoolEntry readEntry(int indexArgument, ClassFileInputStream classFileInputStream, ConstantPool constantPool1) throws ClassFileFormatException, IOException {
        int bb = classFileInputStream.readUnsignedByte();
        switch (bb) {
            case 1:
                return new ConstantUtf8(indexArgument, classFileInputStream, constantPool1);
            case 2:
            case 13:
            case 14:
            default:
                throw new ClassFileFormatException(
                        "Class file '" + constantPool1.getClassLocation() + "' is corrupt. Tag '" + bb + "' at " + indexArgument + " is invalid (1)"
                );
            case 3:
                return new ConstantInteger(indexArgument, classFileInputStream, constantPool1);
            case 4:
                return new ConstantFloat(indexArgument, classFileInputStream, constantPool1);
            case 5:
                return new ConstantLong(indexArgument, classFileInputStream, constantPool1);
            case 6:
                return new ConstantDouble(indexArgument, classFileInputStream, constantPool1);
            case 7:
                return new ConstantClass(indexArgument, classFileInputStream, constantPool1);
            case 8:
                return new ConstantString(indexArgument, classFileInputStream, constantPool1);
            case 9:
                return new ConstantFieldref(indexArgument, classFileInputStream, constantPool1);
            case 10:
                return new ConstantMethodref(indexArgument, classFileInputStream, constantPool1);
            case 11:
                return new ConstantInterfaceMethodref(indexArgument, classFileInputStream, constantPool1);
            case 12:
                return new ConstantNameAndType(indexArgument, classFileInputStream, constantPool1);
            case 15:
                return new ConstantMethodHandle(indexArgument, classFileInputStream, constantPool1);
            case 16:
                return new ConstantMethodType(indexArgument, classFileInputStream, constantPool1);
            case 17:
                return new ConstantDynamic(indexArgument, classFileInputStream, constantPool1);
            case 18:
                return new ConstantInvokeDynamic(indexArgument, classFileInputStream, constantPool1);
            case 19:
                return new ConstantModule(indexArgument, classFileInputStream, constantPool1);
            case 20:
                return new ConstantPackage(indexArgument, classFileInputStream, constantPool1);
        }
    }

    public static boolean isMethodDescriptor(String string) {
        return string.charAt(0) == '(' && string.indexOf(")") > 0;
    }

    public static String getReturnDescriptor(String string) {
        return string.substring(string.indexOf(")") + 1);
    }

    public static String getWrapperClassName(Object key) {
        return (String) PRIMITIVE_WRAPPER_CLASSES.get(key);
    }

    public abstract ConstantPoolTag getTag();

    public int compareIndex(ConstantPoolEntry constantPoolEntry1) {
        if (this.index < constantPoolEntry1.index) {
            return -1;
        } else {
            return this.index == constantPoolEntry1.index ? 0 : 1;
        }
    }

    public static String getTagName(ConstantPoolTag constantPoolTag) {
        switch (ConstantTagOrdinalSwitchMap.TAG_SWITCH_TABLE[constantPoolTag.ordinal()]) {
            case 1:
                return "CONSTANT_Utf8";
            case 2:
                return "CONSTANT_Integer";
            case 3:
                return "CONSTANT_Float";
            case 4:
                return "CONSTANT_Long";
            case 5:
                return "CONSTANT_Double";
            case 6:
                return "CONSTANT_Class";
            case 7:
                return "CONSTANT_String";
            case 8:
                return "CONSTANT_Fieldref";
            case 9:
                return "CONSTANT_Methodref";
            case 10:
                return "CONSTANT_InterfaceMethodref";
            case 11:
                return "CONSTANT_NameAndType";
            case 12:
                return "CONSTANT_MethodHandle";
            case 13:
                return "CONSTANT_MethodType";
            case 14:
                return "CONSTANT_Dynamic";
            case 15:
                return "CONSTANT_InvokeDynamic";
            case 16:
                return "CONSTANT_Module";
            case 17:
                return "CONSTANT_Package";
            default:
                return "";
        }
    }

    public static String getPrimitiveTypeName(Object key) {
        return (String) PRIMITIVE_TYPE_NAMES.get(key);
    }

    public static String getStackWrapperClassName(String string) {
        String key = string;
        Map map1;
        switch (key.charAt(0)) {
            case 'B':
            case 'C':
            case 'S':
                key = "I";
                map1 = PRIMITIVE_WRAPPER_CLASSES;
                break;
            default:
                map1 = PRIMITIVE_WRAPPER_CLASSES;
        }

        return (String) map1.get(key);
    }

    public static int countParameterSlots(List list1) {
        int ba = 0;
        Iterator iterator = list1.iterator();

        while (iterator.hasNext()) {
            if (isWideType((String) iterator.next())) {
                ba += 2;
            } else {
                ba++;
            }
        }

        return ba;
    }

    public static final String formatMemberSignature(String string, String descriptorArgument) {
        StringBuilder stringBuilder = new StringBuilder();
        int ba = descriptorArgument.indexOf(41);
        if (ba > -1) {
            stringBuilder.append(descriptorToJavaType(descriptorArgument.substring(ba + 1)));
            stringBuilder.append(' ');
            stringBuilder.append(string);
            stringBuilder.append('(');
            List list1 = getParameterJavaTypes(descriptorArgument);
            int list1Size = list1.size();

            for (int elementIndex = 0; elementIndex < list1Size; elementIndex++) {
                stringBuilder.append((String) list1.get(elementIndex));
                if (elementIndex < list1Size - 1) {
                    stringBuilder.append(", ");
                }
            }

            stringBuilder.append(')');
        } else {
            stringBuilder.append(descriptorToJavaType(descriptorArgument));
            stringBuilder.append(' ');
            stringBuilder.append(string);
        }

        return stringBuilder.toString();
    }

    public static List getReferencedProgramClasses(String descriptorArgument) {
        ArrayList arrayList = new ArrayList();
        List list1 = getParameterAndReturnTypes(descriptorArgument);

        for (int elementIndex = 0; elementIndex < list1.size(); elementIndex++) {
            ProgramClass programClass1 = findProgramClassByDescriptor((String) list1.get(elementIndex));
            if (programClass1 != null) {
                arrayList.add(programClass1);
            }
        }

        return arrayList;
    }

    public final int getIndex() {
        return this.index;
    }

    public static List parseParameterTypes(String descriptor, boolean validate) {
        int flowSeed = ConstantPoolTag.getFlowSeed();
        String parameterPart = MethodSignature.getParameterPart(descriptor);
        if (flowSeed == 0 && validate) {
            if (parameterPart.length() < 2 || parameterPart.charAt(0) != '('
                    || parameterPart.charAt(parameterPart.length() - 1) != ')') {
                return null;
            }
        }

        ZkmAssert.assertTrue(flowSeed == 0 ? parameterPart.length() >= 2 : validate,
                new String[]{"'" + parameterPart + "' : '" + descriptor + "'"});
        String remainingTypes = parameterPart.substring(1, parameterPart.indexOf(41));
        ArrayList parameterTypes = new ArrayList();
        if (flowSeed != 0) {
            if (remainingTypes.length() == 0) {
                parameterTypes.trimToSize();
            }
            return parameterTypes;
        }

        while (remainingTypes.length() != 0) {
            int elementTypeIndex = 0;
            while (remainingTypes.charAt(elementTypeIndex) == '[') {
                elementTypeIndex++;
            }

            int typeEnd = elementTypeIndex;
            if (remainingTypes.charAt(elementTypeIndex) == 'L') {
                typeEnd = remainingTypes.indexOf(59);
                if (validate && (typeEnd == -1 || typeEnd == elementTypeIndex + 1)) {
                    return null;
                }
            }

            String parameterType = remainingTypes.substring(0, typeEnd + 1);
            parameterTypes.add(parameterType);
            if (validate && parameterType.length() == 1 && PRIMITIVE_TYPE_NAMES.get(parameterType) == null) {
                return null;
            }
            remainingTypes = typeEnd == remainingTypes.length() - 1 ? "" : remainingTypes.substring(typeEnd + 1);
        }

        parameterTypes.trimToSize();
        return parameterTypes;
    }

    public static List getParameterClassForNames(String descriptorArgument) {
        List list1 = getParameterTypes(descriptorArgument);
        int list1Size = list1.size();

        for (int elementIndex = 0; elementIndex < list1Size; elementIndex++) {
            String string1 = (String) list1.get(elementIndex);
            list1.set(elementIndex, toClassForNameFormat(string1));
        }

        return list1;
    }

    public abstract void writeTo(DataOutputStream dataOutputStream) throws IOException;

    public boolean isUnresolved() {
        return false;
    }

    public static String descriptorToJavaType(String descriptorArgument, boolean validateArgument) {
        return descriptorToJavaType(descriptorArgument, validateArgument, true);
    }

    public String getDisplayString() {
        return this.getValueString();
    }

    public boolean isWellFormed() {
        return true;
    }

    public static String stripClassDescriptor(String string) {
        int ba = string.length();
        return ba > 2 && string.charAt(0) == 'L' && string.charAt(ba - 1) == ';' ? string.substring(1, ba - 1) : string;
    }

    public static boolean isWideType(String string) {
        return string.equals("J") || string.equals("D");
    }

    public static int getCommaChar() {
        return 44;
    }

    public ConstantPoolEntry(int index, AbstractConstantPool abstractConstantPool) {
        this.index = index;
        this.constantPool = abstractConstantPool;
    }

    public final ClassFileBase getOwnerClass() {
        return this.constantPool.getClassFile();
    }

    public static String remapDescriptorClassNames(String string, HashMap hashMap) {
        int fromIndex = 0;
        String string4 = string;

        for (byte searchCharacter = 76; (fromIndex = string4.indexOf(searchCharacter, fromIndex)) != -1; searchCharacter = 76) {
            int fromIndexValue = string.indexOf(59, fromIndex);
            if (fromIndexValue == -1) {
                break;
            }

            String string1 = string.substring(fromIndex + 1, fromIndexValue);
            String string2 = null;
            if (string1.indexOf(46) > 0 && string1.lastIndexOf(46) < string1.length() && string1.indexOf(47) == -1) {
                string2 = string1.replace('.', '/');
            }

            String string3 = (String) hashMap.get(string2 == null ? string1 : string2);
            if (string3 != null) {
                if (string2 != null) {
                    string3 = string3.replace('/', '.');
                }

                if (!string3.equals(string1)) {
                    string = string.substring(0, fromIndex + 1) + string3 + string.substring(fromIndexValue);
                    fromIndexValue = fromIndexValue + string3.length() - string1.length();
                }
            }

            fromIndex = fromIndexValue;
            string4 = string;
        }

        return string;
    }

    public static List getParameterTypes(String descriptorArgument) {
        return parseParameterTypes(descriptorArgument, false);
    }

    public final void setIndex(int index) {
        this.index = index;
    }

    public static boolean isFieldDescriptor(String string) {
        if (string.indexOf(",") <= -1 && string.indexOf(93) <= -1 && string.indexOf(46) <= -1 && string.indexOf(" ") <= -1) {
            int ba = 0;

            while (string.charAt(ba) == '[') {
                ba++;
            }

            String key = string.substring(ba);
            if (key.startsWith("L")) {
                return key.endsWith(";") || key.length() > 2;
            } else {
                return ((key.length() == 1) && (PRIMITIVE_TYPE_NAMES.containsKey(key)));
            }
        } else {
            return false;
        }
    }

    public static List getParameterAndReturnTypes(String descriptorArgument) {
        List list1 = getParameterTypes(descriptorArgument);
        String returnDescriptor = getReturnDescriptor(descriptorArgument);
        list1.add(returnDescriptor);
        return list1;
    }

    public static String getWrapperPrimitiveDescriptor(Object key) {
        return (String) WRAPPER_TO_PRIMITIVE.get(key);
    }

    public void writeRemappedTo(DataOutputStream dataOutputStream, Map map1) throws IOException {
        this.writeTo(dataOutputStream);
    }

    public static List getStackParameterTypes(ResolvedNameAndType resolvedNameAndType) {
        List list1 = getParameterTypes(resolvedNameAndType.getDescriptor());
        int list1Size = list1.size();

        for (int elementIndex = 0; elementIndex < list1Size; elementIndex++) {
            String string = (String) list1.get(elementIndex);
            if (string.equals("B") || string.equals("C") || string.equals("S") || string.equals("Z")) {
                list1.set(elementIndex, "I");
            }
        }

        return list1;
    }

    public static String toDescriptor(String key) {
        if (key.length() == 1) {
            return isPrimitiveDescriptor(key) ? key : "L" + key + ";";
        }

        int endIndex = key.indexOf("[]");
        String keyValue;
        String keyValue2;
        byte searchCharacter;
        if (endIndex > -1) {
            keyValue = key.substring(0, endIndex);
            keyValue2 = keyValue;
            searchCharacter = 91;
        } else {
            keyValue = key;
            keyValue2 = keyValue;
            searchCharacter = 91;
        }

        int bb = keyValue2.lastIndexOf(searchCharacter);
        if (bb > -1) {
            keyValue = keyValue.substring(bb + 1);
        }

        if (keyValue.length() == 1) {
            if (!isPrimitiveDescriptor(keyValue)) {
                keyValue = "L" + keyValue + ";";
            }
        } else {
            keyValue = keyValue.replace('.', '/');
            if (!keyValue.startsWith("L") || !keyValue.endsWith(";")) {
                keyValue = "L" + keyValue + ";";
            }
        }

        int bc = 0;
        if (endIndex > -1) {
            bc = ZkmStringUtils.countOccurrences(key, "[]");
        } else if (bb > -1) {
            bc = bb + 1;
        }

        for (int i = 0; i < bc; i++) {
            keyValue = '[' + keyValue;
        }

        return keyValue;
    }

    public static List getParameterJavaTypes(String descriptorArgument) {
        java.util.List list2 = null;
        int flowSeed = ConstantPoolTag.getFlowSeed();
        List list1 = getParameterTypes(descriptorArgument);
        int flowSeedValue = flowSeed;
        int list1Size = list1.size();
        int elementIndex = 0;

        while (true) {
            if (elementIndex < list1Size) {
                list2 = list1;
                if (flowSeedValue != 0) {
                    break;
                }

                String descriptorValue = (String) list1.get(elementIndex);
                list1.set(elementIndex, descriptorToJavaType(descriptorValue));
                elementIndex++;
                if (flowSeedValue == 0) {
                    continue;
                }
            }

            list2 = list1;
            break;
        }

        return list2;
    }

    public final String getOwnerJavaName() {
        return this.constantPool.getExternalClassName();
    }

    public static String descriptorToJavaType(String descriptor, boolean validate, boolean useDottedNames) {
        int flowSeed = ConstantPoolTag.getFlowSeed();
        int parameterEnd = descriptor.indexOf(")");
        String typeDescriptor = descriptor;
        if (parameterEnd > -1) {
            typeDescriptor = descriptor.substring(parameterEnd + 1);
            if (flowSeed != 0) {
                typeDescriptor = descriptor;
            }
        }

        if (flowSeed != 0) {
            if (typeDescriptor.length() == 0 || typeDescriptor.charAt(0) == 'L') {
                return typeDescriptor;
            }
            return (String) PRIMITIVE_TYPE_NAMES.get(String.valueOf(typeDescriptor.charAt(0)));
        }

        int dimensions = 0;
        while (dimensions < typeDescriptor.length() && typeDescriptor.charAt(dimensions) == '[') {
            dimensions++;
        }
        typeDescriptor = typeDescriptor.substring(dimensions);
        String javaType;
        if (typeDescriptor.charAt(0) == 'L') {
            if (validate && (typeDescriptor.length() <= 2 || typeDescriptor.charAt(typeDescriptor.length() - 1) != ';')) {
                return null;
            }
            javaType = typeDescriptor.substring(1, typeDescriptor.length() - 1);
            if (useDottedNames) {
                javaType = javaType.replace('/', '.');
            }
        } else {
            javaType = (String) PRIMITIVE_TYPE_NAMES.get(String.valueOf(typeDescriptor.charAt(0)));
            if (validate && javaType == null) {
                return null;
            }
        }

        for (int dimension = 0; dimension < dimensions; dimension++) {
            javaType = javaType + "[]";
        }
        return javaType;
    }

    public static ClassFileBase lookupClassByDescriptor(String string) {
        String key = ClassFileBase.extractClassName(string);
        if (key != null) {
            ClassFileBase classFileBase = ClassHierarchyNode.findClassFile(key);
            if (classFileBase != null) {
                return classFileBase;
            }
        }

        return null;
    }

    public final String getOwnerClassName() {
        return this.constantPool.getClassName();
    }

    public static String toClassForNameFormat(String string) {
        String string1 = string.replace('.', '/');
        StringBuilder stringBuilder = new StringBuilder();
        int ba = 0;

        for (int characterIndex = 0; characterIndex < string.length() && string.charAt(characterIndex) == '['; characterIndex++) {
            ba++;
        }

        stringBuilder.append(string1.substring(0, ba));
        string1 = string1.substring(ba);
        if (string1.charAt(0) == 'L') {
            if (string1.length() <= 2 || string1.charAt(string1.length() - 1) != ';') {
                return null;
            }

            if (ba != 0) {
                string1 = string1.replace('/', '.');
            }

            stringBuilder.append(string1);
        } else {
            stringBuilder.append(string1.charAt(0));
        }

        return stringBuilder.toString();
    }

    public static void setFlowKey() {
        flowKey = 43;
    }

    public static boolean isPrimitiveDescriptor(String key) {
        return key.length() == 1 && PRIMITIVE_TYPE_NAMES.containsKey(key);
    }

    public static ConstantPoolEntry readLibraryEntry(int indexArgument, ClassFileInputStream classFileInputStream, AbstractConstantPool abstractConstantPool) throws ClassFileFormatException, IOException {
        int bb = classFileInputStream.readUnsignedByte();
        switch (bb) {
            case 1:
                return new ConstantUtf8(indexArgument, classFileInputStream, abstractConstantPool);
            case 2:
            case 13:
            case 14:
            default:
                throw new ClassFileFormatException(
                        "Class file '" + abstractConstantPool.getClassLocation() + "' is corrupt. Tag '" + bb + "' at " + indexArgument + " is invalid (2)"
                );
            case 3:
                return new ConstantInteger(indexArgument, classFileInputStream, abstractConstantPool);
            case 4:
                return new ConstantFloat(indexArgument, classFileInputStream, abstractConstantPool);
            case 5:
                return new ConstantLong(indexArgument, classFileInputStream, abstractConstantPool);
            case 6:
                return new ConstantDouble(indexArgument, classFileInputStream, abstractConstantPool);
            case 7:
                return new ConstantClass(indexArgument, classFileInputStream, abstractConstantPool);
            case 8:
                return new ConstantString(indexArgument, classFileInputStream, abstractConstantPool);
            case 9:
                return new ConstantFieldref(indexArgument, classFileInputStream, abstractConstantPool);
            case 10:
                return new ConstantMethodref(indexArgument, classFileInputStream, abstractConstantPool);
            case 11:
                return new ConstantInterfaceMethodref(indexArgument, classFileInputStream, abstractConstantPool);
            case 12:
                return new ConstantNameAndType(indexArgument, classFileInputStream, abstractConstantPool);
            case 15:
                return new ConstantMethodHandle(indexArgument, classFileInputStream, abstractConstantPool);
            case 16:
                return new ConstantMethodType(indexArgument, classFileInputStream, abstractConstantPool);
            case 17:
                return new ConstantDynamic(indexArgument, classFileInputStream, abstractConstantPool);
            case 18:
                return new ConstantInvokeDynamic(indexArgument, classFileInputStream, abstractConstantPool);
            case 19:
                return new ConstantModule(indexArgument, classFileInputStream, abstractConstantPool);
            case 20:
                return new ConstantPackage(indexArgument, classFileInputStream, abstractConstantPool);
        }
    }

    public static int getFlowKey() {
        return flowKey;
    }

    public int getSlotCount() {
        return 1;
    }

    public static String parseClassNameCandidate(String string) {
        String string1 = string;
        int ba = string1.length();
        if (string1.endsWith(";")) {
            StringBuffer stringBuffer = new StringBuffer();
            String string3;
            int stringBufferLength;
            if (!string1.startsWith("[")) {
                if (!string1.startsWith("L")) {
                    return null;
                }

                stringBuffer.append('L');
                string3 = string1;
                stringBufferLength = stringBuffer.length();
            } else {
                stringBuffer.append('[');

                for (int characterIndex = 1; characterIndex < ba; characterIndex++) {
                    char bc = string1.charAt(characterIndex);
                    if (bc != '[') {
                        if (bc != 'L') {
                            return null;
                        }

                        stringBuffer.append('L');
                        break;
                    }

                    stringBuffer.append('[');
                }

                string3 = string1;
                stringBufferLength = stringBuffer.length();
            }

            string1 = string3.substring(stringBufferLength, ba - 1);
        }

        String string2;
        if (string1.indexOf(46) != -1) {
            string2 = string1.replace('.', '/');
        } else {
            string2 = string1;
        }

        int bh = string1.length();
        int bd = string1.indexOf(46);
        int be = string1.lastIndexOf(46);
        int bf = string1.indexOf(47);
        int bg = string1.lastIndexOf(47);
        return string2.length() < 3
                || !Character.isJavaIdentifierStart(string2.charAt(0))
                || (bd <= 0 || be >= bh - 1 || bf != -1) && (bf <= 0 || bg >= bh - 1 || bd != -1)
                ? null
                : string2;
    }

    public static String descriptorToJavaType(String descriptorArgument) {
        return descriptorToJavaType(descriptorArgument, false, true);
    }

    public static boolean isValidMethodDescriptor(String string) {
        if (string.length() < 2 || string.charAt(0) != '(' || string.indexOf(")") == -1) {
            return false;
        }

        if (string.indexOf(",") <= -1 && string.indexOf("]") <= -1 && string.indexOf(" ") <= -1) {
            String string1 = string.substring(1, string.lastIndexOf(")"));
            if (string.lastIndexOf(")") < string.length() - 1) {
                string1 = string1 + string.substring(string.lastIndexOf(")") + 1);
            }

            while (string1.length() != 0) {
                int characterIndex = 0;

                while (string1.charAt(characterIndex) == '[') {
                    characterIndex++;
                }

                int characterIndexValue;
                if (string1.charAt(characterIndex) == 'L') {
                    characterIndexValue = string1.indexOf(59);
                    if (characterIndexValue == -1 || characterIndexValue == characterIndex + 1) {
                        return false;
                    }
                } else {
                    characterIndexValue = characterIndex;
                }

                String key = string1.substring(0, characterIndexValue + 1);
                if (key.length() == 1 && PRIMITIVE_TYPE_NAMES.get(key) == null) {
                    return false;
                }

                if (characterIndexValue == string1.length() - 1) {
                    string1 = "";
                } else {
                    string1 = string1.substring(characterIndexValue + 1);
                }
            }

            return true;
        } else {
            return false;
        }
    }

    public final String getOwnerLocationDescription() {
        return this.constantPool.getClassLocationDescription();
    }

    public static ProgramClass findProgramClassByDescriptor(String string) {
        String key = ClassFileBase.extractClassName(string);
        if (key != null) {
            ProgramClass programClass1 = ClassHierarchyNode.findProgramClass(key);
            if (programClass1 != null) {
                return programClass1;
            }
        }

        return null;
    }

    public static String remapClassNameString(String string, Map map1, BooleanFlag booleanFlag) {
        booleanFlag.setValue(false);
        String keyValue = string;
        int keyLength = keyValue.length();
        StringBuilder stringBuilder = null;
        String string2 = null;
        if (keyValue.endsWith(";")) {
            stringBuilder = new StringBuilder();
            if (keyValue.startsWith("[")) {
                stringBuilder.append('[');

                for (int characterIndex = 1; characterIndex < keyLength; characterIndex++) {
                    char bc = keyValue.charAt(characterIndex);
                    if (bc != '[') {
                        if (bc != 'L') {
                            return string;
                        }

                        stringBuilder.append('L');
                        break;
                    }

                    stringBuilder.append('[');
                }
            } else {
                if (!keyValue.startsWith("L")) {
                    return string;
                }

                stringBuilder.append('L');
            }

            string2 = ";";
            keyValue = keyValue.substring(stringBuilder.length(), keyLength - 1);
        }

        boolean bl = false;
        String key;
        if (keyValue.indexOf(46) != -1 && keyValue.indexOf(47) == -1) {
            key = keyValue.replace('.', '/');
            bl = true;
        } else {
            key = keyValue;
        }

        String string3 = (String) map1.get(key);
        booleanFlag.setValue(string3 != null);
        int keyLengthValue = keyValue.length();
        int be = keyValue.indexOf(46);
        int bf = keyValue.lastIndexOf(46);
        int bg = keyValue.indexOf(47);
        int bh = keyValue.lastIndexOf(47);
        if (string3 != null
                && !string3.equals(key)
                && key.length() >= 3
                && Character.isJavaIdentifierStart(key.charAt(0))
                && (be > 0 && bf < keyLengthValue - 1 && bg == -1 || bg > 0 && bh < keyLengthValue - 1 && be == -1)) {
            if (bl) {
                string3 = string3.replace('/', '.');
            }

            StringBuilder stringBuilder1 = new StringBuilder();
            if (stringBuilder != null) {
                stringBuilder1.append(stringBuilder.toString());
            }

            stringBuilder1.append(string3);
            if (string2 != null) {
                stringBuilder1.append(string2);
            }

            return stringBuilder1.toString();
        } else {
            return string;
        }
    }

    public String getAbbreviatedValue() {
        return this.getValueString();
    }

    public String getValueString() {
        return "";
    }

    public final ClassFileBase selectVersionedClass(ClassFileBase classFileBase) {
        ClassFileBase classFileBase2 = this.getOwnerClass();
        ClassFileBase classFileBase1;
        if (classFileBase2.isVersionedVariant() && classFileBase.hasVersionedVariants()) {
            classFileBase1 = classFileBase.selectVersionForRelease(classFileBase2.getReleaseVersion());
        } else {
            classFileBase1 = classFileBase;
        }

        return classFileBase1;
    }

    public static List getReferencedClasses(String descriptorArgument) {
        ArrayList arrayList = new ArrayList();
        List list1 = getParameterAndReturnTypes(descriptorArgument);

        for (int elementIndex = 0; elementIndex < list1.size(); elementIndex++) {
            ClassFileBase classFileBase = lookupClassByDescriptor((String) list1.get(elementIndex));
            if (classFileBase != null) {
                if (classFileBase.isMultiRelease()) {
                    arrayList.addAll(classFileBase.getAllVersions());
                } else {
                    arrayList.add(classFileBase);
                }
            }
        }

        return arrayList;
    }

    @Override
    public final int hashCode() {
        return super.hashCode();
    }

    public final String getOwnerFilePath() {
        return this.constantPool.getClassLocation();
    }

    static {
        if (getFlowKey() != 0) {
            setFlowKey();
        }

        PRIMITIVE_TYPE_NAMES = ZkmUtils.createHashMap(17);
        PRIMITIVE_WRAPPER_CLASSES = ZkmUtils.createHashMap(17);
        WRAPPER_TO_PRIMITIVE = ZkmUtils.createHashMap(17);
        PRIMITIVE_TYPE_NAMES.put("B", "byte");
        PRIMITIVE_TYPE_NAMES.put("C", "char");
        PRIMITIVE_TYPE_NAMES.put("D", "double");
        PRIMITIVE_TYPE_NAMES.put("F", "float");
        PRIMITIVE_TYPE_NAMES.put("I", "int");
        PRIMITIVE_TYPE_NAMES.put("J", "long");
        PRIMITIVE_TYPE_NAMES.put("S", "short");
        PRIMITIVE_TYPE_NAMES.put("Z", "boolean");
        PRIMITIVE_TYPE_NAMES.put("V", "void");
        PRIMITIVE_WRAPPER_CLASSES.put("B", "java/lang/Byte");
        PRIMITIVE_WRAPPER_CLASSES.put("C", "java/lang/Character");
        PRIMITIVE_WRAPPER_CLASSES.put("D", "java/lang/Double");
        PRIMITIVE_WRAPPER_CLASSES.put("F", "java/lang/Float");
        PRIMITIVE_WRAPPER_CLASSES.put("I", "java/lang/Integer");
        PRIMITIVE_WRAPPER_CLASSES.put("J", "java/lang/Long");
        PRIMITIVE_WRAPPER_CLASSES.put("S", "java/lang/Short");
        PRIMITIVE_WRAPPER_CLASSES.put("Z", "java/lang/Boolean");
        PRIMITIVE_WRAPPER_CLASSES.put("V", "java/lang/Void");
        WRAPPER_TO_PRIMITIVE.put("java/lang/Byte", "B");
        WRAPPER_TO_PRIMITIVE.put("java/lang/Character", "C");
        WRAPPER_TO_PRIMITIVE.put("java/lang/Double", "D");
        WRAPPER_TO_PRIMITIVE.put("java/lang/Float", "F");
        WRAPPER_TO_PRIMITIVE.put("java/lang/Integer", "I");
        WRAPPER_TO_PRIMITIVE.put("java/lang/Long", "J");
        WRAPPER_TO_PRIMITIVE.put("java/lang/Short", "S");
        WRAPPER_TO_PRIMITIVE.put("java/lang/Boolean", "Z");
        WRAPPER_TO_PRIMITIVE.put("java/lang/Void", "V");
    }

    public final ProgramClass selectVersionedProgramClass(ProgramClass programClass1) {
        return (ProgramClass) this.selectVersionedClass(programClass1);
    }

    public static String remapQualifiedName(String string, Map map1, boolean bl) {
        if (string != null && string.length() != 0) {
            boolean bl1 = false;
            if (string.indexOf(".") > -1 && string.indexOf("/") == -1) {
                bl1 = true;
            }

            String key = bl1 ? string.replace(".", "/") : string;
            String string1;
            if (bl) {
                string1 = (String) ZkmUtils.mapOrSelf(key, map1);
            } else if ((string.indexOf(".") != -1 || string.indexOf("/") != -1)
                    && (string.indexOf(".") == -1 || string.indexOf("/") == -1)
                    && string.charAt(0) != '.'
                    && string.charAt(0) != '/'
                    && string.charAt(string.length() - 1) != '.'
                    && string.charAt(string.length() - 1) != '/') {
                string1 = string.replace(".", "/");
                StringTokenizer stringTokenizer = new StringTokenizer(string1, "/");
                boolean bl2 = true;

                while (stringTokenizer.hasMoreTokens()) {
                    String string3 = stringTokenizer.nextToken();
                    if (!ConstantPool.isIdentifierPath(string3)) {
                        bl2 = false;
                        break;
                    }

                    if (string3.length() < 2) {
                        bl2 = false;
                        break;
                    }
                }

                if (bl2) {
                    string1 = (String) ZkmUtils.mapOrSelf(key, map1);
                } else {
                    string1 = string;
                }
            } else {
                string1 = string;
            }

            return bl1 ? string1.replace("/", ".") : string1;
        } else {
            return string;
        }
    }

    public static String getStackReturnType(ResolvedNameAndType resolvedNameAndType) {
        String descriptorValue = resolvedNameAndType.getDescriptor();
        String returnDescriptor;
        if (isMethodDescriptor(descriptorValue)) {
            returnDescriptor = getReturnDescriptor(descriptorValue);
        } else {
            returnDescriptor = descriptorValue;
        }

        if (returnDescriptor.equals("V")) {
            return null;
        } else {
            return !returnDescriptor.equals("B") && !returnDescriptor.equals("C") && !returnDescriptor.equals("S") && !returnDescriptor.equals("Z") ? returnDescriptor : "I";
        }
    }
}

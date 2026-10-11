package com.zelix.klassmaster.classfile.hierarchy;

import com.zelix.klassmaster.classfile.ClassFileBase;
import com.zelix.klassmaster.classfile.insn.VerifierType;
import com.zelix.klassmaster.config.HiddenOptionFlags;
import com.zelix.klassmaster.exceptions.ZkmException;
import com.zelix.klassmaster.util.SyncIndexedSet;
import com.zelix.klassmaster.util.TwoKeyMap;
import com.zelix.klassmaster.util.ZkmStringUtils;
import com.zelix.klassmaster.util.ZkmUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

public class CommonSuperTypeResolver {
    public final TwoKeyMap commonSuperTypeCache = new TwoKeyMap(HiddenOptionFlags.USE_PARALLEL);
    private Map versionByThread = ZkmUtils.createHashMap();
    private final ClassHierarchyQuery hierarchyQuery;
    private final ClassResolver classResolver;

    public CommonSuperTypeResolver(ClassHierarchyQuery classHierarchyQuery, ClassResolver classResolver1) {
        this.hierarchyQuery = classHierarchyQuery;
        this.classResolver = classResolver1;
    }

    public boolean isInterfaceType(VerifierType verifierType, String string) throws ZkmException, IOException {
        String key = stripClassDescriptor(verifierType.getDescriptor());
        return (!(key.charAt(0) == '[') && (this.classResolver.getVersionedClass(key, this.getThreadVersion(), string).isInterface()));
    }

    public void clearCache() {
        this.commonSuperTypeCache.clear();
    }

    public static String toClassDescriptor(String string) {
        return string.charAt(string.length() - 1) != ';' ? 'L' + string + ';' : string;
    }

    public String getCommonArraySuperType(String string, String string1, String string2) throws ZkmException, IOException {
        int beginIndex = string.lastIndexOf("[") + 1;
        int beginIndexValue = string1.lastIndexOf("[") + 1;
        String key = string.substring(beginIndex);
        String keyValue = string1.substring(beginIndexValue);
        if (beginIndex != beginIndexValue) {
            String keyValue2;
            if (beginIndex < beginIndexValue) {
                keyValue2 = key;
            } else {
                keyValue2 = keyValue;
            }

            return keyValue2.startsWith("L") && keyValue2.endsWith(";")
                    ? ZkmStringUtils.pad("", 76, Math.min(beginIndex, beginIndexValue), 91) + "Ljava/lang/Object;"
                    : ZkmStringUtils.pad("", 76, Math.min(beginIndex, beginIndexValue) - 1, 91) + "Ljava/lang/Object;";
        } else if (key.length() == 1 && keyValue.length() == 1) {
            if (key.equals(keyValue)) {
                return string;
            } else {
                return beginIndex == 1 ? "Ljava/lang/Object;" : ZkmStringUtils.pad("", 76, beginIndex - 1, 91) + "Ljava/lang/Object;";
            }
        } else {
            String commonArraySuperType;
            if (key.length() != 1) {
                if (keyValue.length() != 1) {
                    String commonSuperType = this.getCommonSuperType(key, keyValue, string2);
                    return ZkmStringUtils.pad(commonSuperType, 82, commonSuperType.length() + beginIndex, 91);
                }

                commonArraySuperType = "Ljava/lang/Object;";
            } else {
                commonArraySuperType = "Ljava/lang/Object;";
            }

            return commonArraySuperType;
        }
    }

    public boolean isArrayAssignableTo(String string, String string1, String string2) throws ZkmException, IOException {
        int beginIndex = string.lastIndexOf("[") + 1;
        int beginIndexValue = string1.lastIndexOf("[") + 1;
        String keyValue3 = string.substring(beginIndex);
        String keyArgumentValue = string1.substring(beginIndexValue);
        if (beginIndex < beginIndexValue) {
            return false;
        } else if (beginIndex > beginIndexValue) {
            return keyArgumentValue.equals("Ljava/lang/Object;");
        } else if (keyValue3.length() == 1 && keyArgumentValue.length() == 1) {
            return keyValue3.equals(keyArgumentValue);
        } else {
            return ((keyValue3.length() != 1 && keyArgumentValue.length() != 1) && (this.isAssignableTo(keyValue3, keyArgumentValue, string2)));
        }
    }

    public boolean isAssignableTo(String key, String keyArgument, String string2) throws ZkmException, IOException {
        if (key.equals(keyArgument)) {
            return true;
        }

        key = stripClassDescriptor(key);
        keyArgument = stripClassDescriptor(keyArgument);
        Integer integer = this.getThreadVersion();
        if (key.charAt(0) == '[') {
            if (keyArgument.charAt(0) == '[') {
                return this.isArrayAssignableTo(key, keyArgument, string2);
            }

            ClassFileBase classFileBase2 = this.classResolver.getVersionedClass(keyArgument, integer, string2);
            return classFileBase2.isInterface() ? keyArgument.equals("java/lang/Cloneable") : keyArgument.equals("java/lang/Object");
        } else {
            if (keyArgument.charAt(0) == '[') {
                return false;
            }

            ClassFileBase classFileBase = this.classResolver.getVersionedClass(key, integer, string2);
            ClassFileBase classFileBase1 = this.classResolver.getVersionedClass(keyArgument, integer, string2);
            if (classFileBase.isInterface()) {
                return classFileBase1.isInterface() ? this.isSameOrImplements(key, keyArgument) : keyArgument.equals("java/lang/Object");
            }

            if (classFileBase1.isInterface()) {
                if (HiddenOptionFlags.LENIENT_INTERFACE_SUPERTYPE) {
                    return (!(!this.isSameOrImplements(key, keyArgument)) || (key.equals("java/lang/Object")));
                } else {
                    return this.isSameOrImplements(key, keyArgument);
                }
            } else {
                return this.isSameOrSubclass(key, keyArgument);
            }
        }
    }

    public void setThreadVersion(Object threadVersion) {
        this.versionByThread.put(ZkmUtils.getCurrentThreadId(), threadVersion);
    }

    public static String stripClassDescriptor(String string) {
        int ba = string.length();
        if (string.charAt(0) == 'L' && string.charAt(ba - 1) == ';') {
            string = string.substring(1, ba - 1);
        }

        return string;
    }

    public boolean isSameOrImplements(String string, String string1) throws ZkmException, IOException {
        return ((string.equals(string1)) || (this.hierarchyQuery.implementsInterface(string, string1)));
    }

    public Integer getThreadVersion() {
        return (Integer) this.versionByThread.get(ZkmUtils.getCurrentThreadId());
    }

    public String getCommonSuperType(String key, String keyArgument, String string2) throws ZkmException, IOException {
        Integer integer = this.getThreadVersion();
        if (key.equals(keyArgument)) {
            return key;
        }

        String value = (String) this.commonSuperTypeCache.getValue(key, keyArgument);
        if (value != null) {
            return value;
        }

        String keyValue = stripClassDescriptor(key);
        String keyValue2 = stripClassDescriptor(keyArgument);
        TwoKeyMap twoKeyMap;
        if (keyValue.charAt(0) == '[') {
            if (keyValue2.charAt(0) == '[') {
                value = this.getCommonArraySuperType(keyValue, keyValue2, string2);
                twoKeyMap = this.commonSuperTypeCache;
            } else {
                ClassFileBase classFileBase1 = this.classResolver.getVersionedClass(keyValue2, integer, string2);
                if (classFileBase1.isInterface()) {
                    if (keyValue2.equals("java/lang/Cloneable")) {
                        value = "Ljava/lang/Cloneable;";
                        twoKeyMap = this.commonSuperTypeCache;
                    } else {
                        value = "Ljava/lang/Object;";
                        twoKeyMap = this.commonSuperTypeCache;
                    }
                } else {
                    value = "Ljava/lang/Object;";
                    twoKeyMap = this.commonSuperTypeCache;
                }
            }
        } else if (keyValue2.charAt(0) == '[') {
            ClassFileBase classFileBase = this.classResolver.getVersionedClass(keyValue, integer, string2);
            if (classFileBase.isInterface()) {
                if (keyValue.equals("java/lang/Cloneable")) {
                    value = "Ljava/lang/Cloneable;";
                    twoKeyMap = this.commonSuperTypeCache;
                } else {
                    value = "Ljava/lang/Object;";
                    twoKeyMap = this.commonSuperTypeCache;
                }
            } else {
                value = "Ljava/lang/Object;";
                twoKeyMap = this.commonSuperTypeCache;
            }
        } else {
            ClassFileBase classFileBase2 = this.classResolver.getVersionedClass(keyValue, integer, string2);
            ClassFileBase classFileBase3 = this.classResolver.getVersionedClass(keyValue2, integer, string2);
            if (!classFileBase2.isInterface() && !classFileBase3.isInterface()) {
                ArrayList arrayList = classFileBase2.getSuperclassChain(this.classResolver, integer);
                ArrayList arrayList1 = classFileBase3.getSuperclassChain(this.classResolver, integer);
                int arrayListSize = arrayList.size();
                if (arrayList1.contains(keyValue)) {
                    value = toClassDescriptor(keyValue);
                    twoKeyMap = this.commonSuperTypeCache;
                } else if (arrayList.contains(keyValue2)) {
                    value = toClassDescriptor(keyValue2);
                    twoKeyMap = this.commonSuperTypeCache;
                } else {
                    for (int elementIndex = 0; elementIndex < arrayListSize; elementIndex++) {
                        String string6 = (String) arrayList.get(elementIndex);
                        if (arrayList1.indexOf(string6) != -1) {
                            value = toClassDescriptor(string6);
                            if (value.equals("Ljava/lang/Object;")) {
                                value = this.findCommonInterface(keyValue, classFileBase2, keyValue2, classFileBase3);
                            }
                            break;
                        }
                    }

                    if (value == null) {
                        value = "Ljava/lang/Object;";
                        twoKeyMap = this.commonSuperTypeCache;
                    } else {
                        twoKeyMap = this.commonSuperTypeCache;
                    }
                }
            } else {
                value = this.findCommonInterface(keyValue, classFileBase2, keyValue2, classFileBase3);
                twoKeyMap = this.commonSuperTypeCache;
            }
        }

        twoKeyMap.putValue(key, keyArgument, value);
        this.commonSuperTypeCache.putValue(keyArgument, key, value);
        return value;
    }

    public ClassResolver getClassResolver() {
        return this.classResolver;
    }

    public ClassHierarchyQuery getHierarchyQuery() {
        return this.hierarchyQuery;
    }

    public String mergeVerifierTypes(VerifierType verifierType, VerifierType verifierType1, String string) throws ZkmException, IOException {
        return this.getCommonSuperType(verifierType.getDescriptor(), verifierType1.getDescriptor(), string);
    }

    public boolean isVerifierTypeAssignable(VerifierType verifierType, VerifierType verifierType1, String string) throws ZkmException, IOException {
        return this.isAssignableTo(verifierType.getDescriptor(), verifierType1.getDescriptor(), string);
    }

    public String findCommonInterface(String string, ClassFileBase classFileBase, String string1, ClassFileBase classFileBase1) throws ZkmException, IOException {
        Integer integer = this.getThreadVersion();
        String string2 = null;
        SyncIndexedSet syncIndexedSet = classFileBase.collectAllInterfaces(this.classResolver, integer);
        SyncIndexedSet syncIndexedSet1 = classFileBase1.collectAllInterfaces(this.classResolver, integer);
        int syncIndexedSetSize = syncIndexedSet.size();
        if (classFileBase.isInterface() && syncIndexedSet1.contains(string)) {
            string2 = toClassDescriptor(string);
        }

        if (string2 == null && classFileBase1.isInterface() && syncIndexedSet.contains(string1)) {
            string2 = toClassDescriptor(string1);
        }

        if (string2 == null) {
            for (int elementIndex = 0; elementIndex < syncIndexedSetSize; elementIndex++) {
                String elementAt = (String) syncIndexedSet.getElementAt(elementIndex);
                if (syncIndexedSet1.indexOfElement(elementAt) != -1) {
                    string2 = toClassDescriptor(elementAt);
                    break;
                }
            }

            if (string2 == null) {
                string2 = "Ljava/lang/Object;";
            }
        }

        return string2;
    }

    public boolean isDescriptorAssignable(String keyArgument2, String keyArgumentArgument) throws ZkmException, IOException {
        if (keyArgument2.equals(keyArgumentArgument)) {
            return true;
        }

        if (keyArgument2.length() > 1 && keyArgumentArgument.length() > 1) {
            return this.isAssignableTo(keyArgument2, keyArgumentArgument, null);
        }

        if (keyArgument2.length() == 1 && keyArgumentArgument.length() == 1) {
            char ba = keyArgument2.charAt(0);
            char bb = keyArgumentArgument.charAt(0);
            switch (ba) {
                case 'B':
                    return bb == 'B' || bb == 'C' || bb == 'I' || bb == 'J';
                case 'C':
                    return bb == 'C' || bb == 'I' || bb == 'J';
                case 'D':
                    return bb == 'D';
                case 'E':
                case 'G':
                case 'H':
                case 'K':
                case 'L':
                case 'M':
                case 'N':
                case 'O':
                case 'P':
                case 'Q':
                case 'R':
                case 'T':
                case 'U':
                case 'W':
                case 'X':
                case 'Y':
                default:
                    return false;
                case 'F':
                    return bb == 'F' || bb == 'D';
                case 'I':
                    return bb == 'I' || bb == 'J';
                case 'J':
                    return bb == 'J';
                case 'S':
                    return bb == 'S' || bb == 'I' || bb == 'J';
                case 'V':
                    return bb == 'V';
                case 'Z':
                    return bb == 'Z';
            }
        } else {
            return false;
        }
    }

    public Integer clearThreadVersion() {
        return (Integer) this.versionByThread.remove(ZkmUtils.getCurrentThreadId());
    }

    public boolean isSameOrSubclass(String string, String string1) throws ZkmException, IOException {
        return ((string.equals(string1)) || (this.hierarchyQuery.isSubclass(string, string1)));
    }
}

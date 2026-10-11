package com.zelix.klassmaster.obfuscator.rename;

import com.zelix.klassmaster.changelog.ChangeLogMapping;
import com.zelix.klassmaster.classfile.ClassFileBase;
import com.zelix.klassmaster.classfile.ProgramClass;
import com.zelix.klassmaster.classfile.constpool.ConstantPoolEntry;
import com.zelix.klassmaster.classfile.hierarchy.ClassHierarchyNode;
import com.zelix.klassmaster.config.HiddenOptionFlags;
import com.zelix.klassmaster.exceptions.ZkmProcessingException;
import com.zelix.klassmaster.obfuscator.exclude.NameExclusionSet;
import com.zelix.klassmaster.util.ListMultimap;
import com.zelix.klassmaster.util.MutableInt;
import com.zelix.klassmaster.util.Triple;
import com.zelix.klassmaster.util.ZkmUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

public abstract class ClassNameGenerator {
    public static final String[] RESERVED_DEVICE_NAMES = new String[]{
            "CON",
            "PRN",
            "AUX",
            "CLOCK$",
            "NUL",
            "COM0",
            "COM1",
            "COM2",
            "COM3",
            "COM4",
            "COM5",
            "COM6",
            "COM7",
            "COM8",
            "COM9",
            "LPT0",
            "LPT1",
            "LPT2",
            "LPT3",
            "LPT4",
            "LPT5",
            "LPT6",
            "LPT7",
            "LPT8",
            "LPT9"
    };
    public static final Set RESERVED_DEVICE_NAME_SET = ZkmUtils.createHashSetFrom(Arrays.asList(RESERVED_DEVICE_NAMES));
    public final Set assignedNames = ZkmUtils.createHashSet();
    public final Set fileClassLowerCaseNames = ZkmUtils.createHashSet();
    public final Set allLowerCaseNames = ZkmUtils.createHashSet();
    public ClassRenameClashChecker clashChecker;
    public final HashMap packageRenameMap;
    public final Map packageNodes;
    public final int keepInnerClassInfoMode;
    public final int keepGenericsInfoMode;
    public final boolean randomizeNames;
    public final Set fixedClasses;
    public final Set fixedClassNames;
    public final HashMap classRenameMap;
    public final Map classesByName;
    public final NameExclusionSet exclusionSet;
    public final ChangeLogMapping changeLogMapping;
    public final ListMultimap newToOldPackages;

    public String getPackageOnlyRename(ClassHierarchyNode classHierarchyNode) {
        String className = classHierarchyNode.getClassName();
        int ba = className.lastIndexOf(47);
        if (ba != -1) {
            String renamedPackagePrefix = this.getRenamedPackagePrefix(className);
            String string2 = className.substring(ba + 1);
            String string3 = renamedPackagePrefix + string2;
            return string3.equals(className) ? null : string3;
        } else {
            return null;
        }
    }

    public final boolean checkNameAvailable(String string, String key, boolean bl, boolean bl1, boolean bl2, MutableInt mutableInt) throws IOException {
        if (this.changeLogMapping == null || !this.changeLogMapping.isNewClassName(key) && !this.changeLogMapping.isLookupClass(key)) {
            if (this.fixedClassNames.contains(key)) {
                mutableInt.setValue(2);
                return false;
            }

            if (this.assignedNames.contains(key)) {
                mutableInt.setValue(3);
                return false;
            }

            if (this.packageNodes.get(key) != null) {
                mutableInt.setValue(4);
                return false;
            }

            if (!bl && string.equals(key)) {
                mutableInt.setValue(5);
                return false;
            }

            boolean alwaysCheckReservedClassNames;
            if (key.length() == 1) {
                if (ConstantPoolEntry.isPrimitiveDescriptor(key)) {
                    mutableInt.setValue(6);
                    return false;
                }

                alwaysCheckReservedClassNames = HiddenOptionFlags.ALWAYS_CHECK_RESERVED_CLASS_NAMES;
            } else {
                alwaysCheckReservedClassNames = HiddenOptionFlags.ALWAYS_CHECK_RESERVED_CLASS_NAMES;
            }

            Set set1;
            if (!alwaysCheckReservedClassNames && bl1) {
                set1 = this.fileClassLowerCaseNames;
            } else {
                if (RESERVED_DEVICE_NAME_SET.contains(ClassFileBase.stripPackage(key).toUpperCase())) {
                    mutableInt.setValue(7);
                    return false;
                }

                set1 = this.fileClassLowerCaseNames;
            }

            if (set1.contains(key.toLowerCase())) {
                mutableInt.setValue(8);
                return false;
            } else if (!bl1 && this.allLowerCaseNames.contains(key.toLowerCase())) {
                mutableInt.setValue(9);
                return false;
            } else if (bl2 && !Character.isUpperCase(ClassFileBase.stripPackage(key).charAt(0))) {
                mutableInt.setValue(10);
                return false;
            } else {
                mutableInt.setValue(0);
                return true;
            }
        } else {
            mutableInt.setValue(1);
            return false;
        }
    }

    public ClassNameGenerator(ClassRenameClashChecker classRenameClashChecker, HashMap hashMap, ListMultimap listMultimap, Map map1, int keepInnerClassInfoMode, int keepGenericsInfoMode, boolean randomizeNames) {
        this.clashChecker = classRenameClashChecker;
        this.packageRenameMap = hashMap;
        this.packageNodes = map1;
        this.keepInnerClassInfoMode = keepInnerClassInfoMode;
        this.keepGenericsInfoMode = keepGenericsInfoMode;
        this.randomizeNames = randomizeNames;
        this.fixedClasses = classRenameClashChecker.fixedClasses;
        this.fixedClassNames = classRenameClashChecker.fixedClassNames;
        this.classRenameMap = classRenameClashChecker.oldToNewNames;
        this.classesByName = classRenameClashChecker.classesByName;
        this.exclusionSet = classRenameClashChecker.exclusionSet;
        this.changeLogMapping = classRenameClashChecker.changeLogMapping;
        this.newToOldPackages = listMultimap;
        Iterator iterator = map1.entrySet().iterator();

        while (iterator.hasNext()) {
            Entry entry = (Entry) iterator.next();
            Set set1;
            if (!((PackageTreeNode) entry.getValue()).isArchiveOnly()) {
                this.fileClassLowerCaseNames.add(((String) entry.getKey()).toLowerCase());
                set1 = this.allLowerCaseNames;
            } else {
                set1 = this.allLowerCaseNames;
            }

            set1.add(((String) entry.getKey()).toLowerCase());
        }
    }

    public static String getSimpleName(String string) {
        int ba = string.lastIndexOf(47);
        return ba == -1 ? string : string.substring(ba + 1);
    }

    public ArrayList getPackageClashCandidates(String string, String key) {
        ArrayList arrayList = new ArrayList();
        if (string != null && this.newToOldPackages != null) {
            String keyValue;
            if (key.length() > 0) {
                keyValue = (String) this.packageRenameMap.get(key);
            } else {
                keyValue = key;
            }

            int ba = string.lastIndexOf(47);
            String string3 = string.substring(ba + 1);
            List list1 = this.newToOldPackages.getValues(keyValue);
            if (list1 != null) {
                for (int elementIndex = 0; elementIndex < list1.size(); elementIndex++) {
                    String string4 = (String) list1.get(elementIndex);
                    arrayList.add(string4 + "/" + string3);
                }
            } else {
                arrayList.add(string);
            }
        }

        return arrayList;
    }

    public String getInnerClassSeparator(long ba, String string) {
        long bb = ba;
        bb = 66405443639848L ^ bb;
        String buildTag = ZkmProcessingException.getBuildTag();
        int beginIndex = string.lastIndexOf("$");
        String innerClassSeparator = buildTag;
        String innerClassSeparatorValue;
        if (beginIndex > -1) {
            innerClassSeparatorValue = string.substring(beginIndex, beginIndex + 1);
            if (6204837985703228051L <= bb) {
                return innerClassSeparator;
            }
        } else {
            innerClassSeparatorValue = "";
        }

        return innerClassSeparatorValue;
    }

    public abstract String generateNewName(ClassHierarchyNode classHierarchyNode, String string, HashMap hashMap, String string1, Triple triple, boolean bl) throws IOException;

    public int getKeepGenericsInfoMode() {
        return this.keepGenericsInfoMode;
    }

    public int getKeepInnerClassInfoMode() {
        return this.keepInnerClassInfoMode;
    }

    public final boolean isNameAvailable(String string, String key, boolean bl, boolean bl1) throws IOException {
        return this.checkNameAvailable(string, key, false, bl, bl1, new MutableInt(0));
    }

    public String getRenamedPackagePrefix(String string) {
        String renamedPackagePrefix = "";
        int endIndex = string.lastIndexOf(47);
        if (endIndex != -1) {
            renamedPackagePrefix = string.substring(0, endIndex);
            renamedPackagePrefix = (String) this.packageRenameMap.get(renamedPackagePrefix);
            if (renamedPackagePrefix.length() > 0) {
                renamedPackagePrefix = renamedPackagePrefix + "/";
            }
        }

        return renamedPackagePrefix;
    }

    public final boolean isNameAvailableOrSame(String string, String key, boolean bl, MutableInt mutableInt) throws IOException {
        return this.checkNameAvailable(string, key, true, bl, false, mutableInt);
    }

    public final String createNewClassName(ClassHierarchyNode classHierarchyNode, HashMap hashMap) throws ZkmProcessingException, IOException {
        Triple triple = null;
        String string = "";
        String className = classHierarchyNode.getClassName();
        ProgramClass programClass1 = classHierarchyNode.getProgramClass();
        if (this.keepInnerClassInfoMode == 0 && classHierarchyNode.hasEnclosingNode() && classHierarchyNode.hasOuterClassNode()) {
            ClassHierarchyNode classHierarchyNode1 = classHierarchyNode.getOuterClassNode();
            String classNameValue = classHierarchyNode1.getClassName();
            if (this.classesByName.get(classNameValue) == null) {
                throw new ZkmProcessingException("Missing outer class : " + ZkmUtils.slashesToDots(classNameValue) + "' for '" + ZkmUtils.slashesToDots(className) + "'");
            }

            if (hashMap != null) {
                Iterator iterator = hashMap.keySet().iterator();

                while (iterator.hasNext()) {
                    ClassHierarchyNode classHierarchyNode2 = (ClassHierarchyNode) iterator.next();
                    ProgramClass programClass3 = classHierarchyNode2.getProgramClass();
                    if (!this.classRenameMap.containsKey(classHierarchyNode2.getClassName()) && !this.fixedClasses.contains(programClass3)) {
                        return null;
                    }
                }
            }

            String classNameValue3;
            Object object;
            if ((object = this.classRenameMap.get(classNameValue)) == null) {
                if (!this.fixedClasses.contains(classHierarchyNode1.getProgramClass())) {
                    return null;
                }

                classNameValue3 = classNameValue;
            } else {
                classNameValue3 = (String) object;
            }

            String innerSimpleName = programClass1.getInnerSimpleName();
            if (innerSimpleName != null) {
                int beginIndex = classNameValue.length();
                int classNameLength = className.length();
                int innerSimpleNameLength = innerSimpleName.length();
                if (classNameLength - innerSimpleNameLength > beginIndex) {
                    String string7 = className.substring(beginIndex, classNameLength - innerSimpleNameLength);
                    string = classNameValue3 + string7;
                    className = innerSimpleName;
                } else {
                    String innerClassSeparator = this.getInnerClassSeparator(29969281254997L, className);
                    string = classNameValue3 + innerClassSeparator;
                    className = className.substring(classNameValue.length() + 1);
                }
            } else {
                String innerClassSeparatorValue = this.getInnerClassSeparator(29969281254997L, className);
                string = classNameValue3 + innerClassSeparatorValue;
                className = className.substring(classNameValue.length() + 1);
            }
        } else {
            triple = this.exclusionSet.getClassLink(classHierarchyNode.getProgramClass());
            if (triple != null) {
                String second = (String) triple.getSecond();
                String third = (String) triple.getThird();
                ProgramClass programClass2 = (ProgramClass) triple.getFirst();
                if (programClass2 != null) {
                    String classNameValue2 = programClass2.getClassName();
                    String string5;
                    if ((string5 = (String) this.classRenameMap.get(classNameValue2)) == null) {
                        if (!this.fixedClasses.contains(programClass2)) {
                            return null;
                        }

                        String key = this.getRenamedPackagePrefix(classHierarchyNode.getProgramClass().getClassName())
                                + second
                                + getSimpleName(classNameValue2)
                                + third;
                        MutableInt mutableInt1 = new MutableInt(0);
                        if (!this.isNameAvailableOrSame(className, key, programClass1.isFromArchive(), mutableInt1)) {
                            throw new ZkmProcessingException(
                                    "Error renaming "
                                            + classHierarchyNode.getProgramClass().getDisplayLocationName()
                                            + ". Check <link> search paths. Code="
                                            + mutableInt1.getValue()
                                            + " : "
                                            + second
                                            + " : "
                                            + key
                                            + " : "
                                            + third
                                            + " : "
                                            + className
                                            + " (1)"
                            );
                        }

                        this.assignedNames.add(key);
                        Set set2;
                        if (!programClass1.isFromArchive()) {
                            this.fileClassLowerCaseNames.add(key.toLowerCase());
                            set2 = this.allLowerCaseNames;
                        } else {
                            set2 = this.allLowerCaseNames;
                        }

                        set2.add(key.toLowerCase());
                        return key;
                    }

                    String keyValue = this.getRenamedPackagePrefix(classHierarchyNode.getProgramClass().getClassName()) + second + getSimpleName(string5) + third;
                    MutableInt mutableInt = new MutableInt(0);
                    if (!this.isNameAvailableOrSame(className, keyValue, programClass1.isFromArchive(), mutableInt)) {
                        throw new ZkmProcessingException(
                                "Error renaming "
                                        + classHierarchyNode.getProgramClass().getDisplayLocationName()
                                        + ". Check <link> search paths. Code="
                                        + mutableInt.getValue()
                                        + " : "
                                        + second
                                        + " : "
                                        + keyValue
                                        + " : "
                                        + third
                                        + " : "
                                        + className
                                        + " (2)"
                        );
                    }

                    this.assignedNames.add(keyValue);
                    if (!programClass1.isFromArchive()) {
                        this.fileClassLowerCaseNames.add(keyValue.toLowerCase());
                        this.allLowerCaseNames.add(keyValue.toLowerCase());
                    }

                    return keyValue;
                }
            }
        }

        String string8 = this.generateNewName(classHierarchyNode, string, hashMap, className, triple, programClass1.isFromArchive());
        this.assignedNames.add(string8);
        Set set1;
        if (!programClass1.isFromArchive()) {
            this.fileClassLowerCaseNames.add(string8.toLowerCase());
            set1 = this.allLowerCaseNames;
        } else {
            set1 = this.allLowerCaseNames;
        }

        set1.add(string8.toLowerCase());
        return string8;
    }

    public abstract String generateFallbackName(ClassHierarchyNode classHierarchyNode, boolean bl) throws IOException;
}

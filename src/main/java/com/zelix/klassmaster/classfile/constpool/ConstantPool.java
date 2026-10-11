package com.zelix.klassmaster.classfile.constpool;

import com.zelix.klassmaster.classfile.AbstractFieldInfo;
import com.zelix.klassmaster.classfile.AbstractMethodInfo;
import com.zelix.klassmaster.classfile.ClassFileBase;
import com.zelix.klassmaster.classfile.ClassFileInputStream;
import com.zelix.klassmaster.classfile.FieldInfo;
import com.zelix.klassmaster.classfile.MethodInfo;
import com.zelix.klassmaster.classfile.ProgramClass;
import com.zelix.klassmaster.classfile.attribute.BootstrapMethodsAttribute;
import com.zelix.klassmaster.classfile.hierarchy.ClassHierarchyNode;
import com.zelix.klassmaster.classfile.hierarchy.ClassHierarchyQuery;
import com.zelix.klassmaster.classfile.hierarchy.ClassMemberLookup;
import com.zelix.klassmaster.classfile.hierarchy.ClassResolver;
import com.zelix.klassmaster.classfile.hierarchy.ClasspathClassFile;
import com.zelix.klassmaster.config.HiddenOptionFlags;
import com.zelix.klassmaster.config.IgnoreMissingReferencesSpec;
import com.zelix.klassmaster.exceptions.ClassFileFormatException;
import com.zelix.klassmaster.exceptions.ClassFileLoadException;
import com.zelix.klassmaster.exceptions.ZkmClassNotFoundException;
import com.zelix.klassmaster.exceptions.ZkmException;
import com.zelix.klassmaster.log.MessageReporter;
import com.zelix.klassmaster.obfuscator.reflection.ReflectionApiMethod;
import com.zelix.klassmaster.util.BooleanFlag;
import com.zelix.klassmaster.util.CountingBag;
import com.zelix.klassmaster.util.EnumerableMap;
import com.zelix.klassmaster.util.ListMultimap;
import com.zelix.klassmaster.util.MutableInt;
import com.zelix.klassmaster.util.SetMultiMap;
import com.zelix.klassmaster.util.ZkmStringUtils;
import com.zelix.klassmaster.util.ZkmUtils;
import com.zelix.klassmaster.xml.ResourcePathTranslator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Map.Entry;

public class ConstantPool extends AbstractConstantPool {
    public static final char[] ZKM_CHARS = new char[]{'Z', 'K', 'M'};
    public static final Map UNBOX_METHOD_NAMES = ZkmUtils.createHashMap(17);
    public static final Map UNBOX_METHOD_DESCRIPTORS = ZkmUtils.createHashMap(17);
    public static final String FILLER_UTF8_VALUE;
    public ArrayList floatConstants = new ArrayList();
    public ArrayList doubleConstants = new ArrayList();
    public ArrayList methodHandleConstants = new ArrayList();
    public ArrayList methodTypeConstants = new ArrayList();
    public List moduleConstants = new ArrayList();
    public List packageConstants = new ArrayList();
    public ArrayList rawEntries = new ArrayList();
    public int baseEntryCount;

    public ConstantInteger getOrAddIntegerConstant(int value, List list1) {
        return this.getOrAddIntegerConstant(value, list1, true, false);
    }

    static {
        StringBuilder stringBuilder = new StringBuilder();

        for (char ba : ZKM_CHARS) {
            stringBuilder.append(ba);
        }

        if (!HiddenOptionFlags.NO_VERSION_STAMP) {
            stringBuilder.append("27.0.0");
            stringBuilder.append('E');
        } else {
            for (char bb : ZKM_CHARS) {
                stringBuilder.append(bb);
            }
        }

        FILLER_UTF8_VALUE = stringBuilder.toString();
        UNBOX_METHOD_NAMES.put("B", "byteValue");
        UNBOX_METHOD_NAMES.put("C", "charValue");
        UNBOX_METHOD_NAMES.put("D", "doubleValue");
        UNBOX_METHOD_NAMES.put("F", "floatValue");
        UNBOX_METHOD_NAMES.put("I", "intValue");
        UNBOX_METHOD_NAMES.put("J", "longValue");
        UNBOX_METHOD_NAMES.put("S", "shortValue");
        UNBOX_METHOD_NAMES.put("Z", "booleanValue");
        UNBOX_METHOD_DESCRIPTORS.put("B", "()B");
        UNBOX_METHOD_DESCRIPTORS.put("C", "()C");
        UNBOX_METHOD_DESCRIPTORS.put("D", "()D");
        UNBOX_METHOD_DESCRIPTORS.put("F", "()F");
        UNBOX_METHOD_DESCRIPTORS.put("I", "()I");
        UNBOX_METHOD_DESCRIPTORS.put("J", "()J");
        UNBOX_METHOD_DESCRIPTORS.put("S", "()S");
        UNBOX_METHOD_DESCRIPTORS.put("Z", "()Z");
    }

    public ResolvedFieldRef getOrAddFieldRef(
            String value, String valueArgument, String valueArgument2, List list1, ClassMemberLookup classMemberLookup1, ClassResolver classResolver1, Boolean boolean1
    ) throws ZkmException, IOException {
        ResolvedFieldRef resolvedFieldRef = this.findFieldRef(value, valueArgument, valueArgument2);
        if (resolvedFieldRef != null) {
            return resolvedFieldRef;
        }

        resolvedFieldRef = (ResolvedFieldRef) this.findPendingMemberRef(value, valueArgument, valueArgument2, list1);
        if (resolvedFieldRef != null) {
            return resolvedFieldRef;
        }

        ResolvedClassConstant resolvedClassConstant = this.getOrCreateClassConstant(value, list1);
        ResolvedNameAndType resolvedNameAndType = this.createNameAndType(valueArgument, valueArgument2, list1);
        ResolvedFieldRef resolvedFieldRef1 = new ResolvedFieldRef(this, resolvedClassConstant, resolvedNameAndType, classMemberLookup1, classResolver1, boolean1);
        list1.add(resolvedFieldRef1);
        return resolvedFieldRef1;
    }

    public synchronized void removeEntries(Set set1) {
        Iterator iterator = this.stringConstants.iterator();

        while (iterator.hasNext()) {
            ResolvedStringConstant resolvedStringConstant = (ResolvedStringConstant) iterator.next();
            if (set1.contains(resolvedStringConstant)) {
                iterator.remove();
            }
        }

        iterator = this.utf8Constants.iterator();

        while (iterator.hasNext()) {
            ConstantUtf8 constantUtf8 = (ConstantUtf8) iterator.next();
            if (set1.contains(constantUtf8)) {
                iterator.remove();
            }
        }

        iterator = super.integerConstants.iterator();

        while (iterator.hasNext()) {
            ConstantInteger constantInteger = (ConstantInteger) iterator.next();
            if (set1.contains(constantInteger)) {
                iterator.remove();
            }
        }

        iterator = super.longConstants.iterator();

        while (iterator.hasNext()) {
            ConstantLong constantLong = (ConstantLong) iterator.next();
            if (set1.contains(constantLong)) {
                iterator.remove();
            }
        }

        int lengthValue = this.entries.length;
        ArrayList arrayList = new ArrayList(lengthValue);

        for (int entryIndex = 0; entryIndex < lengthValue; entryIndex++) {
            ConstantPoolEntry constantPoolEntry = this.entries[entryIndex];
            if (!(constantPoolEntry instanceof ConstantNullEntry) && set1.contains(constantPoolEntry)) {
                if (constantPoolEntry.getSlotCount() == 2) {
                    entryIndex++;
                }
            } else {
                arrayList.add(constantPoolEntry);
            }
        }

        this.entries = ((com.zelix.klassmaster.classfile.constpool.ConstantPoolEntry[]) (arrayList.toArray(new ConstantPoolEntry[arrayList.size()])));
        this.renumberEntries();
        this.baseEntryCount = this.entries.length;
    }

    public void resolveMemberRefTargets(
            Set set1, ClassMemberLookup classMemberLookup1, ClassResolver classResolver1, IgnoreMissingReferencesSpec ignoreMissingReferencesSpec1
    ) throws ZkmException, IOException {
        int fieldRefsSize = this.fieldRefs.size();

        for (int elementIndex = 0; elementIndex < fieldRefsSize; elementIndex++) {
            ResolvedFieldRef resolvedFieldRef = (ResolvedFieldRef) this.fieldRefs.get(elementIndex);
            resolvedFieldRef.resolveMember(classMemberLookup1, classResolver1, ignoreMissingReferencesSpec1);
        }

        int methodRefsSize = this.methodRefs.size();

        for (int elementIndexValue = 0; elementIndexValue < methodRefsSize; elementIndexValue++) {
            ResolvedMethodRefConstant resolvedMethodRefConstant = (ResolvedMethodRefConstant) this.methodRefs.get(elementIndexValue);
            resolvedMethodRefConstant.resolveMember(classMemberLookup1, classResolver1, ignoreMissingReferencesSpec1);
        }

        int interfaceMethodRefsSize = super.interfaceMethodRefs.size();

        for (int elementIndexValue2 = 0; elementIndexValue2 < interfaceMethodRefsSize; elementIndexValue2++) {
            ResolvedInterfaceMethodRef resolvedInterfaceMethodRef = (ResolvedInterfaceMethodRef) super.interfaceMethodRefs.get(elementIndexValue2);
            resolvedInterfaceMethodRef.resolveMember(classMemberLookup1, classResolver1, ignoreMissingReferencesSpec1);
        }

        int invokeDynamicsSize = super.invokeDynamics.size();

        for (int elementIndexValue3 = 0; elementIndexValue3 < invokeDynamicsSize; elementIndexValue3++) {
            ResolvedInvokeDynamic resolvedInvokeDynamic = (ResolvedInvokeDynamic) super.invokeDynamics.get(elementIndexValue3);
            resolvedInvokeDynamic.resolveTargetMethod(set1, classMemberLookup1, classResolver1, ignoreMissingReferencesSpec1);
        }

        int constantDynamicsSize = super.constantDynamics.size();

        for (int elementIndexValue4 = 0; elementIndexValue4 < constantDynamicsSize; elementIndexValue4++) {
            ((ResolvedConstantDynamic) super.constantDynamics.get(elementIndexValue4)).resolveTypeClass(classResolver1);
        }
    }

    public RemappedConstantPool createRemappedPool(UsedConstantsCollector usedConstantsCollector, Set set1) {
        HashMap hashMap = ZkmUtils.createHashMap(ZkmUtils.getPrimeCapacity(Math.max(7, (int) ((this.getEntryCount() - this.baseEntryCount) * 0.7))));
        ConstantPoolEntry[] constantPoolEntrys = this.entries.clone();
        int ba = Math.max(15, (int) (this.baseEntryCount * 0.7));
        ListMultimap listMultimap = new ListMultimap(ba, 1);
        ArrayList arrayList = new ArrayList(Math.max(5, (int) (this.baseEntryCount * 0.7)));

        for (int elementIndex = 0; elementIndex < this.utf8Constants.size(); elementIndex++) {
            ConstantUtf8 constantUtf8 = (ConstantUtf8) this.utf8Constants.get(elementIndex);
            if (usedConstantsCollector.isUtf8Used(constantUtf8)) {
                String constantKey = constantUtf8.getConstantKey();
                listMultimap.addValue(constantKey, constantUtf8);
            } else {
                constantPoolEntrys[constantUtf8.getIndex()] = null;
            }
        }

        this.mergeDuplicateEntries(listMultimap, arrayList, constantPoolEntrys, hashMap);
        ListMultimap listMultimap1 = new ListMultimap(Math.max(10, (this.getEntryCount() - this.baseEntryCount) / 4), 1);

        for (int elementIndexValue = 0; elementIndexValue < this.nameAndTypes.size(); elementIndexValue++) {
            ResolvedNameAndType resolvedNameAndType = (ResolvedNameAndType) this.nameAndTypes.get(elementIndexValue);
            if (usedConstantsCollector.isNameAndTypeUsed(resolvedNameAndType)) {
                String constantKeyValue = resolvedNameAndType.getConstantKey();
                listMultimap1.addValue(constantKeyValue, resolvedNameAndType);
            } else {
                constantPoolEntrys[resolvedNameAndType.getIndex()] = null;
            }
        }

        this.mergeDuplicateEntries(listMultimap1, arrayList, constantPoolEntrys, hashMap);
        ListMultimap listMultimap2 = new ListMultimap(Math.max(10, (this.getEntryCount() - this.baseEntryCount) / 4), 1);

        for (int elementIndexValue2 = 0; elementIndexValue2 < this.stringConstants.size(); elementIndexValue2++) {
            ResolvedStringConstant resolvedStringConstant = (ResolvedStringConstant) this.stringConstants.get(elementIndexValue2);
            if (usedConstantsCollector.isStringUsed(resolvedStringConstant)) {
                String constantKeyValue2 = resolvedStringConstant.getConstantKey();
                listMultimap2.addValue(constantKeyValue2, resolvedStringConstant);
            } else {
                constantPoolEntrys[resolvedStringConstant.getIndex()] = null;
            }
        }

        this.mergeDuplicateEntries(listMultimap2, arrayList, constantPoolEntrys, hashMap);
        ListMultimap listMultimap3 = new ListMultimap(Math.max(10, (this.getEntryCount() - this.baseEntryCount) / 4), 1);
        int elementIndexValue3 = 0;
        int bj = 0;

        for (ArrayList arrayList6 = super.invokeDynamics; bj < arrayList6.size(); arrayList6 = super.invokeDynamics) {
            ResolvedInvokeDynamic resolvedInvokeDynamic = (ResolvedInvokeDynamic) super.invokeDynamics.get(elementIndexValue3);
            if (usedConstantsCollector.isInvokeDynamicUsed(resolvedInvokeDynamic)) {
                String constantKeyValue3 = resolvedInvokeDynamic.getConstantKey();
                listMultimap3.addValue(constantKeyValue3, resolvedInvokeDynamic);
            } else {
                constantPoolEntrys[resolvedInvokeDynamic.getIndex()] = null;
            }

            bj = ++elementIndexValue3;
        }

        this.mergeDuplicateEntries(listMultimap3, arrayList, constantPoolEntrys, hashMap);
        ListMultimap listMultimap4 = new ListMultimap(Math.max(10, (this.getEntryCount() - this.baseEntryCount) / 4), 1);
        int elementIndexValue4 = 0;
        bj = 0;

        for (ArrayList arrayList4 = super.integerConstants; bj < arrayList4.size(); arrayList4 = super.integerConstants) {
            ConstantInteger constantInteger = (ConstantInteger) super.integerConstants.get(elementIndexValue4);
            if (usedConstantsCollector.isIntegerUsed(constantInteger)) {
                listMultimap4.addValue(constantInteger.getConstantKey(), constantInteger);
            } else {
                constantPoolEntrys[constantInteger.getIndex()] = null;
            }

            bj = ++elementIndexValue4;
        }

        this.mergeDuplicateEntries(listMultimap4, arrayList, constantPoolEntrys, hashMap);
        ListMultimap listMultimap5 = new ListMultimap(Math.max(10, (this.getEntryCount() - this.baseEntryCount) / 4), 1);
        int lengthValue = 0;
        bj = 0;

        for (ArrayList arrayList5 = super.longConstants; bj < arrayList5.size(); arrayList5 = super.longConstants) {
            ConstantLong constantLong = (ConstantLong) super.longConstants.get(lengthValue);
            if (usedConstantsCollector.isLongUsed(constantLong)) {
                listMultimap5.addValue(constantLong.getConstantKey(), constantLong);
            } else {
                int index = constantLong.getIndex();
                constantPoolEntrys[index] = null;
                constantPoolEntrys[index + 1] = null;
            }

            bj = ++lengthValue;
        }

        this.mergeDuplicateEntries(listMultimap5, arrayList, constantPoolEntrys, hashMap);
        this.clearUnusedEntries(usedConstantsCollector, constantPoolEntrys);
        lengthValue = constantPoolEntrys.length;
        ArrayList arrayList1 = new ArrayList(lengthValue);
        boolean dontAddWatermark = HiddenOptionFlags.DONT_ADD_WATERMARK;
        HashSet hashSet = ZkmUtils.createHashSet(ZkmUtils.getPrimeCapacity(lengthValue));
        arrayList1.add(constantPoolEntrys[0]);
        hashSet.add(constantPoolEntrys[0]);
        SetMultiMap setMultiMap = new SetMultiMap(hashMap.size());
        Iterator iterator = hashMap.entrySet().iterator();

        while (iterator.hasNext()) {
            Entry entry = (Entry) iterator.next();
            setMultiMap.addValue(entry.getValue(), entry.getKey());
        }

        Random random1 = null;
        List list1 = usedConstantsCollector.getLdcConstants();
        if (!HiddenOptionFlags.DONT_SHUFFLE_CONSTANT_POOL) {
            random1 = ZkmUtils.createRandom(1428);
            ZkmUtils.shuffleList(list1, random1);
        } else {
            Collections.sort(list1);
        }

        Iterator iterator1 = list1.iterator();

        while (iterator1.hasNext()) {
            ConstantPoolEntry constantPoolEntry = (ConstantPoolEntry) iterator1.next();
            ConstantPoolEntry constantPoolEntry1 = (ConstantPoolEntry) ZkmUtils.mapOrSelf(constantPoolEntry, hashMap);
            if (hashSet.add(constantPoolEntry1)) {
                arrayList1.add(constantPoolEntry1);
                if (arrayList1.size() > 255) {
                    set1.addAll(usedConstantsCollector.getLdcReferencingMethods(constantPoolEntry1));
                    Set set2 = setMultiMap.getValues(constantPoolEntry1);
                    if (set2 != null) {
                        Iterator iterator2 = set2.iterator();

                        while (iterator2.hasNext()) {
                            ConstantPoolEntry constantPoolEntry2 = (ConstantPoolEntry) iterator2.next();
                            set1.addAll(usedConstantsCollector.getLdcReferencingMethods(constantPoolEntry2));
                        }
                    }
                }
            }
        }

        List list2 = usedConstantsCollector.getLdcWideConstants();
        if (!HiddenOptionFlags.DONT_SHUFFLE_CONSTANT_POOL) {
            ZkmUtils.shuffleList(list2, random1);
        } else {
            Collections.sort(list2);
        }

        Iterator iterator3 = list2.iterator();

        while (iterator3.hasNext()) {
            ConstantPoolEntry constantPoolEntry4 = (ConstantPoolEntry) iterator3.next();
            ConstantPoolEntry constantPoolEntry5 = (ConstantPoolEntry) ZkmUtils.mapOrSelf(constantPoolEntry4, hashMap);
            if (hashSet.add(constantPoolEntry5)) {
                arrayList1.add(constantPoolEntry5);
                if (arrayList1.size() <= 255) {
                    set1.addAll(usedConstantsCollector.getLdcReferencingMethods(constantPoolEntry5));
                    Set set3 = setMultiMap.getValues(constantPoolEntry5);
                    if (set3 != null) {
                        Iterator iterator6 = set3.iterator();

                        while (iterator6.hasNext()) {
                            ConstantPoolEntry constantPoolEntry3 = (ConstantPoolEntry) iterator6.next();
                            set1.addAll(usedConstantsCollector.getLdcReferencingMethods(constantPoolEntry3));
                        }
                    }
                }
            }
        }

        ArrayList arrayList2 = new ArrayList();
        ArrayList arrayList3 = new ArrayList();

        for (int indexValue = 0; indexValue < lengthValue; indexValue++) {
            ConstantPoolEntry constantPoolEntry6 = constantPoolEntrys[indexValue];
            if (constantPoolEntry6 != null) {
                if (!hashSet.contains(constantPoolEntry6)) {
                    if (!HiddenOptionFlags.DONT_SHUFFLE_CONSTANT_POOL) {
                        if (constantPoolEntry6.getSlotCount() == 2) {
                            arrayList3.add(constantPoolEntry6);
                            indexValue++;
                        } else {
                            arrayList2.add(constantPoolEntry6);
                        }
                    } else {
                        arrayList1.add(constantPoolEntry6);
                        hashSet.add(constantPoolEntry6);
                        if (constantPoolEntry6.getSlotCount() == 2) {
                            ConstantNullEntry constantNullEntry1 = (ConstantNullEntry) constantPoolEntrys[++indexValue];
                            arrayList1.add(constantNullEntry1);
                            hashSet.add(constantNullEntry1);
                        }
                    }
                }
            } else if (!dontAddWatermark) {
                arrayList1.add(new ConstantUtf8(indexValue, this, FILLER_UTF8_VALUE));
                dontAddWatermark = true;
            }
        }

        if (arrayList2.size() > 0) {
            ZkmUtils.shuffleList(arrayList2, random1);
            Iterator iterator4 = arrayList2.iterator();

            while (iterator4.hasNext()) {
                ConstantPoolEntry constantPoolEntry7 = (ConstantPoolEntry) iterator4.next();
                arrayList1.add(constantPoolEntry7);
                hashSet.add(constantPoolEntry7);
            }
        }

        if (arrayList3.size() > 0) {
            ZkmUtils.shuffleList(arrayList3, random1);
            ConstantNullEntry constantNullEntry = this.nullEntries[0];
            Iterator iterator5 = arrayList3.iterator();

            while (iterator5.hasNext()) {
                ConstantPoolEntry constantPoolEntry8 = (ConstantPoolEntry) iterator5.next();
                arrayList1.add(constantPoolEntry8);
                hashSet.add(constantPoolEntry8);
                arrayList1.add(constantNullEntry);
                hashSet.add(constantNullEntry);
            }
        }

        constantPoolEntrys = ((com.zelix.klassmaster.classfile.constpool.ConstantPoolEntry[]) (arrayList1.toArray(new ConstantPoolEntry[arrayList1.size()])));
        return new RemappedConstantPool(this, constantPoolEntrys, hashMap);
    }

    public String translateQualifiedName(String string, EnumerableMap enumerableMap, String string1) {
        StringTokenizer stringTokenizer = new StringTokenizer(string, string1);
        int ba = 0;

        while (stringTokenizer.hasMoreTokens()) {
            String string2 = stringTokenizer.nextToken();
            if (!isIdentifierPath(string2)) {
                return null;
            }

            if (ba++ == 0 && string2.length() < 2) {
                return null;
            }
        }

        MutableInt mutableInt = new MutableInt();
        String string3 = ResourcePathTranslator.renamePackagePath(string.replace(string1, "/"), enumerableMap, mutableInt).replace("/", string1);
        return !string.equals(string3) && mutableInt.getValue() < 2 ? null : string3;
    }

    public void resolveConstants(
            ArrayList arrayList, ListMultimap listMultimap, ListMultimap listMultimap1, ListMultimap listMultimap2, ListMultimap listMultimap3
    ) throws ClassFileFormatException {
        int ba = 0;

        boolean bl;
        do {
            bl = false;
            ListIterator listIterator = arrayList.listIterator();

            while (listIterator.hasNext()) {
                ResolvableConstant resolvableConstant = (ResolvableConstant) listIterator.next();
                ConstantPoolEntry constantPoolEntry = resolvableConstant.resolve(listMultimap, listMultimap1, listMultimap2, listMultimap3);
                if (constantPoolEntry == null) {
                    bl = true;
                } else {
                    listIterator.remove();
                    this.entries[resolvableConstant.getIndex()] = constantPoolEntry;
                    if (constantPoolEntry instanceof RawConstantEntry) {
                        this.rawEntries.add((RawConstantEntry) constantPoolEntry);
                    } else {
                        this.registerEntry(constantPoolEntry);
                    }
                }
            }

            if (++ba > 3) {
                throw new ClassFileFormatException(this.getClassLocationDescription() + " : " + " Unknown problem validating constant pool (B).");
            }
        } while (bl);
    }

    public ConstantInteger getOrAddIntegerConstant_s_0(int value, List list1) {
        return this.getOrAddIntegerConstant(value, list1);
    }

    public static boolean isIdentifierPath(String string) {
        if (string != null && string.length() != 0) {
            if (!Character.isJavaIdentifierStart(string.charAt(0))) {
                return false;
            }

            int ba = string.length();

            for (int characterIndex = 1; characterIndex < ba; characterIndex++) {
                char bc = string.charAt(characterIndex);
                if (bc != '/' && !Character.isJavaIdentifierPart(bc)) {
                    return false;
                }
            }

            return true;
        } else {
            return false;
        }
    }

    @Override
    public void renameThisClass(Object object) {
        String className = (String) object;
        String classNameValue = this.getClassName();
        HashMap hashMap = ZkmUtils.createHashMap(ZkmUtils.getPrimeCapacity(super.classesByName.size()));
        Iterator iterator = this.classConstants.iterator();

        while (iterator.hasNext()) {
            ResolvedClassConstant resolvedClassConstant = (ResolvedClassConstant) iterator.next();
            if (resolvedClassConstant.getClassName().equals(classNameValue)) {
                resolvedClassConstant.setClassName(className);
            }

            hashMap.put(resolvedClassConstant.getClassName(), resolvedClassConstant);
        }

        super.classesByName = hashMap;
    }

    public void renumberBootstrapMethods() {
        if (super.invokeDynamics.size() > 0) {
            ((ResolvedInvokeDynamic) super.invokeDynamics.get(0)).renumberBootstrapMethods();
        }
    }

    public void loadLibraryMethodOwner(AbstractMethodInfo abstractMethodInfo, Set set1, HashMap hashMap, MessageReporter messageReporter1) throws ZkmException, IOException {
        if (abstractMethodInfo != null && abstractMethodInfo.isLibraryStub()) {
            ClasspathClassFile classpathClassFile = (ClasspathClassFile) abstractMethodInfo.getOwningClass();
            if (set1.add(classpathClassFile)) {
                classpathClassFile.updateClassNames(hashMap, messageReporter1);
            }
        }
    }

    public int replaceStringConstantValue(Object object, String value) {
        int ba = 0;
        Iterator iterator = this.stringConstants.iterator();

        while (iterator.hasNext()) {
            ResolvedStringConstant resolvedStringConstant = (ResolvedStringConstant) iterator.next();
            if (resolvedStringConstant.getEditableValue().equals(object)) {
                resolvedStringConstant.getValueUtf8().setValue(value);
                ba++;
            }
        }

        return ba;
    }

    public void mergeDuplicateEntries(ListMultimap listMultimap, List list1, ConstantPoolEntry[] constantPoolEntrys, Map map1) {
        Enumeration enumeration = listMultimap.keys();

        while (enumeration.hasMoreElements()) {
            String key = (String) enumeration.nextElement();
            List list2 = listMultimap.getValues(key);
            int list2Size = list2.size();
            ArrayList arrayList = null;
            if (list2Size > 1) {
                arrayList = new ArrayList(list2Size - 1);
            }

            ConstantPoolEntry constantPoolEntry = (ConstantPoolEntry) list2.get(0);
            list1.add(constantPoolEntry);

            for (int elementIndex = 1; elementIndex < list2Size; elementIndex++) {
                ConstantPoolEntry constantPoolEntry1 = (ConstantPoolEntry) list2.get(elementIndex);
                int index = constantPoolEntry1.getIndex();
                constantPoolEntrys[index] = null;
                map1.put(constantPoolEntry1, constantPoolEntry);
                if (constantPoolEntry1.isLongOrDouble()) {
                    constantPoolEntrys[index + 1] = null;
                }

                arrayList.add(constantPoolEntry1);
            }
        }
    }

    @Override
    public boolean isProgramPool() {
        return true;
    }

    @Override
    public synchronized int appendEntries(List list1) {
        int ba = super.appendEntries(list1);
        int list1Size = list1.size();

        for (int elementIndex = 0; elementIndex < list1Size; elementIndex++) {
            this.registerEntry((ConstantPoolEntry) list1.get(elementIndex));
        }

        return ba;
    }

    public void collectReferencedProgramMembers(HashSet hashSet, HashSet hashSet1, HashSet hashSet2) {
        int classConstantsSize = this.classConstants.size();

        for (int elementIndex = 0; elementIndex < classConstantsSize; elementIndex++) {
            String className = ((ResolvedClassConstant) this.classConstants.get(elementIndex)).getClassName();
            String key = ClassFileBase.descriptorToClassName(className);
            if (key != null) {
                ProgramClass programClass1 = ClassHierarchyNode.findProgramClass(key);
                if (programClass1 != null) {
                    hashSet.add(programClass1);
                }
            }
        }

        int fieldRefsSize = this.fieldRefs.size();

        for (int elementIndexValue = 0; elementIndexValue < fieldRefsSize; elementIndexValue++) {
            ResolvedFieldRef resolvedFieldRef = (ResolvedFieldRef) this.fieldRefs.get(elementIndexValue);
            AbstractFieldInfo abstractFieldInfo = (AbstractFieldInfo) resolvedFieldRef.getResolvedMember();
            if (abstractFieldInfo != null && abstractFieldInfo.isProgramMember()) {
                hashSet1.add((FieldInfo) abstractFieldInfo);
            }

            ProgramClass programClass2 = resolvedFieldRef.getFieldTypeProgramClass();
            if (programClass2 != null) {
                hashSet.add(programClass2);
            }
        }

        int methodRefsSize = this.methodRefs.size();

        for (int elementIndexValue2 = 0; elementIndexValue2 < methodRefsSize; elementIndexValue2++) {
            ResolvedMethodRefConstant resolvedMethodRefConstant = (ResolvedMethodRefConstant) this.methodRefs.get(elementIndexValue2);
            AbstractMethodInfo abstractMethodInfo = (AbstractMethodInfo) resolvedMethodRefConstant.getResolvedMember();
            if (abstractMethodInfo != null && abstractMethodInfo.isProgramMember()) {
                hashSet2.add((MethodInfo) abstractMethodInfo);
            }

            List list1 = resolvedMethodRefConstant.getDescriptorProgramClasses();

            for (int elementIndexValue3 = 0; elementIndexValue3 < list1.size(); elementIndexValue3++) {
                hashSet.add(list1.get(elementIndexValue3));
            }
        }

        int interfaceMethodRefsSize = super.interfaceMethodRefs.size();

        for (int elementIndexValue4 = 0; elementIndexValue4 < interfaceMethodRefsSize; elementIndexValue4++) {
            ResolvedInterfaceMethodRef resolvedInterfaceMethodRef = (ResolvedInterfaceMethodRef) super.interfaceMethodRefs.get(elementIndexValue4);
            AbstractMethodInfo abstractMethodInfo1 = (AbstractMethodInfo) resolvedInterfaceMethodRef.getResolvedMember();
            if (abstractMethodInfo1 != null && abstractMethodInfo1.isProgramMember()) {
                hashSet2.add((MethodInfo) abstractMethodInfo1);
            }

            List list2 = resolvedInterfaceMethodRef.getDescriptorProgramClasses();

            for (int elementIndexValue5 = 0; elementIndexValue5 < list2.size(); elementIndexValue5++) {
                hashSet.add(list2.get(elementIndexValue5));
            }
        }
    }

    public void remapResourceNameStrings(HashMap hashMap, HashSet hashSet, Set set1, HashMap hashMap1) {
        EnumerableMap enumerableMap = new EnumerableMap(hashMap1);
        int stringConstantsSize = this.stringConstants.size();

        for (int elementIndex = 0; elementIndex < stringConstantsSize; elementIndex++) {
            ResolvedStringConstant resolvedStringConstant = (ResolvedStringConstant) this.stringConstants.get(elementIndex);
            if (!hashSet.contains(resolvedStringConstant)) {
                ConstantUtf8 constantUtf8 = resolvedStringConstant.getValueUtf8();
                String value = constantUtf8.getValue();
                int valueLength = value.length();
                BooleanFlag booleanFlag = new BooleanFlag();
                String valueValue = ConstantPoolEntry.remapClassNameString(value, hashMap, booleanFlag);
                if (!valueValue.equals(value)) {
                    constantUtf8.setValue(valueValue);
                } else if (!booleanFlag.getValue()) {
                    String valueValue8;
                    if (value.startsWith("/") && valueLength > 0) {
                        valueValue8 = value.substring(1);
                    } else {
                        valueValue8 = value;
                    }

                    if (set1.contains(valueValue8)) {
                        String valueValue2 = ResourcePathTranslator.renameResourceParentPath(value, enumerableMap);
                        if (!valueValue2.equals(value)) {
                            constantUtf8.setValue(valueValue2);
                        }
                    } else {
                        int bd = value.indexOf("/");
                        int be = value.lastIndexOf("/");
                        int bf = value.indexOf(".");
                        if (bd > 0 && bf > 0 && be < bf && bf < valueLength - 1) {
                            ArrayList arrayList = new ArrayList();
                            ArrayList arrayList1 = new ArrayList();
                            String suffix = this.getPackageName() + (value.charAt(value.length() - 1) == '/' ? "" : '/') + value;
                            Iterator iterator = set1.iterator();

                            while (iterator.hasNext()) {
                                String string6 = (String) iterator.next();
                                if (string6.endsWith(suffix)) {
                                    arrayList.add(string6);
                                }

                                if (string6.endsWith(value)) {
                                    arrayList1.add(string6);
                                }
                            }

                            String string12;
                            if (arrayList.size() == 1) {
                                string12 = (String) arrayList.get(0);
                            } else {
                                if (arrayList1.size() != 1) {
                                    continue;
                                }

                                string12 = (String) arrayList1.get(0);
                            }

                            if (string12 != null) {
                                String valueValue3 = ResourcePathTranslator.translateRelativeReference(string12, value, enumerableMap);
                                if (!valueValue3.equals(value)) {
                                    constantUtf8.setValue(valueValue3);
                                }
                            }
                        } else if (bf == -1) {
                            if (bd > 0 && be < valueLength - 1 && ZkmStringUtils.countChar(value, '/') >= 2) {
                                if (enumerableMap.containsKey(value)) {
                                    String valueValue4 = (String) enumerableMap.get(value);
                                    if (valueValue4 != null && !value.equals(valueValue4)) {
                                        constantUtf8.setValue(valueValue4);
                                    }
                                } else if (!HiddenOptionFlags.DONT_RENAME_SLASHED_STRINGS) {
                                    String valueValue5 = this.translateQualifiedName(value, enumerableMap, "/");
                                    if (valueValue5 != null && !value.equals(valueValue5)) {
                                        constantUtf8.setValue(valueValue5);
                                    }
                                }
                            }
                        } else if (bd == -1 && bf > 0 && value.lastIndexOf(".") < valueLength - 1 && ZkmStringUtils.countChar(value, '.') >= 2) {
                            String key = value.replace('.', '/');
                            if (enumerableMap.containsKey(key)) {
                                String string4 = (String) enumerableMap.get(key);
                                if (string4 != null && !key.equals(string4) && !HiddenOptionFlags.DONT_RENAME_DOTTED_STRINGS) {
                                    String valueValue6 = string4.replace('/', '.');
                                    constantUtf8.setValue(valueValue6);
                                }
                            } else if (HiddenOptionFlags.RENAME_PARTIAL_DOTTED_STRINGS) {
                                String valueValue7 = this.translateQualifiedName(value, enumerableMap, ".");
                                if (valueValue7 != null && !value.equals(valueValue7)) {
                                    constantUtf8.setValue(valueValue7);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public ResolvedMethodHandleConstant getOrAddMethodHandle(MethodHandleRefKind methodHandleRefKind, ResolvedMemberRef resolvedMemberRef, List list1) {
        Iterator iterator = this.methodHandleConstants.iterator();

        while (iterator.hasNext()) {
            ResolvedMethodHandleConstant resolvedMethodHandleConstant = (ResolvedMethodHandleConstant) iterator.next();
            if (resolvedMethodHandleConstant.getRefKind() == methodHandleRefKind && resolvedMethodHandleConstant.matches(methodHandleRefKind, resolvedMemberRef)) {
                return resolvedMethodHandleConstant;
            }
        }

        iterator = list1.iterator();

        while (iterator.hasNext()) {
            ConstantPoolEntry constantPoolEntry = (ConstantPoolEntry) iterator.next();
            if (constantPoolEntry instanceof ResolvedMethodHandleConstant) {
                ResolvedMethodHandleConstant resolvedMethodHandleConstant1 = (ResolvedMethodHandleConstant) constantPoolEntry;
                if (resolvedMethodHandleConstant1.getRefKind() == methodHandleRefKind
                        && resolvedMethodHandleConstant1.matches(methodHandleRefKind, resolvedMemberRef)) {
                    return resolvedMethodHandleConstant1;
                }
            }
        }

        ResolvedMethodHandleConstant resolvedMethodHandleConstant2 = new ResolvedMethodHandleConstant(this, methodHandleRefKind, resolvedMemberRef);
        list1.add(resolvedMethodHandleConstant2);
        return resolvedMethodHandleConstant2;
    }

    private void registerEntry(ConstantPoolEntry constantPoolEntry) {
        ConstantPoolTag constantPoolTag = constantPoolEntry.getTag();
        switch (ConstantPoolTagSwitchMap.TAG_SWITCH_TABLE[constantPoolTag.ordinal()]) {
            case 1:
                this.classConstants.add((ResolvedClassConstant) constantPoolEntry);
                ResolvedClassConstant resolvedClassConstant = (ResolvedClassConstant) super.classesByName
                        .put(((ResolvedClassConstant) constantPoolEntry).getClassName(), (ResolvedClassConstant) constantPoolEntry);
                break;
            case 2:
                this.nameAndTypes.add((ResolvedNameAndType) constantPoolEntry);
                break;
            case 3:
                this.methodRefs.add((ResolvedMethodRefConstant) constantPoolEntry);
                break;
            case 4:
                super.interfaceMethodRefs.add((ResolvedInterfaceMethodRef) constantPoolEntry);
                break;
            case 5:
                this.fieldRefs.add((ResolvedFieldRef) constantPoolEntry);
                break;
            case 6:
                this.utf8Constants.add((ConstantUtf8) constantPoolEntry);
                break;
            case 7:
                this.stringConstants.add((ResolvedStringConstant) constantPoolEntry);
                break;
            case 8:
                super.integerConstants.add((ConstantInteger) constantPoolEntry);
                break;
            case 9:
                super.longConstants.add((ConstantLong) constantPoolEntry);
                break;
            case 10:
                this.floatConstants.add((ConstantFloat) constantPoolEntry);
                break;
            case 11:
                this.doubleConstants.add((ConstantDouble) constantPoolEntry);
                break;
            case 12:
                this.methodHandleConstants.add((ResolvedMethodHandleConstant) constantPoolEntry);
                break;
            case 13:
                this.methodTypeConstants.add((ResolvedMethodType) constantPoolEntry);
                break;
            case 14:
                super.constantDynamics.add((ResolvedConstantDynamic) constantPoolEntry);
                break;
            case 15:
                super.invokeDynamics.add((ResolvedInvokeDynamic) constantPoolEntry);
                break;
            case 16:
                this.moduleConstants.add((ResolvedConstantModule) constantPoolEntry);
                break;
            case 17:
                this.packageConstants.add((ResolvedPackageConstant) constantPoolEntry);
        }
    }

    public void countReferencedProgramClasses(CountingBag countingBag) {
        String className = this.classFile.getClassName();
        int classConstantsSize = this.classConstants.size();

        for (int elementIndex = 0; elementIndex < classConstantsSize; elementIndex++) {
            String classNameValue = ((ResolvedClassConstant) this.classConstants.get(elementIndex)).getClassName();
            if (classNameValue.startsWith("[")) {
                classNameValue = ClassFileBase.descriptorToClassName(classNameValue);
            }

            ProgramClass programClass1;
            if (classNameValue != null && !classNameValue.equals(className) && (programClass1 = ClassHierarchyNode.findProgramClass(classNameValue)) != null) {
                countingBag.add(programClass1);
            }
        }
    }

    public ResolvedInterfaceMethodRef getOrAddInterfaceMethodRef(
            String value, String valueArgument, String valueArgument2, List list1, ClassMemberLookup classMemberLookup1, ClassResolver classResolver1
    ) throws ZkmException, IOException {
        ResolvedInterfaceMethodRef resolvedInterfaceMethodRef = this.findInterfaceMethodRef(value, valueArgument, valueArgument2);
        if (resolvedInterfaceMethodRef != null) {
            return resolvedInterfaceMethodRef;
        }

        resolvedInterfaceMethodRef = (ResolvedInterfaceMethodRef) this.findPendingMemberRef(value, valueArgument, valueArgument2, list1);
        if (resolvedInterfaceMethodRef != null) {
            return resolvedInterfaceMethodRef;
        }

        ResolvedClassConstant resolvedClassConstant = this.getOrCreateClassConstant(value, list1);
        ResolvedNameAndType resolvedNameAndType = this.createNameAndType(valueArgument, valueArgument2, list1);
        ResolvedInterfaceMethodRef resolvedInterfaceMethodRef1 = new ResolvedInterfaceMethodRef(
                this, resolvedClassConstant, resolvedNameAndType, classMemberLookup1, classResolver1
        );
        list1.add(resolvedInterfaceMethodRef1);
        return resolvedInterfaceMethodRef1;
    }

    public ConstantPool(
            ClassFileInputStream classFileInputStream,
            ProgramClass programClass1,
            ListMultimap listMultimap,
            ListMultimap listMultimap1,
            ListMultimap listMultimap2,
            ListMultimap listMultimap3
    ) throws ClassFileFormatException, IOException {
        super(programClass1);
        int baseEntryCountValue = classFileInputStream.readUnsignedShort();
        this.baseEntryCount = baseEntryCountValue;
        this.entries = new ConstantPoolEntry[baseEntryCountValue];
        ArrayList arrayList = new ArrayList();
        ArrayList arrayList1 = new ArrayList();
        ArrayList arrayList2 = new ArrayList();
        this.utf8Constants = new ArrayList(Math.max(5, (int) (baseEntryCountValue * 1.5)));
        this.nameAndTypes = new ArrayList(Math.max(5, (int) (baseEntryCountValue * 0.2)));
        this.classConstants = new ArrayList(Math.max(5, (int) (baseEntryCountValue * 0.09)));
        super.classesByName = ZkmUtils.createHashMap(Math.max(5, (int) (baseEntryCountValue * 0.09)));
        this.stringConstants = new ArrayList(Math.max(5, (int) (baseEntryCountValue * 0.05)));
        super.integerConstants = new ArrayList(Math.max(5, (int) (baseEntryCountValue * 0.01)));
        super.longConstants = new ArrayList(Math.max(5, (int) (baseEntryCountValue * 0.01)));
        this.methodRefs = new ArrayList(Math.max(5, (int) (baseEntryCountValue * 0.15)));
        super.interfaceMethodRefs = new ArrayList(Math.max(5, (int) (baseEntryCountValue * 0.15)));
        this.fieldRefs = new ArrayList(Math.max(5, (int) (baseEntryCountValue * 0.15)));
        this.entries[0] = ConstantNullEntry.getSharedInstance(this);
        int indexValue = 1;

        while (indexValue < baseEntryCountValue) {
            ConstantPoolEntry constantPoolEntry = ConstantPoolEntry.readEntry(indexValue, classFileInputStream, this);
            this.entries[indexValue++] = constantPoolEntry;
            if (constantPoolEntry.getSlotCount() == 2) {
                this.entries[indexValue++] = ConstantNullEntry.getSharedInstance(this);
            }

            ConstantPoolTag constantPoolTag = constantPoolEntry.getTag();
            switch (ConstantPoolTagSwitchMap.TAG_SWITCH_TABLE[constantPoolTag.ordinal()]) {
                case 1:
                    arrayList.add(constantPoolEntry);
                    break;
                case 2:
                    arrayList1.add(constantPoolEntry);
                    break;
                case 3:
                    arrayList2.add(constantPoolEntry);
                    break;
                case 4:
                    arrayList2.add(constantPoolEntry);
                    break;
                case 5:
                    arrayList2.add(constantPoolEntry);
                    break;
                case 6:
                    this.utf8Constants.add((ConstantUtf8) constantPoolEntry);
                    break;
                case 7:
                    arrayList2.add(constantPoolEntry);
                    break;
                case 8:
                    super.integerConstants.add((ConstantInteger) constantPoolEntry);
                    break;
                case 9:
                    super.longConstants.add((ConstantLong) constantPoolEntry);
                    break;
                case 10:
                    this.floatConstants.add((ConstantFloat) constantPoolEntry);
                    break;
                case 11:
                    this.doubleConstants.add((ConstantDouble) constantPoolEntry);
                    break;
                case 12:
                    arrayList2.add(constantPoolEntry);
                    break;
                case 13:
                    arrayList2.add(constantPoolEntry);
                    break;
                case 14:
                    arrayList2.add(constantPoolEntry);
                    break;
                case 15:
                    arrayList2.add(constantPoolEntry);
                    break;
                case 16:
                    arrayList2.add(constantPoolEntry);
                    break;
                case 17:
                    arrayList2.add(constantPoolEntry);
            }
        }

        this.resolveConstants(arrayList, listMultimap, listMultimap1, listMultimap2, listMultimap3);
        this.resolveConstants(arrayList1, listMultimap, listMultimap1, listMultimap2, listMultimap3);
        this.resolveConstants(arrayList2, listMultimap, listMultimap1, listMultimap2, listMultimap3);
    }

    public ResolvedMethodRefConstant getOrAddMethodRef(
            String value, String valueArgument, String valueArgument2, List list1, ClassMemberLookup classMemberLookup1, ClassResolver classResolver1
    ) throws ZkmException, IOException {
        ResolvedMethodRefConstant resolvedMethodRefConstant = this.findMethodRef(value, valueArgument, valueArgument2);
        if (resolvedMethodRefConstant != null) {
            return resolvedMethodRefConstant;
        }

        resolvedMethodRefConstant = (ResolvedMethodRefConstant) this.findPendingMemberRef(value, valueArgument, valueArgument2, list1);
        if (resolvedMethodRefConstant != null) {
            return resolvedMethodRefConstant;
        }

        ResolvedClassConstant resolvedClassConstant = this.getOrCreateClassConstant(value, list1);
        ResolvedNameAndType resolvedNameAndType = this.createNameAndType(valueArgument, valueArgument2, list1);
        ResolvedMethodRefConstant resolvedMethodRefConstant1 = new ResolvedMethodRefConstant(
                this, resolvedClassConstant, resolvedNameAndType, classMemberLookup1, classResolver1
        );
        list1.add(resolvedMethodRefConstant1);
        return resolvedMethodRefConstant1;
    }

    @Override
    public void applyPackageRenames(Object object) {
        HashMap hashMap = (HashMap) object;
        Iterator iterator = this.packageConstants.iterator();

        while (iterator.hasNext()) {
            ((ResolvedPackageConstant) iterator.next()).remapName(hashMap);
        }
    }

    @Override
    public void unshareConstants(
            ListMultimap listMultimap,
            ListMultimap listMultimap1,
            ListMultimap listMultimap2,
            ListMultimap listMultimap3,
            ListMultimap listMultimap4,
            ListMultimap listMultimap5,
            ListMultimap listMultimap6,
            ListMultimap listMultimap7
    ) {
        super.unshareConstants(listMultimap, listMultimap1, listMultimap2, listMultimap3, listMultimap4, listMultimap5, listMultimap6, listMultimap7);
        HashMap hashMap = ZkmUtils.createHashMap(ZkmUtils.getPrimeCapacity(this.classConstants.size()));
        Iterator iterator = this.classConstants.iterator();

        while (iterator.hasNext()) {
            ResolvedClassConstant resolvedClassConstant = (ResolvedClassConstant) iterator.next();
            hashMap.put(resolvedClassConstant.getClassName(), resolvedClassConstant);
        }

        super.classesByName = hashMap;
    }

    public void remapDescriptorStrings(HashMap hashMap) {
        int elementIndex = 0;
        int bb = 0;

        for (ArrayList arrayList = super.unreferencedUtf8s; bb < arrayList.size(); arrayList = super.unreferencedUtf8s) {
            ConstantUtf8 constantUtf8 = (ConstantUtf8) super.unreferencedUtf8s.get(elementIndex);
            String value = constantUtf8.getValue();
            String valueValue = ConstantPoolEntry.remapDescriptorClassNames(value, hashMap);
            if (!valueValue.equals(value)) {
                constantUtf8.setValue(valueValue);
            }

            bb = ++elementIndex;
        }

        Iterator iterator = this.stringConstants.iterator();

        while (iterator.hasNext()) {
            ResolvedStringConstant resolvedStringConstant = (ResolvedStringConstant) iterator.next();
            ConstantUtf8 constantUtf81 = resolvedStringConstant.getValueUtf8();
            String valueValue2 = constantUtf81.getValue();
            String valueValue3 = ConstantPoolEntry.remapDescriptorClassNames(valueValue2, hashMap);
            if (!valueValue3.equals(valueValue2)) {
                constantUtf81.setValue(valueValue3);
            }
        }
    }

    public int replaceIntegerValue(int value) {
        int bb = 0;
        Iterator iterator = super.integerConstants.iterator();

        while (iterator.hasNext()) {
            ConstantInteger constantInteger = (ConstantInteger) iterator.next();
            if (constantInteger.getValue() == Integer.MAX_VALUE) {
                constantInteger.setValue(value);
                bb++;
            }
        }

        return bb;
    }

    public ResolvedStringConstant addStringConstant(String value, List list1) {
        return this.addStringConstant(value, list1, true);
    }

    public NumericConstantEntry[] getLiteralConstants() {
        NumericConstantEntry[] numericConstantEntrys = new NumericConstantEntry[super.integerConstants.size()
                + super.longConstants.size()
                + this.floatConstants.size()
                + this.doubleConstants.size()
                + this.stringConstants.size()];
        int ba = 0;

        for (int elementIndex = 0; elementIndex < this.stringConstants.size(); elementIndex++) {
            numericConstantEntrys[ba++] = (NumericConstantEntry) this.stringConstants.get(elementIndex);
        }

        for (int elementIndexValue = 0; elementIndexValue < super.integerConstants.size(); elementIndexValue++) {
            numericConstantEntrys[ba++] = (NumericConstantEntry) super.integerConstants.get(elementIndexValue);
        }

        int elementIndexValue2 = 0;
        int elementIndexValue3 = elementIndexValue2;

        for (ArrayList arrayList = super.longConstants; elementIndexValue3 < arrayList.size(); arrayList = super.longConstants) {
            numericConstantEntrys[ba++] = (NumericConstantEntry) super.longConstants.get(elementIndexValue2);
            elementIndexValue3 = ++elementIndexValue2;
        }

        elementIndexValue2 = 0;
        elementIndexValue3 = 0;

        for (ArrayList arrayList1 = this.floatConstants; elementIndexValue3 < arrayList1.size(); arrayList1 = this.floatConstants) {
            numericConstantEntrys[ba++] = (NumericConstantEntry) this.floatConstants.get(elementIndexValue2);
            elementIndexValue3 = ++elementIndexValue2;
        }

        elementIndexValue2 = 0;
        elementIndexValue3 = 0;

        for (ArrayList arrayList2 = this.doubleConstants; elementIndexValue3 < arrayList2.size(); arrayList2 = this.doubleConstants) {
            numericConstantEntrys[ba++] = (NumericConstantEntry) this.doubleConstants.get(elementIndexValue2);
            elementIndexValue3 = ++elementIndexValue2;
        }

        return numericConstantEntrys;
    }

    public ResolvedMethodRefConstant getOrAddUnboxMethodRef(String string, List list1, ClassMemberLookup classMemberLookup1, ClassResolver classResolver1) throws ZkmException, IOException {
        String key = string;
        switch (key.charAt(0)) {
            case 'B':
            case 'C':
            case 'S':
                key = "I";
            default:
                String wrapperClassName = ConstantPoolEntry.getWrapperClassName(key);
                String value = (String) UNBOX_METHOD_NAMES.get(key);
                String valueValue = (String) UNBOX_METHOD_DESCRIPTORS.get(key);
                return this.getOrAddMethodRef(wrapperClassName, value, valueValue, list1, classMemberLookup1, classResolver1);
        }
    }

    public List findSubclassConstants(ClassHierarchyQuery classHierarchyQuery) throws ZkmException, IOException {
        ArrayList arrayList = new ArrayList();
        Iterator iterator = this.classConstants.iterator();

        while (iterator.hasNext()) {
            ResolvedClassConstant resolvedClassConstant = (ResolvedClassConstant) iterator.next();
            if (!resolvedClassConstant.isArrayClass()) {
                String className = resolvedClassConstant.getClassName();

                try {
                    if (className.equals("java/lang/RuntimeException") || classHierarchyQuery.isSubclass(className, "java/lang/RuntimeException")) {
                        arrayList.add(resolvedClassConstant);
                    }
                } catch (ClassFileLoadException classFileLoadException) {
                }
            }
        }

        return arrayList;
    }

    public void clearUnusedEntries(UsedConstantsCollector usedConstantsCollector, ConstantPoolEntry[] constantPoolEntrys) {
        ConstantPoolTag.getFlowKey();

        for (int constantPoolEntryIndex = 0; constantPoolEntryIndex < constantPoolEntrys.length; constantPoolEntryIndex++) {
            if (constantPoolEntrys[constantPoolEntryIndex] != null && !(constantPoolEntrys[constantPoolEntryIndex] instanceof ConstantNullEntry)) {
                ConstantPoolEntry constantPoolEntry = constantPoolEntrys[constantPoolEntryIndex];
                if (!usedConstantsCollector.isUsed(constantPoolEntry)) {
                    constantPoolEntrys[constantPoolEntryIndex] = null;
                    if (constantPoolEntry.isLongOrDouble()) {
                        ConstantPoolEntry constantPoolEntry1 = constantPoolEntrys[++constantPoolEntryIndex];
                        constantPoolEntrys[constantPoolEntryIndex] = null;
                    }
                }
            }
        }
    }

    public ResolvedStringConstant addStringConstant(String value, List list1, boolean fromClassFile) {
        ConstantUtf8 constantUtf8 = new ConstantUtf8(0, this, value);
        ResolvedStringConstant resolvedStringConstant = new ResolvedStringConstant(this, constantUtf8, fromClassFile);
        list1.add(constantUtf8);
        list1.add(resolvedStringConstant);
        return resolvedStringConstant;
    }

    public ConstantLong getOrAddLongConstant(long value, List list1) {
        return this.getOrAddLongConstant(value, list1, true, false);
    }

    public ConstantInteger getOrAddIntegerConstant(int value, List list1, boolean bl, boolean bl1) {
        if (bl) {
            int elementIndex = 0;
            int bc = 0;

            for (ArrayList arrayList = super.integerConstants; bc < arrayList.size(); arrayList = super.integerConstants) {
                ConstantInteger constantInteger = (ConstantInteger) super.integerConstants.get(elementIndex);
                if (!constantInteger.isInProgramPool() && constantInteger.hasValue(value)) {
                    return constantInteger;
                }

                bc = ++elementIndex;
            }

            Iterator iterator = list1.iterator();

            while (iterator.hasNext()) {
                ConstantPoolEntry constantPoolEntry = (ConstantPoolEntry) iterator.next();
                if (constantPoolEntry instanceof ConstantInteger) {
                    ConstantInteger constantInteger1 = (ConstantInteger) constantPoolEntry;
                    if (!constantInteger1.isInProgramPool() && constantInteger1.hasValue(value)) {
                        return constantInteger1;
                    }
                }
            }
        }

        ConstantInteger constantInteger2;
        if (bl1) {
            constantInteger2 = new ConstantInteger(this, value, true);
        } else {
            constantInteger2 = new ConstantInteger(this, value, false);
        }

        list1.add(constantInteger2);
        return constantInteger2;
    }

    public ConstantLong getOrAddLongConstant(long value, List list1, boolean bl, boolean bl1) {
        if (bl) {
            int elementIndex = 0;
            int bc = 0;

            for (ArrayList arrayList = super.longConstants; bc < arrayList.size(); arrayList = super.longConstants) {
                ConstantLong constantLong = (ConstantLong) super.longConstants.get(elementIndex);
                if (!constantLong.isInProgramPool() && constantLong.hasValue(value)) {
                    return constantLong;
                }

                bc = ++elementIndex;
            }

            Iterator iterator = list1.iterator();

            while (iterator.hasNext()) {
                ConstantPoolEntry constantPoolEntry = (ConstantPoolEntry) iterator.next();
                if (constantPoolEntry instanceof ConstantLong) {
                    ConstantLong constantLong1 = (ConstantLong) constantPoolEntry;
                    if (!constantLong1.isInProgramPool() && constantLong1.hasValue(value)) {
                        return constantLong1;
                    }
                }
            }
        }

        ConstantLong constantLong2;
        if (bl1) {
            constantLong2 = new ConstantLong(this, value, true);
        } else {
            constantLong2 = new ConstantLong(this, value, false);
        }

        list1.add(constantLong2);
        return constantLong2;
    }

    public void findReflectionApiCalls(Set set1, Map map1, ClassMemberLookup classMemberLookup1, MessageReporter messageReporter1) throws ZkmException, IOException {
        for (int elementIndex = 0; elementIndex < this.methodRefs.size(); elementIndex++) {
            ResolvedMethodRef resolvedMethodRef = (ResolvedMethodRef) this.methodRefs.get(elementIndex);

            try {
                ReflectionApiMethod reflectionApiMethod;
                if ((reflectionApiMethod = resolvedMethodRef.findReflectionApiMethod(classMemberLookup1)) != null) {
                    map1.put(resolvedMethodRef, reflectionApiMethod);
                    set1.add((ProgramClass) this.classFile);
                }
            } catch (ZkmClassNotFoundException zkmClassNotFoundException1) {
                messageReporter1.reportWarning(
                        "CLASSPATH WARNING:",
                        "Reference to method '"
                                + resolvedMethodRef.getDisplayString()
                                + "' couldn't be checked because couldn't find '"
                                + zkmClassNotFoundException1.getClassName()
                                + "'. Ensure appropriate classpath is set BEFORE classes are loaded. (1)"
                );
            } catch (ClassFileLoadException classFileLoadException1) {
                messageReporter1.reportWarning(
                        "CLASSPATH WARNING:",
                        "Reference to method '" + resolvedMethodRef.getDisplayString() + "' couldn't be checked. (1) : " + classFileLoadException1.getMessage()
                );
            }
        }

        int elementIndexValue = 0;
        int bc = 0;

        for (ArrayList arrayList = super.interfaceMethodRefs; bc < arrayList.size(); arrayList = super.interfaceMethodRefs) {
            ResolvedMethodRef resolvedMethodRef1 = (ResolvedMethodRef) super.interfaceMethodRefs.get(elementIndexValue);

            try {
                ReflectionApiMethod reflectionApiMethod1;
                if ((reflectionApiMethod1 = resolvedMethodRef1.findReflectionApiMethod(classMemberLookup1)) != null) {
                    map1.put(resolvedMethodRef1, reflectionApiMethod1);
                    set1.add((ProgramClass) this.classFile);
                }
            } catch (ZkmClassNotFoundException zkmClassNotFoundException) {
                messageReporter1.reportWarning(
                        "CLASSPATH WARNING:",
                        "Reference to method '"
                                + resolvedMethodRef1.getDisplayString()
                                + "' couldn't be checked because couldn't find '"
                                + zkmClassNotFoundException.getClassName()
                                + "'. Ensure appropriate classpath is set BEFORE classes are loaded. (2)"
                );
            } catch (ClassFileLoadException classFileLoadException) {
                messageReporter1.reportWarning(
                        "CLASSPATH WARNING:",
                        "Reference to method '" + resolvedMethodRef1.getDisplayString() + "' couldn't be checked. (2) : " + classFileLoadException.getMessage()
                );
            }

            bc = ++elementIndexValue;
        }
    }

    @Override
    public void applyClassRenames(Object object, Object object1) throws ZkmException, IOException {
        MessageReporter messageReporter1 = (MessageReporter) object1;
        HashMap hashMap = (HashMap) object;
        HashMap hashMap1 = ZkmUtils.createHashMap(ZkmUtils.getPrimeCapacity(super.classesByName.size()));
        Iterator iterator = this.classConstants.iterator();

        while (iterator.hasNext()) {
            ResolvedClassConstant resolvedClassConstant = (ResolvedClassConstant) iterator.next();
            resolvedClassConstant.remapName(hashMap);
            hashMap1.put(resolvedClassConstant.getClassName(), resolvedClassConstant);
        }

        super.classesByName = hashMap1;
        int nameAndTypesSize = this.nameAndTypes.size();

        for (int elementIndex = 0; elementIndex < nameAndTypesSize; elementIndex++) {
            if (this.nameAndTypes.get(elementIndex) != null) {
                ((ResolvedNameAndType) this.nameAndTypes.get(elementIndex)).remapDescriptor(hashMap);
            }
        }

        int methodTypeConstantsSize = this.methodTypeConstants.size();

        for (int elementIndexValue = 0; elementIndexValue < methodTypeConstantsSize; elementIndexValue++) {
            if (this.methodTypeConstants.get(elementIndexValue) != null) {
                ((ResolvedMethodType) this.methodTypeConstants.get(elementIndexValue)).remapDescriptor(hashMap);
            }
        }

        HashSet hashSet = ZkmUtils.createHashSet(13);
        Iterator iterator1 = this.methodRefs.iterator();

        while (iterator1.hasNext()) {
            ResolvedMethodRefConstant resolvedMethodRefConstant = (ResolvedMethodRefConstant) iterator1.next();
            AbstractMethodInfo abstractMethodInfo = (AbstractMethodInfo) resolvedMethodRefConstant.getResolvedMember();
            this.loadLibraryMethodOwner(abstractMethodInfo, hashSet, hashMap, messageReporter1);
        }

        iterator1 = super.interfaceMethodRefs.iterator();

        while (iterator1.hasNext()) {
            ResolvedInterfaceMethodRef resolvedInterfaceMethodRef = (ResolvedInterfaceMethodRef) iterator1.next();
            AbstractMethodInfo abstractMethodInfo1 = (AbstractMethodInfo) resolvedInterfaceMethodRef.getResolvedMember();
            this.loadLibraryMethodOwner(abstractMethodInfo1, hashSet, hashMap, messageReporter1);
        }

        iterator1 = super.invokeDynamics.iterator();

        while (iterator1.hasNext()) {
            ResolvedInvokeDynamic resolvedInvokeDynamic = (ResolvedInvokeDynamic) iterator1.next();
            AbstractMethodInfo abstractMethodInfo2 = resolvedInvokeDynamic.getTargetMethod();
            this.loadLibraryMethodOwner(abstractMethodInfo2, hashSet, hashMap, messageReporter1);
        }
    }

    public int computeDedupedEntryCount() {
        HashSet hashSet = ZkmUtils.createHashSet();
        HashSet hashSet1 = ZkmUtils.createHashSet();
        HashSet hashSet2 = ZkmUtils.createHashSet();
        Iterator iterator = this.utf8Constants.iterator();

        while (iterator.hasNext()) {
            ConstantUtf8 constantUtf8 = (ConstantUtf8) iterator.next();
            hashSet.add(constantUtf8.getConstantKey());
        }

        iterator = this.nameAndTypes.iterator();

        while (iterator.hasNext()) {
            ResolvedNameAndType resolvedNameAndType = (ResolvedNameAndType) iterator.next();
            hashSet1.add(resolvedNameAndType.getConstantKey());
        }

        iterator = this.stringConstants.iterator();

        while (iterator.hasNext()) {
            ResolvedStringConstant resolvedStringConstant = (ResolvedStringConstant) iterator.next();
            hashSet2.add(resolvedStringConstant.getConstantKey());
        }

        int ba = this.getEntryCount()
                - this.utf8Constants.size()
                + hashSet.size()
                - this.nameAndTypes.size()
                + hashSet1.size()
                - this.stringConstants.size()
                + hashSet2.size();
        if (ba > 65535) {
        }

        return ba;
    }

    public void clearOriginalStringFlags() {
        Iterator iterator = this.stringConstants.iterator();

        while (iterator.hasNext()) {
            ((ResolvedStringConstant) iterator.next()).setFromClassFile();
        }
    }

    public void removeUnusedEntries(UsedConstantsCollector usedConstantsCollector) {
        int classConstantsSize = this.classConstants.size();
        HashMap hashMap = ZkmUtils.createHashMap(ZkmUtils.getPrimeCapacity(classConstantsSize));
        ArrayList arrayList = new ArrayList(classConstantsSize);
        Set set1 = usedConstantsCollector.getUsedClasses();

        for (int elementIndex = 0; elementIndex < classConstantsSize; elementIndex++) {
            ResolvedClassConstant resolvedClassConstant = (ResolvedClassConstant) this.classConstants.get(elementIndex);
            if (set1.contains(resolvedClassConstant)) {
                arrayList.add(resolvedClassConstant);
                hashMap.put(resolvedClassConstant.getClassName(), resolvedClassConstant);
            }
        }

        super.classesByName = hashMap;
        this.classConstants = arrayList;
        classConstantsSize = this.utf8Constants.size();
        Set set2 = usedConstantsCollector.getUsedUtf8s();
        ArrayList arrayList1 = new ArrayList(classConstantsSize);

        for (int elementIndexValue = 0; elementIndexValue < classConstantsSize; elementIndexValue++) {
            ConstantUtf8 constantUtf8 = (ConstantUtf8) this.utf8Constants.get(elementIndexValue);
            if (set2.contains(constantUtf8)) {
                arrayList1.add(constantUtf8);
            }
        }

        this.utf8Constants = arrayList1;
        classConstantsSize = this.nameAndTypes.size();
        Set set3 = usedConstantsCollector.getUsedNameAndTypes();
        ArrayList arrayList2 = new ArrayList(classConstantsSize);

        for (int elementIndexValue2 = 0; elementIndexValue2 < classConstantsSize; elementIndexValue2++) {
            ResolvedNameAndType resolvedNameAndType = (ResolvedNameAndType) this.nameAndTypes.get(elementIndexValue2);
            if (set3.contains(resolvedNameAndType)) {
                arrayList2.add(resolvedNameAndType);
            }
        }

        this.nameAndTypes = arrayList2;
        classConstantsSize = this.fieldRefs.size();
        Set set4 = usedConstantsCollector.getUsedMemberRefs();
        ArrayList arrayList3 = new ArrayList(classConstantsSize);

        for (int elementIndexValue3 = 0; elementIndexValue3 < classConstantsSize; elementIndexValue3++) {
            ResolvedFieldRef resolvedFieldRef = (ResolvedFieldRef) this.fieldRefs.get(elementIndexValue3);
            if (set4.contains(resolvedFieldRef)) {
                arrayList3.add(resolvedFieldRef);
            }
        }

        this.fieldRefs = arrayList3;
        classConstantsSize = this.methodRefs.size();
        ArrayList arrayList4 = new ArrayList(classConstantsSize);

        for (int elementIndexValue4 = 0; elementIndexValue4 < classConstantsSize; elementIndexValue4++) {
            ResolvedMethodRefConstant resolvedMethodRefConstant = (ResolvedMethodRefConstant) this.methodRefs.get(elementIndexValue4);
            if (set4.contains(resolvedMethodRefConstant)) {
                arrayList4.add(resolvedMethodRefConstant);
            }
        }

        this.methodRefs = arrayList4;
        classConstantsSize = super.interfaceMethodRefs.size();
        ArrayList arrayList5 = new ArrayList(classConstantsSize);

        for (int elementIndexValue5 = 0; elementIndexValue5 < classConstantsSize; elementIndexValue5++) {
            ResolvedInterfaceMethodRef resolvedInterfaceMethodRef = (ResolvedInterfaceMethodRef) super.interfaceMethodRefs.get(elementIndexValue5);
            if (set4.contains(resolvedInterfaceMethodRef)) {
                arrayList5.add(resolvedInterfaceMethodRef);
            }
        }

        super.interfaceMethodRefs = arrayList5;
        classConstantsSize = this.stringConstants.size();
        Set set5 = usedConstantsCollector.getUsedLiterals();
        ArrayList arrayList6 = new ArrayList(classConstantsSize);

        for (int elementIndexValue6 = 0; elementIndexValue6 < classConstantsSize; elementIndexValue6++) {
            ResolvedStringConstant resolvedStringConstant = (ResolvedStringConstant) this.stringConstants.get(elementIndexValue6);
            if (set5.contains(resolvedStringConstant)) {
                arrayList6.add(resolvedStringConstant);
            }
        }

        this.stringConstants = arrayList6;
        classConstantsSize = super.integerConstants.size();
        ArrayList arrayList7 = new ArrayList(classConstantsSize);

        for (int elementIndexValue7 = 0; elementIndexValue7 < classConstantsSize; elementIndexValue7++) {
            ConstantInteger constantInteger = (ConstantInteger) super.integerConstants.get(elementIndexValue7);
            if (set5.contains(constantInteger)) {
                arrayList7.add(constantInteger);
            }
        }

        super.integerConstants = arrayList7;
        classConstantsSize = super.longConstants.size();
        ArrayList arrayList8 = new ArrayList(classConstantsSize);

        for (int elementIndexValue8 = 0; elementIndexValue8 < classConstantsSize; elementIndexValue8++) {
            ConstantLong constantLong = (ConstantLong) super.longConstants.get(elementIndexValue8);
            if (set5.contains(constantLong)) {
                arrayList8.add(constantLong);
            }
        }

        super.longConstants = arrayList8;
        classConstantsSize = this.floatConstants.size();
        ArrayList arrayList9 = new ArrayList(classConstantsSize);

        for (int elementIndexValue9 = 0; elementIndexValue9 < classConstantsSize; elementIndexValue9++) {
            ConstantFloat constantFloat = (ConstantFloat) this.floatConstants.get(elementIndexValue9);
            if (set5.contains(constantFloat)) {
                arrayList9.add(constantFloat);
            }
        }

        this.floatConstants = arrayList9;
        classConstantsSize = this.doubleConstants.size();
        ArrayList arrayList10 = new ArrayList(classConstantsSize);

        for (int elementIndexValue10 = 0; elementIndexValue10 < classConstantsSize; elementIndexValue10++) {
            ConstantDouble constantDouble = (ConstantDouble) this.doubleConstants.get(elementIndexValue10);
            if (set5.contains(constantDouble)) {
                arrayList10.add(constantDouble);
            }
        }

        this.doubleConstants = arrayList10;
        classConstantsSize = super.constantDynamics.size();
        Set set6 = usedConstantsCollector.getUsedConstantDynamics();
        ArrayList arrayList11 = new ArrayList(classConstantsSize);

        for (int elementIndexValue11 = 0; elementIndexValue11 < classConstantsSize; elementIndexValue11++) {
            ResolvedConstantDynamic resolvedConstantDynamic = (ResolvedConstantDynamic) super.constantDynamics.get(elementIndexValue11);
            if (set6.contains(resolvedConstantDynamic)) {
                arrayList11.add(resolvedConstantDynamic);
            }
        }

        super.constantDynamics = arrayList11;
        classConstantsSize = super.invokeDynamics.size();
        Set set7 = usedConstantsCollector.getUsedInvokeDynamics();
        ArrayList arrayList12 = new ArrayList(classConstantsSize);

        for (int elementIndexValue12 = 0; elementIndexValue12 < classConstantsSize; elementIndexValue12++) {
            ResolvedInvokeDynamic resolvedInvokeDynamic = (ResolvedInvokeDynamic) super.invokeDynamics.get(elementIndexValue12);
            if (set7.contains(resolvedInvokeDynamic)) {
                arrayList12.add(resolvedInvokeDynamic);
            }
        }

        super.invokeDynamics = arrayList12;
        classConstantsSize = this.methodTypeConstants.size();
        Set set8 = usedConstantsCollector.getUsedMethodTypes();
        ArrayList arrayList13 = new ArrayList(classConstantsSize);

        for (int elementIndexValue13 = 0; elementIndexValue13 < classConstantsSize; elementIndexValue13++) {
            ResolvedMethodType resolvedMethodType = (ResolvedMethodType) this.methodTypeConstants.get(elementIndexValue13);
            if (set8.contains(resolvedMethodType)) {
                arrayList13.add(resolvedMethodType);
            }
        }

        this.methodTypeConstants = arrayList13;
        classConstantsSize = this.methodHandleConstants.size();
        Set set9 = usedConstantsCollector.getUsedMethodHandles();
        ArrayList arrayList14 = new ArrayList(classConstantsSize);

        for (int elementIndexValue14 = 0; elementIndexValue14 < classConstantsSize; elementIndexValue14++) {
            ResolvedMethodHandleConstant resolvedMethodHandleConstant = (ResolvedMethodHandleConstant) this.methodHandleConstants.get(elementIndexValue14);
            if (set9.contains(resolvedMethodHandleConstant)) {
                arrayList14.add(resolvedMethodHandleConstant);
            }
        }

        this.methodHandleConstants = arrayList14;
        int lengthValue = this.entries.length;
        ArrayList arrayList15 = new ArrayList(classConstantsSize);

        for (int entryIndex = 0; entryIndex < lengthValue; entryIndex++) {
            ConstantPoolEntry constantPoolEntry = this.entries[entryIndex];
            if (!(constantPoolEntry instanceof ConstantNullEntry) && !usedConstantsCollector.isUsed(constantPoolEntry)) {
                if (constantPoolEntry.getSlotCount() == 2) {
                    entryIndex++;
                }
            } else {
                arrayList15.add(constantPoolEntry);
            }
        }

        this.entries = ((com.zelix.klassmaster.classfile.constpool.ConstantPoolEntry[]) (arrayList15.toArray(new ConstantPoolEntry[arrayList15.size()])));
        this.renumberEntries();
        this.baseEntryCount = this.entries.length;
    }

    public void linkBootstrapMethods(BootstrapMethodsAttribute bootstrapMethodsAttribute1) {
        Iterator iterator = super.invokeDynamics.iterator();

        while (iterator.hasNext()) {
            ResolvedInvokeDynamic resolvedInvokeDynamic = (ResolvedInvokeDynamic) iterator.next();
            resolvedInvokeDynamic.setBootstrapMethod(bootstrapMethodsAttribute1.getEntry(resolvedInvokeDynamic.getBootstrapMethodIndex()));
        }

        iterator = super.constantDynamics.iterator();

        while (iterator.hasNext()) {
            ResolvedConstantDynamic resolvedConstantDynamic = (ResolvedConstantDynamic) iterator.next();
            resolvedConstantDynamic.setBootstrapMethod(bootstrapMethodsAttribute1.getEntry(resolvedConstantDynamic.getBootstrapMethodIndex()));
        }
    }
}

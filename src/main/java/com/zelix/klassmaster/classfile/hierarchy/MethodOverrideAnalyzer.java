package com.zelix.klassmaster.classfile.hierarchy;

import com.zelix.klassmaster.classfile.AbstractMethodInfo;
import com.zelix.klassmaster.classfile.ClassFileBase;
import com.zelix.klassmaster.classfile.MemberSignatureBase;
import com.zelix.klassmaster.classfile.MethodSignature;
import com.zelix.klassmaster.classfile.ProgramClass;
import com.zelix.klassmaster.config.HiddenOptionFlags;
import com.zelix.klassmaster.exceptions.ClassFileLoadException;
import com.zelix.klassmaster.exceptions.MissingClassException;
import com.zelix.klassmaster.exceptions.ZkmClassNotFoundException;
import com.zelix.klassmaster.exceptions.ZkmException;
import com.zelix.klassmaster.obfuscator.parameters.ChangedMethodDescriptor;
import com.zelix.klassmaster.script.ScriptEnvironment;
import com.zelix.klassmaster.util.EnumerableHashSet;
import com.zelix.klassmaster.util.EnumerableMap;
import com.zelix.klassmaster.util.IndexedValueRelation;
import com.zelix.klassmaster.util.ListMultimap;
import com.zelix.klassmaster.util.NestedMultiMap;
import com.zelix.klassmaster.util.ObjectPair;
import com.zelix.klassmaster.util.ObservableHolder;
import com.zelix.klassmaster.util.SetMultiMap;
import com.zelix.klassmaster.util.TwoKeyMap;
import com.zelix.klassmaster.util.ZkmAssert;
import com.zelix.klassmaster.util.ZkmUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Vector;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public class MethodOverrideAnalyzer {
    public static final NameReservationMark VIRTUAL_RESERVATION = new NameReservationMark(1);
    public static final NameReservationMark STATIC_RESERVATION = new NameReservationMark(2);
    public static final NameReservationMark PRIVATE_RESERVATION = new NameReservationMark(3);
    private Map rootMethodByMethod;
    public IndexedValueRelation methodsByRoot;
    public final SetMultiMap interfaceImplementors = new SetMultiMap();
    public TwoKeyMap interfaceMethodGroups = new TwoKeyMap();
    public Map groupRenameTargets = ZkmUtils.createHashMap();
    public Set reservedSignatures = ZkmUtils.createHashSet();
    public SetMultiMap relatedInterfaceNodes = new SetMultiMap();
    public final ClassResolver classResolver;
    public final boolean useFullDescriptor;
    public final boolean changesDescriptors;
    public TwoKeyMap nameReservations;
    public TwoKeyMap visibleMethodOwners;
    public TwoKeyMap overriddenMethodOwners;
    public TwoKeyMap determiningInterfaceMethods;

    public final String getGroupNewName(Object key) {
        return (String) ((ObservableHolder) this.groupRenameTargets.get(key)).getValue();
    }

    public final boolean tryReserveGroupRename(
            ClassHierarchyNode classHierarchyNode, MethodSignature methodSignature1, MethodSignature methodSignature2, boolean bl, ObservableHolder observableHolder
    ) throws ZkmException, IOException {
        MemberSignatureBase memberSignatureBase;
        MemberSignatureBase memberSignatureBase1;
        if (this.useFullDescriptor) {
            memberSignatureBase = methodSignature1;
            memberSignatureBase1 = methodSignature2;
        } else {
            memberSignatureBase = methodSignature1.getNameTypeSignature();
            memberSignatureBase1 = methodSignature2.getNameTypeSignature();
        }

        HashSet hashSet = ZkmUtils.createHashSet();
        HashSet hashSet1 = ZkmUtils.createHashSet();
        this.collectGroupNodes(classHierarchyNode, memberSignatureBase1, hashSet, hashSet1);
        HashSet hashSet2 = ZkmUtils.createHashSet(ZkmUtils.getPrimeCapacity(hashSet.size() + hashSet1.size()));
        LinkedHashSet linkedHashSet = new LinkedHashSet();
        Iterator iterator = hashSet.iterator();

        while (iterator.hasNext()) {
            ClassHierarchyNode classHierarchyNode1 = (ClassHierarchyNode) iterator.next();
            int ba = this.reserveName(classHierarchyNode1, memberSignatureBase, memberSignatureBase1, observableHolder);
            switch (ba) {
                case -1:
                    this.rollbackReservations(hashSet2, linkedHashSet, memberSignatureBase);
                    return false;
                case 0:
                    hashSet2.add(classHierarchyNode1);
                case 1:
                case 2:
                default:
                    if (!this.reserveInImplementors(classHierarchyNode1, memberSignatureBase, memberSignatureBase1, hashSet2, linkedHashSet, observableHolder)) {
                        return false;
                    }
            }
        }

        iterator = hashSet1.iterator();

        while (iterator.hasNext()) {
            ClassHierarchyNode classHierarchyNode2 = (ClassHierarchyNode) iterator.next();
            int bb;
            if (bl) {
                bb = this.reserveVirtualStrict(classHierarchyNode2, memberSignatureBase, memberSignatureBase1, observableHolder);
            } else {
                bb = this.reserveVirtual(classHierarchyNode2, memberSignatureBase, memberSignatureBase1, linkedHashSet, observableHolder);
            }

            switch (bb) {
                case -1:
                    this.rollbackReservations(hashSet2, linkedHashSet, memberSignatureBase);
                    return false;
                case 0:
                    hashSet2.add(classHierarchyNode2);
                case 1:
                case 2:
            }
        }

        return true;
    }

    public final void setGroupChangedDescriptor(Object key, Object value) throws ZkmException, IOException {
        ((ObservableHolder) this.groupRenameTargets.get(key)).setValue(value);
    }

    public final ClassFileBase findTopOverriddenOwner(ClassFileBase classFileBase, MethodSignature methodSignature1) {
        String className = classFileBase.getClassName();
        HashSet hashSet = null;
        ClassFileBase classFileBase1 = (ClassFileBase) this.overriddenMethodOwners.getValue(className, methodSignature1);
        ClassFileBase classFileBase2 = null;

        while (classFileBase1 != null) {
            classFileBase2 = classFileBase1;
            classFileBase1 = (ClassFileBase) this.overriddenMethodOwners.getValue(classFileBase1.getClassName(), methodSignature1);
            if (classFileBase1 != null) {
                if (hashSet == null) {
                    hashSet = ZkmUtils.createHashSet(13);
                }

                if (!hashSet.add(classFileBase1)) {
                    StringBuffer stringBuffer = new StringBuffer();
                    Iterator iterator = hashSet.iterator();

                    while (iterator.hasNext()) {
                        ClassFileBase classFileBase3 = (ClassFileBase) iterator.next();
                        stringBuffer.append("'" + classFileBase3.getClassName() + "'");
                        if (iterator.hasNext()) {
                            stringBuffer.append(", ");
                        }
                    }

                    ZkmAssert.assertTrue(false, new String[]{"LOOP detected..." + methodSignature1 + HiddenOptionFlags.LINE_SEPARATOR + stringBuffer});
                }
            }
        }

        return classFileBase2;
    }

    public final ObservableHolder getInterfaceMethodGroup(Object key, Object keyArgument) {
        return (ObservableHolder) this.interfaceMethodGroups.getValue(key, keyArgument);
    }

    public Map getVisibleMethodOwners(Object key) {
        return this.visibleMethodOwners.getInnerMap(key);
    }

    public int reserveVirtual(
            ClassHierarchyNode classHierarchyNode, MemberSignatureBase memberSignatureBase, Object object, Set set1, ObservableHolder observableHolder
    ) throws ZkmException, IOException {
        if (classHierarchyNode.hasNoClassFile()) {
            return 2;
        }

        NameReservationMark nameReservationMark = (NameReservationMark) this.nameReservations
                .putValue(classHierarchyNode, memberSignatureBase, VIRTUAL_RESERVATION);
        if (nameReservationMark == null) {
            return 0;
        }

        if (nameReservationMark == PRIVATE_RESERVATION || nameReservationMark == STATIC_RESERVATION) {
            PendingHierarchyLookup pendingHierarchyLookup = new PendingHierarchyLookup(classHierarchyNode, nameReservationMark, VIRTUAL_RESERVATION);
            set1.add(pendingHierarchyLookup);
            return 0;
        }

        if (nameReservationMark == VIRTUAL_RESERVATION) {
            return 1;
        }

        TwoKeyMap twoKeyMap;
        if (nameReservationMark.reservedSignature != null) {
            if (nameReservationMark.reservedSignature.equals(object)) {
                this.nameReservations.putValue(classHierarchyNode, memberSignatureBase, nameReservationMark);
                return 1;
            }

            twoKeyMap = this.nameReservations;
        } else {
            twoKeyMap = this.nameReservations;
        }

        twoKeyMap.putValue(classHierarchyNode, memberSignatureBase, nameReservationMark);
        observableHolder.setValue(nameReservationMark.reservedSignature);
        return -1;
    }

    public final boolean tryRenameGroup(
            ObservableHolder observableHolder, MethodSignature methodSignature1, MethodSignature methodSignature2, ObservableHolder observableHolder1
    ) throws ZkmException, IOException {
        return this.tryRenameGroup(observableHolder, methodSignature1, methodSignature2, observableHolder1, (ChangedMethodDescriptor) null);
    }

    public boolean tryReserveVirtual(
            ClassHierarchyNode classHierarchyNode,
            MemberSignatureBase memberSignatureBase,
            MemberSignatureBase memberSignatureBase1,
            Set set1,
            Set set2,
            ObservableHolder observableHolder
    ) throws ZkmException, IOException {
        switch (this.reserveVirtual(classHierarchyNode, memberSignatureBase, memberSignatureBase1, set2, observableHolder)) {
            case -1:
                this.rollbackReservations(set1, set2, memberSignatureBase);
                return false;
            case 0:
                set1.add(classHierarchyNode);
            case 1:
            case 2:
            default:
                return true;
        }
    }

    public boolean tryReservePrivate(ClassHierarchyNode classHierarchyNode, MethodSignature methodSignature1, ObservableHolder observableHolder) throws ZkmException, IOException {
        MemberSignatureBase memberSignatureBase;
        if (this.useFullDescriptor) {
            memberSignatureBase = methodSignature1;
        } else {
            memberSignatureBase = methodSignature1.getNameTypeSignature();
        }

        return this.reservePrivate(classHierarchyNode, memberSignatureBase, observableHolder) != -1;
    }

    public Map getOverriddenMethodOwners(Object key) {
        return this.overriddenMethodOwners.getInnerMap(key);
    }

    public final AbstractMethodInfo getRootMethod(AbstractMethodInfo abstractMethodInfo) {
        AbstractMethodInfo abstractMethodInfo1 = (AbstractMethodInfo) this.rootMethodByMethod.get(abstractMethodInfo);
        return abstractMethodInfo1 != null ? abstractMethodInfo1 : abstractMethodInfo;
    }

    public boolean reserveInSuperclasses(
            ClassHierarchyNode classHierarchyNode,
            MemberSignatureBase memberSignatureBase,
            MemberSignatureBase memberSignatureBase1,
            Set set1,
            Set set2,
            Set set3,
            AbstractMethodInfo abstractMethodInfo,
            ObservableHolder observableHolder
    ) throws ZkmException, IOException {
        ClassHierarchyNode classHierarchyNode1 = classHierarchyNode.getSuperclassNode();
        if (classHierarchyNode1 != null) {
            int ba;
            if (abstractMethodInfo.isStrictlyPrivate()) {
                ba = this.reservePrivate(classHierarchyNode1, memberSignatureBase, observableHolder);
            } else if (abstractMethodInfo.isStatic()) {
                ba = this.reserveStatic(classHierarchyNode1, memberSignatureBase, observableHolder);
            } else {
                ba = this.reserveVirtual(classHierarchyNode1, memberSignatureBase, memberSignatureBase1, set2, observableHolder);
            }

            switch (ba) {
                case -1:
                    this.rollbackReservations(set1, set2, memberSignatureBase);
                    return false;
                case 0:
                    set1.add(classHierarchyNode1);
                default:
                    classHierarchyNode1.collectAllInterfaces(set3);
                    if (!this.reserveInSuperclasses(
                            classHierarchyNode1, memberSignatureBase, memberSignatureBase1, set1, set2, set3, abstractMethodInfo, observableHolder
                    )) {
                        return false;
                    }
                    break;
                case 1:
                case 2:
                    return true;
            }
        }

        return true;
    }

    public int reserveName(
            ClassHierarchyNode classHierarchyNode,
            MemberSignatureBase memberSignatureBase,
            MemberSignatureBase memberSignatureBase1,
            ObservableHolder observableHolder
    ) throws ZkmException, IOException {
        if (classHierarchyNode.hasNoClassFile()) {
            return 2;
        }

        NameReservationMark nameReservationMark = new NameReservationMark(memberSignatureBase1);
        NameReservationMark nameReservationMark1 = (NameReservationMark) this.nameReservations
                .putValue(classHierarchyNode, memberSignatureBase, nameReservationMark);
        if (nameReservationMark1 == null) {
            return 0;
        }

        TwoKeyMap twoKeyMap;
        if (nameReservationMark1.reservedSignature != null) {
            if (nameReservationMark1.reservedSignature.equals(memberSignatureBase1)) {
                return 1;
            }

            twoKeyMap = this.nameReservations;
        } else {
            twoKeyMap = this.nameReservations;
        }

        twoKeyMap.putValue(classHierarchyNode, memberSignatureBase, nameReservationMark1);
        observableHolder.setValue(nameReservationMark1.reservedSignature);
        return -1;
    }

    public Set getOverrideGroup(AbstractMethodInfo abstractMethodInfo) {
        if (!abstractMethodInfo.isStatic() && !abstractMethodInfo.isStrictlyPrivate()) {
            if (this.methodsByRoot == null) {
                this.buildMethodsByRoot();
            }

            AbstractMethodInfo abstractMethodInfo1 = this.findRootMethod(abstractMethodInfo);
            IndexedValueRelation indexedValueRelation;
            if (abstractMethodInfo1 == null) {
                abstractMethodInfo1 = abstractMethodInfo;
                indexedValueRelation = this.methodsByRoot;
            } else {
                indexedValueRelation = this.methodsByRoot;
            }

            Set set1 = indexedValueRelation.getValues(abstractMethodInfo1);
            HashSet hashSet = null;
            if (set1 != null) {
                hashSet = ZkmUtils.createHashSetFrom(set1);
                hashSet.add(abstractMethodInfo1);
            }

            return hashSet;
        } else {
            return null;
        }
    }

    public final Set getInterfaceImplementors(Object key) {
        return this.interfaceImplementors.getValues(key);
    }

    public final Map putVisibleMethodOwners(String key, Map map1) {
        return this.visibleMethodOwners.putInnerMap(key, map1);
    }

    public Map getMethodSignatureByGroup() {
        HashMap hashMap = ZkmUtils.createHashMap();
        Iterator iterator = this.interfaceMethodGroups.entrySet().iterator();

        while (iterator.hasNext()) {
            Iterator iterator1 = ((Map) ((Entry) iterator.next()).getValue()).entrySet().iterator();

            while (iterator1.hasNext()) {
                Entry entry = (Entry) iterator1.next();
                MethodSignature methodSignature1 = (MethodSignature) entry.getKey();
                ObservableHolder observableHolder = (ObservableHolder) entry.getValue();
                hashMap.put(observableHolder, methodSignature1);
            }
        }

        return hashMap;
    }

    public boolean reserveInSubclasses(
            ClassHierarchyNode classHierarchyNode,
            MemberSignatureBase memberSignatureBase,
            MemberSignatureBase memberSignatureBase1,
            Set set1,
            Set set2,
            Set set3,
            AbstractMethodInfo abstractMethodInfo,
            ObservableHolder observableHolder
    ) throws ZkmException, IOException {
        int ba;
        if (abstractMethodInfo.isStrictlyPrivate() || abstractMethodInfo.isConstructor()) {
            ba = this.reservePrivate(classHierarchyNode, memberSignatureBase, observableHolder);
        } else if (abstractMethodInfo.isStatic()) {
            ba = this.reserveStatic(classHierarchyNode, memberSignatureBase, observableHolder);
        } else {
            ba = this.reserveName(classHierarchyNode, memberSignatureBase, memberSignatureBase1, observableHolder);
        }

        switch (ba) {
            case -1:
                this.rollbackReservations(set1, set2, memberSignatureBase);
                return false;
            case 0:
                set1.add(classHierarchyNode);
            case 1:
            case 2:
        }

        classHierarchyNode.collectAllInterfaces(set3);
        if (!abstractMethodInfo.isStrictlyPrivate() && !abstractMethodInfo.isStatic() && !abstractMethodInfo.isStaticInitializer()) {
            Enumeration enumeration = classHierarchyNode.enumerateSubclasses();
            if (enumeration != null) {
                while (enumeration.hasMoreElements()) {
                    ClassHierarchyNode classHierarchyNode1 = (ClassHierarchyNode) enumeration.nextElement();
                    if (!this.reserveInSubclasses(
                            classHierarchyNode1, memberSignatureBase, memberSignatureBase1, set1, set2, set3, abstractMethodInfo, observableHolder
                    )) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    public final AbstractMethodInfo findRootMethod(Object key) {
        return (AbstractMethodInfo) this.rootMethodByMethod.get(key);
    }

    public void collectGroupNodes(ClassHierarchyNode classHierarchyNode, MemberSignatureBase memberSignatureBase, Set set1, Set set2) {
        ObservableHolder observableHolder = this.getInterfaceMethodGroup(classHierarchyNode, new MethodSignature(memberSignatureBase));
        if (observableHolder != null) {
            List list1 = (List) observableHolder.getValue();

            for (int elementIndex = 0; elementIndex < list1.size(); elementIndex++) {
                ClassHierarchyNode classHierarchyNode1 = (ClassHierarchyNode) list1.get(elementIndex);
                set1.add(classHierarchyNode1);
            }
        } else {
            set1.add(classHierarchyNode);
        }

        Iterator iterator = set1.iterator();

        while (iterator.hasNext()) {
            ClassHierarchyNode classHierarchyNode2 = (ClassHierarchyNode) iterator.next();
            this.addRelatedInterfaceNodes(classHierarchyNode2, set2);
        }
    }

    public final void reserveSignature(MethodSignature methodSignature1) {
        MemberSignatureBase memberSignatureBase;
        Set set1;
        if (this.useFullDescriptor) {
            memberSignatureBase = methodSignature1;
            set1 = this.reservedSignatures;
        } else {
            memberSignatureBase = methodSignature1.getNameTypeSignature();
            set1 = this.reservedSignatures;
        }

        set1.add(memberSignatureBase);
    }

    public final void setGroupNewName(Object key, Object value) throws ZkmException, IOException {
        ((ObservableHolder) this.groupRenameTargets.get(key)).setValue(value);
    }

    public int reserveStatic(ClassHierarchyNode classHierarchyNode, MemberSignatureBase memberSignatureBase, ObservableHolder observableHolder) throws ZkmException, IOException {
        if (classHierarchyNode.hasNoClassFile()) {
            return 2;
        } else {
            NameReservationMark nameReservationMark = (NameReservationMark) this.nameReservations
                    .putValue(classHierarchyNode, memberSignatureBase, STATIC_RESERVATION);
            if (nameReservationMark == null) {
                return 0;
            } else if (nameReservationMark == PRIVATE_RESERVATION) {
                this.nameReservations.putValue(classHierarchyNode, memberSignatureBase, nameReservationMark);
                return 1;
            } else if (nameReservationMark == STATIC_RESERVATION) {
                return 1;
            } else if (nameReservationMark == VIRTUAL_RESERVATION) {
                this.nameReservations.putValue(classHierarchyNode, memberSignatureBase, VIRTUAL_RESERVATION);
                return 1;
            } else {
                this.nameReservations.putValue(classHierarchyNode, memberSignatureBase, nameReservationMark);
                observableHolder.setValue(nameReservationMark.reservedSignature);
                return -1;
            }
        }
    }

    public boolean hasRootMethod(Object key) {
        return this.rootMethodByMethod.containsKey(key);
    }

    public final void collectImplementors(
            ClassHierarchyNode classHierarchyNode, ClassHierarchyNode classHierarchyNode1, boolean bl, Set set1, TwoKeyMap twoKeyMap
    ) {
        boolean bl1 = bl;
        if (bl1 && !classHierarchyNode1.isInterface()) {
            bl1 = false;
            ArrayList arrayList = new ArrayList(5);
            classHierarchyNode1.collectSuperclasses(arrayList);

            for (int elementIndex = 0; elementIndex < arrayList.size(); elementIndex++) {
                ClassHierarchyNode classHierarchyNode2 = (ClassHierarchyNode) arrayList.get(elementIndex);
                if (twoKeyMap.containsKeys(classHierarchyNode2, classHierarchyNode)) {
                    break;
                }

                twoKeyMap.putValue(classHierarchyNode2, classHierarchyNode, "d");
                set1.add(classHierarchyNode2);
            }
        }

        Enumeration enumeration = classHierarchyNode1.enumerateSubclasses();
        if (enumeration != null) {
            while (enumeration.hasMoreElements()) {
                ClassHierarchyNode classHierarchyNode3 = (ClassHierarchyNode) enumeration.nextElement();
                twoKeyMap.putValue(classHierarchyNode3, classHierarchyNode, "i");
                this.collectImplementors(classHierarchyNode, classHierarchyNode3, bl1, set1, twoKeyMap);
                set1.add(classHierarchyNode3);
            }
        }

        Enumeration enumeration1 = classHierarchyNode1.enumerateImplementors();
        if (enumeration1 != null) {
            while (enumeration1.hasMoreElements()) {
                ClassHierarchyNode classHierarchyNode4 = (ClassHierarchyNode) enumeration1.nextElement();
                twoKeyMap.putValue(classHierarchyNode4, classHierarchyNode, "i");
                this.collectImplementors(classHierarchyNode, classHierarchyNode4, bl1, set1, twoKeyMap);
                if (!classHierarchyNode4.isInterface()) {
                    set1.add(classHierarchyNode4);
                }
            }
        }
    }

    public final boolean isReserved(ClassHierarchyNode classHierarchyNode, MemberSignatureBase memberSignatureBase) {
        return this.nameReservations.containsKeys(classHierarchyNode, memberSignatureBase);
    }

    public MethodOverrideAnalyzer(
            ClassRepository classRepository1,
            ClassHierarchy classHierarchy1,
            ClassResolver classResolver1,
            ScriptEnvironment scriptEnvironment1,
            int ba,
            boolean useFullDescriptor,
            boolean bl1,
            boolean changesDescriptors
    ) throws ZkmException, IOException {
        this.classResolver = classResolver1;
        this.useFullDescriptor = useFullDescriptor;
        this.changesDescriptors = changesDescriptors;
        this.nameReservations = new TwoKeyMap(ba, 20);
        this.visibleMethodOwners = new TwoKeyMap(ba, 50);
        this.overriddenMethodOwners = new TwoKeyMap(ba, 50);
        this.determiningInterfaceMethods = new TwoKeyMap(101, 37);
        this.analyze(classRepository1, classHierarchy1, scriptEnvironment1, bl1);
    }

    public void analyze(ClassMemberLookup classMemberLookup1, ClassHierarchy classHierarchy1, ScriptEnvironment scriptEnvironment1, boolean bl) throws ZkmException, IOException {
        List list1 = classHierarchy1.getTopLoadedNodes();
        int list1Size = list1.size();
        if (!bl) {
            this.rootMethodByMethod = ZkmUtils.createHashMap();
        } else {
            Random random1 = ZkmUtils.createRandom(2049);
            ObservableHolder observableHolder = new ObservableHolder();

            for (int elementIndex = 0; elementIndex < list1Size; elementIndex++) {
                ClassHierarchyNode classHierarchyNode = (ClassHierarchyNode) list1.get(elementIndex);
                ClassFileBase classFileBase = classHierarchyNode.getClassFile();
                String className = classFileBase.getClassName();
                String string1 = "looking for superclass of '" + classFileBase.getLocationName() + "'";
                this.getOrBuildVisibleMethodOwners(className, observableHolder, scriptEnvironment1, random1, string1);
                classHierarchyNode.analyzeSubclassOverrides(this, this.classResolver, observableHolder, scriptEnvironment1, random1);
            }

            NestedMultiMap nestedMultiMap = new NestedMultiMap();
            TwoKeyMap twoKeyMap = new TwoKeyMap();
            TwoKeyMap twoKeyMap1 = new TwoKeyMap();

            for (int elementIndexValue = 0; elementIndexValue < list1Size; elementIndexValue++) {
                ClassHierarchyNode classHierarchyNode4 = (ClassHierarchyNode) list1.get(elementIndexValue);
                ClassFileBase classFileBase1 = classHierarchyNode4.getClassFile();
                if (classFileBase1.isInterface()) {
                    String classNameValue = classFileBase1.getClassName();
                    Map map1 = this.getOverriddenMethodOwners(classNameValue);
                    Iterator iterator = map1.entrySet().iterator();

                    while (iterator.hasNext()) {
                        Entry entry = (Entry) iterator.next();
                        MethodSignature methodSignature1 = (MethodSignature) entry.getKey();
                        ClassFileBase classFileBase2 = (ClassFileBase) entry.getValue();
                        ClassHierarchyNode classHierarchyNode1 = ClassHierarchyNode.findNode(classFileBase2.getClassName());
                        if (classHierarchyNode1 != null && !classHierarchyNode1.hasNoClassFile()) {
                            nestedMultiMap.addValue(methodSignature1, classHierarchyNode1, classHierarchyNode4);
                        } else {
                            twoKeyMap1.putValue(classHierarchyNode4, methodSignature1, classFileBase2);
                        }
                    }

                    HashSet hashSet = ZkmUtils.createHashSet();
                    this.interfaceImplementors.putValueSet(classHierarchyNode4, hashSet);
                    this.collectImplementors(classHierarchyNode4, classHierarchyNode4, true, hashSet, twoKeyMap);
                    twoKeyMap.putValue(classHierarchyNode4, classHierarchyNode4, "s");
                }
            }

            Enumeration enumeration = nestedMultiMap.keys();

            while (enumeration.hasMoreElements()) {
                MethodSignature methodSignature2 = (MethodSignature) enumeration.nextElement();
                ListMultimap listMultimap = nestedMultiMap.getMultimap(methodSignature2);
                ArrayList arrayList = new ArrayList();
                Enumeration enumeration1 = listMultimap.keys();

                while (enumeration1.hasMoreElements()) {
                    ClassHierarchyNode classHierarchyNode6 = (ClassHierarchyNode) enumeration1.nextElement();
                    EnumerableHashSet enumerableHashSet = new EnumerableHashSet();
                    this.collectConnectedNodes(enumerableHashSet, classHierarchyNode6, listMultimap);
                    arrayList.add(enumerableHashSet);
                }

                enumeration1 = listMultimap.keys();

                while (enumeration1.hasMoreElements()) {
                    ClassHierarchyNode classHierarchyNode7 = (ClassHierarchyNode) enumeration1.nextElement();
                    EnumerableHashSet enumerableHashSet1 = null;
                    Iterator iterator4 = arrayList.iterator();

                    while (iterator4.hasNext()) {
                        EnumerableHashSet enumerableHashSet3 = (EnumerableHashSet) iterator4.next();
                        if (enumerableHashSet3.contains(classHierarchyNode7)) {
                            if (enumerableHashSet1 == null) {
                                enumerableHashSet1 = enumerableHashSet3;
                            } else {
                                Enumeration enumeration2 = enumerableHashSet3.elements();

                                while (enumeration2.hasMoreElements()) {
                                    ClassHierarchyNode classHierarchyNode2 = (ClassHierarchyNode) enumeration2.nextElement();
                                    enumerableHashSet1.add(classHierarchyNode2);
                                }

                                iterator4.remove();
                            }
                        }
                    }
                }

                for (int elementIndexValue2 = 0; elementIndexValue2 < arrayList.size(); elementIndexValue2++) {
                    EnumerableHashSet enumerableHashSet2 = (EnumerableHashSet) arrayList.get(elementIndexValue2);
                    Vector vector = new Vector(enumerableHashSet2.size());
                    ObservableHolder observableHolder1 = new ObservableHolder(vector);
                    this.groupRenameTargets.put(observableHolder1, new ObservableHolder());
                    Iterator iterator5 = enumerableHashSet2.iterator();

                    while (iterator5.hasNext()) {
                        ClassHierarchyNode classHierarchyNode9 = (ClassHierarchyNode) iterator5.next();
                        vector.addElement(classHierarchyNode9);
                        this.interfaceMethodGroups.putValue(classHierarchyNode9, methodSignature2, observableHolder1);
                    }

                    ClassFileBase classFileBase5 = null;

                    for (int elementIndexValue3 = 0; elementIndexValue3 < vector.size(); elementIndexValue3++) {
                        ClassHierarchyNode classHierarchyNode3 = (ClassHierarchyNode) vector.elementAt(elementIndexValue3);
                        ClassFileBase classFileBase3 = (ClassFileBase) twoKeyMap1.getValue(classHierarchyNode3, methodSignature2);
                        if (classFileBase3 != null) {
                            classFileBase5 = classFileBase3;
                            break;
                        }
                    }

                    if (classFileBase5 == null) {
                        ClassFileBase classFileBase7 = null;

                        for (int elementIndexValue4 = 0; elementIndexValue4 < vector.size(); elementIndexValue4++) {
                            ClassFileBase classFileBase4 = ((ClassHierarchyNode) vector.elementAt(elementIndexValue4)).getClassFile();
                            ClassFileBase classFileBase6 = this.findTopOverriddenOwner(classFileBase4, methodSignature2);
                            if (classFileBase4.isProgramClass() && classMemberLookup1.declaresMethod(methodSignature2, (ProgramClass) classFileBase4)
                                    || !classFileBase4.isProgramClass() && classFileBase4.findMethod(methodSignature2) != null) {
                                if (classFileBase6 == null) {
                                    classFileBase5 = classFileBase4;
                                    break;
                                }

                                classFileBase7 = classFileBase6;
                            }
                        }

                        if (classFileBase5 == null) {
                            classFileBase5 = classFileBase7;
                        }
                    }

                    AbstractMethodInfo abstractMethodInfo = classFileBase5.findMethod(methodSignature2);
                    ZkmAssert.assertNotNull(classFileBase5, "Failed to find deemed determining interface " + methodSignature2 + " " + vector);

                    for (int elementIndexValue5 = 0; elementIndexValue5 < vector.size(); elementIndexValue5++) {
                        ClassHierarchyNode classHierarchyNode10 = (ClassHierarchyNode) vector.elementAt(elementIndexValue5);
                        this.determiningInterfaceMethods
                                .putValue(classHierarchyNode10.getClassName(), methodSignature2, new ObjectPair(classFileBase5, abstractMethodInfo));
                    }
                }
            }

            enumeration = twoKeyMap.innerMaps();

            while (enumeration.hasMoreElements()) {
                Map map2 = (Map) enumeration.nextElement();
                if (map2.size() > 1) {
                    Iterator iterator2 = map2.entrySet().iterator();

                    while (iterator2.hasNext()) {
                        Entry entry2 = (Entry) iterator2.next();
                        ClassHierarchyNode classHierarchyNode5 = (ClassHierarchyNode) entry2.getKey();
                        String value = (String) entry2.getValue();
                        if (value != "d") {
                            Iterator iterator3 = map2.keySet().iterator();

                            while (iterator3.hasNext()) {
                                ClassHierarchyNode classHierarchyNode8 = (ClassHierarchyNode) iterator3.next();
                                if (classHierarchyNode5 != classHierarchyNode8) {
                                    this.relatedInterfaceNodes.addValue(classHierarchyNode5, classHierarchyNode8);
                                }
                            }
                        }
                    }
                }
            }

            this.rootMethodByMethod = HiddenOptionFlags.USE_PARALLEL ? new ConcurrentHashMap() : ZkmUtils.createHashMap();
            if (HiddenOptionFlags.USE_PARALLEL && HiddenOptionFlags.PROCESSOR_COUNT >= 2) {
                this.overriddenMethodOwners.entrySet().parallelStream().forEach(entryx -> this.resolveRootMethods(((java.util.Map.Entry) entryx)));
            } else {
                Iterator iterator1 = this.overriddenMethodOwners.entrySet().iterator();

                while (iterator1.hasNext()) {
                    Entry entry1 = (Entry) iterator1.next();
                    this.resolveRootMethods(entry1);
                }
            }
        }
    }

    public final ClassFileBase putVisibleMethodOwner(Object key, Object keyArgument, Object value) {
        return (ClassFileBase) this.visibleMethodOwners.putValue(key, keyArgument, value);
    }

    public void resolveRootMethods(Entry entry) {
        ClassHierarchyNode classHierarchyNode = ClassHierarchyNode.findNode((String) entry.getKey());
        if (classHierarchyNode != null && !classHierarchyNode.hasNoClassFile()) {
            ClassFileBase classFileBase = classHierarchyNode.getClassFile();
            Map map1 = (Map) entry.getValue();
            if (map1 != null) {
                Iterator iterator = map1.entrySet().iterator();

                while (iterator.hasNext()) {
                    Entry entry1 = (Entry) iterator.next();
                    MethodSignature methodSignature1 = (MethodSignature) entry1.getKey();
                    ClassFileBase classFileBase1 = (ClassFileBase) entry1.getValue();
                    AbstractMethodInfo abstractMethodInfo = classFileBase.findMethod(methodSignature1);
                    if (abstractMethodInfo != null && !abstractMethodInfo.isStatic() && !abstractMethodInfo.isStrictlyPrivate()) {
                        AbstractMethodInfo abstractMethodInfo1 = classFileBase1.findMethod(methodSignature1);
                        boolean bl;
                        ObjectPair objectPair;
                        if (classFileBase1.isInterface()
                                && (objectPair = (ObjectPair) this.determiningInterfaceMethods.getValue(classFileBase1.getClassName(), methodSignature1)) != null) {
                            ClassFileBase classFileBase3 = (ClassFileBase) objectPair.getFirst();
                            abstractMethodInfo1 = (AbstractMethodInfo) objectPair.getSecond();
                        } else {
                            do {
                                bl = false;
                                ClassFileBase classFileBase2 = (ClassFileBase) this.overriddenMethodOwners.getValue(classFileBase1.getClassName(), methodSignature1);
                                if (classFileBase2 != null) {
                                    classFileBase1 = classFileBase2;
                                    abstractMethodInfo1 = classFileBase1.findMethod(methodSignature1);
                                    bl = true;
                                }
                            } while (bl);
                        }

                        this.rootMethodByMethod.put(abstractMethodInfo, abstractMethodInfo1);
                    }
                }
            }
        }
    }

    public boolean hasOverridingMethods(Object key) {
        if (this.methodsByRoot == null) {
            this.buildMethodsByRoot();
        }

        return this.methodsByRoot.containsKey(key);
    }

    public int reservePrivate(ClassHierarchyNode classHierarchyNode, MemberSignatureBase memberSignatureBase, ObservableHolder observableHolder) throws ZkmException, IOException {
        if (classHierarchyNode.hasNoClassFile()) {
            return 2;
        } else {
            NameReservationMark nameReservationMark = (NameReservationMark) this.nameReservations
                    .putValue(classHierarchyNode, memberSignatureBase, PRIVATE_RESERVATION);
            if (nameReservationMark == null) {
                return 0;
            } else if (nameReservationMark == PRIVATE_RESERVATION) {
                return 1;
            } else if (nameReservationMark == STATIC_RESERVATION) {
                this.nameReservations.putValue(classHierarchyNode, memberSignatureBase, nameReservationMark);
                return 1;
            } else if (nameReservationMark == VIRTUAL_RESERVATION) {
                this.nameReservations.putValue(classHierarchyNode, memberSignatureBase, VIRTUAL_RESERVATION);
                return 1;
            } else {
                this.nameReservations.putValue(classHierarchyNode, memberSignatureBase, nameReservationMark);
                observableHolder.setValue(nameReservationMark.reservedSignature);
                return -1;
            }
        }
    }

    public final boolean tryReserveMethodRename(
            ClassHierarchyNode classHierarchyNode,
            MethodSignature methodSignature1,
            MethodSignature methodSignature2,
            AbstractMethodInfo abstractMethodInfo,
            ObservableHolder observableHolder
    ) throws ZkmException, IOException {
        MemberSignatureBase memberSignatureBase;
        MemberSignatureBase memberSignatureBase1;
        byte bc;
        if (this.useFullDescriptor) {
            memberSignatureBase = methodSignature1;
            memberSignatureBase1 = methodSignature2;
            long bb = 56257057901662L;
            bc = 17;
        } else {
            memberSignatureBase = methodSignature1.getNameTypeSignature();
            memberSignatureBase1 = methodSignature2.getNameTypeSignature();
            long ba = 56257057901662L;
            bc = 17;
        }

        Integer integer = Integer.valueOf(bc);
        HashSet hashSet = ZkmUtils.createHashSet(integer);
        HashSet hashSet1 = ZkmUtils.createHashSet();
        LinkedHashSet linkedHashSet = new LinkedHashSet();
        boolean bl = this.reserveInSubclasses(
                classHierarchyNode, memberSignatureBase, memberSignatureBase1, hashSet1, linkedHashSet, hashSet, abstractMethodInfo, observableHolder
        );
        if (bl) {
            if (abstractMethodInfo.isConstructor()) {
                return true;
            }

            bl = this.reserveInSuperclasses(
                    classHierarchyNode, memberSignatureBase, memberSignatureBase1, hashSet1, linkedHashSet, hashSet, abstractMethodInfo, observableHolder
            );
            if (bl) {
                Iterator iterator = hashSet.iterator();

                while (iterator.hasNext()) {
                    ClassHierarchyNode classHierarchyNode1 = (ClassHierarchyNode) iterator.next();
                    if (!this.tryReserveVirtual(classHierarchyNode1, memberSignatureBase, memberSignatureBase1, hashSet1, linkedHashSet, observableHolder)) {
                        return false;
                    }
                }

                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public void addRelatedInterfaceNodes(Object key, Set set1) {
        Set set2 = this.relatedInterfaceNodes.getValues(key);
        if (set2 != null) {
            Iterator iterator = set2.iterator();

            while (iterator.hasNext()) {
                ClassHierarchyNode classHierarchyNode = (ClassHierarchyNode) iterator.next();
                set1.add(classHierarchyNode);
            }
        }
    }

    public final boolean tryRenameGroup(
            ObservableHolder observableHolder,
            MethodSignature methodSignature1,
            MethodSignature methodSignature2,
            ObservableHolder observableHolder1,
            ChangedMethodDescriptor changedMethodDescriptor
    ) throws ZkmException, IOException {
        List list1 = (List) observableHolder.getValue();
        MemberSignatureBase memberSignatureBase;
        MemberSignatureBase memberSignatureBase1;
        if (this.useFullDescriptor) {
            memberSignatureBase = methodSignature1;
            memberSignatureBase1 = methodSignature2;
        } else {
            memberSignatureBase = methodSignature1.getNameTypeSignature();
            memberSignatureBase1 = methodSignature2.getNameTypeSignature();
        }

        HashSet hashSet = ZkmUtils.createHashSet();
        LinkedHashSet linkedHashSet = new LinkedHashSet();

        for (int elementIndex = 0; elementIndex < list1.size(); elementIndex++) {
            ClassHierarchyNode classHierarchyNode = (ClassHierarchyNode) list1.get(elementIndex);
            switch (this.reserveName(classHierarchyNode, memberSignatureBase, memberSignatureBase1, observableHolder1)) {
                case -1:
                    this.rollbackReservations(hashSet, linkedHashSet, memberSignatureBase);
                    return false;
                case 0:
                    hashSet.add(classHierarchyNode);
                    break;
                case 1:
                case 2:
            }
        }

        if (!this.changesDescriptors) {
            this.setGroupNewName(observableHolder, memberSignatureBase.getName());
        } else {
            this.setGroupChangedDescriptor(observableHolder, changedMethodDescriptor);
        }

        return true;
    }

    public boolean reserveInImplementors(
            ClassHierarchyNode classHierarchyNode,
            MemberSignatureBase memberSignatureBase,
            MemberSignatureBase memberSignatureBase1,
            Set set1,
            Set set2,
            ObservableHolder observableHolder
    ) throws ZkmException, IOException {
        Set set3 = this.getInterfaceImplementors(classHierarchyNode);
        if (set3 != null) {
            Iterator iterator = set3.iterator();

            while (iterator.hasNext()) {
                ClassHierarchyNode classHierarchyNode1 = (ClassHierarchyNode) iterator.next();
                int ba;
                if (this.hasOverriddenMethod(classHierarchyNode1.getClassName(), new MethodSignature(memberSignatureBase1))) {
                    ba = this.reserveName(classHierarchyNode1, memberSignatureBase, memberSignatureBase1, observableHolder);
                } else {
                    ba = this.reserveVirtual(classHierarchyNode1, memberSignatureBase, memberSignatureBase1, set2, observableHolder);
                }

                switch (ba) {
                    case -1:
                        this.rollbackReservations(set1, set2, memberSignatureBase);
                        return false;
                    case 0:
                        set1.add(classHierarchyNode);
                    case 1:
                    case 2:
                }
            }
        }

        return true;
    }

    public final Map getOrBuildVisibleMethodOwners(
            String key, ObservableHolder observableHolder, ScriptEnvironment scriptEnvironment1, Random random1, String string1
    ) throws ZkmException, IOException {
        Map map1 = this.getVisibleMethodOwners(key);
        if (map1 == null) {
            ClassFileBase classFileBase;
            try {
                classFileBase = this.classResolver.getClassFile(key, string1);
            } catch (ZkmClassNotFoundException zkmClassNotFoundException) {
                throw new MissingClassException(
                        "Class '" + ZkmUtils.slashesToDots(zkmClassNotFoundException.getClassName()) + "' not found while " + string1 + ". (1)"
                );
            } catch (ClassFileLoadException classFileLoadException) {
                throw new MissingClassException(classFileLoadException.getMessage());
            }

            map1 = classFileBase.computeInheritedMethods(this, observableHolder, scriptEnvironment1, random1);
        }

        return map1;
    }

    public final ClassFileBase getRootMethodClass(Object key) {
        AbstractMethodInfo abstractMethodInfo = (AbstractMethodInfo) this.rootMethodByMethod.get(key);
        return abstractMethodInfo != null ? abstractMethodInfo.getOwningClass() : null;
    }

    public final Map putOverriddenMethodOwners(String key, Map map1) {
        return this.overriddenMethodOwners.putInnerMap(key, map1);
    }

    public boolean isFullDescriptorMode() {
        return this.useFullDescriptor;
    }

    public final ClassFileBase putOverriddenMethodOwner(Object key, Object keyArgument, Object value) {
        return (ClassFileBase) this.overriddenMethodOwners.putValue(key, keyArgument, value);
    }

    public final boolean tryReserveRename(
            ClassHierarchyNode classHierarchyNode, MethodSignature methodSignature1, MethodSignature methodSignature2, ObservableHolder observableHolder
    ) throws ZkmException, IOException {
        return this.tryReserveGroupRename(classHierarchyNode, methodSignature1, methodSignature2, false, observableHolder);
    }

    public final boolean isSignatureReserved(MethodSignature methodSignature1) {
        MemberSignatureBase memberSignatureBase;
        Set set1;
        if (this.useFullDescriptor) {
            memberSignatureBase = methodSignature1;
            set1 = this.reservedSignatures;
        } else {
            memberSignatureBase = methodSignature1.getNameTypeSignature();
            set1 = this.reservedSignatures;
        }

        return set1.contains(memberSignatureBase);
    }

    public boolean hasOverriddenMethod(String key, MethodSignature methodSignature1) {
        return this.overriddenMethodOwners.containsKeys(key, methodSignature1);
    }

    public final boolean isReservedForOther(Object key, MethodSignature methodSignature1, MethodSignature methodSignature2) {
        MemberSignatureBase memberSignatureBase;
        MemberSignatureBase memberSignatureBase1;
        TwoKeyMap twoKeyMap;
        if (this.useFullDescriptor) {
            memberSignatureBase = methodSignature2;
            memberSignatureBase1 = methodSignature1;
            twoKeyMap = this.nameReservations;
        } else {
            memberSignatureBase = methodSignature2.getNameTypeSignature();
            memberSignatureBase1 = methodSignature1.getNameTypeSignature();
            twoKeyMap = this.nameReservations;
        }

        NameReservationMark nameReservationMark = (NameReservationMark) twoKeyMap.getValue(key, memberSignatureBase1);
        return (!(nameReservationMark == null) && (nameReservationMark.reservedSignature == null || !nameReservationMark.reservedSignature.equals(memberSignatureBase)));
    }

    public final ChangedMethodDescriptor getGroupChangedDescriptor(Object key) {
        return (ChangedMethodDescriptor) ((ObservableHolder) this.groupRenameTargets.get(key)).getValue();
    }

    public int reserveVirtualStrict(
            ClassHierarchyNode classHierarchyNode, MemberSignatureBase memberSignatureBase, Object object, ObservableHolder observableHolder
    ) throws ZkmException, IOException {
        if (classHierarchyNode.hasNoClassFile()) {
            return 2;
        }

        NameReservationMark nameReservationMark = (NameReservationMark) this.nameReservations
                .putValue(classHierarchyNode, memberSignatureBase, VIRTUAL_RESERVATION);
        if (nameReservationMark == null) {
            return 0;
        }

        TwoKeyMap twoKeyMap;
        if (nameReservationMark != PRIVATE_RESERVATION) {
            if (nameReservationMark != STATIC_RESERVATION) {
                if (nameReservationMark == VIRTUAL_RESERVATION) {
                    this.nameReservations.putValue(classHierarchyNode, memberSignatureBase, nameReservationMark);
                    observableHolder.setValue(nameReservationMark.reservedSignature);
                    return -1;
                }

                if (nameReservationMark.reservedSignature != null) {
                    if (nameReservationMark.reservedSignature.equals(object)) {
                        this.nameReservations.putValue(classHierarchyNode, memberSignatureBase, nameReservationMark);
                        return 1;
                    }

                    twoKeyMap = this.nameReservations;
                } else {
                    twoKeyMap = this.nameReservations;
                }

                twoKeyMap.putValue(classHierarchyNode, memberSignatureBase, nameReservationMark);
                observableHolder.setValue(nameReservationMark.reservedSignature);
                return -1;
            }

            twoKeyMap = this.nameReservations;
        } else {
            twoKeyMap = this.nameReservations;
        }

        twoKeyMap.putValue(classHierarchyNode, memberSignatureBase, nameReservationMark);
        observableHolder.setValue(nameReservationMark.reservedSignature);
        return -1;
    }

    public Map getRootMethodMap() {
        return new EnumerableMap(this.rootMethodByMethod);
    }

    public void collectConnectedNodes(EnumerableHashSet enumerableHashSet, ClassHierarchyNode classHierarchyNode, ListMultimap listMultimap) {
        if (enumerableHashSet.add(classHierarchyNode)) {
            List list1 = listMultimap.getValues(classHierarchyNode);
            if (list1 != null) {
                for (int elementIndex = 0; elementIndex < list1.size(); elementIndex++) {
                    ClassHierarchyNode classHierarchyNode1 = (ClassHierarchyNode) list1.get(elementIndex);
                    this.collectConnectedNodes(enumerableHashSet, classHierarchyNode1, listMultimap);
                }
            }
        }
    }

    public void rollbackReservations(Set set1, Set set2, MemberSignatureBase memberSignatureBase) {
        Iterator iterator = set1.iterator();

        while (iterator.hasNext()) {
            ClassHierarchyNode classHierarchyNode = (ClassHierarchyNode) iterator.next();
            this.nameReservations.removeValue(classHierarchyNode, memberSignatureBase);
        }

        ArrayList arrayList = new ArrayList(set2);
        Collections.reverse(arrayList);
        Iterator iterator1 = arrayList.iterator();

        while (iterator1.hasNext()) {
            PendingHierarchyLookup pendingHierarchyLookup = (PendingHierarchyLookup) iterator1.next();
            NameReservationMark nameReservationMark = (NameReservationMark) this.nameReservations
                    .putValue(pendingHierarchyLookup.node, memberSignatureBase, pendingHierarchyLookup.previousMark);
        }

        set1.clear();
        arrayList.clear();
    }

    public void buildMethodsByRoot() {
        AbstractMethodInfo[] abstractMethodInfos = new AbstractMethodInfo[this.rootMethodByMethod.size()];
        this.rootMethodByMethod.keySet().toArray(abstractMethodInfos);
        this.methodsByRoot = new IndexedValueRelation(abstractMethodInfos, this.rootMethodByMethod.values().size());
        Iterator iterator = this.rootMethodByMethod.entrySet().iterator();

        while (iterator.hasNext()) {
            Entry entry = (Entry) iterator.next();
            this.methodsByRoot.addValue(entry.getValue(), entry.getKey());
        }
    }
}

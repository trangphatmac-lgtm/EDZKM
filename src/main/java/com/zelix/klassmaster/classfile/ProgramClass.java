package com.zelix.klassmaster.classfile;

import com.zelix.klassmaster.archive.InputFileLocation;
import com.zelix.klassmaster.changelog.ChangeLogMapping;
import com.zelix.klassmaster.classfile.attribute.AnnotationValueMap;
import com.zelix.klassmaster.classfile.attribute.Attribute;
import com.zelix.klassmaster.classfile.attribute.BootstrapMethodEntry;
import com.zelix.klassmaster.classfile.attribute.BootstrapMethodIndex;
import com.zelix.klassmaster.classfile.attribute.BootstrapMethodsAttribute;
import com.zelix.klassmaster.classfile.attribute.CodeAttributeBody;
import com.zelix.klassmaster.classfile.attribute.ConstantValueAttribute;
import com.zelix.klassmaster.classfile.attribute.DeprecatedAttribute;
import com.zelix.klassmaster.classfile.attribute.EnclosingMethodAttribute;
import com.zelix.klassmaster.classfile.attribute.InnerClassesAttribute;
import com.zelix.klassmaster.classfile.attribute.LineNumberEntry;
import com.zelix.klassmaster.classfile.attribute.NestHostAttribute;
import com.zelix.klassmaster.classfile.attribute.NestMembersAttribute;
import com.zelix.klassmaster.classfile.attribute.PermittedSubclassesAttribute;
import com.zelix.klassmaster.classfile.attribute.RecordAttribute;
import com.zelix.klassmaster.classfile.attribute.ReferencingAttribute;
import com.zelix.klassmaster.classfile.attribute.RuntimeInvisibleAnnotationsAttribute;
import com.zelix.klassmaster.classfile.attribute.SignatureAttribute;
import com.zelix.klassmaster.classfile.attribute.SignatureTypeReferences;
import com.zelix.klassmaster.classfile.attribute.SourceDebugExtensionAttribute;
import com.zelix.klassmaster.classfile.attribute.SourceFileAttribute;
import com.zelix.klassmaster.classfile.attribute.SyntheticAttribute;
import com.zelix.klassmaster.classfile.attribute.TypeAnnotationsAttribute;
import com.zelix.klassmaster.classfile.attribute.UnknownAttribute;
import com.zelix.klassmaster.classfile.constpool.AbstractConstantPool;
import com.zelix.klassmaster.classfile.constpool.ClassConstantBase;
import com.zelix.klassmaster.classfile.constpool.ClassConstantReplaceable;
import com.zelix.klassmaster.classfile.constpool.ConstantInteger;
import com.zelix.klassmaster.classfile.constpool.ConstantLong;
import com.zelix.klassmaster.classfile.constpool.ConstantPool;
import com.zelix.klassmaster.classfile.constpool.ConstantPoolEntry;
import com.zelix.klassmaster.classfile.constpool.ConstantPoolTag;
import com.zelix.klassmaster.classfile.constpool.ConstantUtf8;
import com.zelix.klassmaster.classfile.constpool.MethodHandleRefKind;
import com.zelix.klassmaster.classfile.constpool.NumericConstantEntry;
import com.zelix.klassmaster.classfile.constpool.RemappedConstantPool;
import com.zelix.klassmaster.classfile.constpool.ResolvedClassConstant;
import com.zelix.klassmaster.classfile.constpool.ResolvedFieldRef;
import com.zelix.klassmaster.classfile.constpool.ResolvedInvokeDynamic;
import com.zelix.klassmaster.classfile.constpool.ResolvedMethodHandleConstant;
import com.zelix.klassmaster.classfile.constpool.ResolvedMethodRef;
import com.zelix.klassmaster.classfile.constpool.ResolvedMethodRefConstant;
import com.zelix.klassmaster.classfile.constpool.ResolvedNameAndType;
import com.zelix.klassmaster.classfile.constpool.ResolvedStringConstant;
import com.zelix.klassmaster.classfile.constpool.UsedConstantsCollector;
import com.zelix.klassmaster.classfile.hierarchy.ClassHierarchyNode;
import com.zelix.klassmaster.classfile.hierarchy.ClassHierarchyQuery;
import com.zelix.klassmaster.classfile.hierarchy.ClassMemberLookup;
import com.zelix.klassmaster.classfile.hierarchy.ClassRepository;
import com.zelix.klassmaster.classfile.hierarchy.ClassResolver;
import com.zelix.klassmaster.classfile.hierarchy.ClasspathClassLoader;
import com.zelix.klassmaster.classfile.hierarchy.CommonSuperTypeResolver;
import com.zelix.klassmaster.classfile.hierarchy.InheritedMemberAnalyzer;
import com.zelix.klassmaster.classfile.hierarchy.MethodOverrideAnalyzer;
import com.zelix.klassmaster.classfile.insn.ConstantRefInstruction;
import com.zelix.klassmaster.classfile.insn.ExceptionHandlerSpec;
import com.zelix.klassmaster.classfile.insn.Instruction;
import com.zelix.klassmaster.classfile.insn.IntCounter;
import com.zelix.klassmaster.classfile.insn.LabelInstruction;
import com.zelix.klassmaster.classfile.insn.LocalVariableAllocator;
import com.zelix.klassmaster.classfile.insn.LocalVariableIndex;
import com.zelix.klassmaster.classfile.insn.LocalVariableList;
import com.zelix.klassmaster.classfile.insn.MethodBytecode;
import com.zelix.klassmaster.classfile.insn.SimpleInstruction;
import com.zelix.klassmaster.config.HiddenOptionFlags;
import com.zelix.klassmaster.config.IgnoreMissingReferencesSpec;
import com.zelix.klassmaster.config.ObfuscateOptions;
import com.zelix.klassmaster.config.TrimOptions;
import com.zelix.klassmaster.engine.ProcessingStatistics;
import com.zelix.klassmaster.exceptions.AssertionFailedException;
import com.zelix.klassmaster.exceptions.ClassFileFormatException;
import com.zelix.klassmaster.exceptions.MethodAnalysisException;
import com.zelix.klassmaster.exceptions.StackAnalysisException;
import com.zelix.klassmaster.exceptions.ZkmClassNotFoundException;
import com.zelix.klassmaster.exceptions.ZkmException;
import com.zelix.klassmaster.exceptions.ZkmProcessingException;
import com.zelix.klassmaster.exceptions.ZkmRuntimeException;
import com.zelix.klassmaster.log.MessageReporter;
import com.zelix.klassmaster.obfuscator.constants.EncryptedValueLocation;
import com.zelix.klassmaster.obfuscator.constants.IntegerConstantEncryptor;
import com.zelix.klassmaster.obfuscator.constants.IntegerEncryptionExclusions;
import com.zelix.klassmaster.obfuscator.constants.LongConstantEncryptor;
import com.zelix.klassmaster.obfuscator.constants.LongEncryptionExclusionHandler;
import com.zelix.klassmaster.obfuscator.constants.ObfuscatedReferenceSlot;
import com.zelix.klassmaster.obfuscator.exceptions.ExceptionObfuscationExclusions;
import com.zelix.klassmaster.obfuscator.exceptions.ExceptionObfuscator;
import com.zelix.klassmaster.obfuscator.exclude.AbstractExclusionSpec;
import com.zelix.klassmaster.obfuscator.exclude.NameExclusionSet;
import com.zelix.klassmaster.obfuscator.flow.FlowObfuscationExclusions;
import com.zelix.klassmaster.obfuscator.flow.FlowObfuscationManager;
import com.zelix.klassmaster.obfuscator.flow.OpaquePredicateField;
import com.zelix.klassmaster.obfuscator.flow.StaticInitCalleeAnalyzer;
import com.zelix.klassmaster.obfuscator.parameters.MethodKeyInjector;
import com.zelix.klassmaster.obfuscator.parameters.MethodParamChangeNode;
import com.zelix.klassmaster.obfuscator.parameters.MethodParameterChangeSet;
import com.zelix.klassmaster.obfuscator.parameters.MethodParameterChanger;
import com.zelix.klassmaster.obfuscator.parameters.MethodParameterExclusions;
import com.zelix.klassmaster.obfuscator.parameters.ParameterListGenerator;
import com.zelix.klassmaster.obfuscator.references.ReferenceObfuscator;
import com.zelix.klassmaster.obfuscator.rename.FieldNameAssigner;
import com.zelix.klassmaster.obfuscator.rename.IndexedNameGenerator;
import com.zelix.klassmaster.obfuscator.rename.MethodNameGeneratorBase;
import com.zelix.klassmaster.obfuscator.rename.MethodRenamer;
import com.zelix.klassmaster.obfuscator.string.EncryptedStringLocation;
import com.zelix.klassmaster.obfuscator.string.JsrGotoKind;
import com.zelix.klassmaster.obfuscator.string.StringEncryptionExclusionSpec;
import com.zelix.klassmaster.obfuscator.string.StringEncryptor;
import com.zelix.klassmaster.obfuscator.trim.TrimProcessor;
import com.zelix.klassmaster.script.ScriptEnvironment;
import com.zelix.klassmaster.ui.component.ClassConstantsNode;
import com.zelix.klassmaster.ui.component.ClassNamePropertyNode;
import com.zelix.klassmaster.ui.component.ClassPropertyNode;
import com.zelix.klassmaster.ui.component.MemberListNode;
import com.zelix.klassmaster.util.ArrayEnumeration;
import com.zelix.klassmaster.util.BooleanFlag;
import com.zelix.klassmaster.util.CountingBag;
import com.zelix.klassmaster.util.DisableableMap;
import com.zelix.klassmaster.util.EmptyEnumeration;
import com.zelix.klassmaster.util.EnumerableMap;
import com.zelix.klassmaster.util.IntegerCache;
import com.zelix.klassmaster.util.ListMultimap;
import com.zelix.klassmaster.util.ListenerRegistry;
import com.zelix.klassmaster.util.MultiMapTable;
import com.zelix.klassmaster.util.MutableInt;
import com.zelix.klassmaster.util.NestedMultiMap;
import com.zelix.klassmaster.util.NodeVisitor;
import com.zelix.klassmaster.util.ObjectPair;
import com.zelix.klassmaster.util.ObservableHolder;
import com.zelix.klassmaster.util.ObservableModel;
import com.zelix.klassmaster.util.PairMultiMap;
import com.zelix.klassmaster.util.PairValueMap;
import com.zelix.klassmaster.util.RankedValue;
import com.zelix.klassmaster.util.SetMultiMap;
import com.zelix.klassmaster.util.SetValuedMap;
import com.zelix.klassmaster.util.ThreeKeyMultiMap;
import com.zelix.klassmaster.util.TwoKeyMap;
import com.zelix.klassmaster.util.TwoKeySetMultiMap;
import com.zelix.klassmaster.util.UniqueWorkQueue;
import com.zelix.klassmaster.util.VisitableNode;
import com.zelix.klassmaster.util.WeightedObject;
import com.zelix.klassmaster.util.ZkmUtils;

import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Vector;
import java.util.Map.Entry;

public class ProgramClass extends ClassFileBase implements NodeVisitor, ClassChangeListener, ClassConstantReplaceable {
    public static Vector propertyNodes;
    public AnnotationValueMap zkmAnnotationValues;
    public SetMultiMap inheritedFieldReferrers;
    public SetMultiMap inheritedMethodReferrers;
    public ResolvedMethodRef stringDecryptMethodRef;
    public boolean zkmMethodAnnotationsPresent;
    public boolean lineNumberTablesPresent = false;
    public boolean stackMapsPresent = false;
    public ObservableHolder statusHolder;
    public ListenerRegistry listenerRegistry;
    public ConstantPool constantPool;
    private FieldInfo[] fields;
    private MethodInfo[] methods;

    public void collectFlowObfuscationCandidates(ListMultimap listMultimap) throws ZkmProcessingException {
        if (!this.isInterface()) {
            for (int methodIndex = 0; methodIndex < this.methodCount; methodIndex++) {
                MethodBytecode methodBytecode1 = this.methods[methodIndex].getBytecode();
                if (methodBytecode1 != null) {
                    int shortestPathLength = this.methods[methodIndex].getShortestPathLength();
                    if (shortestPathLength == -1) {
                        float weight = 1.0F - (float) (1.0 / methodBytecode1.getCodeLength());
                        WeightedObject weightedObject = new WeightedObject(weight, methodBytecode1);
                        listMultimap.addValue(this, weightedObject);
                    } else if (shortestPathLength < Integer.MAX_VALUE) {
                        WeightedObject weightedObject1 = new WeightedObject(shortestPathLength, methodBytecode1);
                        listMultimap.addValue(this, weightedObject1);
                    }
                }
            }
        }
    }

    public ClassPropertyNode getPropertyNode(int elementIndex) {
        return (ClassPropertyNode) propertyNodes.elementAt(elementIndex);
    }

    public FieldInfo createUniquelyNamedStaticField(
            String descriptor, int ba, boolean bl, InheritedMemberAnalyzer inheritedMemberAnalyzer, ClassMemberLookup classMemberLookup1, int bb
    ) throws ZkmException, IOException {
        synchronized (inheritedMemberAnalyzer.getFieldCreationLock()) {
            String string1 = this.generateUniqueFieldName(classMemberLookup1.getNewFieldNames(), descriptor, inheritedMemberAnalyzer);
            Integer integer = bb;
            return this.createStaticField(string1, descriptor, ba, bl, inheritedMemberAnalyzer, classMemberLookup1, integer);
        }
    }

    public void encryptStrings(
            InheritedMemberAnalyzer inheritedMemberAnalyzer,
            StringEncryptor stringEncryptor,
            boolean bl,
            boolean bl1,
            boolean bl2,
            boolean bl3,
            boolean bl4,
            ClassMemberLookup classMemberLookup1,
            ClassResolver classResolver1,
            List list1,
            List list2,
            PairMultiMap pairMultiMap,
            StringEncryptionExclusionSpec stringEncryptionExclusionSpec,
            Set set1,
            MethodParameterChanger methodParameterChanger,
            Map map1,
            Map map2,
            List list3,
            CommonSuperTypeResolver commonSuperTypeResolver1,
            ClassHierarchyQuery classHierarchyQuery
    ) throws ZkmException, IOException {
        List list5 = list2;
        PairMultiMap pairMultiMap1 = pairMultiMap;
        List list4 = list1;
        boolean bl10 = this.isInterface();
        if (!bl10 || this.supportsJava8() || bl) {
            if (stringEncryptionExclusionSpec == null || !stringEncryptionExclusionSpec.isClassExcluded(this)) {
                if (list4 == null) {
                    list4 = new ArrayList(13);
                }

                if (list5 == null) {
                    list5 = new ArrayList(0);
                }

                if (pairMultiMap1 == null) {
                    pairMultiMap1 = new PairMultiMap(13);
                }

                if (list4.size() != 0 || pairMultiMap1.getKeyCount() != 0) {
                    int maxValue = Integer.MAX_VALUE;
                    int maxValueValue = Integer.MAX_VALUE;
                    HashSet hashSet = ZkmUtils.createHashSet();
                    HashSet hashSet1 = ZkmUtils.createHashSet();
                    SetMultiMap setMultiMap = new SetMultiMap();
                    HashMap hashMap = ZkmUtils.createHashMap();
                    MethodInfo methodInfo1 = this.groupStringConstants(pairMultiMap1, hashSet, hashSet1, list4, setMultiMap, hashMap, bl1);
                    if (methodInfo1 == null) {
                        if (!this.isVersionedVariant()) {
                            methodInfo1 = classMemberLookup1.findDeclaredMethod(this, MethodSignature.STATIC_INITIALIZER);
                        } else {
                            methodInfo1 = this.findMethodBySignature(MethodSignature.STATIC_INITIALIZER);
                        }
                    }

                    HashSet hashSet2 = ZkmUtils.createHashSet();
                    ArrayList arrayList = new ArrayList(list4.size());
                    Iterator iterator = list4.iterator();

                    while (iterator.hasNext()) {
                        ObjectPair objectPair = (ObjectPair) iterator.next();
                        arrayList.add(objectPair.getFirst());
                    }

                    hashSet2.addAll(arrayList);
                    hashSet2.addAll(hashSet);
                    hashSet2.addAll(hashSet1);
                    HashSet hashSet4 = ZkmUtils.createHashSetFrom(hashSet1);
                    hashSet4.addAll(arrayList);
                    int bd = methodInfo1 == null ? 0 : methodInfo1.getLocalVariableCount();
                    hashSet1.size();
                    int hashSet4Size = hashSet4.size();
                    boolean boolean1Value = false;
                    JsrGotoKind jsrGotoKind;
                    if (bl10) {
                        if (!bl && hashSet.size() == 0) {
                            return;
                        }

                        if (bl2) {
                            jsrGotoKind = JsrGotoKind.GOTO;
                        } else {
                            jsrGotoKind = hashSet4Size <= 5000 && bd <= 50 ? JsrGotoKind.JSR : JsrGotoKind.GOTO;
                        }
                    } else {
                        if (!bl) {
                            boolean1Value = true;
                        }

                        if (bl2) {
                            jsrGotoKind = JsrGotoKind.GOTO;
                        } else if (hashSet4Size <= 5000 && bd <= 50) {
                            jsrGotoKind = JsrGotoKind.JSR;
                        } else {
                            jsrGotoKind = JsrGotoKind.GOTO;
                        }
                    }

                    ArrayList arrayList1 = new ArrayList();
                    HashMap hashMap1 = ZkmUtils.createHashMap();
                    this.prepareStringConstantFields(list4, hashMap, hashMap1, list5, arrayList1);
                    if (methodInfo1 == null) {
                        Integer integer = 1;
                        methodInfo1 = this.getOrCreateStaticInitializer(arrayList1, classMemberLookup1, "String Encryption", integer);
                    }

                    LocalVariableAllocator localVariableAllocator = new LocalVariableAllocator(methodInfo1);
                    LocalVariableList localVariableList1 = methodInfo1.getLocalVariableList();
                    ResolvedFieldRef resolvedFieldRef1 = null;
                    boolean bl12 = setMultiMap.getKeyCount() > 1;
                    boolean bl13 = hashSet.size() > 0;
                    ObservableHolder[] observableHolders = null;
                    if (bl12) {
                        observableHolders = new ObservableHolder[setMultiMap.getKeyCount()];
                    }

                    if (setMultiMap.getKeyCount() == 1) {
                        HashSet hashSet3 = ZkmUtils.createHashSet();
                        Iterator iterator1 = list4.iterator();

                        while (iterator1.hasNext()) {
                            ObjectPair objectPair1 = (ObjectPair) iterator1.next();
                            hashSet3.add(objectPair1.getSecond());
                        }

                        if (hashSet3.size() == 1) {
                            FieldInfo fieldInfo = (FieldInfo) hashSet3.iterator().next();
                            resolvedFieldRef1 = this.constantPool
                                    .getOrAddFieldRef(
                                            this.getClassName(), fieldInfo.getSourceName(), fieldInfo.getDescriptor(), arrayList1, classMemberLookup1, classResolver1, true
                                    );
                            bl13 = true;
                        }
                    }

                    if (bl13 && resolvedFieldRef1 == null) {
                        String descriptor;
                        if (setMultiMap.getKeyCount() == 1) {
                            descriptor = "Ljava/lang/String;";
                        } else {
                            descriptor = "[Ljava/lang/String;";
                        }

                        FieldInfo fieldInfo1 = this.createUniquelyNamedStaticField(descriptor, bl10 ? 4 : 1, true, inheritedMemberAnalyzer, classMemberLookup1, 1);
                        resolvedFieldRef1 = this.constantPool
                                .getOrAddFieldRef(
                                        this.getClassName(), fieldInfo1.getSourceName(), fieldInfo1.getDescriptor(), arrayList1, classMemberLookup1, classResolver1, true
                                );
                    }

                    if (bl1 && bl13 && bl12 && observableHolders.length > 65535) {
                        throw new ZkmProcessingException(
                                "String Encryption 'enhanced' setting can handle a maximumum of 65535 Strings. Number of Strings in class '"
                                        + this.getLocationName()
                                        + "' is "
                                        + observableHolders.length
                                        + "."
                        );
                    }

                    boolean bl15 = bl1 && resolvedFieldRef1 != null && bl12;
                    boolean bl16 = methodParameterChanger != null && methodParameterChanger.canKeyStringEncryption(methodInfo1) && !boolean1Value;
                    boolean bl17 = bl15 && bl16 && StringEncryptor.canUseIndyStrings(this);
                    boolean bl14 = bl4 && !bl16;
                    boolean bl18 = methodParameterChanger != null;
                    boolean bl5 = bl16;
                    boolean bl6 = bl17;
                    boolean bl7 = bl18;
                    ResolvedFieldRef resolvedFieldRef = resolvedFieldRef1;
                    boolean bl8 = bl14;
                    boolean bl9 = bl15;
                    Boolean boolean6 = bl5;
                    Boolean boolean5 = bl6;
                    Boolean boolean4 = bl7;
                    ResolvedFieldRef resolvedFieldRef2 = resolvedFieldRef;
                    Boolean boolean3 = bl8;
                    Boolean boolean2 = bl9;
                    Boolean boolean1 = bl3;
                    stringEncryptor.setUpDecryptionMembers(
                            this,
                            inheritedMemberAnalyzer,
                            classMemberLookup1,
                            classResolver1,
                            arrayList1,
                            boolean1Value,
                            boolean1,
                            boolean2,
                            boolean3,
                            resolvedFieldRef2,
                            boolean4,
                            boolean5,
                            boolean6
                    );
                    if (list3 != null) {
                        list3.add(this);
                    }

                    PairMultiMap pairMultiMap2 = new PairMultiMap();
                    HashMap hashMap2 = ZkmUtils.createHashMap();
                    this.assignStringValueLocations(
                            pairMultiMap2,
                            hashMap2,
                            pairMultiMap1,
                            setMultiMap,
                            hashMap,
                            resolvedFieldRef1,
                            bl12,
                            bl16,
                            stringEncryptor.getStringIndyEntry(),
                            stringEncryptor.getLookupMethodRef(),
                            observableHolders,
                            localVariableAllocator
                    );
                    int andIncrementValue = -1;
                    if (!bl13) {
                        andIncrementValue = localVariableAllocator.getAndIncrement();
                    }

                    HashMap hashMap3 = ZkmUtils.createHashMap();
                    ListMultimap listMultimap = new ListMultimap();
                    ObservableHolder observableHolder = new ObservableHolder();
                    ObservableHolder observableHolder1 = new ObservableHolder();
                    LabelInstruction labelInstruction = this.createDecryptSubroutineLabel(observableHolder1, observableHolder, jsrGotoKind, hashMap3);
                    int value = localVariableAllocator.getValue();
                    MutableInt mutableInt = new MutableInt(-2);
                    ObservableHolder observableHolder2 = new ObservableHolder();
                    Long long1 = null;
                    LocalVariableIndex localVariableIndex1 = null;
                    if (methodParameterChanger != null && (bl15 && hashSet4Size > 0 || bl16)) {
                        long1 = map1 != null ? (Long) map1.get(methodInfo1) : null;
                        localVariableIndex1 = map2 != null ? (LocalVariableIndex) map2.get(methodInfo1) : null;
                        if (long1 == null) {
                            long1 = methodParameterChanger.assignMethodKey(methodInfo1);
                        }
                    }

                    ObservableHolder observableHolder3 = new ObservableHolder();
                    ArrayList arrayList2 = new ArrayList();
                    if (stringEncryptor.getCacheMapFieldRef() != null) {
                        stringEncryptor.emitCacheMapInit(arrayList2, arrayList1, this.constantPool, classMemberLookup1, classResolver1);
                    }

                    if (bl12) {
                        this.emitMultiStringDecryption(
                                localVariableList1,
                                arrayList2,
                                resolvedFieldRef1,
                                stringEncryptor.getStringArrayFieldRef(),
                                andIncrementValue,
                                observableHolders,
                                setMultiMap,
                                hashMap2,
                                hashMap1,
                                jsrGotoKind,
                                observableHolder1,
                                mutableInt,
                                listMultimap,
                                localVariableAllocator,
                                arrayList1,
                                stringEncryptor,
                                methodInfo1,
                                bl1,
                                bl14,
                                observableHolder3,
                                long1,
                                localVariableIndex1,
                                map2,
                                bl16,
                                set1,
                                methodParameterChanger,
                                classMemberLookup1,
                                classResolver1
                        );
                    } else {
                        ObservableHolder observableHolder4 = (ObservableHolder) setMultiMap.keys().nextElement();
                        this.emitSingleStringDecryption(
                                localVariableList1,
                                arrayList2,
                                resolvedFieldRef1,
                                (ResolvedStringConstant) setMultiMap.distinctValues().nextElement(),
                                jsrGotoKind,
                                (LabelInstruction) observableHolder1.getValue(),
                                listMultimap,
                                mutableInt,
                                hashMap1,
                                (EncryptedStringLocation) hashMap2.get(observableHolder4),
                                bl14,
                                observableHolder2,
                                stringEncryptor,
                                methodInfo1,
                                localVariableAllocator,
                                observableHolder3,
                                long1,
                                localVariableIndex1,
                                map2,
                                bl16,
                                set1,
                                methodParameterChanger,
                                arrayList1,
                                classMemberLookup1,
                                classResolver1
                        );
                    }

                    Enumeration enumeration = pairMultiMap2.keys();

                    while (enumeration.hasMoreElements()) {
                        MethodInfo methodInfo2 = (MethodInfo) enumeration.nextElement();
                        List list6 = pairMultiMap2.getPairs(methodInfo2);
                        IntCounter intCounter;
                        if (methodInfo2.isStaticInitializer()) {
                            if (localVariableAllocator.getValue() > value) {
                                intCounter = new MutableInt(value);
                            } else {
                                intCounter = localVariableAllocator;
                            }
                        } else {
                            intCounter = new MutableInt(methodInfo2.getLocalVariableCount());
                        }

                        Long long2 = map1 != null ? (Long) map1.get(methodInfo2) : null;
                        LocalVariableIndex localVariableIndex2 = map2 != null ? (LocalVariableIndex) map2.get(methodInfo2) : null;
                        Long long3 = bl1 ? long2 : null;
                        LocalVariableIndex localVariableIndex4 = bl1 ? localVariableIndex2 : null;
                        ClassHierarchyQuery classHierarchyQuery2 = classHierarchyQuery;
                        CommonSuperTypeResolver commonSuperTypeResolver3 = commonSuperTypeResolver1;
                        ArrayList arrayList4 = arrayList1;
                        ConstantPool constantPool2 = this.constantPool;
                        IntCounter intCounter1 = intCounter;
                        LocalVariableIndex localVariableIndex3 = localVariableIndex4;
                        ClassHierarchyQuery classHierarchyQuery1 = classHierarchyQuery2;
                        CommonSuperTypeResolver commonSuperTypeResolver2 = commonSuperTypeResolver3;
                        ArrayList arrayList3 = arrayList4;
                        ConstantPool constantPool1 = constantPool2;
                        methodInfo2.applyStringEncryption(
                                list6,
                                hashMap3,
                                listMultimap,
                                long3,
                                localVariableIndex3,
                                intCounter1,
                                constantPool1,
                                arrayList3,
                                commonSuperTypeResolver2,
                                classHierarchyQuery1
                        );
                    }

                    Integer integer1 = (Integer) observableHolder3.getValue();
                    ClassResolver classResolver2 = classResolver1;
                    ClassMemberLookup classMemberLookup2 = classMemberLookup1;
                    stringEncryptor.emitDecryptSubroutine(
                            localVariableList1,
                            labelInstruction,
                            localVariableAllocator,
                            arrayList2,
                            arrayList1,
                            jsrGotoKind,
                            listMultimap,
                            integer1,
                            bl14,
                            classMemberLookup2,
                            classResolver2
                    );
                    int be = localVariableAllocator.getValue() - bd;
                    ArrayList arrayList5 = new ArrayList();
                    int maxValueValue2 = bd;
                    maxValue = maxValueValue2;

                    for (int valueValue = localVariableAllocator.getValue(); maxValue < valueValue; valueValue = localVariableAllocator.getValue()) {
                        arrayList5.add(maxValueValue2);
                        maxValueValue2 += 1;
                        maxValue = maxValueValue2;
                    }

                    LabelInstruction labelInstruction1 = new LabelInstruction(4096);
                    arrayList2.add(labelInstruction1);
                    methodInfo1.setInitializerEndLabel(labelInstruction1);
                    methodInfo1.insertInstructions(arrayList2, be, methodInfo1.getMaxStack(), 0, "String Encryption", arrayList5);
                    if (arrayList1.size() > 0) {
                        this.constantPool.appendEntries(arrayList1);
                        this.setChanged();
                    } else {
                        this.setChanged();
                    }

                    MutableInt mutableInt2 = new MutableInt(4);
                    Object object = null;
                    ProgramClass programClass2 = this;
                    MutableInt mutableInt1 = mutableInt2;
                    this.notifyObservers(mutableInt1, programClass2, object);
                }
            }
        }
    }

    public void trimAttributes(TrimProcessor trimProcessor1, TrimOptions trimOptions1, ScriptEnvironment scriptEnvironment1, PrintWriter printWriter) throws ZkmException, IOException {
        PrintWriter printWriter1 = scriptEnvironment1.getLogWriter();
        if (trimOptions1.d && this.isAnnotation()) {
            if (trimProcessor1.hasAnnotationRetainedClasses() && trimProcessor1.isAnnotationRetained(this)) {
                String string = "Retaining 'annotation' modifier on class "
                        + ClassFileBase.toDottedName(this.getClassName())
                        + " because of annotation attribute exclusion.";
                if (scriptEnvironment1.isVerbose()) {
                    printWriter1.print('\t');
                    printWriter1.println(string);
                }

                printWriter.println(string);
            } else {
                this.setAnnotationFlag();
            }
        }

        ArrayList arrayList = new ArrayList();

        for (int attributeIndex = 0; attributeIndex < this.attributeCount; attributeIndex++) {
            if (this.attributes[attributeIndex] instanceof SourceFileAttribute) {
                if (!trimOptions1.a) {
                    arrayList.add(this.attributes[attributeIndex]);
                }
            } else if (this.attributes[attributeIndex] instanceof DeprecatedAttribute) {
                if (!trimOptions1.b) {
                    arrayList.add(this.attributes[attributeIndex]);
                }
            } else if (this.attributes[attributeIndex] instanceof ReferencingAttribute) {
                ReferencingAttribute referencingAttribute = (ReferencingAttribute) this.attributes[attributeIndex];
                if (trimOptions1.d) {
                    if (trimProcessor1.hasAnnotationRetainedClasses() && !referencingAttribute.trimAnnotations(trimProcessor1, scriptEnvironment1, printWriter)) {
                        arrayList.add(this.attributes[attributeIndex]);
                    }
                } else {
                    arrayList.add(this.attributes[attributeIndex]);
                }
            } else if (this.attributes[attributeIndex] instanceof RecordAttribute) {
                if (trimOptions1.d) {
                    RecordAttribute recordAttribute = (RecordAttribute) this.attributes[attributeIndex];
                    recordAttribute.trimComponentAttributes(trimProcessor1, scriptEnvironment1, printWriter);
                }
            } else if (this.attributes[attributeIndex] instanceof SourceDebugExtensionAttribute) {
                if (!trimOptions1.f) {
                    arrayList.add(this.attributes[attributeIndex]);
                }
            } else if (this.attributes[attributeIndex] instanceof UnknownAttribute) {
                if (trimOptions1.c) {
                    UnknownAttribute unknownAttribute = (UnknownAttribute) this.attributes[attributeIndex];
                    if (scriptEnvironment1.isVerbose()) {
                        printWriter1.println(
                                "\tDeleting unknown attribute '" + unknownAttribute.getAttributeName() + "' in class " + ClassFileBase.toDottedName(this.getClassName())
                        );
                    }

                    printWriter.println(
                            "Deleting unknown attribute '" + unknownAttribute.getAttributeName() + "' in class " + ClassFileBase.toDottedName(this.getClassName())
                    );
                } else {
                    arrayList.add(this.attributes[attributeIndex]);
                }
            } else {
                arrayList.add(this.attributes[attributeIndex]);
            }
        }

        if (arrayList.size() < this.attributeCount) {
            this.attributes = ((com.zelix.klassmaster.classfile.attribute.Attribute[]) (arrayList.toArray(new Attribute[arrayList.size()])));
            this.attributeCount = this.attributes.length;
        }

        for (int methodIndex = 0; methodIndex < this.methodCount; methodIndex++) {
            this.methods[methodIndex].trimAttributes(trimProcessor1, trimOptions1, scriptEnvironment1, printWriter);
        }

        for (int fieldIndex = 0; fieldIndex < this.fieldCount; fieldIndex++) {
            this.fields[fieldIndex].trimAttributes(trimProcessor1, trimOptions1, scriptEnvironment1, printWriter);
        }
    }

    public FieldInfo createStaticField(
            String string, String string1, int ba, boolean bl, InheritedMemberAnalyzer inheritedMemberAnalyzer, ClassMemberLookup classMemberLookup1, Integer integer
    ) throws ZkmException, IOException {
        if (this.findFieldBySignature(new FieldSignature(string, string1)) != null) {
            return null;
        }

        ArrayList arrayList = new ArrayList(2);
        ConstantUtf8 constantUtf8 = new ConstantUtf8(0, this.constantPool, string);
        arrayList.add(constantUtf8);
        ConstantUtf8 constantUtf81 = new ConstantUtf8(0, this.constantPool, string1);
        arrayList.add(constantUtf81);
        Attribute[] attributes1;
        if (HiddenOptionFlags.MARK_ADDED_MEMBERS_SYNTHETIC) {
            ConstantUtf8 constantUtf82 = new ConstantUtf8(0, this.constantPool, "Synthetic");
            arrayList.add(constantUtf82);
            SyntheticAttribute syntheticAttribute = new SyntheticAttribute(constantUtf82);
            attributes1 = new Attribute[]{syntheticAttribute};
        } else {
            attributes1 = new Attribute[0];
        }

        FieldInfo fieldInfo = new FieldInfo(this, constantUtf8, constantUtf81, attributes1, integer);
        fieldInfo.setStatic();
        if (bl) {
            fieldInfo.setFinal();
        }

        switch (ba) {
            case 1:
                fieldInfo.makePrivate();
                break;
            case 2:
                fieldInfo.makePackagePrivate();
            case 3:
            default:
                break;
            case 4:
                fieldInfo.makePublic();
        }

        this.addField(fieldInfo, classMemberLookup1);
        inheritedMemberAnalyzer.addField(fieldInfo);
        this.constantPool.appendEntries(arrayList);
        this.setChanged();
        this.notifyObservers(new MutableInt(4), this, null);
        return fieldInfo;
    }

    public int addPoolConstants(List list1) {
        return this.constantPool.appendEntries(list1);
    }

    public void registerMethods(ClassMemberLookup classMemberLookup1) {
        for (int methodIndex = 0; methodIndex < this.methodCount; methodIndex++) {
            classMemberLookup1.registerMethod(this.methods[methodIndex], this);
        }
    }

    public void emitMultiLongDecryption(
            LocalVariableList localVariableList1,
            List list1,
            ResolvedFieldRef resolvedFieldRef,
            int ba,
            ObservableHolder[] observableHolders,
            SetMultiMap setMultiMap,
            Map map1,
            Map map2,
            ObservableHolder observableHolder,
            MutableInt mutableInt,
            ListMultimap listMultimap,
            LocalVariableAllocator localVariableAllocator,
            List list2,
            LongConstantEncryptor longConstantEncryptor,
            MethodInfo methodInfo1,
            ObservableHolder observableHolder1,
            Long long1,
            LocalVariableIndex localVariableIndex1,
            Map map3,
            ObservableHolder observableHolder2,
            boolean bl,
            MethodParameterChanger methodParameterChanger,
            ClassMemberLookup classMemberLookup1,
            ClassResolver classResolver1
    ) throws ZkmException, IOException {
        LocalVariableIndex localVariableIndex2 = localVariableIndex1;
        if (long1 != null) {
            if (localVariableIndex2 == null) {
                localVariableIndex2 = methodParameterChanger.emitKeyLocalInit(
                        long1, localVariableAllocator, list1, methodInfo1, map3, this.getClassConstantPool(), list2
                );
            }

            if (bl) {
                int andIncrement = localVariableAllocator.getAndIncrement();
                longConstantEncryptor.emitDesCipherInit(
                        localVariableList1, list1, andIncrement, localVariableIndex2, localVariableAllocator, list2, classMemberLookup1, classResolver1
                );
                observableHolder1.setValue(integerCache.valueOf(andIncrement));
            }
        } else if (longConstantEncryptor.hasClassXorKey()) {
            Instruction.appendLongConstant(longConstantEncryptor.getClassXorKey(), list1, this.constantPool, list2);
            int value = localVariableAllocator.getValue();
            localVariableAllocator.addAndGet(2);
            list1.add(Instruction.createLongStore(value, localVariableList1, 12));
            observableHolder2.setValue(integerCache.valueOf(value));
        }

        HashSet hashSet = ZkmUtils.createHashSet();
        longConstantEncryptor.emitEncryptedStringTable(
                localVariableList1,
                list1,
                localVariableAllocator,
                hashSet,
                list2,
                resolvedFieldRef,
                ba,
                observableHolders,
                setMultiMap,
                map1,
                (LabelInstruction) observableHolder.getValue(),
                mutableInt,
                listMultimap,
                long1,
                bl,
                classMemberLookup1,
                classResolver1
        );
        this.constantPool.removeEntries(hashSet);
        if (!map2.isEmpty()) {
            ArrayList arrayList = new ArrayList();
            if (resolvedFieldRef != null) {
                Iterator iterator1 = map2.entrySet().iterator();

                while (iterator1.hasNext()) {
                    Entry entry1 = (Entry) iterator1.next();
                    ResolvedFieldRef resolvedFieldRef2 = (ResolvedFieldRef) entry1.getKey();
                    ObservableHolder observableHolder4 = (ObservableHolder) entry1.getValue();
                    ObfuscatedReferenceSlot obfuscatedReferenceSlot1 = (ObfuscatedReferenceSlot) map1.get(observableHolder4);
                    longConstantEncryptor.emitDecryptAndStore(
                            localVariableList1, arrayList, resolvedFieldRef2, obfuscatedReferenceSlot1, long1, localVariableIndex2, list2
                    );
                }
            } else {
                arrayList.add(Instruction.createObjectLoad(ba, localVariableList1, 12));
                Iterator iterator = map2.entrySet().iterator();

                while (iterator.hasNext()) {
                    Entry entry = (Entry) iterator.next();
                    ResolvedFieldRef resolvedFieldRef1 = (ResolvedFieldRef) entry.getKey();
                    ObservableHolder observableHolder3 = (ObservableHolder) entry.getValue();
                    ObfuscatedReferenceSlot obfuscatedReferenceSlot = (ObfuscatedReferenceSlot) map1.get(observableHolder3);
                    longConstantEncryptor.emitDecryptAndStore(
                            localVariableList1, arrayList, resolvedFieldRef1, obfuscatedReferenceSlot, long1, localVariableIndex2, list2
                    );
                }

                arrayList.add(SimpleInstruction.forOpcode(87));
            }

            list1.addAll(arrayList);
        }
    }

    @Override
    public AbstractFieldInfo findField(Object object) {
        FieldSignature fieldSignature = (FieldSignature) object;
        return this.findFieldBySignature(fieldSignature);
    }

    public MethodInfo createUniquelyNamedStaticMethod(
            String valueArgument,
            ArrayList arrayList,
            int maxStack,
            int maxLocals,
            int bc,
            LocalVariableList localVariableList1,
            ExceptionHandlerSpec[] exceptionHandlerSpecs,
            String string1,
            List list1,
            InheritedMemberAnalyzer inheritedMemberAnalyzer,
            ClassMemberLookup classMemberLookup1,
            int creationKind
    ) throws ZkmProcessingException {
        synchronized (inheritedMemberAnalyzer.getMethodCreationLock()) {
            String valueValue = this.generateUniqueMethodName(classMemberLookup1.getNewMethodNames(), valueArgument, inheritedMemberAnalyzer, true);
            return this.createStaticMethod(
                    valueValue, valueArgument, arrayList, maxStack, maxLocals, bc, localVariableList1, exceptionHandlerSpecs, string1, list1, inheritedMemberAnalyzer, classMemberLookup1, creationKind
            );
        }
    }

    public boolean regenerateStackMap(
            MethodInfo methodInfo1,
            CommonSuperTypeResolver commonSuperTypeResolver1,
            ClassHierarchyQuery classHierarchyQuery,
            ConstantPool constantPool1,
            Map map1,
            boolean bl,
            boolean bl1
    ) throws ZkmException, IOException {
        boolean bl2 = bl;
        if (bl2 & !bl1) {
            bl2 = this.supportsJava6();
        }

        ArrayList arrayList = new ArrayList();

        boolean bl3;
        try {
            bl3 = methodInfo1.getCodeAttribute()
                    .regenerateStackMapIfNeeded(commonSuperTypeResolver1, classHierarchyQuery, constantPool1, arrayList, map1, bl2, bl1);
        } catch (StackAnalysisException stackAnalysisException) {
            throw new ZkmProcessingException(this.getDisplayLocationName() + " " + methodInfo1.toDisplayString() + " : " + stackAnalysisException.getMessage());
        }

        if (!arrayList.isEmpty()) {
            constantPool1.appendEntries(arrayList);
        }

        return bl3 && (bl2 || bl1) || !arrayList.isEmpty();
    }

    public void emitSingleLongDecryption(
            LocalVariableList localVariableList1,
            List list1,
            ResolvedFieldRef resolvedFieldRef,
            long ba,
            LabelInstruction labelInstruction,
            ListMultimap listMultimap,
            MutableInt mutableInt,
            Map map1,
            ObfuscatedReferenceSlot obfuscatedReferenceSlot,
            LongConstantEncryptor longConstantEncryptor,
            MethodInfo methodInfo1,
            LocalVariableAllocator localVariableAllocator,
            ObservableHolder observableHolder,
            Long long1,
            LocalVariableIndex localVariableIndex1,
            Map map2,
            ObservableHolder observableHolder1,
            boolean bl,
            MethodParameterChanger methodParameterChanger,
            List list2,
            ClassMemberLookup classMemberLookup1,
            ClassResolver classResolver1
    ) throws ZkmException, IOException {
        LocalVariableIndex localVariableIndex2 = localVariableIndex1;
        if (long1 != null) {
            if (localVariableIndex2 == null) {
                localVariableIndex2 = methodParameterChanger.emitKeyLocalInit(
                        long1, localVariableAllocator, list1, methodInfo1, map2, this.getClassConstantPool(), list2
                );
            }

            if (bl) {
                int andIncrement = localVariableAllocator.getAndIncrement();
                longConstantEncryptor.emitDesCipherInit(
                        localVariableList1, list1, andIncrement, localVariableIndex2, localVariableAllocator, list2, classMemberLookup1, classResolver1
                );
                observableHolder.setValue(integerCache.valueOf(andIncrement));
            }
        } else if (longConstantEncryptor.hasClassXorKey()) {
            Instruction.appendLongConstant(longConstantEncryptor.getClassXorKey(), list1, this.constantPool, list2);
            int value = localVariableAllocator.getValue();
            localVariableAllocator.addAndGet(2);
            list1.add(Instruction.createLongStore(value, localVariableList1, 12));
            observableHolder1.setValue(integerCache.valueOf(value));
        }

        ConstantLong constantLong = this.constantPool.getOrAddLongConstant(-1L, list2);
        long valueValue;
        if (bl) {
            valueValue = longConstantEncryptor.desEncrypt(ba, long1);
        } else if (long1 != null) {
            valueValue = ba ^ long1;
        } else {
            valueValue = ba ^ longConstantEncryptor.getClassXorKey();
        }

        constantLong.setValue(valueValue);
        longConstantEncryptor.emitEncryptedConstantLoad(
                localVariableList1, list1, resolvedFieldRef, constantLong, obfuscatedReferenceSlot, labelInstruction, mutableInt, listMultimap
        );
        if (!map1.isEmpty()) {
            ArrayList arrayList = new ArrayList();
            if (resolvedFieldRef != null) {
                arrayList.add(new ConstantRefInstruction(178, resolvedFieldRef));
            } else if (obfuscatedReferenceSlot.hasIndex()) {
                arrayList.add(Instruction.createObjectLoad(obfuscatedReferenceSlot.getLocalVariableIndex(), localVariableList1, 12));
                arrayList.add(Instruction.createIntLoad(obfuscatedReferenceSlot.getIndex(), localVariableList1, 12));
                arrayList.add(SimpleInstruction.forOpcode(47));
            } else {
                arrayList.add(Instruction.createLongLoad(obfuscatedReferenceSlot.getLocalVariableIndex(), localVariableList1, 12));
            }

            Iterator iterator = map1.entrySet().iterator();

            while (iterator.hasNext()) {
                ResolvedFieldRef resolvedFieldRef1 = (ResolvedFieldRef) ((Entry) iterator.next()).getKey();
                if (resolvedFieldRef == null) {
                    longConstantEncryptor.emitDecryptAndStore(
                            localVariableList1, arrayList, resolvedFieldRef1, obfuscatedReferenceSlot, long1, localVariableIndex2, list2
                    );
                }
            }

            arrayList.add(SimpleInstruction.forOpcode(88));
            list1.addAll(arrayList);
        }
    }

    public void resolveReferences(
            Set set1,
            ClassMemberLookup classMemberLookup1,
            ClassResolver classResolver1,
            IgnoreMissingReferencesSpec ignoreMissingReferencesSpec1,
            ScriptEnvironment scriptEnvironment1
    ) throws ZkmException, IOException {
        this.constantPool
                .resolveMemberRefTargets(
                        set1, classMemberLookup1, classResolver1, scriptEnvironment1 == null ? null : scriptEnvironment1.getIgnoreMissingReferencesSpec()
                );

        for (int attributeIndex = 0; attributeIndex < this.attributes.length; attributeIndex++) {
            if (this.attributes[attributeIndex] instanceof ReferencingAttribute) {
                ((ReferencingAttribute) this.attributes[attributeIndex]).resolveReferences(classResolver1, ignoreMissingReferencesSpec1, scriptEnvironment1);
            } else if (this.attributes[attributeIndex] instanceof EnclosingMethodAttribute) {
                ((EnclosingMethodAttribute) this.attributes[attributeIndex]).resolveEnclosingMethod(classResolver1);
            } else if (this.attributes[attributeIndex] instanceof RecordAttribute) {
                ((RecordAttribute) this.attributes[attributeIndex]).resolveReferences(classResolver1, ignoreMissingReferencesSpec1, scriptEnvironment1);
            }
        }

        for (int fieldIndex = 0; fieldIndex < this.fieldCount; fieldIndex++) {
            this.fields[fieldIndex].resolveAttributeReferences(classResolver1, ignoreMissingReferencesSpec1, scriptEnvironment1);
        }

        for (int methodIndex = 0; methodIndex < this.methodCount; methodIndex++) {
            this.methods[methodIndex].resolveAttributeReferences(classResolver1, ignoreMissingReferencesSpec1, scriptEnvironment1);
        }
    }

    public boolean applyAutoReflectionHandling(
            ListMultimap listMultimap,
            ProgramClass programClass2,
            SetValuedMap setValuedMap,
            boolean bl,
            boolean bl1,
            boolean bl2,
            CommonSuperTypeResolver commonSuperTypeResolver1,
            ClassMemberLookup classMemberLookup1,
            boolean bl3,
            ScriptEnvironment scriptEnvironment1
    ) throws ZkmException, IOException {
        boolean bl4 = false;
        ArrayList arrayList = new ArrayList();

        for (int methodIndex = 0; methodIndex < this.methodCount; methodIndex++) {
            MethodInfo methodInfo1 = this.methods[methodIndex];
            List list1 = listMultimap.getValues(methodInfo1);
            if (list1 != null && list1.size() > 0) {
                boolean bl5 = methodInfo1.applyAutoReflectionHandling(
                        list1, programClass2.getClassName(), arrayList, bl, bl1, bl2, commonSuperTypeResolver1, classMemberLookup1, bl3, scriptEnvironment1
                );
                bl4 = bl4 || bl5;
            }
        }

        if (bl4) {
            setValuedMap.addValue(programClass2, this);
            if (arrayList.size() > 0) {
                this.addPoolConstants(arrayList);
                this.setChanged();
            } else {
                this.setChanged();
            }

            this.notifyObservers(new MutableInt(4), this, null);
        }

        return bl4;
    }

    public boolean hasStackMaps() {
        return this.stackMapsPresent;
    }

    public void pruneBootstrapMethods(Set set1) {
        ArrayList arrayList = new ArrayList();

        for (int attributeIndex = 0; attributeIndex < this.attributeCount; attributeIndex++) {
            if (this.attributes[attributeIndex] instanceof BootstrapMethodsAttribute) {
                BootstrapMethodsAttribute bootstrapMethodsAttribute1 = (BootstrapMethodsAttribute) this.attributes[attributeIndex];
                bootstrapMethodsAttribute1.retainEntries(set1);
                if (!bootstrapMethodsAttribute1.isEmpty()) {
                    arrayList.add(bootstrapMethodsAttribute1);
                }
            } else {
                arrayList.add(this.attributes[attributeIndex]);
            }
        }

        if (arrayList.size() < this.attributeCount) {
            this.attributes = ((com.zelix.klassmaster.classfile.attribute.Attribute[]) (arrayList.toArray(new Attribute[arrayList.size()])));
            this.attributeCount = this.attributes.length;
        }
    }

    public void removeInnerClassEntries(HashSet hashSet) {
        if (super.innerClassesTrimPending || super.innerClassesRemovalPending) {
            InnerClassesAttribute innerClassesAttribute = null;
            EnclosingMethodAttribute enclosingMethodAttribute = null;

            for (int attributeIndex = 0; attributeIndex < this.attributeCount; attributeIndex++) {
                if (this.attributes[attributeIndex] instanceof InnerClassesAttribute) {
                    innerClassesAttribute = (InnerClassesAttribute) this.attributes[attributeIndex];
                } else if (this.attributes[attributeIndex] instanceof EnclosingMethodAttribute) {
                    enclosingMethodAttribute = (EnclosingMethodAttribute) this.attributes[attributeIndex];
                }
            }

            int bd = innerClassesAttribute.removeEntriesForClasses(hashSet);
            boolean bl = false;
            boolean bl1 = false;
            boolean bl2;
            if (bd == 0) {
                bl = true;
                super.innerClassesTrimPending = false;
                super.innerClassesRemovalPending = false;
                bl2 = hashSet.contains(this);
            } else {
                bl2 = hashSet.contains(this);
            }

            if (bl2) {
                bl1 = true;
                this.clearInnerClassInfo();

                for (int methodIndex = 0; methodIndex < this.methodCount; methodIndex++) {
                    this.methods[methodIndex].clearSynthetic();
                }

                for (int fieldIndex = 0; fieldIndex < this.fieldCount; fieldIndex++) {
                    this.fields[fieldIndex].clearSynthetic();
                }
            }

            if (bl || bl1) {
                ArrayList arrayList = new ArrayList(this.attributeCount);

                for (int attributeIndexValue = 0; attributeIndexValue < this.attributeCount; attributeIndexValue++) {
                    if ((this.attributes[attributeIndexValue] != innerClassesAttribute || !bl) && (this.attributes[attributeIndexValue] != enclosingMethodAttribute || !bl1)) {
                        arrayList.add(this.attributes[attributeIndexValue]);
                    }
                }

                this.attributes = ((com.zelix.klassmaster.classfile.attribute.Attribute[]) (arrayList.toArray(new Attribute[arrayList.size()])));
                this.attributeCount = arrayList.size();
            }
        }
    }

    public ClassFileBase findInheritedFieldReferrer(Object key) {
        if (this.inheritedFieldReferrers == null) {
            return null;
        }

        Set set1 = this.inheritedFieldReferrers.getValues(key);
        return set1 != null ? (ClassFileBase) set1.iterator().next() : null;
    }

    public void analyzeMethodFlows(
            CommonSuperTypeResolver commonSuperTypeResolver1, ClassHierarchyQuery classHierarchyQuery, ScriptEnvironment scriptEnvironment1
    ) throws ZkmException, IOException {
        MethodInfo[] methodInfos = this.methods;
        int lengthValue = methodInfos.length;

        for (int methodInfoIndex = 0; methodInfoIndex < lengthValue; methodInfoIndex++) {
            methodInfos[methodInfoIndex].analyzeFlow(commonSuperTypeResolver1, classHierarchyQuery, scriptEnvironment1);
        }
    }

    public void setMethodsCreationKind(int methodsCreationKind) {
        MethodInfo[] methodInfos = this.methods;
        int lengthValue = methodInfos.length;

        for (int methodInfoIndex = 0; methodInfoIndex < lengthValue; methodInfoIndex++) {
            methodInfos[methodInfoIndex].setCreationKind(methodsCreationKind);
        }

        this.constantPool.clearOriginalStringFlags();
    }

    @Override
    public final boolean isProgramClass() {
        return true;
    }

    public MethodInfo groupStringConstants(PairMultiMap pairMultiMap, Set set1, Set set2, List list1, SetMultiMap setMultiMap, Map map1, boolean bl) throws ZkmException, IOException {
        MethodInfo methodInfo1 = null;
        HashMap hashMap = ZkmUtils.createHashMap();
        HashMap hashMap1 = ZkmUtils.createHashMap();
        ArrayList arrayList = new ArrayList();
        Iterator iterator = pairMultiMap.entrySet().iterator();

        while (iterator.hasNext()) {
            Entry entry = (Entry) iterator.next();
            MethodInfo methodInfo2 = (MethodInfo) entry.getKey();
            List list2 = (List) entry.getValue();
            arrayList.clear();
            Iterator iterator1 = list2.iterator();

            while (iterator1.hasNext()) {
                ObjectPair objectPair = (ObjectPair) iterator1.next();
                arrayList.add(objectPair.getFirst());
            }

            if (methodInfo2.isStaticInitializer()) {
                methodInfo1 = methodInfo2;
                set2.addAll(arrayList);
            } else {
                set1.addAll(arrayList);
            }

            iterator1 = arrayList.iterator();

            while (iterator1.hasNext()) {
                ResolvedStringConstant resolvedStringConstant1 = (ResolvedStringConstant) iterator1.next();
                String valueString = resolvedStringConstant1.getValueString();
                ObservableHolder observableHolder = (ObservableHolder) hashMap.get(valueString);
                if (observableHolder == null) {
                    observableHolder = new ObservableHolder(valueString);
                    hashMap.put(valueString, new ObservableHolder(valueString));
                }

                setMultiMap.addValue(observableHolder, resolvedStringConstant1);
                map1.put(resolvedStringConstant1, observableHolder);
            }
        }

        iterator = list1.iterator();

        while (iterator.hasNext()) {
            ObjectPair objectPair1 = (ObjectPair) iterator.next();
            ResolvedStringConstant resolvedStringConstant = (ResolvedStringConstant) objectPair1.getFirst();
            String valueStringValue = resolvedStringConstant.getValueString();
            ObservableHolder observableHolder1;
            if (bl) {
                observableHolder1 = (ObservableHolder) hashMap1.get(valueStringValue);
                if (observableHolder1 == null) {
                    observableHolder1 = new ObservableHolder(valueStringValue);
                    hashMap1.put(valueStringValue, observableHolder1);
                }
            } else {
                observableHolder1 = (ObservableHolder) hashMap.get(valueStringValue);
                if (observableHolder1 == null) {
                    observableHolder1 = new ObservableHolder(valueStringValue);
                    hashMap.put(valueStringValue, new ObservableHolder(valueStringValue));
                }
            }

            setMultiMap.addValue(observableHolder1, resolvedStringConstant);
            map1.put(resolvedStringConstant, observableHolder1);
        }

        return methodInfo1;
    }

    public void removeMethodParametersAttributes() {
        for (int methodIndex = 0; methodIndex < this.methodCount; methodIndex++) {
            this.methods[methodIndex].removeMethodParametersAttribute();
        }
    }

    public ArrayEnumeration enumerateMethods() {
        return new ArrayEnumeration(this.methods);
    }

    @Override
    public int getFieldCount() {
        return this.fields.length;
    }

    public ConstantPool getClassConstantPool() {
        return this.constantPool;
    }

    public void createReferenceObfuscationSupport(
            boolean bl,
            boolean bl1,
            boolean bl2,
            Map map1,
            Map map2,
            int[] integerArray,
            int[] integerArrayArgument,
            Map map3,
            List list1,
            InheritedMemberAnalyzer inheritedMemberAnalyzer,
            MethodKeyInjector methodKeyInjector,
            Set set1,
            ClassResolver classResolver1,
            ClassRepository classRepository1,
            ReferenceObfuscator referenceObfuscator,
            TwoKeyMap twoKeyMap,
            ObservableHolder observableHolder,
            ObservableHolder observableHolder1,
            ObservableHolder observableHolder2,
            boolean bl3
    ) throws ZkmException, IOException {
        map1.size();
        map2.size();
        if (map1.size() + map2.size() > 262144) {
            throw new ZkmProcessingException(
                    "Reference Obfuscation : Too many references specified : "
                            + (map1.size() + map2.size())
                            + " > "
                            + 262144
                            + " in class '"
                            + this.getDisplayLocationName()
                            + "'"
            );
        }

        MethodInfo methodInfo1 = classRepository1.findDeclaredMethod(this, MethodSignature.STATIC_INITIALIZER);
        String string3;
        if (methodInfo1 == null) {
            methodInfo1 = this.getOrCreateStaticInitializer(list1, classRepository1, "Reference Obfuscation", 4);
            string3 = "[Ljava/lang/Object;";
        } else {
            string3 = "[Ljava/lang/Object;";
        }

        String descriptor = string3;
        FieldInfo fieldInfo = this.createUniquelyNamedStaticField(descriptor, this.isInterface() ? 4 : 1, true, inheritedMemberAnalyzer, classRepository1, 4);
        String sourceName = fieldInfo.getSourceName();
        ResolvedFieldRef resolvedFieldRef = this.constantPool
                .getOrAddFieldRef(this.getClassName(), sourceName, fieldInfo.getDescriptor(), list1, classRepository1, classResolver1, true);
        FieldInfo fieldInfo1 = this.createUniquelyNamedStaticField(
                "[Ljava/lang/String;", this.isInterface() ? 4 : 1, true, inheritedMemberAnalyzer, classRepository1, 4
        );
        String sourceNameValue = fieldInfo1.getSourceName();
        ResolvedFieldRef resolvedFieldRef1 = this.constantPool
                .getOrAddFieldRef(this.getClassName(), sourceNameValue, fieldInfo1.getDescriptor(), list1, classRepository1, classResolver1, true);
        ConstantPool constantPool1 = this.getClassConstantPool();
        ResolvedMethodRef resolvedMethodRef = referenceObfuscator.createIndexDecoderMethod(
                this, resolvedFieldRef, resolvedFieldRef1, integerArray, inheritedMemberAnalyzer, classRepository1, classResolver1, list1, constantPool1
        );
        referenceObfuscator.buildReferenceTableInit(
                this,
                resolvedFieldRef,
                resolvedFieldRef1,
                methodInfo1,
                map1,
                map2,
                integerArray,
                map3,
                inheritedMemberAnalyzer,
                methodKeyInjector,
                classRepository1,
                classResolver1,
                list1,
                constantPool1
        );
        ResolvedMethodRef resolvedMethodRef1 = referenceObfuscator.createClassResolverMethod(
                this, resolvedMethodRef, resolvedFieldRef1, resolvedFieldRef, inheritedMemberAnalyzer, classRepository1, classResolver1, list1, constantPool1
        );
        Integer integer = (Integer) map1.get(classResolver1.getClassFile("java/lang/Object"));
        Long long1 = (Long) map3.get(integer);
        ConstantLong constantLong = constantPool1.getOrAddLongConstant(long1, list1);
        ResolvedMethodRef resolvedMethodRef2 = referenceObfuscator.createDeclaredFieldFinderMethod(
                this, inheritedMemberAnalyzer, classRepository1, classResolver1, list1, constantPool1
        );
        ResolvedMethodRef resolvedMethodRef3 = referenceObfuscator.createFieldLookupMethod(
                this, resolvedMethodRef2, inheritedMemberAnalyzer, classRepository1, classResolver1, list1, constantPool1
        );
        observableHolder.setValue(
                referenceObfuscator.createFieldResolverMethod(
                        bl,
                        this,
                        resolvedMethodRef,
                        resolvedFieldRef1,
                        resolvedFieldRef,
                        resolvedMethodRef1,
                        resolvedMethodRef2,
                        resolvedMethodRef3,
                        inheritedMemberAnalyzer,
                        classRepository1,
                        classResolver1,
                        list1,
                        constantLong,
                        constantPool1
                )
        );
        ResolvedMethodRef resolvedMethodRef4 = referenceObfuscator.createDeclaredMethodFinderMethod(
                this, inheritedMemberAnalyzer, classRepository1, classResolver1, list1, constantPool1
        );
        ResolvedMethodRef resolvedMethodRef5 = referenceObfuscator.createMethodLookupMethod(
                this, resolvedMethodRef4, inheritedMemberAnalyzer, classRepository1, classResolver1, list1, constantPool1
        );
        observableHolder1.setValue(
                referenceObfuscator.createMethodResolverMethod(
                        bl,
                        this,
                        resolvedMethodRef,
                        resolvedFieldRef1,
                        resolvedFieldRef,
                        resolvedMethodRef1,
                        resolvedMethodRef4,
                        resolvedMethodRef5,
                        inheritedMemberAnalyzer,
                        classRepository1,
                        classResolver1,
                        list1,
                        constantLong,
                        constantPool1
                )
        );
        if (bl1) {
            ResolvedMethodRef resolvedMethodRef6 = referenceObfuscator.createMethodHandleResolverMethod(
                    bl,
                    this,
                    integerArrayArgument,
                    (ResolvedMethodRef) observableHolder.getValue(),
                    (ResolvedMethodRef) observableHolder1.getValue(),
                    inheritedMemberAnalyzer,
                    classRepository1,
                    classResolver1,
                    list1,
                    constantPool1
            );
            ResolvedMethodRef resolvedMethodRef7 = referenceObfuscator.createCallSiteInvokerMethod(
                    bl, this, resolvedMethodRef6, inheritedMemberAnalyzer, classRepository1, classResolver1, list1, constantPool1
            );
            ResolvedMethodRef resolvedMethodRef8 = referenceObfuscator.createBootstrapMethod(
                    bl, this, resolvedMethodRef7, inheritedMemberAnalyzer, classRepository1, classResolver1, list1, constantPool1
            );
            observableHolder2.setValue(resolvedMethodRef8);
        }

        if (bl2 && bl3) {
            referenceObfuscator.createArgumentPackerMethods(
                    bl, this, twoKeyMap, map2, inheritedMemberAnalyzer, set1, classRepository1, classResolver1, list1, constantPool1
            );
        }
    }

    public void obfuscateFlow(
            ListMultimap listMultimap,
            CommonSuperTypeResolver commonSuperTypeResolver1,
            ClassHierarchyQuery classHierarchyQuery,
            Map map1,
            int ba,
            boolean bl,
            boolean bl1,
            FlowObfuscationManager flowObfuscationManager,
            ScriptEnvironment scriptEnvironment1,
            FlowObfuscationExclusions flowObfuscationExclusions,
            StaticInitCalleeAnalyzer staticInitCalleeAnalyzer1,
            Random random1
    ) throws ZkmException, IOException {
        int integerValue = ba;
        if (!this.isInterface() || this.supportsJava8()) {
            PrintWriter printWriter = scriptEnvironment1.getLogWriter();
            ArrayList arrayList = new ArrayList(7);
            boolean bl2 = !bl;
            if (flowObfuscationExclusions == null || !flowObfuscationExclusions.isClassExcluded(this)) {
                if (this.zkmAnnotationValues != null && this.zkmAnnotationValues.hasValue("obfuscateFlow")) {
                    String valueValue = this.zkmAnnotationValues.getValue("obfuscateFlow");
                    int integerValue2 = ObfuscateOptions.parseFlowLevel(valueValue);
                    if (integerValue != integerValue2) {
                        String flowLevelName = ObfuscateOptions.getFlowLevelName(integerValue);
                        integerValue = integerValue2;
                        if (scriptEnvironment1.isVerbose()) {
                            printWriter.println(
                                    "Overriding Flow Obfuscation setting in class '"
                                            + AbstractExclusionSpec.formatClass(this, classHierarchyQuery, true)
                                            + "' : '"
                                            + flowLevelName
                                            + "' changed to '"
                                            + valueValue
                                            + "'"
                            );
                        }

                        if (integerValue == 0) {
                            return;
                        }
                    }
                }

                RankedValue[] rankedValues = new RankedValue[this.methodCount];

                for (int i = 0; i < this.methodCount; i++) {
                    rankedValues[i] = new RankedValue(this.methods[i].getCodeSize(), this.methods[i]);
                }

                Arrays.sort(rankedValues);
                int maxValue = Integer.MAX_VALUE;
                int bh = 0;

                for (int i = rankedValues.length - 1; i >= 0; i += -1) {
                    MethodInfo methodInfo1 = (MethodInfo) rankedValues[i].getValue();
                    if ((flowObfuscationExclusions == null || !flowObfuscationExclusions.isMethodExcluded(methodInfo1))
                            && (staticInitCalleeAnalyzer1 == null || !staticInitCalleeAnalyzer1.isInitializerOrCallee(methodInfo1))
                            && !flowObfuscationManager.isGeneratedMethod(methodInfo1)) {
                        try {
                            int be = methodInfo1.obfuscateFlow(
                                    listMultimap, commonSuperTypeResolver1, scriptEnvironment1, classHierarchyQuery, integerValue, bl, bl1, map1, false, random1
                            );
                            bh += be;
                            if (scriptEnvironment1.isVerbose() && be > 0) {
                                printWriter.println(
                                        "\tObfuscated flow in method '"
                                                + AbstractExclusionSpec.formatMethod(this.methods[i], false, classHierarchyQuery)
                                                + "' in class '"
                                                + AbstractExclusionSpec.formatClass(this, classHierarchyQuery, false)
                                                + "'"
                                                + (bl2 ? ' ' : "")
                                );
                                bl2 = false;
                            }
                        } catch (MethodAnalysisException methodAnalysisException) {
                            scriptEnvironment1.logMessage(
                                    "Couldn't obfuscate flow in method '"
                                            + AbstractExclusionSpec.formatMethod(this.methods[i], false, classHierarchyQuery)
                                            + "' in class '"
                                            + AbstractExclusionSpec.formatClass(this, classHierarchyQuery, false)
                                            + "' : "
                                            + methodAnalysisException.getMessage()
                            );
                        } catch (ZkmClassNotFoundException zkmClassNotFoundException) {
                            scriptEnvironment1.logWarning(
                                    "Method '"
                                            + AbstractExclusionSpec.formatMethod(this.methods[i], false, classHierarchyQuery)
                                            + "' in class '"
                                            + AbstractExclusionSpec.formatClass(this, classHierarchyQuery, false)
                                            + "' could not be flow obfuscated because class '"
                                            + zkmClassNotFoundException.getClassName()
                                            + "' could not be found in classpath."
                            );
                        } catch (StackAnalysisException stackAnalysisException) {
                            throw new ZkmProcessingException(
                                    "Method '"
                                            + AbstractExclusionSpec.formatMethod(this.methods[i], false, classHierarchyQuery)
                                            + "' in class '"
                                            + this.getDisplayLocationName()
                                            + "' appears to be invalid (3) : '"
                                            + stackAnalysisException.getMessage()
                                            + "'"
                            );
                        }
                    }
                }
            }

            if (arrayList.size() > 0) {
                this.constantPool.appendEntries(arrayList);
                this.setChanged();
                this.notifyObservers(new MutableInt(4), this, null);
            }
        }
    }

    public void emitMultiIntDecryption(
            LocalVariableList localVariableList1,
            List list1,
            ResolvedFieldRef resolvedFieldRef,
            int ba,
            ObservableHolder[] observableHolders,
            SetMultiMap setMultiMap,
            Map map1,
            Map map2,
            ObservableHolder observableHolder,
            MutableInt mutableInt,
            ListMultimap listMultimap,
            LocalVariableAllocator localVariableAllocator,
            List list2,
            IntegerConstantEncryptor integerConstantEncryptor,
            MethodInfo methodInfo1,
            ObservableHolder observableHolder1,
            Long long1,
            LocalVariableIndex localVariableIndex1,
            Map map3,
            ObservableHolder observableHolder2,
            boolean bl,
            MethodParameterChanger methodParameterChanger,
            ClassMemberLookup classMemberLookup1,
            ClassResolver classResolver1
    ) throws ZkmException, IOException {
        LocalVariableIndex localVariableIndex2 = localVariableIndex1;
        if (long1 != null) {
            if (localVariableIndex2 == null) {
                localVariableIndex2 = methodParameterChanger.emitKeyLocalInit(
                        long1, localVariableAllocator, list1, methodInfo1, map3, this.getClassConstantPool(), list2
                );
            }

            if (bl) {
                int andIncrement = localVariableAllocator.getAndIncrement();
                integerConstantEncryptor.emitDesCipherInit(
                        localVariableList1, list1, andIncrement, localVariableIndex2, localVariableAllocator, list2, classMemberLookup1, classResolver1
                );
                observableHolder1.setValue(integerCache.valueOf(andIncrement));
            }
        } else if (integerConstantEncryptor.hasClassKey()) {
            Instruction.appendLongConstant(integerConstantEncryptor.getClassKey(), list1, this.constantPool, list2);
            int value = localVariableAllocator.getValue();
            localVariableAllocator.addAndGet(2);
            list1.add(Instruction.createLongStore(value, localVariableList1, 11));
            observableHolder2.setValue(integerCache.valueOf(value));
        }

        HashSet hashSet = ZkmUtils.createHashSet();
        integerConstantEncryptor.emitEncryptedArrayInit(
                localVariableList1,
                list1,
                localVariableAllocator,
                hashSet,
                list2,
                resolvedFieldRef,
                ba,
                observableHolders,
                setMultiMap,
                map1,
                (LabelInstruction) observableHolder.getValue(),
                mutableInt,
                listMultimap,
                long1,
                bl,
                classMemberLookup1,
                classResolver1
        );
        this.constantPool.removeEntries(hashSet);
        if (!map2.isEmpty()) {
            ArrayList arrayList = new ArrayList();
            if (resolvedFieldRef != null) {
                Iterator iterator1 = map2.entrySet().iterator();

                while (iterator1.hasNext()) {
                    Entry entry1 = (Entry) iterator1.next();
                    ResolvedFieldRef resolvedFieldRef2 = (ResolvedFieldRef) entry1.getKey();
                    ObservableHolder observableHolder4 = (ObservableHolder) entry1.getValue();
                    EncryptedValueLocation encryptedValueLocation1 = (EncryptedValueLocation) map1.get(observableHolder4);
                    integerConstantEncryptor.emitStoreDecryptedValue(
                            localVariableList1, arrayList, resolvedFieldRef2, encryptedValueLocation1, long1, localVariableIndex2, list2
                    );
                }
            } else {
                arrayList.add(Instruction.createObjectLoad(ba, localVariableList1, 11));
                Iterator iterator = map2.entrySet().iterator();

                while (iterator.hasNext()) {
                    Entry entry = (Entry) iterator.next();
                    ResolvedFieldRef resolvedFieldRef1 = (ResolvedFieldRef) entry.getKey();
                    ObservableHolder observableHolder3 = (ObservableHolder) entry.getValue();
                    EncryptedValueLocation encryptedValueLocation = (EncryptedValueLocation) map1.get(observableHolder3);
                    integerConstantEncryptor.emitStoreDecryptedValue(
                            localVariableList1, arrayList, resolvedFieldRef1, encryptedValueLocation, long1, localVariableIndex2, list2
                    );
                }

                arrayList.add(SimpleInstruction.forOpcode(87));
            }

            list1.addAll(arrayList);
        }
    }

    public MethodInfo createUniquelyNamedPublicStaticMethod(
            String valueArgument,
            ArrayList arrayList,
            int maxLocals,
            LocalVariableList localVariableList1,
            ExceptionHandlerSpec[] exceptionHandlerSpecs,
            List list1,
            InheritedMemberAnalyzer inheritedMemberAnalyzer,
            ClassMemberLookup classMemberLookup1
    ) throws ZkmProcessingException {
        return this.createUniquelyNamedStaticMethod(
                valueArgument, arrayList, 1, maxLocals, 4, localVariableList1, exceptionHandlerSpecs, "Flow Obfuscation", list1, inheritedMemberAnalyzer, classMemberLookup1, 2
        );
    }

    public void pruneInnerClassesAttribute(TrimProcessor trimProcessor1) {
        int ba = 0;
        ArrayList arrayList = new ArrayList(this.attributeCount);
        InnerClassesAttribute innerClassesAttribute = null;

        for (int attributeIndex = 0; attributeIndex < this.attributeCount; attributeIndex++) {
            if (this.attributes[attributeIndex] instanceof InnerClassesAttribute) {
                innerClassesAttribute = (InnerClassesAttribute) this.attributes[attributeIndex];
                ba = innerClassesAttribute.removeTrimmedEntries(trimProcessor1);
            } else {
                arrayList.add(this.attributes[attributeIndex]);
            }
        }

        if (ba == 0 && innerClassesAttribute != null) {
            int attributeCountValue = arrayList.size();
            this.attributes = ((com.zelix.klassmaster.classfile.attribute.Attribute[]) (arrayList.toArray(new Attribute[attributeCountValue])));
            this.attributeCount = attributeCountValue;
            super.innerClassesTrimPending = false;
            super.innerClassesRemovalPending = false;
            this.clearInnerClassInfo();
        }
    }

    public String generateUniqueFieldName(List list1, String descriptor, InheritedMemberAnalyzer inheritedMemberAnalyzer, String prefix) {
        ArrayList arrayList = list1 != null ? new ArrayList(list1) : null;
        IndexedNameGenerator indexedNameGenerator = new IndexedNameGenerator(prefix);
        int ba = 0;
        HashSet hashSet = null;
        HashSet hashSet1 = null;
        Set set1 = inheritedMemberAnalyzer.getVisibleFieldNames(this);

        String nameAt;
        boolean bl;
        do {
            bl = false;

            do {
                if (arrayList != null && !arrayList.isEmpty()) {
                    nameAt = (String) arrayList.remove(0);
                } else {
                    nameAt = indexedNameGenerator.getNameAt(ba++);
                }
            } while (nameAt == null);

            FieldSignature fieldSignature = new FieldSignature(nameAt, descriptor);
            if (set1.contains(nameAt)) {
                bl = true;
            } else {
                if (hashSet == null) {
                    hashSet = ZkmUtils.createHashSet();

                    for (int fieldIndex = 0; fieldIndex < this.fieldCount; fieldIndex++) {
                        FieldSignature fieldSignature1 = new FieldSignature(this.fields[fieldIndex].getOriginalMemberName(), this.fields[fieldIndex].getDescriptor());
                        hashSet.add(fieldSignature1);
                    }
                }

                if (hashSet.contains(fieldSignature)) {
                    bl = true;
                }
            }

            if (!bl && this.inheritedFieldReferrers != null) {
                if (hashSet1 == null) {
                    hashSet1 = ZkmUtils.createHashSet();
                    Iterator iterator = this.inheritedFieldReferrers.keySet().iterator();

                    while (iterator.hasNext()) {
                        AbstractFieldInfo abstractFieldInfo = (AbstractFieldInfo) iterator.next();
                        String sourceName = abstractFieldInfo.getSourceName();
                        hashSet1.add(sourceName);
                    }
                }

                if (hashSet1.contains(nameAt)) {
                    bl = true;
                }
            }
        } while (bl);

        return nameAt;
    }

    public boolean hasZkmMethodAnnotations() {
        return this.zkmMethodAnnotationsPresent;
    }

    public MethodInfo createPublicStaticMethod(
            String valueArgument,
            String valueArgument2,
            ArrayList arrayList,
            int maxLocals,
            LocalVariableList localVariableList1,
            ExceptionHandlerSpec[] exceptionHandlerSpecs,
            List list1,
            InheritedMemberAnalyzer inheritedMemberAnalyzer,
            ClassMemberLookup classMemberLookup1
    ) throws ZkmProcessingException {
        return this.createStaticMethod(
                valueArgument,
                valueArgument2,
                arrayList,
                1,
                maxLocals,
                4,
                localVariableList1,
                exceptionHandlerSpecs,
                "Flow Obfuscation",
                list1,
                inheritedMemberAnalyzer,
                classMemberLookup1,
                2
        );
    }

    public void setAccessFlags(int accessFlagsArgument) throws ZkmException, IOException {
        this.accessFlags.setFlags(accessFlagsArgument);
        this.setChanged();
        this.notifyObservers(new MutableInt(1), this, null);
    }

    @Override
    public AbstractMethodInfo[] getDeclaredMethods() {
        return this.getMethodInfos();
    }

    public String generateUniqueMethodName(List list1, String string, InheritedMemberAnalyzer inheritedMemberAnalyzer, boolean bl) {
        return this.generateUniqueMethodName(list1, string, inheritedMemberAnalyzer, bl, (String) null);
    }

    public void rebuildStackMapTables(CommonSuperTypeResolver commonSuperTypeResolver1, ClassHierarchyQuery classHierarchyQuery) throws ZkmException, IOException {
        ArrayList arrayList = new ArrayList();

        for (int methodIndex = 0; methodIndex < this.methodCount; methodIndex++) {
            try {
                this.methods[methodIndex].rebuildStackMapTable(commonSuperTypeResolver1, classHierarchyQuery, this.constantPool, arrayList);
            } catch (StackAnalysisException stackAnalysisException) {
                throw new ZkmProcessingException(this.getLocationName() + " " + this.methods[methodIndex].toDisplayString() + " : " + stackAnalysisException.getMessage());
            }
        }

        this.constantPool.appendEntries(arrayList);
    }

    public void renameMethods(
            NameExclusionSet nameExclusionSet,
            ChangeLogMapping changeLogMapping1,
            MethodRenamer methodRenamer1,
            MethodOverrideAnalyzer methodOverrideAnalyzer,
            MethodNameGeneratorBase methodNameGeneratorBase,
            Map map1,
            ListMultimap listMultimap,
            int ba,
            Map map2,
            DisableableMap disableableMap,
            DisableableMap disableableMap1,
            boolean bl,
            boolean bl1,
            Set set1,
            ClassHierarchyNode classHierarchyNode,
            Map map3
    ) throws ZkmException, IOException {
        HashMap hashMap = ZkmUtils.createHashMap();
        HashMap hashMap1 = ZkmUtils.createHashMap();
        ArrayList arrayList = new ArrayList(this.methodCount);

        for (int methodIndex = 0; methodIndex < this.methodCount; methodIndex++) {
            arrayList.add(this.methods[methodIndex]);
        }

        if (bl1 || HiddenOptionFlags.RANDOMIZE_OBFUSCATION) {
            ZkmUtils.shuffleList(arrayList, ZkmUtils.createRandom(512));
        }

        Iterator iterator = arrayList.iterator();

        while (iterator.hasNext()) {
            ((MethodInfo) iterator.next())
                    .renameMethod(
                            nameExclusionSet,
                            changeLogMapping1,
                            methodRenamer1,
                            methodOverrideAnalyzer,
                            methodNameGeneratorBase,
                            map1,
                            listMultimap,
                            classHierarchyNode,
                            this.getClassName(),
                            this.isInterface(),
                            ba,
                            hashMap,
                            map2,
                            hashMap1,
                            disableableMap,
                            disableableMap1,
                            set1,
                            bl,
                            map3
                    );
        }
    }

    public MethodInfo groupIntegerConstants(PairMultiMap pairMultiMap, Set set1, Set set2, List list1, SetMultiMap setMultiMap, Map map1) throws ZkmException, IOException {
        MethodInfo methodInfo1 = null;
        HashMap hashMap = ZkmUtils.createHashMap();
        HashMap hashMap1 = ZkmUtils.createHashMap();
        ArrayList arrayList = new ArrayList();
        Iterator iterator = pairMultiMap.entrySet().iterator();

        while (iterator.hasNext()) {
            Entry entry = (Entry) iterator.next();
            MethodInfo methodInfo2 = (MethodInfo) entry.getKey();
            List list2 = (List) entry.getValue();
            arrayList.clear();
            Iterator iterator1 = list2.iterator();

            while (iterator1.hasNext()) {
                ObjectPair objectPair = (ObjectPair) iterator1.next();
                arrayList.add(objectPair.getFirst());
            }

            if (methodInfo2.isStaticInitializer()) {
                methodInfo1 = methodInfo2;
                set2.addAll(arrayList);
            } else {
                set1.addAll(arrayList);
            }

            iterator1 = arrayList.iterator();

            while (iterator1.hasNext()) {
                ConstantInteger constantInteger1 = (ConstantInteger) iterator1.next();
                Integer integer = constantInteger1.getValue();
                ObservableHolder observableHolder = (ObservableHolder) hashMap.get(integer);
                if (observableHolder == null) {
                    observableHolder = new ObservableHolder(integer);
                    hashMap.put(integer, new ObservableHolder(integer));
                }

                setMultiMap.addValue(observableHolder, constantInteger1);
                map1.put(constantInteger1, observableHolder);
            }
        }

        iterator = list1.iterator();

        while (iterator.hasNext()) {
            ObjectPair objectPair1 = (ObjectPair) iterator.next();
            ConstantInteger constantInteger = (ConstantInteger) objectPair1.getFirst();
            Integer integer1 = constantInteger.getValue();
            ObservableHolder observableHolder1 = (ObservableHolder) hashMap1.get(integer1);
            if (observableHolder1 == null) {
                observableHolder1 = new ObservableHolder(integer1);
                hashMap1.put(integer1, observableHolder1);
            }

            setMultiMap.addValue(observableHolder1, constantInteger);
            map1.put(constantInteger, observableHolder1);
        }

        return methodInfo1;
    }

    public synchronized void addField(FieldInfo fieldInfo, ClassMemberLookup classMemberLookup1) {
        FieldInfo[] fieldInfos = new FieldInfo[this.fieldCount + 1];
        System.arraycopy(this.fields, 0, fieldInfos, 0, this.fieldCount);
        fieldInfos[this.fieldCount] = fieldInfo;
        fieldInfo.setParent(this);
        this.fields = fieldInfos;
        this.fieldCount++;
        if (!this.isVersionedVariant()) {
            classMemberLookup1.registerField(this, fieldInfo);
        }
    }

    public boolean obfuscateReferences(
            boolean bl,
            ProgramClass programClass2,
            Map map1,
            ListMultimap listMultimap,
            NestedMultiMap nestedMultiMap,
            List list1,
            Map map2,
            Map map3,
            InheritedMemberAnalyzer inheritedMemberAnalyzer,
            int[] integerArray,
            Map map4,
            ResolvedMethodRef resolvedMethodRef,
            ResolvedMethodRef resolvedMethodRef1,
            ResolvedMethodRef resolvedMethodRef2,
            MutableInt mutableInt,
            MutableInt mutableInt1,
            MutableInt mutableInt2,
            Map map5,
            Map map6,
            ClassRepository classRepository1,
            CommonSuperTypeResolver commonSuperTypeResolver1,
            ClassResolver classResolver1,
            ReferenceObfuscator referenceObfuscator,
            ScriptEnvironment scriptEnvironment1
    ) throws ZkmException, IOException {
        int[] integerArrayValue = integerArray;
        Map map8 = map1;
        Map map7 = map4;
        boolean bl1 = false;
        TwoKeyMap twoKeyMap = new TwoKeyMap();
        if (!this.isInterface() || this.supportsJava8()) {
            boolean bl2 = ReferenceObfuscator.bothSupportInvokedynamic(bl ? programClass2 : this, this);
            ArrayList arrayList = new ArrayList();
            ObservableHolder observableHolder = new ObservableHolder();
            ObservableHolder observableHolder1 = new ObservableHolder();
            ObservableHolder observableHolder2 = new ObservableHolder();
            new ObservableHolder();
            Map map9 = referenceObfuscator.assignTypeIndices(listMultimap, classResolver1, scriptEnvironment1.getIgnoreMissingReferencesSpec());
            BooleanFlag booleanFlag = new BooleanFlag();
            BooleanFlag booleanFlag1 = new BooleanFlag();
            if (!bl) {
                BooleanFlag booleanFlag2 = new BooleanFlag();
                BooleanFlag booleanFlag3 = new BooleanFlag();
                HashSet hashSet = ZkmUtils.createHashSet();
                map8 = referenceObfuscator.assignMemberRefIndices(
                        bl2, booleanFlag, booleanFlag1, listMultimap, map9.size(), hashSet, booleanFlag2, booleanFlag3, classRepository1
                );
                int[] integerArrayValue2 = referenceObfuscator.shuffleKeyBitPositions();
                integerArrayValue = referenceObfuscator.pickRandomLetters();
                map7 = ZkmUtils.createHashMap(ZkmUtils.getPrimeCapacity(map9.size() + map8.size()));
                boolean value = booleanFlag.getValue();
                boolean valueValue = booleanFlag1.getValue();
                MethodKeyInjector methodKeyInjector = (MethodKeyInjector) null;
                booleanFlag2.getValue();
                Boolean boolean1 = booleanFlag3.getValue();
                this.createReferenceObfuscationSupport(
                        bl,
                        value,
                        valueValue,
                        map9,
                        map8,
                        integerArrayValue2,
                        integerArrayValue,
                        map7,
                        arrayList,
                        inheritedMemberAnalyzer,
                        methodKeyInjector,
                        hashSet,
                        classResolver1,
                        classRepository1,
                        referenceObfuscator,
                        twoKeyMap,
                        observableHolder,
                        observableHolder1,
                        observableHolder2,
                        boolean1
                );
            } else {
                Iterator iterator = listMultimap.entrySet().iterator();

                while (iterator.hasNext()) {
                    Entry entry = (Entry) iterator.next();
                    Iterator iterator1 = ((List) entry.getValue()).iterator();

                    while (iterator1.hasNext()) {
                        ClassMemberRef classMemberRef = (ClassMemberRef) iterator1.next();
                        if (bl2 && ReferenceObfuscator.canUseInvokedynamic(classMemberRef, classRepository1)) {
                            booleanFlag.setValue(true);
                        } else {
                            booleanFlag1.setValue(true);
                        }
                    }
                }

                if (booleanFlag.getValue() && resolvedMethodRef2 != null) {
                    observableHolder2.setValue(this.constantPool.getOrCreateMethodRefConstant((AbstractMethodInfo) resolvedMethodRef2.getResolvedMember(), arrayList));
                }

                if (booleanFlag1.getValue()) {
                    if (resolvedMethodRef != null) {
                        observableHolder.setValue(
                                this.constantPool.getOrCreateMethodRefConstant((AbstractMethodInfo) resolvedMethodRef.getResolvedMember(), arrayList)
                        );
                    }

                    if (resolvedMethodRef1 != null) {
                        observableHolder1.setValue(
                                this.constantPool.getOrCreateMethodRefConstant((AbstractMethodInfo) resolvedMethodRef1.getResolvedMember(), arrayList)
                        );
                    }

                    iterator = list1.iterator();

                    while (iterator.hasNext()) {
                        ClassMemberRef classMemberRef1 = (ClassMemberRef) iterator.next();
                        if (ReferenceObfuscator.needsReflectionInvoker(programClass2, this, classMemberRef1, classRepository1)) {
                            ResolvedMethodRef resolvedMethodRef3 = (ResolvedMethodRef) map3.get(classMemberRef1);
                            ResolvedMethodRefConstant resolvedMethodRefConstant = this.constantPool
                                    .getOrCreateMethodRefConstant((AbstractMethodInfo) resolvedMethodRef3.getResolvedMember(), arrayList);
                            twoKeyMap.putValue(programClass2, classMemberRef1, resolvedMethodRefConstant);
                        }
                    }
                }
            }

            ConstantPool constantPool1 = this.getClassConstantPool();
            ListMultimap listMultimap1 = new ListMultimap();
            ListMultimap listMultimap2 = new ListMultimap();
            HashMap hashMap = ZkmUtils.createHashMap();

            for (int methodIndex = 0; methodIndex < this.methodCount; methodIndex += 1) {
                MethodInfo methodInfo1 = this.methods[methodIndex];
                LocalVariableIndex localVariableIndex1 = map6 != null ? (LocalVariableIndex) map6.get(methodInfo1) : null;
                Long long1 = localVariableIndex1 != null ? (Long) map5.get(methodInfo1) : null;
                MethodBytecode methodBytecode1 = methodInfo1.getBytecode();
                if (methodBytecode1 != null && listMultimap.containsKey(methodBytecode1)) {
                    ArrayList arrayList1 = new ArrayList();
                    ObservableHolder observableHolder3 = new ObservableHolder();
                    List list2 = methodBytecode1.obfuscateReferences(
                            listMultimap.getValues(methodBytecode1),
                            nestedMultiMap,
                            map8,
                            map2,
                            map7,
                            (ResolvedMethodRef) observableHolder.getValue(),
                            (ResolvedMethodRef) observableHolder1.getValue(),
                            twoKeyMap.getInnerMap(programClass2),
                            (ResolvedMethodRef) observableHolder2.getValue(),
                            integerArrayValue,
                            hashMap,
                            observableHolder3,
                            long1,
                            localVariableIndex1,
                            referenceObfuscator,
                            constantPool1,
                            arrayList1,
                            arrayList,
                            classRepository1,
                            classResolver1,
                            commonSuperTypeResolver1,
                            bl2
                    );
                    if (list2.size() > 0) {
                        listMultimap1.appendValues(methodBytecode1, list2);
                    }

                    if (arrayList1.size() > 0) {
                        listMultimap2.appendValues(methodBytecode1, arrayList1);
                    }
                }
            }

            if (arrayList.size() > 0) {
                constantPool1.appendEntries(arrayList);
            }

            PrintWriter printWriter = null;
            if (scriptEnvironment1.isVerbose() && listMultimap1.getKeyCount() > 0) {
                printWriter = scriptEnvironment1.getLogWriter();
                printWriter.println("\tObfuscated references in class '" + this.getDisplayLocationName() + "'");
                mutableInt.incrementAndGet();
            }

            Iterator iterator2 = listMultimap1.entrySet().iterator();

            while (iterator2.hasNext()) {
                Entry entry1 = (Entry) iterator2.next();
                MethodBytecode methodBytecode2 = (MethodBytecode) entry1.getKey();
                List list3 = (List) entry1.getValue();
                methodBytecode2.applyCodeInsertions(list3, "Reference Obfuscation");
                if (list3 != null && list3.size() > 0) {
                    bl1 = true;
                }

                List list4 = listMultimap2.getValues(methodBytecode2);
                if (list4 != null) {
                    ((CodeAttributeBody) methodBytecode2.getParent()).insertExceptionTableEntries(false, list4, scriptEnvironment1);
                }

                if (scriptEnvironment1.isVerbose()) {
                    printWriter.println("\t\tmethod '" + AbstractExclusionSpec.formatMethod(methodBytecode2.getMethod(), false, classRepository1) + "'");
                    mutableInt1.incrementAndGet();
                    if (listMultimap.containsKey(methodBytecode2)) {
                        Iterator iterator3 = listMultimap.getValues(methodBytecode2).iterator();

                        while (iterator3.hasNext()) {
                            ClassMemberRef classMemberRef2 = (ClassMemberRef) iterator3.next();
                            int valuesSize = nestedMultiMap.getValues(classMemberRef2, methodBytecode2).size();
                            String string;
                            if (classMemberRef2.isField()) {
                                AbstractFieldInfo abstractFieldInfo = (AbstractFieldInfo) classMemberRef2.getMember();
                                string = AbstractExclusionSpec.formatField(abstractFieldInfo, false, classRepository1);
                            } else {
                                AbstractMethodInfo abstractMethodInfo = (AbstractMethodInfo) classMemberRef2.getMember();
                                string = AbstractExclusionSpec.formatMethod(abstractMethodInfo, false, classRepository1);
                            }

                            printWriter.println(
                                    "\t\t\t"
                                            + valuesSize
                                            + " reference"
                                            + (valuesSize > 1 ? "s" : "")
                                            + " to "
                                            + (classMemberRef2.isField() ? "field" : "method")
                                            + " '"
                                            + string
                                            + "' in class '"
                                            + classMemberRef2.getOwnerLocationName()
                                            + "'"
                                            + ""
                            );
                            mutableInt2.addAndGet(valuesSize);
                        }
                    }
                }
            }
        }

        return bl1;
    }

    public ProgramClass(
            ClassFileInputStream classFileInputStream,
            InputFileLocation inputFileLocation,
            ObservableHolder observableHolder,
            ListenerRegistry listenerRegistry1,
            PrintWriter printWriter,
            ThreeKeyMultiMap threeKeyMultiMap,
            int creationKind
    ) throws ZkmException, IOException {
        super(inputFileLocation, classFileInputStream.getDigest(), creationKind);
        observableHolder.setValue("Opening " + inputFileLocation.getName());
        this.statusHolder = observableHolder;
        this.listenerRegistry = listenerRegistry1;

        try {
            this.readMagicAndVersion(classFileInputStream);
            ListMultimap listMultimap = new ListMultimap(Math.max(20, (int) (super.constantPoolCount * 0.7)));
            ListMultimap listMultimap1 = new ListMultimap(Math.max(10, (int) (super.constantPoolCount * 0.2)));
            ListMultimap listMultimap2 = new ListMultimap();
            ListMultimap listMultimap3 = new ListMultimap();
            ListMultimap listMultimap4 = new ListMultimap();
            ListMultimap listMultimap5 = new ListMultimap();
            ListMultimap listMultimap6 = new ListMultimap();
            ListMultimap listMultimap7 = new ListMultimap();
            this.constantPool = new ConstantPool(classFileInputStream, this, listMultimap, listMultimap1, listMultimap5, listMultimap6);
            super.constantPoolCount = this.constantPool.getEntryCount();
            this.accessFlags = new AccessFlags(this, classFileInputStream);
            int bb = classFileInputStream.readUnsignedShort();
            int bc = classFileInputStream.readUnsignedShort();
            super.interfaceCount = classFileInputStream.readUnsignedShort();
            int[] integerArray = new int[super.interfaceCount];
            int integerArrayElementIndex = 0;
            int bj = 0;

            for (int interfaceCountValue = super.interfaceCount; bj < interfaceCountValue; interfaceCountValue = super.interfaceCount) {
                integerArray[integerArrayElementIndex] = classFileInputStream.readUnsignedShort();
                bj = ++integerArrayElementIndex;
            }

            ListMultimap listMultimap8 = listMultimap5;
            int[] integerArrayValue = integerArray;
            Integer integer = bc;
            this.readClassConstants(bb, integer, integerArrayValue, listMultimap8);
            this.fieldCount = classFileInputStream.readUnsignedShort();
            this.fields = new FieldInfo[this.fieldCount];

            for (int fieldIndex = 0; fieldIndex < this.fieldCount; fieldIndex++) {
                this.fields[fieldIndex] = new FieldInfo(
                        this,
                        classFileInputStream,
                        listMultimap,
                        listMultimap1,
                        listMultimap2,
                        listMultimap3,
                        listMultimap4,
                        listMultimap5,
                        listMultimap6,
                        printWriter,
                        threeKeyMultiMap
                );
                this.fields[fieldIndex].addObserver(this);
            }

            this.methodCount = classFileInputStream.readUnsignedShort();
            this.methods = new MethodInfo[this.methodCount];

            for (int methodIndex = 0; methodIndex < this.methodCount; methodIndex++) {
                this.methods[methodIndex] = new MethodInfo(
                        this,
                        classFileInputStream,
                        listMultimap,
                        listMultimap1,
                        listMultimap2,
                        listMultimap3,
                        listMultimap4,
                        listMultimap5,
                        listMultimap6,
                        listMultimap7,
                        printWriter,
                        threeKeyMultiMap
                );
                this.methods[methodIndex].addObserver(this);
                if (this.methods[methodIndex].isVarargs() && this.methods[methodIndex].getParameterTypes().size() == 0) {
                    this.methods[methodIndex].setVarargs();
                    printWriter.println(
                            "ERROR: Method '"
                                    + this.methods[methodIndex].toOriginalDisplayString()
                                    + "' in class '"
                                    + this.methods[methodIndex].getDisplayLocationName()
                                    + "' has its ACC_VARARGS flag set yet it has no parameters. This could result in unpredictable behaviour so clearing ACC_VARARGS flag."
                    );
                }
            }

            this.attributeCount = classFileInputStream.readUnsignedShort();
            this.attributes = new Attribute[this.attributeCount];

            for (int attributeIndex = 0; attributeIndex < this.attributeCount; attributeIndex++) {
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
                        listMultimap7,
                        printWriter,
                        (ListMultimap) null,
                        threeKeyMultiMap
                );
                if (this.attributes[attributeIndex] instanceof BootstrapMethodsAttribute) {
                    this.constantPool.linkBootstrapMethods((BootstrapMethodsAttribute) this.attributes[attributeIndex]);
                }
            }

            try {
                integerArrayElementIndex = classFileInputStream.readUnsignedByte();
                throw new ClassFileFormatException(
                        "Extra byte at the end of class file " + integerArrayElementIndex + " in '" + inputFileLocation.getQualifiedName() + "' : File is probably corrupt (AZ)"
                );
            } catch (EOFException eOFException) {
            } catch (IOException iOException1) {
            }

            this.resolveInnerClassEntry();
            if (!HiddenOptionFlags.SKIP_REFERENCE_INDEXING) {
                this.constantPool
                        .unshareConstants(listMultimap, listMultimap1, listMultimap2, listMultimap3, listMultimap4, listMultimap5, listMultimap6, listMultimap7);
            }
        } catch (EOFException eOFException1) {
            throw new ClassFileFormatException(inputFileLocation.getQualifiedName() + " : IO error. File is probably corrupt (E)");
        } catch (AssertionFailedException assertionFailedException) {
            throw assertionFailedException;
        } catch (ZkmRuntimeException zkmRuntimeException) {
            throw new ClassFileFormatException(inputFileLocation.getQualifiedName() + " : File is probably corrupt. (F) : " + zkmRuntimeException.getMessage());
        } catch (IOException iOException2) {
            throw new ClassFileFormatException(inputFileLocation.getQualifiedName() + " : IO error");
        } finally {
            if (classFileInputStream != null) {
                try {
                    classFileInputStream.close();
                } catch (IOException iOException) {
                    throw new ZkmProcessingException("IOException on closing : " + inputFileLocation.getQualifiedName() + " : " + iOException);
                }
            }
        }
    }

    public void obfuscateMethodParameterNames() {
        for (int methodIndex = 0; methodIndex < this.methodCount; methodIndex++) {
            this.methods[methodIndex].obfuscateParameterNames();
        }
    }

    @Override
    public void updateMethodReferences(ScriptEnvironment scriptEnvironment1, ClassMemberLookup classMemberLookup1, Object object) throws ZkmException, IOException {
        boolean bl = (Boolean) object;
        this.constantPool.resolveMethodRefs(scriptEnvironment1, classMemberLookup1);

        for (int attributeIndex = 0; attributeIndex < this.attributes.length; attributeIndex++) {
            if (this.attributes[attributeIndex] instanceof ReferencingAttribute) {
                ((ReferencingAttribute) this.attributes[attributeIndex]).applyMethodRenames();
            } else if (this.attributes[attributeIndex] instanceof EnclosingMethodAttribute) {
                ((EnclosingMethodAttribute) this.attributes[attributeIndex]).updateAfterMethodRename();
            } else if (this.attributes[attributeIndex] instanceof RecordAttribute) {
                ((RecordAttribute) this.attributes[attributeIndex]).applyMethodRenames();
            }
        }

        for (int fieldIndex = 0; fieldIndex < this.fieldCount; fieldIndex++) {
            this.fields[fieldIndex].updateAttributesAfterMethodRename();
        }

        for (int methodIndex = 0; methodIndex < this.methodCount; methodIndex++) {
            this.methods[methodIndex].updateAttributesAfterMethodRename(scriptEnvironment1, bl);
        }
    }

    public void refreshMethodListNode() throws ZkmException, IOException {
        if (propertyNodes != null) {
            ((MemberListNode) propertyNodes.elementAt(2)).updateMembers(this.methods);
        }
    }

    public void obfuscateFlowForParameterChanges(
            ListMultimap listMultimap,
            CommonSuperTypeResolver commonSuperTypeResolver1,
            ClassHierarchyQuery classHierarchyQuery,
            Map map1,
            int ba,
            boolean bl,
            boolean bl1,
            ScriptEnvironment scriptEnvironment1,
            FlowObfuscationExclusions flowObfuscationExclusions,
            Random random1
    ) throws ZkmException, IOException {
        int integerValue = ba;
        if (!this.isInterface() || this.supportsJava8()) {
            if (!this.isGenerated()) {
                PrintWriter printWriter = scriptEnvironment1.getLogWriter();
                ArrayList arrayList = new ArrayList(7);
                if (flowObfuscationExclusions == null || !flowObfuscationExclusions.isClassExcluded(this)) {
                    if (this.zkmAnnotationValues != null && this.zkmAnnotationValues.hasValue("obfuscateFlow")) {
                        String valueValue = this.zkmAnnotationValues.getValue("obfuscateFlow");
                        int integerValue2 = ObfuscateOptions.parseFlowLevel(valueValue);
                        if (integerValue != integerValue2) {
                            String flowLevelName = ObfuscateOptions.getFlowLevelName(integerValue);
                            integerValue = integerValue2;
                            if (scriptEnvironment1.isVerbose()) {
                                printWriter.println(
                                        "Overriding Method Parameter Changing's Flow Obfuscation setting in class '"
                                                + AbstractExclusionSpec.formatClass(this, classHierarchyQuery, true)
                                                + "' : '"
                                                + flowLevelName
                                                + "' changed to '"
                                                + valueValue
                                                + "'"
                                );
                            }

                            if (integerValue == 0) {
                                return;
                            }
                        }
                    }

                    RankedValue[] rankedValues = new RankedValue[this.methodCount];

                    for (int i = 0; i < this.methodCount; i++) {
                        rankedValues[i] = new RankedValue(this.methods[i].getCodeSize(), this.methods[i]);
                    }

                    Arrays.sort(rankedValues);
                    int maxValue = Integer.MAX_VALUE;
                    int bh = 0;

                    for (int rankedValueIndex = rankedValues.length - 1; rankedValueIndex >= 0; rankedValueIndex += -1) {
                        MethodInfo methodInfo1 = (MethodInfo) rankedValues[rankedValueIndex].getValue();
                        if (!methodInfo1.isManufactured()
                                && !methodInfo1.isStaticInitializer()
                                && (flowObfuscationExclusions == null || !flowObfuscationExclusions.isMethodExcluded(methodInfo1))) {
                            try {
                                int be = methodInfo1.obfuscateFlow(
                                        listMultimap, commonSuperTypeResolver1, scriptEnvironment1, classHierarchyQuery, integerValue, bl, bl1, map1, true, random1
                                );
                                bh += be;
                            } catch (MethodAnalysisException methodAnalysisException) {
                            } catch (ZkmClassNotFoundException zkmClassNotFoundException) {
                            } catch (StackAnalysisException stackAnalysisException) {
                            }
                        }
                    }
                }

                if (arrayList.size() > 0) {
                    this.constantPool.appendEntries(arrayList);
                    this.setChanged();
                    this.notifyObservers(new MutableInt(4), this, null);
                }
            }
        }
    }

    public void collectFixedClassReferences(HashSet hashSet, HashSet hashSet1, HashSet hashSet2, HashSet hashSet3) throws ZkmProcessingException {
        for (int attributeIndex = 0; attributeIndex < this.attributes.length; attributeIndex++) {
            Attribute attribute = this.attributes[attributeIndex];
            if (attribute instanceof SignatureAttribute) {
                ((SignatureAttribute) attribute).collectReferencedClasses(hashSet1);
            } else if (attribute instanceof ReferencingAttribute) {
                ((ReferencingAttribute) attribute).collectReferencedMembers(hashSet, hashSet1, hashSet2, hashSet3);
            } else if (attribute instanceof InnerClassesAttribute) {
                ((InnerClassesAttribute) attribute).collectReferencedClasses(hashSet1);
            } else if (attribute instanceof PermittedSubclassesAttribute) {
                ((PermittedSubclassesAttribute) attribute).collectReferencedClasses(hashSet1);
            }
        }
    }

    public void refreshPropertyNodes() throws ZkmException, IOException {
        this.refreshFieldListNode();
        this.refreshMethodListNode();
        this.refreshConstantsNode();
    }

    public void removeFields(EnumerableMap enumerableMap) {
        ArrayList arrayList = new ArrayList();

        for (int fieldIndex = 0; fieldIndex < this.fields.length; fieldIndex++) {
            if (!enumerableMap.containsKey(this.fields[fieldIndex])) {
                arrayList.add(this.fields[fieldIndex]);
            }
        }

        this.fields = ((com.zelix.klassmaster.classfile.FieldInfo[]) (arrayList.toArray(new FieldInfo[arrayList.size()])));
        this.fieldCount = this.fields.length;
    }

    public void collectMethodCallSites(ListMultimap listMultimap) {
        for (int methodIndex = 0; methodIndex < this.methodCount; methodIndex++) {
            this.methods[methodIndex].collectMethodCallSites(listMultimap);
        }
    }

    public final void collectCallGraph(SetMultiMap setMultiMap, Set set1) {
        MethodInfo[] methodInfos = this.getMethodInfos();

        for (int methodInfoIndex = 0; methodInfoIndex < this.methodCount; methodInfoIndex++) {
            methodInfos[methodInfoIndex].collectCallGraph(setMultiMap, set1);
        }
    }

    public ResolvedMethodRef addMethodRef(AbstractMethodInfo abstractMethodInfo, List list1) {
        return this.constantPool.getOrCreateMethodRef(abstractMethodInfo, list1);
    }

    public void removeInnerClassAttributes() {
        if (super.innerClassesTrimPending || super.innerClassesRemovalPending) {
            Vector vector = new Vector(this.attributeCount);

            for (int attributeIndex = 0; attributeIndex < this.attributeCount; attributeIndex++) {
                if (!(this.attributes[attributeIndex] instanceof InnerClassesAttribute) && !(this.attributes[attributeIndex] instanceof EnclosingMethodAttribute)) {
                    vector.addElement(this.attributes[attributeIndex]);
                }
            }

            int attributeCountValue = vector.size();
            if (attributeCountValue >= this.attributeCount) {
                super.innerClassesTrimPending = false;
            } else {
                this.attributes = new Attribute[attributeCountValue];

                for (int elementIndex = 0; elementIndex < attributeCountValue; elementIndex++) {
                    this.attributes[elementIndex] = (Attribute) vector.elementAt(elementIndex);
                }

                this.attributeCount = attributeCountValue;
                super.innerClassesTrimPending = false;
            }

            super.innerClassesRemovalPending = false;
            this.clearInnerClassInfo();

            for (int methodIndex = 0; methodIndex < this.methodCount; methodIndex++) {
                this.methods[methodIndex].clearSynthetic();
            }

            for (int fieldIndex = 0; fieldIndex < this.fieldCount; fieldIndex++) {
                this.fields[fieldIndex].clearSynthetic();
            }
        }

        ClassHierarchyNode classHierarchyNode = ClassHierarchyNode.findNode(this.getClassName());
        classHierarchyNode.clearEnclosingLinks();
    }

    @Override
    public void replaceClassConstant(ResolvedClassConstant resolvedClassConstant, ResolvedClassConstant resolvedClassConstant1) {
        if (!this.isModule()) {
            if (super.thisClassConstant == resolvedClassConstant) {
                super.thisClassConstant = resolvedClassConstant1;
            }

            if (this.superClassConstant == resolvedClassConstant) {
                this.superClassConstant = resolvedClassConstant1;
            }
        }

        int interfaceIndex = 0;
        int interfaceIndexValue = interfaceIndex;

        for (ClassConstantBase[] classConstantBases = super.interfaces; interfaceIndexValue < classConstantBases.length; classConstantBases = super.interfaces) {
            if (super.interfaces[interfaceIndex] == resolvedClassConstant) {
                super.interfaces[interfaceIndex] = resolvedClassConstant1;
                break;
            }

            interfaceIndexValue = ++interfaceIndex;
        }
    }

    public ResolvedMethodRef getStringDecryptMethodRef() {
        return this.stringDecryptMethodRef;
    }

    public MethodInfo getOrCreateStaticInitializer(ClassMemberLookup classMemberLookup1) throws ZkmProcessingException {
        ArrayList arrayList = new ArrayList();
        MethodInfo methodInfo1 = this.getOrCreateStaticInitializer(arrayList, classMemberLookup1, "Method Parameter List Changing", 5);
        if (!arrayList.isEmpty()) {
            this.constantPool.appendEntries(arrayList);
        }

        return methodInfo1;
    }

    public ListMultimap collectLineNumbers(IntegerCache integerCache1) {
        ArrayList arrayList = new ArrayList(Math.max(10, (int) (this.constantPool.getEntryCount() * 0.25)));

        for (int methodIndex = 0; methodIndex < this.methodCount; methodIndex++) {
            this.methods[methodIndex].collectLineNumberEntries(arrayList);
        }

        int arrayListSize = arrayList.size();
        ListMultimap listMultimap = new ListMultimap(arrayListSize, 1);
        HashSet hashSet = ZkmUtils.createHashSet(ZkmUtils.getPrimeCapacity(arrayListSize));

        for (int elementIndex = 0; elementIndex < arrayList.size(); elementIndex++) {
            LineNumberEntry lineNumberEntry = (LineNumberEntry) arrayList.get(elementIndex);
            Integer integer = integerCache1.valueOf(lineNumberEntry.getLineNumber());
            if (hashSet.add(integer)) {
                listMultimap.addValue(integer, integer);
            }
        }

        return listMultimap;
    }

    public void trimLocalVariableTables(boolean bl, boolean bl1) {
        boolean bl2 = false;
        if (bl1 && this.isRenamed()) {
            bl2 = true;
        }

        if (bl2) {
            this.removeLocalVariableTables();
        } else {
            for (int methodIndex = 0; methodIndex < this.methodCount; methodIndex++) {
                this.methods[methodIndex].trimLocalVariableTables(bl, bl1);
            }
        }
    }

    public void removeStackMaps() {
        if (this.hasStackMaps()) {
            for (int methodIndex = 0; methodIndex < this.methodCount; methodIndex++) {
                this.methods[methodIndex].removeStackMaps();
            }

            this.setHasStackMaps(false);
        }
    }

    public MethodInfo createMethod(
            String valueArgument,
            String valueArgument2,
            ArrayList arrayList,
            int maxStack,
            int maxLocals,
            int bc,
            boolean bl,
            LocalVariableList localVariableList1,
            ExceptionHandlerSpec[] exceptionHandlerSpecs,
            String string2,
            List list1,
            InheritedMemberAnalyzer inheritedMemberAnalyzer,
            ClassMemberLookup classMemberLookup1,
            int creationKind
    ) throws ZkmProcessingException {
        ConstantPool constantPool1 = this.getClassConstantPool();
        MethodBytecode methodBytecode1 = new MethodBytecode(arrayList, localVariableList1, string2);
        ConstantUtf8 constantUtf8 = new ConstantUtf8(0, constantPool1, valueArgument);
        list1.add(constantUtf8);
        ConstantUtf8 constantUtf81 = new ConstantUtf8(0, constantPool1, valueArgument2);
        list1.add(constantUtf81);
        ConstantUtf8 constantUtf82 = new ConstantUtf8(0, constantPool1, "Code");
        list1.add(constantUtf82);
        CodeAttributeBody codeAttributeBody = new CodeAttributeBody(constantUtf82, maxStack, maxLocals, methodBytecode1, exceptionHandlerSpecs);
        Attribute[] attributes1 = new Attribute[]{codeAttributeBody};
        MethodInfo methodInfo1 = new MethodInfo(this, constantUtf8, constantUtf81, attributes1, creationKind);
        switch (bc) {
            case 1:
                methodInfo1.makePrivate();
                break;
            case 2:
                methodInfo1.makePackagePrivate();
                break;
            case 3:
                methodInfo1.makeProtected();
                break;
            case 4:
                methodInfo1.makePublic();
        }

        if (bl) {
            methodInfo1.setStatic();
        }

        this.addMethod(methodInfo1, classMemberLookup1);
        if (inheritedMemberAnalyzer != null) {
            inheritedMemberAnalyzer.addMethod(methodInfo1);
        }

        return methodInfo1;
    }

    public void collectObfuscatableReferences(
            TwoKeySetMultiMap twoKeySetMultiMap,
            ReferenceObfuscator referenceObfuscator,
            CommonSuperTypeResolver commonSuperTypeResolver1,
            ClassHierarchyQuery classHierarchyQuery,
            ScriptEnvironment scriptEnvironment1
    ) throws ZkmException, IOException {
        for (int methodIndex = 0; methodIndex < this.methodCount; methodIndex++) {
            try {
                this.methods[methodIndex].collectObfuscatableReferences(twoKeySetMultiMap, referenceObfuscator, commonSuperTypeResolver1, classHierarchyQuery);
            } catch (MethodAnalysisException methodAnalysisException) {
                scriptEnvironment1.logMessage(methodAnalysisException.getMessage() + " : " + this.getDisplayLocationName());
            } catch (StackAnalysisException stackAnalysisException) {
                throw new ZkmProcessingException(
                        "Method '"
                                + this.methods[methodIndex].getNameWithParameters((HashMap) null)
                                + "' in class '"
                                + this.getDisplayLocationName()
                                + "' appears to be invalid (7) : '"
                                + stackAnalysisException.getMessage()
                                + "'"
                );
            }
        }
    }

    public void collectClassAttributeReferences(HashSet hashSet, HashSet hashSet1, HashSet hashSet2, HashSet hashSet3) throws ZkmProcessingException {
        for (int attributeIndex = 0; attributeIndex < this.attributes.length; attributeIndex++) {
            Attribute attribute = this.attributes[attributeIndex];
            if (attribute instanceof SignatureAttribute) {
                ((SignatureAttribute) attribute).collectReferencedClasses(hashSet1);
            } else if (attribute instanceof ReferencingAttribute) {
                ((ReferencingAttribute) attribute).collectReferencedMembers(hashSet, hashSet1, hashSet2, hashSet3);
            } else if (attribute instanceof RecordAttribute) {
                ((RecordAttribute) attribute).collectReferencedMembers(hashSet, hashSet1, hashSet2, hashSet3);
            }
        }
    }

    public boolean hasLineNumberTables() {
        return this.lineNumberTablesPresent;
    }

    @Override
    public void handleObservedChange(ObservableModel observableModel1, Object object, Object object1, Object object2) throws ZkmException, IOException {
        this.setChanged();
        this.notifyObservers(object, object1, object2);
    }

    @Override
    public AbstractMethodInfo findMethod(MethodSignature methodSignature1) {
        return this.findMethodBySignature(methodSignature1);
    }

    public void setHasLineNumberTables(boolean lineNumberTablesPresent) {
        this.lineNumberTablesPresent = lineNumberTablesPresent;
    }

    public void markAsFromArchive() {
        super.fromArchive = true;
    }

    public List getSerialPersistentFieldNames(ClassMemberLookup classMemberLookup1) throws ZkmProcessingException {
        ArrayList arrayList = new ArrayList();
        if (!classMemberLookup1.hasFieldNamed(this.getClassName(), "serialPersistentFields")) {
            return arrayList;
        }

        MethodInfo methodInfo1 = classMemberLookup1.findDeclaredMethod(this, MethodSignature.STATIC_INITIALIZER);
        if (methodInfo1 == null) {
            throw new ZkmProcessingException("No '<clinit>' found.");
        }

        methodInfo1.collectSerialPersistentFields(arrayList, classMemberLookup1);
        return arrayList;
    }

    public void trimNestHostAttribute(TrimProcessor trimProcessor1) {
        boolean bl = true;
        ArrayList arrayList = new ArrayList(this.attributeCount);

        for (int attributeIndex = 0; attributeIndex < this.attributeCount; attributeIndex++) {
            if (this.attributes[attributeIndex] instanceof NestHostAttribute) {
                ProgramClass programClass2 = ClassHierarchyNode.findProgramClass(((NestHostAttribute) this.attributes[attributeIndex]).getHostClassName());
                if (programClass2 != null && trimProcessor1.isClassOrOuterExcluded(programClass2)) {
                    break;
                }

                bl = false;
            } else {
                arrayList.add(this.attributes[attributeIndex]);
            }
        }

        if (!bl) {
            int attributeCountValue = arrayList.size();
            this.attributes = ((com.zelix.klassmaster.classfile.attribute.Attribute[]) (arrayList.toArray(new Attribute[attributeCountValue])));
            this.attributeCount = attributeCountValue;
        }
    }

    public ResolvedInvokeDynamic createInvokeDynamic(String valueArgument, String valueArgument2, ResolvedMethodRef resolvedMethodRef, List list1, ConstantPool constantPool1) {
        BootstrapMethodsAttribute bootstrapMethodsAttribute1 = this.getOrCreateBootstrapMethodsAttribute(list1);
        ResolvedNameAndType resolvedNameAndType = constantPool1.createNameAndType(valueArgument, valueArgument2, list1);
        ResolvedMethodHandleConstant resolvedMethodHandleConstant = constantPool1.getOrAddMethodHandle(
                MethodHandleRefKind.REF_INVOKE_STATIC, resolvedMethodRef, list1
        );
        ConstantPoolEntry[] constantPoolEntrys = new ConstantPoolEntry[0];
        BootstrapMethodEntry bootstrapMethodEntry = bootstrapMethodsAttribute1.addEntry(resolvedMethodHandleConstant, constantPoolEntrys);
        ResolvedInvokeDynamic resolvedInvokeDynamic = new ResolvedInvokeDynamic(constantPool1, resolvedNameAndType, bootstrapMethodEntry);
        list1.add(resolvedInvokeDynamic);
        return resolvedInvokeDynamic;
    }

    public ListMultimap scrambleLineNumbers(IntegerCache integerCache1, ListMultimap listMultimap, Random random1) {
        ArrayList arrayList = new ArrayList(Math.max(10, (int) (this.constantPool.getEntryCount() * 0.25)));

        for (int methodIndex = 0; methodIndex < this.methodCount; methodIndex++) {
            this.methods[methodIndex].collectLineNumberEntries(arrayList);
        }

        int arrayListSize = arrayList.size();
        int bb = 0;
        ListMultimap listMultimap1 = new ListMultimap(arrayListSize, 2);
        ArrayList arrayList1 = new ArrayList();
        ArrayList arrayList2 = new ArrayList(arrayListSize);
        if (listMultimap != null) {
            int keyCount = listMultimap.getKeyCount();
            ListMultimap listMultimap2 = new ListMultimap(keyCount);
            ArrayList arrayList3 = new ArrayList(keyCount);
            Enumeration enumeration = listMultimap.keys();

            while (enumeration.hasMoreElements()) {
                Integer integer = (Integer) enumeration.nextElement();
                List list1 = listMultimap.getValues(integer);
                listMultimap2.appendValues(integer, list1);
                arrayList3.addAll(list1);
            }

            Collections.sort(arrayList3);
            int bh = (Integer) arrayList3.get(arrayList3.size() - 1);
            Iterator iterator2 = arrayList3.iterator();
            int bj = (Integer) iterator2.next();

            for (int integerValue = 0; integerValue < bh; integerValue++) {
                if (integerValue < bj) {
                    arrayList1.add(integerCache1.valueOf(integerValue));
                } else if (integerValue == bj && iterator2.hasNext()) {
                    bj = (Integer) iterator2.next();
                }
            }

            for (int elementIndex = 0; elementIndex < arrayList.size(); elementIndex++) {
                LineNumberEntry lineNumberEntry = (LineNumberEntry) arrayList.get(elementIndex);
                int lineNumber = lineNumberEntry.getLineNumber();
                Integer integer1 = integerCache1.valueOf(lineNumber);
                List list2 = listMultimap2.getValues(integer1);
                if (list2 != null && list2.size() > 0) {
                    Integer integer2 = (Integer) list2.get(0);
                    lineNumberEntry.setLineNumber(integer2);
                    listMultimap1.addValue(integer1, integer2);
                    int integer2Value = integer2;
                    if (integer2Value > bb) {
                        bb = integer2Value;
                    }

                    listMultimap2.removeValue(integer1, integer2);
                } else {
                    arrayList2.add(lineNumberEntry);
                }
            }

            Enumeration enumeration1 = listMultimap2.allValues();

            while (enumeration1.hasMoreElements()) {
                arrayList1.add(enumeration1.nextElement());
            }
        } else {
            arrayList2.addAll(arrayList);
        }

        ZkmUtils.shuffleList(arrayList2, random1);
        Iterator iterator = arrayList1.iterator();
        Iterator iterator1 = arrayList2.iterator();

        while (iterator1.hasNext()) {
            LineNumberEntry lineNumberEntry1 = (LineNumberEntry) iterator1.next();
            int lineNumberValue = lineNumberEntry1.getLineNumber();
            if (iterator.hasNext()) {
                Integer integer3 = (Integer) iterator.next();
                int integer3Value = integer3;
                lineNumberEntry1.setLineNumber(integer3Value);
                listMultimap1.addValue(integerCache1.valueOf(lineNumberValue), integer3);
                if (integer3Value > bb) {
                    bb = integer3Value;
                }
            } else {
                lineNumberEntry1.setLineNumber(++bb);
                listMultimap1.addValue(integerCache1.valueOf(lineNumberValue), integerCache1.valueOf(bb));
            }
        }

        return listMultimap1;
    }

    public void removeZkmAnnotations(ScriptEnvironment scriptEnvironment1) {
        if (this.zkmAnnotationValues != null) {
            RuntimeInvisibleAnnotationsAttribute runtimeInvisibleAnnotationsAttribute = this.zkmAnnotationValues.getSourceAttribute();
            ArrayList arrayList = new ArrayList(this.attributeCount);

            for (Attribute attribute : this.attributes) {
                if (attribute == runtimeInvisibleAnnotationsAttribute) {
                    runtimeInvisibleAnnotationsAttribute.removeAnnotationsOfType("com/zelix/annotation/ZKMClassLevel", scriptEnvironment1);
                    if (runtimeInvisibleAnnotationsAttribute.getAnnotationCount() > 0) {
                        arrayList.add(attribute);
                    }
                } else {
                    arrayList.add(attribute);
                }
            }

            if (arrayList.size() < this.attributeCount) {
                this.attributes = ((com.zelix.klassmaster.classfile.attribute.Attribute[]) (arrayList.toArray(new Attribute[arrayList.size()])));
                this.attributeCount = this.attributes.length;
            }
        }

        if (this.hasZkmMethodAnnotations()) {
            for (MethodInfo methodInfo1 : this.methods) {
                if (methodInfo1.hasZkmAnnotationValues()) {
                    methodInfo1.removeZkmMethodAnnotation(scriptEnvironment1);
                }
            }
        }
    }

    public void removeGenericSignatures(int ba) {
        if (ba == 1) {
            ArrayList arrayList = new ArrayList(this.attributes.length);

            for (int attributeIndex = 0; attributeIndex < this.attributes.length; attributeIndex++) {
                if (!(this.attributes[attributeIndex] instanceof SignatureAttribute)) {
                    if (this.attributes[attributeIndex] instanceof TypeAnnotationsAttribute) {
                        TypeAnnotationsAttribute typeAnnotationsAttribute = (TypeAnnotationsAttribute) this.attributes[attributeIndex];
                        typeAnnotationsAttribute.removeSignatureDependentAnnotations();
                        if (!typeAnnotationsAttribute.isEmpty()) {
                            arrayList.add(this.attributes[attributeIndex]);
                        } else if (this.attributes[attributeIndex] instanceof RecordAttribute) {
                            ((RecordAttribute) this.attributes[attributeIndex]).removeSignatureAttributes();
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

            for (int fieldIndex = 0; fieldIndex < this.fieldCount; fieldIndex++) {
                this.fields[fieldIndex].stripGenericInfo(ba);
            }

            for (int methodIndex = 0; methodIndex < this.methodCount; methodIndex++) {
                this.methods[methodIndex].removeGenericSignatures(ba);
            }
        }
    }

    public void countReferencedProgramClasses(CountingBag countingBag) {
        this.constantPool.countReferencedProgramClasses(countingBag);
    }

    public void renameClass(String classNameArgument, HashMap hashMap, HashMap hashMap1) throws ZkmException, IOException {
        String classNameValue = this.getClassName();
        this.getClassConstantPool().renameThisClass(classNameArgument);
        this.className = classNameArgument;
        super.lowerCaseClassName = this.className.toLowerCase();
        int attributeIndex = 0;

        while (true) {
            if (attributeIndex >= this.attributeCount) {
                this.setChanged();
                break;
            }

            if (this.attributes[attributeIndex] instanceof SourceFileAttribute) {
                ((SourceFileAttribute) this.attributes[attributeIndex]).sourceFileName.setValue(this.getSimpleName() + ".java");
                this.setChanged();
                break;
            }

            attributeIndex++;
        }

        this.notifyObservers(new MutableInt(0), this, null);
        hashMap.put(classNameArgument, classNameValue);
        hashMap1.put(classNameValue, classNameArgument);
        ClassHierarchyNode classHierarchyNode = ClassHierarchyNode.findNode(classNameValue);
        classHierarchyNode.renameEnclosedClassReferences(hashMap, hashMap1);
    }

    @Override
    public AbstractConstantPool getConstantPool() {
        return this.getClassConstantPool();
    }

    public void applyStaticInitParameterChange(
            Map map1,
            InheritedMemberAnalyzer inheritedMemberAnalyzer,
            ClassMemberLookup classMemberLookup1,
            ClassResolver classResolver1,
            List list1,
            MethodParameterChanger methodParameterChanger
    ) throws ZkmException, IOException {
        MethodInfo methodInfo1;
        if (!this.isVersionedVariant()) {
            methodInfo1 = classMemberLookup1.findDeclaredMethod(this, MethodSignature.STATIC_INITIALIZER);
        } else {
            methodInfo1 = this.findMethodBySignature(MethodSignature.STATIC_INITIALIZER);
        }

        if (methodInfo1 != null) {
            MethodParamChangeNode methodParamChangeNode = (MethodParamChangeNode) map1.get(methodInfo1);
            if (methodParamChangeNode != null) {
                methodParamChangeNode.buildInitInstructions(this, classMemberLookup1, classResolver1, list1, inheritedMemberAnalyzer, methodParameterChanger);
            }
        }
    }

    public void addOpaquePredicateInitialization(
            OpaquePredicateField opaquePredicateField,
            NestedMultiMap nestedMultiMap,
            CommonSuperTypeResolver commonSuperTypeResolver1,
            ClassMemberLookup classMemberLookup1,
            StaticInitCalleeAnalyzer staticInitCalleeAnalyzer1,
            Random random1
    ) throws ZkmException, IOException {
        MethodInfo methodInfo1 = classMemberLookup1.findDeclaredMethod(this, MethodSignature.STATIC_INITIALIZER);
        ArrayList arrayList = new ArrayList();
        if (methodInfo1 == null) {
            methodInfo1 = this.getOrCreateStaticInitializer(arrayList, classMemberLookup1, "Flow Obfuscation", 2);
        }

        methodInfo1.addOpaquePredicateInitialization(
                opaquePredicateField, nestedMultiMap, arrayList, commonSuperTypeResolver1, classMemberLookup1, staticInitCalleeAnalyzer1, random1
        );
        if (arrayList.size() > 0) {
            this.getClassConstantPool().appendEntries(arrayList);
        }
    }

    public void shuffleMembers(Random random1) {
        ZkmUtils.shuffleArray(this.fields, random1);
        ZkmUtils.shuffleArray(this.methods, random1);
    }

    public void assignIntegerValueLocations(
            PairMultiMap pairMultiMap,
            Map map1,
            PairMultiMap pairMultiMap1,
            SetMultiMap setMultiMap,
            Map map2,
            ResolvedFieldRef resolvedFieldRef,
            ResolvedInvokeDynamic resolvedInvokeDynamic,
            boolean bl,
            boolean bl1,
            ResolvedMethodRef resolvedMethodRef,
            ObservableHolder[] observableHolders,
            LocalVariableAllocator localVariableAllocator
    ) {
        int observableHolderIndex = 0;
        Iterator iterator = setMultiMap.entrySet().iterator();

        while (iterator.hasNext()) {
            Entry entry = (Entry) iterator.next();
            ObservableHolder observableHolder = (ObservableHolder) entry.getKey();
            observableHolder.getValue();
            EncryptedValueLocation encryptedValueLocation = (EncryptedValueLocation) map1.get(observableHolder);
            if (encryptedValueLocation == null) {
                if (resolvedFieldRef != null) {
                    if (bl) {
                        observableHolders[observableHolderIndex] = observableHolder;
                        if (resolvedInvokeDynamic != null) {
                            int index = observableHolderIndex++;
                            ResolvedInvokeDynamic resolvedInvokeDynamic1 = resolvedInvokeDynamic;
                            encryptedValueLocation = new EncryptedValueLocation(resolvedInvokeDynamic1, index);
                        } else if (resolvedMethodRef != null) {
                            if (bl1) {
                                int indexValue = observableHolderIndex++;
                                encryptedValueLocation = new EncryptedValueLocation(indexValue, resolvedMethodRef);
                            } else {
                                int indexValue2 = observableHolderIndex++;
                                ResolvedMethodRef resolvedMethodRef1 = resolvedMethodRef;
                                encryptedValueLocation = new EncryptedValueLocation(resolvedMethodRef1, indexValue2);
                            }
                        } else {
                            int indexValue3 = observableHolderIndex++;
                            ResolvedFieldRef resolvedFieldRef1 = resolvedFieldRef;
                            encryptedValueLocation = new EncryptedValueLocation(resolvedFieldRef1, indexValue3);
                        }
                    } else {
                        encryptedValueLocation = new EncryptedValueLocation(resolvedFieldRef, -1);
                    }

                    map1.put(observableHolder, encryptedValueLocation);
                } else {
                    if (bl) {
                        observableHolders[observableHolderIndex] = observableHolder;
                    }

                    int value = localVariableAllocator.getValue();
                    int indexValue4 = bl ? observableHolderIndex++ : -1;
                    int valueValue = value;
                    encryptedValueLocation = new EncryptedValueLocation(valueValue, indexValue4);
                    map1.put(observableHolder, encryptedValueLocation);
                }
            }
        }

        iterator = pairMultiMap1.entrySet().iterator();

        while (iterator.hasNext()) {
            Entry entry1 = (Entry) iterator.next();
            MethodInfo methodInfo1 = (MethodInfo) entry1.getKey();
            Iterator iterator1 = ((List) entry1.getValue()).iterator();

            while (iterator1.hasNext()) {
                ObjectPair objectPair = (ObjectPair) iterator1.next();
                ConstantInteger constantInteger = (ConstantInteger) objectPair.getFirst();
                RankedValue rankedValue = (RankedValue) objectPair.getSecond();
                ObservableHolder observableHolder1 = (ObservableHolder) map2.get(constantInteger);
                EncryptedValueLocation encryptedValueLocation1 = (EncryptedValueLocation) map1.get(observableHolder1);
                pairMultiMap.addPair(methodInfo1, rankedValue, encryptedValueLocation1);
            }
        }
    }

    public Set collectExcludedLongConstants(LongEncryptionExclusionHandler longEncryptionExclusionHandler) {
        HashSet hashSet = ZkmUtils.createHashSet();
        if (longEncryptionExclusionHandler != null && longEncryptionExclusionHandler.hasExcludedFields(this)) {
            Enumeration enumeration = longEncryptionExclusionHandler.getExcludedFieldsOf(this);

            while (enumeration.hasMoreElements()) {
                FieldInfo fieldInfo = (FieldInfo) enumeration.nextElement();
                ConstantValueAttribute constantValueAttribute = fieldInfo.getConstantValueAttribute();
                if (constantValueAttribute != null) {
                    ConstantLong constantLong = constantValueAttribute.getLongConstant();
                    if (constantLong != null) {
                        hashSet.add(constantLong);
                    }
                }
            }

            for (int methodIndex = 0; methodIndex < this.methodCount; methodIndex++) {
                MethodInfo methodInfo2 = this.methods[methodIndex];
                if (methodInfo2.hasLongConstants() && (methodInfo2.isConstructor() || methodInfo2.isStaticInitializer())) {
                    methodInfo2.collectExcludedFieldLongs(hashSet, longEncryptionExclusionHandler);
                }
            }
        }

        if (longEncryptionExclusionHandler != null && longEncryptionExclusionHandler.hasExcludedMethods(this)) {
            Enumeration enumeration1 = longEncryptionExclusionHandler.getExcludedMethodsOf(this);

            while (enumeration1.hasMoreElements()) {
                MethodInfo methodInfo1 = (MethodInfo) enumeration1.nextElement();
                if (methodInfo1.hasLongConstants()) {
                    methodInfo1.collectLongConstants(hashSet);
                }
            }
        }

        return hashSet;
    }

    public void markHasZkmMethodAnnotations() {
        this.zkmMethodAnnotationsPresent = true;
    }

    public void changeMethodParameterLists(
            Set set1,
            ChangeLogMapping changeLogMapping1,
            MethodParameterChangeSet methodParameterChangeSet,
            ParameterListGenerator parameterListGenerator,
            MethodOverrideAnalyzer methodOverrideAnalyzer,
            SetMultiMap setMultiMap,
            boolean bl,
            boolean bl1,
            ClassHierarchyNode classHierarchyNode,
            Map map1
    ) throws ZkmException, IOException {
        HashMap hashMap = ZkmUtils.createHashMap();
        HashMap hashMap1 = ZkmUtils.createHashMap();
        ArrayList arrayList = new ArrayList(this.methodCount);

        for (int methodIndex = 0; methodIndex < this.methodCount; methodIndex++) {
            MethodInfo methodInfo1 = this.methods[methodIndex];
            arrayList.add(methodInfo1);
            if (set1 == null || !set1.contains(methodInfo1)) {
                MethodSignature methodSignature1 = methodInfo1.getSignature();
                FieldNameTypeSignature fieldNameTypeSignature = methodSignature1.getNameTypeSignature();
                hashMap.put(methodSignature1, methodInfo1);
                hashMap1.put(fieldNameTypeSignature, methodInfo1);
                setMultiMap.addValue(bl ? methodSignature1 : fieldNameTypeSignature, methodInfo1);
            }
        }

        if (bl1 || HiddenOptionFlags.RANDOMIZE_OBFUSCATION) {
            ZkmUtils.shuffleList(arrayList, ZkmUtils.createRandom(512));
        }

        if (changeLogMapping1 != null && changeLogMapping1.hasParameterChangeData()) {
            Iterator iterator = arrayList.iterator();

            while (iterator.hasNext()) {
                MethodInfo methodInfo2 = (MethodInfo) iterator.next();
                AbstractMethodInfo abstractMethodInfo = methodOverrideAnalyzer.getRootMethod(methodInfo2);
                if (changeLogMapping1.hasAddedParameters(abstractMethodInfo)) {
                    this.getClassName();
                    boolean bl7 = this.isInterface();
                    Map map2 = map1;
                    boolean includeReturnTypeInConflicts = bl;
                    HashMap hashMap2 = hashMap1;
                    HashMap hashMap3 = hashMap;
                    boolean reserveOverrideGroup = bl7;
                    ClassHierarchyNode classHierarchyNode1 = classHierarchyNode;
                    SetMultiMap setMultiMap1 = setMultiMap;
                    MethodOverrideAnalyzer methodOverrideAnalyzer1 = methodOverrideAnalyzer;
                    ParameterListGenerator parameterListGenerator1 = parameterListGenerator;
                    MethodParameterChangeSet methodParameterChangeSet1 = methodParameterChangeSet;
                    ChangeLogMapping changeLogMapping2 = changeLogMapping1;
                    methodInfo2.changeParameterList(
                            changeLogMapping2,
                            methodParameterChangeSet1,
                            parameterListGenerator1,
                            methodOverrideAnalyzer1,
                            setMultiMap1,
                            classHierarchyNode1,
                            reserveOverrideGroup,
                            hashMap3,
                            hashMap2,
                            includeReturnTypeInConflicts,
                            map2
                    );
                    iterator.remove();
                }
            }
        }

        Iterator iterator1 = arrayList.iterator();

        while (iterator1.hasNext()) {
            MethodInfo methodInfo3 = (MethodInfo) iterator1.next();
            if (set1 != null && set1.contains(methodInfo3)) {
                this.getClassName();
                boolean bl6 = this.isInterface();
                Map map3 = map1;
                boolean includeReturnTypeInConflictsValue = bl;
                HashMap hashMap4 = hashMap1;
                HashMap hashMap5 = hashMap;
                boolean reserveOverrideGroupValue = bl6;
                ClassHierarchyNode classHierarchyNode2 = classHierarchyNode;
                SetMultiMap setMultiMap2 = setMultiMap;
                MethodOverrideAnalyzer methodOverrideAnalyzer2 = methodOverrideAnalyzer;
                ParameterListGenerator parameterListGenerator2 = parameterListGenerator;
                MethodParameterChangeSet methodParameterChangeSet2 = methodParameterChangeSet;
                ChangeLogMapping changeLogMapping3 = changeLogMapping1;
                methodInfo3.changeParameterList(
                        changeLogMapping3,
                        methodParameterChangeSet2,
                        parameterListGenerator2,
                        methodOverrideAnalyzer2,
                        setMultiMap2,
                        classHierarchyNode2,
                        reserveOverrideGroupValue,
                        hashMap5,
                        hashMap4,
                        includeReturnTypeInConflictsValue,
                        map3
                );
            }
        }
    }

    @Override
    public int getMethodCount() {
        return this.methods.length;
    }

    public MethodInfo getOrCreateStaticInitializer(List list1, ClassMemberLookup classMemberLookup1, String string, Integer integer) throws ZkmProcessingException {
        MethodInfo methodInfo1 = classMemberLookup1.findDeclaredMethod(this, MethodSignature.STATIC_INITIALIZER);
        if (methodInfo1 == null) {
            ConstantUtf8 constantUtf8 = new ConstantUtf8(0, this.constantPool, "Code");
            list1.add(constantUtf8);
            LocalVariableList localVariableList1 = new LocalVariableList(true, MethodSignature.STATIC_INITIALIZER.getDescriptor(), 0);
            ArrayList arrayList = new ArrayList();
            arrayList.add(SimpleInstruction.forOpcode(177));
            MethodBytecode methodBytecode1 = new MethodBytecode(arrayList, localVariableList1, string);
            CodeAttributeBody codeAttributeBody = new CodeAttributeBody(constantUtf8, 0, 0, methodBytecode1, new ExceptionHandlerSpec[0]);
            ConstantUtf8 constantUtf81 = new ConstantUtf8(0, this.constantPool, MethodSignature.STATIC_INITIALIZER.getName());
            list1.add(constantUtf81);
            ConstantUtf8 constantUtf82 = new ConstantUtf8(0, this.constantPool, MethodSignature.STATIC_INITIALIZER.getDescriptor());
            list1.add(constantUtf82);
            Attribute[] attributes1 = new Attribute[]{codeAttributeBody};
            methodInfo1 = new MethodInfo(this, constantUtf81, constantUtf82, attributes1, integer);
            methodInfo1.makePackagePrivate();
            methodInfo1.setStatic();
            this.addMethod(methodInfo1, classMemberLookup1);
        }

        return methodInfo1;
    }

    public void refreshConstantsNode() throws ZkmException, IOException {
        if (propertyNodes != null) {
            ClassConstantsNode classConstantsNode = (ClassConstantsNode) propertyNodes.elementAt(3);
            NumericConstantEntry[] numericConstantEntrys = this.constantPool.getLiteralConstants();
            classConstantsNode.updateConstants(numericConstantEntrys);
        }
    }

    public void collectStringConstantFields(PairMultiMap pairMultiMap, ListMultimap listMultimap, Set set1) {
        for (int fieldIndex = 0; fieldIndex < this.fields.length; fieldIndex++) {
            FieldInfo fieldInfo = this.fields[fieldIndex];
            if (fieldInfo.isStringType()) {
                ConstantValueAttribute constantValueAttribute = fieldInfo.getConstantValueAttribute();
                if (constantValueAttribute != null) {
                    ResolvedStringConstant resolvedStringConstant = constantValueAttribute.getStringConstant();
                    if (resolvedStringConstant.getValueUtf8().getLength() >= 2 && !set1.contains(resolvedStringConstant)) {
                        if (fieldInfo.isStatic()) {
                            pairMultiMap.addPair(this, resolvedStringConstant, fieldInfo);
                        } else {
                            listMultimap.addValue(this, fieldInfo);
                        }
                    }
                }
            }
        }
    }

    public void collectParameterChangeFlowCandidates(
            Set set1, FlowObfuscationExclusions flowObfuscationExclusions, MethodParameterExclusions methodParameterExclusions
    ) {
        for (int methodIndex = 0; methodIndex < this.methodCount; methodIndex++) {
            MethodInfo methodInfo1 = this.methods[methodIndex];
            if (!methodInfo1.isManufactured()
                    && !methodInfo1.isStaticInitializer()
                    && (flowObfuscationExclusions == null || !flowObfuscationExclusions.isMethodExcluded(methodInfo1))
                    && (methodParameterExclusions == null || methodParameterExclusions.isMethodExcluded(methodInfo1))
                    && methodInfo1.hasConditionalBranches()) {
                set1.add(methodInfo1);
            }
        }
    }

    public void writeClassFile(
            DataOutputStream dataOutputStream,
            CommonSuperTypeResolver commonSuperTypeResolver1,
            ClassHierarchyQuery classHierarchyQuery,
            ScriptEnvironment scriptEnvironment1
    ) throws ZkmException, IOException {
        this.statusHolder.setValue("Saving " + this.getDottedClassName());
        int ba = 0;

        RemappedConstantPool remappedConstantPool;
        Map map1;
        boolean bl;
        do {
            HashSet hashSet = ZkmUtils.createHashSet(ZkmUtils.getPrimeCapacity(this.getMethodCount()));
            this.constantPool.renumberBootstrapMethods();
            UsedConstantsCollector usedConstantsCollector = new UsedConstantsCollector(this.constantPool.getEntryCount() / 4, true);
            UsedConstantsCollector usedConstantsCollector1 = usedConstantsCollector;
            this.collectUsedConstants('\u0000', 1103830477, usedConstantsCollector1, '鞍');
            remappedConstantPool = this.constantPool.createRemappedPool(usedConstantsCollector, hashSet);
            if (remappedConstantPool.getEntryCount() > 65535) {
                throw new ZkmProcessingException(
                        "Class '" + this.getLocationName() + "' constant pool size exceeds " + 65535 + ". (" + remappedConstantPool.getEntryCount() + ")"
                );
            }

            map1 = remappedConstantPool.getReplacementMap();
            ZkmUtils.createHashSet();
            bl = false;

            for (MethodInfo methodInfo1 : this.methods) {
                MethodBytecode methodBytecode1 = methodInfo1.getBytecode();
                if (methodBytecode1 != null
                        && (methodBytecode1.isModified() || hashSet.contains(methodBytecode1) || HiddenOptionFlags.REBUILD_ALL_STACK_MAPS && ba == 0)) {
                    boolean bl1 = this.regenerateStackMap(
                            methodInfo1,
                            commonSuperTypeResolver1,
                            classHierarchyQuery,
                            this.constantPool,
                            map1,
                            !scriptEnvironment1.isStackMapUpdateSkipped(),
                            scriptEnvironment1.hasMicroEditionClasses()
                    );
                    bl = bl || bl1;
                }
            }

            if (bl) {
                this.constantPool.renumberEntries();
            }
        } while (bl && ba++ < 3);

        if (bl) {
            throw new ZkmProcessingException("Incompatible stack map change in class '" + this.getDisplayLocationName() + "'");
        }

        dataOutputStream.writeInt(-889275714);
        dataOutputStream.writeShort(super.minorVersion);
        dataOutputStream.writeShort(this.majorVersion);
        remappedConstantPool.writeEntries(dataOutputStream);
        dataOutputStream.writeShort(this.accessFlags.getFlags());
        ConstantPoolEntry constantPoolEntry1 = (ConstantPoolEntry) map1.get(super.thisClassConstant);
        if (constantPoolEntry1 != null) {
            dataOutputStream.writeShort(constantPoolEntry1.getIndex());
        } else {
            dataOutputStream.writeShort(super.thisClassConstant.getIndex());
        }

        DataOutputStream dataOutputStream2;
        int interfaceCountValue;
        if (this.superClassConstant != null) {
            ConstantPoolEntry constantPoolEntry2 = (ConstantPoolEntry) map1.get(this.superClassConstant);
            if (constantPoolEntry2 != null) {
                dataOutputStream.writeShort(constantPoolEntry2.getIndex());
            } else {
                dataOutputStream.writeShort(this.isModule() ? 0 : this.superClassConstant.getIndex());
            }

            dataOutputStream2 = dataOutputStream;
            interfaceCountValue = super.interfaceCount;
        } else {
            dataOutputStream.writeShort(0);
            dataOutputStream2 = dataOutputStream;
            interfaceCountValue = super.interfaceCount;
        }

        dataOutputStream2.writeShort(interfaceCountValue);
        int interfaceIndex = 0;
        int bf = 0;

        for (int interfaceCountValue2 = super.interfaceCount; bf < interfaceCountValue2; interfaceCountValue2 = super.interfaceCount) {
            ConstantPoolEntry constantPoolEntry = (ConstantPoolEntry) map1.get(super.interfaces[interfaceIndex]);
            if (constantPoolEntry != null) {
                dataOutputStream.writeShort(constantPoolEntry.getIndex());
            } else {
                dataOutputStream.writeShort(super.interfaces[interfaceIndex].getIndex());
            }

            bf = ++interfaceIndex;
        }

        if (this.fieldCount > 65535) {
            String string = "Class '" + ClassFileBase.toDottedName(this.getClassName()) + "' field count exceeds " + 65535 + ". (" + this.fieldCount + ")";
            throw new ZkmProcessingException(string);
        }

        dataOutputStream.writeShort(this.fieldCount);

        for (int fieldIndex = 0; fieldIndex < this.fieldCount; fieldIndex++) {
            this.fields[fieldIndex].write(dataOutputStream, map1, scriptEnvironment1);
        }

        if (this.methodCount > 65535) {
            String string1 = "Class '" + ClassFileBase.toDottedName(this.getClassName()) + "' method count exceeds " + 65535 + ". (" + this.methodCount + ")";
            throw new ZkmProcessingException(string1);
        }

        dataOutputStream.writeShort(this.methodCount);

        for (int methodIndex = 0; methodIndex < this.methodCount; methodIndex++) {
            this.methods[methodIndex].write(dataOutputStream, map1, scriptEnvironment1);
        }

        dataOutputStream.writeShort(this.attributeCount);

        for (int attributeIndex = 0; attributeIndex < this.attributeCount; attributeIndex++) {
            if (this.attributes[attributeIndex] instanceof UnknownAttribute && this.attributes[attributeIndex].getLength() > 0) {
                UnknownAttribute unknownAttribute = (UnknownAttribute) this.attributes[attributeIndex];
                if (unknownAttribute.isCommonAcrossClasses()) {
                    scriptEnvironment1.logWarning(
                            "Classes contain unknown attribute '"
                                    + unknownAttribute.getAttributeName()
                                    + "'. The integrity of this attribute may be affected by obfuscation."
                    );
                } else {
                    scriptEnvironment1.logWarning(
                            "Class '"
                                    + this.getLocationName()
                                    + "' contains unknown attribute '"
                                    + unknownAttribute.getAttributeName()
                                    + "'. The integrity of this attribute may be affected by obfuscation. Consider using the Trim function to delete it."
                    );
                }
            }

            Attribute attribute = this.attributes[attributeIndex];
            Map map2 = map1;
            ScriptEnvironment scriptEnvironment2 = scriptEnvironment1;
            Map map3 = map2;
            DataOutputStream dataOutputStream1 = dataOutputStream;
            attribute.writeRemapped(dataOutputStream1, map3, scriptEnvironment2);
        }

        dataOutputStream.flush();
        this.constantPool.renumberEntries();
        this.statusHolder.setValue(" ");
    }

    public int replaceIntegerConstant(int valueArgument) {
        return this.constantPool.replaceIntegerValue(valueArgument);
    }

    public void obfuscateLocalVariableNames() {
        for (int methodIndex = 0; methodIndex < this.methodCount; methodIndex++) {
            this.methods[methodIndex].obfuscateLocalVariableNames();
        }
    }

    public void applyOpaquePredicateFlow(
            Set set1,
            Set set2,
            OpaquePredicateField opaquePredicateField,
            OpaquePredicateField opaquePredicateField1,
            NestedMultiMap nestedMultiMap,
            ListMultimap listMultimap,
            Map map1,
            ClassMemberLookup classMemberLookup1,
            CommonSuperTypeResolver commonSuperTypeResolver1,
            ClassResolver classResolver1,
            Random random1
    ) throws ZkmException, IOException {
        if (!this.isInterface() || this.supportsJava8()) {
            ArrayList arrayList = new ArrayList(7);

            for (int methodIndex = 0; methodIndex < this.methodCount; methodIndex++) {
                MethodInfo methodInfo1 = this.methods[methodIndex];

                try {
                    methodInfo1.applyOpaquePredicateFlow(
                            set1,
                            set2,
                            opaquePredicateField,
                            opaquePredicateField1,
                            nestedMultiMap,
                            arrayList,
                            this.constantPool,
                            listMultimap,
                            map1,
                            classMemberLookup1,
                            commonSuperTypeResolver1,
                            classResolver1,
                            random1
                    );
                } catch (StackAnalysisException stackAnalysisException) {
                    throw new ZkmProcessingException(
                            "Method '"
                                    + this.methods[methodIndex].getOriginalNameWithParameters()
                                    + "' in class '"
                                    + this.getDisplayLocationName()
                                    + "' appears to be invalid (8) : '"
                                    + stackAnalysisException.getMessage()
                                    + "'"
                    );
                }
            }

            if (arrayList.size() > 0) {
                this.constantPool.appendEntries(arrayList);
                this.setChanged();
                this.notifyObservers(new MutableInt(4), this, null);
            }
        }
    }

    public Enumeration enumerateInheritedReferencedFields() {
        return this.inheritedFieldReferrers == null ? null : this.inheritedFieldReferrers.keys();
    }

    public boolean hasStringDecryptMethodRef() {
        return this.stringDecryptMethodRef != null;
    }

    public FieldInfo findFieldBySignature(FieldSignature fieldSignature) {
        return (FieldInfo) super.findField(fieldSignature);
    }

    public void indexMemberReferences(NestedMultiMap nestedMultiMap, NestedMultiMap nestedMultiMap1, NestedMultiMap nestedMultiMap2, IntegerCache integerCache1) {
        for (int methodIndex = 0; methodIndex < this.methods.length; methodIndex++) {
            this.methods[methodIndex].indexMemberReferences(nestedMultiMap, nestedMultiMap1, nestedMultiMap2, integerCache1);
        }
    }

    public void reportReflectionApiCalls(
            Set set1,
            Map map1,
            NestedMultiMap nestedMultiMap,
            ClasspathClassLoader classpathClassLoader1,
            NestedMultiMap nestedMultiMap1,
            NestedMultiMap nestedMultiMap2,
            PrintWriter printWriter,
            MutableInt mutableInt,
            ClassMemberLookup classMemberLookup1,
            CommonSuperTypeResolver commonSuperTypeResolver1,
            String string,
            Map map2,
            Map map3,
            Map map4,
            HashMap hashMap,
            Map map5,
            TwoKeyMap twoKeyMap,
            ListMultimap listMultimap,
            TwoKeyMap twoKeyMap1,
            TwoKeyMap twoKeyMap2,
            TwoKeyMap twoKeyMap3,
            Set set2,
            ListMultimap listMultimap1,
            MessageReporter messageReporter1,
            List list1,
            boolean bl
    ) throws ZkmException, IOException {
        if (set1.contains(this)) {
            boolean bl1 = false;

            for (int methodIndex = 0; methodIndex < this.methodCount; methodIndex++) {
                try {
                    ListMultimap listMultimap2 = this.methods[methodIndex]
                            .analyzeReflectionCalls(
                                    map1,
                                    classpathClassLoader1,
                                    nestedMultiMap,
                                    nestedMultiMap1,
                                    nestedMultiMap2,
                                    commonSuperTypeResolver1,
                                    map2,
                                    map3,
                                    map4,
                                    hashMap,
                                    classMemberLookup1,
                                    map5,
                                    twoKeyMap,
                                    listMultimap,
                                    twoKeyMap1,
                                    twoKeyMap2,
                                    twoKeyMap3,
                                    set2,
                                    listMultimap1,
                                    list1,
                                    bl
                            );
                    if (listMultimap2 != null && listMultimap2.getKeyCount() > 0) {
                        if (!bl1) {
                            printWriter.write(
                                    "In class "
                                            + this.getDottedClassName()
                                            + (this.isVersionedVariant() ? " (multi-release version " + this.getReleaseVersion() + ")" : "")
                                            + HiddenOptionFlags.LINE_SEPARATOR
                            );
                            bl1 = true;
                        }

                        printWriter.write(string + "in method " + this.methods[methodIndex].buildDeclaration(false) + HiddenOptionFlags.LINE_SEPARATOR);
                        Enumeration enumeration = listMultimap2.keys();

                        while (enumeration.hasMoreElements()) {
                            ResolvedMethodRef resolvedMethodRef = (ResolvedMethodRef) enumeration.nextElement();
                            List list2 = listMultimap2.getValues(resolvedMethodRef);

                            for (int elementIndex = 0; elementIndex < list2.size(); elementIndex++) {
                                String string1;
                                if (bl) {
                                    string1 = " " + (String) list2.get(elementIndex);
                                } else {
                                    string1 = "";
                                }

                                printWriter.write(string + string + resolvedMethodRef.getDisplayString() + string1 + HiddenOptionFlags.LINE_SEPARATOR);
                                mutableInt.incrementAndGet();
                            }
                        }
                    }
                } catch (StackAnalysisException stackAnalysisException) {
                    throw new ZkmProcessingException(
                            "Method '"
                                    + this.methods[methodIndex].getNameWithParameters((HashMap) null)
                                    + "' in class '"
                                    + this.getDisplayLocationName()
                                    + "' appears to be invalid (1) : '"
                                    + stackAnalysisException.getMessage()
                                    + "'"
                    );
                } catch (AssertionFailedException assertionFailedException) {
                    throw assertionFailedException;
                } catch (Throwable throwable) {
                    messageReporter1.reportErrorWithDetail(
                            "ERROR: ",
                            "Possible invalid class '" + this.getLocationName() + "'",
                            "Unexpected error while analyzing method '"
                                    + this.methods[methodIndex].getNameWithParameters((HashMap) null)
                                    + "' in class '"
                                    + this.getDottedClassName()
                                    + "' ("
                                    + this.getLocationName()
                                    + "). The class file may be invalid : "
                                    + throwable.getClass().getName()
                                    + " '"
                                    + throwable.getMessage()
                                    + "'"
                    );
                }
            }
        }
    }

    public ResolvedMethodRefConstant addMethodRefConstant(AbstractMethodInfo abstractMethodInfo, List list1) {
        return this.constantPool.getOrCreateMethodRefConstant(abstractMethodInfo, list1);
    }

    public FieldInfo[] getFieldInfos() {
        return this.fields;
    }

    public void checkConstantPoolSize(String string, String string1) throws ZkmProcessingException {
        int ba = this.constantPool.computeDedupedEntryCount();
        if (ba > 65535) {
            throw new ZkmProcessingException(
                    string
                            + " : ConstantPool length too large in class '"
                            + this.getDisplayLocationName()
                            + "' : "
                            + ba
                            + " > "
                            + 65535
                            + " : ("
                            + this.constantPool.getEntryCount()
                            + ") : "
                            + string1
            );
        }
    }

    public String getSourceFileName() {
        for (int attributeIndex = 0; attributeIndex < this.attributeCount; attributeIndex++) {
            if (this.attributes[attributeIndex] instanceof SourceFileAttribute) {
                return ((SourceFileAttribute) this.attributes[attributeIndex]).sourceFileName.getValue();
            }
        }

        return null;
    }

    public void propagateDebugAttributeFlags(Map map1) {
        for (int methodIndex = 0; methodIndex < this.methodCount; methodIndex++) {
            MethodInfo methodInfo1 = this.methods[methodIndex];
            AbstractMethodInfo abstractMethodInfo = (AbstractMethodInfo) ZkmUtils.mapOrSelf(methodInfo1, map1);
            if (methodInfo1.hasLocalVariableTable() && abstractMethodInfo.isProgramMember()) {
                abstractMethodInfo.setHadLocalVariableTable(true);
                methodInfo1.setHadLocalVariableTable(true);
            }

            if (methodInfo1.hasMethodParametersAttribute() && abstractMethodInfo.isProgramMember()) {
                abstractMethodInfo.setHadMethodParameters();
                methodInfo1.setHadMethodParameters();
            }
        }
    }

    public void collectEncryptableStrings(
            MultiMapTable multiMapTable,
            PairMultiMap pairMultiMap,
            ListMultimap listMultimap,
            StringEncryptionExclusionSpec stringEncryptionExclusionSpec,
            boolean bl,
            CommonSuperTypeResolver commonSuperTypeResolver1,
            ClassHierarchyQuery classHierarchyQuery,
            ScriptEnvironment scriptEnvironment1
    ) throws ZkmException, IOException {
        Set set1 = this.collectExcludedStringConstants(stringEncryptionExclusionSpec, commonSuperTypeResolver1, classHierarchyQuery, scriptEnvironment1);
        this.collectStringConstantFields(pairMultiMap, listMultimap, set1);

        for (int methodIndex = 0; methodIndex < this.methods.length; methodIndex++) {
            if (bl || !this.methods[methodIndex].isManufactured()) {
                this.methods[methodIndex].collectEncryptableStrings(multiMapTable, set1);
            }
        }
    }

    @Override
    public ConstantPoolEntry getConstantPoolEntry(int entryIndex) {
        return this.constantPool.getConstantPoolEntry(entryIndex);
    }

    public void renamePackagePrefix(String string, String prefix, HashMap hashMap, HashMap hashMap1) throws ZkmException, IOException {
        String classNameValue = this.getClassName();
        if (classNameValue.startsWith(prefix)) {
            String string3 = classNameValue.substring(prefix.length());
            String classNameValue2 = string + string3;
            this.renameClass(classNameValue2, hashMap, hashMap1);
        }
    }

    public void collectAttributeReferencedClasses(Set set1) {
        for (int attributeIndex = 0; attributeIndex < this.attributeCount; attributeIndex++) {
            if (this.attributes[attributeIndex] instanceof ReferencingAttribute) {
                ((ReferencingAttribute) this.attributes[attributeIndex]).collectReferencedClasses(set1);
            }
        }

        for (int methodIndex = 0; methodIndex < this.methodCount; methodIndex++) {
            this.methods[methodIndex].collectAttributeReferences(set1);
        }

        for (int fieldIndex = 0; fieldIndex < this.fieldCount; fieldIndex++) {
            this.fields[fieldIndex].collectAttributeReferences(set1);
        }
    }

    public Set collectExcludedIntegerConstants(IntegerEncryptionExclusions integerEncryptionExclusions) {
        HashSet hashSet = ZkmUtils.createHashSet();
        if (integerEncryptionExclusions != null && integerEncryptionExclusions.hasExcludedFields(this)) {
            Enumeration enumeration = integerEncryptionExclusions.getExcludedFields(this);

            while (enumeration.hasMoreElements()) {
                FieldInfo fieldInfo = (FieldInfo) enumeration.nextElement();
                ConstantValueAttribute constantValueAttribute = fieldInfo.getConstantValueAttribute();
                if (constantValueAttribute != null) {
                    ConstantInteger constantInteger = constantValueAttribute.getIntegerConstant();
                    if (constantInteger != null) {
                        hashSet.add(constantInteger);
                    }
                }
            }

            for (int methodIndex = 0; methodIndex < this.methodCount; methodIndex++) {
                MethodInfo methodInfo2 = this.methods[methodIndex];
                if (methodInfo2.hasIntegerConstants() && (methodInfo2.isConstructor() || methodInfo2.isStaticInitializer())) {
                    methodInfo2.collectExcludedFieldIntegers(hashSet, integerEncryptionExclusions);
                }
            }
        }

        if (integerEncryptionExclusions != null && integerEncryptionExclusions.hasExcludedMethods(this)) {
            Enumeration enumeration1 = integerEncryptionExclusions.getExcludedMethods(this);

            while (enumeration1.hasMoreElements()) {
                MethodInfo methodInfo1 = (MethodInfo) enumeration1.nextElement();
                if (methodInfo1.hasIntegerConstants()) {
                    methodInfo1.collectIntegerConstants(hashSet);
                }
            }
        }

        return hashSet;
    }

    public FieldInfo addSerialVersionUidField(long valueArgument, InheritedMemberAnalyzer inheritedMemberAnalyzer, ClassMemberLookup classMemberLookup1) throws ZkmException, IOException {
        ArrayList arrayList = new ArrayList(4);
        ConstantUtf8 constantUtf8 = new ConstantUtf8(0, this.constantPool, "serialVersionUID");
        arrayList.add(constantUtf8);
        ConstantUtf8 constantUtf81 = new ConstantUtf8(0, this.constantPool, "J");
        arrayList.add(constantUtf81);
        ConstantUtf8 constantUtf82 = new ConstantUtf8(0, this.constantPool, "ConstantValue");
        arrayList.add(constantUtf82);
        ConstantLong constantLong = new ConstantLong(this.constantPool, valueArgument);
        arrayList.add(constantLong);
        ConstantValueAttribute constantValueAttribute = new ConstantValueAttribute(this, constantUtf82, constantLong);
        Attribute[] attributes1 = new Attribute[]{constantValueAttribute};
        FieldInfo fieldInfo = new FieldInfo(this, constantUtf8, constantUtf81, attributes1, 7);
        fieldInfo.setStatic();
        fieldInfo.setFinal();
        fieldInfo.makePackagePrivate();
        this.addField(fieldInfo, classMemberLookup1);
        if (inheritedMemberAnalyzer != null) {
            inheritedMemberAnalyzer.addField(fieldInfo);
        }

        this.constantPool.appendEntries(arrayList);
        this.setChanged();
        this.notifyObservers(new MutableInt(4), this, null);
        return fieldInfo;
    }

    public void emitSingleIntDecryption(
            LocalVariableList localVariableList1,
            List list1,
            ResolvedFieldRef resolvedFieldRef,
            int ba,
            LabelInstruction labelInstruction,
            ListMultimap listMultimap,
            MutableInt mutableInt,
            Map map1,
            EncryptedValueLocation encryptedValueLocation,
            IntegerConstantEncryptor integerConstantEncryptor,
            MethodInfo methodInfo1,
            LocalVariableAllocator localVariableAllocator,
            ObservableHolder observableHolder,
            Long long1,
            LocalVariableIndex localVariableIndex1,
            Map map2,
            ObservableHolder observableHolder1,
            boolean bl,
            MethodParameterChanger methodParameterChanger,
            List list2,
            ClassMemberLookup classMemberLookup1,
            ClassResolver classResolver1
    ) throws ZkmException, IOException {
        LocalVariableIndex localVariableIndex2 = localVariableIndex1;
        if (long1 != null) {
            if (localVariableIndex2 == null) {
                localVariableIndex2 = methodParameterChanger.emitKeyLocalInit(
                        long1, localVariableAllocator, list1, methodInfo1, map2, this.getClassConstantPool(), list2
                );
            }

            if (bl) {
                int andIncrement = localVariableAllocator.getAndIncrement();
                integerConstantEncryptor.emitDesCipherInit(
                        localVariableList1, list1, andIncrement, localVariableIndex2, localVariableAllocator, list2, classMemberLookup1, classResolver1
                );
                observableHolder.setValue(integerCache.valueOf(andIncrement));
            }
        } else if (integerConstantEncryptor.hasClassKey()) {
            Instruction.appendLongConstant(integerConstantEncryptor.getClassKey(), list1, this.constantPool, list2);
            int value = localVariableAllocator.getValue();
            localVariableAllocator.addAndGet(2);
            list1.add(Instruction.createLongStore(value, localVariableList1, 11));
            observableHolder1.setValue(integerCache.valueOf(value));
        }

        ConstantLong constantLong = this.constantPool.getOrAddLongConstant(-1L, list2);
        long bd = integerConstantEncryptor.toRandomizedLong(ba);
        long valueValue;
        if (bl) {
            valueValue = integerConstantEncryptor.desEncrypt(bd, long1);
        } else if (long1 != null) {
            valueValue = bd ^ long1;
        } else {
            valueValue = bd ^ integerConstantEncryptor.getClassKey();
        }

        constantLong.setValue(valueValue);
        integerConstantEncryptor.emitEncryptedValueSlot(
                localVariableList1, list1, resolvedFieldRef, constantLong, encryptedValueLocation, labelInstruction, mutableInt, listMultimap
        );
        if (!map1.isEmpty()) {
            ArrayList arrayList = new ArrayList();
            if (resolvedFieldRef != null) {
                arrayList.add(new ConstantRefInstruction(178, resolvedFieldRef));
            } else if (encryptedValueLocation.hasIndex()) {
                arrayList.add(Instruction.createObjectLoad(encryptedValueLocation.getLocalVariableIndex(), localVariableList1, 11));
                arrayList.add(Instruction.createIntLoad(encryptedValueLocation.getIndex(), localVariableList1, 11));
                arrayList.add(SimpleInstruction.forOpcode(47));
            } else {
                arrayList.add(Instruction.createLongLoad(encryptedValueLocation.getLocalVariableIndex(), localVariableList1, 11));
            }

            Iterator iterator = map1.entrySet().iterator();

            while (iterator.hasNext()) {
                ResolvedFieldRef resolvedFieldRef1 = (ResolvedFieldRef) ((Entry) iterator.next()).getKey();
                if (resolvedFieldRef == null) {
                    integerConstantEncryptor.emitStoreDecryptedValue(
                            localVariableList1, arrayList, resolvedFieldRef1, encryptedValueLocation, long1, localVariableIndex2, list2
                    );
                }
            }

            arrayList.add(SimpleInstruction.forOpcode(88));
            list1.addAll(arrayList);
        }
    }

    public MethodInfo groupLongConstants(PairMultiMap pairMultiMap, Set set1, Set set2, List list1, SetMultiMap setMultiMap, Map map1) throws ZkmException, IOException {
        MethodInfo methodInfo1 = null;
        HashMap hashMap = ZkmUtils.createHashMap();
        HashMap hashMap1 = ZkmUtils.createHashMap();
        ArrayList arrayList = new ArrayList();
        Iterator iterator = pairMultiMap.entrySet().iterator();

        while (iterator.hasNext()) {
            Entry entry = (Entry) iterator.next();
            MethodInfo methodInfo2 = (MethodInfo) entry.getKey();
            List list2 = (List) entry.getValue();
            arrayList.clear();
            Iterator iterator1 = list2.iterator();

            while (iterator1.hasNext()) {
                ObjectPair objectPair = (ObjectPair) iterator1.next();
                arrayList.add(objectPair.getFirst());
            }

            if (methodInfo2.isStaticInitializer()) {
                methodInfo1 = methodInfo2;
                set2.addAll(arrayList);
            } else {
                set1.addAll(arrayList);
            }

            iterator1 = arrayList.iterator();

            while (iterator1.hasNext()) {
                ConstantLong constantLong1 = (ConstantLong) iterator1.next();
                Long long1 = constantLong1.getValue();
                ObservableHolder observableHolder = (ObservableHolder) hashMap.get(long1);
                if (observableHolder == null) {
                    observableHolder = new ObservableHolder(long1);
                    hashMap.put(long1, new ObservableHolder(long1));
                }

                setMultiMap.addValue(observableHolder, constantLong1);
                map1.put(constantLong1, observableHolder);
            }
        }

        iterator = list1.iterator();

        while (iterator.hasNext()) {
            ObjectPair objectPair1 = (ObjectPair) iterator.next();
            ConstantLong constantLong = (ConstantLong) objectPair1.getFirst();
            Long long2 = constantLong.getValue();
            ObservableHolder observableHolder1 = (ObservableHolder) hashMap1.get(long2);
            if (observableHolder1 == null) {
                observableHolder1 = new ObservableHolder(long2);
                hashMap1.put(long2, observableHolder1);
            }

            setMultiMap.addValue(observableHolder1, constantLong);
            map1.put(constantLong, observableHolder1);
        }

        return methodInfo1;
    }

    public void trimNestMembersAttribute(TrimProcessor trimProcessor1) {
        int ba = 0;
        ArrayList arrayList = new ArrayList(this.attributeCount);
        NestMembersAttribute nestMembersAttribute = null;

        for (int attributeIndex = 0; attributeIndex < this.attributeCount; attributeIndex++) {
            if (this.attributes[attributeIndex] instanceof NestMembersAttribute) {
                nestMembersAttribute = (NestMembersAttribute) this.attributes[attributeIndex];
                ba = nestMembersAttribute.removeTrimmedMembers(trimProcessor1);
            } else {
                arrayList.add(this.attributes[attributeIndex]);
            }
        }

        if (ba == 0 && nestMembersAttribute != null) {
            int attributeCountValue = arrayList.size();
            this.attributes = ((com.zelix.klassmaster.classfile.attribute.Attribute[]) (arrayList.toArray(new Attribute[attributeCountValue])));
            this.attributeCount = attributeCountValue;
        }
    }

    public MethodInfo[] getMethodInfos() {
        return this.methods;
    }

    public void encryptIntegerConstants(
            InheritedMemberAnalyzer inheritedMemberAnalyzer,
            IntegerConstantEncryptor integerConstantEncryptor,
            ClassMemberLookup classMemberLookup1,
            ClassResolver classResolver1,
            List list1,
            List list2,
            PairMultiMap pairMultiMap,
            IntegerEncryptionExclusions integerEncryptionExclusions,
            MethodParameterChanger methodParameterChanger,
            Map map1,
            Map map2,
            List list3,
            CommonSuperTypeResolver commonSuperTypeResolver1,
            ClassHierarchyQuery classHierarchyQuery
    ) throws ZkmException, IOException {
        List list4 = list1;
        List list5 = list2;
        PairMultiMap pairMultiMap1 = pairMultiMap;
        boolean bl = this.isInterface();
        if (integerEncryptionExclusions == null || !integerEncryptionExclusions.isClassExcluded(this)) {
            if (list4 == null) {
                list4 = new ArrayList(13);
            }

            if (list5 == null) {
                list5 = new ArrayList(0);
            }

            if (pairMultiMap1 == null) {
                pairMultiMap1 = new PairMultiMap(13);
            }

            if (list4.size() != 0 || pairMultiMap1.getKeyCount() != 0) {
                int maxValue = Integer.MAX_VALUE;
                int maxValueValue = Integer.MAX_VALUE;
                HashSet hashSet = ZkmUtils.createHashSet();
                HashSet hashSet1 = ZkmUtils.createHashSet();
                SetMultiMap setMultiMap = new SetMultiMap();
                HashMap hashMap = ZkmUtils.createHashMap();
                MethodInfo methodInfo1 = this.groupIntegerConstants(pairMultiMap1, hashSet, hashSet1, list4, setMultiMap, hashMap);
                if (methodInfo1 == null) {
                    if (!this.isVersionedVariant()) {
                        methodInfo1 = classMemberLookup1.findDeclaredMethod(this, MethodSignature.STATIC_INITIALIZER);
                    } else {
                        methodInfo1 = this.findMethodBySignature(MethodSignature.STATIC_INITIALIZER);
                    }
                }

                HashSet hashSet2 = ZkmUtils.createHashSet();
                ArrayList arrayList = new ArrayList(list4.size());
                Iterator iterator = list4.iterator();

                while (iterator.hasNext()) {
                    ObjectPair objectPair = (ObjectPair) iterator.next();
                    arrayList.add(objectPair.getFirst());
                }

                hashSet2.addAll(arrayList);
                hashSet2.addAll(hashSet);
                hashSet2.addAll(hashSet1);
                HashSet hashSet3 = ZkmUtils.createHashSetFrom(hashSet1);
                hashSet3.addAll(arrayList);
                int bd = methodInfo1 == null ? 0 : methodInfo1.getLocalVariableCount();
                hashSet1.size();
                hashSet3.size();
                if (!bl || !hashSet.isEmpty() || !arrayList.isEmpty()) {
                    ArrayList arrayList1 = new ArrayList();
                    HashMap hashMap1 = ZkmUtils.createHashMap();
                    this.prepareIntegerConstantFields(list4, hashMap, hashMap1, list5, arrayList1);
                    if (methodInfo1 == null) {
                        Integer integer = 11;
                        methodInfo1 = this.getOrCreateStaticInitializer(arrayList1, classMemberLookup1, "Integer Constant Encryption", integer);
                    }

                    LocalVariableAllocator localVariableAllocator = new LocalVariableAllocator(methodInfo1);
                    LocalVariableList localVariableList1 = methodInfo1.getLocalVariableList();
                    ResolvedFieldRef resolvedFieldRef = null;
                    boolean bl1 = setMultiMap.getKeyCount() > 1;
                    boolean bl2 = hashSet.size() > 0;
                    ObservableHolder[] observableHolders = null;
                    if (bl1) {
                        observableHolders = new ObservableHolder[setMultiMap.getKeyCount()];
                    }

                    if (bl2) {
                        String descriptor;
                        if (setMultiMap.getKeyCount() == 1) {
                            descriptor = "J";
                        } else {
                            descriptor = "[J";
                        }

                        FieldInfo fieldInfo = this.createUniquelyNamedStaticField(descriptor, bl ? 4 : 1, true, inheritedMemberAnalyzer, classMemberLookup1, 1);
                        resolvedFieldRef = this.constantPool
                                .getOrAddFieldRef(
                                        this.getClassName(), fieldInfo.getSourceName(), fieldInfo.getDescriptor(), arrayList1, classMemberLookup1, classResolver1, true
                                );
                    }

                    if (bl1 && observableHolders.length > 65535) {
                        throw new ZkmProcessingException(
                                "Integer Constant Encryption can handle a maximumum of 65535 Integers. Number of Integers in class '"
                                        + this.getLocationName()
                                        + "' is "
                                        + observableHolders.length
                                        + "."
                        );
                    }

                    boolean bl4 = methodParameterChanger != null && methodParameterChanger.canKeyIntegerEncryption(methodInfo1);
                    boolean bl5 = resolvedFieldRef != null && bl4 && IntegerConstantEncryptor.canUseIndyEncryption(this);
                    integerConstantEncryptor.createDecryptionMembers(
                            this,
                            inheritedMemberAnalyzer,
                            classMemberLookup1,
                            classResolver1,
                            arrayList1,
                            resolvedFieldRef,
                            methodParameterChanger != null,
                            bl1,
                            bl5,
                            bl4
                    );
                    if (list3 != null) {
                        list3.add(this);
                    }

                    PairMultiMap pairMultiMap2 = new PairMultiMap();
                    HashMap hashMap2 = ZkmUtils.createHashMap();
                    this.assignIntegerValueLocations(
                            pairMultiMap2,
                            hashMap2,
                            pairMultiMap1,
                            setMultiMap,
                            hashMap,
                            resolvedFieldRef,
                            integerConstantEncryptor.getLookupIndyEntry(),
                            bl1,
                            bl4,
                            integerConstantEncryptor.getLookupMethod(),
                            observableHolders,
                            localVariableAllocator
                    );
                    int ba = -1;
                    if (!bl2) {
                        if (bl1) {
                            ba = localVariableAllocator.getAndIncrement();
                        } else {
                            ba = localVariableAllocator.getValue();
                            localVariableAllocator.addAndGet(2);
                        }
                    }

                    HashMap hashMap3 = ZkmUtils.createHashMap();
                    ListMultimap listMultimap = new ListMultimap();
                    ObservableHolder observableHolder = new ObservableHolder();
                    ObservableHolder observableHolder1 = new ObservableHolder();
                    LabelInstruction labelInstruction = this.createDecryptSubroutineLabel(observableHolder1, observableHolder, hashMap3);
                    int value = localVariableAllocator.getValue();
                    MutableInt mutableInt = new MutableInt(-2);
                    Long long1 = null;
                    LocalVariableIndex localVariableIndex1 = null;
                    if (methodParameterChanger != null) {
                        long1 = map1 != null ? (Long) map1.get(methodInfo1) : null;
                        localVariableIndex1 = map2 != null ? (LocalVariableIndex) map2.get(methodInfo1) : null;
                        if (long1 == null) {
                            long1 = methodParameterChanger.assignMethodKey(methodInfo1);
                        }
                    }

                    ObservableHolder observableHolder2 = new ObservableHolder();
                    ObservableHolder observableHolder3 = new ObservableHolder();
                    ArrayList arrayList2 = new ArrayList();
                    if (integerConstantEncryptor.getValueCacheMapField() != null) {
                        integerConstantEncryptor.emitCacheMapInit(arrayList2, arrayList1, this.constantPool, classMemberLookup1, classResolver1);
                    }

                    if (bl1) {
                        integerConstantEncryptor.getValueCacheArrayField();
                        ClassResolver classResolver2 = classResolver1;
                        ClassMemberLookup classMemberLookup2 = classMemberLookup1;
                        MethodParameterChanger methodParameterChanger1 = methodParameterChanger;
                        boolean bl3 = bl4;
                        ObservableHolder observableHolder5 = observableHolder3;
                        Map map3 = map2;
                        LocalVariableIndex localVariableIndex3 = localVariableIndex1;
                        Long long3 = long1;
                        ObservableHolder observableHolder6 = observableHolder2;
                        MethodInfo methodInfo3 = methodInfo1;
                        IntegerConstantEncryptor integerConstantEncryptor1 = integerConstantEncryptor;
                        ArrayList arrayList4 = arrayList1;
                        LocalVariableAllocator localVariableAllocator1 = localVariableAllocator;
                        ListMultimap listMultimap1 = listMultimap;
                        MutableInt mutableInt1 = mutableInt;
                        ObservableHolder observableHolder7 = observableHolder1;
                        HashMap hashMap4 = hashMap1;
                        HashMap hashMap5 = hashMap2;
                        SetMultiMap setMultiMap1 = setMultiMap;
                        ObservableHolder[] observableHolders1 = observableHolders;
                        int bc = ba;
                        ResolvedFieldRef resolvedFieldRef1 = resolvedFieldRef;
                        ArrayList arrayList5 = arrayList2;
                        LocalVariableList localVariableList2 = localVariableList1;
                        this.emitMultiIntDecryption(
                                localVariableList2,
                                arrayList5,
                                resolvedFieldRef1,
                                bc,
                                observableHolders1,
                                setMultiMap1,
                                hashMap5,
                                hashMap4,
                                observableHolder7,
                                mutableInt1,
                                listMultimap1,
                                localVariableAllocator1,
                                arrayList4,
                                integerConstantEncryptor1,
                                methodInfo3,
                                observableHolder6,
                                long3,
                                localVariableIndex3,
                                map3,
                                observableHolder5,
                                bl3,
                                methodParameterChanger1,
                                classMemberLookup2,
                                classResolver2
                        );
                    } else {
                        ObservableHolder observableHolder4 = (ObservableHolder) setMultiMap.keys().nextElement();
                        this.emitSingleIntDecryption(
                                localVariableList1,
                                arrayList2,
                                resolvedFieldRef,
                                ((ConstantInteger) setMultiMap.distinctValues().nextElement()).getValue(),
                                (LabelInstruction) observableHolder1.getValue(),
                                listMultimap,
                                mutableInt,
                                hashMap1,
                                (EncryptedValueLocation) hashMap2.get(observableHolder4),
                                integerConstantEncryptor,
                                methodInfo1,
                                localVariableAllocator,
                                observableHolder2,
                                long1,
                                localVariableIndex1,
                                map2,
                                observableHolder3,
                                bl4,
                                methodParameterChanger,
                                arrayList1,
                                classMemberLookup1,
                                classResolver1
                        );
                    }

                    localVariableIndex1 = map2 != null ? (LocalVariableIndex) map2.get(methodInfo1) : null;
                    Enumeration enumeration = pairMultiMap2.keys();

                    while (enumeration.hasMoreElements()) {
                        MethodInfo methodInfo2 = (MethodInfo) enumeration.nextElement();
                        List list6 = pairMultiMap2.getPairs(methodInfo2);
                        IntCounter intCounter;
                        if (methodInfo2.isStaticInitializer()) {
                            if (localVariableAllocator.getValue() > value) {
                                intCounter = new MutableInt(value);
                            } else {
                                intCounter = localVariableAllocator;
                            }
                        } else {
                            intCounter = new MutableInt(methodInfo2.getLocalVariableCount());
                        }

                        Long long2 = map1 != null ? (Long) map1.get(methodInfo2) : null;
                        LocalVariableIndex localVariableIndex2 = map2 != null ? (LocalVariableIndex) map2.get(methodInfo2) : null;
                        CommonSuperTypeResolver commonSuperTypeResolver2 = commonSuperTypeResolver1;
                        ArrayList arrayList3 = arrayList1;
                        ConstantPool constantPool1 = this.constantPool;
                        methodInfo2.applyIntegerEncryption(
                                list6, long2, localVariableIndex2, intCounter, constantPool1, arrayList3, commonSuperTypeResolver2, classHierarchyQuery
                        );
                    }

                    Integer integer3 = (Integer) observableHolder2.getValue();
                    Integer integer4 = (Integer) observableHolder3.getValue();
                    ClassResolver classResolver3 = classResolver1;
                    ClassMemberLookup classMemberLookup3 = classMemberLookup1;
                    Integer integer1 = integer4;
                    Integer integer2 = integer3;
                    ListMultimap listMultimap2 = listMultimap;
                    ArrayList arrayList6 = arrayList1;
                    ArrayList arrayList7 = arrayList2;
                    LocalVariableAllocator localVariableAllocator2 = localVariableAllocator;
                    LocalVariableIndex localVariableIndex4 = localVariableIndex1;
                    LabelInstruction labelInstruction1 = labelInstruction;
                    LocalVariableList localVariableList3 = localVariableList1;
                    integerConstantEncryptor.emitDecryptDispatch(
                            localVariableList3,
                            labelInstruction1,
                            localVariableIndex4,
                            localVariableAllocator2,
                            arrayList7,
                            arrayList6,
                            listMultimap2,
                            integer2,
                            integer1,
                            classMemberLookup3,
                            classResolver3
                    );
                    int be = localVariableList1.getSlotCount() - bd;
                    ArrayList arrayList8 = new ArrayList(be);

                    for (int i = bd; i < localVariableList1.getSlotCount(); i += 1) {
                        arrayList8.add(i);
                    }

                    int bg = 0;
                    LabelInstruction labelInstruction2 = methodInfo1.getInitializerEndLabel();
                    if (labelInstruction2 != null) {
                        bg = methodInfo1.indexOfInstruction(labelInstruction2);
                    }

                    LabelInstruction labelInstruction3 = new LabelInstruction(4096);
                    arrayList2.add(labelInstruction3);
                    methodInfo1.setInitializerEndLabel(labelInstruction3);
                    methodInfo1.insertInstructions(arrayList2, be, methodInfo1.getMaxStack(), bg, "Integer Constant Encryption", arrayList8);
                    if (arrayList1.size() > 0) {
                        this.constantPool.appendEntries(arrayList1);
                        this.setChanged();
                    } else {
                        this.setChanged();
                    }

                    this.notifyObservers(new MutableInt(4), this, null);
                }
            }
        }
    }

    public void emitSingleStringDecryption(
            LocalVariableList localVariableList1,
            List list1,
            ResolvedFieldRef resolvedFieldRef,
            ResolvedStringConstant resolvedStringConstant,
            JsrGotoKind jsrGotoKind,
            LabelInstruction labelInstruction,
            ListMultimap listMultimap,
            MutableInt mutableInt,
            Map map1,
            EncryptedStringLocation encryptedStringLocation,
            boolean bl,
            ObservableHolder observableHolder,
            StringEncryptor stringEncryptor,
            MethodInfo methodInfo1,
            LocalVariableAllocator localVariableAllocator,
            ObservableHolder observableHolder1,
            Long long1,
            LocalVariableIndex localVariableIndex1,
            Map map2,
            boolean bl1,
            Set set1,
            MethodParameterChanger methodParameterChanger,
            List list2,
            ClassMemberLookup classMemberLookup1,
            ClassResolver classResolver1
    ) throws ZkmException, IOException {
        LocalVariableIndex localVariableIndex2 = localVariableIndex1;
        if (long1 != null) {
            if (localVariableIndex2 == null) {
                localVariableIndex2 = methodParameterChanger.emitKeyLocalInit(
                        long1, localVariableAllocator, list1, methodInfo1, map2, this.getClassConstantPool(), list2
                );
            }

            if (bl1) {
                int andIncrement = localVariableAllocator.getAndIncrement();
                stringEncryptor.emitDesCipherInit(
                        localVariableList1, list1, andIncrement, localVariableIndex2, localVariableAllocator, list2, classMemberLookup1, classResolver1
                );
                observableHolder1.setValue(integerCache.valueOf(andIncrement));
            }
        }

        stringEncryptor.emitEncryptedStringLoad(
                localVariableList1,
                list1,
                resolvedFieldRef,
                resolvedStringConstant,
                encryptedStringLocation,
                jsrGotoKind,
                labelInstruction,
                mutableInt,
                listMultimap,
                bl,
                observableHolder
        );
        if (!map1.isEmpty()) {
            ArrayList arrayList = new ArrayList();
            if (resolvedFieldRef != null) {
                arrayList.add(new ConstantRefInstruction(178, resolvedFieldRef));
            } else {
                arrayList.add(Instruction.createObjectLoad(encryptedStringLocation.getArrayLocalIndex(), localVariableList1, 1));
            }

            Iterator iterator = map1.entrySet().iterator();

            while (iterator.hasNext()) {
                Entry entry = (Entry) iterator.next();
                ResolvedFieldRef resolvedFieldRef1 = (ResolvedFieldRef) entry.getKey();
                if (resolvedFieldRef == null || resolvedFieldRef1.getResolvedMember() != resolvedFieldRef.getResolvedMember()) {
                    stringEncryptor.emitStringFieldInit(localVariableList1, arrayList, resolvedFieldRef1, encryptedStringLocation, long1, localVariableIndex2, list2);
                }
            }

            arrayList.add(SimpleInstruction.forOpcode(87));
            list1.addAll(arrayList);
        }

        String editableValue = resolvedStringConstant.getEditableValue();
        String valueFromString;
        if (bl1) {
            valueFromString = stringEncryptor.desEncrypt(editableValue, long1);
        } else {
            int[] xorKeys;
            if (!observableHolder.isValueNull()) {
                xorKeys = stringEncryptor.deriveXorKeys((Integer) observableHolder.getValue());
            } else {
                xorKeys = stringEncryptor.getXorKeys();
            }

            valueFromString = StringEncryptor.xorWithKeys(editableValue, xorKeys);
        }

        if (valueFromString.length() > 65000) {
            byte[] byteArray = ZkmUtils.toModifiedUtf8(valueFromString);
            if (byteArray.length > 65535) {
                byte[] byteArrayValue = ZkmUtils.toModifiedUtf8(editableValue);
                throw new ZkmProcessingException(
                        "String too large to be String encrypted in class '"
                                + this.getDisplayLocationName()
                                + "'. Original String length "
                                + byteArrayValue.length
                                + ". Encrypted String length "
                                + byteArray.length
                                + " Consider excluding the class from String Encryption. First 50 characters of original String are '"
                                + ZkmUtils.escapeJavaString(editableValue.substring(0, 50))
                                + "'"
                );
            }
        }

        resolvedStringConstant.setValueFromString(valueFromString);
        set1.add(resolvedStringConstant);
    }

    public String getImplementsClause() {
        String implementsClause;
        if (super.interfaceCount == 0) {
            implementsClause = "";
        } else {
            implementsClause = "implements ";
            int interfaceIndex = 0;
            int bb = 0;

            for (int interfaceCountValue = super.interfaceCount; bb < interfaceCountValue; interfaceCountValue = super.interfaceCount) {
                implementsClause = implementsClause + super.interfaces[interfaceIndex].getDottedClassName();
                implementsClause = implementsClause + (interfaceIndex < super.interfaceCount - 1 ? ", " : " ");
                bb = ++interfaceIndex;
            }
        }

        return implementsClause;
    }

    public void renameFieldsWithPrefix(String prefix, String string1, TwoKeyMap twoKeyMap, ClassRepository classRepository1) throws ZkmException, IOException {
        for (int fieldIndex = 0; fieldIndex < this.fieldCount; fieldIndex++) {
            if (this.fields[fieldIndex].getSourceName().startsWith(prefix)) {
                this.fields[fieldIndex].replaceNamePrefix(prefix, string1, twoKeyMap, this, classRepository1);
            }
        }
    }

    public MethodInfo findMethodBySignature(MethodSignature methodSignature1) {
        return (MethodInfo) super.findMethod(methodSignature1);
    }

    public void refreshFieldListNode() throws ZkmException, IOException {
        if (propertyNodes != null) {
            ((MemberListNode) propertyNodes.elementAt(1)).updateMembers(this.fields);
        }
    }

    public Enumeration createPropertyNodes() {
        this.listenerRegistry.removeObserversForKey("CLASS_PROPERTIES");
        propertyNodes = new Vector();
        ClassNamePropertyNode classNamePropertyNode = new ClassNamePropertyNode(this);
        this.listenerRegistry.addObserver(this, classNamePropertyNode, "CLASS_PROPERTIES");
        propertyNodes.addElement(classNamePropertyNode);
        MemberListNode memberListNode1 = new MemberListNode("fields", this, this.fields, integerCache);
        this.listenerRegistry.addObserver(this, memberListNode1, "CLASS_PROPERTIES");
        propertyNodes.addElement(memberListNode1);
        MemberListNode memberListNode2 = new MemberListNode("methods", this, this.methods, integerCache);
        this.listenerRegistry.addObserver(this, memberListNode2, "CLASS_PROPERTIES");
        propertyNodes.addElement(memberListNode2);
        NumericConstantEntry[] numericConstantEntrys = this.constantPool.getLiteralConstants();
        ClassConstantsNode classConstantsNode = new ClassConstantsNode(this, numericConstantEntrys, integerCache);
        this.listenerRegistry.addObserver(this, classConstantsNode, "CLASS_PROPERTIES");
        propertyNodes.addElement(classConstantsNode);
        return propertyNodes.elements();
    }

    public void makePublicAndNonFinal(ScriptEnvironment scriptEnvironment1) {
        PrintWriter printWriter = scriptEnvironment1.getLogWriter();
        if (!this.isPublic()) {
            this.makePublic();
            if (scriptEnvironment1.isVerbose()) {
                printWriter.println("\tChanging class '" + this.getOriginalDottedName() + "' to be public.");
            }
        }

        if (this.isFinal()) {
            this.setFinal();
            if (scriptEnvironment1.isVerbose()) {
                printWriter.println("\tChanging class '" + this.getOriginalDottedName() + "' to not be final.");
            }
        }
    }

    public void prepareStringConstantFields(List list1, Map map1, Map map2, List list2, List list3) {
        Iterator iterator = list1.iterator();

        while (iterator.hasNext()) {
            ObjectPair objectPair = (ObjectPair) iterator.next();
            ResolvedStringConstant resolvedStringConstant = (ResolvedStringConstant) objectPair.getFirst();
            FieldInfo fieldInfo = (FieldInfo) objectPair.getSecond();
            fieldInfo.removeConstantValueAttribute();
            ResolvedFieldRef resolvedFieldRef = this.constantPool
                    .getOrCreateFieldRef(this.getClassName(), fieldInfo.getSourceName(), fieldInfo.getDescriptor(), list3, fieldInfo);
            map2.put(resolvedFieldRef, map1.get(resolvedStringConstant));
        }

        iterator = list2.iterator();

        while (iterator.hasNext()) {
            FieldInfo fieldInfo1 = (FieldInfo) iterator.next();
            boolean bl = false;

            for (int methodIndex = 0; methodIndex < this.methodCount; methodIndex++) {
                MethodInfo methodInfo1 = this.methods[methodIndex];
                if (methodInfo1.isConstructor() && methodInfo1.storesToField(fieldInfo1)) {
                    bl = true;
                    break;
                }
            }

            if (bl) {
                fieldInfo1.removeConstantValueAttribute();
            }
        }
    }

    public void collectIntegerConstantFields(PairMultiMap pairMultiMap, PairMultiMap pairMultiMap1, Set set1) {
        for (int fieldIndex = 0; fieldIndex < this.fields.length; fieldIndex++) {
            FieldInfo fieldInfo = this.fields[fieldIndex];
            if (fieldInfo.isIntCompatibleType()) {
                ConstantValueAttribute constantValueAttribute = fieldInfo.getConstantValueAttribute();
                if (constantValueAttribute != null) {
                    ConstantInteger constantInteger = constantValueAttribute.getIntegerConstant();
                    int value = constantInteger.getValue();
                    if ((HiddenOptionFlags.ENCRYPT_TRIVIAL_INTEGERS || value < -1 || value > 1) && !set1.contains(constantInteger)) {
                        if (fieldInfo.isStatic()) {
                            pairMultiMap.addPair(this, constantInteger, fieldInfo);
                        } else {
                            pairMultiMap1.addPair(this, constantInteger, fieldInfo);
                        }
                    }
                }
            }
        }
    }

    public void prepareIntegerConstantFields(List list1, Map map1, Map map2, List list2, List list3) {
        Iterator iterator = list1.iterator();

        while (iterator.hasNext()) {
            ObjectPair objectPair = (ObjectPair) iterator.next();
            ConstantInteger constantInteger = (ConstantInteger) objectPair.getFirst();
            FieldInfo fieldInfo = (FieldInfo) objectPair.getSecond();
            fieldInfo.removeConstantValueAttribute();
            ResolvedFieldRef resolvedFieldRef = this.constantPool
                    .getOrCreateFieldRef(this.getClassName(), fieldInfo.getSourceName(), fieldInfo.getDescriptor(), list3, fieldInfo);
            map2.put(resolvedFieldRef, map1.get(constantInteger));
        }

        iterator = list2.iterator();

        while (iterator.hasNext()) {
            FieldInfo fieldInfo1 = (FieldInfo) ((ObjectPair) iterator.next()).getSecond();
            boolean bl = false;

            for (int methodIndex = 0; methodIndex < this.methodCount; methodIndex++) {
                MethodInfo methodInfo1 = this.methods[methodIndex];
                if (methodInfo1.isConstructor() && methodInfo1.storesToField(fieldInfo1)) {
                    bl = true;
                    break;
                }
            }

            if (bl) {
                fieldInfo1.removeConstantValueAttribute();
            }
        }
    }

    public void registerFields(ClassMemberLookup classMemberLookup1) {
        for (int fieldIndex = 0; fieldIndex < this.fieldCount; fieldIndex++) {
            classMemberLookup1.registerField(this, this.fields[fieldIndex]);
        }
    }

    public void clearParameterObfuscatedFlags() {
        for (MethodInfo methodInfo1 : this.methods) {
            if (methodInfo1.isNameChanged()) {
                methodInfo1.setNameChanged(false);
            }
        }
    }

    public BootstrapMethodsAttribute getOrCreateBootstrapMethodsAttribute(List list1) {
        for (int attributeIndex = 0; attributeIndex < this.attributeCount; attributeIndex++) {
            if (this.attributes[attributeIndex] instanceof BootstrapMethodsAttribute) {
                return (BootstrapMethodsAttribute) this.attributes[attributeIndex];
            }
        }

        BootstrapMethodsAttribute bootstrapMethodsAttribute1 = new BootstrapMethodsAttribute(
                this, this.constantPool.createUtf8Constant("BootstrapMethods", list1)
        );
        Attribute[] attributes1 = new Attribute[this.attributes.length + 1];
        System.arraycopy(this.attributes, 0, attributes1, 0, this.attributes.length);
        attributes1[this.attributes.length] = bootstrapMethodsAttribute1;
        this.attributes = attributes1;
        this.attributeCount = this.attributes.length;
        return bootstrapMethodsAttribute1;
    }

    public void findReflectionApiCalls(Set set1, Map map1, ClassMemberLookup classMemberLookup1, MessageReporter messageReporter1) throws ZkmException, IOException {
        this.constantPool.findReflectionApiCalls(set1, map1, classMemberLookup1, messageReporter1);
    }

    public final void addInheritedMethodReferrer(Object key, Object object1) {
        if (this.inheritedMethodReferrers == null) {
            this.inheritedMethodReferrers = new SetMultiMap(10, 5);
        }

        this.inheritedMethodReferrers.addValue(key, object1);
    }

    public void collectEncryptableIntegers(
            MultiMapTable multiMapTable,
            PairMultiMap pairMultiMap,
            PairMultiMap pairMultiMap1,
            IntegerEncryptionExclusions integerEncryptionExclusions,
            boolean bl,
            boolean bl1
    ) {
        Set set1 = this.collectExcludedIntegerConstants(integerEncryptionExclusions);
        this.collectIntegerConstantFields(pairMultiMap, pairMultiMap1, set1);
        ArrayList arrayList = new ArrayList();
        ConstantPool constantPool1 = this.getClassConstantPool();

        for (int methodIndex = 0; methodIndex < this.methods.length; methodIndex++) {
            if ((integerEncryptionExclusions == null || !integerEncryptionExclusions.isMethodExcluded(this.methods[methodIndex]))
                    && (bl || !this.methods[methodIndex].isManufactured())) {
                this.methods[methodIndex].collectEncryptableIntegers(multiMapTable, set1, integerEncryptionExclusions, bl1, arrayList, constantPool1);
            }
        }

        if (!arrayList.isEmpty()) {
            constantPool1.appendEntries(arrayList);
        }
    }

    public String generateUniqueMethodName(List list1, String string, InheritedMemberAnalyzer inheritedMemberAnalyzer, boolean bl, String prefix) {
        ArrayList arrayList = list1 != null ? new ArrayList(list1) : null;
        IndexedNameGenerator indexedNameGenerator = new IndexedNameGenerator(prefix);
        int ba = 0;
        HashSet hashSet = null;
        HashSet hashSet1;
        if (bl) {
            hashSet1 = ZkmUtils.createHashSetFrom(inheritedMemberAnalyzer.getVisibleMethodNameTypes(this));
        } else {
            hashSet1 = ZkmUtils.createHashSetFrom(inheritedMemberAnalyzer.getVisibleMethodSignatures(this));
        }

        String nameAt;
        boolean bl1;
        do {
            bl1 = false;

            do {
                if (arrayList != null && !arrayList.isEmpty()) {
                    nameAt = (String) arrayList.remove(0);
                } else {
                    nameAt = indexedNameGenerator.getNameAt(ba++);
                }
            } while (nameAt == null);

            MethodSignature methodSignature1 = new MethodSignature(nameAt, string);
            FieldNameTypeSignature fieldNameTypeSignature = methodSignature1.getNameTypeSignature();
            if ((!bl || !hashSet1.contains(fieldNameTypeSignature)) && (!bl || !hashSet1.contains(methodSignature1))) {
                if (hashSet == null) {
                    hashSet = ZkmUtils.createHashSet(ZkmUtils.getPrimeCapacity(this.methodCount));

                    for (int methodIndex = 0; methodIndex < this.methodCount; methodIndex++) {
                        MethodSignature methodSignature2 = new MethodSignature(this.methods[methodIndex].getOriginalMemberName(), this.methods[methodIndex].getDescriptor());
                        hashSet.add(methodSignature2);
                    }
                }

                if (hashSet.contains(methodSignature1)) {
                    bl1 = true;
                }
            } else {
                bl1 = true;
            }
        } while (bl1);

        return nameAt;
    }

    public Enumeration enumerateInheritedFieldReferrers(Object key) {
        if (this.inheritedFieldReferrers == null) {
            return new EmptyEnumeration();
        }

        Set set1 = this.inheritedFieldReferrers.getValues(key);
        return set1 != null ? Collections.enumeration(set1) : new EmptyEnumeration();
    }

    public void collectReferencedClasses(HashSet hashSet, HashSet hashSet1, HashSet hashSet2) throws ZkmProcessingException {
        this.collectClassAttributeReferences(hashSet, hashSet, hashSet1, hashSet2);

        for (int fieldIndex = 0; fieldIndex < this.fieldCount; fieldIndex++) {
            FieldInfo fieldInfo = this.fields[fieldIndex];
            fieldInfo.collectSignatureTypeReferences(hashSet, hashSet, hashSet1, hashSet2);
        }

        for (int methodIndex = 0; methodIndex < this.methodCount; methodIndex++) {
            MethodInfo methodInfo1 = this.methods[methodIndex];
            methodInfo1.collectSignatureTypeReferences(hashSet, hashSet, hashSet1, hashSet2);
        }

        this.constantPool.collectReferencedProgramMembers(hashSet, hashSet1, hashSet2);
    }

    public void updatePreverification(
            ScriptEnvironment scriptEnvironment1, CommonSuperTypeResolver commonSuperTypeResolver1, ClassHierarchyQuery classHierarchyQuery
    ) throws ZkmException, IOException {
        ArrayList arrayList = new ArrayList();

        for (int methodIndex = 0; methodIndex < this.methodCount; methodIndex++) {
            try {
                this.methods[methodIndex].rebuildStackMap(commonSuperTypeResolver1, classHierarchyQuery, this.constantPool, arrayList);
            } catch (MethodAnalysisException methodAnalysisException) {
                scriptEnvironment1.logWarning(
                        "Couldn't update preverification in '"
                                + this.getLocationName()
                                + "' : \""
                                + methodAnalysisException.getMessage()
                                + "\". You must correct this error or preverify AFTER obfuscation. (A)"
                );
            } catch (StackAnalysisException stackAnalysisException) {
                scriptEnvironment1.logWarning(
                        "Couldn't update preverification in '"
                                + this.getLocationName()
                                + "' : \""
                                + stackAnalysisException.getMessage()
                                + "\". You must correct this error or preverify AFTER obfuscation. (B)"
                );
            } catch (ZkmClassNotFoundException zkmClassNotFoundException) {
                scriptEnvironment1.logWarning(
                        "Couldn't update preverification in '"
                                + this.getLocationName()
                                + "' : \""
                                + zkmClassNotFoundException.getMessage()
                                + "\". You must correct this error or preverify AFTER obfuscation. (C)"
                );
            }
        }

        this.constantPool.appendEntries(arrayList);
    }

    public void markAllMethodsModified() {
        for (int methodIndex = 0; methodIndex < this.methodCount; methodIndex++) {
            this.methods[methodIndex].setCodeModified();
        }
    }

    public ProgramClass(
            ClassFileInputStream classFileInputStream,
            InputFileLocation inputFileLocation,
            ObservableHolder observableHolder,
            ListenerRegistry listenerRegistry1,
            PrintWriter printWriter,
            ThreeKeyMultiMap threeKeyMultiMap
    ) throws ZkmException, IOException {
        this(classFileInputStream, inputFileLocation, observableHolder, listenerRegistry1, printWriter, threeKeyMultiMap, 0);
    }

    public void encryptLongConstants(
            InheritedMemberAnalyzer inheritedMemberAnalyzer,
            LongConstantEncryptor longConstantEncryptor,
            ClassMemberLookup classMemberLookup1,
            ClassResolver classResolver1,
            List list1,
            List list2,
            PairMultiMap pairMultiMap,
            LongEncryptionExclusionHandler longEncryptionExclusionHandler,
            MethodParameterChanger methodParameterChanger,
            Map map1,
            Map map2,
            List list3,
            CommonSuperTypeResolver commonSuperTypeResolver1,
            ClassHierarchyQuery classHierarchyQuery
    ) throws ZkmException, IOException {
        List list5 = list1;
        List list4 = list2;
        PairMultiMap pairMultiMap1 = pairMultiMap;
        boolean bl = this.isInterface();
        if (longEncryptionExclusionHandler == null || !longEncryptionExclusionHandler.isClassExcluded(this)) {
            if (list5 == null) {
                list5 = new ArrayList(13);
            }

            if (list4 == null) {
                list4 = new ArrayList(0);
            }

            if (pairMultiMap1 == null) {
                pairMultiMap1 = new PairMultiMap(13);
            }

            if (list5.size() != 0 || pairMultiMap1.getKeyCount() != 0) {
                int maxValue = Integer.MAX_VALUE;
                int maxValueValue = Integer.MAX_VALUE;
                HashSet hashSet = ZkmUtils.createHashSet();
                HashSet hashSet1 = ZkmUtils.createHashSet();
                SetMultiMap setMultiMap = new SetMultiMap();
                HashMap hashMap = ZkmUtils.createHashMap();
                MethodInfo methodInfo1 = this.groupLongConstants(pairMultiMap1, hashSet, hashSet1, list5, setMultiMap, hashMap);
                if (methodInfo1 == null) {
                    if (!this.isVersionedVariant()) {
                        methodInfo1 = classMemberLookup1.findDeclaredMethod(this, MethodSignature.STATIC_INITIALIZER);
                    } else {
                        methodInfo1 = this.findMethodBySignature(MethodSignature.STATIC_INITIALIZER);
                    }
                }

                HashSet hashSet2 = ZkmUtils.createHashSet();
                ArrayList arrayList = new ArrayList(list5.size());
                Iterator iterator = list5.iterator();

                while (iterator.hasNext()) {
                    ObjectPair objectPair = (ObjectPair) iterator.next();
                    arrayList.add(objectPair.getFirst());
                }

                hashSet2.addAll(arrayList);
                hashSet2.addAll(hashSet);
                hashSet2.addAll(hashSet1);
                HashSet hashSet3 = ZkmUtils.createHashSetFrom(hashSet1);
                hashSet3.addAll(arrayList);
                int bd = methodInfo1 == null ? 0 : methodInfo1.getLocalVariableCount();
                hashSet1.size();
                hashSet3.size();
                if (!bl || !hashSet.isEmpty() || !arrayList.isEmpty()) {
                    ArrayList arrayList1 = new ArrayList();
                    HashMap hashMap1 = ZkmUtils.createHashMap();
                    this.prepareLongConstantFields(list5, hashMap, hashMap1, list4, arrayList1);
                    if (methodInfo1 == null) {
                        Integer integer = 11;
                        methodInfo1 = this.getOrCreateStaticInitializer(arrayList1, classMemberLookup1, "Integer Constant Encryption", integer);
                    }

                    LocalVariableAllocator localVariableAllocator = new LocalVariableAllocator(methodInfo1);
                    LocalVariableList localVariableList1 = methodInfo1.getLocalVariableList();
                    ResolvedFieldRef resolvedFieldRef = null;
                    boolean bl1 = setMultiMap.getKeyCount() > 1;
                    boolean bl2 = hashSet.size() > 0;
                    ObservableHolder[] observableHolders = null;
                    if (bl1) {
                        observableHolders = new ObservableHolder[setMultiMap.getKeyCount()];
                    }

                    if (bl2) {
                        String descriptor;
                        if (setMultiMap.getKeyCount() == 1) {
                            descriptor = "J";
                        } else {
                            descriptor = "[J";
                        }

                        FieldInfo fieldInfo = this.createUniquelyNamedStaticField(descriptor, bl ? 4 : 1, true, inheritedMemberAnalyzer, classMemberLookup1, 1);
                        resolvedFieldRef = this.constantPool
                                .getOrAddFieldRef(
                                        this.getClassName(), fieldInfo.getSourceName(), fieldInfo.getDescriptor(), arrayList1, classMemberLookup1, classResolver1, true
                                );
                    }

                    if (bl1 && observableHolders.length > 65535) {
                        throw new ZkmProcessingException(
                                "Long Constant Encryption can handle a maximumum of 65535 Longs. Number of Longs in class '"
                                        + this.getLocationName()
                                        + "' is "
                                        + observableHolders.length
                                        + "."
                        );
                    }

                    boolean bl4 = methodParameterChanger != null && methodParameterChanger.canKeyLongEncryption(methodInfo1);
                    boolean bl5 = resolvedFieldRef != null && bl4 && LongConstantEncryptor.isLongEncryptionEnabled(this);
                    longConstantEncryptor.initializeForClass(
                            this,
                            inheritedMemberAnalyzer,
                            classMemberLookup1,
                            classResolver1,
                            arrayList1,
                            resolvedFieldRef,
                            methodParameterChanger != null,
                            bl1,
                            bl5,
                            bl4
                    );
                    if (list3 != null) {
                        list3.add(this);
                    }

                    PairMultiMap pairMultiMap2 = new PairMultiMap();
                    HashMap hashMap2 = ZkmUtils.createHashMap();
                    this.assignLongValueLocations(
                            pairMultiMap2,
                            hashMap2,
                            pairMultiMap1,
                            setMultiMap,
                            hashMap,
                            resolvedFieldRef,
                            longConstantEncryptor.getLookupInvokeDynamic(),
                            bl1,
                            bl4,
                            longConstantEncryptor.getLookupMethod(),
                            observableHolders,
                            localVariableAllocator
                    );
                    int ba = -1;
                    if (!bl2) {
                        if (bl1) {
                            ba = localVariableAllocator.getAndIncrement();
                        } else {
                            ba = localVariableAllocator.getValue();
                            localVariableAllocator.addAndGet(2);
                        }
                    }

                    HashMap hashMap3 = ZkmUtils.createHashMap();
                    ListMultimap listMultimap = new ListMultimap();
                    ObservableHolder observableHolder = new ObservableHolder();
                    ObservableHolder observableHolder1 = new ObservableHolder();
                    LabelInstruction labelInstruction = this.createDecryptSubroutineLabel(observableHolder1, observableHolder, hashMap3);
                    int value = localVariableAllocator.getValue();
                    MutableInt mutableInt = new MutableInt(-2);
                    Long long1 = null;
                    LocalVariableIndex localVariableIndex1 = null;
                    if (methodParameterChanger != null) {
                        long1 = map1 != null ? (Long) map1.get(methodInfo1) : null;
                        localVariableIndex1 = map2 != null ? (LocalVariableIndex) map2.get(methodInfo1) : null;
                        if (long1 == null) {
                            long1 = methodParameterChanger.assignMethodKey(methodInfo1);
                        }
                    }

                    ObservableHolder observableHolder2 = new ObservableHolder();
                    ObservableHolder observableHolder3 = new ObservableHolder();
                    ArrayList arrayList2 = new ArrayList();
                    if (longConstantEncryptor.getCipherCacheField() != null) {
                        longConstantEncryptor.emitCipherCacheInit(arrayList2, arrayList1, this.constantPool, classMemberLookup1, classResolver1);
                    }

                    if (bl1) {
                        longConstantEncryptor.getLongCacheField();
                        ClassResolver classResolver2 = classResolver1;
                        ClassMemberLookup classMemberLookup2 = classMemberLookup1;
                        MethodParameterChanger methodParameterChanger1 = methodParameterChanger;
                        boolean bl3 = bl4;
                        ObservableHolder observableHolder5 = observableHolder3;
                        Map map3 = map2;
                        LocalVariableIndex localVariableIndex3 = localVariableIndex1;
                        Long long3 = long1;
                        ObservableHolder observableHolder6 = observableHolder2;
                        MethodInfo methodInfo3 = methodInfo1;
                        LongConstantEncryptor longConstantEncryptor1 = longConstantEncryptor;
                        ArrayList arrayList4 = arrayList1;
                        LocalVariableAllocator localVariableAllocator1 = localVariableAllocator;
                        ListMultimap listMultimap1 = listMultimap;
                        MutableInt mutableInt1 = mutableInt;
                        ObservableHolder observableHolder7 = observableHolder1;
                        HashMap hashMap4 = hashMap1;
                        HashMap hashMap5 = hashMap2;
                        SetMultiMap setMultiMap1 = setMultiMap;
                        ObservableHolder[] observableHolders1 = observableHolders;
                        int bc = ba;
                        ResolvedFieldRef resolvedFieldRef1 = resolvedFieldRef;
                        ArrayList arrayList5 = arrayList2;
                        LocalVariableList localVariableList2 = localVariableList1;
                        this.emitMultiLongDecryption(
                                localVariableList2,
                                arrayList5,
                                resolvedFieldRef1,
                                bc,
                                observableHolders1,
                                setMultiMap1,
                                hashMap5,
                                hashMap4,
                                observableHolder7,
                                mutableInt1,
                                listMultimap1,
                                localVariableAllocator1,
                                arrayList4,
                                longConstantEncryptor1,
                                methodInfo3,
                                observableHolder6,
                                long3,
                                localVariableIndex3,
                                map3,
                                observableHolder5,
                                bl3,
                                methodParameterChanger1,
                                classMemberLookup2,
                                classResolver2
                        );
                    } else {
                        ObservableHolder observableHolder4 = (ObservableHolder) setMultiMap.keys().nextElement();
                        this.emitSingleLongDecryption(
                                localVariableList1,
                                arrayList2,
                                resolvedFieldRef,
                                ((ConstantLong) setMultiMap.distinctValues().nextElement()).getValue(),
                                (LabelInstruction) observableHolder1.getValue(),
                                listMultimap,
                                mutableInt,
                                hashMap1,
                                (ObfuscatedReferenceSlot) hashMap2.get(observableHolder4),
                                longConstantEncryptor,
                                methodInfo1,
                                localVariableAllocator,
                                observableHolder2,
                                long1,
                                localVariableIndex1,
                                map2,
                                observableHolder3,
                                bl4,
                                methodParameterChanger,
                                arrayList1,
                                classMemberLookup1,
                                classResolver1
                        );
                    }

                    localVariableIndex1 = map2 != null ? (LocalVariableIndex) map2.get(methodInfo1) : null;
                    Enumeration enumeration = pairMultiMap2.keys();

                    while (enumeration.hasMoreElements()) {
                        MethodInfo methodInfo2 = (MethodInfo) enumeration.nextElement();
                        List list6 = pairMultiMap2.getPairs(methodInfo2);
                        IntCounter intCounter;
                        if (methodInfo2.isStaticInitializer()) {
                            if (localVariableAllocator.getValue() > value) {
                                intCounter = new MutableInt(value);
                            } else {
                                intCounter = localVariableAllocator;
                            }
                        } else {
                            intCounter = new MutableInt(methodInfo2.getLocalVariableCount());
                        }

                        Long long2 = map1 != null ? (Long) map1.get(methodInfo2) : null;
                        LocalVariableIndex localVariableIndex2 = map2 != null ? (LocalVariableIndex) map2.get(methodInfo2) : null;
                        CommonSuperTypeResolver commonSuperTypeResolver2 = commonSuperTypeResolver1;
                        ArrayList arrayList3 = arrayList1;
                        ConstantPool constantPool1 = this.constantPool;
                        methodInfo2.applyLongEncryption(
                                list6, long2, localVariableIndex2, intCounter, constantPool1, arrayList3, commonSuperTypeResolver2, classHierarchyQuery
                        );
                    }

                    Integer integer3 = (Integer) observableHolder2.getValue();
                    Integer integer4 = (Integer) observableHolder3.getValue();
                    ClassResolver classResolver3 = classResolver1;
                    ClassMemberLookup classMemberLookup3 = classMemberLookup1;
                    Integer integer1 = integer4;
                    Integer integer2 = integer3;
                    ListMultimap listMultimap2 = listMultimap;
                    ArrayList arrayList6 = arrayList1;
                    ArrayList arrayList7 = arrayList2;
                    LocalVariableAllocator localVariableAllocator2 = localVariableAllocator;
                    LocalVariableIndex localVariableIndex4 = localVariableIndex1;
                    LabelInstruction labelInstruction1 = labelInstruction;
                    LocalVariableList localVariableList3 = localVariableList1;
                    longConstantEncryptor.emitSwitchDispatch(
                            localVariableList3,
                            labelInstruction1,
                            localVariableIndex4,
                            localVariableAllocator2,
                            arrayList7,
                            arrayList6,
                            listMultimap2,
                            integer2,
                            integer1,
                            classMemberLookup3,
                            classResolver3
                    );
                    int be = localVariableList1.getSlotCount() - bd;
                    ArrayList arrayList8 = new ArrayList(be);

                    for (int i = bd; i < localVariableList1.getSlotCount(); i += 1) {
                        arrayList8.add(i);
                    }

                    int bg = 0;
                    LabelInstruction labelInstruction2 = methodInfo1.getInitializerEndLabel();
                    if (labelInstruction2 != null) {
                        bg = methodInfo1.indexOfInstruction(labelInstruction2);
                    }

                    methodInfo1.insertInstructions(arrayList2, be, methodInfo1.getMaxStack(), bg, "Integer Constant Encryption", arrayList8);
                    if (arrayList1.size() > 0) {
                        this.constantPool.appendEntries(arrayList1);
                        this.setChanged();
                    } else {
                        this.setChanged();
                    }

                    this.notifyObservers(new MutableInt(4), this, null);
                }
            }
        }
    }

    public void trimPermittedSubclasses(TrimProcessor trimProcessor1) {
        int ba = 0;
        ArrayList arrayList = new ArrayList(this.attributeCount);
        PermittedSubclassesAttribute permittedSubclassesAttribute = null;

        for (int attributeIndex = 0; attributeIndex < this.attributeCount; attributeIndex++) {
            if (this.attributes[attributeIndex] instanceof PermittedSubclassesAttribute) {
                permittedSubclassesAttribute = (PermittedSubclassesAttribute) this.attributes[attributeIndex];
                ba = permittedSubclassesAttribute.removeTrimmedSubclasses(trimProcessor1);
            } else {
                arrayList.add(this.attributes[attributeIndex]);
            }
        }

        if (ba == 0 && permittedSubclassesAttribute != null) {
            int attributeCountValue = arrayList.size();
            this.attributes = ((com.zelix.klassmaster.classfile.attribute.Attribute[]) (arrayList.toArray(new Attribute[attributeCountValue])));
            this.attributeCount = attributeCountValue;
            super.permittedSubclassesPresent = false;
        }
    }

    public SignatureTypeReferences getSignatureTypeReferences() throws ZkmProcessingException {
        SignatureTypeReferences signatureTypeReferences = new SignatureTypeReferences();
        this.collectClassAttributeReferences(
                signatureTypeReferences.getAnnotationClasses(),
                signatureTypeReferences.getReferencedClasses(),
                signatureTypeReferences.getReferencedFields(),
                signatureTypeReferences.getReferencedMethods()
        );
        return signatureTypeReferences;
    }

    public ArrayEnumeration enumerateFields() {
        return new ArrayEnumeration(this.fields);
    }

    public void setHasStackMaps(boolean stackMapsPresent) {
        this.stackMapsPresent = stackMapsPresent;
    }

    public void trimMethodParametersAttributes(boolean bl, boolean bl1) {
        boolean bl2 = false;
        if (bl1 && this.isRenamed()) {
            bl2 = true;
        }

        if (bl2) {
            this.removeMethodParametersAttributes();
        } else {
            for (int methodIndex = 0; methodIndex < this.methodCount; methodIndex++) {
                this.methods[methodIndex].trimMethodParametersAttribute(bl, bl1);
            }
        }
    }

    public void collectLongConstantFields(PairMultiMap pairMultiMap, PairMultiMap pairMultiMap1, Set set1) {
        for (int fieldIndex = 0; fieldIndex < this.fields.length; fieldIndex++) {
            FieldInfo fieldInfo = this.fields[fieldIndex];
            if (fieldInfo.isLongType()) {
                ConstantValueAttribute constantValueAttribute = fieldInfo.getConstantValueAttribute();
                if (constantValueAttribute != null) {
                    ConstantLong constantLong = constantValueAttribute.getLongConstant();
                    long value = constantLong.getValue();
                    if ((HiddenOptionFlags.ENCRYPT_TRIVIAL_LONGS || value < -1L || value > 1L) && !set1.contains(constantLong)) {
                        if (fieldInfo.isStatic()) {
                            pairMultiMap.addPair(this, constantLong, fieldInfo);
                        } else {
                            pairMultiMap1.addPair(this, constantLong, fieldInfo);
                        }
                    }
                }
            }
        }
    }

    public void setStringDecryptMethodRef(ResolvedMethodRef resolvedMethodRef) {
        this.stringDecryptMethodRef = resolvedMethodRef;
    }

    public boolean removeDeadCode(
            CommonSuperTypeResolver commonSuperTypeResolver1, ClassMemberLookup classMemberLookup1, boolean bl, ScriptEnvironment scriptEnvironment1
    ) throws ZkmException, IOException {
        boolean bl1 = false;

        for (int methodIndex = 0; methodIndex < this.methodCount; methodIndex++) {
            try {
                if (this.methods[methodIndex].removeDeadCode(commonSuperTypeResolver1, classMemberLookup1, bl, scriptEnvironment1)) {
                    bl1 = true;
                }
            } catch (MethodAnalysisException methodAnalysisException) {
                scriptEnvironment1.logWarning(
                        "Method could not be analyzed in class '"
                                + this.getOwningClass().getLocationName()
                                + "' while attempting the remove dead instructions : '"
                                + methodAnalysisException.getMessage()
                                + "'",
                        true
                );
            } catch (StackAnalysisException stackAnalysisException) {
                throw new ZkmProcessingException(
                        "Method '"
                                + this.methods[methodIndex].getNameWithParameters((HashMap) null)
                                + "' in class '"
                                + this.getDisplayLocationName()
                                + "' appears to be invalid (6) : '"
                                + stackAnalysisException.getMessage()
                                + "'"
                );
            }
        }

        return bl1;
    }

    public void applyParameterChangesToMethods(
            Map map1,
            Map map2,
            Map map3,
            Map map4,
            Set set1,
            InheritedMemberAnalyzer inheritedMemberAnalyzer,
            Map map5,
            CommonSuperTypeResolver commonSuperTypeResolver1,
            ClassMemberLookup classMemberLookup1,
            ClassResolver classResolver1,
            List list1,
            MethodParameterChanger methodParameterChanger,
            MethodOverrideAnalyzer methodOverrideAnalyzer,
            Set set2
    ) throws ZkmException, IOException {
        MethodInfo methodInfo1;
        if (!this.isVersionedVariant()) {
            methodInfo1 = classMemberLookup1.findDeclaredMethod(this, MethodSignature.STATIC_INITIALIZER);
        } else {
            methodInfo1 = this.findMethodBySignature(MethodSignature.STATIC_INITIALIZER);
        }

        MethodParamChangeNode methodParamChangeNode = null;
        if (methodInfo1 != null) {
            methodParamChangeNode = (MethodParamChangeNode) map2.get(methodInfo1);
        }

        for (MethodInfo methodInfo2 : this.getMethodInfos()) {
            if (!methodInfo2.isManufactured() || methodInfo2.isStaticInitializer()) {
                MethodParamChangeNode methodParamChangeNode1 = null;
                if (methodParamChangeNode != null && !methodInfo2.isStaticInitializer()) {
                    methodParamChangeNode1 = (MethodParamChangeNode) map2.get(methodInfo2);
                    if (methodParamChangeNode1 != null) {
                        methodParamChangeNode1.buildInitInstructions(this, classMemberLookup1, classResolver1, list1, inheritedMemberAnalyzer, methodParameterChanger);
                    }
                }

                MethodParamChangeNode methodParamChangeNode3 = methodParamChangeNode1 == null ? methodParamChangeNode : methodParamChangeNode1;
                ConstantPool constantPool2 = this.getClassConstantPool();
                methodParameterChanger.getScriptEnvironment();
                Set set3 = set2;
                MethodOverrideAnalyzer methodOverrideAnalyzer1 = methodOverrideAnalyzer;
                ConstantPool constantPool1 = constantPool2;
                List list2 = list1;
                ClassMemberLookup classMemberLookup2 = classMemberLookup1;
                CommonSuperTypeResolver commonSuperTypeResolver2 = commonSuperTypeResolver1;
                Map map6 = map5;
                Set set4 = set1;
                Map map7 = map4;
                Map map8 = map1;
                Map map9 = map3;
                MethodParamChangeNode methodParamChangeNode2 = methodParamChangeNode3;
                methodInfo2.applyParameterChanges(
                        methodParamChangeNode2,
                        map9,
                        map8,
                        map7,
                        set4,
                        map6,
                        commonSuperTypeResolver2,
                        classMemberLookup2,
                        list2,
                        constantPool1,
                        methodOverrideAnalyzer1,
                        set3
                );
            }
        }
    }

    public BootstrapMethodIndex getBootstrapMethodIndex() {
        BootstrapMethodsAttribute bootstrapMethodsAttribute1 = null;

        for (Attribute attribute : this.attributes) {
            if (attribute instanceof BootstrapMethodsAttribute) {
                bootstrapMethodsAttribute1 = (BootstrapMethodsAttribute) attribute;
                break;
            }
        }

        if (bootstrapMethodsAttribute1 != null) {
            BootstrapMethodIndex bootstrapMethodIndex1 = bootstrapMethodsAttribute1.buildStringConcatIndex();
            return bootstrapMethodIndex1.isEmpty() ? null : bootstrapMethodIndex1;
        } else {
            return null;
        }
    }

    public void emitMultiStringDecryption(
            LocalVariableList localVariableList1,
            List list1,
            ResolvedFieldRef resolvedFieldRef,
            ResolvedFieldRef resolvedFieldRef1,
            int ba,
            ObservableHolder[] observableHolders,
            SetMultiMap setMultiMap,
            Map map1,
            Map map2,
            JsrGotoKind jsrGotoKind,
            ObservableHolder observableHolder,
            MutableInt mutableInt,
            ListMultimap listMultimap,
            LocalVariableAllocator localVariableAllocator,
            List list2,
            StringEncryptor stringEncryptor,
            MethodInfo methodInfo1,
            boolean bl,
            boolean bl1,
            ObservableHolder observableHolder1,
            Long long1,
            LocalVariableIndex localVariableIndex1,
            Map map3,
            boolean bl2,
            Set set1,
            MethodParameterChanger methodParameterChanger,
            ClassMemberLookup classMemberLookup1,
            ClassResolver classResolver1
    ) throws ZkmException, IOException {
        LocalVariableIndex localVariableIndex2 = localVariableIndex1;
        if (long1 != null) {
            if (localVariableIndex2 == null) {
                localVariableIndex2 = methodParameterChanger.emitKeyLocalInit(
                        long1, localVariableAllocator, list1, methodInfo1, map3, this.getClassConstantPool(), list2
                );
            }

            if (bl2) {
                int andIncrement = localVariableAllocator.getAndIncrement();
                stringEncryptor.emitDesCipherInit(
                        localVariableList1, list1, andIncrement, localVariableIndex2, localVariableAllocator, list2, classMemberLookup1, classResolver1
                );
                observableHolder1.setValue(integerCache.valueOf(andIncrement));
            }
        }

        HashSet hashSet = ZkmUtils.createHashSet();
        stringEncryptor.emitStringArrayInit(
                localVariableList1,
                list1,
                localVariableAllocator,
                hashSet,
                list2,
                resolvedFieldRef,
                resolvedFieldRef1,
                ba,
                observableHolders,
                setMultiMap,
                map1,
                jsrGotoKind,
                (LabelInstruction) observableHolder.getValue(),
                mutableInt,
                listMultimap,
                bl1,
                bl2 ? long1 : null,
                classMemberLookup1,
                classResolver1
        );
        this.constantPool.removeEntries(hashSet);
        Iterator iterator = hashSet.iterator();

        while (iterator.hasNext()) {
            ConstantPoolEntry constantPoolEntry = (ConstantPoolEntry) iterator.next();
            if (constantPoolEntry.getTag() == ConstantPoolTag.STRING) {
                set1.add((ResolvedStringConstant) constantPoolEntry);
            }
        }

        if (!map2.isEmpty()) {
            ArrayList arrayList = new ArrayList();
            if (bl && resolvedFieldRef != null) {
                Iterator iterator2 = map2.entrySet().iterator();

                while (iterator2.hasNext()) {
                    Entry entry1 = (Entry) iterator2.next();
                    ResolvedFieldRef resolvedFieldRef3 = (ResolvedFieldRef) entry1.getKey();
                    ObservableHolder observableHolder3 = (ObservableHolder) entry1.getValue();
                    EncryptedStringLocation encryptedStringLocation1 = (EncryptedStringLocation) map1.get(observableHolder3);
                    stringEncryptor.emitStringFieldInit(
                            localVariableList1, arrayList, resolvedFieldRef3, encryptedStringLocation1, long1, localVariableIndex2, list2
                    );
                }
            } else {
                if (resolvedFieldRef != null) {
                    arrayList.add(new ConstantRefInstruction(178, resolvedFieldRef));
                } else {
                    arrayList.add(Instruction.createObjectLoad(ba, localVariableList1, 1));
                }

                Iterator iterator1 = map2.entrySet().iterator();

                while (iterator1.hasNext()) {
                    Entry entry = (Entry) iterator1.next();
                    ResolvedFieldRef resolvedFieldRef2 = (ResolvedFieldRef) entry.getKey();
                    ObservableHolder observableHolder2 = (ObservableHolder) entry.getValue();
                    EncryptedStringLocation encryptedStringLocation = (EncryptedStringLocation) map1.get(observableHolder2);
                    stringEncryptor.emitStringFieldInit(localVariableList1, arrayList, resolvedFieldRef2, encryptedStringLocation, long1, localVariableIndex2, list2);
                }

                arrayList.add(SimpleInstruction.forOpcode(87));
            }

            list1.addAll(arrayList);
        }
    }

    public void expandStringConcatenations(
            BootstrapMethodIndex bootstrapMethodIndex1,
            CommonSuperTypeResolver commonSuperTypeResolver1,
            ClassHierarchyQuery classHierarchyQuery,
            ScriptEnvironment scriptEnvironment1
    ) throws ZkmException, IOException {
        ConstantPool constantPool1 = this.getClassConstantPool();
        HashMap hashMap = ZkmUtils.createHashMap();
        ListMultimap listMultimap = new ListMultimap(ZkmUtils.getPrimeCapacity(this.methodCount));
        ArrayList arrayList = new ArrayList();

        try {
            for (int methodIndex = 0; methodIndex < this.methodCount; methodIndex++) {
                this.methods[methodIndex]
                        .expandStringConcatInvokes(bootstrapMethodIndex1, constantPool1, hashMap, listMultimap, arrayList, commonSuperTypeResolver1, classHierarchyQuery);
            }
        } catch (StackAnalysisException stackAnalysisException) {
            scriptEnvironment1.logWarning(
                    "java/lang/invoke/StringConcatFactory makeConcatWithConstants call in class '" + this.getLocationName() + "' could not be processed. (1)"
            );
            return;
        } catch (ZkmProcessingException zkmProcessingException) {
            scriptEnvironment1.logWarning(
                    "java/lang/invoke/StringConcatFactory makeConcatWithConstants call in class '" + this.getLocationName() + "' could not be processed. (2)"
            );
            return;
        }

        bootstrapMethodIndex1.applyRewrittenRecipes();
        Iterator iterator = hashMap.entrySet().iterator();

        while (iterator.hasNext()) {
            Entry entry = (Entry) iterator.next();
            ((ResolvedInvokeDynamic) entry.getKey()).setParameterDescriptor((String) entry.getValue());
        }

        if (arrayList.size() > 0) {
            this.addPoolConstants(arrayList);
        }

        String string = "Preparing class '" + this.getLocationName() + "' for String Encryption";
        if (!listMultimap.isEmpty()) {
            Iterator iterator1 = listMultimap.entrySet().iterator();

            while (iterator1.hasNext()) {
                Entry entry1 = (Entry) iterator1.next();
                ((MethodBytecode) entry1.getKey()).applyCodeInsertions((List) entry1.getValue(), string);
            }
        }
    }

    public void collectEncryptableLongs(
            MultiMapTable multiMapTable, PairMultiMap pairMultiMap, PairMultiMap pairMultiMap1, LongEncryptionExclusionHandler longEncryptionExclusionHandler
    ) {
        Set set1 = this.collectExcludedLongConstants(longEncryptionExclusionHandler);
        this.collectLongConstantFields(pairMultiMap, pairMultiMap1, set1);
        ArrayList arrayList = new ArrayList();
        ConstantPool constantPool1 = this.getClassConstantPool();

        for (int methodIndex = 0; methodIndex < this.methods.length; methodIndex++) {
            if ((longEncryptionExclusionHandler == null || !longEncryptionExclusionHandler.isMethodExcluded(this.methods[methodIndex]))
                    && !this.methods[methodIndex].isManufactured()) {
                this.methods[methodIndex].collectEncryptableLongs(multiMapTable, set1, longEncryptionExclusionHandler, arrayList, constantPool1);
            }
        }

        if (!arrayList.isEmpty()) {
            constantPool1.appendEntries(arrayList);
        }
    }

    public int replaceStringConstant(String string, String valueArgument) {
        return this.constantPool.replaceStringConstantValue(string, valueArgument);
    }

    public void setZkmAnnotationValues(AnnotationValueMap annotationValueMap) {
        this.zkmAnnotationValues = annotationValueMap;
    }

    public String getExtendsClause() {
        return this.superClassConstant == null ? "" : "extends " + this.superClassConstant.getDottedClassName() + " ";
    }

    public boolean hasZkmAnnotationValues() {
        return this.zkmAnnotationValues != null;
    }

    public FieldInfo createStaticField(
            String string, String string1, int ba, InheritedMemberAnalyzer inheritedMemberAnalyzer, ClassMemberLookup classMemberLookup1
    ) throws ZkmException, IOException {
        Integer integer = 2;
        return this.createStaticField(string, string1, ba, false, inheritedMemberAnalyzer, classMemberLookup1, integer);
    }

    public String generateUniqueFieldName(List list1, String descriptor, InheritedMemberAnalyzer inheritedMemberAnalyzer) {
        return this.generateUniqueFieldName(list1, descriptor, inheritedMemberAnalyzer, (String) null);
    }

    public void collectBootstrapMethodHandleTargets(Set set1) {
        for (int attributeIndex = 0; attributeIndex < this.attributes.length; attributeIndex++) {
            if (this.attributes[attributeIndex] instanceof BootstrapMethodsAttribute) {
                ((BootstrapMethodsAttribute) this.attributes[attributeIndex]).collectMethodHandleTargets(set1);
            }
        }
    }

    @Override
    public AbstractFieldInfo[] getFields() {
        return this.getFieldInfos();
    }

    @Override
    public void updateFieldReferences(ScriptEnvironment scriptEnvironment1, ClassMemberLookup classMemberLookup1) throws ZkmException, IOException {
        this.constantPool.resolveFieldRefs(scriptEnvironment1, classMemberLookup1);

        for (int attributeIndex = 0; attributeIndex < this.attributes.length; attributeIndex++) {
            if (this.attributes[attributeIndex] instanceof ReferencingAttribute) {
                ((ReferencingAttribute) this.attributes[attributeIndex]).applyFieldRenames();
            } else if (this.attributes[attributeIndex] instanceof RecordAttribute) {
                ((RecordAttribute) this.attributes[attributeIndex]).applyFieldRenames();
            } else if (this.attributes[attributeIndex] instanceof BootstrapMethodsAttribute) {
                ((BootstrapMethodsAttribute) this.attributes[attributeIndex]).updateRecordObjectMethodsNames();
            }
        }

        for (int fieldIndex = 0; fieldIndex < this.fieldCount; fieldIndex++) {
            this.fields[fieldIndex].updateAttributesAfterFieldRename();
        }

        for (int methodIndex = 0; methodIndex < this.methodCount; methodIndex++) {
            this.methods[methodIndex].updateAttributesAfterFieldRename();
        }
    }

    public MethodInfo createStaticMethod(
            String valueArgument,
            String valueArgument2,
            ArrayList arrayList,
            int maxStack,
            int maxLocals,
            int bc,
            LocalVariableList localVariableList1,
            ExceptionHandlerSpec[] exceptionHandlerSpecs,
            String string2,
            List list1,
            InheritedMemberAnalyzer inheritedMemberAnalyzer,
            ClassMemberLookup classMemberLookup1,
            int creationKind
    ) throws ZkmProcessingException {
        return this.createMethod(
                valueArgument,
                valueArgument2,
                arrayList,
                maxStack,
                maxLocals,
                bc,
                true,
                localVariableList1,
                exceptionHandlerSpecs,
                string2,
                list1,
                inheritedMemberAnalyzer,
                classMemberLookup1,
                creationKind
        );
    }

    public Set collectExcludedStringConstants(
            StringEncryptionExclusionSpec stringEncryptionExclusionSpec,
            CommonSuperTypeResolver commonSuperTypeResolver1,
            ClassHierarchyQuery classHierarchyQuery,
            ScriptEnvironment scriptEnvironment1
    ) throws ZkmException, IOException {
        if (this.supportsJava9()
                && (stringEncryptionExclusionSpec == null || !stringEncryptionExclusionSpec.isClassExcluded(this))
                && !HiddenOptionFlags.SKIP_BOOTSTRAP_METHOD_PROCESSING) {
            BootstrapMethodIndex bootstrapMethods = this.getBootstrapMethodIndex();
            if (bootstrapMethods != null) {
                this.expandStringConcatenations(bootstrapMethods, commonSuperTypeResolver1, classHierarchyQuery, scriptEnvironment1);
            }
        }

        HashSet hashSet = ZkmUtils.createHashSet();
        if (stringEncryptionExclusionSpec != null && stringEncryptionExclusionSpec.hasExcludedFields(this)) {
            Enumeration enumeration = stringEncryptionExclusionSpec.getExcludedFields(this);

            while (enumeration.hasMoreElements()) {
                FieldInfo fieldInfo = (FieldInfo) enumeration.nextElement();
                ConstantValueAttribute constantValueAttribute = fieldInfo.getConstantValueAttribute();
                if (constantValueAttribute != null) {
                    ResolvedStringConstant resolvedStringConstant = constantValueAttribute.getStringConstant();
                    if (resolvedStringConstant != null) {
                        hashSet.add(resolvedStringConstant);
                    }
                }
            }

            for (int methodIndex = 0; methodIndex < this.methodCount; methodIndex++) {
                MethodInfo methodInfo2 = this.methods[methodIndex];
                if (methodInfo2.hasStringConstants() && (methodInfo2.isConstructor() || methodInfo2.isStaticInitializer())) {
                    methodInfo2.collectExcludedFieldStrings(hashSet, stringEncryptionExclusionSpec);
                }
            }
        }

        if (stringEncryptionExclusionSpec != null && stringEncryptionExclusionSpec.hasExcludedMethods(this)) {
            Enumeration enumeration1 = stringEncryptionExclusionSpec.getExcludedMethods(this);

            while (enumeration1.hasMoreElements()) {
                MethodInfo methodInfo1 = (MethodInfo) enumeration1.nextElement();
                if (methodInfo1.hasStringConstants()) {
                    methodInfo1.collectStringConstants(hashSet);
                }
            }
        }

        return hashSet;
    }

    public void obfuscateExceptions(
            Set set1,
            NestedMultiMap nestedMultiMap,
            Map map1,
            FlowObfuscationExclusions flowObfuscationExclusions,
            ExceptionObfuscationExclusions exceptionObfuscationExclusions,
            CommonSuperTypeResolver commonSuperTypeResolver1,
            ClassHierarchyQuery classHierarchyQuery,
            ScriptEnvironment scriptEnvironment1,
            List list1,
            boolean bl,
            boolean bl1,
            IgnoreMissingReferencesSpec ignoreMissingReferencesSpec1,
            ProcessingStatistics processingStatistics1,
            InheritedMemberAnalyzer inheritedMemberAnalyzer,
            ClassMemberLookup classMemberLookup1
    ) throws ZkmException, IOException {
        boolean bl2 = bl;
        ArrayList arrayList = new ArrayList();
        PairValueMap pairValueMap = new PairValueMap();
        boolean flowExcludesExceptionObfuscation;
        if (exceptionObfuscationExclusions != null) {
            if (exceptionObfuscationExclusions.isClassExcluded(this)) {
                return;
            }

            flowExcludesExceptionObfuscation = HiddenOptionFlags.FLOW_EXCLUDES_EXCEPTION_OBFUSCATION;
        } else {
            flowExcludesExceptionObfuscation = HiddenOptionFlags.FLOW_EXCLUDES_EXCEPTION_OBFUSCATION;
        }

        if (!flowExcludesExceptionObfuscation || flowObfuscationExclusions == null || !flowObfuscationExclusions.isClassExcluded(this)) {
            if (this.zkmAnnotationValues != null && this.zkmAnnotationValues.hasValue("exceptionObfuscation")) {
                String valueValue = this.zkmAnnotationValues.getValue("exceptionObfuscation");
                int ba = ObfuscateOptions.parseExceptionLevel(valueValue);
                if (ba != (bl2 ? 2 : 1)) {
                    String exceptionLevelName = ObfuscateOptions.getExceptionLevelName(bl2 ? 2 : 1);
                    if (scriptEnvironment1.isVerbose()) {
                        scriptEnvironment1.getLogWriter()
                                .println(
                                        "Overriding Exception Obfuscation setting in class '"
                                                + AbstractExclusionSpec.formatClass(this, classHierarchyQuery, true)
                                                + "' : '"
                                                + exceptionLevelName
                                                + "' changed to '"
                                                + valueValue
                                                + "'"
                                );
                    }

                    switch (ba) {
                        case 0:
                            return;
                        case 1:
                            bl2 = false;
                            break;
                        case 2:
                            bl2 = true;
                    }
                }
            }

            for (int methodIndex = 0; methodIndex < this.methodCount; methodIndex++) {
                MethodInfo methodInfo1 = this.methods[methodIndex];

                try {
                    if (!this.methods[methodIndex].isManufactured()) {
                        if (exceptionObfuscationExclusions != null) {
                            if (exceptionObfuscationExclusions.isMethodExcluded(methodInfo1)) {
                                continue;
                            }

                            flowExcludesExceptionObfuscation = HiddenOptionFlags.FLOW_EXCLUDES_EXCEPTION_OBFUSCATION;
                        } else {
                            flowExcludesExceptionObfuscation = HiddenOptionFlags.FLOW_EXCLUDES_EXCEPTION_OBFUSCATION;
                        }

                        if (!flowExcludesExceptionObfuscation || flowObfuscationExclusions == null || !flowObfuscationExclusions.isMethodExcluded(methodInfo1)) {
                            List list2 = this.constantPool.findSubclassConstants(classHierarchyQuery);
                            methodInfo1.analyzeExceptionObfuscation(
                                    commonSuperTypeResolver1,
                                    classHierarchyQuery,
                                    list1,
                                    list2,
                                    arrayList,
                                    pairValueMap,
                                    bl2,
                                    bl1,
                                    ignoreMissingReferencesSpec1,
                                    scriptEnvironment1
                            );
                        }
                    }
                } catch (MethodAnalysisException methodAnalysisException) {
                    scriptEnvironment1.logMessage(
                            "Couldn't fully process method '"
                                    + this.methods[methodIndex].getNameWithParameters((HashMap) null)
                                    + "' in class '"
                                    + this.getLocationName()
                                    + "' : "
                                    + methodAnalysisException.getMessage()
                    );
                } catch (StackAnalysisException stackAnalysisException) {
                    throw new ZkmProcessingException(
                            "Method '"
                                    + this.methods[methodIndex].getNameWithParameters((HashMap) null)
                                    + "' in class '"
                                    + this.getDisplayLocationName()
                                    + "' appears to be invalid (5) : '"
                                    + stackAnalysisException.getMessage()
                                    + "'"
                    );
                }
            }

            if (pairValueMap.size() > 0) {
                ExceptionObfuscator.obfuscateExceptionHandlers(
                        this,
                        set1,
                        nestedMultiMap,
                        map1,
                        pairValueMap,
                        arrayList,
                        commonSuperTypeResolver1,
                        processingStatistics1,
                        inheritedMemberAnalyzer,
                        classMemberLookup1,
                        scriptEnvironment1
                );
                this.constantPool.appendEntries(arrayList);
                this.setChanged();
                this.notifyObservers(new MutableInt(4), this, null);
            }
        }
    }

    public void trimUnusedReferences(TrimProcessor trimProcessor1) {
        if (this.inheritedFieldReferrers != null) {
            SetMultiMap setMultiMap = this.inheritedFieldReferrers.deepCopy();
            Enumeration enumeration = setMultiMap.keys();

            while (enumeration.hasMoreElements()) {
                AbstractFieldInfo abstractFieldInfo = (AbstractFieldInfo) enumeration.nextElement();
                if (abstractFieldInfo.isProgramMember() && !trimProcessor1.isFieldExcluded((FieldInfo) abstractFieldInfo)) {
                    this.inheritedFieldReferrers.removeKey(abstractFieldInfo);
                } else {
                    Set set1 = setMultiMap.getValues(abstractFieldInfo);
                    Iterator iterator = set1.iterator();

                    while (iterator.hasNext()) {
                        ClassFileBase classFileBase = (ClassFileBase) iterator.next();
                        if (classFileBase.isProgramClass() && !trimProcessor1.isClassExcluded((ProgramClass) classFileBase)) {
                            this.inheritedFieldReferrers.removeValue(abstractFieldInfo, classFileBase);
                        }
                    }
                }
            }
        }

        if (this.inheritedMethodReferrers != null) {
            SetMultiMap setMultiMap1 = this.inheritedMethodReferrers.deepCopy();
            Enumeration enumeration1 = setMultiMap1.keys();

            while (enumeration1.hasMoreElements()) {
                AbstractMethodInfo abstractMethodInfo = (AbstractMethodInfo) enumeration1.nextElement();
                if (abstractMethodInfo.isProgramMember() && !trimProcessor1.isMethodExcluded((MethodInfo) abstractMethodInfo)) {
                    this.inheritedMethodReferrers.removeKey(abstractMethodInfo);
                } else {
                    Set set2 = setMultiMap1.getValues(abstractMethodInfo);
                    Iterator iterator1 = set2.iterator();

                    while (iterator1.hasNext()) {
                        ClassFileBase classFileBase1 = (ClassFileBase) iterator1.next();
                        if (classFileBase1.isProgramClass() && !trimProcessor1.isClassExcluded((ProgramClass) classFileBase1)) {
                            this.inheritedMethodReferrers.removeValue(abstractMethodInfo, classFileBase1);
                        }
                    }
                }
            }
        }

        HashSet hashSet = ZkmUtils.createHashSet();

        for (MethodInfo methodInfo1 : this.methods) {
            methodInfo1.collectUsedBootstrapMethods(hashSet);
        }

        UniqueWorkQueue uniqueWorkQueue = new UniqueWorkQueue();
        uniqueWorkQueue.enqueueAll(hashSet);

        while (!uniqueWorkQueue.isEmpty()) {
            BootstrapMethodEntry bootstrapMethodEntry = (BootstrapMethodEntry) uniqueWorkQueue.dequeue();
            Set set3 = bootstrapMethodEntry.getNestedBootstrapEntries();
            Iterator iterator2 = set3.iterator();

            while (iterator2.hasNext()) {
                BootstrapMethodEntry bootstrapMethodEntry1 = (BootstrapMethodEntry) iterator2.next();
                if (hashSet.add(bootstrapMethodEntry1)) {
                    uniqueWorkQueue.enqueue(bootstrapMethodEntry1);
                }
            }
        }

        this.pruneBootstrapMethods(hashSet);
        UsedConstantsCollector usedConstantsCollector1 = new UsedConstantsCollector(this.constantPool.getEntryCount());
        UsedConstantsCollector usedConstantsCollector = usedConstantsCollector1;
        this.collectUsedConstants('\u0000', 1103830477, usedConstantsCollector, '鞍');
        this.constantPool.removeUnusedEntries(usedConstantsCollector1);
    }

    public void renameFields(
            FieldNameAssigner fieldNameAssigner,
            TwoKeyMap twoKeyMap,
            TwoKeyMap twoKeyMap1,
            Map map1,
            ListMultimap listMultimap,
            ListMultimap listMultimap1,
            NameExclusionSet nameExclusionSet,
            ChangeLogMapping changeLogMapping1,
            HashMap hashMap,
            Map map2,
            HashMap hashMap1,
            SetMultiMap setMultiMap,
            TwoKeyMap twoKeyMap2,
            Map map3,
            BooleanFlag booleanFlag,
            ClassHierarchyQuery classHierarchyQuery,
            boolean bl
    ) throws ZkmException, IOException {
        HashMap hashMap2 = null;
        HashMap hashMap3 = ZkmUtils.createHashMap();
        Enumeration enumeration = this.enumerateInheritedReferencedFields();
        if (enumeration != null) {
            while (enumeration.hasMoreElements()) {
                AbstractFieldInfo abstractFieldInfo = (AbstractFieldInfo) enumeration.nextElement();
                if (abstractFieldInfo.isProgramMember() && !hashMap1.containsKey(abstractFieldInfo)) {
                    ClassFileBase classFileBase = abstractFieldInfo.getOwningClass();
                    if (!classFileBase.isInterface() || !classHierarchyQuery.implementsInterface(classFileBase.getClassName(), this.getClassName())) {
                        booleanFlag.setValue(true);
                        ClassFileBase classFileBase1 = abstractFieldInfo.getOwningClass();
                        ClassFileBase classFileBase2 = this.findInheritedFieldReferrer(abstractFieldInfo);

                        for (int fieldIndex = 0; fieldIndex < this.fieldCount; fieldIndex++) {
                            twoKeyMap2.putValue(classFileBase1, this.fields[fieldIndex], classFileBase2);
                        }
                    }
                }
            }
        }

        for (int fieldIndexValue = 0; fieldIndexValue < this.fieldCount; fieldIndexValue++) {
            String sourceName = this.fields[fieldIndexValue].getSourceName();
            FieldSignature fieldSignature2 = this.fields[fieldIndexValue].getSignature();
            if (map2 != null && map2.containsKey(this.fields[fieldIndexValue])) {
                if (hashMap2 == null) {
                    hashMap2 = ZkmUtils.createHashMap(ZkmUtils.getPrimeCapacity(this.fieldCount));
                }

                String nameValue2 = (String) map2.get(this.fields[fieldIndexValue]);
                if (nameValue2.equals(sourceName)) {
                    hashMap3.put(sourceName, this.fields[fieldIndexValue]);
                    map1.put(sourceName, this.fields[fieldIndexValue]);
                    listMultimap1.addValue(this.fields[fieldIndexValue].getDescriptorKey(), sourceName);
                    hashMap2.put(this.fields[fieldIndexValue], sourceName);
                } else {
                    boolean bl1 = true;
                    enumeration = this.enumerateInheritedReferencedFields();
                    if (enumeration != null) {
                        while (enumeration.hasMoreElements()) {
                            AbstractFieldInfo abstractFieldInfo1 = (AbstractFieldInfo) enumeration.nextElement();
                            ClassFileBase classFileBase3 = abstractFieldInfo1.getOwningClass();
                            ClassFileBase classFileBase4 = this.findInheritedFieldReferrer(abstractFieldInfo1);
                            String string = (String) hashMap1.get(abstractFieldInfo1);
                            if (string != null && string.equals(nameValue2)) {
                                bl1 = false;
                                String string1 = ClassFileBase.toDottedName((String) ZkmUtils.mapOrSelf(this.getClassName(), hashMap));
                                String string2 = ClassFileBase.toDottedName((String) ZkmUtils.mapOrSelf(classFileBase4.getClassName(), hashMap));
                                String string3 = ClassFileBase.toDottedName((String) ZkmUtils.mapOrSelf(classFileBase3.getClassName(), hashMap));
                                FieldSignature fieldSignature = (FieldSignature) twoKeyMap1.getValue(classFileBase3.getClassName(), abstractFieldInfo1.getSignature());
                                String name;
                                if (fieldSignature != null) {
                                    name = fieldSignature.getName();
                                } else {
                                    name = nameValue2;
                                }

                                changeLogMapping1.logWarning(
                                        "Field '"
                                                + sourceName
                                                + "' in class '"
                                                + string1
                                                + "' could not be renamed to '"
                                                + nameValue2
                                                + "' because symbolic references to field '"
                                                + string3
                                                + "."
                                                + name
                                                + "' appear as '"
                                                + string2
                                                + "."
                                                + name
                                                + "' and '"
                                                + string3
                                                + "."
                                                + name
                                                + "' is renamed to '"
                                                + nameValue2
                                                + "'. You may need to distribute this application as a whole. (G)"
                                );
                                changeLogMapping1.removeFieldMapping(this.getClassName(), fieldSignature2);
                                String string14 = (String) map2.remove(this.fields[fieldIndexValue]);
                                break;
                            }
                        }
                    }

                    if (bl1) {
                        Map map4 = twoKeyMap2.getInnerMap(this);
                        if (map4 != null) {
                            Iterator iterator = map4.entrySet().iterator();

                            while (iterator.hasNext()) {
                                Entry entry = (Entry) iterator.next();
                                FieldInfo fieldInfo1 = (FieldInfo) entry.getKey();
                                ProgramClass programClass2 = fieldInfo1.getProgramClass();
                                String string9 = (String) hashMap1.get(fieldInfo1);
                                if (string9 != null && string9.equals(nameValue2)) {
                                    bl1 = false;
                                    String string10 = ClassFileBase.toDottedName((String) ZkmUtils.mapOrSelf(this.getClassName(), hashMap));
                                    String classNameValue = programClass2.getClassName();
                                    String string12 = ClassFileBase.toDottedName((String) ZkmUtils.mapOrSelf(classNameValue, hashMap));
                                    FieldSignature fieldSignature1 = (FieldSignature) twoKeyMap1.getValue(classNameValue, fieldInfo1.getSignature());
                                    String nameValue;
                                    if (fieldSignature1 != null) {
                                        nameValue = fieldSignature1.getName();
                                    } else {
                                        nameValue = nameValue2;
                                    }

                                    String string6 = ClassFileBase.toDottedName((String) ZkmUtils.mapOrSelf(((ProgramClass) entry.getValue()).getClassName(), hashMap));
                                    changeLogMapping1.logWarning(
                                            "Field '"
                                                    + sourceName
                                                    + "' in class '"
                                                    + string10
                                                    + "' could not be renamed to '"
                                                    + nameValue2
                                                    + "' because symbolic references to it appearing as '"
                                                    + string6
                                                    + "."
                                                    + sourceName
                                                    + "' could be masked by references to field '"
                                                    + string12
                                                    + "."
                                                    + nameValue
                                                    + "' which is renamed to '"
                                                    + nameValue2
                                                    + "'. You may need to distribute this application as a whole. (H)"
                                    );
                                    changeLogMapping1.removeFieldMapping(this.getClassName(), fieldSignature2);
                                    String string13 = (String) map2.remove(this.fields[fieldIndexValue]);
                                    break;
                                }
                            }
                        }
                    }

                    if (bl1) {
                        hashMap3.put(nameValue2, this.fields[fieldIndexValue]);
                        map1.put(nameValue2, this.fields[fieldIndexValue]);
                        listMultimap1.addValue(this.fields[fieldIndexValue].getDescriptorKey(), nameValue2);
                        hashMap2.put(this.fields[fieldIndexValue], nameValue2);
                    }
                }
            } else if (nameExclusionSet.isFieldExcluded(this.fields[fieldIndexValue])) {
                hashMap3.put(sourceName, this.fields[fieldIndexValue]);
                map1.put(sourceName, this.fields[fieldIndexValue]);
                listMultimap1.addValue(this.fields[fieldIndexValue].getDescriptorKey(), sourceName);
            }
        }

        ClassHierarchyNode classHierarchyNode = ClassHierarchyNode.findNode(this.getClassName());
        ArrayList arrayList = new ArrayList(this.fieldCount);

        for (int fieldIndexValue2 = 0; fieldIndexValue2 < this.fieldCount; fieldIndexValue2++) {
            arrayList.add(this.fields[fieldIndexValue2]);
        }

        if (bl || HiddenOptionFlags.RANDOMIZE_OBFUSCATION) {
            ZkmUtils.shuffleList(arrayList, ZkmUtils.createRandom(256));
        }

        for (int elementIndex = 0; elementIndex < this.fieldCount; elementIndex++) {
            FieldInfo fieldInfo = (FieldInfo) arrayList.get(elementIndex);
            fieldInfo.assignNewName(
                    classHierarchyNode,
                    fieldNameAssigner,
                    twoKeyMap,
                    twoKeyMap1,
                    map1,
                    listMultimap,
                    listMultimap1,
                    hashMap3,
                    hashMap2,
                    this.getClassName(),
                    nameExclusionSet,
                    map2,
                    hashMap1,
                    hashMap,
                    setMultiMap,
                    twoKeyMap2,
                    map3
            );
        }
    }

    public void assignLongValueLocations(
            PairMultiMap pairMultiMap,
            Map map1,
            PairMultiMap pairMultiMap1,
            SetMultiMap setMultiMap,
            Map map2,
            ResolvedFieldRef resolvedFieldRef,
            ResolvedInvokeDynamic resolvedInvokeDynamic,
            boolean bl,
            boolean bl1,
            ResolvedMethodRef resolvedMethodRef,
            ObservableHolder[] observableHolders,
            LocalVariableAllocator localVariableAllocator
    ) {
        int observableHolderIndex = 0;
        Iterator iterator = setMultiMap.entrySet().iterator();

        while (iterator.hasNext()) {
            Entry entry = (Entry) iterator.next();
            ObservableHolder observableHolder = (ObservableHolder) entry.getKey();
            observableHolder.getValue();
            ObfuscatedReferenceSlot obfuscatedReferenceSlot = (ObfuscatedReferenceSlot) map1.get(observableHolder);
            if (obfuscatedReferenceSlot == null) {
                if (resolvedFieldRef != null) {
                    if (bl) {
                        observableHolders[observableHolderIndex] = observableHolder;
                        if (resolvedInvokeDynamic != null) {
                            int index = observableHolderIndex++;
                            ResolvedInvokeDynamic resolvedInvokeDynamic1 = resolvedInvokeDynamic;
                            obfuscatedReferenceSlot = new ObfuscatedReferenceSlot(resolvedInvokeDynamic1, index);
                        } else if (resolvedMethodRef != null) {
                            if (bl1) {
                                int bi = observableHolderIndex++;
                                ResolvedMethodRef resolvedMethodRef1 = resolvedMethodRef;
                                int indexValue = bi;
                                obfuscatedReferenceSlot = new ObfuscatedReferenceSlot(indexValue, resolvedMethodRef1);
                            } else {
                                int indexValue2 = observableHolderIndex++;
                                ResolvedMethodRef resolvedMethodRef2 = resolvedMethodRef;
                                obfuscatedReferenceSlot = new ObfuscatedReferenceSlot(resolvedMethodRef2, indexValue2);
                            }
                        } else {
                            int indexValue3 = observableHolderIndex++;
                            ResolvedFieldRef resolvedFieldRef1 = resolvedFieldRef;
                            obfuscatedReferenceSlot = new ObfuscatedReferenceSlot(resolvedFieldRef1, indexValue3);
                        }
                    } else {
                        obfuscatedReferenceSlot = new ObfuscatedReferenceSlot(resolvedFieldRef, -1);
                    }

                    map1.put(observableHolder, obfuscatedReferenceSlot);
                } else {
                    if (bl) {
                        observableHolders[observableHolderIndex] = observableHolder;
                    }

                    int value = localVariableAllocator.getValue();
                    int indexValue4 = bl ? observableHolderIndex++ : -1;
                    int valueValue = value;
                    obfuscatedReferenceSlot = new ObfuscatedReferenceSlot(valueValue, indexValue4);
                    map1.put(observableHolder, obfuscatedReferenceSlot);
                }
            }
        }

        iterator = pairMultiMap1.entrySet().iterator();

        while (iterator.hasNext()) {
            Entry entry1 = (Entry) iterator.next();
            MethodInfo methodInfo1 = (MethodInfo) entry1.getKey();
            Iterator iterator1 = ((List) entry1.getValue()).iterator();

            while (iterator1.hasNext()) {
                ObjectPair objectPair = (ObjectPair) iterator1.next();
                ConstantLong constantLong = (ConstantLong) objectPair.getFirst();
                RankedValue rankedValue = (RankedValue) objectPair.getSecond();
                ObservableHolder observableHolder1 = (ObservableHolder) map2.get(constantLong);
                ObfuscatedReferenceSlot obfuscatedReferenceSlot1 = (ObfuscatedReferenceSlot) map1.get(observableHolder1);
                pairMultiMap.addPair(methodInfo1, rankedValue, obfuscatedReferenceSlot1);
            }
        }
    }

    public ClassFileBase findInheritedMethodReferrer(Object key) {
        if (this.inheritedMethodReferrers == null) {
            return null;
        }

        Set set1 = this.inheritedMethodReferrers.getValues(key);
        return set1 != null ? (ClassFileBase) set1.iterator().next() : null;
    }

    public void prepareLongConstantFields(List list1, Map map1, Map map2, List list2, List list3) {
        Iterator iterator = list1.iterator();

        while (iterator.hasNext()) {
            ObjectPair objectPair = (ObjectPair) iterator.next();
            ConstantLong constantLong = (ConstantLong) objectPair.getFirst();
            FieldInfo fieldInfo = (FieldInfo) objectPair.getSecond();
            fieldInfo.removeConstantValueAttribute();
            ResolvedFieldRef resolvedFieldRef = this.constantPool
                    .getOrCreateFieldRef(this.getClassName(), fieldInfo.getSourceName(), fieldInfo.getDescriptor(), list3, fieldInfo);
            map2.put(resolvedFieldRef, map1.get(constantLong));
        }

        iterator = list2.iterator();

        while (iterator.hasNext()) {
            FieldInfo fieldInfo1 = (FieldInfo) ((ObjectPair) iterator.next()).getSecond();
            boolean bl = false;

            for (int methodIndex = 0; methodIndex < this.methodCount; methodIndex++) {
                MethodInfo methodInfo1 = this.methods[methodIndex];
                if (methodInfo1.isConstructor() && methodInfo1.storesToField(fieldInfo1)) {
                    bl = true;
                    break;
                }
            }

            if (bl) {
                fieldInfo1.removeConstantValueAttribute();
            }
        }
    }

    public void countSubclassConstants(CountingBag countingBag, ClassHierarchyQuery classHierarchyQuery) throws ZkmException, IOException {
        Iterator iterator = this.constantPool.findSubclassConstants(classHierarchyQuery).iterator();

        while (iterator.hasNext()) {
            String classNameValue = ((ResolvedClassConstant) iterator.next()).getClassName();
            countingBag.add(classNameValue);
        }
    }

    @Override
    public String formatDeclaration() throws ZkmProcessingException, IOException {
        StringBuffer stringBuffer = new StringBuffer();
        Enumeration enumeration = this.enumerateAnnotationTypes();

        while (enumeration.hasMoreElements()) {
            stringBuffer.append('@');
            stringBuffer.append(ClassFileBase.toDottedName((String) enumeration.nextElement()));
            stringBuffer.append(HiddenOptionFlags.LINE_SEPARATOR);
        }

        stringBuffer.append(this.accessFlags.getModifierString());
        stringBuffer.append(this.accessFlags.isInterface() ? "interface " : "class ");
        stringBuffer.append(super.thisClassConstant.getDottedClassName());
        stringBuffer.append(" ");
        stringBuffer.append(this.getExtendsClause() + this.getImplementsClause() + " {");
        stringBuffer.append(HiddenOptionFlags.LINE_SEPARATOR);

        for (int fieldIndex = 0; fieldIndex < this.fieldCount; fieldIndex++) {
            if (this.fields[fieldIndex].isValid()) {
                stringBuffer.append(HiddenOptionFlags.LINE_SEPARATOR);
                Enumeration enumeration1 = this.fields[fieldIndex].enumerateAnnotationTypes();

                while (enumeration1.hasMoreElements()) {
                    stringBuffer.append("   ");
                    stringBuffer.append('@');
                    stringBuffer.append(ClassFileBase.toDottedName((String) enumeration1.nextElement()));
                    stringBuffer.append(HiddenOptionFlags.LINE_SEPARATOR);
                }

                stringBuffer.append("   ");
                stringBuffer.append(this.fields[fieldIndex].getModifierString());
                stringBuffer.append(ConstantPoolEntry.descriptorToJavaType(this.fields[fieldIndex].getDescriptor()));
                stringBuffer.append(" ");
                stringBuffer.append(this.fields[fieldIndex].getSourceName());
                stringBuffer.append(HiddenOptionFlags.LINE_SEPARATOR);
            }
        }

        for (int methodIndex = 0; methodIndex < this.methodCount; methodIndex++) {
            if (this.methods[methodIndex].isValid()) {
                stringBuffer.append(HiddenOptionFlags.LINE_SEPARATOR);
                Enumeration enumeration2 = this.methods[methodIndex].enumerateAnnotationTypes();

                while (enumeration2.hasMoreElements()) {
                    stringBuffer.append("   ");
                    stringBuffer.append('@');
                    stringBuffer.append(ClassFileBase.toDottedName((String) enumeration2.nextElement()));
                    stringBuffer.append(HiddenOptionFlags.LINE_SEPARATOR);
                }

                stringBuffer.append("   ");
                stringBuffer.append(this.methods[methodIndex].buildDeclaration(false));
                stringBuffer.append(this.methods[methodIndex].getThrowsClause());
                stringBuffer.append(';');
                stringBuffer.append(HiddenOptionFlags.LINE_SEPARATOR);
            }
        }

        stringBuffer.append("}");
        stringBuffer.append(HiddenOptionFlags.LINE_SEPARATOR);
        return stringBuffer.toString();
    }

    public void updateLocalVariableTypes(
            CommonSuperTypeResolver commonSuperTypeResolver1, ClassHierarchyQuery classHierarchyQuery, ScriptEnvironment scriptEnvironment1
    ) throws ZkmException, IOException {
        ArrayList arrayList = new ArrayList();

        for (int methodIndex = 0; methodIndex < this.methodCount; methodIndex++) {
            try {
                this.methods[methodIndex].updateLocalVariableTypes(this.constantPool, arrayList, commonSuperTypeResolver1, classHierarchyQuery);
            } catch (MethodAnalysisException methodAnalysisException) {
                scriptEnvironment1.logWarning(
                        "Method could not be analyzed in class '"
                                + this.getOwningClass().getLocationName()
                                + "' while attempting to update local variable types : '"
                                + methodAnalysisException.getMessage()
                                + "'",
                        true
                );
            } catch (StackAnalysisException stackAnalysisException) {
                throw new ZkmProcessingException(this.getLocationName() + " " + this.methods[methodIndex].toDisplayString() + " : " + stackAnalysisException.getMessage());
            }
        }

        this.constantPool.appendEntries(arrayList);
    }

    public void assignStringValueLocations(
            PairMultiMap pairMultiMap,
            Map map1,
            PairMultiMap pairMultiMap1,
            SetMultiMap setMultiMap,
            Map map2,
            ResolvedFieldRef resolvedFieldRef,
            boolean bl,
            boolean bl1,
            ResolvedInvokeDynamic resolvedInvokeDynamic,
            ResolvedMethodRef resolvedMethodRef,
            ObservableHolder[] observableHolders,
            LocalVariableAllocator localVariableAllocator
    ) {
        int observableHolderIndex = 0;
        Iterator iterator = setMultiMap.entrySet().iterator();

        while (iterator.hasNext()) {
            Entry entry = (Entry) iterator.next();
            ObservableHolder observableHolder = (ObservableHolder) entry.getKey();
            String valueValue = (String) observableHolder.getValue();
            EncryptedStringLocation encryptedStringLocation = (EncryptedStringLocation) map1.get(observableHolder);
            if (encryptedStringLocation == null) {
                if (resolvedFieldRef != null) {
                    if (bl) {
                        observableHolders[observableHolderIndex] = observableHolder;
                        if (resolvedInvokeDynamic != null) {
                            int stringIndex = observableHolderIndex++;
                            ResolvedInvokeDynamic resolvedInvokeDynamic1 = resolvedInvokeDynamic;
                            encryptedStringLocation = new EncryptedStringLocation(resolvedInvokeDynamic1, stringIndex);
                        } else if (resolvedMethodRef != null) {
                            if (bl1) {
                                int bh = observableHolderIndex++;
                                ResolvedMethodRef resolvedMethodRef1 = resolvedMethodRef;
                                int stringIndexValue = bh;
                                encryptedStringLocation = new EncryptedStringLocation(stringIndexValue, resolvedMethodRef1);
                            } else {
                                int stringIndexValue2 = observableHolderIndex++;
                                ResolvedMethodRef resolvedMethodRef2 = resolvedMethodRef;
                                encryptedStringLocation = new EncryptedStringLocation(resolvedMethodRef2, stringIndexValue2);
                            }
                        } else {
                            int stringIndexValue3 = observableHolderIndex++;
                            ResolvedFieldRef resolvedFieldRef1 = resolvedFieldRef;
                            encryptedStringLocation = new EncryptedStringLocation(resolvedFieldRef1, stringIndexValue3);
                        }
                    } else {
                        encryptedStringLocation = new EncryptedStringLocation(resolvedFieldRef, -1);
                    }

                    map1.put(observableHolder, encryptedStringLocation);
                } else {
                    if (bl) {
                        observableHolders[observableHolderIndex] = observableHolder;
                    }

                    int value = localVariableAllocator.getValue();
                    int stringIndexValue4 = bl ? observableHolderIndex++ : -1;
                    int valueValue2 = value;
                    encryptedStringLocation = new EncryptedStringLocation(valueValue2, stringIndexValue4);
                    map1.put(observableHolder, encryptedStringLocation);
                }
            }
        }

        iterator = pairMultiMap1.entrySet().iterator();

        while (iterator.hasNext()) {
            Entry entry1 = (Entry) iterator.next();
            MethodInfo methodInfo1 = (MethodInfo) entry1.getKey();
            Iterator iterator1 = ((List) entry1.getValue()).iterator();

            while (iterator1.hasNext()) {
                ObjectPair objectPair = (ObjectPair) iterator1.next();
                ResolvedStringConstant resolvedStringConstant = (ResolvedStringConstant) objectPair.getFirst();
                RankedValue rankedValue = (RankedValue) objectPair.getSecond();
                ObservableHolder observableHolder1 = (ObservableHolder) map2.get(resolvedStringConstant);
                EncryptedStringLocation encryptedStringLocation1 = (EncryptedStringLocation) map1.get(observableHolder1);
                pairMultiMap.addPair(methodInfo1, rankedValue, encryptedStringLocation1);
            }
        }
    }

    public void removeLocalVariableTables() {
        for (int methodIndex = 0; methodIndex < this.methodCount; methodIndex++) {
            this.methods[methodIndex].removeLocalVariableTables();
        }
    }

    public LabelInstruction createDecryptSubroutineLabel(ObservableHolder observableHolder, ObservableHolder observableHolder1, Map map1) throws ZkmException, IOException {
        LabelInstruction labelInstruction = new LabelInstruction(true, 1);
        MutableInt mutableInt = new MutableInt(-2);
        map1.put(labelInstruction, mutableInt);
        observableHolder.setValue(labelInstruction);
        observableHolder1.setValue(mutableInt);
        return labelInstruction;
    }

    @Override
    public void remapClassNames(int ba, int bb, HashMap hashMap, Object object, Object object1, HashMap hashMap1, MessageReporter messageReporter1) throws ZkmException, IOException {
        HashSet hashSet = (HashSet) object;
        Set set1 = (Set) object1;
        this.constantPool.applyClassRenames(hashMap, messageReporter1);

        for (int methodIndex = 0; methodIndex < this.methodCount; methodIndex++) {
            this.methods[methodIndex].remapClassNames(ba, bb, hashMap1, hashMap);
        }

        for (int fieldIndex = 0; fieldIndex < this.fieldCount; fieldIndex++) {
            this.fields[fieldIndex].remapClassNames(ba, bb, hashMap1, hashMap);
        }

        this.trimInnerClassesAttribute();

        for (int attributeIndex = 0; attributeIndex < this.attributes.length; attributeIndex++) {
            Attribute attribute = this.attributes[attributeIndex];
            HashMap hashMap3 = hashMap;
            HashMap hashMap2 = hashMap1;
            Integer integer = bb;
            attribute.remapClassNames(ba, integer, hashMap2, hashMap3);
        }

        this.constantPool.remapResourceNameStrings(hashMap, hashSet, set1, hashMap1);
        this.constantPool.remapDescriptorStrings(hashMap);
    }

    @Override
    public void collectUsedConstants(char controlTag, int controlSeed, UsedConstantsCollector collector, char controlMode) {
        ClassFileComponent.getFlowGuardNodes();
        this.accessFlags.collectUsedConstants('\u0000', 1103830477, collector, '鞍');
        if (super.thisClassConstant != null && controlSeed > 0 && !super.thisClassConstant.isUnresolved()) {
            ((ResolvedClassConstant) super.thisClassConstant).registerUsage(collector, this, ClassReferenceRole.THIS_CLASS);
        }

        if (this.superClassConstant != null && !this.superClassConstant.isUnresolved()) {
            ((ResolvedClassConstant) this.superClassConstant).registerUsage(collector, this, ClassReferenceRole.SUPER_CLASS);
        }

        for (ClassConstantBase interfaceClass : super.interfaces) {
            if (!interfaceClass.isUnresolved()) {
                ((ResolvedClassConstant) interfaceClass).registerUsage(collector, this, ClassReferenceRole.INTERFACE_ENTRY);
            }
        }

        for (Attribute attribute : this.attributes) {
            attribute.collectUsedConstants('\u0000', 1103830477, collector, '鞍');
        }

        for (FieldInfo fieldInfo : this.fields) {
            fieldInfo.collectUsedConstants('\u0000', 1103830477, collector, '鞍');
        }

        for (MethodInfo method : this.methods) {
            method.collectUsedConstants('\u0000', 1103830477, collector, '鞍');
        }
    }

    public void removeLineNumberTables() {
        if (this.hasLineNumberTables()) {
            for (int methodIndex = 0; methodIndex < this.methodCount; methodIndex++) {
                this.methods[methodIndex].removeLineNumberTable();
            }

            this.setHasLineNumberTables(false);
        }
    }

    public synchronized void addMethod(MethodInfo methodInfo1, ClassMemberLookup classMemberLookup1) {
        MethodInfo[] methodInfos = new MethodInfo[this.methodCount + 1];
        System.arraycopy(this.methods, 0, methodInfos, 0, this.methodCount);
        methodInfos[this.methodCount] = methodInfo1;
        methodInfo1.setParent(this);
        this.methods = methodInfos;
        this.methodCount++;
        if (!this.isVersionedVariant()) {
            classMemberLookup1.registerMethod(methodInfo1, this);
        }
    }

    public final void addInheritedFieldReferrer(Object key, Object object1) {
        if (this.inheritedFieldReferrers == null) {
            this.inheritedFieldReferrers = new SetMultiMap(10, 5);
        }

        this.inheritedFieldReferrers.addValue(key, object1);
    }

    public void removeMethods(EnumerableMap enumerableMap) {
        ArrayList arrayList = new ArrayList();

        for (int methodIndex = 0; methodIndex < this.methods.length; methodIndex++) {
            if (!enumerableMap.containsKey(this.methods[methodIndex])) {
                arrayList.add(this.methods[methodIndex]);
            }
        }

        this.methods = ((com.zelix.klassmaster.classfile.MethodInfo[]) (arrayList.toArray(new MethodInfo[arrayList.size()])));
        this.methodCount = this.methods.length;
    }

    public LabelInstruction createDecryptSubroutineLabel(
            ObservableHolder observableHolder, ObservableHolder observableHolder1, JsrGotoKind jsrGotoKind, Map map1
    ) throws ZkmException, IOException {
        MutableInt mutableInt = null;
        LabelInstruction labelInstruction = new LabelInstruction(true, 1);
        if (jsrGotoKind == JsrGotoKind.GOTO) {
            mutableInt = new MutableInt(-2);
            map1.put(labelInstruction, mutableInt);
        }

        observableHolder.setValue(labelInstruction);
        observableHolder1.setValue(mutableInt);
        return labelInstruction;
    }
}

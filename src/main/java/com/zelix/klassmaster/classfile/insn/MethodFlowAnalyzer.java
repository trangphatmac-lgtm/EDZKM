package com.zelix.klassmaster.classfile.insn;

import com.zelix.klassmaster.classfile.AbstractFieldInfo;
import com.zelix.klassmaster.classfile.AbstractMethodInfo;
import com.zelix.klassmaster.classfile.FieldInfo;
import com.zelix.klassmaster.classfile.MethodInfo;
import com.zelix.klassmaster.classfile.attribute.CodeAttributeBody;
import com.zelix.klassmaster.classfile.attribute.ExceptionRangeField;
import com.zelix.klassmaster.classfile.attribute.ExceptionTableEntry;
import com.zelix.klassmaster.classfile.attribute.StackMapAttribute;
import com.zelix.klassmaster.classfile.attribute.StackMapEntry;
import com.zelix.klassmaster.classfile.constpool.ConstantPool;
import com.zelix.klassmaster.classfile.constpool.ConstantPoolEntry;
import com.zelix.klassmaster.classfile.constpool.ResolvedClassConstant;
import com.zelix.klassmaster.classfile.constpool.ResolvedFieldRef;
import com.zelix.klassmaster.classfile.constpool.ResolvedMethodRef;
import com.zelix.klassmaster.classfile.constpool.ResolvedMethodRefConstant;
import com.zelix.klassmaster.classfile.constpool.ResolvedMethodType;
import com.zelix.klassmaster.classfile.constpool.ResolvedStringConstant;
import com.zelix.klassmaster.classfile.hierarchy.ClassHierarchyQuery;
import com.zelix.klassmaster.classfile.hierarchy.ClassMemberLookup;
import com.zelix.klassmaster.classfile.hierarchy.CommonSuperTypeResolver;
import com.zelix.klassmaster.config.HiddenOptionFlags;
import com.zelix.klassmaster.exceptions.AssertionFailedException;
import com.zelix.klassmaster.exceptions.ClassFileLoadException;
import com.zelix.klassmaster.exceptions.MethodAnalysisException;
import com.zelix.klassmaster.exceptions.StackAnalysisException;
import com.zelix.klassmaster.exceptions.ZkmException;
import com.zelix.klassmaster.exceptions.ZkmProcessingException;
import com.zelix.klassmaster.obfuscator.reflection.ClassDescriptorStringValue;
import com.zelix.klassmaster.obfuscator.reflection.InstructionReasonNote;
import com.zelix.klassmaster.obfuscator.reflection.LiteralStringValue;
import com.zelix.klassmaster.obfuscator.reflection.NullReflectionValue;
import com.zelix.klassmaster.obfuscator.reflection.PendingReflectionValue;
import com.zelix.klassmaster.obfuscator.reflection.ReflectionAnalysisSwitchMap;
import com.zelix.klassmaster.obfuscator.reflection.ReflectionApiMethod;
import com.zelix.klassmaster.obfuscator.reflection.ReflectionArgumentKind;
import com.zelix.klassmaster.obfuscator.reflection.ReflectionParamDetail;
import com.zelix.klassmaster.obfuscator.reflection.ValueFlowTracker;
import com.zelix.klassmaster.util.BooleanFlag;
import com.zelix.klassmaster.util.EnumerationBackedList;
import com.zelix.klassmaster.util.GenericTree;
import com.zelix.klassmaster.util.IdentityMapWrapper;
import com.zelix.klassmaster.util.IntegerCache;
import com.zelix.klassmaster.util.ListMultimap;
import com.zelix.klassmaster.util.MutableInt;
import com.zelix.klassmaster.util.NestedMultiMap;
import com.zelix.klassmaster.util.NonNullList;
import com.zelix.klassmaster.util.ObjectPair;
import com.zelix.klassmaster.util.ObjectTreeNode;
import com.zelix.klassmaster.util.ObservableHolder;
import com.zelix.klassmaster.util.SetMultiMap;
import com.zelix.klassmaster.util.Triple;
import com.zelix.klassmaster.util.TwoKeyMap;
import com.zelix.klassmaster.util.UniqueWorkQueue;
import com.zelix.klassmaster.util.ZkmAssert;
import com.zelix.klassmaster.util.ZkmUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;

public class MethodFlowAnalyzer {
    public static final IntegerCache INTEGER_CACHE = IntegerCache.getInstance();
    private StackFrameState[] frameStates;
    public TwoKeyMap retReturnTargets;
    private BasicBlock[] blockByIndexCache;
    private List catchHandlerBlocks = new ArrayList();
    private List basicBlocks = new ArrayList();
    private ListMultimap tryCatchInfosByHandler = null;
    private TwoKeyMap handlerCoveredBlocks = null;
    private boolean closed = false;
    private MethodBytecode methodBytecode;
    private String methodDescription;
    private CommonSuperTypeResolver superTypeResolver;
    private ClassHierarchyQuery hierarchyQuery;
    private boolean isStatic;
    private boolean isConstructor;
    private NonNullList instructions;
    private ExceptionTableEntry[] exceptionTable;
    private int maxLocals;
    private boolean livenessAnalysisEnabled;
    private VerifierType[] initialLocals;
    private int parameterSlotCount;
    public boolean hasSubroutineCalls;
    private BasicBlock entryBlock;
    private List naturalLoops;
    private int shortestReturnPathLength;
    public boolean localLivenessConsistent;

    public void linkSubroutineReturns(BasicBlock basicBlock, BasicBlock basicBlock1, HashSet hashSet) {
        if (!hashSet.contains(basicBlock1)) {
            hashSet.add(basicBlock1);
            BasicBlock basicBlock4 = basicBlock1.getJsrReturnBlock();
            if (basicBlock4 != null) {
                this.linkSubroutineReturns(basicBlock, basicBlock4, hashSet);
            } else {
                Enumeration enumeration = basicBlock1.enumerateSuccessors();
                if (enumeration != null && enumeration.hasMoreElements()) {
                    while (enumeration.hasMoreElements()) {
                        BasicBlock basicBlock8 = (BasicBlock) enumeration.nextElement();
                        HashSet hashSet1 = hashSet;
                        BasicBlock basicBlock2 = basicBlock8;
                        BasicBlock basicBlock3 = basicBlock;
                        this.linkSubroutineReturns(basicBlock3, basicBlock2, hashSet1);
                    }
                } else {
                    Instruction instruction1 = (Instruction) this.instructions.get(basicBlock1.getEndIndex());
                    if (instruction1.isRet() && basicBlock1.getSubroutineEntry() == null) {
                        LocalVariableInstruction localVariableInstruction = (LocalVariableInstruction) instruction1;
                        Iterator iterator = basicBlock.getJsrCallers().iterator();

                        while (iterator.hasNext()) {
                            BasicBlock basicBlock5 = (BasicBlock) iterator.next();
                            if (this.retReturnTargets == null) {
                                this.retReturnTargets = new TwoKeyMap(13, 13);
                            }

                            Integer integer = INTEGER_CACHE.valueOf(basicBlock5.getJsrReturnBlock().getStartIndex());
                            this.retReturnTargets.putValue(localVariableInstruction, integer, integer);
                        }

                        basicBlock1.setSubroutineEntry(basicBlock);
                        basicBlock.addRetBlock(basicBlock1);
                    }
                }
            }

            Enumeration enumeration1 = basicBlock1.enumerateExceptionHandlers();

            while (enumeration1.hasMoreElements()) {
                BasicBlock basicBlock9 = (BasicBlock) enumeration1.nextElement();
                HashSet hashSet2 = hashSet;
                BasicBlock basicBlock6 = basicBlock9;
                BasicBlock basicBlock7 = basicBlock;
                this.linkSubroutineReturns(basicBlock7, basicBlock6, hashSet2);
            }
        }
    }

    public MethodFlowAnalyzer(
            NonNullList nonNullList,
            ExceptionTableEntry[] exceptionTableEntrys,
            int maxLocals,
            List list1,
            boolean isStatic,
            boolean isConstructor,
            CommonSuperTypeResolver commonSuperTypeResolver1,
            ClassHierarchyQuery classHierarchyQuery,
            MethodBytecode methodBytecode1,
            boolean livenessAnalysisEnabled
    ) throws ZkmException, IOException {
        this.methodBytecode = methodBytecode1;
        this.methodDescription = methodBytecode1.getQualifiedMethodName();
        this.superTypeResolver = commonSuperTypeResolver1;
        this.hierarchyQuery = classHierarchyQuery;
        this.isStatic = isStatic;
        this.isConstructor = isConstructor;
        this.instructions = nonNullList;
        this.exceptionTable = exceptionTableEntrys;
        this.maxLocals = maxLocals;
        this.livenessAnalysisEnabled = livenessAnalysisEnabled;
        MutableInt mutableInt = new MutableInt(0);

        try {
            this.initialLocals = createInitialLocals(maxLocals, this.isStatic, list1, this.isConstructor, methodBytecode1.getClassName(), mutableInt);
        } catch (ArrayIndexOutOfBoundsException arrayIndexOutOfBoundsException) {
            String string = "Method '"
                    + this.methodDescription
                    + "' appears to be corrupt. : Invalid max_locals="
                    + maxLocals
                    + " : "
                    + list1
                    + " : "
                    + (this.isStatic ? "static " : "instance ")
                    + arrayIndexOutOfBoundsException.getMessage();
            throw new StackAnalysisException(string);
        }

        this.parameterSlotCount = mutableInt.getValue();
        int bb = 0;
        int nonNullListSize = nonNullList.size();
        HashMap hashMap = ZkmUtils.createHashMap();
        ListMultimap listMultimap = new ListMultimap();

        for (int instructionIndexValue = 0; instructionIndexValue < nonNullListSize; instructionIndexValue++) {
            Instruction instruction1 = (Instruction) nonNullList.get(instructionIndexValue);
            if (instruction1 instanceof JumpingInstruction) {
                ((JumpingInstruction) instruction1).addSuccessorBlocks(hashMap, listMultimap, this.basicBlocks);
                if (instruction1.isJsr()) {
                    this.hasSubroutineCalls = true;
                }
            } else if (instruction1 instanceof LabelInstruction) {
                ((LabelInstruction) instruction1).setInstructionIndex(instructionIndexValue);
            }
        }

        TryCatchBlockInfo[] tryCatchBlockInfos;
        if (exceptionTableEntrys != null) {
            tryCatchBlockInfos = new TryCatchBlockInfo[exceptionTableEntrys.length];
            this.tryCatchInfosByHandler = new ListMultimap(exceptionTableEntrys.length);
            int bf = 0;

            for (int entryIndex = 0; entryIndex < exceptionTableEntrys.length; entryIndex++) {
                LabelInstruction labelInstruction = exceptionTableEntrys[entryIndex].getLabel(ExceptionRangeField.START_PC);
                LabelInstruction labelInstruction1 = exceptionTableEntrys[entryIndex].getLabel(ExceptionRangeField.END_PC);
                LabelInstruction labelInstruction2 = exceptionTableEntrys[entryIndex].getLabel(ExceptionRangeField.HANDLER_PC);
                BasicBlock basicBlock;
                if ((basicBlock = (BasicBlock) hashMap.get(labelInstruction2)) == null) {
                    basicBlock = new BasicBlock();
                    this.basicBlocks.add(basicBlock);
                    hashMap.put(labelInstruction2, basicBlock);
                    basicBlock.setLabel("catchNode" + bf++);
                    this.catchHandlerBlocks.add(basicBlock);
                } else if (!this.tryCatchInfosByHandler.containsKey(basicBlock) && !this.catchHandlerBlocks.contains(basicBlock)) {
                    this.catchHandlerBlocks.add(basicBlock);
                    basicBlock.setLabel("catchNode" + bf++);
                }

                tryCatchBlockInfos[entryIndex] = new TryCatchBlockInfo(
                        this,
                        entryIndex,
                        labelInstruction.getInstructionIndex(),
                        labelInstruction1.getInstructionIndex(),
                        labelInstruction2.getInstructionIndex(),
                        exceptionTableEntrys[entryIndex].getCatchTypeName(),
                        exceptionTableEntrys[entryIndex].isCatchAny(),
                        basicBlock,
                        exceptionTableEntrys[entryIndex]
                );
                this.tryCatchInfosByHandler.addValue(basicBlock, tryCatchBlockInfos[entryIndex]);
            }
        } else {
            tryCatchBlockInfos = new TryCatchBlockInfo[0];
        }

        int elementIndexValue2 = 0;

        for (Instruction instruction4 = (Instruction) nonNullList.get(elementIndexValue2); instruction4.isLabel(); instruction4 = (Instruction) nonNullList.get(++elementIndexValue2)) {
            BasicBlock basicBlock3 = (BasicBlock) hashMap.get(instruction4);
            if (basicBlock3 != null) {
                this.entryBlock = basicBlock3;
                break;
            }
        }

        if (this.entryBlock == null) {
            this.entryBlock = new BasicBlock();
            this.basicBlocks.add(this.entryBlock);
        }

        this.entryBlock.setLabel("header");
        BasicBlock basicBlock4 = this.entryBlock;
        basicBlock4.setStartIndex(0);
        LinkedHashSet linkedHashSet = new LinkedHashSet();

        for (int elementIndexValue3 = elementIndexValue2; elementIndexValue3 < nonNullListSize; elementIndexValue3++) {
            Instruction instruction5 = (Instruction) nonNullList.get(elementIndexValue3);
            if (instruction5 instanceof JumpingInstruction) {
                basicBlock4.setEndIndex(elementIndexValue3);
                List list3 = listMultimap.getValues((JumpingInstruction) instruction5);

                for (int elementIndex = 0; elementIndex < list3.size(); elementIndex++) {
                    basicBlock4.addSuccessor((BasicBlock) list3.get(elementIndex));
                }

                if (elementIndexValue3 + 1 < nonNullListSize) {
                    Instruction instruction3 = (Instruction) nonNullList.get(elementIndexValue3 + 1);
                    BasicBlock basicBlock8;
                    if (!(instruction3 instanceof LabelInstruction) || (basicBlock8 = (BasicBlock) hashMap.get(instruction3)) == null) {
                        basicBlock8 = new BasicBlock();
                        this.basicBlocks.add(basicBlock8);
                    }

                    if (basicBlock8.getLabel() == null) {
                        basicBlock8.setLabel("node" + bb++);
                    }

                    basicBlock8.setStartIndex(elementIndexValue3 + 1);
                    if (instruction5.canFallThrough()) {
                        basicBlock4.addSuccessor(basicBlock8);
                    } else if (instruction5.isJsr()) {
                        basicBlock4.setJsrReturnBlock(basicBlock8);
                        BasicBlock basicBlock2 = (BasicBlock) list3.get(0);
                        basicBlock2.addJsrCaller(basicBlock4);
                        linkedHashSet.add(basicBlock2);
                    }

                    basicBlock4 = basicBlock8;
                }
            } else if (instruction5 instanceof LabelInstruction) {
                BasicBlock basicBlock6 = (BasicBlock) hashMap.get(instruction5);
                if (basicBlock6 != null && basicBlock4 != basicBlock6) {
                    Instruction instruction6 = (Instruction) nonNullList.get(elementIndexValue3 - 1);
                    basicBlock4.setEndIndex(elementIndexValue3 - 1);
                    basicBlock6.setStartIndex(elementIndexValue3);
                    if (basicBlock6.getLabel() == null) {
                        basicBlock6.setLabel("node" + bb++);
                    }

                    if (instruction6.canFallThrough()) {
                        basicBlock4.addSuccessor(basicBlock6);
                    }

                    basicBlock4 = basicBlock6;
                }
            } else if (instruction5.isExit()) {
                basicBlock4.setEndIndex(elementIndexValue3);
                if (elementIndexValue3 + 1 < nonNullListSize) {
                    Instruction instruction2 = (Instruction) nonNullList.get(elementIndexValue3 + 1);
                    BasicBlock basicBlock1;
                    if (!(instruction2 instanceof LabelInstruction) || (basicBlock1 = (BasicBlock) hashMap.get(instruction2)) == null) {
                        basicBlock1 = new BasicBlock();
                        this.basicBlocks.add(basicBlock1);
                    }

                    if (basicBlock1.getLabel() == null) {
                        basicBlock1.setLabel("node" + bb++);
                    }

                    basicBlock1.setStartIndex(elementIndexValue3 + 1);
                    basicBlock4 = basicBlock1;
                }
            }
        }

        Collections.sort(this.catchHandlerBlocks);
        Collections.sort(this.basicBlocks);

        for (int elementIndexValue = 0; elementIndexValue < this.basicBlocks.size(); elementIndexValue++) {
            BasicBlock basicBlock5 = (BasicBlock) this.basicBlocks.get(elementIndexValue);
            if (basicBlock5.getStartIndex() > basicBlock5.getEndIndex()) {
                if (elementIndexValue < this.basicBlocks.size() - 1) {
                    BasicBlock basicBlock7 = (BasicBlock) this.basicBlocks.get(elementIndexValue + 1);
                    basicBlock5.setEndIndex(basicBlock7.getStartIndex() - 1);
                } else {
                    basicBlock5.setEndIndex(nonNullListSize - 1);
                }
            }
        }

        TryCatchBlockInfo[][] tryCatchBlockInfos1 = new TryCatchBlockInfo[nonNullListSize][];
        if (this.catchHandlerBlocks.size() > 0) {
            this.handlerCoveredBlocks = new TwoKeyMap(this.catchHandlerBlocks.size());
            this.assignExceptionHandlers(tryCatchBlockInfos1, tryCatchBlockInfos);
        }

        List list2 = this.orderSubroutines(linkedHashSet);
        Iterator iterator = list2.iterator();

        while (iterator.hasNext()) {
            BasicBlock basicBlock9 = (BasicBlock) iterator.next();
            this.linkSubroutineReturns(basicBlock9, basicBlock9, ZkmUtils.createHashSet(13));
            if (!basicBlock9.hasRetBlocks()) {
                List list4 = basicBlock9.getJsrCallers();
                Iterator iterator1 = list4.iterator();

                while (iterator1.hasNext()) {
                    ((BasicBlock) iterator1.next()).setJsrReturnBlock((BasicBlock) null);
                }
            }
        }

        this.removeUnreachableBlocks(this.handlerCoveredBlocks);

        try {
            this.naturalLoops = this.findNaturalLoops();
        } catch (StackOverflowError stackOverflowError2) {
            throw new ZkmProcessingException("StackOverflowError : Try increasing the thread stack size (-Xss) (A)");
        }

        try {
            this.shortestReturnPathLength = this.computeShortestReturnPath();
        } catch (StackOverflowError stackOverflowError1) {
            throw new ZkmProcessingException(
                    "StackOverflowError : Try increasing the thread stack size (-Xss) (B) : '"
                            + methodBytecode1.getOriginalDottedName()
                            + "' '"
                            + methodBytecode1.getMethodName()
                            + "'"
            );
        }

        if (methodBytecode1.getOwningClass().hasReleaseVersion()) {
            commonSuperTypeResolver1.setThreadVersion(methodBytecode1.getOwningClass().getReleaseVersion());
        } else {
            commonSuperTypeResolver1.clearThreadVersion();
        }

        String diagnosticContext = "analyzing the control flow of method '"
                + methodBytecode1.getMethod().getOriginalNameWithParameters()
                + "' in class '"
                + methodBytecode1.getLocationName()
                + "' : '"
                + methodBytecode1.getClassName()
                + "' (A)";

        try {
            this.computeFrameStates(
                    commonSuperTypeResolver1,
                    tryCatchBlockInfos1,
                    HiddenOptionFlags.STRICT_TRY_CATCH_ANALYSIS && !HiddenOptionFlags.LENIENT_TRY_CATCH_ANALYSIS,
                    diagnosticContext
            );
        } catch (AssertionFailedException assertionFailedException1) {
            throw assertionFailedException1;
        }

        if (livenessAnalysisEnabled && !HiddenOptionFlags.STACK_MAP_ALGORITHM.equals("1")) {
            try {
                this.localLivenessConsistent = this.computeLocalLiveness();
            } catch (AssertionFailedException assertionFailedException) {
                throw assertionFailedException;
            } catch (Exception exception) {
            } catch (StackOverflowError stackOverflowError) {
                throw stackOverflowError;
            }
        }

        int maxStackValue = this.computeMaxStack();
        int maxStack = ((CodeAttributeBody) methodBytecode1.getParent()).getMaxStack();
        if (maxStackValue != maxStack) {
            ((CodeAttributeBody) methodBytecode1.getParent()).setMaxStack(maxStackValue);
        }

        commonSuperTypeResolver1.clearThreadVersion();
    }

    private void removeUnreachableBlocks(TwoKeyMap twoKeyMap) {
        if (this.basicBlocks.size() != 1) {
            int basicBlocksSize = this.basicBlocks.size();
            ArrayList arrayList = null;

            for (int elementIndex = 0; elementIndex < basicBlocksSize; elementIndex++) {
                BasicBlock basicBlock = (BasicBlock) this.basicBlocks.get(elementIndex);
                if (basicBlock.getLabel() == null) {
                    if (arrayList == null) {
                        arrayList = new ArrayList();
                    }

                    arrayList.add(basicBlock);
                }
            }

            if (arrayList != null) {
                for (int elementIndexValue = 0; elementIndexValue < arrayList.size(); elementIndexValue++) {
                    this.basicBlocks.remove(arrayList.get(elementIndexValue));
                }
            }

            HashSet hashSet = ZkmUtils.createHashSet(ZkmUtils.getPrimeCapacity(this.basicBlocks.size()));
            this.collectReachableBlocks(this.entryBlock, hashSet);
            UniqueWorkQueue uniqueWorkQueue = new UniqueWorkQueue();

            for (int elementIndexValue2 = 0; elementIndexValue2 < this.catchHandlerBlocks.size(); elementIndexValue2++) {
                uniqueWorkQueue.enqueue(this.catchHandlerBlocks.get(elementIndexValue2));
            }

            boolean bl1 = true;

            while (!uniqueWorkQueue.isEmpty() && bl1) {
                int uniqueWorkQueueSize = uniqueWorkQueue.size();
                int hashSetSize = hashSet.size();

                for (int i = 0; i < uniqueWorkQueueSize; i++) {
                    BasicBlock basicBlock1 = (BasicBlock) uniqueWorkQueue.dequeue();
                    Map map1 = twoKeyMap.getInnerMap(basicBlock1);
                    boolean bl = false;
                    if (map1 != null) {
                        Iterator iterator = map1.keySet().iterator();

                        while (iterator.hasNext()) {
                            BasicBlock basicBlock2 = (BasicBlock) iterator.next();
                            if (hashSet.contains(basicBlock2)) {
                                this.collectReachableBlocks(basicBlock1, hashSet);
                                bl = true;
                                break;
                            }
                        }
                    }

                    if (!bl) {
                        uniqueWorkQueue.enqueue(basicBlock1);
                    }
                }

                bl1 = hashSetSize != hashSet.size();
            }

            while (!uniqueWorkQueue.isEmpty()) {
                BasicBlock basicBlock3 = (BasicBlock) uniqueWorkQueue.dequeue();
                this.catchHandlerBlocks.remove(basicBlock3);
                this.tryCatchInfosByHandler.removeKey(basicBlock3);
            }

            if (basicBlocksSize > hashSet.size()) {
                this.basicBlocks.clear();
                this.basicBlocks.addAll(hashSet);
                Collections.sort(this.basicBlocks);
                this.blockByIndexCache = null;
            }
        }
    }

    public Integer[] getFrameLabelIndices() {
        HashMap hashMap = ZkmUtils.createHashMap(ZkmUtils.getPrimeCapacity(this.basicBlocks.size()), 3.0F);

        for (int i = 0; i < this.instructions.size(); i++) {
            Instruction instruction1 = (Instruction) this.instructions.get(i);
            if (instruction1.isLabel()) {
                LabelInstruction labelInstruction = (LabelInstruction) this.instructions.get(i);
                if (labelInstruction.hasUsageBits(1) || labelInstruction.hasUsageBits(1024)) {
                    hashMap.put(INTEGER_CACHE.valueOf(labelInstruction.getOffset()), INTEGER_CACHE.valueOf(i));
                }
            }
        }

        Collection collection1 = hashMap.values();
        Integer[] integers = ((java.lang.Integer[]) (collection1.toArray(new Integer[collection1.size()])));
        Arrays.sort(integers);
        return integers;
    }

    public HashSet findDirectNestedLoops(Object key, Set set1, SetMultiMap setMultiMap, SetMultiMap setMultiMap1) {
        HashSet hashSet = ZkmUtils.createHashSet();
        Set set2 = setMultiMap.getValues(key);
        if (set2 != null) {
            Iterator iterator = set2.iterator();

            while (iterator.hasNext()) {
                NaturalLoop naturalLoop = (NaturalLoop) iterator.next();
                Set set3 = setMultiMap1.getValues(naturalLoop);
                if (set1.containsAll(set3)) {
                    hashSet.add(naturalLoop);
                }
            }
        }

        return hashSet;
    }

    public static StackFrameState[] getFrameStates(MethodFlowAnalyzer methodFlowAnalyzer) {
        return methodFlowAnalyzer.frameStates;
    }

    public boolean isGetComponentTypeCall(ResolvedMethodRef resolvedMethodRef, ClassMemberLookup classMemberLookup1) throws ZkmException, IOException {
        try {
            if (resolvedMethodRef.getSignatureString().equals("getComponentType()Ljava/lang/Class;")) {
                String referencedClassName = resolvedMethodRef.getReferencedClassName();
                return referencedClassName.equals("java/lang/Class") || classMemberLookup1.isSubclass(referencedClassName, "java/lang/Class");
            } else {
                return false;
            }
        } catch (ClassFileLoadException classFileLoadException) {
            return false;
        }
    }

    public PropertiesLoadTracker createPropertiesLoadTracker() {
        return new PropertiesLoadTracker(this);
    }

    public void traceFieldReadSource(ValueTraceFrame valueTraceFrame, int elementIndex) throws IOException {
        ConstantRefInstruction constantRefInstruction = (ConstantRefInstruction) this.instructions.get(elementIndex);
        ResolvedFieldRef resolvedFieldRef = (ResolvedFieldRef) constantRefInstruction.getConstantPoolEntry();
        AbstractFieldInfo abstractFieldInfo = (AbstractFieldInfo) resolvedFieldRef.getResolvedMember();
        if (abstractFieldInfo != null && abstractFieldInfo.isProgramMember()) {
            MemberRefKey memberRefKey = abstractFieldInfo.toMemberRefKey();
            HashSet hashSet;
            if (valueTraceFrame.fieldValueCache.containsKey(memberRefKey)) {
                hashSet = (HashSet) valueTraceFrame.fieldValueCache.get(memberRefKey);
            } else {
                hashSet = ZkmUtils.createHashSet(13);
                ListMultimap listMultimap = valueTraceFrame.fieldStoreSites.getMultimap(memberRefKey);
                if (listMultimap != null) {
                    Enumeration enumeration = listMultimap.keys();

                    while (enumeration.hasMoreElements()) {
                        MethodBytecode methodBytecode1 = (MethodBytecode) enumeration.nextElement();
                        List list1 = listMultimap.getValues(methodBytecode1);

                        try {
                            MethodFlowAnalyzer methodFlowAnalyzer1 = methodBytecode1.getCachedFlowAnalyzer(
                                    valueTraceFrame.flowAnalyzerCache, valueTraceFrame.superTypeResolver, this.hierarchyQuery
                            );
                            HashSet hashSet1 = methodFlowAnalyzer1.traceFieldStoreValues(valueTraceFrame, (FieldInfo) abstractFieldInfo, list1);
                            Iterator iterator = hashSet1.iterator();

                            while (iterator.hasNext()) {
                                if (iterator.next() instanceof NullReflectionValue) {
                                    iterator.remove();
                                }
                            }

                            hashSet.addAll(hashSet1);
                        } catch (ZkmException zkmException) {
                            valueTraceFrame.results.add(new InstructionReasonNote(constantRefInstruction, "Exception '" + zkmException.getMessage() + "'"));
                        }
                    }
                }

                valueTraceFrame.fieldValueCache.put(memberRefKey, hashSet);
            }

            if (hashSet != null) {
                valueTraceFrame.results.addAll(hashSet);
            }
        } else {
            String referencedClassName = resolvedFieldRef.getReferencedClassName();
            if (resolvedFieldRef.getMemberName().equals("TYPE")) {
                if (referencedClassName.equals("java/lang/Integer")) {
                    valueTraceFrame.results.add(new LiteralStringValue("I"));
                } else if (referencedClassName.equals("java/lang/Byte")) {
                    valueTraceFrame.results.add(new LiteralStringValue("B"));
                } else if (referencedClassName.equals("java/lang/Character")) {
                    valueTraceFrame.results.add(new LiteralStringValue("C"));
                } else if (referencedClassName.equals("java/lang/Short")) {
                    valueTraceFrame.results.add(new LiteralStringValue("S"));
                } else if (referencedClassName.equals("java/lang/Long")) {
                    valueTraceFrame.results.add(new LiteralStringValue("J"));
                } else if (referencedClassName.equals("java/lang/Float")) {
                    valueTraceFrame.results.add(new LiteralStringValue("F"));
                } else if (referencedClassName.equals("java/lang/Double")) {
                    valueTraceFrame.results.add(new LiteralStringValue("D"));
                } else if (referencedClassName.equals("java/lang/Boolean")) {
                    valueTraceFrame.results.add(new LiteralStringValue("Z"));
                } else if (referencedClassName.equals("java/lang/Void")) {
                    valueTraceFrame.results.add(new LiteralStringValue("V"));
                } else {
                    valueTraceFrame.results.add(new InstructionReasonNote(constantRefInstruction, "Source field is external: " + referencedClassName + "." + "TYPE"));
                }
            } else {
                valueTraceFrame.results
                        .add(new InstructionReasonNote(constantRefInstruction, "Source field is external: " + referencedClassName + "." + resolvedFieldRef.getMemberName()));
            }
        }
    }

    public boolean hasUninitializedThisAt(int frameStateIndex) {
        return this.frameStates[frameStateIndex].hasUninitializedThis();
    }

    public void traceFieldWriteUses(ValueTraceFrame valueTraceFrame, ValueFlowTracker valueFlowTracker, int elementIndexArgument) throws ZkmException, IOException {
        ResolvedFieldRef resolvedFieldRef = (ResolvedFieldRef) ((ConstantRefInstruction) this.instructions.get(elementIndexArgument)).getConstantPoolEntry();
        AbstractFieldInfo abstractFieldInfo = (AbstractFieldInfo) resolvedFieldRef.getResolvedMember();
        HashSet hashSet;
        if (abstractFieldInfo != null) {
            if (abstractFieldInfo.isProgramMember()) {
                MemberRefKey memberRefKey = abstractFieldInfo.toMemberRefKey();
                ListMultimap listMultimap = valueTraceFrame.fieldStoreSites.getMultimap(memberRefKey);
                if (listMultimap != null) {
                    if (listMultimap.getValueCount() == 1) {
                        ListMultimap listMultimap1 = valueTraceFrame.fieldLoadSites.getMultimap(memberRefKey);
                        if (listMultimap1 != null) {
                            Enumeration enumeration = listMultimap1.keys();

                            while (enumeration.hasMoreElements()) {
                                MethodBytecode methodBytecode1 = (MethodBytecode) enumeration.nextElement();
                                if (methodBytecode1 == this.methodBytecode) {
                                    List list2 = listMultimap1.getValues(methodBytecode1);

                                    for (int elementIndex = 0; elementIndex < list2.size(); elementIndex++) {
                                        int frameStateIndex = (Integer) list2.get(elementIndex);
                                        StackFrameState stackFrameState1 = this.frameStates[frameStateIndex];
                                        if (stackFrameState1 != null) {
                                            this.traceStackValueUses(
                                                    valueTraceFrame, valueFlowTracker, this.getBlockContaining(frameStateIndex), frameStateIndex + 1, stackFrameState1.getStack().length - 1
                                            );
                                        }
                                    }
                                } else {
                                    try {
                                        MethodFlowAnalyzer methodFlowAnalyzer1 = methodBytecode1.getCachedFlowAnalyzer(
                                                valueTraceFrame.flowAnalyzerCache, valueTraceFrame.superTypeResolver, this.hierarchyQuery
                                        );
                                        List list1 = listMultimap1.getValues(methodBytecode1);

                                        for (int elementIndexValue = 0; elementIndexValue < list1.size(); elementIndexValue++) {
                                            int frameStateIndexValue = (Integer) list1.get(elementIndexValue);
                                            StackFrameState stackFrameState = methodFlowAnalyzer1.frameStates[frameStateIndexValue];
                                            if (stackFrameState != null) {
                                                ValueFlowTracker valueFlowTracker1 = valueFlowTracker.getTrackerFrom(methodFlowAnalyzer1);
                                                methodFlowAnalyzer1.traceStackValueUses(
                                                        valueTraceFrame,
                                                        valueFlowTracker1,
                                                        methodFlowAnalyzer1.getBlockContaining(frameStateIndexValue),
                                                        frameStateIndexValue + 1,
                                                        stackFrameState.getStack().length - 1
                                                );
                                            }
                                        }
                                    } catch (ZkmException zkmException) {
                                        valueTraceFrame.results.add(new InstructionReasonNote(null, "Error while tracing field use : " + zkmException.getMessage()));
                                    }
                                }
                            }
                        }
                    } else {
                        valueTraceFrame.results.add(new InstructionReasonNote(null, "Field set more than once"));
                    }

                    return;
                }

                return;
            }

            hashSet = valueTraceFrame.results;
        } else {
            hashSet = valueTraceFrame.results;
        }

        hashSet.add(new InstructionReasonNote(null, "Field is external : " + resolvedFieldRef.getValueString()));
    }

    public boolean isGetPropertyCall(ResolvedMethodRef resolvedMethodRef, ClassMemberLookup classMemberLookup1) throws ZkmException, IOException {
        return this.isPropertiesMethodCall(resolvedMethodRef, "getProperty(Ljava/lang/String;)Ljava/lang/String;", classMemberLookup1);
    }

    public void collectNestedSubroutineCalls(BasicBlock basicBlock, BasicBlock basicBlock1, TwoKeyMap twoKeyMap, Set set1, boolean bl) {
        if (!set1.contains(basicBlock1)) {
            set1.add(basicBlock1);
            BasicBlock basicBlock2 = basicBlock1.getJsrReturnBlock();
            if (basicBlock2 != null) {
                BranchInstruction branchInstruction = (BranchInstruction) this.instructions.get(basicBlock1.getEndIndex());
                LabelInstruction labelInstruction = branchInstruction.getTargetLabel();
                BasicBlock basicBlock3 = this.findBlockStartingAt(labelInstruction.getInstructionIndex());
                Boolean boolean1 = bl ? Boolean.TRUE : Boolean.FALSE;
                if (basicBlock != basicBlock3) {
                    Boolean boolean2 = (Boolean) twoKeyMap.putValue(basicBlock, basicBlock3, boolean1);
                    if (boolean2 != null && !boolean2 && boolean1) {
                        twoKeyMap.putValue(basicBlock, basicBlock3, boolean2);
                    }
                }

                this.collectNestedSubroutineCalls(basicBlock, basicBlock2, twoKeyMap, set1, bl);
            } else {
                Enumeration enumeration = basicBlock1.enumerateSuccessors();
                if (enumeration != null) {
                    while (enumeration.hasMoreElements()) {
                        BasicBlock basicBlock4 = (BasicBlock) enumeration.nextElement();
                        this.collectNestedSubroutineCalls(basicBlock, basicBlock4, twoKeyMap, set1, bl);
                    }
                }
            }

            Enumeration enumeration1 = basicBlock1.enumerateExceptionHandlers();

            while (enumeration1.hasMoreElements()) {
                BasicBlock basicBlock5 = (BasicBlock) enumeration1.nextElement();
                this.collectNestedSubroutineCalls(basicBlock, basicBlock5, twoKeyMap, set1, true);
            }
        }
    }

    public boolean hasPendingPredecessor(int blockByIndexCacheElementIndex, TreeSet treeSet) {
        BasicBlock basicBlock = this.getBlockContaining(blockByIndexCacheElementIndex);
        if (basicBlock.hasPredecessors()) {
            Iterator iterator = basicBlock.getPredecessors().iterator();

            while (iterator.hasNext()) {
                int startIndex = ((BasicBlock) iterator.next()).getStartIndex();
                if (treeSet.contains(INTEGER_CACHE.valueOf(startIndex)) || this.frameStates[startIndex] == null) {
                    return true;
                }
            }
        }

        return false;
    }

    public int computeShortestReturnPath() {
        int maxValue = Integer.MAX_VALUE;

        for (int elementIndex = 0; elementIndex < this.basicBlocks.size(); elementIndex++) {
            BasicBlock basicBlock = (BasicBlock) this.basicBlocks.get(elementIndex);
            if (((Instruction) this.instructions.get(basicBlock.getEndIndex())).isReturn()) {
                int minDistanceFromEntry = basicBlock.getMinDistanceFromEntry();
                if (minDistanceFromEntry < maxValue) {
                    maxValue = minDistanceFromEntry;
                }
            }
        }

        return maxValue;
    }

    public boolean findStackValueProducers(BasicBlock basicBlock, int ba, int bb, Set set1, Set set2) {
        int bc = bb;
        if (!set2.add(basicBlock)) {
            return true;
        }

        boolean bl = false;
        int startIndex = basicBlock.getStartIndex();

        for (int frameStateIndex = ba - 1; !bl && frameStateIndex >= startIndex; frameStateIndex += -1) {
            Instruction instruction1 = (Instruction) this.instructions.get(frameStateIndex);
            StackFrameState stackFrameState = this.frameStates[frameStateIndex];
            if (instruction1.isStackManipulation()) {
                VerifierType[] verifierTypes = stackFrameState.getStack();
                if (instruction1.affectsStackSlot(verifierTypes, bc)) {
                    return false;
                }

                bc = instruction1.mapStackSlotBackward(verifierTypes, bc);
            } else {
                if (instruction1.isJump()) {
                    return false;
                }

                if (instruction1.isLabel()
                        && (
                        ((LabelInstruction) instruction1).hasUsageBits(1)
                                || ((LabelInstruction) instruction1).hasUsageBits(256)
                                || ((LabelInstruction) instruction1).hasUsageBits(512)
                                || ((LabelInstruction) instruction1).hasUsageBits(1024)
                )) {
                    return false;
                }

                if (stackFrameState.getStackDepth() - 1 == bc && instruction1.pushesValue()) {
                    set1.add(INTEGER_CACHE.valueOf(frameStateIndex));
                    bl = true;
                }
            }
        }

        if (!bl) {
            List list1 = basicBlock.getPredecessors();
            if (list1 != null && list1.size() > 0) {
                for (int elementIndex = 0; elementIndex < list1.size(); elementIndex++) {
                    BasicBlock basicBlock1 = (BasicBlock) list1.get(elementIndex);
                    boolean bl1 = this.findStackValueProducers(basicBlock1, basicBlock1.getEndIndex() + 1, bc, set1, set2);
                    if (!bl1) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    public void traceLocalVariableUses(ValueTraceFrame valueTraceFrame, ValueFlowTracker valueFlowTracker, BasicBlock basicBlock, int ba, int bb) throws ZkmException, IOException {
        int bc = bb;
        int bd = ba;
        if (!valueFlowTracker.isVisited(2, bd, bc)) {
            valueFlowTracker.markVisited(2, ba, bb);
            int endIndex = basicBlock.getEndIndex();

            for (int elementIndexValue2 = ba; elementIndexValue2 < endIndex; elementIndexValue2++) {
                Instruction instruction1 = (Instruction) this.instructions.get(elementIndexValue2);
                StackFrameState stackFrameState = this.frameStates[elementIndexValue2];
                bc = bb;
                if (instruction1.isStoreTo(bc)) {
                    return;
                }

                if (instruction1.isLoadFrom(bb)) {
                    int bg = stackFrameState.getStack().length - 1;
                    this.traceStackValueUses(valueTraceFrame, valueFlowTracker, basicBlock, elementIndexValue2 + 1, bg);
                }
            }

            List list1 = basicBlock.getSuccessorsCopy();
            if (list1 != null && list1.size() > 0) {
                for (int elementIndex = 0; elementIndex < list1.size(); elementIndex++) {
                    BasicBlock basicBlock1 = (BasicBlock) list1.get(elementIndex);
                    this.traceLocalVariableUses(valueTraceFrame, valueFlowTracker, basicBlock1, basicBlock1.getStartIndex(), bb);
                }
            } else if (basicBlock.hasRetBlocks()) {
                List list2 = basicBlock.getRetBlocks();

                for (int elementIndexValue = 0; elementIndexValue < list2.size(); elementIndexValue++) {
                    BasicBlock basicBlock2 = (BasicBlock) list2.get(elementIndexValue);
                    this.traceLocalVariableUses(valueTraceFrame, valueFlowTracker, basicBlock2, basicBlock2.getStartIndex(), bb);
                }
            }
        }
    }

    public BasicBlock findBlockStartingAt(int ba) {
        int bb = 0;
        int bc = this.basicBlocks.size() - 1;

        while (bb <= bc) {
            int elementIndex = (bb + bc) / 2;
            BasicBlock basicBlock = (BasicBlock) this.basicBlocks.get(elementIndex);
            int startIndex = basicBlock.getStartIndex();
            if (ba < startIndex) {
                bc = elementIndex - 1;
            } else {
                if (ba <= startIndex) {
                    return basicBlock;
                }

                bb = elementIndex + 1;
            }
        }

        return null;
    }

    public void buildLoopTree(
            NaturalLoop naturalLoop, HashSet hashSet, GenericTree genericTree, SetMultiMap setMultiMap, SetMultiMap setMultiMap1, Set set1, boolean bl
    ) {
        hashSet.add(naturalLoop);
        Set set2;
        if (bl) {
            set2 = this.findDirectNestedLoops(naturalLoop, hashSet, setMultiMap, setMultiMap1);
        } else {
            set2 = setMultiMap.getValues(naturalLoop);
        }

        if (set2 != null) {
            Iterator iterator = set2.iterator();

            while (iterator.hasNext()) {
                NaturalLoop naturalLoop1 = (NaturalLoop) iterator.next();
                if (!set1.contains(naturalLoop1)) {
                    genericTree.moveToValue(naturalLoop);
                    genericTree.addChild(naturalLoop1);
                    set1.add(naturalLoop1);
                    this.buildLoopTree(naturalLoop1, ZkmUtils.createHashSetFrom(hashSet), genericTree, setMultiMap, setMultiMap1, set1, bl);
                }
            }
        }
    }

    public boolean hasFrameLabels() {
        for (int elementIndex = 0; elementIndex < this.instructions.size(); elementIndex++) {
            if (((Instruction) this.instructions.get(elementIndex)).isLabel()) {
                LabelInstruction labelInstruction = (LabelInstruction) this.instructions.get(elementIndex);
                if (labelInstruction.hasUsageBits(1) || labelInstruction.hasUsageBits(1024)) {
                    return true;
                }
            }
        }

        return false;
    }

    public void traceLocalThroughCatchHandler(ValueTraceFrame valueTraceFrame, BasicBlock basicBlock, int parameterIndexArgument) throws ZkmException, IOException {
        HashSet hashSet = ZkmUtils.createHashSet();
        List list1 = this.tryCatchInfosByHandler.getValues(basicBlock);
        BitSet bitSet = new BitSet(this.basicBlocks.size());

        for (int elementIndex = 0; elementIndex < list1.size(); elementIndex++) {
            TryCatchBlockInfo tryCatchBlockInfo = (TryCatchBlockInfo) list1.get(elementIndex);
            ExceptionTableEntry exceptionTableEntry1 = tryCatchBlockInfo.exceptionTableEntry;
            LabelInstruction labelInstruction = exceptionTableEntry1.getLabel(ExceptionRangeField.START_PC);
            LabelInstruction labelInstruction1 = exceptionTableEntry1.getLabel(ExceptionRangeField.END_PC);
            int instructionIndex = labelInstruction.getInstructionIndex();
            int instructionIndexValue = labelInstruction1.getInstructionIndex();
            BasicBlock basicBlock1 = null;
            HashSet hashSet1 = ZkmUtils.createHashSet();
            Iterator iterator = this.basicBlocks.iterator();

            while (iterator.hasNext()) {
                BasicBlock basicBlock2 = (BasicBlock) iterator.next();
                if (basicBlock2.getStartIndex() >= instructionIndex && basicBlock2.getStartIndex() < instructionIndexValue) {
                    hashSet1.add(basicBlock2);
                } else if (basicBlock2.getStartIndex() < instructionIndex && basicBlock2.getEndIndex() >= instructionIndex) {
                    basicBlock1 = basicBlock2;
                }
            }

            for (int instructionIndexValue2 = instructionIndex; instructionIndexValue2 < instructionIndexValue; instructionIndexValue2++) {
                Instruction instruction1 = (Instruction) this.instructions.get(instructionIndexValue2);
                if (instruction1.isStoreTo(parameterIndexArgument)) {
                    BasicBlock basicBlock3 = this.getBlockContaining(instructionIndexValue2);
                    if (instructionIndexValue2 > basicBlock3.getStartIndex()) {
                        this.traceStackValueSource(valueTraceFrame, basicBlock3, instructionIndexValue2, this.frameStates[instructionIndexValue2].getStackDepth());
                        return;
                    }

                    List list2 = basicBlock3.getPredecessors();
                    if (list2 != null && list2.size() > 0) {
                        for (int elementIndexValue = 0; elementIndexValue < list2.size(); elementIndexValue++) {
                            BasicBlock basicBlock4 = (BasicBlock) list2.get(elementIndexValue);
                            this.traceStackValueSource(valueTraceFrame, basicBlock4, basicBlock4.getEndIndex() + 1, this.frameStates[instructionIndexValue2].getStackDepth());
                        }

                        return;
                    }
                }
            }

            if (basicBlock1 != null) {
                bitSet.clear();
                if (basicBlock != basicBlock1 && !basicBlock.canReach(basicBlock1, bitSet)) {
                    this.traceLocalVariableSource(valueTraceFrame, basicBlock1, instructionIndex, parameterIndexArgument);
                } else {
                    valueTraceFrame.results.add(new InstructionReasonNote(
                            (Instruction) this.instructions.get(basicBlock.getStartIndex()), "Loop through catch"));
                }
            }

            iterator = hashSet1.iterator();

            while (iterator.hasNext()) {
                BasicBlock basicBlock6 = (BasicBlock) iterator.next();
                int startIndex = basicBlock6.getStartIndex();
                if (!((Instruction) this.instructions.get(startIndex)).isStoreTo(parameterIndexArgument) && !StackFrameState.isTopOrMissing(this.frameStates[startIndex].getLocals()[parameterIndexArgument])) {
                    List list3 = basicBlock6.getPredecessors();
                    if (list3 != null && list3.size() > 0) {
                        for (int elementIndexValue2 = 0; elementIndexValue2 < list3.size(); elementIndexValue2++) {
                            BasicBlock basicBlock7 = (BasicBlock) list3.get(elementIndexValue2);
                            if (!hashSet1.contains(basicBlock7) && (basicBlock1 == null || basicBlock7 != basicBlock1)) {
                                hashSet.add(basicBlock7);
                            }
                        }
                    }
                }
            }
        }

        Iterator iterator1 = hashSet.iterator();

        while (iterator1.hasNext()) {
            BasicBlock basicBlock5 = (BasicBlock) iterator1.next();
            bitSet.clear();
            HashSet hashSet3;
            if (basicBlock != basicBlock5) {
                if (!basicBlock.canReach(basicBlock5, bitSet)) {
                    this.traceLocalVariableSource(valueTraceFrame, basicBlock5, basicBlock5.getEndIndex() + 1, parameterIndexArgument);
                    continue;
                }

                hashSet3 = valueTraceFrame.results;
            } else {
                hashSet3 = valueTraceFrame.results;
            }

            hashSet3.add(new InstructionReasonNote((Instruction) this.instructions.get(basicBlock.getStartIndex()), "Loop through catch"));
        }
    }

    public void traceStackValueSource(ValueTraceFrame valueTraceFrame, BasicBlock basicBlock, int integerArgument, int bb) throws ZkmException, IOException {
        int integerValue = bb;
        Triple triple = new Triple(this.methodBytecode, INTEGER_CACHE.valueOf(integerArgument), INTEGER_CACHE.valueOf(integerValue));
        HashSet hashSet = (HashSet) valueTraceFrame.stackSourceCache.get(triple);
        byte bj;
        if (hashSet != null) {
            if (valueTraceFrame.argumentKind != ReflectionArgumentKind.USER_DEFINED_METHOD_1) {
                if (valueTraceFrame.argumentKind != ReflectionArgumentKind.USER_DEFINED_METHOD_2) {
                    valueTraceFrame.results.addAll(hashSet);
                    return;
                }

                long bi = 56257057901662L;
                bj = 13;
            } else {
                long bg = 56257057901662L;
                bj = 13;
            }
        } else {
            long bh = 56257057901662L;
            bj = 13;
        }

        Integer integer = Integer.valueOf(bj);
        hashSet = ZkmUtils.createHashSet(integer);
        HashSet hashSet1 = valueTraceFrame.swapResults(hashSet);
        if (valueTraceFrame.getNestingLevel() > 3) {
            valueTraceFrame.results
                    .add(new InstructionReasonNote(null, "Max Nesting Level Exceeded: " + valueTraceFrame.getNestingLevel() + " " + valueTraceFrame.argumentKind));
        } else {
            boolean bl = false;
            int startIndex = basicBlock.getStartIndex();

            for (int frameStateIndex = integerArgument - 1; !bl && frameStateIndex >= startIndex; frameStateIndex += -1) {
                Instruction instruction1 = (Instruction) this.instructions.get(frameStateIndex);
                StackFrameState stackFrameState = this.frameStates[frameStateIndex];
                if (valueTraceFrame.argumentKind == ReflectionArgumentKind.OBJECT_TYPE) {
                    String descriptor = stackFrameState.getStack()[integerValue].getDescriptor();
                    descriptor = CommonSuperTypeResolver.stripClassDescriptor(descriptor);
                    valueTraceFrame.results.add(new TracedObjectType(instruction1, descriptor));
                    bl = true;
                    break;
                }

                if (instruction1.isStackManipulation()) {
                    integerValue = instruction1.mapStackSlotBackward(stackFrameState.getStack(), integerValue);
                } else if (stackFrameState.getStackDepth() - 1 == integerValue) {
                    if (instruction1.pushesValue()) {
                        switch (instruction1.getOpcode()) {
                            case 1:
                                valueTraceFrame.results.add(new NullReflectionValue());
                                break;
                            case 18:
                                LdcInstruction ldcInstruction = (LdcInstruction) instruction1;
                                ConstantPoolEntry constantPoolEntry = ldcInstruction.getConstantPoolEntry();
                                if (constantPoolEntry instanceof ResolvedStringConstant) {
                                    valueTraceFrame.results.add(new StringConstantNameRef((ResolvedStringConstant) constantPoolEntry));
                                } else if (constantPoolEntry instanceof ResolvedClassConstant) {
                                    valueTraceFrame.results.add(new ClassConstantValue((ResolvedClassConstant) constantPoolEntry));
                                } else if (constantPoolEntry instanceof ResolvedMethodType) {
                                    valueTraceFrame.results.add(new StringConstantValue((ResolvedMethodType) constantPoolEntry));
                                }
                                break;
                            case 19:
                                ConstantPoolEntry constantPoolEntry1 = ((ConstantRefInstruction) instruction1).getConstantPoolEntry();
                                if (constantPoolEntry1 instanceof ResolvedStringConstant) {
                                    valueTraceFrame.results.add(new StringConstantNameRef((ResolvedStringConstant) constantPoolEntry1));
                                } else if (constantPoolEntry1 instanceof ResolvedClassConstant) {
                                    valueTraceFrame.results.add(new ClassConstantValue((ResolvedClassConstant) constantPoolEntry1));
                                } else if (constantPoolEntry1 instanceof ResolvedMethodType) {
                                    valueTraceFrame.results.add(new StringConstantValue((ResolvedMethodType) constantPoolEntry1));
                                }
                                break;
                            case 178:
                            case 180:
                                this.traceFieldReadSource(valueTraceFrame, frameStateIndex);
                                break;
                            case 182:
                            case 183:
                            case 184:
                            case 185:
                                this.traceMethodCallSource(valueTraceFrame, basicBlock, frameStateIndex);
                                break;
                            case 187:
                                this.traceNewInstanceSource(valueTraceFrame, basicBlock, frameStateIndex);
                                break;
                            case 189:
                                this.traceNewArraySource(valueTraceFrame, basicBlock, frameStateIndex);
                                break;
                            case 255:
                                LocalVariableInstruction localVariableInstruction = (LocalVariableInstruction) instruction1;
                                if (localVariableInstruction.getAccessKind() == LocalVariableAccessKind.OBJECT_LOAD) {
                                    this.traceLocalVariableSource(valueTraceFrame, basicBlock, frameStateIndex, localVariableInstruction.getLocalIndex());
                                } else {
                                    valueTraceFrame.results.add(new InstructionReasonNote(instruction1, "Instruction not handled: " + valueTraceFrame.argumentKind));
                                }
                                break;
                            default:
                                valueTraceFrame.results.add(new InstructionReasonNote(instruction1, "Instruction not handled: " + valueTraceFrame.argumentKind));
                        }

                        bl = true;
                    }
                } else if (instruction1.isJsr()) {
                    valueTraceFrame.results.add(new InstructionReasonNote(instruction1, "Instruction not handled: " + valueTraceFrame.argumentKind));
                    bl = true;
                }
            }

            if (!bl) {
                List list1 = basicBlock.getPredecessors();
                if (list1 != null && list1.size() > 0) {
                    for (int elementIndex = 0; elementIndex < list1.size(); elementIndex++) {
                        BasicBlock basicBlock1 = (BasicBlock) list1.get(elementIndex);
                        this.traceStackValueSource(valueTraceFrame, basicBlock1, basicBlock1.getEndIndex() + 1, integerValue);
                    }
                } else {
                    Instruction instruction2 = (Instruction) this.instructions.get(startIndex);
                    valueTraceFrame.results.add(new InstructionReasonNote(instruction2, "No source found 1):" + valueTraceFrame.argumentKind));
                }
            }
        }

        hashSet = valueTraceFrame.swapResults(hashSet1);
        hashSet1.addAll(hashSet);
        if (valueTraceFrame.argumentKind != ReflectionArgumentKind.USER_DEFINED_METHOD_1
                && valueTraceFrame.argumentKind != ReflectionArgumentKind.USER_DEFINED_METHOD_2) {
            HashSet hashSet2 = ((java.util.HashSet) (valueTraceFrame.stackSourceCache.put(triple, hashSet)));
            if (hashSet2 != null) {
                hashSet.addAll(hashSet2);
            }
        }
    }

    public ArrayList buildLoopNestingTrees() {
        HashMap hashMap = ZkmUtils.createHashMap();
        Iterator iterator = this.naturalLoops.iterator();

        while (iterator.hasNext()) {
            NaturalLoop naturalLoop = (NaturalLoop) iterator.next();
            hashMap.put(naturalLoop.getHeader(), naturalLoop);
        }

        SetMultiMap setMultiMap = new SetMultiMap();
        Iterator iterator5 = this.naturalLoops.iterator();

        while (iterator5.hasNext()) {
            NaturalLoop naturalLoop1 = (NaturalLoop) iterator5.next();
            Iterator iterator1 = this.naturalLoops.iterator();

            while (iterator1.hasNext()) {
                NaturalLoop naturalLoop2 = (NaturalLoop) iterator1.next();
                if (naturalLoop1 != naturalLoop2 && naturalLoop1.containsBlock(naturalLoop2.getHeader())) {
                    setMultiMap.addValue(naturalLoop1, naturalLoop2);
                }
            }
        }

        SetMultiMap setMultiMap1 = new SetMultiMap();
        Iterator iterator6 = setMultiMap.entrySet().iterator();

        while (iterator6.hasNext()) {
            Entry entry = (Entry) iterator6.next();
            NaturalLoop naturalLoop6 = (NaturalLoop) entry.getKey();
            Iterator iterator2 = ((Set) entry.getValue()).iterator();

            while (iterator2.hasNext()) {
                NaturalLoop naturalLoop3 = (NaturalLoop) iterator2.next();
                setMultiMap1.addValue(naturalLoop3, naturalLoop6);
            }
        }

        ArrayList arrayList = new ArrayList();
        Iterator iterator7 = this.naturalLoops.iterator();

        while (iterator7.hasNext()) {
            NaturalLoop naturalLoop7 = (NaturalLoop) iterator7.next();
            if (!setMultiMap1.containsKey(naturalLoop7)) {
                arrayList.add(naturalLoop7);
            }
        }

        ArrayList arrayList1 = new ArrayList();
        HashSet hashSet = ZkmUtils.createHashSet();
        Iterator iterator8 = arrayList.iterator();

        while (iterator8.hasNext()) {
            NaturalLoop naturalLoop8 = (NaturalLoop) iterator8.next();
            GenericTree genericTree = new GenericTree(naturalLoop8);
            hashSet.add(naturalLoop8);
            arrayList1.add(genericTree);
            this.buildLoopTree(naturalLoop8, ZkmUtils.createHashSet(), genericTree, setMultiMap, setMultiMap1, hashSet, true);
        }

        if (hashSet.size() < this.naturalLoops.size()) {
            iterator8 = setMultiMap.keySet().iterator();

            while (iterator8.hasNext()) {
                NaturalLoop naturalLoop9 = (NaturalLoop) iterator8.next();
                if (!hashSet.contains(naturalLoop9)) {
                    GenericTree genericTree1 = new GenericTree(naturalLoop9);
                    hashSet.add(naturalLoop9);
                    arrayList1.add(genericTree1);
                    Iterator iterator3 = setMultiMap.getValues(naturalLoop9).iterator();

                    while (iterator3.hasNext()) {
                        NaturalLoop naturalLoop4 = (NaturalLoop) iterator3.next();
                        genericTree1.resetToRoot();
                        genericTree1.addChild(naturalLoop4);
                        hashSet.add(naturalLoop9);
                    }
                }
            }
        }

        if (hashSet.size() < this.naturalLoops.size()) {
            HashSet hashSet1 = ZkmUtils.createHashSet();
            Iterator iterator9 = arrayList1.iterator();

            while (iterator9.hasNext()) {
                GenericTree genericTree2 = (GenericTree) iterator9.next();
                hashSet1.addAll(new EnumerationBackedList(genericTree2.values()));
            }

            HashSet hashSet2 = ZkmUtils.createHashSetFrom(this.naturalLoops);
            hashSet2.removeAll(hashSet1);
            NaturalLoop naturalLoop10 = null;
            Iterator iterator10 = hashSet2.iterator();

            while (iterator10.hasNext()) {
                NaturalLoop naturalLoop11 = (NaturalLoop) iterator10.next();
                Set set1 = setMultiMap1.getValues(naturalLoop11);
                if (set1 != null) {
                    TreeSet treeSet = new TreeSet(set1);
                    Iterator iterator4 = treeSet.iterator();

                    while (iterator4.hasNext()) {
                        NaturalLoop naturalLoop5 = (NaturalLoop) iterator4.next();
                        if (setMultiMap1.containsKey(naturalLoop5)) {
                            naturalLoop10 = naturalLoop5;
                            break;
                        }
                    }

                    if (naturalLoop10 == null) {
                        naturalLoop10 = (NaturalLoop) treeSet.iterator().next();
                    }

                    iterator4 = arrayList1.iterator();

                    while (iterator4.hasNext()) {
                        GenericTree genericTree3 = (GenericTree) iterator4.next();
                        if (genericTree3.moveToValue(naturalLoop10)) {
                            genericTree3.addChild(naturalLoop11);
                            break;
                        }
                    }
                }
            }
        }

        return arrayList1;
    }

    private void propagateFrameToSuccessor(int predecessorIndex, int successorIndex, TreeSet pendingInstructionIndices, boolean verifyTypes, String diagnosticContext) throws ZkmException, IOException {
        Instruction successorInstruction = (Instruction) this.instructions.get(successorIndex);

        StackFrameState successorFrame;
        try {
            ObservableHolder observableHolder = new ObservableHolder();
            StackFrameState predecessorFrame = this.frameStates[predecessorIndex];
            CommonSuperTypeResolver resolver = this.superTypeResolver;
            successorFrame = successorInstruction.computeFrameAfter(predecessorFrame, verifyTypes, resolver, diagnosticContext);
            if (!observableHolder.isValueNull()) {
                AssertionFailedException assertionFailedException = (AssertionFailedException) observableHolder.getValue();
                if (!this.hasPendingPredecessor(predecessorIndex, pendingInstructionIndices)) {
                    throw assertionFailedException;
                }
            }
        } catch (MethodAnalysisException methodAnalysisException) {
            throw methodAnalysisException;
        } catch (StackAnalysisException stackAnalysisException) {
            throw stackAnalysisException;
        } catch (AssertionFailedException assertionFailedException1) {
            throw assertionFailedException1;
        }

        if (this.frameStates[successorIndex] == null) {
            this.frameStates[successorIndex] = successorFrame;
            pendingInstructionIndices.add(INTEGER_CACHE.valueOf(successorIndex));
        } else {
            BooleanFlag frameChanged = new BooleanFlag();
            StackFrameState mergedFrame = this.frameStates[successorIndex].merge(this.superTypeResolver, successorFrame, frameChanged, diagnosticContext);
            if (frameChanged.getValue()) {
                this.frameStates[successorIndex] = mergedFrame;
                pendingInstructionIndices.add(INTEGER_CACHE.valueOf(successorIndex));
            }
        }
    }

    public void assignExceptionHandlers(TryCatchBlockInfo[][] tryCatchBlockInfos, TryCatchBlockInfo[] tryCatchBlockInfos1) throws ZkmException, IOException {
        ArrayList arrayList = new ArrayList();
        Iterator iterator = this.basicBlocks.iterator();

        while (iterator.hasNext()) {
            BasicBlock basicBlock = (BasicBlock) iterator.next();
            int startIndex = basicBlock.getStartIndex();
            int endIndex = basicBlock.getEndIndex();

            for (int startIndexValue = startIndex; startIndexValue <= endIndex; startIndexValue++) {
                Instruction instruction1 = (Instruction) this.instructions.get(startIndexValue);
                TryCatchBlockInfo[] tryCatchBlockInfos2 = this.getActiveHandlersAt(startIndexValue, tryCatchBlockInfos1);

                for (int tryCatchBlockInfos2ElementIndex = 0; tryCatchBlockInfos2ElementIndex < tryCatchBlockInfos2.length; tryCatchBlockInfos2ElementIndex++) {
                    if (this.handlerCoveredBlocks.putValue(tryCatchBlockInfos2[tryCatchBlockInfos2ElementIndex].handlerBlock, basicBlock, basicBlock) == null) {
                        basicBlock.addExceptionHandler(tryCatchBlockInfos2[tryCatchBlockInfos2ElementIndex].handlerBlock);
                    }

                    if (startIndexValue == tryCatchBlockInfos2[tryCatchBlockInfos2ElementIndex].startIndex || startIndexValue == startIndex || instruction1.isStore() || !arrayList.contains(tryCatchBlockInfos2[tryCatchBlockInfos2ElementIndex])) {
                        arrayList.add(tryCatchBlockInfos2[tryCatchBlockInfos2ElementIndex]);
                    }
                }

                if (arrayList.size() > 0) {
                    TryCatchBlockInfo[] tryCatchBlockInfos3 = new TryCatchBlockInfo[arrayList.size()];
                    tryCatchBlockInfos[startIndexValue] = ((com.zelix.klassmaster.classfile.insn.TryCatchBlockInfo[]) (arrayList.toArray(tryCatchBlockInfos3)));
                    arrayList.clear();
                }
            }
        }
    }

    private List orderSubroutines(Set set1) throws MethodAnalysisException {
        ArrayList arrayList = new ArrayList(set1.size());
        if (set1.size() <= 1) {
            if (set1.size() == 1) {
                arrayList.add(set1.iterator().next());
            }

            return arrayList;
        } else {
            TwoKeyMap twoKeyMap = new TwoKeyMap();
            Iterator iterator = set1.iterator();

            while (iterator.hasNext()) {
                BasicBlock basicBlock = (BasicBlock) iterator.next();
                this.collectNestedSubroutineCalls(basicBlock, basicBlock, twoKeyMap, ZkmUtils.createHashSet(13), false);
            }

            HashMap hashMap = ZkmUtils.createHashMap(Math.max(ZkmUtils.getPrimeCapacity(set1.size()), 7));
            TwoKeyMap twoKeyMap1 = ZkmUtils.copyTwoKeyMap(twoKeyMap);
            Enumeration enumeration = twoKeyMap1.keys();

            while (enumeration.hasMoreElements()) {
                BasicBlock basicBlock1 = (BasicBlock) enumeration.nextElement();
                Iterator iterator1 = twoKeyMap1.getInnerMap(basicBlock1).keySet().iterator();

                while (iterator1.hasNext()) {
                    BasicBlock basicBlock2 = (BasicBlock) iterator1.next();
                    BasicBlock basicBlock3 = ((com.zelix.klassmaster.classfile.insn.BasicBlock) (hashMap.put(basicBlock2, basicBlock1)));
                    if (basicBlock3 != null) {
                        boolean valueValue = (Boolean) twoKeyMap1.getValue(basicBlock1, basicBlock2);
                        boolean valueValue2 = (Boolean) twoKeyMap1.getValue(basicBlock3, basicBlock2);
                        if (valueValue2 && !valueValue) {
                            twoKeyMap.removeValue(basicBlock3, basicBlock2);
                        } else if (!valueValue2 && valueValue) {
                            hashMap.put(basicBlock2, basicBlock3);
                            twoKeyMap.removeValue(basicBlock1, basicBlock2);
                        } else {
                            if (valueValue2 && valueValue) {
                                throw new MethodAnalysisException(
                                        "Method '" + this.methodDescription + "' could not be analyzed (C). No action required.", this.methodDescription
                                );
                            }

                            if (!valueValue2 && !valueValue) {
                                ZkmAssert.assertTrue(false, new String[]{"Method '" + this.methodDescription + "' could not be analyzed (D)"});
                            }
                        }
                    }
                }
            }

            Iterator iterator2 = set1.iterator();

            while (iterator2.hasNext()) {
                BasicBlock basicBlock4 = (BasicBlock) iterator2.next();
                if (!hashMap.containsKey(basicBlock4)) {
                    this.collectSubroutineOrder(basicBlock4, twoKeyMap, arrayList);
                }
            }

            if (arrayList.size() != set1.size()) {
                throw new MethodAnalysisException("Method '" + this.methodDescription + "' could not be analyzed (E). No action required.", this.methodDescription);
            } else {
                return arrayList;
            }
        }
    }

    public boolean propagateHandlerLiveness(Collection collection1) {
        boolean bl = false;
        Iterator iterator = collection1.iterator();

        while (iterator.hasNext()) {
            BasicBlock basicBlock = (BasicBlock) iterator.next();
            BitSet bitSet = this.frameStates[basicBlock.getStartIndex()].getLiveLocals();
            ArrayList arrayList = new ArrayList(this.handlerCoveredBlocks.getInnerMap(basicBlock).keySet());
            Collections.sort(arrayList);
            Collections.reverse(arrayList);
            Iterator iterator1 = arrayList.iterator();

            while (iterator1.hasNext()) {
                BasicBlock basicBlock1 = (BasicBlock) iterator1.next();
                int lastCoveredInstructionIndex = this.getLastCoveredInstructionIndex(basicBlock1, basicBlock);
                if (this.propagateLivenessBackward(basicBlock1, lastCoveredInstructionIndex, (BitSet) bitSet.clone())) {
                    bl = true;
                }
            }
        }

        return bl;
    }

    public static NonNullList getInstructions(MethodFlowAnalyzer methodFlowAnalyzer) {
        return methodFlowAnalyzer.instructions;
    }

    public Set getLivenessStartBlocks() {
        TreeSet<BasicBlock> treeSet = new TreeSet<>((basicBlockx, basicBlock1x) -> basicBlock1x.compareStart(basicBlockx));
        Iterator iterator = this.basicBlocks.iterator();

        while (iterator.hasNext()) {
            BasicBlock basicBlock = (BasicBlock) iterator.next();
            if (!basicBlock.hasSuccessors()) {
                treeSet.add(basicBlock);
            } else {
                List list1 = basicBlock.getSuccessorsCopy();
                boolean bl = false;
                Iterator iterator1 = list1.iterator();

                while (iterator1.hasNext()) {
                    BasicBlock basicBlock1 = (BasicBlock) iterator1.next();
                    if (basicBlock1.hasPredecessors() && basicBlock1.getPredecessors().contains(basicBlock)) {
                        bl = true;
                    }
                }

                if (!bl) {
                    treeSet.add(basicBlock);
                }
            }
        }

        return treeSet;
    }

    public boolean isLocalLivenessConsistent() {
        return this.localLivenessConsistent;
    }

    public BasicBlock getBlockContaining(int blockByIndexCacheElementIndex) {
        if (this.blockByIndexCache == null) {
            this.blockByIndexCache = new BasicBlock[this.methodBytecode.getInstructionCount()];
        } else if (this.blockByIndexCache[blockByIndexCacheElementIndex] != null) {
            return this.blockByIndexCache[blockByIndexCacheElementIndex];
        }

        int bb = 0;
        int bc = this.basicBlocks.size() - 1;

        while (bb <= bc) {
            int elementIndex = (bb + bc) / 2;
            BasicBlock basicBlock = (BasicBlock) this.basicBlocks.get(elementIndex);
            if (blockByIndexCacheElementIndex < basicBlock.getStartIndex()) {
                bc = elementIndex - 1;
            } else {
                if (blockByIndexCacheElementIndex <= basicBlock.getEndIndex()) {
                    this.blockByIndexCache[blockByIndexCacheElementIndex] = basicBlock;
                    return basicBlock;
                }

                bb = elementIndex + 1;
            }
        }

        return null;
    }

    public static void collectIntConstantSources(MethodFlowAnalyzer methodFlowAnalyzer, List list1, BasicBlock basicBlock, int ba, int bb, Set set1) {
        methodFlowAnalyzer.collectIntConstantSourcesFrom(list1, basicBlock, ba, bb, set1);
    }

    public void traceNewInstanceSource(ValueTraceFrame valueTraceFrame, BasicBlock basicBlock, int elementIndex) throws ZkmException, IOException {
        ConstantRefInstruction constantRefInstruction = (ConstantRefInstruction) this.instructions.get(elementIndex);
        String className = ((ResolvedClassConstant) constantRefInstruction.getConstantPoolEntry()).getClassName();
        if (className.equals("java/util/Properties")) {
            StackFrameState stackFrameState = this.frameStates[elementIndex];
            PropertiesLoadTracker propertiesLoadTracker = this.createPropertiesLoadTracker();
            this.traceStackValueUses(valueTraceFrame, propertiesLoadTracker, basicBlock, elementIndex + 1, stackFrameState.getStack().length - 1);
            HashSet hashSet = propertiesLoadTracker.getFileNameValues();
            HashSet hashSet1;
            if (hashSet.size() == 0) {
                hashSet.add(new InstructionReasonNote(constantRefInstruction, "Couldn't determine property file name"));
                hashSet1 = valueTraceFrame.results;
            } else {
                hashSet1 = valueTraceFrame.results;
            }

            hashSet1.addAll(hashSet);
        } else {
            valueTraceFrame.results.add(new InstructionReasonNote(constantRefInstruction, "NEW instruction not handled with : " + className));
        }
    }

    public void traceStackValueUses(ValueTraceFrame valueTraceFrame, ValueFlowTracker valueFlowTracker, BasicBlock basicBlock, int ba, int bb) throws ZkmException, IOException {
        int integerArrayElement = bb;
        int integerArrayElementValue = integerArrayElement;
        int be = ba;
        if (!valueFlowTracker.isVisited(1, be, integerArrayElementValue)) {
            valueFlowTracker.markVisited(1, ba, integerArrayElement);
            boolean bl = false;
            int endIndex = basicBlock.getEndIndex();

            for (int elementIndexValue2 = ba; !bl && elementIndexValue2 <= endIndex; elementIndexValue2++) {
                Instruction instruction1 = (Instruction) this.instructions.get(elementIndexValue2);
                StackFrameState stackFrameState = this.frameStates[elementIndexValue2];
                if (instruction1.isStackManipulation()) {
                    int[] integerArray = instruction1.mapStackSlotForward(this.frameStates[elementIndexValue2 - 1].getStack(), stackFrameState.getStack(), integerArrayElement);
                    if (integerArray.length == 0) {
                        bl = true;
                    } else if (integerArray.length == 2) {
                        this.traceStackValueUses(valueTraceFrame, valueFlowTracker, basicBlock, elementIndexValue2 + 1, integerArray[1]);
                    }

                    if (integerArray.length > 0) {
                        integerArrayElement = integerArray[0];
                    }
                } else if (instruction1.consumesStackSlot(integerArrayElement, stackFrameState.getStack().length)) {
                    if (!valueFlowTracker.traceAtInstruction(valueTraceFrame, elementIndexValue2, basicBlock)) {
                        switch (instruction1.getOpcode()) {
                            case 179:
                            case 181:
                                this.traceFieldWriteUses(valueTraceFrame, valueFlowTracker, elementIndexValue2);
                                break;
                            case 255:
                                LocalVariableInstruction localVariableInstruction = (LocalVariableInstruction) instruction1;
                                LocalVariableAccessKind localVariableAccessKind = localVariableInstruction.getAccessKind();
                                localVariableInstruction.getAccessKind();
                                if (localVariableAccessKind == LocalVariableAccessKind.OBJECT_STORE) {
                                    this.traceLocalVariableUses(valueTraceFrame, valueFlowTracker, basicBlock, elementIndexValue2 + 1, localVariableInstruction.getLocalIndex());
                                }
                        }
                    }

                    bl = true;
                }
            }

            if (!bl) {
                List list1 = basicBlock.getSuccessorsCopy();
                if (list1 != null && list1.size() > 0) {
                    for (int elementIndex = 0; elementIndex < list1.size(); elementIndex++) {
                        BasicBlock basicBlock1 = (BasicBlock) list1.get(elementIndex);
                        this.traceStackValueUses(valueTraceFrame, valueFlowTracker, basicBlock1, basicBlock1.getStartIndex(), integerArrayElement);
                    }
                } else if (basicBlock.hasRetBlocks()) {
                    List list2 = basicBlock.getRetBlocks();

                    for (int elementIndexValue = 0; elementIndexValue < list2.size(); elementIndexValue++) {
                        BasicBlock basicBlock2 = (BasicBlock) list2.get(elementIndexValue);
                        this.traceStackValueUses(valueTraceFrame, valueFlowTracker, basicBlock2, basicBlock2.getStartIndex(), integerArrayElement);
                    }
                } else {
                    Instruction instruction2 = (Instruction) this.instructions.get(endIndex);
                }
            }
        }
    }

    public int getStackDepthAt(int frameStateIndex) {
        return this.frameStates[frameStateIndex].getStackDepth();
    }

    public BasicBlock getBlock() {
        if (0 >= this.basicBlocks.size()) {
            throw new IllegalArgumentException("Invalid index : " + 0 + " > " + this.basicBlocks.size());
        } else {
            return (BasicBlock) this.basicBlocks.get(0);
        }
    }

    public void traceLocalVariableSource(ValueTraceFrame valueTraceFrame, BasicBlock basicBlock, int integerArgument, int parameterIndexArgument) throws ZkmException, IOException {
        Triple triple = new Triple(this.methodBytecode, INTEGER_CACHE.valueOf(integerArgument), INTEGER_CACHE.valueOf(parameterIndexArgument));
        valueTraceFrame.visitCounts.add(triple);
        if (valueTraceFrame.visitCounts.getCount(triple) > 9) {
            valueTraceFrame.results.add(new InstructionReasonNote(null, "Possible loop"));
        } else {
            HashSet hashSet = (HashSet) valueTraceFrame.localSourceCache.get(triple);
            int startIndex = basicBlock.getStartIndex();
            if (hashSet != null
                    && integerArgument - 1 >= startIndex
                    && valueTraceFrame.argumentKind != ReflectionArgumentKind.USER_DEFINED_METHOD_1
                    && valueTraceFrame.argumentKind != ReflectionArgumentKind.USER_DEFINED_METHOD_2) {
                valueTraceFrame.results.addAll(hashSet);
            } else {
                boolean bl = false;
                hashSet = ZkmUtils.createHashSet(13);
                HashSet hashSet1 = valueTraceFrame.swapResults(hashSet);

                for (int elementIndexValue2 = integerArgument - 1; elementIndexValue2 >= startIndex; elementIndexValue2 += -1) {
                    Instruction instruction1 = (Instruction) this.instructions.get(elementIndexValue2);
                    StackFrameState stackFrameState = this.frameStates[elementIndexValue2];
                    if (instruction1.isStoreTo(parameterIndexArgument)) {
                        if (elementIndexValue2 > startIndex) {
                            this.traceStackValueSource(valueTraceFrame, basicBlock, elementIndexValue2, stackFrameState.getStackDepth());
                            bl = true;
                            break;
                        }

                        List list1 = basicBlock.getPredecessors();
                        if (list1 != null && list1.size() > 0) {
                            for (int elementIndex = 0; elementIndex < list1.size(); elementIndex++) {
                                BasicBlock basicBlock1 = (BasicBlock) list1.get(elementIndex);
                                this.traceStackValueSource(valueTraceFrame, basicBlock1, basicBlock1.getEndIndex() + 1, stackFrameState.getStackDepth());
                            }

                            bl = true;
                            break;
                        }
                    }
                }

                if (!bl) {
                    List list2 = basicBlock.getPredecessors();
                    if (list2 != null && list2.size() > 0) {
                        for (int elementIndexValue = 0; elementIndexValue < list2.size(); elementIndexValue++) {
                            BasicBlock basicBlock2 = (BasicBlock) list2.get(elementIndexValue);
                            this.traceLocalVariableSource(valueTraceFrame, basicBlock2, basicBlock2.getEndIndex() + 1, parameterIndexArgument);
                        }
                    } else if (basicBlock.isCatchBlock()) {
                        this.traceLocalThroughCatchHandler(valueTraceFrame, basicBlock, parameterIndexArgument);
                    } else if (basicBlock.isHeader()) {
                        int parameterIndexValue;
                        if (valueTraceFrame.isStaticMethod) {
                            parameterIndexValue = parameterIndexArgument;
                        } else {
                            parameterIndexValue = parameterIndexArgument - 1;
                        }

                        if (parameterIndexValue < 0) {
                            valueTraceFrame.results
                                    .add(new InstructionReasonNote((Instruction) this.instructions.get(startIndex), "Source object reference : " + this.methodDescription));
                        } else if (parameterIndexValue < this.parameterSlotCount) {
                            switch (ReflectionAnalysisSwitchMap.ARGUMENT_KIND_SWITCH[valueTraceFrame.argumentKind.ordinal()]) {
                                case 1:
                                case 2:
                                    MethodInfo methodInfo1 = (MethodInfo) this.methodBytecode.getMethod();
                                    if (!methodInfo1.isStrictlyPrivate()) {
                                        valueTraceFrame.setComplete();
                                    }

                                    this.traceParameterFromCallers(valueTraceFrame, parameterIndexValue);
                                    break;
                                case 3:
                                case 4:
                                    valueTraceFrame.results
                                            .add(
                                                    new InstructionReasonNote(
                                                            (Instruction) this.instructions.get(startIndex), "Source is method parameter: " + valueTraceFrame.argumentKind
                                                    )
                                            );
                                    break;
                                case 5:
                                    valueTraceFrame.results.add(new MethodParameterValue(parameterIndexValue, this.methodBytecode));
                            }
                        } else {
                            valueTraceFrame.results
                                    .add(
                                            new InstructionReasonNote(
                                                    (Instruction) this.instructions.get(startIndex),
                                                    "Variable Uninitialized : "
                                                            + parameterIndexValue
                                                            + " "
                                                            + this.parameterSlotCount
                                                            + " "
                                                            + valueTraceFrame.argumentKind
                                                            + " "
                                                            + this.methodDescription
                                            )
                                    );
                        }
                    } else {
                        Instruction instruction2 = (Instruction) this.instructions.get(startIndex);
                        valueTraceFrame.results.add(new InstructionReasonNote(instruction2, "No source found 2) : " + valueTraceFrame.argumentKind));
                    }
                }

                hashSet = valueTraceFrame.swapResults(hashSet1);
                hashSet1.addAll(hashSet);
                if (integerArgument - 1 >= startIndex
                        && valueTraceFrame.argumentKind != ReflectionArgumentKind.USER_DEFINED_METHOD_1
                        && valueTraceFrame.argumentKind != ReflectionArgumentKind.USER_DEFINED_METHOD_2) {
                    HashSet hashSet2 = ((java.util.HashSet) (valueTraceFrame.localSourceCache.put(triple, hashSet)));
                    if (hashSet2 != null && hashSet2.size() > 0) {
                        hashSet.addAll(hashSet2);
                    }
                }
            }
        }
    }

    public void collectSubroutineOrder(BasicBlock basicBlock, TwoKeyMap twoKeyMap, List list1) {
        list1.add(basicBlock);
        Map map1 = twoKeyMap.getInnerMap(basicBlock);
        if (map1 != null) {
            Iterator iterator = map1.keySet().iterator();

            while (iterator.hasNext()) {
                this.collectSubroutineOrder((BasicBlock) iterator.next(), twoKeyMap, list1);
            }
        }
    }

    public Set getMonitorTypes() {
        IdentityMapWrapper identityMapWrapper = new IdentityMapWrapper();
        Iterator iterator = this.instructions.iterator();

        while (iterator.hasNext()) {
            Instruction instruction1 = (Instruction) iterator.next();
            if (instruction1.isMonitor()) {
                VerifierType verifierType = ((MonitorInstruction) instruction1).getLockedObjectType();
                identityMapWrapper.put(verifierType, verifierType);
            }
        }

        return identityMapWrapper.keySet();
    }

    public List getBlocksCopy() {
        return new ArrayList(this.basicBlocks);
    }

    public void reorderSwitchSuccessors() {
        NonNullList nonNullList = new NonNullList(this.instructions);

        for (int elementIndex = 0; elementIndex < this.basicBlocks.size(); elementIndex++) {
            BasicBlock basicBlock = (BasicBlock) this.basicBlocks.get(elementIndex);
            if (basicBlock.getLastInstruction(nonNullList) instanceof SwitchInstruction) {
                basicBlock.sortSuccessors();
            }
        }
    }

    public boolean computeLocalLiveness() {
        Iterator iterator = this.getLivenessStartBlocks().iterator();

        while (iterator.hasNext()) {
            BasicBlock basicBlock = (BasicBlock) iterator.next();
            BitSet bitSet = new BitSet(this.maxLocals);
            if (this.methodBytecode.isConstructor()) {
                bitSet.set(0);
            }

            this.propagateLivenessBackward(basicBlock, basicBlock.getEndIndex(), bitSet);
        }

        this.propagateHandlerLiveness(this.catchHandlerBlocks);
        ArrayList arrayList = this.buildLoopNestingTrees();
        Iterator iterator3 = arrayList.iterator();

        while (iterator3.hasNext()) {
            GenericTree genericTree = (GenericTree) iterator3.next();
            Iterator iterator1 = genericTree.nodeIterator();

            while (iterator1.hasNext()) {
                ObjectTreeNode objectTreeNode = (ObjectTreeNode) iterator1.next();
                NaturalLoop naturalLoop = (NaturalLoop) objectTreeNode.getValue();
                BasicBlock basicBlock1 = naturalLoop.getHeader();
                BitSet bitSet1 = this.frameStates[basicBlock1.getStartIndex()].getLiveLocals();
                List list1 = naturalLoop.getLatchBlocks();
                Iterator iterator2 = list1.iterator();

                while (iterator2.hasNext()) {
                    BasicBlock basicBlock2 = (BasicBlock) iterator2.next();
                    this.propagateLivenessBackward(basicBlock2, basicBlock2.getEndIndex(), (BitSet) bitSet1.clone());
                }
            }
        }

        int bc = -1;

        boolean bl;
        do {
            bl = this.propagateHandlerLiveness(this.catchHandlerBlocks);
        } while (bl && ++bc < 10);

        boolean bl1 = !bl;
        if (bl1) {
            Iterator iterator4 = this.naturalLoops.iterator();

            while (bl1 && iterator4.hasNext()) {
                NaturalLoop naturalLoop1 = (NaturalLoop) iterator4.next();
                BasicBlock basicBlock4 = naturalLoop1.getHeader();
                BitSet bitSet4 = this.frameStates[basicBlock4.getStartIndex()].getLiveLocals();
                Iterator iterator6 = naturalLoop1.getLatchBlocks().iterator();

                while (bl1 && iterator6.hasNext()) {
                    BasicBlock basicBlock5 = (BasicBlock) iterator6.next();
                    BitSet bitSet5 = this.frameStates[basicBlock5.getEndIndex()].getLiveLocals();

                    for (int i = 0; i < bitSet5.size(); i++) {
                        if (bitSet4.get(i) && !bitSet5.get(i)) {
                            bl1 = false;
                            break;
                        }
                    }
                }
            }
        }

        if (bl1) {
            Iterator iterator5 = this.catchHandlerBlocks.iterator();

            while (bl1 && iterator5.hasNext()) {
                BasicBlock basicBlock3 = (BasicBlock) iterator5.next();
                BitSet bitSet3 = this.frameStates[basicBlock3.getStartIndex()].getLiveLocals();
                List list2 = this.tryCatchInfosByHandler.getValues(basicBlock3);
                Iterator iterator7 = list2.iterator();

                while (bl1 && iterator7.hasNext()) {
                    TryCatchBlockInfo tryCatchBlockInfo = (TryCatchBlockInfo) iterator7.next();

                    for (int startIndexValue = tryCatchBlockInfo.startIndex; bl1 && startIndexValue < tryCatchBlockInfo.endIndex; startIndexValue++) {
                        if (this.frameStates[startIndexValue] != null) {
                            BitSet bitSet6 = this.frameStates[startIndexValue].getLiveLocals();

                            for (int j = 0; j < bitSet3.size(); j++) {
                                if (bitSet3.get(j) && !bitSet6.get(j)) {
                                    bl1 = false;
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }

        if (bl1 && !this.isStatic && !this.isConstructor) {
            boolean bl2 = true;

            for (int frameStateIndex = 0; frameStateIndex < this.frameStates.length; frameStateIndex++) {
                StackFrameState stackFrameState = this.frameStates[frameStateIndex];
                if (stackFrameState != null && !stackFrameState.getLocals()[0].isClassType()) {
                    bl2 = false;
                    break;
                }
            }

            if (bl2) {
                BitSet bitSet2 = new BitSet(this.maxLocals);
                bitSet2.set(0);

                for (StackFrameState stackFrameState1 : this.frameStates) {
                    if (stackFrameState1 != null) {
                        stackFrameState1.addLiveLocals(bitSet2);
                    }
                }
            }
        }

        if (!bl1) {
        }

        if (!bl1) {
        }

        return bl1;
    }

    public CallArgumentValues traceReflectionCallArguments(
            int elementIndex,
            ReflectionApiMethod reflectionApiMethod,
            NestedMultiMap nestedMultiMap,
            NestedMultiMap nestedMultiMap1,
            NestedMultiMap nestedMultiMap2,
            Map map1,
            Map map2,
            Map map3,
            Map map4,
            Map map5,
            ClassMemberLookup classMemberLookup1,
            Map map6
    ) throws ZkmException, IOException {
        CallArgumentValues callArgumentValues = new CallArgumentValues(reflectionApiMethod);
        ConstantRefInstruction constantRefInstruction = (ConstantRefInstruction) this.instructions.get(elementIndex);
        BasicBlock basicBlock = this.getBlockContaining(elementIndex);
        StackFrameState stackFrameState = this.frameStates[elementIndex];
        if (stackFrameState == null) {
            HashSet hashSet3 = ZkmUtils.createHashSet(5);
            hashSet3.add(new InstructionReasonNote(constantRefInstruction, "Appears in DEAD code. Can be ignored."));
            callArgumentValues.addArgumentValues(hashSet3);
            return callArgumentValues;
        }

        int stackDepth = stackFrameState.getStackDepth();
        boolean isStaticMethodValue = this.methodBytecode.getMethod().isStatic();
        if (reflectionApiMethod.isObjectNameLookup()) {
            HashSet hashSet = ZkmUtils.createHashSet(5);
            hashSet.add(new InstructionReasonNote(constantRefInstruction, "Analysis not yet implemented"));
            callArgumentValues.addArgumentValues(hashSet);
        } else if (reflectionApiMethod.isFunctionalInterfaceLookup()) {
            if (this.methodBytecode.getMethod().getSourceName().equals("$deserializeLambda$")) {
                HashSet hashSet4 = ZkmUtils.createHashSet(5);
                HashSet hashSet1 = ZkmUtils.createHashSet(5);
                HashSet hashSet2 = ZkmUtils.createHashSet(5);
                ObservableHolder observableHolder = new ObservableHolder();
                ObservableHolder observableHolder1 = new ObservableHolder();
                ObservableHolder observableHolder2 = new ObservableHolder();
                this.traceDeserializeLambdaArguments(elementIndex, observableHolder, observableHolder1, observableHolder2);
                hashSet4.add(observableHolder.getValue());
                hashSet1.add(observableHolder1.getValue());
                hashSet2.add(observableHolder2.getValue());
                callArgumentValues.addArgumentValues(hashSet4);
                callArgumentValues.addArgumentValues(hashSet1);
                callArgumentValues.addArgumentValues(hashSet2);
            } else {
                HashSet hashSet5 = ZkmUtils.createHashSet(5);
                hashSet5.add(new InstructionReasonNote(constantRefInstruction, "Unexpected usage (A) : " + this.methodDescription));
                callArgumentValues.addArgumentValues(hashSet5);
            }
        } else {
            Iterator iterator = reflectionApiMethod.getParamDetails().iterator();

            while (iterator.hasNext()) {
                ReflectionParamDetail reflectionParamDetail = (ReflectionParamDetail) iterator.next();
                int bd = stackDepth + reflectionApiMethod.getArgumentOffset() + reflectionParamDetail.getPosition();
                HashSet hashSet7 = ZkmUtils.createHashSet(13);
                ReflectionArgumentKind reflectionArgumentKind;
                switch (ReflectionAnalysisSwitchMap.PARAM_TYPE_SWITCH[reflectionParamDetail.getParamType().ordinal()]) {
                    case 1:
                        reflectionArgumentKind = ReflectionArgumentKind.CLASS_REFERENCE;
                        break;
                    case 2:
                        reflectionArgumentKind = ReflectionArgumentKind.METHODTYPE_REFERENCE;
                        break;
                    case 3:
                        reflectionArgumentKind = ReflectionArgumentKind.OBJECT_TYPE;
                        break;
                    default:
                        reflectionArgumentKind = ReflectionArgumentKind.NORMAL;
                }

                ValueTraceFrame valueTraceFrame1 = new ValueTraceFrame(
                        this,
                        hashSet7,
                        reflectionParamDetail.isArrayParam(),
                        reflectionArgumentKind,
                        nestedMultiMap,
                        nestedMultiMap1,
                        nestedMultiMap2,
                        this.superTypeResolver,
                        classMemberLookup1,
                        map1,
                        isStaticMethodValue,
                        map2,
                        map3,
                        map4,
                        map5,
                        map6
                );
                this.traceStackValueSource(valueTraceFrame1, basicBlock, elementIndex, bd);
                callArgumentValues.addArgumentValues(hashSet7);
                if (!valueTraceFrame1.isComplete()) {
                    callArgumentValues.setValid();
                }
            }

            if (reflectionApiMethod.isReceiverTargetClass()) {
                int bc = stackDepth - 1;
                HashSet hashSet6 = ZkmUtils.createHashSet(13);
                ValueTraceFrame valueTraceFrame = new ValueTraceFrame(
                        this,
                        hashSet6,
                        false,
                        ReflectionArgumentKind.CLASS_REFERENCE,
                        nestedMultiMap,
                        nestedMultiMap1,
                        nestedMultiMap2,
                        this.superTypeResolver,
                        classMemberLookup1,
                        map1,
                        isStaticMethodValue,
                        map2,
                        map3,
                        map4,
                        map5,
                        map6
                );
                this.traceStackValueSource(valueTraceFrame, basicBlock, elementIndex, bc);
                callArgumentValues.addArgumentValues(hashSet6);
            }
        }

        return callArgumentValues;
    }

    public List findNaturalLoops() {
        this.assignBlockIndices();
        this.reorderSwitchSuccessors();
        int primeCapacity = ZkmUtils.getPrimeCapacity(this.basicBlocks.size());
        TreeMap treeMap = new TreeMap();
        HashSet hashSet = ZkmUtils.createHashSet(primeCapacity);
        HashMap hashMap = ZkmUtils.createHashMap(primeCapacity);
        BitSet bitSet = new BitSet(this.basicBlocks.size());
        bitSet.set(this.entryBlock.getIndex());
        hashSet.add(this.entryBlock);
        BlockBitSet blockBitSet = new BlockBitSet(this.entryBlock, bitSet);
        hashMap.put(this.entryBlock, blockBitSet);
        new NonNullList(this.instructions);
        BasicBlock.findNaturalLoops(blockBitSet, treeMap, hashSet, hashMap);

        for (int elementIndex = 0; elementIndex < this.catchHandlerBlocks.size(); elementIndex++) {
            BasicBlock basicBlock = (BasicBlock) this.catchHandlerBlocks.get(elementIndex);
            if (!hashSet.contains(basicBlock)) {
                BitSet bitSet1 = new BitSet(this.basicBlocks.size());
                hashSet.add(basicBlock);
                BlockBitSet blockBitSet1 = new BlockBitSet(basicBlock, bitSet1);
                hashMap.put(basicBlock, blockBitSet1);
                bitSet1.set(basicBlock.getIndex());
                new NonNullList(this.instructions);
                BasicBlock.findNaturalLoops(blockBitSet1, treeMap, hashSet, hashMap);
            }
        }

        ArrayList arrayList = new ArrayList();
        Iterator iterator = treeMap.values().iterator();

        while (iterator.hasNext()) {
            arrayList.add(iterator.next());
        }

        iterator = arrayList.iterator();

        while (iterator.hasNext()) {
            NaturalLoop naturalLoop = (NaturalLoop) iterator.next();
            naturalLoop.computeLoopBlocks(new NonNullList(this.basicBlocks), this.handlerCoveredBlocks);
        }

        return arrayList;
    }

    private TryCatchBlockInfo[] getActiveHandlersAt(int instructionIndex, TryCatchBlockInfo[] handlers) throws ZkmException, IOException {
        ArrayList activeHandlers = new ArrayList();
        for (int handlerIndex = 0; handlerIndex < handlers.length; handlerIndex++) {
            TryCatchBlockInfo handler = handlers[handlerIndex];
            if (instructionIndex >= handler.startIndex && instructionIndex < handler.endIndex) {
                boolean shadowed = false;
                for (int earlierIndex = 0; earlierIndex < activeHandlers.size(); earlierIndex++) {
                    TryCatchBlockInfo earlierHandler = (TryCatchBlockInfo) activeHandlers.get(earlierIndex);
                    if (handler.catchTypeDescriptor.equals(earlierHandler.catchTypeDescriptor)
                            || this.hierarchyQuery.isSubclass(
                                    handler.catchTypeDescriptor.substring(1, handler.catchTypeDescriptor.length() - 1),
                                    earlierHandler.catchTypeDescriptor.substring(1, earlierHandler.catchTypeDescriptor.length() - 1))) {
                        shadowed = true;
                        break;
                    }
                }
                if (!shadowed) {
                    activeHandlers.add(handler);
                }
            }
        }
        TryCatchBlockInfo[] result = new TryCatchBlockInfo[activeHandlers.size()];
        return (TryCatchBlockInfo[]) activeHandlers.toArray(result);
    }

    public void traceNewArraySource(ValueTraceFrame valueTraceFrame, BasicBlock basicBlock, int elementIndex) throws ZkmException, IOException {
        ConstantRefInstruction constantRefInstruction = (ConstantRefInstruction) this.instructions.get(elementIndex);
        if (elementIndex > basicBlock.getStartIndex()) {
            Instruction instruction1 = (Instruction) this.instructions.get(elementIndex - 1);
            int byteValue;
            switch (instruction1.getOpcode()) {
                case 3:
                    byteValue = 0;
                    break;
                case 4:
                    byteValue = 1;
                    break;
                case 5:
                    byteValue = 2;
                    break;
                case 6:
                    byteValue = 3;
                    break;
                case 7:
                    byteValue = 4;
                    break;
                case 8:
                    byteValue = 5;
                    break;
                case 9:
                case 10:
                case 11:
                case 12:
                case 13:
                case 14:
                case 15:
                default:
                    valueTraceFrame.results
                            .add(new InstructionReasonNote(constantRefInstruction, "Instruction not handled 1) : " + instruction1 + " " + valueTraceFrame.argumentKind));
                    return;
                case 16:
                    byteValue = ((BipushInstruction) instruction1).getByteValue();
            }

            StackFrameState stackFrameState = this.frameStates[elementIndex];
            if (byteValue > 0) {
                ArrayElementTracker arrayElementTracker = this.createArrayElementTracker();
                this.traceStackValueUses(valueTraceFrame, arrayElementTracker, basicBlock, elementIndex + 1, stackFrameState.getStack().length - 1);
                TracedArrayValue tracedArrayValue = new TracedArrayValue(byteValue, arrayElementTracker.getWarnings());

                for (int integerValue = 0; integerValue < byteValue; integerValue++) {
                    HashSet hashSet = arrayElementTracker.getElementValues(integerValue);
                    if (hashSet == null || hashSet.size() == 0) {
                        hashSet = ZkmUtils.createHashSet(3);
                        hashSet.add(new InstructionReasonNote(constantRefInstruction, arrayElementTracker.getWarnings().toString()));
                    }

                    tracedArrayValue.setElementValues(integerValue, hashSet);
                }

                valueTraceFrame.results.add(tracedArrayValue);
            } else {
                valueTraceFrame.results.add(new TracedArrayValue(byteValue));
            }
        } else {
            valueTraceFrame.results.add(new InstructionReasonNote(constantRefInstruction, "Instruction not handled 2) : " + valueTraceFrame.argumentKind));
        }
    }

    public StackFrameState[] copyFrameStates() {
        return this.frameStates.clone();
    }

    public FrameStateKey getFrameStateKey(int frameStateIndex) {
        return this.frameStates[frameStateIndex] != null ? this.frameStates[frameStateIndex].createKey() : null;
    }

    public StackFrameState[] computeStackMapFrameStates() throws ZkmException, IOException {
        Integer[] integers = this.getFrameLabelIndices();
        StackFrameState[] stackFrameStates = new StackFrameState[this.frameStates.length];
        System.arraycopy(this.frameStates, 0, stackFrameStates, 0, this.frameStates.length);
        BooleanFlag booleanFlag = new BooleanFlag();
        Integer[] integers1 = integers;
        int lengthValue = integers1.length;

        for (int integers1ElementIndex = 0; integers1ElementIndex < lengthValue; integers1ElementIndex++) {
            int integers1Element = integers1[integers1ElementIndex];
            if (((LabelInstruction) this.instructions.get(integers1Element)).hasUsageBits(1024)) {
                BasicBlock basicBlock = this.findBlockStartingAt(integers1Element);
                Iterator iterator = this.tryCatchInfosByHandler.getValues(basicBlock).iterator();

                while (iterator.hasNext()) {
                    TryCatchBlockInfo tryCatchBlockInfo = (TryCatchBlockInfo) iterator.next();
                    Integer[] integers2 = integers;
                    int lengthValue2 = integers2.length;

                    for (int integers2ElementIndex = 0; integers2ElementIndex < lengthValue2; integers2ElementIndex++) {
                        int integers2Element = integers2[integers2ElementIndex];
                        if (integers2Element >= tryCatchBlockInfo.startIndex && integers2Element < tryCatchBlockInfo.endIndex) {
                            StackFrameState.mergeLocals(
                                    this.superTypeResolver,
                                    stackFrameStates[integers1Element].getLocals(),
                                    stackFrameStates[integers2Element].getLocals(),
                                    false,
                                    booleanFlag,
                                    "Building special StackMapTable"
                            );
                        }
                    }
                }
            }
        }

        return stackFrameStates;
    }

    public boolean isGetPropertyWithDefaultCall(ResolvedMethodRef resolvedMethodRef, ClassMemberLookup classMemberLookup1) throws ZkmException, IOException {
        return this.isPropertiesMethodCall(resolvedMethodRef, "getProperty(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;", classMemberLookup1);
    }

    private int getLastCoveredInstructionIndex(BasicBlock basicBlock, BasicBlock basicBlock1) {
        int startIndex = basicBlock.getStartIndex();
        Iterator iterator = this.tryCatchInfosByHandler.getValues(basicBlock1).iterator();

        while (iterator.hasNext()) {
            TryCatchBlockInfo tryCatchBlockInfo = (TryCatchBlockInfo) iterator.next();
            if (basicBlock.getStartIndex() < tryCatchBlockInfo.endIndex && basicBlock.getEndIndex() >= tryCatchBlockInfo.startIndex) {
                int startIndexValue;
                if (tryCatchBlockInfo.endIndex > basicBlock.getEndIndex()) {
                    startIndexValue = basicBlock.getEndIndex();
                } else {
                    startIndexValue = Math.max(0, tryCatchBlockInfo.endIndex - 1);
                }

                if (startIndexValue > startIndex) {
                    startIndex = startIndexValue;
                }
            }
        }

        return startIndex;
    }

    public int getBlockCount() {
        return this.basicBlocks.size();
    }

    public List getNaturalLoops() {
        return this.naturalLoops;
    }

    public int computeMaxStack() {
        if (!this.closed && this.frameStates != null) {
            int stackSlotCountValue = 0;

            for (StackFrameState stackFrameState : this.frameStates) {
                if (stackFrameState != null) {
                    int stackSlotCount = stackFrameState.getStackSlotCount();
                    if (stackSlotCount > stackSlotCountValue) {
                        stackSlotCountValue = stackSlotCount;
                    }
                }
            }

            return stackSlotCountValue;
        } else {
            throw new IllegalStateException("Closed object");
        }
    }

    private void collectReachableBlocks(BasicBlock basicBlock, Set set1) {
        HashSet hashSet = ZkmUtils.createHashSet();
        HashSet hashSet1 = ZkmUtils.createHashSet();
        hashSet.add(basicBlock);

        do {
            hashSet1.clear();
            Iterator iterator = hashSet.iterator();

            while (iterator.hasNext()) {
                BasicBlock basicBlock1 = (BasicBlock) iterator.next();
                if (set1.add(basicBlock1)) {
                    Enumeration enumeration = basicBlock1.enumerateSuccessors();
                    if (enumeration != null) {
                        while (enumeration.hasMoreElements()) {
                            BasicBlock basicBlock2 = (BasicBlock) enumeration.nextElement();
                            hashSet1.add(basicBlock2);
                        }
                    }

                    BasicBlock basicBlock3 = basicBlock1.getJsrReturnBlock();
                    if (basicBlock3 != null) {
                        hashSet1.add(basicBlock3);
                    }
                }
            }

            hashSet.clear();
            hashSet.addAll(hashSet1);
        } while (hashSet.size() > 0);
    }

    public static VerifierType[] createInitialLocals(int ba, boolean bl, List list1, boolean bl1, String string, MutableInt mutableInt) {
        VerifierType[] verifierTypes = VerifierType.createArray(ba);
        int bb = 0;
        if (!bl) {
            bb++;
            verifierTypes[0] = VerifierType.forDescriptor("L" + string + ";", !bl1);
        }

        if (list1 != null) {
            for (int elementIndex = 0; elementIndex < list1.size(); elementIndex++) {
                String key = (String) list1.get(elementIndex);
                verifierTypes[bb++] = VerifierType.forDescriptor(key);
                mutableInt.incrementAndGet();
                if (key.equals("J") || key.equals("D")) {
                    verifierTypes[bb++] = VerifierType.WIDE_SECOND_SLOT;
                    mutableInt.incrementAndGet();
                }
            }
        }

        return verifierTypes;
    }

    public ArrayElementTracker createArrayElementTracker() {
        return new ArrayElementTracker(this);
    }

    public void traceDeserializeLambdaArguments(
            int elementIndex, ObservableHolder observableHolder, ObservableHolder observableHolder1, ObservableHolder observableHolder2
    ) throws ZkmException, IOException {
        Instruction instruction1 = (Instruction) this.instructions.get(elementIndex);
        if (this.instructions.size() > elementIndex + 1) {
            Instruction instruction2 = (Instruction) this.instructions.get(elementIndex + 1);
            if (instruction2.isStringConstantLoad()) {
                ResolvedStringConstant resolvedStringConstant;
                if (instruction2.getOpcode() == 18) {
                    resolvedStringConstant = (ResolvedStringConstant) ((LdcInstruction) instruction2).getConstantPoolEntry();
                } else {
                    resolvedStringConstant = (ResolvedStringConstant) ((ConstantRefInstruction) instruction2).getConstantPoolEntry();
                }

                observableHolder.setValue(new StringConstantNameRef(resolvedStringConstant));
                ResolvedStringConstant resolvedStringConstant1 = null;

                for (int elementIndexValue = elementIndex - 1; elementIndexValue > 0; elementIndexValue += -1) {
                    Instruction instruction3 = (Instruction) this.instructions.get(elementIndexValue);
                    if (instruction3.getOpcode() == 182) {
                        ResolvedMethodRefConstant resolvedMethodRefConstant = (ResolvedMethodRefConstant) ((ConstantRefInstruction) instruction3)
                                .getConstantPoolEntry();
                        if (resolvedMethodRefConstant.getMemberName().equals("getFunctionalInterfaceClass")) {
                            Instruction instruction4 = (Instruction) this.instructions.get(elementIndexValue + 1);
                            if (instruction4.isStringConstantLoad()) {
                                if (instruction4.getOpcode() == 18) {
                                    resolvedStringConstant1 = (ResolvedStringConstant) ((LdcInstruction) instruction4).getConstantPoolEntry();
                                } else {
                                    resolvedStringConstant1 = (ResolvedStringConstant) ((ConstantRefInstruction) instruction4).getConstantPoolEntry();
                                }

                                observableHolder2.setValue(new StringConstantNameRef(resolvedStringConstant1));
                            }
                            break;
                        }
                    }
                }

                if (resolvedStringConstant1 != null) {
                    int instructionsSize = this.instructions.size();

                    for (int elementIndexValue2 = elementIndex + 2; elementIndexValue2 < instructionsSize; elementIndexValue2++) {
                        Instruction instruction6 = (Instruction) this.instructions.get(elementIndexValue2);
                        if (instruction6.getOpcode() == 182
                                && ((ResolvedMethodRefConstant) ((ConstantRefInstruction) instruction6).getConstantPoolEntry())
                                .getMemberName()
                                .equals("getFunctionalInterfaceMethodSignature")) {
                            if (elementIndexValue2 + 1 < instructionsSize) {
                                Instruction instruction5 = (Instruction) this.instructions.get(elementIndexValue2 + 1);
                                if (instruction5.isStringConstantLoad()) {
                                    ResolvedStringConstant resolvedStringConstant2;
                                    if (instruction5.getOpcode() == 18) {
                                        resolvedStringConstant2 = (ResolvedStringConstant) ((LdcInstruction) instruction5).getConstantPoolEntry();
                                    } else {
                                        resolvedStringConstant2 = (ResolvedStringConstant) ((ConstantRefInstruction) instruction5).getConstantPoolEntry();
                                    }

                                    observableHolder1.setValue(new StringConstantNameRef(resolvedStringConstant2));
                                }
                            }
                            break;
                        }
                    }

                    if (observableHolder1.isValueNull()) {
                        observableHolder1.setValue(
                                new InstructionReasonNote(instruction1, "Unexpected usage (B) : " + instruction1.getMnemonic() + " : " + this.methodDescription)
                        );
                    }
                } else {
                    observableHolder1.setValue(
                            new InstructionReasonNote(instruction1, "Unexpected usage (C) : " + instruction1.getMnemonic() + " : " + this.methodDescription)
                    );
                    observableHolder2.setValue(new InstructionReasonNote(instruction1, "Unexpected usage (D) "));
                }
            } else {
                observableHolder.setValue(
                        new InstructionReasonNote(instruction2, "Unexpected instruction : " + instruction2.getMnemonic() + " : " + this.methodDescription)
                );
                observableHolder1.setValue(new InstructionReasonNote(instruction1, "Unexpected usage (E) "));
                observableHolder2.setValue(new InstructionReasonNote(instruction1, "Unexpected usage (F) "));
            }
        } else {
            observableHolder.setValue(
                    new InstructionReasonNote(instruction1, "Unexpected usage (G) : " + this.methodDescription + " : " + elementIndex + " : " + this.instructions.size())
            );
            observableHolder1.setValue(new InstructionReasonNote(instruction1, "Unexpected usage (H) "));
            observableHolder2.setValue(new InstructionReasonNote(instruction1, "Unexpected usage (I) "));
        }
    }

    public void traceParameterFromCallers(ValueTraceFrame valueTraceFrame, int ba) throws IOException {
        MethodInfo methodInfo1 = (MethodInfo) this.methodBytecode.getMethod();
        int callStackOffset = methodInfo1.getCallStackOffset();
        MethodKey methodKey = methodInfo1.toMethodKey();
        if (valueTraceFrame.enterMethod(methodKey)) {
            ListMultimap listMultimap = valueTraceFrame.methodCallSites.getMultimap(methodKey);
            if (listMultimap != null) {
                Enumeration enumeration = listMultimap.keys();

                while (enumeration.hasMoreElements()) {
                    MethodBytecode methodBytecode1 = (MethodBytecode) enumeration.nextElement();

                    try {
                        MethodFlowAnalyzer methodFlowAnalyzer1 = methodBytecode1.getCachedFlowAnalyzer(
                                valueTraceFrame.flowAnalyzerCache, valueTraceFrame.superTypeResolver, this.hierarchyQuery
                        );
                        List list1 = listMultimap.getValues(methodBytecode1);

                        for (int elementIndex = 0; elementIndex < list1.size(); elementIndex++) {
                            int integerValue = (Integer) list1.get(elementIndex);
                            StackFrameState stackFrameState = methodFlowAnalyzer1.frameStates[integerValue];
                            if (stackFrameState != null) {
                                int be = stackFrameState.getStackDepth() + callStackOffset + methodInfo1.getParameterIndexForSlot(ba);
                                ValueTraceFrame valueTraceFrame1 = new ValueTraceFrame(this, valueTraceFrame, methodBytecode1.getMethod().isStatic());
                                BasicBlock basicBlock = methodFlowAnalyzer1.getBlockContaining(integerValue);
                                methodFlowAnalyzer1.traceStackValueSource(valueTraceFrame1, basicBlock, integerValue, be);
                            }
                        }
                    } catch (ZkmException zkmException) {
                        valueTraceFrame.results
                                .add(new InstructionReasonNote(null, "Exception '" + zkmException.getMessage() + "' : " + methodBytecode1.getClassName()));
                    }
                }
            }

            valueTraceFrame.exitMethod(methodKey);
        }
    }

    public boolean isClosed() {
        return this.closed;
    }

    public StackMapEntry[] buildStackMapEntries(StackMapAttribute stackMapAttribute, ConstantPool constantPool1, Set set1, List list1, Boolean boolean1) throws MethodAnalysisException {
        if (this.hasSubroutineCalls) {
            throw new MethodAnalysisException("'" + this.methodDescription + "' not already preverified", this.methodDescription);
        }

        Integer[] integers = this.getFrameLabelIndices();
        HashMap hashMap = ZkmUtils.createHashMap();
        HashMap hashMap1 = ZkmUtils.createHashMap();
        StackMapEntry[] stackMapEntrys = new StackMapEntry[integers.length];

        for (int i = 0; i < integers.length; i++) {
            int integerValue = integers[i];
            LabelInstruction labelInstruction = (LabelInstruction) this.instructions.get(integerValue);
            StackFrameState stackFrameState = this.frameStates[integerValue];
            stackMapEntrys[i] = new StackMapEntry(stackMapAttribute, labelInstruction, stackFrameState, constantPool1, set1, list1, boolean1, hashMap, hashMap1);
        }

        return stackMapEntrys;
    }

    public int getShortestReturnPathLength() {
        return this.shortestReturnPathLength;
    }

    public void traceMethodCallSource(ValueTraceFrame valueTraceFrame, BasicBlock basicBlock, int callInstructionIndex) throws ZkmException, IOException {
        ConstantRefInstruction constantRefInstruction = (ConstantRefInstruction) this.instructions.get(callInstructionIndex);
        ResolvedMethodRef resolvedMethodRef = (ResolvedMethodRef) constantRefInstruction.getConstantPoolEntry();
        switch (ReflectionAnalysisSwitchMap.ARGUMENT_KIND_SWITCH[valueTraceFrame.argumentKind.ordinal()]) {
            case 1:
                if (this.isGetPropertyWithDefaultCall(resolvedMethodRef, valueTraceFrame.memberLookup)) {
                    if (valueTraceFrame.tracingPropertiesLoad) {
                        valueTraceFrame.results.add(new InstructionReasonNote(constantRefInstruction, "Possible \"getProperty\" call loop"));
                    } else {
                        HashSet hashSet1 = ZkmUtils.createHashSet(13);
                        HashSet hashSet4 = valueTraceFrame.swapResults(hashSet1);
                        int stackDepth = this.frameStates[callInstructionIndex].getStackDepth();
                        this.traceStackValueSource(valueTraceFrame, basicBlock, callInstructionIndex, stackDepth);
                        HashSet hashSet7 = ZkmUtils.createHashSet(13);
                        valueTraceFrame.swapResults(hashSet7);
                        this.traceStackValueSource(valueTraceFrame, basicBlock, callInstructionIndex, stackDepth + 1);
                        HashSet hashSet9 = ZkmUtils.createHashSet(13);
                        valueTraceFrame.swapResults(hashSet9);
                        this.traceStackValueSource(valueTraceFrame, basicBlock, callInstructionIndex, --stackDepth);
                        hashSet4.add(new PropertyLookupValue(hashSet1, hashSet7, hashSet9));
                        valueTraceFrame.swapResults(hashSet4);
                    }
                } else if (this.isGetPropertyCall(resolvedMethodRef, valueTraceFrame.memberLookup)) {
                    if (valueTraceFrame.tracingPropertiesLoad) {
                        valueTraceFrame.results.add(new InstructionReasonNote(constantRefInstruction, "Possible \"getProperty\" call loop"));
                    } else {
                        HashSet hashSet2 = ZkmUtils.createHashSet(13);
                        HashSet hashSet5 = valueTraceFrame.swapResults(hashSet2);
                        int stackDepthValue = this.frameStates[callInstructionIndex].getStackDepth();
                        this.traceStackValueSource(valueTraceFrame, basicBlock, callInstructionIndex, stackDepthValue);
                        HashSet hashSet8 = ZkmUtils.createHashSet(13);
                        valueTraceFrame.swapResults(hashSet8);
                        this.traceStackValueSource(valueTraceFrame, basicBlock, callInstructionIndex, --stackDepthValue);
                        hashSet5.add(new PropertyLookupValue(hashSet2, hashSet8));
                        valueTraceFrame.swapResults(hashSet5);
                    }
                } else {
                    valueTraceFrame.results
                            .add(
                                    new InstructionReasonNote(
                                            constantRefInstruction, "Method call not handled: '" + resolvedMethodRef.getValueString() + "' : " + valueTraceFrame.argumentKind
                                    )
                            );
                }
                break;
            case 2:
            case 3:
                if (valueTraceFrame.pendingReflectionCalls.containsKey(resolvedMethodRef)) {
                    valueTraceFrame.results.add(new PendingReflectionValue(callInstructionIndex, this.methodBytecode));
                } else {
                    AbstractMethodInfo abstractMethodInfo = (AbstractMethodInfo) resolvedMethodRef.getResolvedMember();
                    if (abstractMethodInfo != null && abstractMethodInfo.isProgramMember()) {
                        MethodInfo methodInfo1 = (MethodInfo) abstractMethodInfo;
                        MethodKey methodKey = methodInfo1.toMethodKey();
                        ReturnedNameSource returnedNameSource = null;
                        if (valueTraceFrame.returnedNameSourceCache.containsKey(methodKey)) {
                            returnedNameSource = (ReturnedNameSource) valueTraceFrame.returnedNameSourceCache.get(methodKey);
                        } else {
                            MethodBytecode methodBytecode1 = methodInfo1.getBytecode();
                            if (methodBytecode1 != null) {
                                try {
                                    MethodFlowAnalyzer methodFlowAnalyzer1 = methodBytecode1.getCachedFlowAnalyzer(
                                            valueTraceFrame.flowAnalyzerCache, valueTraceFrame.superTypeResolver, this.hierarchyQuery
                                    );
                                    returnedNameSource = methodFlowAnalyzer1.resolveReturnedNameSource(valueTraceFrame);
                                    valueTraceFrame.returnedNameSourceCache.put(methodKey, returnedNameSource);
                                } catch (ZkmException zkmException) {
                                    valueTraceFrame.results
                                            .add(
                                                    new InstructionReasonNote(
                                                            constantRefInstruction,
                                                            "Exception '" + zkmException.getMessage() + "' : " + methodInfo1.getClassName() + "." + methodInfo1.getJvmName()
                                                    )
                                            );
                                }
                            } else {
                                valueTraceFrame.returnedNameSourceCache.put(methodKey, null);
                            }
                        }

                        if (returnedNameSource != null) {
                            if (returnedNameSource.hasStringConstant()) {
                                returnedNameSource.getStringConstant();
                                valueTraceFrame.results.add(new StringConstantNameRef(returnedNameSource.getStringConstant()));
                            } else if (returnedNameSource.isParameter()) {
                                int parameterIndex = returnedNameSource.getParameterIndex();
                                int bi = this.frameStates[callInstructionIndex].getStackDepth() + methodInfo1.getCallStackOffset() + methodInfo1.getParameterIndexForSlot(parameterIndex);
                                ValueTraceFrame valueTraceFrame3 = new ValueTraceFrame(this, valueTraceFrame, ReflectionArgumentKind.NORMAL);
                                this.traceStackValueSource(valueTraceFrame3, basicBlock, callInstructionIndex, bi);
                            } else if (returnedNameSource.hasFailureReason()) {
                                valueTraceFrame.results
                                        .add(
                                                new InstructionReasonNote(
                                                        constantRefInstruction,
                                                        returnedNameSource.getFailureReason() + " : " + constantRefInstruction.getConstantPoolEntry().getValueString()
                                                )
                                        );
                            } else if (returnedNameSource.hasArrayValue()) {
                                TracedArrayValue tracedArrayValue = returnedNameSource.getArrayValue();
                                if (tracedArrayValue.hasSingleValuePerElement()) {
                                    ArrayList arrayList = tracedArrayValue.getFirstValues();

                                    for (int elementIndex = 0; elementIndex < arrayList.size(); elementIndex++) {
                                        TrackedValue trackedValue2 = (TrackedValue) arrayList.get(elementIndex);
                                        if (trackedValue2 instanceof MethodParameterValue) {
                                            MethodParameterValue methodParameterValue1 = (MethodParameterValue) trackedValue2;
                                            if (methodParameterValue1.getMethodBytecode() == returnedNameSource.getMethodBytecode()) {
                                                int parameterIndexValue = methodParameterValue1.getParameterIndex();
                                                int bd = this.frameStates[callInstructionIndex].getStackDepth() + methodInfo1.getCallStackOffset() + methodInfo1.getParameterIndexForSlot(parameterIndexValue);
                                                ValueTraceFrame valueTraceFrame2 = new ValueTraceFrame(this, valueTraceFrame, ReflectionArgumentKind.NORMAL);
                                                this.traceStackValueSource(valueTraceFrame2, basicBlock, callInstructionIndex, bd);
                                            } else {
                                                valueTraceFrame.results
                                                        .add(
                                                                new InstructionReasonNote(
                                                                        constantRefInstruction,
                                                                        "Method Parameter : "
                                                                                + methodParameterValue1.getParameterIndex()
                                                                                + " in "
                                                                                + methodParameterValue1.getMethodBytecode().getMethod().buildDeclaration()
                                                                                + " : "
                                                                                + constantRefInstruction.getConstantPoolEntry().getValueString()
                                                                )
                                                        );
                                            }
                                        } else {
                                            valueTraceFrame.results.add(trackedValue2);
                                        }
                                    }
                                } else {
                                    valueTraceFrame.results
                                            .add(
                                                    new InstructionReasonNote(
                                                            constantRefInstruction, "Multiple sources : " + constantRefInstruction.getConstantPoolEntry().getValueString()
                                                    )
                                            );
                                }
                            }
                        } else {
                            valueTraceFrame.results
                                    .add(
                                            new InstructionReasonNote(
                                                    constantRefInstruction,
                                                    "Method was abstract or in an interface: "
                                                            + valueTraceFrame.argumentKind
                                                            + " : "
                                                            + constantRefInstruction.getConstantPoolEntry().getValueString()
                                            )
                                    );
                        }
                    } else {
                        HashSet hashSet10;
                        if (resolvedMethodRef.getReferencedClassName().equals("java/lang/Object")) {
                            if (resolvedMethodRef.getSignatureString().equals("getClass()Ljava/lang/Class;")) {
                                if (callInstructionIndex > basicBlock.getStartIndex()) {
                                    StackFrameState stackFrameState1 = this.frameStates[callInstructionIndex - 1];
                                    String descriptor = stackFrameState1.getStack()[this.frameStates[callInstructionIndex].getStackDepth() - 1].getDescriptor();
                                    descriptor = CommonSuperTypeResolver.stripClassDescriptor(descriptor);
                                    valueTraceFrame.results.add(new TracedObjectType(constantRefInstruction, descriptor));
                                    break;
                                }

                                hashSet10 = valueTraceFrame.results;
                            } else {
                                hashSet10 = valueTraceFrame.results;
                            }
                        } else {
                            hashSet10 = valueTraceFrame.results;
                        }

                        hashSet10.add(new InstructionReasonNote(constantRefInstruction, "Method call not internal: " + valueTraceFrame.argumentKind));
                    }
                }
                break;
            case 4:
                ReflectionApiMethod reflectionApiMethod = resolvedMethodRef.getExactReflectionApiMethod();
                if (reflectionApiMethod != null) {
                    List list1 = reflectionApiMethod.getParamDetails();

                    for (int elementIndexValue = 0; elementIndexValue < list1.size(); elementIndexValue++) {
                        ReflectionParamDetail reflectionParamDetail = (ReflectionParamDetail) list1.get(elementIndexValue);
                        if (reflectionParamDetail.isClassNameParam() || reflectionParamDetail.isClassOrPropertiesNameParam()) {
                            StackFrameState stackFrameState = this.frameStates[callInstructionIndex];
                            int bc = stackFrameState.getStackDepth() + reflectionApiMethod.getArgumentOffset() + reflectionParamDetail.getPosition();
                            ValueTraceFrame valueTraceFrame1 = new ValueTraceFrame(
                                    this, valueTraceFrame, ReflectionArgumentKind.USER_DEFINED_METHOD_2, valueTraceFrame.nestingLevel
                            );
                            this.traceStackValueSource(valueTraceFrame1, basicBlock, callInstructionIndex, bc);
                            return;
                        }
                    }
                } else if (this.isGetComponentTypeCall(resolvedMethodRef, valueTraceFrame.memberLookup)) {
                    HashSet hashSet3 = ZkmUtils.createHashSet(13);
                    HashSet hashSet6 = valueTraceFrame.swapResults(hashSet3);
                    int bg = this.frameStates[callInstructionIndex].getStackDepth() - 1;
                    this.traceStackValueSource(valueTraceFrame, basicBlock, callInstructionIndex, bg);
                    valueTraceFrame.swapResults(hashSet6);
                    if (hashSet3.size() == 0) {
                        hashSet6.add(new InstructionReasonNote(constantRefInstruction, "Unknown object for '" + resolvedMethodRef.getValueString() + "' call"));
                    } else {
                        Iterator iterator1 = hashSet3.iterator();

                        while (iterator1.hasNext()) {
                            TrackedValue trackedValue1 = (TrackedValue) iterator1.next();
                            if (trackedValue1 instanceof MethodParameterValue) {
                                MethodParameterValue methodParameterValue = (MethodParameterValue) trackedValue1;
                                HashSet hashSet = ZkmUtils.createHashSet(13);
                                hashSet6 = valueTraceFrame.swapResults(hashSet);
                                this.traceParameterFromCallers(valueTraceFrame, methodParameterValue.getParameterIndex());
                                valueTraceFrame.swapResults(hashSet6);
                                Iterator iterator = hashSet.iterator();

                                while (iterator.hasNext()) {
                                    TrackedValue trackedValue = (TrackedValue) iterator.next();
                                    if (trackedValue instanceof StringConstantNameRef) {
                                        hashSet6.add(new ClassDescriptorStringValue(((StringConstantNameRef) trackedValue).getStringConstant()));
                                    } else if (trackedValue instanceof InstructionReasonNote) {
                                        hashSet6.add(trackedValue1);
                                    }
                                }
                            } else if (trackedValue1 instanceof StringConstantNameRef) {
                                hashSet6.add(new ClassDescriptorStringValue(((StringConstantNameRef) trackedValue1).getStringConstant()));
                            } else if (trackedValue1 instanceof InstructionReasonNote) {
                                hashSet6.add(trackedValue1);
                            }
                        }
                    }
                } else {
                    valueTraceFrame.results
                            .add(
                                    new InstructionReasonNote(
                                            constantRefInstruction,
                                            "Source is method: " + constantRefInstruction.getConstantPoolEntry().getValueString() + " : " + valueTraceFrame.argumentKind
                                    )
                            );
                }
                break;
            case 5:
                valueTraceFrame.results
                        .add(
                                new InstructionReasonNote(
                                        constantRefInstruction,
                                        "Source is method: " + constantRefInstruction.getConstantPoolEntry().getValueString() + " : " + valueTraceFrame.argumentKind
                                )
                        );
        }
    }

    public HashSet traceFieldStoreValues(ValueTraceFrame valueTraceFrame, FieldInfo fieldInfo, List list1) throws ZkmException, IOException {
        HashSet hashSet = ZkmUtils.createHashSet(ZkmUtils.getPrimeCapacity(list1.size() + 2));
        boolean isStaticMethodValue = this.methodBytecode.getMethod().isStatic();

        for (int elementIndex = 0; elementIndex < list1.size(); elementIndex++) {
            int integerValue = (Integer) list1.get(elementIndex);
            BasicBlock basicBlock = this.getBlockContaining(integerValue);
            if (this.frameStates[integerValue] != null) {
                int bc = this.frameStates[integerValue].getStackDepth() + (fieldInfo.isStatic() ? 0 : 1);
                ValueTraceFrame valueTraceFrame1 = new ValueTraceFrame(
                        this,
                        hashSet,
                        false,
                        valueTraceFrame.argumentKind,
                        valueTraceFrame.fieldLoadSites,
                        valueTraceFrame.fieldStoreSites,
                        valueTraceFrame.methodCallSites,
                        valueTraceFrame.superTypeResolver,
                        valueTraceFrame.memberLookup,
                        valueTraceFrame.pendingReflectionCalls,
                        isStaticMethodValue,
                        valueTraceFrame.returnedNameSourceCache,
                        valueTraceFrame.fieldValueCache,
                        valueTraceFrame.stackSourceCache,
                        valueTraceFrame.localSourceCache,
                        valueTraceFrame.getNestingLevel() + 1,
                        valueTraceFrame.flowAnalyzerCache
                );
                this.traceStackValueSource(valueTraceFrame1, basicBlock, integerValue, bc);
            }
        }

        return hashSet;
    }

    public void release() {
        if (!this.closed) {
            for (int elementIndex = 0; elementIndex < this.basicBlocks.size(); elementIndex++) {
                ((BasicBlock) this.basicBlocks.get(elementIndex)).clearLinks();
            }

            this.instructions = null;
            this.frameStates = null;
            this.exceptionTable = null;
            this.entryBlock = null;
            this.catchHandlerBlocks.clear();
            this.catchHandlerBlocks = null;
            this.basicBlocks.clear();
            this.basicBlocks = null;
            this.tryCatchInfosByHandler.clear();
            this.tryCatchInfosByHandler = null;
            this.initialLocals = null;
            if (this.retReturnTargets != null) {
                this.retReturnTargets.clear();
            }

            this.blockByIndexCache = null;
            this.naturalLoops.clear();
            this.naturalLoops = null;
            this.closed = true;
        }
    }

    public ObjectPair findLocalVariableScope(int localIndex, ObservableHolder observableHolder) throws ZkmException, IOException {
        int firstValue = -1;
        int second = -1;
        String localTypeDescriptorValue = null;
        int bd = 0;

        for (int elementIndex = 0; elementIndex < this.frameStates.length; elementIndex++) {
            StackFrameState stackFrameState = this.frameStates[elementIndex];
            if (stackFrameState != null && stackFrameState.isLocalOccupied(localIndex)) {
                if (firstValue == -1) {
                    firstValue = bd;
                }

                String localTypeDescriptor = stackFrameState.getLocalTypeDescriptor(localIndex);
                if (localTypeDescriptorValue == null && localTypeDescriptor != null) {
                    localTypeDescriptorValue = localTypeDescriptor;
                    observableHolder.setValue(localTypeDescriptor);
                } else if (localTypeDescriptor != null && !localTypeDescriptor.equals(localTypeDescriptorValue)) {
                    break;
                }

                second = bd;
            }

            bd += ((Instruction) this.instructions.get(elementIndex)).getLength();
        }

        return new ObjectPair(firstValue, second);
    }

    public ReturnedNameSource resolveReturnedNameSource(ValueTraceFrame valueTraceFrame) throws ZkmException, IOException {
        ReturnedNameSource returnedNameSource = null;
        boolean isStaticMethodValue = this.methodBytecode.getMethod().isStatic();
        int elementIndexValue = -1;
        int instructionsSize = this.instructions.size();

        for (int elementIndex = 0; elementIndex < instructionsSize; elementIndex++) {
            Instruction instruction1 = (Instruction) this.instructions.get(elementIndex);
            if (instruction1.getOpcode() == 176) {
                if (elementIndexValue == -1) {
                    elementIndexValue = elementIndex;
                } else {
                    returnedNameSource = new ReturnedNameSource("Multiple ARETURNs", this.methodBytecode);
                }
            }
        }

        if (returnedNameSource == null) {
            if (elementIndexValue > -1) {
                BasicBlock basicBlock = this.getBlockContaining(elementIndexValue);
                StackFrameState stackFrameState = this.frameStates[elementIndexValue];
                int stackDepth = stackFrameState.getStackDepth();
                HashSet hashSet = ZkmUtils.createHashSet(13);
                ValueTraceFrame valueTraceFrame1 = new ValueTraceFrame(
                        this,
                        hashSet,
                        false,
                        ReflectionArgumentKind.USER_DEFINED_METHOD_1,
                        valueTraceFrame.fieldLoadSites,
                        valueTraceFrame.fieldStoreSites,
                        valueTraceFrame.methodCallSites,
                        valueTraceFrame.superTypeResolver,
                        valueTraceFrame.memberLookup,
                        valueTraceFrame.pendingReflectionCalls,
                        isStaticMethodValue,
                        valueTraceFrame.returnedNameSourceCache,
                        valueTraceFrame.fieldValueCache,
                        valueTraceFrame.stackSourceCache,
                        valueTraceFrame.localSourceCache,
                        valueTraceFrame.getNestingLevel() + 1,
                        valueTraceFrame.flowAnalyzerCache
                );
                this.traceStackValueSource(valueTraceFrame1, basicBlock, elementIndexValue, stackDepth);
                if (hashSet.size() == 0) {
                    returnedNameSource = new ReturnedNameSource("No source found", this.methodBytecode);
                } else if (hashSet.size() == 1) {
                    Object object = hashSet.iterator().next();
                    if (object instanceof MethodParameterValue) {
                        MethodParameterValue methodParameterValue = (MethodParameterValue) object;
                        if (this.methodBytecode == methodParameterValue.getMethodBytecode()) {
                            returnedNameSource = new ReturnedNameSource(methodParameterValue.getParameterIndex(), this.methodBytecode);
                        } else {
                            returnedNameSource = new ReturnedNameSource(
                                    "NESTING TOO DEEP. "
                                            + this.methodBytecode.getQualifiedMethodName()
                                            + " calls "
                                            + methodParameterValue.getMethodBytecode().getQualifiedMethodName(),
                                    this.methodBytecode
                            );
                        }
                    } else if (object instanceof StringConstantNameRef) {
                        StringConstantNameRef stringConstantNameRef = (StringConstantNameRef) object;
                        returnedNameSource = new ReturnedNameSource(stringConstantNameRef.getStringConstant(), this.methodBytecode);
                    } else if (object instanceof TracedArrayValue) {
                        returnedNameSource = new ReturnedNameSource((TracedArrayValue) object, this.methodBytecode);
                    } else if (object instanceof InstructionReasonNote) {
                        InstructionReasonNote instructionReasonNote = (InstructionReasonNote) object;
                        returnedNameSource = new ReturnedNameSource(instructionReasonNote.getReason(), this.methodBytecode);
                    } else if (object instanceof PendingReflectionValue) {
                        PendingReflectionValue pendingReflectionValue = (PendingReflectionValue) object;
                        returnedNameSource = new ReturnedNameSource("Pending '" + pendingReflectionValue.getMethodDescription() + "'", this.methodBytecode);
                    } else {
                        returnedNameSource = new ReturnedNameSource("UNEXPECTED NAME SOURCE", this.methodBytecode);
                    }
                } else {
                    TracedArrayValue tracedArrayValue = new TracedArrayValue(hashSet.size());
                    int be = 0;
                    Iterator iterator = hashSet.iterator();

                    while (iterator.hasNext()) {
                        TrackedValue trackedValue = (TrackedValue) iterator.next();
                        tracedArrayValue.setElementValue(be++, trackedValue);
                    }

                    returnedNameSource = new ReturnedNameSource(tracedArrayValue, this.methodBytecode);
                }
            } else {
                returnedNameSource = new ReturnedNameSource("No ARETURN", this.methodBytecode);
            }
        } else {
            returnedNameSource = new ReturnedNameSource("Multiple ARETURNs", this.methodBytecode);
        }

        return returnedNameSource;
    }

    private void computeFrameStates(CommonSuperTypeResolver commonSuperTypeResolver1, TryCatchBlockInfo[][] tryCatchBlockInfos, boolean verifyTypes, String diagnosticContext) throws ZkmException, IOException {
        int instructionsSize = this.instructions.size();
        this.frameStates = new StackFrameState[instructionsSize];
        StackFrameState stackFrameState = new StackFrameState(this.initialLocals);
        Instruction instruction1 = (Instruction) this.instructions.get(0);
        CommonSuperTypeResolver commonSuperTypeResolver2 = commonSuperTypeResolver1;
        this.frameStates[0] = instruction1.computeFrameAfter(stackFrameState, verifyTypes, commonSuperTypeResolver2, diagnosticContext);
        TreeSet treeSet1 = new TreeSet();
        TreeSet treeSet2 = new TreeSet();
        UniqueWorkQueue uniqueWorkQueue = new UniqueWorkQueue();
        treeSet1.add(INTEGER_CACHE.valueOf(0));
        TwoKeyMap twoKeyMap = new TwoKeyMap();
        HashSet hashSet = null;
        HashSet hashSet1 = null;

        do {
            Integer integer;
            int integerValue;
            if (!treeSet1.isEmpty()) {
                integer = (Integer) treeSet1.first();
                integerValue = integer;
                treeSet1.remove(integer);
            } else if (!treeSet2.isEmpty()) {
                integer = (Integer) treeSet2.first();
                integerValue = integer;
                treeSet2.remove(integer);
            } else {
                integer = (Integer) uniqueWorkQueue.dequeue();
                integerValue = integer;
            }

            Instruction instruction2 = (Instruction) this.instructions.get(integerValue);
            if (instruction2 instanceof SwitchInstruction) {
                List list1 = ((SwitchInstruction) instruction2).getTargetLabels();
                Iterator iterator = list1.iterator();

                while (iterator.hasNext()) {
                    LabelInstruction labelInstruction1 = (LabelInstruction) iterator.next();
                    int instructionIndex = labelInstruction1.getInstructionIndex();
                    String diagnosticContextValue = diagnosticContext;
                    boolean verifyTypesValue = verifyTypes;
                    TreeSet treeSet = treeSet1;
                    int instructionIndexValue = instructionIndex;
                    int predecessorIndex = integerValue;
                    this.propagateFrameToSuccessor(predecessorIndex, instructionIndexValue, treeSet, verifyTypesValue, diagnosticContextValue);
                }
            } else if (instruction2 instanceof BranchInstruction && !instruction2.isJsr()) {
                LabelInstruction labelInstruction = ((BranchInstruction) instruction2).getTargetLabel();
                int instructionIndexValue2 = labelInstruction.getInstructionIndex();
                this.propagateFrameToSuccessor(integerValue, instructionIndexValue2, treeSet1, verifyTypes, diagnosticContext);
            }

            if (instruction2.canFallThrough()) {
                if (instruction2.isRet()) {
                    twoKeyMap.removeInnerMap(instruction2);
                }

                int bo = integerValue + 1;
                String diagnosticContextValue2 = diagnosticContext;
                boolean verifyTypesValue2 = verifyTypes;
                TreeSet treeSet3 = treeSet1;
                int successorIndex = bo;
                int predecessorIndexValue = integerValue;
                this.propagateFrameToSuccessor(predecessorIndexValue, successorIndex, treeSet3, verifyTypesValue2, diagnosticContextValue2);
            }

            TryCatchBlockInfo[] tryCatchBlockInfos1 = tryCatchBlockInfos[integerValue];
            if (tryCatchBlockInfos1 != null) {
                for (int tryCatchBlockInfos1ElementIndex = 0; tryCatchBlockInfos1ElementIndex < tryCatchBlockInfos1.length; tryCatchBlockInfos1ElementIndex++) {
                    int handlerIndexValue = tryCatchBlockInfos1[tryCatchBlockInfos1ElementIndex].handlerIndex;
                    SubroutineLocalsBitSet subroutineLocalsBitSet1 = this.frameStates[integerValue].getSubroutineLocals();
                    SubroutineLocalsBitSet subroutineLocalsBitSet = null;
                    if (this.frameStates[handlerIndexValue] != null) {
                        subroutineLocalsBitSet = this.frameStates[handlerIndexValue].getSubroutineLocals();
                    }

                    StackFrameState stackFrameState1;
                    if ((subroutineLocalsBitSet1 != null || subroutineLocalsBitSet != null)
                            && (subroutineLocalsBitSet1 == null || subroutineLocalsBitSet == null || !subroutineLocalsBitSet1.isSameSubroutine(subroutineLocalsBitSet))) {
                        if (subroutineLocalsBitSet1 != null && subroutineLocalsBitSet != null && !subroutineLocalsBitSet1.isSameSubroutine(subroutineLocalsBitSet)) {
                            stackFrameState1 = StackFrameState.createWithSubroutineLocals(
                                    this.frameStates[integerValue], this.frameStates[handlerIndexValue].getLocals(), subroutineLocalsBitSet, this.frameStates[handlerIndexValue].getHeldMonitors()
                            );
                        } else {
                            if (subroutineLocalsBitSet1 == null || subroutineLocalsBitSet != null) {
                                throw new MethodAnalysisException(
                                        "Method '" + this.methodDescription + "' could not be analyzed (A). No action required.", this.methodDescription
                                );
                            }

                            if (this.frameStates[handlerIndexValue] != null) {
                                stackFrameState1 = StackFrameState.createWithSubroutineLocals(
                                        this.frameStates[integerValue], this.frameStates[handlerIndexValue].getLocals(), subroutineLocalsBitSet, this.frameStates[handlerIndexValue].getHeldMonitors()
                                );
                            } else {
                                stackFrameState1 = this.frameStates[integerValue];
                            }
                        }
                    } else {
                        stackFrameState1 = this.frameStates[integerValue];
                    }

                    StackFrameState stackFrameState2 = new StackFrameState(
                            tryCatchBlockInfos1[tryCatchBlockInfos1ElementIndex].catchTypeDescriptor,
                            stackFrameState1.getLocals(),
                            stackFrameState1.getSubroutineLocals(),
                            this.frameStates[integerValue].getHeldMonitors()
                    );
                    Instruction instruction3 = (Instruction) this.instructions.get(handlerIndexValue);
                    CommonSuperTypeResolver commonSuperTypeResolver3 = commonSuperTypeResolver1;
                    StackFrameState stackFrameState3 = instruction3.computeFrameAfter(stackFrameState2, verifyTypes, commonSuperTypeResolver3, diagnosticContext);
                    if (this.frameStates[handlerIndexValue] == null) {
                        this.frameStates[handlerIndexValue] = stackFrameState3;
                        treeSet1.add(INTEGER_CACHE.valueOf(handlerIndexValue));
                    } else {
                        BooleanFlag booleanFlag = new BooleanFlag();
                        StackFrameState stackFrameState4 = this.frameStates[handlerIndexValue].merge(commonSuperTypeResolver1, stackFrameState3, booleanFlag, diagnosticContext);
                        if (booleanFlag.getValue()) {
                            this.frameStates[handlerIndexValue] = stackFrameState4;
                            treeSet1.add(INTEGER_CACHE.valueOf(handlerIndexValue));
                        }
                    }
                }
            }

            if (instruction2.isJsr()) {
                BranchInstruction branchInstruction = (BranchInstruction) instruction2;
                LabelInstruction labelInstruction2 = branchInstruction.getTargetLabel();
                int instructionIndexValue3 = labelInstruction2.getInstructionIndex();
                StackFrameState stackFrameState6 = new StackFrameState(
                        this.frameStates[integerValue].getStack(),
                        this.frameStates[integerValue].getLocals(),
                        new SubroutineLocalsBitSet(labelInstruction2, this.maxLocals),
                        this.frameStates[integerValue].getHeldMonitors()
                );
                CommonSuperTypeResolver commonSuperTypeResolver4 = commonSuperTypeResolver1;
                StackFrameState stackFrameState7 = labelInstruction2.computeFrameAfter(stackFrameState6, verifyTypes, commonSuperTypeResolver4, diagnosticContext);
                if (this.frameStates[instructionIndexValue3] == null) {
                    this.frameStates[instructionIndexValue3] = stackFrameState7;
                    treeSet2.add(INTEGER_CACHE.valueOf(instructionIndexValue3));
                } else {
                    StackFrameState stackFrameState8 = this.frameStates[instructionIndexValue3];
                    BooleanFlag booleanFlag1 = new BooleanFlag();
                    StackFrameState stackFrameState10 = stackFrameState8.merge(commonSuperTypeResolver1, stackFrameState6, booleanFlag1, diagnosticContext);
                    if (booleanFlag1.getValue()) {
                        this.frameStates[instructionIndexValue3] = stackFrameState10;
                        treeSet2.add(INTEGER_CACHE.valueOf(instructionIndexValue3));
                    }
                }
            } else if (instruction2.isRet()) {
                LocalVariableInstruction localVariableInstruction = (LocalVariableInstruction) instruction2;
                boolean bl4 = this.retReturnTargets != null;
                String[] strings = new String[]{this.methodDescription + " " + integerValue};
                ZkmAssert.assertTrue(bl4, strings);
                Iterator iterator1 = this.retReturnTargets.getInnerMap(localVariableInstruction).keySet().iterator();
                boolean bl3 = false;

                while (iterator1.hasNext()) {
                    Integer integer1 = (Integer) iterator1.next();
                    int integer1Value = integer1;
                    int frameStateIndex = integer1Value - 1;
                    if (this.frameStates[frameStateIndex] == null) {
                        BasicBlock basicBlock = this.getBlockContaining(frameStateIndex);
                        if (hashSet1 == null) {
                            hashSet1 = ZkmUtils.createHashSetFrom(this.basicBlocks);
                        }

                        if (hashSet1.contains(basicBlock)) {
                            uniqueWorkQueue.enqueue(integer);
                            bl3 = true;
                        }
                    } else if (!twoKeyMap.containsKeys(localVariableInstruction, integer1)) {
                        StackFrameState stackFrameState9 = StackFrameState.createWithSubroutineLocals(
                                this.frameStates[integerValue], this.frameStates[frameStateIndex].getLocals(), this.frameStates[frameStateIndex].getSubroutineLocals(), this.frameStates[frameStateIndex].getHeldMonitors()
                        );
                        Instruction instruction4 = (Instruction) this.instructions.get(integer1Value);
                        CommonSuperTypeResolver commonSuperTypeResolver5 = commonSuperTypeResolver1;
                        StackFrameState stackFrameState11 = instruction4.computeFrameAfter(stackFrameState9, verifyTypes, commonSuperTypeResolver5, diagnosticContext);
                        BooleanFlag booleanFlag2 = new BooleanFlag();
                        if (this.frameStates[integer1Value] == null) {
                            this.frameStates[integer1Value] = stackFrameState11;
                            treeSet1.add(integer1);
                        } else {
                            StackFrameState stackFrameState5 = this.frameStates[integer1Value].merge(commonSuperTypeResolver1, stackFrameState11, booleanFlag2, diagnosticContext);
                            if (booleanFlag2.getValue()) {
                                this.frameStates[integer1Value] = stackFrameState5;
                                treeSet1.add(integer1);
                            }
                        }

                        twoKeyMap.putValue(localVariableInstruction, integer1, integer1);
                    }
                }

                if (bl3) {
                    if (hashSet == null) {
                        hashSet = ZkmUtils.createHashSet();
                    }

                    if (treeSet1.size() == 0) {
                        Integer integer2 = uniqueWorkQueue.hashCode();
                        if (hashSet.contains(integer2)) {
                            throw new MethodAnalysisException(
                                    "Method '" + this.methodDescription + "' could not be analyzed (B). Please report this message to bugs@zelix.com.",
                                    this.methodDescription
                            );
                        }

                        hashSet.add(integer2);
                    } else {
                        hashSet.clear();
                    }
                }
            }
        } while (!treeSet1.isEmpty() || !treeSet2.isEmpty() || !uniqueWorkQueue.isEmpty());
    }

    public boolean hasNaturalLoops() {
        return this.naturalLoops != null && this.naturalLoops.size() > 0;
    }

    public void assignBlockIndices() {
        for (int indexValue = 0; indexValue < this.basicBlocks.size(); indexValue++) {
            ((BasicBlock) this.basicBlocks.get(indexValue)).setIndex(indexValue);
        }
    }

    public boolean isPropertiesMethodCall(ResolvedMethodRef resolvedMethodRef, Object object, ClassMemberLookup classMemberLookup1) throws ZkmException, IOException {
        try {
            if (resolvedMethodRef.getSignatureString().equals(object)) {
                String referencedClassName = resolvedMethodRef.getReferencedClassName();
                return referencedClassName.equals("java/util/Properties") || classMemberLookup1.isSubclass(referencedClassName, "java/util/Properties");
            } else {
                return false;
            }
        } catch (ClassFileLoadException classFileLoadException) {
            return false;
        }
    }

    public int[] findDeadCodeIndices() {
        ArrayList arrayList = new ArrayList();
        int instructionsSize = this.instructions.size();
        boolean bl = false;

        for (int frameStateIndex = 0; frameStateIndex < instructionsSize; frameStateIndex++) {
            Instruction instruction1 = (Instruction) this.instructions.get(frameStateIndex);
            if (this.frameStates[frameStateIndex] != null) {
                bl = false;
            } else {
                boolean bl1 = false;
                if (instruction1.isLabel()) {
                    LabelInstruction labelInstruction = (LabelInstruction) instruction1;
                    if (frameStateIndex == instructionsSize - 1) {
                        bl1 = true;
                    } else if (labelInstruction.hasUsageBits(512)) {
                        bl1 = true;
                    } else {
                        for (int elementIndexValue = frameStateIndex + 1; elementIndexValue < instructionsSize; elementIndexValue++) {
                            if (!((Instruction) this.instructions.get(elementIndexValue)).isLabel()) {
                                if (this.frameStates[elementIndexValue] != null) {
                                    bl1 = true;
                                }
                                break;
                            }
                        }
                    }
                }

                if (bl || !bl1) {
                    arrayList.add(INTEGER_CACHE.valueOf(frameStateIndex));
                    bl = true;
                }
            }
        }

        int[] integerArray = new int[arrayList.size()];

        for (int elementIndex = 0; elementIndex < integerArray.length; elementIndex++) {
            integerArray[elementIndex] = (Integer) arrayList.get(elementIndex);
        }

        return integerArray;
    }

    public boolean hasHeldMonitorsAt(int frameStateIndex) {
        if (this.frameStates != null && this.frameStates[frameStateIndex] != null) {
            Set set1 = this.frameStates[frameStateIndex].getHeldMonitors();
            return set1 != null && set1.size() > 0;
        } else {
            return false;
        }
    }

    private boolean propagateLivenessBackward(BasicBlock basicBlock, int startIndexArgument, BitSet bitSet) {
        boolean bl = false;
        PendingBlockVisit pendingBlockVisit = new PendingBlockVisit(basicBlock, startIndexArgument, bitSet);
        UniqueWorkQueue uniqueWorkQueue = null;
        boolean bl1 = true;

        while (bl1 || uniqueWorkQueue != null && !uniqueWorkQueue.isEmpty()) {
            if (!bl1) {
                pendingBlockVisit = (PendingBlockVisit) uniqueWorkQueue.dequeue();
            }

            bl1 = false;
            BasicBlock basicBlock1 = pendingBlockVisit.block;
            int startIndexValue = pendingBlockVisit.startIndex;
            BitSet bitSet1 = pendingBlockVisit.liveLocals;

            boolean livenessUnchanged = false;
            for (int startIndexValue2 = startIndexValue; startIndexValue2 >= basicBlock1.getStartIndex(); startIndexValue2 += -1) {
                StackFrameState stackFrameState = this.frameStates[startIndexValue2];
                if (stackFrameState != null) {
                    Instruction instruction1 = (Instruction) this.instructions.get(startIndexValue2);

                    for (int localIndex = 0; localIndex < this.maxLocals; localIndex++) {
                        if (stackFrameState.isLocalDefined(localIndex)) {
                            if (instruction1.accessesLocal(localIndex)) {
                                bitSet1.set(localIndex);
                            }
                        } else {
                            bitSet1.clear(localIndex);
                        }
                    }

                    if (stackFrameState.hasLivenessInfo()) {
                        stackFrameState.getLiveLocals();
                    }

                    if (!stackFrameState.addLiveLocals(bitSet1)) {
                        livenessUnchanged = true;
                        break;
                    }

                    bl = true;
                }
            }

            if (livenessUnchanged) {
                continue;
            }

            if (basicBlock1.hasPredecessors()) {
                if (uniqueWorkQueue == null) {
                    uniqueWorkQueue = new UniqueWorkQueue(Math.max(5, this.basicBlocks.size()));
                }

                Iterator iterator = basicBlock1.getUnmodifiablePredecessors().iterator();

                while (iterator.hasNext()) {
                    BasicBlock basicBlock2 = (BasicBlock) iterator.next();
                    uniqueWorkQueue.enqueue(new PendingBlockVisit(basicBlock2, basicBlock2.getEndIndex(), (BitSet) bitSet1.clone()));
                }
            }
        }

        return bl;
    }

    public void collectIntConstantSourcesFrom(List list1, BasicBlock basicBlock, int ba, int bb, Set set1) {
        int bc = bb;
        if (!set1.contains(basicBlock)) {
            set1.add(basicBlock);
            boolean bl = false;
            int startIndex = basicBlock.getStartIndex();

            for (int elementIndexValue = ba - 1; !bl && elementIndexValue >= startIndex; elementIndexValue += -1) {
                Instruction instruction1 = (Instruction) this.instructions.get(elementIndexValue);
                StackFrameState stackFrameState = this.frameStates[elementIndexValue];
                if (instruction1.isStackManipulation()) {
                    bc = instruction1.mapStackSlotBackward(stackFrameState.getStack(), bc);
                } else if (stackFrameState.getStackDepth() - 1 == bc && instruction1.pushesValue()) {
                    switch (instruction1.getOpcode()) {
                        case 3:
                            list1.add(INTEGER_CACHE.valueOf(0));
                            break;
                        case 4:
                            list1.add(INTEGER_CACHE.valueOf(1));
                            break;
                        case 5:
                            list1.add(INTEGER_CACHE.valueOf(2));
                            break;
                        case 6:
                            list1.add(INTEGER_CACHE.valueOf(3));
                            break;
                        case 7:
                            list1.add(INTEGER_CACHE.valueOf(4));
                            break;
                        case 8:
                            list1.add(INTEGER_CACHE.valueOf(5));
                            break;
                        case 9:
                        case 10:
                        case 11:
                        case 12:
                        case 13:
                        case 14:
                        case 15:
                        default:
                            list1.add("Instruction not handled: " + instruction1.getMnemonic());
                            break;
                        case 16:
                            BipushInstruction bipushInstruction = (BipushInstruction) instruction1;
                            list1.add(INTEGER_CACHE.valueOf(bipushInstruction.getByteValue()));
                    }

                    bl = true;
                }
            }

            if (!bl) {
                List list2 = basicBlock.getPredecessors();
                if (list2 != null && list2.size() > 0) {
                    for (int elementIndex = 0; elementIndex < list2.size(); elementIndex++) {
                        BasicBlock basicBlock1 = (BasicBlock) list2.get(elementIndex);
                        this.collectIntConstantSourcesFrom(list1, basicBlock1, basicBlock1.getEndIndex() + 1, bc, set1);
                    }
                } else {
                    list1.add("No value found");
                }
            }
        }
    }
}

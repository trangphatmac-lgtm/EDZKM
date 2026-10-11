package com.zelix.klassmaster.obfuscator.exceptions;

import com.zelix.klassmaster.classfile.ClassMemberRef;
import com.zelix.klassmaster.classfile.MethodInfo;
import com.zelix.klassmaster.classfile.ProgramClass;
import com.zelix.klassmaster.classfile.attribute.CodeAttributeBody;
import com.zelix.klassmaster.classfile.attribute.ExceptionTableEntry;
import com.zelix.klassmaster.classfile.constpool.ResolvedMethodRef;
import com.zelix.klassmaster.classfile.hierarchy.ClassMemberLookup;
import com.zelix.klassmaster.classfile.hierarchy.CommonSuperTypeResolver;
import com.zelix.klassmaster.classfile.hierarchy.InheritedMemberAnalyzer;
import com.zelix.klassmaster.classfile.insn.CodeInsertion;
import com.zelix.klassmaster.classfile.insn.ConstantRefInstruction;
import com.zelix.klassmaster.classfile.insn.ExceptionHandlerSpec;
import com.zelix.klassmaster.classfile.insn.Instruction;
import com.zelix.klassmaster.classfile.insn.LocalVariableList;
import com.zelix.klassmaster.classfile.insn.MethodBytecode;
import com.zelix.klassmaster.classfile.insn.SimpleInstruction;
import com.zelix.klassmaster.config.HiddenOptionFlags;
import com.zelix.klassmaster.config.IgnoreMissingReferencesSpec;
import com.zelix.klassmaster.engine.ProcessingStatistics;
import com.zelix.klassmaster.exceptions.ZkmException;
import com.zelix.klassmaster.script.ScriptEnvironment;
import com.zelix.klassmaster.util.NestedMultiMap;
import com.zelix.klassmaster.util.ObjectPair;
import com.zelix.klassmaster.util.PairValueMap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

public class ExceptionObfuscator {
    public static void obfuscateExceptionHandlers(
            ProgramClass programClass1,
            Set set1,
            NestedMultiMap nestedMultiMap,
            Map map1,
            PairValueMap pairValueMap,
            List list1,
            CommonSuperTypeResolver commonSuperTypeResolver1,
            ProcessingStatistics processingStatistics1,
            InheritedMemberAnalyzer inheritedMemberAnalyzer,
            ClassMemberLookup classMemberLookup1,
            ScriptEnvironment scriptEnvironment1
    ) throws ZkmException, IOException {
        IgnoreMissingReferencesSpec ignoreMissingReferencesSpec1 = scriptEnvironment1 == null ? null : scriptEnvironment1.getIgnoreMissingReferencesSpec();
        ArrayList arrayList = new ArrayList();
        ConstantRefInstruction constantRefInstruction = null;
        ClassMemberRef classMemberRef = null;
        Set set2;
        if (processingStatistics1.meetsFullSizeThreshold()) {
            if (!HiddenOptionFlags.SKIP_EXCEPTION_HANDLER_OBFUSCATION) {
                String commonSuperType = null;
                Iterator iterator = pairValueMap.entrySet().iterator();

                while (iterator.hasNext()) {
                    Entry entry = (Entry) iterator.next();
                    List list2 = (List) ((ObjectPair) entry.getValue()).getSecond();
                    Iterator iterator1 = list2.iterator();

                    while (iterator1.hasNext()) {
                        ExceptionTableEntry exceptionTableEntry1 = (ExceptionTableEntry) iterator1.next();
                        String catchTypeName = exceptionTableEntry1.getCatchTypeName();
                        if (commonSuperType == null) {
                            commonSuperType = 'L' + catchTypeName + ';';
                        } else if (ignoreMissingReferencesSpec1 == null
                                || !ignoreMissingReferencesSpec1.isClassIgnored(catchTypeName)
                                && !ignoreMissingReferencesSpec1.isClassIgnored(commonSuperType.substring(1, commonSuperType.length() - 1))) {
                            commonSuperType = commonSuperTypeResolver1.getCommonSuperType(commonSuperType, 'L' + catchTypeName + ';', "Exception Obfuscation");
                        } else {
                            commonSuperType = "Ljava/lang/Throwable;";
                        }
                    }
                }

                String string2 = '(' + commonSuperType + ')' + commonSuperType;
                LocalVariableList localVariableList1 = new LocalVariableList(true, string2, 5);
                ArrayList arrayList1 = new ArrayList();
                arrayList1.add(Instruction.createObjectLoad(0, localVariableList1, 8));
                arrayList1.add(SimpleInstruction.forOpcode(176));
                ExceptionHandlerSpec[] exceptionHandlerSpecs = new ExceptionHandlerSpec[0];
                MethodInfo methodInfo1 = programClass1.createUniquelyNamedStaticMethod(
                        string2,
                        arrayList1,
                        1,
                        1,
                        1,
                        localVariableList1,
                        exceptionHandlerSpecs,
                        "Exception Obfuscation",
                        list1,
                        inheritedMemberAnalyzer,
                        classMemberLookup1,
                        8
                );
                set1.add(methodInfo1);
                ResolvedMethodRef resolvedMethodRef = programClass1.addMethodRef(methodInfo1, list1);
                constantRefInstruction = new ConstantRefInstruction(184, resolvedMethodRef);
                arrayList.add(constantRefInstruction);
                classMemberRef = new ClassMemberRef(methodInfo1, programClass1);
                set2 = pairValueMap.entrySet();
            } else {
                set2 = pairValueMap.entrySet();
            }
        } else {
            set2 = pairValueMap.entrySet();
        }

        Iterator iterator2 = set2.iterator();

        while (iterator2.hasNext()) {
            Entry entry1 = (Entry) iterator2.next();
            MethodBytecode methodBytecode1 = (MethodBytecode) entry1.getKey();
            List list3 = (List) ((ObjectPair) entry1.getValue()).getFirst();
            if (arrayList.size() > 0) {
                Iterator iterator3 = list3.iterator();

                while (iterator3.hasNext()) {
                    CodeInsertion codeInsertion = (CodeInsertion) iterator3.next();
                    codeInsertion.insertBeforeOpcode(arrayList);
                }
            }

            List list4 = (List) ((ObjectPair) entry1.getValue()).getSecond();
            methodBytecode1.applyCodeInsertions(list3, "Exception Obfuscation");
            ((CodeAttributeBody) methodBytecode1.getParent()).insertExceptionTableEntries(true, list4, scriptEnvironment1);
            methodBytecode1.setModified(true);
            if (nestedMultiMap != null
                    && classMemberRef != null
                    && constantRefInstruction != null
                    && map1 != null
                    && map1.containsKey(methodBytecode1.getMethod())) {
                nestedMultiMap.addValue(classMemberRef, methodBytecode1, constantRefInstruction);
            }
        }
    }

    private ExceptionObfuscator() {
    }
}

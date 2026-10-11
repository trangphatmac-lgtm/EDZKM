package com.zelix.klassmaster.classfile.insn;

import com.zelix.klassmaster.exceptions.ZkmException;
import com.zelix.klassmaster.obfuscator.reflection.ReflectionArgumentKind;
import com.zelix.klassmaster.obfuscator.reflection.ValueFlowTracker;
import com.zelix.klassmaster.util.NestedMultiMap;
import com.zelix.klassmaster.util.ZkmUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class ArrayElementTracker implements ValueFlowTracker {
    public final MethodFlowAnalyzer flowAnalyzer;
    public Map elementValuesByIndex;
    public List warnings;
    public NestedMultiMap visitedStates;

    public HashSet getElementValues(int integerArgument) {
        return (HashSet) this.elementValuesByIndex.get(MethodFlowAnalyzer.INTEGER_CACHE.valueOf(integerArgument));
    }

    @Override
    public ValueFlowTracker getTrackerFrom(MethodFlowAnalyzer methodFlowAnalyzer) {
        return methodFlowAnalyzer.createArrayElementTracker();
    }

    public ArrayElementTracker(MethodFlowAnalyzer methodFlowAnalyzer) {
        this.flowAnalyzer = methodFlowAnalyzer;
        this.elementValuesByIndex = ZkmUtils.createHashMap();
        this.warnings = new ArrayList();
        this.visitedStates = new NestedMultiMap(5, 97, 7);
    }

    public List getWarnings() {
        return this.warnings;
    }

    @Override
    public void markVisited(int integerArgument, int integerArgument2, int integerArgument3) {
        this.visitedStates
                .addValue(MethodFlowAnalyzer.INTEGER_CACHE.valueOf(integerArgument), MethodFlowAnalyzer.INTEGER_CACHE.valueOf(integerArgument2), MethodFlowAnalyzer.INTEGER_CACHE.valueOf(integerArgument3));
    }

    @Override
    public boolean traceAtInstruction(ValueTraceFrame valueTraceFrame, int elementIndexArgument, BasicBlock basicBlock) throws ZkmException, IOException {
        if (((Instruction) MethodFlowAnalyzer.getInstructions(this.flowAnalyzer).get(elementIndexArgument)).getOpcode() != 83) {
            return false;
        }

        StackFrameState stackFrameState = MethodFlowAnalyzer.getFrameStates(this.flowAnalyzer)[elementIndexArgument];
        ArrayList arrayList = new ArrayList();
        MethodFlowAnalyzer.collectIntConstantSources(
                this.flowAnalyzer, arrayList, basicBlock, elementIndexArgument, stackFrameState.getStack().length + 1, ZkmUtils.createHashSet()
        );
        int bb = stackFrameState.getStack().length + 2;
        HashSet hashSet = ZkmUtils.createHashSet(13);
        ValueTraceFrame valueTraceFrame1 = new ValueTraceFrame(this.flowAnalyzer, valueTraceFrame, ReflectionArgumentKind.CLASS_REFERENCE, 0);
        valueTraceFrame1.swapResults(hashSet);
        if (elementIndexArgument > basicBlock.getStartIndex()) {
            this.flowAnalyzer.traceStackValueSource(valueTraceFrame1, basicBlock, elementIndexArgument, bb);
        } else {
            List list1 = basicBlock.getPredecessors();

            for (int elementIndex = 0; elementIndex < list1.size(); elementIndex++) {
                BasicBlock basicBlock1 = (BasicBlock) list1.get(elementIndex);
                this.flowAnalyzer.traceStackValueSource(valueTraceFrame1, basicBlock1, basicBlock1.getEndIndex() + 1, bb);
            }
        }

        Integer integer = null;

        for (int elementIndexValue = 0; elementIndexValue < arrayList.size(); elementIndexValue++) {
            Object object = arrayList.get(elementIndexValue);
            if (!(object instanceof Integer)) {
                this.warnings.add("Could not determine element position at AALOAD : " + object);
                break;
            }

            if (integer == null) {
                integer = (Integer) object;
            } else if (!object.equals(integer)) {
                this.warnings.add("Multiple element positions possible at AALOAD : " + integer + ", " + object);
                break;
            }
        }

        if (integer != null) {
            this.elementValuesByIndex.put(integer, hashSet);
        } else {
            this.warnings.add("Could not determine element position at AALOAD");
        }

        return true;
    }

    @Override
    public boolean isVisited(int integerArgument, int integerArgument2, int integerArgument3) {
        return this.visitedStates
                .containsValue(
                        MethodFlowAnalyzer.INTEGER_CACHE.valueOf(integerArgument), MethodFlowAnalyzer.INTEGER_CACHE.valueOf(integerArgument2), MethodFlowAnalyzer.INTEGER_CACHE.valueOf(integerArgument3)
                );
    }
}

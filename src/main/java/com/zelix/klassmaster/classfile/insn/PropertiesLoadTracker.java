package com.zelix.klassmaster.classfile.insn;

import com.zelix.klassmaster.classfile.constpool.ResolvedMethodRef;
import com.zelix.klassmaster.exceptions.ClassFileLoadException;
import com.zelix.klassmaster.exceptions.ZkmException;
import com.zelix.klassmaster.obfuscator.reflection.ReflectionArgumentKind;
import com.zelix.klassmaster.obfuscator.reflection.ValueFlowTracker;
import com.zelix.klassmaster.util.NestedMultiMap;
import com.zelix.klassmaster.util.ZkmUtils;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;

public class PropertiesLoadTracker implements ValueFlowTracker {
    public final MethodFlowAnalyzer analyzer;
    public NestedMultiMap visitedStates;
    public HashSet fileNameValues;

    public HashSet getFileNameValues() {
        return this.fileNameValues;
    }

    @Override
    public void markVisited(int integer, int integerArgument, int integerArgument2) {
        this.visitedStates
                .addValue(MethodFlowAnalyzer.INTEGER_CACHE.valueOf(integer), MethodFlowAnalyzer.INTEGER_CACHE.valueOf(integerArgument), MethodFlowAnalyzer.INTEGER_CACHE.valueOf(integerArgument2));
    }

    @Override
    public ValueFlowTracker getTrackerFrom(MethodFlowAnalyzer methodFlowAnalyzer) {
        return methodFlowAnalyzer.createPropertiesLoadTracker();
    }

    @Override
    public boolean isVisited(int integer, int integerArgument, int integerArgument2) {
        return this.visitedStates
                .containsValue(
                        MethodFlowAnalyzer.INTEGER_CACHE.valueOf(integer), MethodFlowAnalyzer.INTEGER_CACHE.valueOf(integerArgument), MethodFlowAnalyzer.INTEGER_CACHE.valueOf(integerArgument2)
                );
    }

    @Override
    public boolean traceAtInstruction(ValueTraceFrame valueTraceFrame, int instructionIndex, BasicBlock basicBlock) throws ZkmException, IOException {
        Instruction loadInstruction = (Instruction) MethodFlowAnalyzer.getInstructions(this.analyzer).get(instructionIndex);
        if (loadInstruction.getOpcode() != 182) {
            return false;
        }

        ResolvedMethodRef propertiesLoadMethod = (ResolvedMethodRef) ((ConstantRefInstruction) loadInstruction).getConstantPoolEntry();
        if (propertiesLoadMethod.getReferencedClassName().equals("java/util/Properties")
                && propertiesLoadMethod.getSignatureString().equals("load(Ljava/io/InputStream;)V")) {
            boolean traceComplete = false;
            int startIndex = basicBlock.getStartIndex();
            int sourceIndex = instructionIndex;

            while (!traceComplete) {
                if (sourceIndex > startIndex) {
                    sourceIndex += -1;
                    Instruction sourceInstruction = (Instruction) MethodFlowAnalyzer.getInstructions(this.analyzer).get(sourceIndex);
                    if (sourceInstruction.getOpcode() == 183) {
                        ResolvedMethodRef constructor = (ResolvedMethodRef) ((ConstantRefInstruction) sourceInstruction).getConstantPoolEntry();
                        String constructorClassName = constructor.getReferencedClassName();
                        String constructorName = constructor.getMemberName();

                        try {
                            if (constructorName.equals("<init>")
                                    && (
                                    valueTraceFrame.memberLookup.isSubclass(constructorClassName, "java/io/InputStream")
                                            || valueTraceFrame.memberLookup.isSubclass(constructorClassName, "java/io/Reader")
                            )) {
                                List constructorParameterTypes = constructor.getParameterTypes();
                                if (constructorParameterTypes.size() == 1) {
                                    String parameterClassName = (String) constructorParameterTypes.get(0);
                                    if (parameterClassName.length() > 2) {
                                        parameterClassName = parameterClassName.substring(1, parameterClassName.length() - 1);
                                    }

                                    if (!parameterClassName.equals("java/io/InputStream") && !parameterClassName.equals("java/io/Reader")) {
                                        if (parameterClassName.equals("java/lang/String")
                                                && (constructorClassName.equals("java/io/FileInputStream") || constructorClassName.equals("java/io/FileReader"))) {
                                            int argumentStackDepth = MethodFlowAnalyzer.getFrameStates(this.analyzer)[sourceIndex].getStack().length + 1;
                                            ValueTraceFrame fileNameTrace = new ValueTraceFrame(this.analyzer, valueTraceFrame, ReflectionArgumentKind.NORMAL, 0, true);
                                            fileNameTrace.swapResults(this.fileNameValues);
                                            if (sourceIndex > startIndex) {
                                                this.analyzer.traceStackValueSource(fileNameTrace, basicBlock, sourceIndex, argumentStackDepth);
                                            } else {
                                                List predecessorBlocks = basicBlock.getPredecessors();

                                                for (int elementIndex = 0; elementIndex < predecessorBlocks.size(); elementIndex++) {
                                                    BasicBlock predecessor = (BasicBlock) predecessorBlocks.get(elementIndex);
                                                    this.analyzer.traceStackValueSource(fileNameTrace, predecessor, predecessor.getEndIndex() + 1, argumentStackDepth);
                                                }
                                            }

                                            traceComplete = true;
                                            continue;
                                        }

                                        traceComplete = true;
                                    }
                                }
                            } else {
                                traceComplete = true;
                            }
                        } catch (ClassFileLoadException classFileLoadException) {
                            traceComplete = true;
                        }
                    } else {
                        traceComplete = true;
                    }
                } else {
                    traceComplete = true;
                }
            }

            return true;
        } else {
            return false;
        }
    }

    public PropertiesLoadTracker(MethodFlowAnalyzer methodFlowAnalyzer) {
        this.analyzer = methodFlowAnalyzer;
        this.visitedStates = new NestedMultiMap(5, 97, 7);
        this.fileNameValues = ZkmUtils.createHashSet(13);
    }
}

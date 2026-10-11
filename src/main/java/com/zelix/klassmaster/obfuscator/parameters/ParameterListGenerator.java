package com.zelix.klassmaster.obfuscator.parameters;

import com.zelix.klassmaster.classfile.AbstractMethodInfo;
import com.zelix.klassmaster.classfile.FieldNameTypeSignature;
import com.zelix.klassmaster.classfile.MemberSignatureBase;
import com.zelix.klassmaster.classfile.MethodInfo;
import com.zelix.klassmaster.classfile.MethodSignature;
import com.zelix.klassmaster.classfile.hierarchy.ClassHierarchyNode;
import com.zelix.klassmaster.classfile.hierarchy.MethodOverrideAnalyzer;
import com.zelix.klassmaster.config.HiddenOptionFlags;
import com.zelix.klassmaster.exceptions.ZkmException;
import com.zelix.klassmaster.util.MutableInt;
import com.zelix.klassmaster.util.ObservableHolder;
import com.zelix.klassmaster.util.SetMultiMap;
import com.zelix.klassmaster.util.ZkmUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class ParameterListGenerator {
    public final MethodOverrideAnalyzer overrideAnalyzer;
    public final MethodParameterChangeSet changeSet;
    public final boolean randomizeLayouts;
    public final Random random;

    public ChangedMethodDescriptor generateForStaticMethod(
            ClassHierarchyNode owningClass, MethodInfo method, SetMultiMap reservedMemberNames, Map reservedMethodSignatures, Map reservedNameTypes, boolean forceExtraParameters, boolean includeReturnTypeInConflicts
    ) throws ZkmException, IOException {
        return this.generateDescriptor(owningClass, method, reservedMemberNames, owningClass.isInterface(), reservedMethodSignatures, reservedNameTypes, forceExtraParameters, includeReturnTypeInConflicts);
    }

    public final ChangedMethodDescriptor generateDescriptor(
            ClassHierarchyNode owningClass, MethodInfo method, SetMultiMap reservedMemberNames, boolean reserveOverrideGroup, Map reservedMethodSignatures, Map reservedNameTypes, boolean forceExtraParameters, boolean includeReturnTypeInConflicts
    ) throws ZkmException, IOException {
        boolean shuffleCandidates = false;
        boolean shuffleTypeLayouts = false;
        if (this.randomizeLayouts) {
            int extraParameterOdds = 1;
            if (HiddenOptionFlags.EXTRA_PARAMETER_ODDS != null) {
                try {
                    extraParameterOdds = Integer.parseInt(HiddenOptionFlags.EXTRA_PARAMETER_ODDS);
                } catch (Exception invalidOdds) {
                }
            }

            if (forceExtraParameters || HiddenOptionFlags.ALWAYS_ADD_EXTRA_PARAMETERS || this.random.nextInt(extraParameterOdds) == 0) {
                shuffleCandidates = true;
            }

            int shuffleLayoutOdds = 4;
            if (HiddenOptionFlags.EXTRA_PARAMETER_COUNT != null) {
                try {
                    shuffleLayoutOdds = Integer.parseInt(HiddenOptionFlags.EXTRA_PARAMETER_COUNT);
                } catch (Exception invalidLayoutOdds) {
                }
            } else if (forceExtraParameters || HiddenOptionFlags.ALWAYS_ADD_EXTRA_PARAMETERS) {
                shuffleLayoutOdds = 25;
            }

            if (this.random.nextInt(shuffleLayoutOdds) == 0) {
                shuffleTypeLayouts = true;
            }
        }

        char[][] typeLayouts;
        if (shuffleTypeLayouts) {
            typeLayouts = new char[ExtraParamLayoutGenerator.PARAM_TYPE_LAYOUTS.length][];
            System.arraycopy(ExtraParamLayoutGenerator.PARAM_TYPE_LAYOUTS, 0, typeLayouts, 0, ExtraParamLayoutGenerator.PARAM_TYPE_LAYOUTS.length);
            ZkmUtils.shuffleArray(typeLayouts, ZkmUtils.createRandom(131));
        } else {
            typeLayouts = ExtraParamLayoutGenerator.PARAM_TYPE_LAYOUTS;
        }

        MethodSignature originalSignature = method.getSignature();
        boolean varargs = method.isVarargs();
        if (varargs) {
            List parameterDescriptors = MethodSignature.splitParameterDescriptors(originalSignature.getDescriptor());
            if (parameterDescriptors.size() == 0) {
                return null;
            }
        }

        ChangedMethodDescriptor candidateDescriptor;
        if (!shuffleCandidates && !HiddenOptionFlags.SKIP_PARAMETER_LIST_REUSE) {
            candidateDescriptor = this.findSequentialDescriptor(owningClass, method, originalSignature, varargs, reservedMemberNames, reserveOverrideGroup, reservedMethodSignatures, reservedNameTypes, includeReturnTypeInConflicts, typeLayouts);
        } else {
            candidateDescriptor = this.findShuffledDescriptor(owningClass, method, originalSignature, varargs, reservedMemberNames, reserveOverrideGroup, reservedMethodSignatures, reservedNameTypes, includeReturnTypeInConflicts, typeLayouts);
        }

        return candidateDescriptor;
    }

    public ChangedMethodDescriptor findShuffledDescriptor(
            ClassHierarchyNode owningClass,
            MethodInfo method,
            MethodSignature originalSignature,
            boolean preserveVarargs,
            SetMultiMap reservedMemberNames,
            boolean reserveOverrideGroup,
            Map reservedMethodSignatures,
            Map reservedNameTypes,
            boolean includeReturnTypeInConflicts,
            char[][] typeLayouts
    ) throws ZkmException, IOException {
        String methodName = originalSignature.getName();
        HashSet seenDescriptors = ZkmUtils.createHashSet();

        for (int typeLayoutIndexValue = 0; typeLayoutIndexValue < typeLayouts.length; typeLayoutIndexValue++) {
            char[] parameterTypes = typeLayouts[typeLayoutIndexValue];
            List candidateDescriptors = listCandidateDescriptors(method.hasLambdaArgCount(), originalSignature, preserveVarargs, parameterTypes);
            if (candidateDescriptors.size() > 1) {
                ZkmUtils.shuffleList(candidateDescriptors, this.random);
            }

            Iterator candidateIterator = candidateDescriptors.iterator();

            while (candidateIterator.hasNext()) {
                ChangedMethodDescriptor candidateDescriptor = (ChangedMethodDescriptor) candidateIterator.next();
                String candidateDescriptorText = candidateDescriptor.getDescriptor();
                if (seenDescriptors.add(candidateDescriptorText)) {
                    MethodSignature candidateSignature = new MethodSignature(methodName, candidateDescriptorText);
                    if (this.isDescriptorUsable(
                            owningClass, method, candidateDescriptor, candidateSignature, originalSignature, reservedMemberNames, reservedMethodSignatures, reservedNameTypes, includeReturnTypeInConflicts, reserveOverrideGroup
                    )) {
                        return candidateDescriptor;
                    }
                }
            }
        }

        return null;
    }

    public final boolean isDescriptorUsable(
            ClassHierarchyNode owningClass,
            MethodInfo method,
            ChangedMethodDescriptor candidateDescriptor,
            MethodSignature candidateSignature,
            MethodSignature originalSignature,
            SetMultiMap reservedMemberNames,
            Map reservedMethodSignatures,
            Map reservedNameTypes,
            boolean includeReturnTypeInConflicts,
            boolean reserveOverrideGroup
    ) throws ZkmException, IOException {
        FieldNameTypeSignature candidateNameType = candidateSignature.getNameTypeSignature();
        MemberSignatureBase conflictKey = includeReturnTypeInConflicts ? candidateSignature : candidateNameType;
        if (candidateSignature.getDescriptor().equals(originalSignature.getDescriptor())) {
            return false;
        }

        if ((!includeReturnTypeInConflicts || !reservedMethodSignatures.containsKey(candidateSignature)) && (includeReturnTypeInConflicts || !reservedNameTypes.containsKey(candidateSignature.getNameTypeSignature()))) {
            if (!originalSignature.isConstructor() && reservedMemberNames.containsKey(conflictKey)) {
                return false;
            } else if (this.overrideAnalyzer.isSignatureReserved(candidateSignature)) {
                return false;
            } else if (this.overrideAnalyzer.isReservedForOther(owningClass, candidateSignature, originalSignature)) {
                return false;
            } else {
                return (!(reserveOverrideGroup && !this.overrideAnalyzer.tryReserveGroupRename(owningClass, candidateSignature, originalSignature, true, new ObservableHolder())) && (this.isAddedParameterPlacementValid(method, candidateDescriptor)));
            }
        } else {
            return false;
        }
    }

    public ChangedMethodDescriptor generateForPrivateMethod(
            ClassHierarchyNode owningClass, MethodInfo method, SetMultiMap reservedMemberNames, Map reservedMethodSignatures, Map reservedNameTypes, boolean forceExtraParameters, boolean includeReturnTypeInConflicts
    ) throws ZkmException, IOException {
        return this.generateDescriptor(owningClass, method, reservedMemberNames, owningClass.isInterface(), reservedMethodSignatures, reservedNameTypes, forceExtraParameters, includeReturnTypeInConflicts);
    }

    public static List listCandidateDescriptors(boolean preserveLambdaArguments, MethodSignature originalSignature, boolean preserveVarargs, char[] parameterTypes) {
        ArrayList candidateDescriptors = new ArrayList();
        String methodName = originalSignature.getName();
        String originalDescriptor = originalSignature.getDescriptor();
        String returnDescriptor = MethodSignature.getReturnPart(originalDescriptor);
        List parameterDescriptors = MethodSignature.splitParameterDescriptors(originalDescriptor);
        ChangedMethodDescriptor candidateDescriptor = null;
        ExtraParamLayoutGenerator layoutGenerator = new ExtraParamLayoutGenerator();
        HashSet seenDescriptors = ZkmUtils.createHashSet();

        while (true) {
            candidateDescriptor = layoutGenerator.nextLayout(preserveLambdaArguments, parameterDescriptors, returnDescriptor, parameterTypes, candidateDescriptor, preserveVarargs);
            if (candidateDescriptor == null) {
                return candidateDescriptors;
            }

            String candidateDescriptorText = candidateDescriptor.getDescriptor();
            if (seenDescriptors.add(candidateDescriptorText)) {
                new MethodSignature(methodName, candidateDescriptorText);
                candidateDescriptors.add(candidateDescriptor);
            }
        }
    }

    public ParameterListGenerator(MethodParameterChangeSet changeSet, boolean randomizeLayouts, Random random) {
        this.overrideAnalyzer = changeSet.getOverrideAnalyzer();
        this.changeSet = changeSet;
        this.randomizeLayouts = randomizeLayouts;
        this.random = random;
    }

    public ChangedMethodDescriptor generateForVirtualMethod(
            ClassHierarchyNode owningClass, MethodInfo method, SetMultiMap reservedMemberNames, boolean reserveOverrideGroup, Map reservedMethodSignatures, Map reservedNameTypes, boolean forceExtraParameters, boolean includeReturnTypeInConflicts
    ) throws ZkmException, IOException {
        MethodSignature originalSignature = method.getSignature();
        ObservableHolder interfaceMethodGroup = null;
        if (owningClass.isInterface()) {
            interfaceMethodGroup = this.overrideAnalyzer.getInterfaceMethodGroup(owningClass, originalSignature);
        }

        ChangedMethodDescriptor candidateDescriptor;
        if (interfaceMethodGroup != null) {
            candidateDescriptor = this.overrideAnalyzer.getGroupChangedDescriptor(interfaceMethodGroup);
            if (candidateDescriptor == null) {
                AbstractMethodInfo rootMethod = this.overrideAnalyzer.findRootMethod(method);
                if (rootMethod != null) {
                    String rootClassName = rootMethod.getClassName();
                    ClassHierarchyNode rootClass = ClassHierarchyNode.findNode(rootClassName);
                    if (rootClass == null || rootClass.hasNoClassFile()) {
                        candidateDescriptor = null;
                    } else if (!rootClass.isProgramClass()) {
                        candidateDescriptor = this.changeSet.getChangedDescriptor(rootMethod);
                    } else {
                        candidateDescriptor = this.generateDescriptor(owningClass, method, reservedMemberNames, reserveOverrideGroup, reservedMethodSignatures, reservedNameTypes, forceExtraParameters, includeReturnTypeInConflicts);
                    }
                } else {
                    candidateDescriptor = this.generateDescriptor(owningClass, method, reservedMemberNames, reserveOverrideGroup, reservedMethodSignatures, reservedNameTypes, forceExtraParameters, includeReturnTypeInConflicts);
                }

                if (candidateDescriptor != null) {
                    this.overrideAnalyzer.setGroupChangedDescriptor(interfaceMethodGroup, candidateDescriptor);
                }
            }
        } else {
            AbstractMethodInfo rootMethod = this.overrideAnalyzer.findRootMethod(method);
            if (rootMethod != null) {
                ClassHierarchyNode rootClass = ClassHierarchyNode.findNode(rootMethod.getClassName());
                if (rootClass != null && !rootClass.hasNoClassFile()) {
                    candidateDescriptor = this.changeSet.getChangedDescriptor(rootMethod);
                } else {
                    candidateDescriptor = null;
                }
            } else {
                candidateDescriptor = this.generateDescriptor(owningClass, method, reservedMemberNames, reserveOverrideGroup, reservedMethodSignatures, reservedNameTypes, forceExtraParameters, includeReturnTypeInConflicts);
            }
        }

        return candidateDescriptor;
    }

    public ChangedMethodDescriptor findSequentialDescriptor(
            ClassHierarchyNode owningClass,
            MethodInfo method,
            MethodSignature originalSignature,
            boolean preserveVarargs,
            SetMultiMap reservedMemberNames,
            boolean reserveOverrideGroup,
            Map reservedMethodSignatures,
            Map reservedNameTypes,
            boolean includeReturnTypeInConflicts,
            char[][] typeLayouts
    ) throws ZkmException, IOException {
        String methodName = originalSignature.getName();
        String originalDescriptor = originalSignature.getDescriptor();
        String returnDescriptor = MethodSignature.getReturnPart(originalDescriptor);
        List parameterDescriptors = MethodSignature.splitParameterDescriptors(originalDescriptor);
        ChangedMethodDescriptor candidateDescriptor = null;
        MutableInt typeLayoutIndex = new MutableInt();
        ExtraParamLayoutGenerator layoutGenerator = new ExtraParamLayoutGenerator();
        HashSet seenDescriptors = ZkmUtils.createHashSet();

        while (true) {
            candidateDescriptor = layoutGenerator.nextLayoutWithTypes(
                    method.hasLambdaArgCount(), parameterDescriptors, returnDescriptor, typeLayoutIndex, candidateDescriptor, preserveVarargs, typeLayouts
            );
            if (candidateDescriptor == null) {
                return null;
            }

            String candidateDescriptorText = candidateDescriptor.getDescriptor();
            if (seenDescriptors.add(candidateDescriptorText)) {
                MethodSignature candidateSignature = new MethodSignature(methodName, candidateDescriptorText);
                if (this.isDescriptorUsable(
                        owningClass, method, candidateDescriptor, candidateSignature, originalSignature, reservedMemberNames, reservedMethodSignatures, reservedNameTypes, includeReturnTypeInConflicts, reserveOverrideGroup
                )) {
                    return candidateDescriptor;
                }
            }
        }
    }

    public boolean isAddedParameterPlacementValid(MethodInfo method, ChangedMethodDescriptor candidateDescriptor) {
        if (!method.hasLambdaArgCount()) {
            return true;
        }

        int lambdaArgCount = method.getLambdaArgCount();
        if (lambdaArgCount > 0) {
            AddedParameter[] addedParameters = candidateDescriptor.getAddedParameters();
            AddedParameter lastAddedParameter = addedParameters[addedParameters.length - 1];
            List parameterDescriptors = MethodSignature.splitParameterDescriptors(method.getDescriptor());
            if (lastAddedParameter.getIndex() >= parameterDescriptors.size() + addedParameters.length - lambdaArgCount) {
                return false;
            }
        }

        return true;
    }
}

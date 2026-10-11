package com.zelix.klassmaster.obfuscator.parameters;

import com.zelix.klassmaster.config.HiddenOptionFlags;
import com.zelix.klassmaster.util.MutableInt;

import java.util.List;

public class ExtraParamLayoutGenerator {
    public static final char[][] PARAM_TYPE_LAYOUTS = new char[][]{
            {'J'},
            {'I', 'I'},
            {'B', 'J'},
            {'J', 'B'},
            {'S', 'J'},
            {'J', 'S'},
            {'C', 'J'},
            {'J', 'C'},
            {'I', 'J'},
            {'J', 'I'},
            {'I', 'I', 'B'},
            {'I', 'B', 'I'},
            {'B', 'I', 'I'},
            {'S', 'S', 'I'},
            {'S', 'I', 'S'},
            {'S', 'I', 'I'},
            {'S', 'I', 'C'},
            {'S', 'C', 'I'},
            {'I', 'S', 'S'},
            {'I', 'S', 'I'},
            {'I', 'S', 'C'},
            {'I', 'I', 'S'},
            {'I', 'I', 'I'},
            {'I', 'I', 'C'},
            {'I', 'C', 'S'},
            {'I', 'C', 'I'},
            {'I', 'C', 'C'},
            {'C', 'S', 'I'},
            {'C', 'I', 'S'},
            {'C', 'I', 'I'},
            {'C', 'I', 'C'},
            {'C', 'C', 'I'}
    };

    public static boolean isFullyShiftedLeft(ChangedMethodDescriptor layout) {
        if (layout != null) {
            int lastAddedParameterIndex = layout.getAddedParamCount() - 1;
            return layout.getLastParam().getIndex() == lastAddedParameterIndex;
        } else {
            return false;
        }
    }

    public static ChangedMethodDescriptor nextLayoutShiftingLeft(
            List parameterDescriptors, String returnDescriptor, char[] addedParameterTypes, ChangedMethodDescriptor previousLayout, boolean preserveVarargs, boolean firstLayout
    ) {
        int originalParameterCount = parameterDescriptors.size();
        int addedParameterCount = addedParameterTypes.length;
        int[] parameterIndices = new int[addedParameterCount];
        if (!firstLayout) {
            parameterIndices = previousLayout.getParamIndices();

            for (int parameterIndiceIndex = 0; parameterIndiceIndex < addedParameterCount; parameterIndiceIndex++) {
                if (parameterIndices[parameterIndiceIndex] != parameterIndiceIndex) {
                    parameterIndices[parameterIndiceIndex]--;

                    for (int parameterIndiceIndexValue = parameterIndiceIndex - 1; parameterIndiceIndexValue >= 0; parameterIndiceIndexValue += -1) {
                        parameterIndices[parameterIndiceIndexValue] = parameterIndices[parameterIndiceIndexValue + 1] - 1;
                    }
                    break;
                }
            }
        } else {
            for (int parameterIndiceIndexValue2 = 0; parameterIndiceIndexValue2 < addedParameterCount; parameterIndiceIndexValue2++) {
                parameterIndices[parameterIndiceIndexValue2] = originalParameterCount + parameterIndiceIndexValue2 - (preserveVarargs ? 1 : 0);
            }
        }

        return new ChangedMethodDescriptor(parameterDescriptors, returnDescriptor, addedParameterTypes, parameterIndices);
    }

    public ChangedMethodDescriptor nextLayout(boolean preserveLambdaArguments, List parameterDescriptors, String returnDescriptor, char[] addedParameterTypes, ChangedMethodDescriptor previousLayout, boolean preserveVarargs) {
        boolean firstLayout = previousLayout == null;
        boolean exhausted;
        if (!HiddenOptionFlags.ALWAYS_REGENERATE_PARAM_LAYOUT && !preserveLambdaArguments) {
            exhausted = isFullyShiftedLeft(previousLayout);
        } else {
            exhausted = isFullyShiftedRight(parameterDescriptors, previousLayout, preserveVarargs);
        }

        if (exhausted) {
            return null;
        }

        ChangedMethodDescriptor nextLayout;
        if (!HiddenOptionFlags.ALWAYS_REGENERATE_PARAM_LAYOUT && !preserveLambdaArguments) {
            nextLayout = nextLayoutShiftingLeft(parameterDescriptors, returnDescriptor, addedParameterTypes, previousLayout, preserveVarargs, firstLayout);
        } else {
            nextLayout = nextLayoutShiftingRight(parameterDescriptors, returnDescriptor, addedParameterTypes, previousLayout, preserveVarargs, firstLayout);
        }

        return nextLayout;
    }

    public static boolean isFullyShiftedRight(List parameterDescriptors, ChangedMethodDescriptor layout, boolean preserveVarargs) {
        return ((layout != null) && (layout.getFirstParam().getIndex() >= parameterDescriptors.size() - (preserveVarargs ? 1 : 0)));
    }

    public static ChangedMethodDescriptor nextLayoutShiftingRight(
            List parameterDescriptors, String returnDescriptor, char[] addedParameterTypes, ChangedMethodDescriptor previousLayout, boolean preserveVarargs, boolean firstLayout
    ) {
        int originalParameterCount = parameterDescriptors.size();
        int addedParameterCount = addedParameterTypes.length;
        int[] parameterIndices = new int[addedParameterCount];
        if (!firstLayout) {
            parameterIndices = previousLayout.getParamIndices();
            int totalParameterCount = originalParameterCount + addedParameterCount;

            for (int parameterIndiceIndex = addedParameterCount - 1; parameterIndiceIndex >= 0; parameterIndiceIndex += -1) {
                if (parameterIndices[parameterIndiceIndex] < totalParameterCount - addedParameterCount + parameterIndiceIndex - (preserveVarargs ? 1 : 0)) {
                    parameterIndices[parameterIndiceIndex]++;

                    for (int parameterIndiceIndexValue = parameterIndiceIndex + 1; parameterIndiceIndexValue < addedParameterCount; parameterIndiceIndexValue++) {
                        parameterIndices[parameterIndiceIndexValue] = parameterIndices[parameterIndiceIndexValue - 1] + 1;
                    }
                    break;
                }
            }
        } else {
            int addedParameterIndex = 0;

            while (addedParameterIndex < addedParameterCount) {
                parameterIndices[addedParameterIndex] = addedParameterIndex++;
            }
        }

        return new ChangedMethodDescriptor(parameterDescriptors, returnDescriptor, addedParameterTypes, parameterIndices);
    }

    public ChangedMethodDescriptor nextLayoutWithTypes(
            boolean preserveLambdaArguments, List parameterDescriptors, String returnDescriptor, MutableInt typeLayoutIndex, ChangedMethodDescriptor previousLayout, boolean preserveVarargs, char[][] typeLayouts
    ) {
        char[] addedParameterTypes = typeLayouts[typeLayoutIndex.getValue()];
        boolean firstLayout = previousLayout == null;
        boolean exhausted;
        if (!HiddenOptionFlags.ALWAYS_REGENERATE_PARAM_LAYOUT && !preserveLambdaArguments) {
            exhausted = isFullyShiftedLeft(previousLayout);
        } else {
            exhausted = isFullyShiftedRight(parameterDescriptors, previousLayout, preserveVarargs);
        }

        boolean regenerateLayouts;
        if (exhausted) {
            typeLayoutIndex.incrementAndGet();
            if (typeLayoutIndex.getValue() >= typeLayouts.length) {
                return null;
            }

            addedParameterTypes = typeLayouts[typeLayoutIndex.getValue()];
            firstLayout = true;
            regenerateLayouts = HiddenOptionFlags.ALWAYS_REGENERATE_PARAM_LAYOUT;
        } else {
            regenerateLayouts = HiddenOptionFlags.ALWAYS_REGENERATE_PARAM_LAYOUT;
        }

        ChangedMethodDescriptor nextLayout;
        if (!regenerateLayouts && !preserveLambdaArguments) {
            nextLayout = nextLayoutShiftingLeft(parameterDescriptors, returnDescriptor, addedParameterTypes, previousLayout, preserveVarargs, firstLayout);
        } else {
            nextLayout = nextLayoutShiftingRight(parameterDescriptors, returnDescriptor, addedParameterTypes, previousLayout, preserveVarargs, firstLayout);
        }

        return nextLayout;
    }
}

package com.zelix.klassmaster.obfuscator.parameters;

import com.zelix.klassmaster.classfile.MethodSignature;
import com.zelix.klassmaster.classfile.constpool.ConstantPoolEntry;
import com.zelix.klassmaster.classfile.insn.LocalVariableSlot;
import com.zelix.klassmaster.classfile.insn.MethodBytecode;
import com.zelix.klassmaster.config.HiddenOptionFlags;
import com.zelix.klassmaster.util.ZkmUtils;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChangedMethodDescriptor {
    public Map localSlotsByMethod;
    private final String newDescriptor;
    private final AddedParameter[] addedParameters;

    public int countByteParams() {
        int byteParameterCount = 0;
        AddedParameter[] parameters = this.addedParameters;
        int addedParameterCount = parameters.length;

        for (int parameterIndexValue = 0; parameterIndexValue < addedParameterCount; parameterIndexValue++) {
            if (parameters[parameterIndexValue].getTypeChar() == 'B') {
                byteParameterCount++;
            }
        }

        return byteParameterCount;
    }

    public AddedParameter[] getAddedParameters() {
        return this.addedParameters.clone();
    }

    public int getKeyBitCount(int parameterIndex) {
        char typeChar = this.addedParameters[parameterIndex].getTypeChar();
        byte bitCount;
        if (typeChar == 'B') {
            bitCount = 8;
        } else {
            byte shortBitCount;
            if (typeChar != 'S') {
                if (typeChar != 'C') {
                    if (typeChar == 'J') {
                        return 64 - (parameterIndex == 0 ? this.addedParameters[1].getBitWidth() : this.addedParameters[0].getBitWidth());
                    }

                    if (this.addedParameters.length == 2) {
                        return 32;
                    }

                    if (parameterIndex == 0) {
                        return 32;
                    }

                    int byteParameterCount = this.countByteParams();
                    int intParameterCount = this.countIntParams();
                    int firstIntParameterIndex = this.indexOfIntParam();
                    if (parameterIndex == firstIntParameterIndex) {
                        return 32;
                    }

                    if (intParameterCount == 3) {
                        return 16;
                    }

                    if (byteParameterCount == 1) {
                        bitCount = 24;
                    } else {
                        bitCount = 16;
                    }

                    return bitCount;
                }

                shortBitCount = 16;
            } else {
                shortBitCount = 16;
            }

            bitCount = shortBitCount;
        }

        return bitCount;
    }

    public ChangedMethodDescriptor(String originalDescriptor, AddedParameter[] addedParameters) {
        String returnDescriptor = MethodSignature.getReturnPart(originalDescriptor);
        List parameterDescriptors = MethodSignature.splitParameterDescriptors(originalDescriptor);
        int totalParameterCount = parameterDescriptors.size() + addedParameters.length;
        int addedParameterCount = addedParameters.length;
        int addedParameterIndex = 0;
        Iterator originalParameters = parameterDescriptors.iterator();
        StringBuilder descriptorBuilder = new StringBuilder();
        descriptorBuilder.append('(');

        for (int i = 0; i < totalParameterCount; i++) {
            while (addedParameterIndex < addedParameterCount && addedParameters[addedParameterIndex].getIndex() == i) {
                descriptorBuilder.append(addedParameters[addedParameterIndex].getTypeChar());
                addedParameterIndex++;
                i++;
            }

            if (originalParameters.hasNext()) {
                String parameterDescriptor = (String) originalParameters.next();
                descriptorBuilder.append(parameterDescriptor);
            }
        }

        descriptorBuilder.append(')');
        descriptorBuilder.append(returnDescriptor);
        this.newDescriptor = descriptorBuilder.toString();
        this.addedParameters = addedParameters;
    }

    public void registerLocalSlot(MethodBytecode methodBytecode, int addedParameterIndex, LocalVariableSlot localSlot) {
        if (this.localSlotsByMethod == null) {
            if (HiddenOptionFlags.USE_PARALLEL) {
                this.localSlotsByMethod = new ConcurrentHashMap(13);
            } else {
                this.localSlotsByMethod = ZkmUtils.createHashMap(13);
            }
        }

        IndexedLocalSlot[] registeredSlots = (IndexedLocalSlot[]) this.localSlotsByMethod.get(methodBytecode);
        if (registeredSlots == null) {
            registeredSlots = new IndexedLocalSlot[this.addedParameters.length];
            this.localSlotsByMethod.put(methodBytecode, registeredSlots);
        }

        registeredSlots[addedParameterIndex] = new IndexedLocalSlot(localSlot, addedParameterIndex);
    }

    public int getBitOffset(int parameterIndex) {
        int bitOffset = 0;
        int firstParameterWidth = this.getKeyBitCount(0);
        if (parameterIndex == 1) {
            bitOffset = firstParameterWidth;
        } else if (parameterIndex == 2) {
            int secondParameterWidth = this.getKeyBitCount(1);
            bitOffset = firstParameterWidth + secondParameterWidth;
        }

        return bitOffset;
    }

    public ChangedMethodDescriptor(List parameterDescriptors, String returnDescriptor, char[] addedParameterTypes, int[] addedParameterIndices) {
        this.addedParameters = new AddedParameter[addedParameterTypes.length];

        for (int i = 0; i < addedParameterTypes.length; i++) {
            this.addedParameters[i] = new AddedParameter(addedParameterIndices[i], addedParameterTypes[i]);
        }

        int addedParameterCount = addedParameterTypes.length;
        int totalParameterCount = parameterDescriptors.size() + addedParameterTypes.length;
        StringBuilder descriptorBuilder = new StringBuilder();
        descriptorBuilder.append('(');
        int addedParameterIndex = 0;
        Iterator originalParameters = parameterDescriptors.iterator();

        for (int i = 0; i < totalParameterCount; i++) {
            while (addedParameterIndex < addedParameterCount && addedParameterIndices[addedParameterIndex] == i) {
                descriptorBuilder.append(addedParameterTypes[addedParameterIndex]);
                addedParameterIndex++;
                i++;
            }

            if (originalParameters.hasNext()) {
                String parameterDescriptor = (String) originalParameters.next();
                descriptorBuilder.append(parameterDescriptor);
            }
        }

        descriptorBuilder.append(')');
        descriptorBuilder.append(returnDescriptor);
        this.newDescriptor = descriptorBuilder.toString();
    }

    public static int[] computeLocalIndices(ChangedMethodDescriptor descriptor, boolean staticMethod) {
        List parameterTypes = ConstantPoolEntry.getParameterTypes(descriptor.newDescriptor);
        int parameterCount = parameterTypes.size();
        int addedParameterCount = descriptor.addedParameters.length;
        int[] localIndices = new int[addedParameterCount];
        int localSlotIndex = staticMethod ? 0 : 1;
        int addedParameterIndex = 0;
        int nextAddedParameterIndex = descriptor.addedParameters[0].getIndex();

        for (int elementIndex = 0; elementIndex < parameterCount; elementIndex++) {
            if (elementIndex == nextAddedParameterIndex) {
                localIndices[addedParameterIndex] = localSlotIndex;
                if (++addedParameterIndex < addedParameterCount) {
                    nextAddedParameterIndex = descriptor.addedParameters[addedParameterIndex].getIndex();
                } else {
                    nextAddedParameterIndex = -1;
                }
            }

            String parameterType = (String) parameterTypes.get(elementIndex);
            localSlotIndex++;
            if (parameterType.equals("J") || parameterType.equals("D")) {
                localSlotIndex++;
            }
        }

        return localIndices;
    }

    public char getTypeCharAt(int parameterIndex) {
        return this.addedParameters[parameterIndex].getTypeChar();
    }

    public boolean hasMultipleParams() {
        return this.addedParameters.length > 1;
    }

    public String getDescriptor() {
        return this.newDescriptor;
    }

    public int indexOfIntParam() {
        int parameterIndex = 0;
        AddedParameter[] parameters = this.addedParameters;
        int addedParameterCount = parameters.length;

        for (int parameterIndexValue = 0; parameterIndexValue < addedParameterCount; parameterIndexValue++) {
            if (parameters[parameterIndexValue].getTypeChar() == 'I') {
                return parameterIndex;
            }

            parameterIndex++;
        }

        return -1;
    }

    public IndexedLocalSlot[] getLocalSlots(Object method) {
        return (IndexedLocalSlot[]) this.localSlotsByMethod.get(method);
    }

    public int getUnusedBitCount(int parameterIndex) {
        return 64 - this.getKeyBitCount(parameterIndex);
    }

    public AddedParameter getLastParam() {
        return this.addedParameters[this.addedParameters.length - 1];
    }

    public int getParamIndex(int parameterIndex) {
        return this.addedParameters[parameterIndex].getIndex();
    }

    public int getParamCount() {
        return ConstantPoolEntry.getParameterTypes(this.newDescriptor).size();
    }

    public boolean hasNoAddedParams() {
        return this.addedParameters.length == 0;
    }

    public boolean isLongParam(int parameterIndex) {
        return this.addedParameters[parameterIndex].getTypeChar() == 'J';
    }

    public int[] getParamIndices() {
        int addedParameterCount = this.addedParameters.length;
        int[] parameterIndices = new int[addedParameterCount];

        for (int i = 0; i < addedParameterCount; i++) {
            parameterIndices[i] = this.addedParameters[i].getIndex();
        }

        return parameterIndices;
    }

    public int getShiftCount(int parameterIndex) {
        return 64 - this.getKeyBitCount(parameterIndex);
    }

    public AddedParameter getFirstParam() {
        return this.addedParameters[0];
    }

    public int getAddedParamCount() {
        return this.addedParameters.length;
    }

    public int getKeyBitOffset(int parameterIndex) {
        int bitOffset;
        if (parameterIndex == 0) {
            bitOffset = 0;
        } else {
            int firstParameterWidth = this.getKeyBitCount(0);
            if (parameterIndex == 1) {
                bitOffset = firstParameterWidth;
            } else {
                int secondParameterWidth = this.getKeyBitCount(1);
                bitOffset = firstParameterWidth + secondParameterWidth;
            }
        }

        return bitOffset;
    }

    public int countIntParams() {
        int intParameterCount = 0;
        AddedParameter[] parameters = this.addedParameters;
        int addedParameterCount = parameters.length;

        for (int parameterIndexValue = 0; parameterIndexValue < addedParameterCount; parameterIndexValue++) {
            if (parameters[parameterIndexValue].getTypeChar() == 'I') {
                intParameterCount++;
            }
        }

        return intParameterCount;
    }

    public static String buildParameterDescriptor(List parameterDescriptors, AddedParameter[] addedParameters) {
        int totalParameterCount = parameterDescriptors.size() + addedParameters.length;
        int addedParameterCount = addedParameters.length;
        StringBuilder descriptorBuilder = new StringBuilder();
        int originalParameterIndex = 0;
        int addedParameterIndex = 0;
        AddedParameter nextAddedParameter;
        StringBuilder descriptorBuilderAlias;
        char openingParenthesis;
        if (addedParameters.length == 0) {
            nextAddedParameter = null;
            descriptorBuilderAlias = descriptorBuilder;
            openingParenthesis = '(';
        } else {
            nextAddedParameter = addedParameters[0];
            descriptorBuilderAlias = descriptorBuilder;
            openingParenthesis = '(';
        }

        descriptorBuilderAlias.append(openingParenthesis);

        for (int i = 0; i < totalParameterCount; i++) {
            if (nextAddedParameter != null && i == nextAddedParameter.getIndex()) {
                descriptorBuilder.append(nextAddedParameter.getTypeChar());
                if (++addedParameterIndex < addedParameterCount) {
                    nextAddedParameter = addedParameters[addedParameterIndex];
                } else {
                    nextAddedParameter = null;
                }
            } else {
                String parameterDescriptor = (String) parameterDescriptors.get(originalParameterIndex);
                descriptorBuilder.append(parameterDescriptor);
                originalParameterIndex++;
            }
        }

        descriptorBuilder.append(')');
        return descriptorBuilder.toString();
    }
}

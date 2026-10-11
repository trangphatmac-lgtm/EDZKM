package com.zelix.klassmaster.obfuscator.reflection;

import com.zelix.klassmaster.classfile.insn.MethodBytecode;
import com.zelix.klassmaster.classfile.insn.StackFrameState;
import com.zelix.klassmaster.classfile.insn.TrackedValue;

public class PendingReflectionValue implements TrackedValue {
    private static final String PENDING_DISPLAY = "<pending>";
    public int callInstructionIndex = -1;
    public MethodBytecode methodBytecode;

    public MethodBytecode getMethodBytecode() {
        return this.methodBytecode;
    }

    public PendingReflectionValue(int callInstructionIndex, MethodBytecode methodBytecode1) {
        this.callInstructionIndex = callInstructionIndex;
        this.methodBytecode = methodBytecode1;
    }

    @Override
    public int getPendingInstructionIndex() {
        return this.callInstructionIndex;
    }

    @Override
    public boolean isPending() {
        return true;
    }

    @Override
    public boolean isKnown() {
        return false;
    }

    @Override
    public String getCombinedValueKey() {
        return this.getStringValue();
    }

    @Override
    public boolean isConstant() {
        return false;
    }

    @Override
    public String getStringValue() {
        return null;
    }

    public String getMethodDescription() {
        return this.methodBytecode.getQualifiedMethodName();
    }

    @Override
    public String getDisplayText() {
        return PENDING_DISPLAY;
    }

    @Override
    public boolean equals(Object other) {
        boolean strictMergeEnabled = StackFrameState.isStrictMergeEnabled();
        if (!(other instanceof PendingReflectionValue)) {
            return false;
        }
        return !strictMergeEnabled || this.callInstructionIndex == ((PendingReflectionValue) other).callInstructionIndex;
    }

    @Override
    public int hashCode() {
        return this.callInstructionIndex;
    }

    @Override
    public String getNormalizedName() {
        return this.getStringValue();
    }
}

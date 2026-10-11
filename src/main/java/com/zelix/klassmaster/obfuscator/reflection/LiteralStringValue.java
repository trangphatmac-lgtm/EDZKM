package com.zelix.klassmaster.obfuscator.reflection;

import com.zelix.klassmaster.classfile.insn.StackFrameState;
import com.zelix.klassmaster.classfile.insn.TrackedValue;

public class LiteralStringValue implements TrackedValue {
    public final String value;

    public LiteralStringValue(String valueArgument) {
        this.value = valueArgument;
    }

    @Override
    public boolean isKnown() {
        return true;
    }

    @Override
    public int getPendingInstructionIndex() {
        return -1;
    }

    @Override
    public String getStringValue() {
        return this.value;
    }

    @Override
    public String getCombinedValueKey() {
        return this.getStringValue();
    }

    @Override
    public boolean isConstant() {
        return true;
    }

    @Override
    public boolean equals(Object otherArgument) {
        boolean strictMergeEnabled = StackFrameState.isStrictMergeEnabled();
        if (!(otherArgument instanceof LiteralStringValue)) {
            return false;
        }
        if (!strictMergeEnabled) {
            return true;
        }
        LiteralStringValue other = (LiteralStringValue) otherArgument;
        return this.value == null ? other.value == null : other.value != null && this.value.equals(other.value);
    }

    @Override
    public int hashCode() {
        boolean strictMergeDisabled = StackFrameState.isStrictMergeDisabled();
        if (!strictMergeDisabled && this.value == null) {
            return 0;
        }
        return this.value.hashCode();
    }

    @Override
    public String getNormalizedName() {
        return this.getStringValue();
    }

    @Override
    public String getDisplayText() {
        return this.value;
    }

    @Override
    public boolean isPending() {
        return false;
    }
}

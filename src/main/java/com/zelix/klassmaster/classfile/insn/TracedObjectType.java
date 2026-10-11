package com.zelix.klassmaster.classfile.insn;

public class TracedObjectType implements TrackedValue {
    private static final String OBJECT_CLASS_NAME = "java/lang/Object";
    public Instruction sourceInstruction;
    public String className;

    public TracedObjectType(Instruction sourceInstruction, String className) {
        this.sourceInstruction = sourceInstruction;
        if (className.startsWith("[")) {
            this.className = OBJECT_CLASS_NAME;
        } else {
            this.className = className;
        }
    }

    @Override
    public String getNormalizedName() {
        return this.getStringValue();
    }

    @Override
    public int hashCode() {
        boolean strictMergeDisabled = StackFrameState.isStrictMergeDisabled();
        int hash = 0;
        if (!strictMergeDisabled && this.sourceInstruction != null) {
            hash = this.sourceInstruction.hashCode();
        }
        if (this.className != null) {
            hash ^= this.className.hashCode();
        }
        return hash;
    }

    @Override
    public boolean isPending() {
        return false;
    }

    @Override
    public boolean equals(Object other) {
        boolean strictMergeEnabled = StackFrameState.isStrictMergeEnabled();
        if (!(other instanceof TracedObjectType)) {
            return false;
        }
        if (!strictMergeEnabled) {
            return true;
        }

        TracedObjectType otherType = (TracedObjectType) other;
        if (this.sourceInstruction != null) {
            if (otherType.sourceInstruction == null
                    || !this.sourceInstruction.equals(otherType.sourceInstruction)) {
                return false;
            }
        } else if (otherType.sourceInstruction != null) {
            return false;
        }

        if (this.className != null) {
            return otherType.className != null && this.className.equals(otherType.className);
        }
        return otherType.className == null;
    }

    @Override
    public String getStringValue() {
        return this.className;
    }

    @Override
    public boolean isConstant() {
        return true;
    }

    @Override
    public int getPendingInstructionIndex() {
        return -1;
    }

    @Override
    public String getDisplayText() {
        return this.className;
    }

    @Override
    public boolean isKnown() {
        return true;
    }

    @Override
    public String getCombinedValueKey() {
        return this.getStringValue();
    }
}

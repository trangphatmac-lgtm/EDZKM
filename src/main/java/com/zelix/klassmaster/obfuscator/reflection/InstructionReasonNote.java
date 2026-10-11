package com.zelix.klassmaster.obfuscator.reflection;

import com.zelix.klassmaster.classfile.constpool.ResolvedMethodRef;
import com.zelix.klassmaster.classfile.insn.ConstantRefInstruction;
import com.zelix.klassmaster.classfile.insn.Instruction;
import com.zelix.klassmaster.classfile.insn.StackFrameState;
import com.zelix.klassmaster.classfile.insn.TrackedValue;

public class InstructionReasonNote implements TrackedValue {
    public Instruction instruction;
    public String reason;
    private static final String QUOTE_CLOSE_SUFFIX = "'>";

    @Override
    public String getDisplayText() {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("<");
        if (this.instruction != null) {
            switch (this.instruction.getOpcode()) {
                case 182:
                case 183:
                case 185:
                    ConstantRefInstruction constantRefInstruction = (ConstantRefInstruction) this.instruction;
                    ResolvedMethodRef resolvedMethodRef = (ResolvedMethodRef) constantRefInstruction.getConstantPoolEntry();
                    stringBuffer.append(
                            constantRefInstruction.getMnemonic() + " " + resolvedMethodRef.getReferencedClassName() + "." + resolvedMethodRef.getMemberName() + " "
                    );
                    break;
                case 184:
                default:
                    stringBuffer.append(this.instruction.getMnemonic() + " ");
            }
        }

        stringBuffer.append("'" + this.reason + QUOTE_CLOSE_SUFFIX);
        return stringBuffer.toString();
    }

    @Override
    public int getPendingInstructionIndex() {
        return -1;
    }

    public String getReason() {
        return this.reason;
    }

    @Override
    public boolean isConstant() {
        return false;
    }

    @Override
    public int hashCode() {
        boolean strictMergeDisabled = StackFrameState.isStrictMergeDisabled();
        int hash = 0;
        if (!strictMergeDisabled && this.instruction != null) {
            hash = this.instruction.hashCode();
        }
        if (this.reason != null) {
            hash ^= this.reason.hashCode();
        }
        return hash;
    }

    @Override
    public boolean isKnown() {
        return false;
    }

    public InstructionReasonNote(Instruction instruction1, String reasonArgument) {
        this.instruction = instruction1;
        this.reason = reasonArgument;
    }

    @Override
    public boolean isPending() {
        return false;
    }

    @Override
    public String getCombinedValueKey() {
        return this.getStringValue();
    }

    @Override
    public String getNormalizedName() {
        return this.getStringValue();
    }

    @Override
    public String getStringValue() {
        return null;
    }

    @Override
    public boolean equals(Object otherArgument) {
        boolean strictMergeEnabled = StackFrameState.isStrictMergeEnabled();
        if (!(otherArgument instanceof InstructionReasonNote)) {
            return false;
        }
        if (!strictMergeEnabled) {
            return true;
        }

        InstructionReasonNote other = (InstructionReasonNote) otherArgument;
        boolean sameInstruction = this.instruction == null
                ? other.instruction == null
                : other.instruction != null && this.instruction.equals(other.instruction);
        if (!sameInstruction) {
            return false;
        }
        return this.reason == null
                ? other.reason == null
                : other.reason != null && this.reason.equals(other.reason);
    }

}

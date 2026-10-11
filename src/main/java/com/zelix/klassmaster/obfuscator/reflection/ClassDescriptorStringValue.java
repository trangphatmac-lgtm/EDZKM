package com.zelix.klassmaster.obfuscator.reflection;

import com.zelix.klassmaster.classfile.constpool.ResolvedStringConstant;
import com.zelix.klassmaster.classfile.insn.StackFrameState;
import com.zelix.klassmaster.classfile.insn.StringConstantNameRef;

public class ClassDescriptorStringValue extends StringConstantNameRef {
    @Override
    public boolean equals(Object otherArgument) {
        boolean strictMergeDisabled = StackFrameState.isStrictMergeDisabled();
        if (otherArgument == null || !this.getClass().equals(otherArgument.getClass())) {
            return false;
        }
        if (strictMergeDisabled) {
            return true;
        }
        ClassDescriptorStringValue other = (ClassDescriptorStringValue) otherArgument;
        return super.stringConstant == null
                ? other.stringConstant == null
                : other.stringConstant != null && super.stringConstant.equals(other.stringConstant);
    }

    public ClassDescriptorStringValue(ResolvedStringConstant resolvedStringConstant) {
        super(resolvedStringConstant);
    }

    @Override
    public String getNormalizedName() {
        String editableValue = null;
        if (super.stringConstant != null) {
            editableValue = super.stringConstant.getEditableValue();
            int ba = editableValue.lastIndexOf("[");
            if (ba > -1) {
                editableValue = editableValue.substring(ba + 1);
            }

            if (editableValue.endsWith(";")) {
                editableValue = editableValue.substring(1, editableValue.length() - 1);
            }
        }

        return editableValue;
    }

    @Override
    public int hashCode() {
        boolean strictMergeDisabled = StackFrameState.isStrictMergeDisabled();
        if (!strictMergeDisabled && super.stringConstant == null) {
            return 0;
        }
        return super.stringConstant.hashCode() + 1;
    }
}

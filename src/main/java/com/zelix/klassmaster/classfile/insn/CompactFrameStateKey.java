package com.zelix.klassmaster.classfile.insn;

public class CompactFrameStateKey extends FrameStateKey {
    static {
        new CompactFrameStateKey();
    }

    public CompactFrameStateKey(StackFrameState stackFrameState) {
        VerifierType[] verifierTypes = stackFrameState.getStack();
        StringBuilder stringBuilder = new StringBuilder();
        super.stackSlotCount = 0;
        int lengthValue = verifierTypes.length;

        for (int verifierTypeIndex = 0; verifierTypeIndex < lengthValue; verifierTypeIndex++) {
            VerifierType verifierType = verifierTypes[verifierTypeIndex];
            super.stackSlotCount = super.stackSlotCount + verifierType.getSlotSize();
            if (!verifierType.isInitialized()) {
                super.hasUninitializedThis = true;
            }

            stringBuilder.append(verifierType.toDisplayString());
            if (verifierTypeIndex < lengthValue - 1) {
                stringBuilder.append("|");
            }
        }

        stringBuilder.append("||");
        boolean bl = false;
        VerifierType[] verifierTypes1 = stackFrameState.getLocals();
        int lengthValue2 = verifierTypes1.length;

        for (int verifierTypes1ElementIndex = 0; verifierTypes1ElementIndex < lengthValue2; verifierTypes1ElementIndex++) {
            if (!verifierTypes1[verifierTypes1ElementIndex].isInitialized()) {
                if (!bl) {
                    stringBuilder.append("**");
                }

                bl = true;
                super.hasUninitializedThis = true;
            }
        }

        SubroutineLocalsBitSet subroutineLocalsBitSet = stackFrameState.getSubroutineLocals();
        if (subroutineLocalsBitSet != null) {
            stringBuilder.append("|||");
            stringBuilder.append(subroutineLocalsBitSet.getSubroutineEntry().hashCode());
        }

        super.keyString = stringBuilder.toString();
    }

    private CompactFrameStateKey() {
        super.keyString = "";
        super.stackSlotCount = 0;
        super.hasUninitializedThis = true;
    }
}

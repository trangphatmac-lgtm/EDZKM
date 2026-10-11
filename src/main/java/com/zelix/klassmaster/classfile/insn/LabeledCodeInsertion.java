package com.zelix.klassmaster.classfile.insn;

import java.util.ArrayList;
import java.util.List;

public class LabeledCodeInsertion extends CodeInsertion {
    public boolean existingBranchTarget;
    public LabelInstruction targetLabel;

    public LabeledCodeInsertion(boolean existingBranchTarget, LabelInstruction labelInstruction, int position, int replacedCount) {
        super((Instruction) null, position, replacedCount, 0);
        this.existingBranchTarget = existingBranchTarget;
        this.targetLabel = labelInstruction;
    }

    public void appendInstructions(List list1) {
        int list1Size = list1.size();

        for (int elementIndex = 0; elementIndex < list1Size; elementIndex++) {
            this.instructions.add(list1.get(elementIndex));
        }
    }

    public void prependInstructions(List list1) {
        if (this.instructions.size() == 0) {
            for (int elementIndex = 0; elementIndex < list1.size(); elementIndex++) {
                this.instructions.add(list1.get(elementIndex));
            }
        } else {
            ArrayList arrayList = new ArrayList(this.instructions.size() + list1.size());

            for (int elementIndexValue = 0; elementIndexValue < list1.size(); elementIndexValue++) {
                arrayList.add(list1.get(elementIndexValue));
            }

            for (int elementIndexValue2 = 0; elementIndexValue2 < this.instructions.size(); elementIndexValue2++) {
                arrayList.add(this.instructions.get(elementIndexValue2));
            }

            this.instructions = arrayList;
        }
    }

    public boolean isExistingBranchTarget() {
        return this.existingBranchTarget;
    }

    public LabelInstruction getTargetLabel() {
        return this.targetLabel;
    }
}

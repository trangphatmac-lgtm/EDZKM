package com.zelix.klassmaster.classfile.insn;

import com.zelix.klassmaster.classfile.ClassFileInputStream;
import com.zelix.klassmaster.util.ListMultimap;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

public class LookupSwitchInstruction extends SwitchInstruction {
    public final int caseCount;
    public final int[] caseKeys;

    @Override
    public void printDisassembly(PrintWriter printWriter, StringBuilder stringBuilder) throws UnknownOpcodeException {
        StringBuilder stringBuilder1 = new StringBuilder();
        stringBuilder1.append(this.getMnemonic());
        printWriter.println(stringBuilder.toString() + stringBuilder1.toString());
        Object targetLabel = this.targetLabels[this.targetLabels.length - 1];
        printWriter.println(stringBuilder.toString() + "\tdefault: goto " + (targetLabel != null ? ((LabelInstruction) targetLabel).getLabelName() : targetLabel));
        int ba = 0;
        int bb = 0;

        for (int caseCountValue = this.caseCount; bb < caseCountValue; caseCountValue = this.caseCount) {
            Object targetLabelValue = this.targetLabels[ba];
            printWriter.println(
                    stringBuilder.toString() + "\tcase " + this.caseKeys[ba] + ": goto " + (targetLabelValue != null ? ((LabelInstruction) targetLabelValue).getLabelName() : targetLabelValue)
            );
            bb = ++ba;
        }
    }

    @Override
    public void writeTo(DataOutputStream dataOutputStream) throws IOException {
        super.writeTo(dataOutputStream);
        dataOutputStream.writeInt(this.caseCount);
        int ba = 0;
        int bb = 0;

        for (int caseCountValue = this.caseCount; bb < caseCountValue; caseCountValue = this.caseCount) {
            dataOutputStream.writeInt(this.caseKeys[ba]);
            LabelInstruction labelInstruction = this.targetLabels[ba];
            dataOutputStream.writeInt(labelInstruction.getOffset() - super.switchOffset);
            bb = ++ba;
        }
    }

    @Override
    public int getLength() {
        return 1 + super.paddingSize + 8 + this.caseCount * 8;
    }

    @Override
    public String toAssembly() {
        return this.getMnemonic();
    }

    public LookupSwitchInstruction(ClassFileInputStream classFileInputStream, int switchOffsetArgument, ListMultimap listMultimap) throws IOException {
        super(171, classFileInputStream, switchOffsetArgument);
        int bb = classFileInputStream.readInt();
        this.caseCount = classFileInputStream.readInt();
        this.caseKeys = new int[this.caseCount];
        this.targetLabels = new LabelInstruction[this.caseCount + 1];
        this.targetOffsets = new ArrayList(this.targetLabels.length);
        int caseKeyIndex = 0;
        int bf = 0;

        for (int caseCountValue = this.caseCount; bf < caseCountValue; caseCountValue = this.caseCount) {
            int caseKey = classFileInputStream.readInt();
            this.caseKeys[caseKeyIndex] = caseKey;
            int be = classFileInputStream.readInt();
            Integer integer = Instruction.integerCache.valueOf(super.switchOffset + be);
            listMultimap.addValue(integer, this);
            this.targetOffsets.add(integer);
            bf = ++caseKeyIndex;
        }

        Integer integer1 = Instruction.integerCache.valueOf(super.switchOffset + bb);
        listMultimap.addValue(integer1, this);
        this.targetOffsets.add(integer1);
    }
}

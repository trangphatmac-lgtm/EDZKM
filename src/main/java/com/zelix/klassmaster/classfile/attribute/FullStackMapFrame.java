package com.zelix.klassmaster.classfile.attribute;

import com.zelix.klassmaster.classfile.ClassFileComponent;
import com.zelix.klassmaster.classfile.ClassFileInputStream;
import com.zelix.klassmaster.classfile.constpool.UsedConstantsCollector;
import com.zelix.klassmaster.classfile.insn.LabelInstruction;
import com.zelix.klassmaster.util.IntegerCache;
import com.zelix.klassmaster.util.ListMultimap;
import com.zelix.klassmaster.util.MutableInt;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

public class FullStackMapFrame extends StackMapTableFrame {
    private final int frameType = 255;
    public final int offsetDelta;
    private final VerificationTypeInfo[] stackItems;
    private final VerificationTypeInfo[] locals;

    @Override
    public int getFrameSize() {
        int frameSize = 1;
        int lengthValue = this.locals.length;
        int lengthValue2 = this.stackItems.length;
        frameSize += 2;
        frameSize += 2;

        for (int localIndex = 0; localIndex < lengthValue; localIndex++) {
            frameSize += this.locals[localIndex].getByteSize();
        }

        frameSize += 2;

        for (int stackItemIndex = 0; stackItemIndex < lengthValue2; stackItemIndex++) {
            frameSize += this.stackItems[stackItemIndex].getByteSize();
        }

        return frameSize;
    }

    @Override
    public void collectUsedConstants(char ba, int be, UsedConstantsCollector usedConstantsCollector, char bf) {
        int lengthValue = this.locals.length;
        int lengthValue2 = this.stackItems.length;

        int bd;
        for (bd = 0; bd < lengthValue; bd++) {
            VerificationTypeInfo verificationTypeInfo1 = this.locals[bd];
            UsedConstantsCollector usedConstantsCollector1 = usedConstantsCollector;
            verificationTypeInfo1.collectUsedConstants('\u0000', 1103830477, usedConstantsCollector1, '鞍');
        }

        if (ba >= 0) {
            bd = 0;
        }

        while (bd < lengthValue2) {
            VerificationTypeInfo verificationTypeInfo = this.stackItems[bd];
            UsedConstantsCollector usedConstantsCollector2 = usedConstantsCollector;
            verificationTypeInfo.collectUsedConstants('\u0000', 1103830477, usedConstantsCollector2, '鞍');
            bd++;
        }
    }

    @Override
    public void writeRemapped(DataOutputStream dataOutputStream, Map map1) throws IOException {
        int lengthValue = this.locals.length;
        int lengthValue2 = this.stackItems.length;
        dataOutputStream.writeByte(255);
        dataOutputStream.writeShort(this.offsetDelta);
        dataOutputStream.writeShort(lengthValue);

        for (int localIndex = 0; localIndex < lengthValue; localIndex++) {
            this.locals[localIndex].writeRemapped(dataOutputStream, map1);
        }

        dataOutputStream.writeShort(lengthValue2);

        for (int stackItemIndex = 0; stackItemIndex < lengthValue2; stackItemIndex++) {
            this.stackItems[stackItemIndex].writeRemapped(dataOutputStream, map1);
        }
    }

    public FullStackMapFrame(
            StackMapTableAttribute stackMapTableAttribute,
            int offsetDelta,
            LabelInstruction labelInstruction,
            VerificationTypeInfo[] verificationTypeInfos,
            VerificationTypeInfo[] verificationTypeInfos1
    ) {
        super(stackMapTableAttribute);
        this.label = labelInstruction;
        this.offsetDelta = offsetDelta;
        this.offset = labelInstruction.getOffset();
        this.stackItems = verificationTypeInfos;
        this.locals = verificationTypeInfos1;
    }

    public FullStackMapFrame(
            ClassFileComponent classFileComponent,
            ClassFileInputStream classFileInputStream,
            ListMultimap listMultimap,
            ListMultimap listMultimap1,
            PrintWriter printWriter,
            MutableInt mutableInt,
            Map map1,
            Map map2
    ) throws IOException {
        super(classFileComponent);
        IntegerCache integerCache1 = IntegerCache.getInstance();
        this.offsetDelta = classFileInputStream.readUnsignedShort();
        int ba = classFileInputStream.readUnsignedShort();
        this.locals = new VerificationTypeInfo[ba];

        for (int localIndex = 0; localIndex < ba; localIndex++) {
            this.locals[localIndex] = VerificationTypeInfo.readTypeInfo(
                    (StackMapTableAttribute) classFileComponent, classFileInputStream, listMultimap, listMultimap1, printWriter, map1, map2, integerCache1
            );
            if (!this.locals[localIndex].isValid()) {
                this.valid = false;
            }
        }

        int bd = classFileInputStream.readUnsignedShort();
        this.stackItems = new VerificationTypeInfo[bd];

        for (int stackItemIndex = 0; stackItemIndex < bd; stackItemIndex++) {
            this.stackItems[stackItemIndex] = VerificationTypeInfo.readTypeInfo(
                    (StackMapTableAttribute) classFileComponent, classFileInputStream, listMultimap, listMultimap1, printWriter, map1, map2, integerCache1
            );
            if (!this.stackItems[stackItemIndex].isValid()) {
                this.valid = false;
            }
        }

        int value = mutableInt.getValue();
        if (value == -1) {
            this.offset = this.offsetDelta;
        } else {
            this.offset = value + 1 + this.offsetDelta;
        }

        mutableInt.setValue(this.offset);
        listMultimap.addValue(integerCache1.valueOf(this.offset), this);
    }

    @Override
    public int getFrameType() {
        return 255;
    }
}

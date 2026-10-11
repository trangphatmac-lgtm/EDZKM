package com.zelix.klassmaster.classfile.attribute;

import com.zelix.klassmaster.classfile.ClassFileComponent;
import com.zelix.klassmaster.classfile.ClassFileInputStream;
import com.zelix.klassmaster.classfile.constpool.ConstantUtf8;
import com.zelix.klassmaster.classfile.constpool.UsedConstantsCollector;
import com.zelix.klassmaster.classfile.insn.InstructionUsageClearFlag;
import com.zelix.klassmaster.classfile.insn.LabelInstruction;
import com.zelix.klassmaster.script.ScriptEnvironment;
import com.zelix.klassmaster.util.ListMultimap;
import com.zelix.klassmaster.util.SetMultiMap;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;

public abstract class AbstractStackMapAttribute extends Attribute {
    public byte[] rawBytes;
    public StackMapFrame[] frames;
    public int frameCount;
    public boolean parsed = true;

    public abstract void writeFramesRemapped(DataOutputStream dataOutputStream, Map map1) throws IOException;

    public AbstractStackMapAttribute(
            ClassFileComponent classFileComponent, int constantPoolIndex, String attributeName, ClassFileInputStream classFileInputStream, ListMultimap listMultimap
    ) throws IOException {
        super(classFileComponent, constantPoolIndex, attributeName, classFileInputStream, listMultimap);
    }

    @Override
    public final ConstantUtf8 getNameConstant() {
        return this.nameConstant;
    }

    public final void setFrames(StackMapFrame[] stackMapFrames) {
        this.frameCount = stackMapFrames.length;
        this.frames = stackMapFrames;
    }

    @Override
    public void collectUsedConstants(char bc, int ba, UsedConstantsCollector usedConstantsCollector, char bd) {
        usedConstantsCollector.markUsed(this.nameConstant, this, this.getParent());
        int frameIndex = 0;

        while (true) {
            if (frameIndex < this.frameCount) {
                this.frames[frameIndex].collectUsedConstants('\u0000', 1103830477, usedConstantsCollector, '鞍');
            } else if (ba >= 0) {
                return;
            }

            frameIndex++;
        }
    }

    @Override
    public final void writeRemapped(DataOutputStream dataOutputStream, Object object, Object object1) throws IOException {
        Map map1 = (Map) object;
        ScriptEnvironment scriptEnvironment1 = (ScriptEnvironment) object1;
        ScriptEnvironment scriptEnvironment2 = scriptEnvironment1;
        Map map2 = map1;
        ScriptEnvironment scriptEnvironment3 = scriptEnvironment2;
        Map map3 = map2;
        DataOutputStream dataOutputStream1 = dataOutputStream;
        super.writeRemapped(dataOutputStream1, map3, scriptEnvironment3);
        if (this.parsed) {
            this.writeFramesRemapped(dataOutputStream, map1);
        } else {
            dataOutputStream.write(this.rawBytes);
        }
    }

    public void removeFramesAtDeadLabels(HashSet hashSet, SetMultiMap setMultiMap) {
        if (this.parsed && hashSet.size() > 0) {
            ArrayList arrayList = new ArrayList(this.frameCount);

            for (int frameIndex = 0; frameIndex < this.frameCount; frameIndex++) {
                LabelInstruction labelInstruction = this.frames[frameIndex].getLabel();
                if (!hashSet.contains(labelInstruction)) {
                    arrayList.add(this.frames[frameIndex]);
                } else {
                    setMultiMap.removeValue(labelInstruction, this.frames[frameIndex]);
                    labelInstruction.clearUsageFlag(InstructionUsageClearFlag.CLEAR_USAGE_STACK_MAP);
                }
            }

            if (arrayList.size() < this.frames.length) {
                StackMapFrame[] stackMapFrames = new StackMapFrame[arrayList.size()];
                this.frames = ((com.zelix.klassmaster.classfile.attribute.StackMapFrame[]) (arrayList.toArray(stackMapFrames)));
                this.frameCount = this.frames.length;
                this.getLength();
            }
        }
    }

    public final void collectLabelReferences(SetMultiMap setMultiMap) {
        for (int frameIndex = 0; frameIndex < this.frameCount; frameIndex++) {
            this.frames[frameIndex].registerLabelTargets(setMultiMap);
        }
    }

    @Override
    public final int getLength() {
        int lengthValue = 2;

        for (int frameIndex = 0; frameIndex < this.frameCount; frameIndex++) {
            lengthValue += this.frames[frameIndex].getFrameSize();
        }

        this.length = lengthValue;
        return lengthValue;
    }

    public int getFrameCount() {
        return this.frameCount;
    }

    public abstract void writeFrames(DataOutputStream dataOutputStream) throws IOException;

    public AbstractStackMapAttribute(ClassFileComponent classFileComponent, ConstantUtf8 constantUtf8) {
        super(classFileComponent, constantUtf8, 0);
    }

    @Override
    public final void write(DataOutputStream dataOutputStream) throws IOException {
        super.write(dataOutputStream);
        if (this.parsed) {
            this.writeFrames(dataOutputStream);
        } else {
            dataOutputStream.write(this.rawBytes);
        }
    }
}

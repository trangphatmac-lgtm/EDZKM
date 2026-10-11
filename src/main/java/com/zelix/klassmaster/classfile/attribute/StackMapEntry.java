package com.zelix.klassmaster.classfile.attribute;

import com.zelix.klassmaster.classfile.ClassFileComponent;
import com.zelix.klassmaster.classfile.ClassFileInputStream;
import com.zelix.klassmaster.classfile.constpool.ConstantPool;
import com.zelix.klassmaster.classfile.constpool.UsedConstantsCollector;
import com.zelix.klassmaster.classfile.insn.LabelInstruction;
import com.zelix.klassmaster.classfile.insn.StackFrameState;
import com.zelix.klassmaster.classfile.insn.VerifierType;
import com.zelix.klassmaster.util.IntegerCache;
import com.zelix.klassmaster.util.ListMultimap;
import com.zelix.klassmaster.util.VisitableNode;
import com.zelix.klassmaster.util.ZkmAssert;
import com.zelix.klassmaster.util.ZkmUtils;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class StackMapEntry extends StackMapFrame {
    public VerificationTypeInfo[] locals;
    public VerificationTypeInfo[] stackItems;

    @Override
    public int getFrameSize() {
        int byteSize = 6;
        for (int localIndex = 0; localIndex < this.locals.length; localIndex++) {
            byteSize += this.locals[localIndex].getByteSize();
        }
        for (int stackIndex = 0; stackIndex < this.stackItems.length; stackIndex++) {
            byteSize += this.stackItems[stackIndex].getByteSize();
        }
        return byteSize;
    }

    @Override
    public String getHolderTypeName(Object object, Object object1, Object object2) {
        return "StackMap";
    }

    
    @Override
    public final void collectUsedConstants(char controlTag, int controlSeed, UsedConstantsCollector collector, char controlMode) {
        VisitableNode[] flowGuardNodes = ClassFileComponent.getFlowGuardNodes();
        int localCount = this.locals.length;
        int localIndex = 0;
        while (localIndex < localCount) {
            if (controlMode > 0) {
                this.locals[localIndex].collectUsedConstants('\u0000', 1103830477, collector, '鞍');
            } else {
                if (this.locals != null) {
                    continue;
                }
                break;
            }

            localIndex++;
            if (flowGuardNodes == null) {
                break;
            }
        }

        int stackSize = this.stackItems.length;
        int stackIndex = 0;
        while (true) {
            if (stackIndex < stackSize) {
                this.stackItems[stackIndex].collectUsedConstants('\u0000', 1103830477, collector, '鞍');
            } else if (controlSeed >= 0) {
                return;
            }
            stackIndex++;
        }
    }

    public StackMapEntry(
            StackMapAttribute stackMapAttribute,
            LabelInstruction labelInstruction,
            StackFrameState stackFrameState,
            ConstantPool constantPool1,
            Set set1,
            List list1,
            boolean bl,
            Map map1,
            Map map2
    ) {
        super(stackMapAttribute);
        this.label = labelInstruction;
        VerifierType[] verifierTypes;
        if (bl) {
            verifierTypes = stackFrameState.getLocalsMaskedByLiveness();
        } else {
            verifierTypes = stackFrameState.getLocals();
        }

        this.locals = VerificationTypeInfo.convertVerifierTypes(stackMapAttribute, verifierTypes, constantPool1, set1, list1, true, map1, map2);
        this.stackItems = VerificationTypeInfo.convertVerifierTypes(stackMapAttribute, stackFrameState.getStack(), constantPool1, set1, list1, false, map1, map2);
        this.valid = true;
    }

    public void writeTo(DataOutputStream dataOutputStream) throws IOException {
        ZkmAssert.assertTrue(this.valid, new String[]{"Invalid StackMap Entry in " + this.getDottedClassName()});
        VerificationTypeInfo[] verificationTypeInfos;
        if (this.label != null) {
            dataOutputStream.writeShort(this.label.getOffset());
            verificationTypeInfos = this.locals;
        } else {
            dataOutputStream.writeShort(this.offset);
            verificationTypeInfos = this.locals;
        }

        int lengthValue = verificationTypeInfos.length;
        dataOutputStream.writeShort(lengthValue);

        for (int localIndexValue = 0; localIndexValue < lengthValue; localIndexValue++) {
            this.locals[localIndexValue].writeTo(dataOutputStream);
        }

        int lengthValue2 = this.stackItems.length;
        dataOutputStream.writeShort(lengthValue2);

        for (int stackItemIndex = 0; stackItemIndex < lengthValue2; stackItemIndex++) {
            this.stackItems[stackItemIndex].writeTo(dataOutputStream);
        }
    }

    public StackMapEntry(
            ClassFileComponent classFileComponent,
            ClassFileInputStream classFileInputStream,
            ListMultimap listMultimap,
            ListMultimap listMultimap1,
            PrintWriter printWriter
    ) throws IOException {
        super(classFileComponent);
        HashMap hashMap = ZkmUtils.createHashMap();
        HashMap hashMap1 = ZkmUtils.createHashMap();
        IntegerCache integerCache1 = IntegerCache.getInstance();
        this.offset = classFileInputStream.readUnsignedShort();
        listMultimap.addValue(integerCache1.valueOf(this.offset), this);
        int ba = classFileInputStream.readUnsignedShort();
        this.locals = new VerificationTypeInfo[ba];

        for (int localIndexValue = 0; localIndexValue < ba; localIndexValue++) {
            this.locals[localIndexValue] = VerificationTypeInfo.readTypeInfo(
                    (AbstractStackMapAttribute) classFileComponent, classFileInputStream, listMultimap, listMultimap1, printWriter, hashMap, hashMap1, integerCache1
            );
            if (!this.locals[localIndexValue].isValid()) {
                this.valid = false;
            }
        }

        int bd = classFileInputStream.readUnsignedShort();
        this.stackItems = new VerificationTypeInfo[bd];

        for (int stackItemIndex = 0; stackItemIndex < bd; stackItemIndex++) {
            this.stackItems[stackItemIndex] = VerificationTypeInfo.readTypeInfo(
                    (AbstractStackMapAttribute) classFileComponent, classFileInputStream, listMultimap, listMultimap1, printWriter, hashMap, hashMap1, integerCache1
            );
            if (!this.stackItems[stackItemIndex].isValid()) {
                this.valid = false;
            }
        }
    }

    public void writeRemapped(DataOutputStream dataOutputStream, Map map1) throws IOException {
        ZkmAssert.assertTrue(this.valid, new String[]{"Invalid StackMap Entry in " + this.getDottedClassName()});
        VerificationTypeInfo[] verificationTypeInfos;
        if (this.label != null) {
            dataOutputStream.writeShort(this.label.getOffset());
            verificationTypeInfos = this.locals;
        } else {
            dataOutputStream.writeShort(this.offset);
            verificationTypeInfos = this.locals;
        }

        int lengthValue = verificationTypeInfos.length;
        dataOutputStream.writeShort(lengthValue);

        for (int localIndexValue = 0; localIndexValue < lengthValue; localIndexValue++) {
            this.locals[localIndexValue].writeRemapped(dataOutputStream, map1);
        }

        int lengthValue2 = this.stackItems.length;
        dataOutputStream.writeShort(lengthValue2);

        for (int stackItemIndex = 0; stackItemIndex < lengthValue2; stackItemIndex++) {
            this.stackItems[stackItemIndex].writeRemapped(dataOutputStream, map1);
        }
    }

    static {
        try {
            staticInit();
        } catch (Exception exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private static void staticInit() {
    }
}

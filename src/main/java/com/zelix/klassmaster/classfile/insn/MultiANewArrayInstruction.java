package com.zelix.klassmaster.classfile.insn;

import com.zelix.klassmaster.classfile.ClassFileInputStream;
import com.zelix.klassmaster.classfile.constpool.ConstantPoolProvider;
import com.zelix.klassmaster.classfile.constpool.ResolvedClassConstant;
import com.zelix.klassmaster.exceptions.ZkmException;
import com.zelix.klassmaster.util.ListMultimap;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

public class MultiANewArrayInstruction extends ConstantRefInstruction {
    public int dimensions;

    @Override
    public boolean consumesStackSlot(int ba, int bb) {
        return ba >= bb - 1;
    }

    @Override
    public int getLength() {
        return 4;
    }

    public MultiANewArrayInstruction(
            ClassFileInputStream classFileInputStream,
            ConstantPoolProvider constantPoolProvider,
            ListMultimap listMultimap,
            ListMultimap listMultimap1,
            ListMultimap listMultimap2,
            ListMultimap listMultimap3,
            ListMultimap listMultimap4
    ) throws IOException {
        super(197, classFileInputStream, constantPoolProvider, listMultimap, listMultimap1, listMultimap2, listMultimap3, listMultimap4);
        this.dimensions = classFileInputStream.read();
    }

    @Override
    public void writeRemapped(DataOutputStream dataOutputStream, Map map1) throws IOException {
        super.writeRemapped(dataOutputStream, map1);
        dataOutputStream.writeByte(this.dimensions);
    }

    @Override
    public String toAssembly() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(this.getMnemonic());
        stringBuilder.append(' ');
        stringBuilder.append(this.constantEntry.getValueString());
        stringBuilder.append(' ');
        stringBuilder.append(this.dimensions);
        return stringBuilder.toString();
    }

    @Override
    public void printDisassembly(PrintWriter printWriter, StringBuilder stringBuilder) throws UnknownOpcodeException {
        StringBuilder stringBuilder1 = new StringBuilder(100);
        String mnemonic = this.getMnemonic();
        stringBuilder1.append(mnemonic + " " + this.constantEntry.getIndex() + " " + this.dimensions);
        String descriptionComment = Instruction.getDescriptionComment(this.opcode);
        descriptionComment = Instruction.formatDescription(descriptionComment, this.constantEntry.getValueString());
        descriptionComment = Instruction.formatDescription(descriptionComment, String.valueOf(this.dimensions));
        if (descriptionComment.length() > 0) {
            stringBuilder1.append("\t" + descriptionComment);
        }

        printWriter.println(stringBuilder.toString() + stringBuilder.toString() + stringBuilder1);
    }

    @Override
    public StackFrameState computeFrameAfter(StackFrameState stackFrameState, Object object, Object object1, Object object2) throws ZkmException, IOException {
        VerifierType[] verifierTypes = stackFrameState.getStack();
        VerifierType[] verifierTypes1 = stackFrameState.getLocals();
        VerifierType[] verifierTypes2 = VerifierType.createArray(verifierTypes.length - this.dimensions + 1);
        int lengthValue = verifierTypes2.length;
        System.arraycopy(verifierTypes, 0, verifierTypes2, 0, lengthValue - 1);
        String typeDescriptor = ((ResolvedClassConstant) this.constantEntry).getTypeDescriptor();
        verifierTypes2[lengthValue - 1] = VerifierType.forDescriptor(typeDescriptor);
        return new StackFrameState(verifierTypes2, verifierTypes1, stackFrameState.getSubroutineLocals(), stackFrameState.getHeldMonitors());
    }

    @Override
    public void writeTo(DataOutputStream dataOutputStream) throws IOException {
        super.writeTo(dataOutputStream);
        dataOutputStream.writeByte(this.dimensions);
    }

    @Override
    public boolean requiresTypedValueAt(Object object, Object object2, Object object1) {
        int bb = (Integer) object;
        int ba = (Integer) object1;
        return bb >= ba - 1;
    }
}

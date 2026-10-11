package com.zelix.klassmaster.classfile.insn;

import com.zelix.klassmaster.classfile.ClassFileInputStream;
import com.zelix.klassmaster.classfile.MethodInfo;
import com.zelix.klassmaster.classfile.constpool.ClassConstantReplaceable;
import com.zelix.klassmaster.classfile.constpool.ConstantInteger;
import com.zelix.klassmaster.classfile.constpool.ConstantIntegerReplacer;
import com.zelix.klassmaster.classfile.constpool.ConstantLong;
import com.zelix.klassmaster.classfile.constpool.ConstantPoolEntry;
import com.zelix.klassmaster.classfile.constpool.ConstantPoolProvider;
import com.zelix.klassmaster.classfile.constpool.InvokeDynamicConstantReplaceable;
import com.zelix.klassmaster.classfile.constpool.LoadableConstant;
import com.zelix.klassmaster.classfile.constpool.LongConstantReplaceable;
import com.zelix.klassmaster.classfile.constpool.MemberRefReplaceable;
import com.zelix.klassmaster.classfile.constpool.MethodHandleRefKind;
import com.zelix.klassmaster.classfile.constpool.ResolvedClassConstant;
import com.zelix.klassmaster.classfile.constpool.ResolvedConstantDynamic;
import com.zelix.klassmaster.classfile.constpool.ResolvedFieldRef;
import com.zelix.klassmaster.classfile.constpool.ResolvedInvokeDynamic;
import com.zelix.klassmaster.classfile.constpool.ResolvedMemberRef;
import com.zelix.klassmaster.classfile.constpool.ResolvedMethodHandleConstant;
import com.zelix.klassmaster.classfile.constpool.ResolvedMethodRef;
import com.zelix.klassmaster.classfile.constpool.ResolvedMethodType;
import com.zelix.klassmaster.classfile.constpool.ResolvedNameAndType;
import com.zelix.klassmaster.classfile.constpool.ResolvedStringConstant;
import com.zelix.klassmaster.classfile.constpool.StringConstantReplaceable;
import com.zelix.klassmaster.classfile.hierarchy.CommonSuperTypeResolver;
import com.zelix.klassmaster.exceptions.ClassFileLoadException;
import com.zelix.klassmaster.exceptions.StackAnalysisException;
import com.zelix.klassmaster.exceptions.ZkmException;
import com.zelix.klassmaster.util.ListMultimap;
import com.zelix.klassmaster.util.MultiMapTable;
import com.zelix.klassmaster.util.RankedValue;
import com.zelix.klassmaster.util.TruncatedStringDisplay;
import com.zelix.klassmaster.util.ZkmAssert;
import com.zelix.klassmaster.util.ZkmUtils;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ConstantRefInstruction
        extends Instruction
        implements ConstantPoolOperand,
        StringConstantReplaceable,
        ClassConstantReplaceable,
        MemberRefReplaceable,
        ConstantIntegerReplacer,
        LongConstantReplaceable,
        InvokeDynamicConstantReplaceable {
    public ConstantPoolEntry constantEntry;

    public ConstantRefInstruction(int opcodeArgument, ConstantPoolEntry constantPoolEntry) {
        super(opcodeArgument);
        this.constantEntry = constantPoolEntry;
    }

    @Override
    public final boolean isMethodInvoke() {
        switch (this.opcode) {
            case 182:
            case 183:
            case 184:
            case 185:
                return true;
            default:
                return false;
        }
    }

    @Override
    public final boolean pushesWideValue() {
        switch (this.opcode) {
            case 19:
            case 179:
            case 181:
            case 187:
            case 189:
            case 192:
            case 193:
            case 197:
                return false;
            case 20:
                return true;
            case 178:
            case 180:
                String stackFieldType = ((ResolvedFieldRef) this.constantEntry).getStackFieldType();
                return stackFieldType != null && (stackFieldType.equals("J") || stackFieldType.equals("D"));
            case 182:
            case 183:
            case 184:
            case 185:
                String stackReturnType = ((ResolvedMethodRef) this.constantEntry).getStackReturnType();
                return stackReturnType != null && (stackReturnType.equals("J") || stackReturnType.equals("D"));
            case 186:
                String stackReturnTypeValue = ((ResolvedInvokeDynamic) this.constantEntry).getStackReturnType();
                return stackReturnTypeValue != null && (stackReturnTypeValue.equals("J") || stackReturnTypeValue.equals("D"));
            default:
                ZkmAssert.assertTrue(false, new String[]{"invalid opcode " + this.opcode});
                return false;
        }
    }

    @Override
    public final void replaceMemberRef(ResolvedMemberRef resolvedMemberRef, ResolvedMemberRef resolvedMemberRef1) {
        if (this.constantEntry == resolvedMemberRef) {
            this.constantEntry = resolvedMemberRef1;
        }
    }

    @Override
    public void writeTo(DataOutputStream dataOutputStream) throws IOException {
        super.writeTo(dataOutputStream);
        dataOutputStream.writeShort(this.constantEntry.getIndex());
    }

    @Override
    public String toAssembly() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(this.getMnemonic());
        stringBuilder.append(' ');
        stringBuilder.append(this.constantEntry.getAbbreviatedValue());
        return stringBuilder.toString();
    }

    @Override
    public Instruction adjustLdcWidth(Map map1) {
        if (this.opcode == 19) {
            ConstantPoolEntry constantPoolEntry1 = (ConstantPoolEntry) ZkmUtils.mapOrSelf(this.constantEntry, map1);
            ConstantPoolEntry constantPoolEntry;
            if (constantPoolEntry1 != null) {
                constantPoolEntry = constantPoolEntry1;
            } else {
                constantPoolEntry = this.constantEntry;
            }

            return constantPoolEntry.getIndex() <= 255 ? new LdcInstruction(this.constantEntry) : null;
        } else {
            return null;
        }
    }

    @Override
    public boolean isIntConstantLdc() {
        return this.opcode == 19 && this.constantEntry instanceof ConstantInteger;
    }

    @Override
    public boolean isStringConstantLoad() {
        return this.opcode == 19 && this.constantEntry instanceof ResolvedStringConstant;
    }

    @Override
    public boolean isFieldStore() {
        switch (this.opcode) {
            case 179:
            case 181:
                return true;
            default:
                return false;
        }
    }

    @Override
    public StackFrameState computeFrameAfter(StackFrameState stackFrameState, Object object, Object object1, Object object2) throws ZkmException, IOException {
        String text = (String) object2;
        boolean bl = (Boolean) object;
        CommonSuperTypeResolver commonSuperTypeResolver1 = (CommonSuperTypeResolver) object1;
        new TruncatedStringDisplay(text);
        VerifierType[] verifierTypes = stackFrameState.getLocals();
        VerifierType[] verifierTypes1 = stackFrameState.getStack();
        int lengthValue = verifierTypes1.length;
        SubroutineLocalsBitSet subroutineLocalsBitSet = stackFrameState.getSubroutineLocals();
        Set set1 = stackFrameState.getHeldMonitors();
        switch (this.opcode) {
            case 19:
                VerifierType[] verifierTypes11 = VerifierType.createArray(lengthValue + 1);
                System.arraycopy(verifierTypes1, 0, verifierTypes11, 0, lengthValue);
                if (this.constantEntry instanceof LoadableConstant) {
                    String typeName = ((LoadableConstant) this.constantEntry).getTypeName();
                    if (typeName.equals("integer")) {
                        verifierTypes11[lengthValue] = VerifierType.INT;
                    } else if (typeName.equals("string")) {
                        verifierTypes11[lengthValue] = VerifierType.STRING;
                    } else if (typeName.equals("float")) {
                        verifierTypes11[lengthValue] = VerifierType.FLOAT;
                    }
                } else if (this.constantEntry instanceof ResolvedClassConstant) {
                    verifierTypes11[lengthValue] = VerifierType.CLASS;
                } else if (this.constantEntry instanceof ResolvedMethodType) {
                    verifierTypes11 = VerifierType.createArray(lengthValue + 1);
                    System.arraycopy(verifierTypes1, 0, verifierTypes11, 0, lengthValue);
                    verifierTypes11[lengthValue] = VerifierType.forDescriptor("Ljava/lang/invoke/MethodType;");
                } else if (this.constantEntry instanceof ResolvedMethodHandleConstant) {
                    verifierTypes11 = VerifierType.createArray(lengthValue + 1);
                    System.arraycopy(verifierTypes1, 0, verifierTypes11, 0, lengthValue);
                    verifierTypes11[lengthValue] = VerifierType.forDescriptor("Ljava/lang/invoke/MethodHandle;");
                } else if (this.constantEntry instanceof ResolvedConstantDynamic) {
                    verifierTypes11 = VerifierType.createArray(lengthValue + 1);
                    System.arraycopy(verifierTypes1, 0, verifierTypes11, 0, lengthValue);
                    ResolvedConstantDynamic resolvedConstantDynamic = (ResolvedConstantDynamic) this.constantEntry;
                    String stackReturnType = resolvedConstantDynamic.getStackReturnType();
                    verifierTypes11[lengthValue] = VerifierType.forDescriptor(stackReturnType);
                }

                return new StackFrameState(verifierTypes11, verifierTypes, subroutineLocalsBitSet, set1);
            case 20:
                VerifierType[] verifierTypes10 = VerifierType.createArray(lengthValue + 1);
                System.arraycopy(verifierTypes1, 0, verifierTypes10, 0, lengthValue);
                String typeNameValue = ((LoadableConstant) this.constantEntry).getTypeName();
                if (typeNameValue.equals("long")) {
                    verifierTypes10[lengthValue] = VerifierType.LONG;
                } else if (typeNameValue.equals("double")) {
                    verifierTypes10[lengthValue] = VerifierType.DOUBLE;
                }

                return new StackFrameState(verifierTypes10, verifierTypes, subroutineLocalsBitSet, set1);
            case 178:
                VerifierType[] verifierTypes9 = VerifierType.createArray(lengthValue + 1);
                System.arraycopy(verifierTypes1, 0, verifierTypes9, 0, lengthValue);
                ResolvedFieldRef resolvedFieldRef = (ResolvedFieldRef) this.constantEntry;
                verifierTypes9[lengthValue] = VerifierType.forDescriptor(resolvedFieldRef.getStackFieldType());
                return new StackFrameState(verifierTypes9, verifierTypes, subroutineLocalsBitSet, set1);
            case 179:
                VerifierType[] verifierTypes8 = VerifierType.createArray(lengthValue - 1);
                System.arraycopy(verifierTypes1, 0, verifierTypes8, 0, lengthValue - 1);
                return new StackFrameState(verifierTypes8, verifierTypes, subroutineLocalsBitSet, set1);
            case 180:
                VerifierType[] verifierTypes7 = VerifierType.createArray(lengthValue);
                System.arraycopy(verifierTypes1, 0, verifierTypes7, 0, lengthValue - 1);
                ResolvedFieldRef resolvedFieldRef1 = (ResolvedFieldRef) this.constantEntry;
                verifierTypes7[lengthValue - 1] = VerifierType.forDescriptor(resolvedFieldRef1.getStackFieldType());
                return new StackFrameState(verifierTypes7, verifierTypes, subroutineLocalsBitSet, set1);
            case 181:
                VerifierType[] verifierTypes6 = VerifierType.createArray(lengthValue - 2);
                System.arraycopy(verifierTypes1, 0, verifierTypes6, 0, lengthValue - 2);
                if (bl) {
                    ResolvedFieldRef resolvedFieldRef2 = (ResolvedFieldRef) this.constantEntry;
                    if (!StackFrameState.isAssignable(
                            verifierTypes1[lengthValue - 1], VerifierType.forDescriptor(resolvedFieldRef2.getStackFieldType()), commonSuperTypeResolver1, text
                    )) {
                    }
                }

                return new StackFrameState(verifierTypes6, verifierTypes, subroutineLocalsBitSet, set1);
            case 182:
            case 185:
                return this.simulateInvoke(verifierTypes1, lengthValue, verifierTypes, subroutineLocalsBitSet, set1, true, false, bl, commonSuperTypeResolver1, text);
            case 183:
                return this.simulateInvoke(verifierTypes1, lengthValue, verifierTypes, subroutineLocalsBitSet, set1, true, true, bl, commonSuperTypeResolver1, text);
            case 184:
                return this.simulateInvoke(verifierTypes1, lengthValue, verifierTypes, subroutineLocalsBitSet, set1, false, false, bl, commonSuperTypeResolver1, text);
            case 186:
                return this.simulateInvokeDynamic(verifierTypes1, lengthValue, verifierTypes, subroutineLocalsBitSet, set1, bl, commonSuperTypeResolver1, text);
            case 187:
                VerifierType[] verifierTypes5 = VerifierType.createArray(lengthValue + 1);
                System.arraycopy(verifierTypes1, 0, verifierTypes5, 0, lengthValue);
                ResolvedClassConstant resolvedClassConstant = (ResolvedClassConstant) this.constantEntry;
                verifierTypes5[lengthValue] = VerifierType.resolveType(resolvedClassConstant.getTypeDescriptor(), false, (TypeInstruction) this);
                return new StackFrameState(verifierTypes5, verifierTypes, subroutineLocalsBitSet, set1);
            case 189:
                VerifierType[] verifierTypes4 = VerifierType.createArray(lengthValue);
                System.arraycopy(verifierTypes1, 0, verifierTypes4, 0, lengthValue - 1);
                ResolvedClassConstant resolvedClassConstant1 = (ResolvedClassConstant) this.constantEntry;
                verifierTypes4[lengthValue - 1] = VerifierType.forDescriptor("[" + resolvedClassConstant1.getTypeDescriptor());
                return new StackFrameState(verifierTypes4, verifierTypes, subroutineLocalsBitSet, set1);
            case 192:
                VerifierType[] verifierTypes3 = VerifierType.createArray(lengthValue);
                System.arraycopy(verifierTypes1, 0, verifierTypes3, 0, lengthValue - 1);
                ResolvedClassConstant resolvedClassConstant2 = (ResolvedClassConstant) this.constantEntry;
                verifierTypes3[lengthValue - 1] = VerifierType.forDescriptor(resolvedClassConstant2.getTypeDescriptor());
                return new StackFrameState(verifierTypes3, verifierTypes, subroutineLocalsBitSet, set1);
            case 193:
                VerifierType[] verifierTypes2 = VerifierType.createArray(lengthValue);
                System.arraycopy(verifierTypes1, 0, verifierTypes2, 0, lengthValue - 1);
                verifierTypes2[lengthValue - 1] = VerifierType.INT;
                return new StackFrameState(verifierTypes2, verifierTypes, subroutineLocalsBitSet, set1);
            default:
                ZkmAssert.assertTrue(false, new String[]{"invalid opcode " + this.opcode});
                return null;
        }
    }

    @Override
    public final void replaceIntegerConstant(ConstantInteger constantInteger, ConstantInteger constantInteger1) {
        if (this.constantEntry == constantInteger) {
            this.constantEntry = constantInteger1;
        }
    }

    @Override
    public void replaceLongConstant(ConstantLong constantLong, ConstantLong constantLong1) {
        if (this.constantEntry == constantLong) {
            this.constantEntry = constantLong1;
        }
    }

    @Override
    public final boolean pushesValue() {
        switch (this.opcode) {
            case 19:
            case 20:
            case 178:
            case 180:
            case 187:
            case 189:
            case 193:
            case 197:
                return true;
            case 179:
            case 181:
            case 192:
                return false;
            case 182:
            case 183:
            case 184:
            case 185:
                return ((ResolvedMethodRef) this.constantEntry).getStackReturnType() != null;
            case 186:
                return ((ResolvedInvokeDynamic) this.constantEntry).getStackReturnType() != null;
            default:
                ZkmAssert.assertTrue(false, new String[]{"invalid opcode " + this.opcode});
                return false;
        }
    }

    @Override
    public final boolean canFallThrough() {
        return true;
    }

    @Override
    public final void replaceClassConstant(ResolvedClassConstant resolvedClassConstant, ResolvedClassConstant resolvedClassConstant1) {
        if (this.constantEntry == resolvedClassConstant) {
            this.constantEntry = resolvedClassConstant1;
        }
    }

    @Override
    public int getStackDelta() {
        switch (this.opcode) {
            case 19:
                return 1;
            case 20:
                return 2;
            case 178:
                return ConstantPoolEntry.isWideType(((ResolvedFieldRef) this.constantEntry).getStackFieldType()) ? 2 : 1;
            case 179:
                return ConstantPoolEntry.isWideType(((ResolvedFieldRef) this.constantEntry).getStackFieldType()) ? -2 : -1;
            case 180:
                return (ConstantPoolEntry.isWideType(((ResolvedFieldRef) this.constantEntry).getStackFieldType()) ? 2 : 1) - 1;
            case 181:
                return (ConstantPoolEntry.isWideType(((ResolvedFieldRef) this.constantEntry).getStackFieldType()) ? -2 : -1) - 1;
            case 182:
            case 183:
            case 184:
            case 185:
                return this.getInvokeStackDelta();
            case 186:
                return this.getInvokeDynamicStackDelta();
            case 187:
                return 1;
            case 189:
            case 192:
            case 193:
                return 0;
            default:
                return 0;
        }
    }

    @Override
    public boolean consumesStackSlot(int ba, int bb) {
        switch (this.opcode) {
            case 19:
            case 20:
            case 178:
            case 187:
                return false;
            case 179:
            case 181:
                return ba >= bb;
            case 180:
            case 189:
            case 192:
            case 193:
                return ba >= bb - 1;
            case 182:
            case 183:
            case 184:
            case 185:
                return ba >= bb - ((ResolvedMethodRef) this.constantEntry).getReturnValueCount();
            case 186:
                return ba >= bb - ((ResolvedInvokeDynamic) this.constantEntry).getResultValueCount();
            default:
                ZkmAssert.assertTrue(false, new String[]{"invalid opcode " + this.opcode});
                return false;
        }
    }

    @Override
    public boolean collectIntConstant(Object object, Object object1, Object object2, Object object3) {
        MethodInfo methodInfo1 = (MethodInfo) object2;
        MultiMapTable multiMapTable = (MultiMapTable) object;
        if (this.opcode == 19 && this.constantEntry instanceof ConstantInteger) {
            ConstantInteger constantInteger = (ConstantInteger) this.constantEntry;
            if (constantInteger.isInProgramPool() && !((Set) object1).contains(constantInteger)) {
                multiMapTable.addEntry(methodInfo1.getOwnerProgramClass(), methodInfo1, constantInteger, new RankedValue((Integer) object3, this));
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    @Override
    public int getLength() {
        return 3;
    }

    @Override
    public final void replaceInvokeDynamic(ResolvedInvokeDynamic resolvedInvokeDynamic, ResolvedInvokeDynamic resolvedInvokeDynamic1) {
        if (this.constantEntry == resolvedInvokeDynamic) {
            this.constantEntry = resolvedInvokeDynamic1;
        }
    }

    @Override
    public ConstantPoolEntry getConstantPoolEntry() {
        return this.constantEntry;
    }

    public ConstantRefInstruction(
            int opcodeArgument,
            ClassFileInputStream classFileInputStream,
            ConstantPoolProvider constantPoolProvider,
            ListMultimap listMultimap,
            ListMultimap listMultimap1,
            ListMultimap listMultimap2,
            ListMultimap listMultimap3,
            ListMultimap listMultimap4
    ) throws IOException {
        super(opcodeArgument);
        int bb = classFileInputStream.readUnsignedShort();
        this.constantEntry = constantPoolProvider.getConstantPoolEntry(bb);
        switch (LdcConstantSwitchMap.CONSTANT_TAG_SWITCH_MAP[this.constantEntry.getTag().ordinal()]) {
            case 1:
                listMultimap3.addValue((ResolvedClassConstant) this.constantEntry, this);
                break;
            case 2:
                listMultimap.addValue((ResolvedStringConstant) this.constantEntry, this);
                break;
            case 3:
                listMultimap1.addValue((ConstantInteger) this.constantEntry, this);
                break;
            case 4:
                listMultimap2.addValue((ConstantLong) this.constantEntry, this);
                break;
            case 5:
            case 6:
            case 7:
                listMultimap4.addValue((ResolvedMemberRef) this.constantEntry, this);
        }
    }

    @Override
    public boolean collectLongConstant(Object object, Object object1, Object object2, Object object3) {
        MethodInfo methodInfo1 = (MethodInfo) object2;
        MultiMapTable multiMapTable = (MultiMapTable) object;
        if (this.opcode == 20 && this.constantEntry instanceof ConstantLong) {
            ConstantLong constantLong = (ConstantLong) this.constantEntry;
            if (constantLong.isInProgramPool() && !((Set) object1).contains(constantLong)) {
                multiMapTable.addEntry(methodInfo1.getOwnerProgramClass(), methodInfo1, constantLong, new RankedValue((Integer) object3, this));
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    @Override
    public boolean isFieldAccess() {
        switch (this.opcode) {
            case 178:
            case 179:
            case 180:
            case 181:
                return true;
            default:
                return false;
        }
    }

    @Override
    public final void replaceStringConstant(ResolvedStringConstant resolvedStringConstant, ResolvedStringConstant resolvedStringConstant1) {
        if (this.constantEntry == resolvedStringConstant) {
            this.constantEntry = resolvedStringConstant1;
        }
    }

    public int getInvokeDynamicStackDelta() {
        ResolvedNameAndType resolvedNameAndType = ((ResolvedInvokeDynamic) this.constantEntry).getNameAndType();
        String stackReturnType = ConstantPoolEntry.getStackReturnType(resolvedNameAndType);
        int invokeDynamicStackDelta = 0 + (stackReturnType == null ? 0 : (ConstantPoolEntry.isWideType(stackReturnType) ? 2 : 1));
        Iterator iterator = ConstantPoolEntry.getStackParameterTypes(resolvedNameAndType).iterator();

        while (iterator.hasNext()) {
            String string1 = (String) iterator.next();
            invokeDynamicStackDelta += ConstantPoolEntry.isWideType(string1) ? -2 : -1;
        }

        return invokeDynamicStackDelta;
    }

    @Override
    public boolean requiresTypedValueAt(Object object, Object object1, Object object2) {
        int ba = (Integer) object;
        int bb = (Integer) object2;
        switch (this.opcode) {
            case 19:
            case 20:
            case 178:
            case 187:
                return false;
            case 179:
            case 181:
                return ba >= bb;
            case 180:
            case 189:
                return ba >= bb - 1;
            case 182:
            case 183:
            case 184:
            case 185:
            case 186:
                int bc;
                if (this.opcode == 186) {
                    ResolvedInvokeDynamic resolvedInvokeDynamic = (ResolvedInvokeDynamic) this.constantEntry;
                    bc = resolvedInvokeDynamic.getResultValueCount();
                } else {
                    ResolvedMethodRef resolvedMethodRef = (ResolvedMethodRef) this.constantEntry;
                    bc = resolvedMethodRef.getReturnValueCount();
                }

                if (ba >= bb - bc) {
                    return !((VerifierType) object1).getDescriptor().equals("Ljava/lang/Object;");
                }

                return false;
            case 192:
            case 193:
                return false;
            default:
                ZkmAssert.assertTrue(false, new String[]{"invalid opcode " + this.opcode});
                return false;
        }
    }

    @Override
    public void printDisassembly(PrintWriter printWriter, StringBuilder stringBuilder) throws UnknownOpcodeException {
        StringBuilder stringBuilder1 = new StringBuilder(100);
        stringBuilder1.append(stringBuilder);
        stringBuilder1.append(stringBuilder);
        stringBuilder1.append(this.getMnemonic());
        stringBuilder1.append(' ');
        stringBuilder1.append(this.constantEntry.getAbbreviatedValue());
        String descriptionComment = this.getDescriptionComment();
        descriptionComment = Instruction.formatDescription(descriptionComment, this.constantEntry.getAbbreviatedValue());
        if (descriptionComment.length() > 0) {
            stringBuilder1.append('\t');
            stringBuilder1.append(descriptionComment);
        }

        printWriter.println(stringBuilder1);
    }

    public StackFrameState simulateInvokeDynamic(
            VerifierType[] verifierTypes,
            int ba,
            VerifierType[] verifierTypes1,
            SubroutineLocalsBitSet subroutineLocalsBitSet,
            Set set1,
            boolean bl,
            CommonSuperTypeResolver commonSuperTypeResolver1,
            String text
    ) throws ZkmException, IOException {
        TruncatedStringDisplay truncatedStringDisplay = new TruncatedStringDisplay(text);
        ResolvedInvokeDynamic resolvedInvokeDynamic = (ResolvedInvokeDynamic) this.constantEntry;
        ResolvedMethodHandleConstant resolvedMethodHandleConstant = resolvedInvokeDynamic.getBootstrapMethodHandle();
        if (!resolvedMethodHandleConstant.getRefKind().equals(MethodHandleRefKind.REF_INVOKE_STATIC)) {
            throw new StackAnalysisException(this.getMnemonic() + " " + resolvedMethodHandleConstant.getRefKind().toString() + " (A)");
        }

        resolvedInvokeDynamic.getBootstrapArguments();
        ResolvedNameAndType resolvedNameAndType = resolvedInvokeDynamic.getNameAndType();
        List list1 = ConstantPoolEntry.getStackParameterTypes(resolvedNameAndType);
        int list1Size = list1.size();
        String stackReturnType = ConstantPoolEntry.getStackReturnType(resolvedNameAndType);
        int bc = stackReturnType == null ? 0 : 1;
        if (bl) {
            for (int elementIndex = 0; elementIndex < list1Size; elementIndex++) {
                VerifierType verifierType = verifierTypes[ba - list1Size + elementIndex];
                VerifierType verifierType1 = VerifierType.forDescriptor((String) list1.get(elementIndex));

                try {
                    if (!StackFrameState.isAssignable(verifierType, verifierType1, commonSuperTypeResolver1, text)) {
                        throw new StackAnalysisException(
                                "Type mismatch at dynamic method invocation : "
                                        + verifierType.getDescriptor()
                                        + " not assignment compatible with "
                                        + verifierType1.getDescriptor()
                                        + " :  parameter "
                                        + elementIndex
                                        + " calling "
                                        + resolvedNameAndType.getDisplayString()
                                        + " while "
                                        + truncatedStringDisplay
                        );
                    }
                } catch (ClassFileLoadException classFileLoadException) {
                }
            }
        }

        int be = ba - list1Size + bc;
        if (be - bc < 0) {
            throw new StackAnalysisException("Stack underflow in dynamic method invocation : " + be + " while " + truncatedStringDisplay);
        }

        VerifierType[] verifierTypes2 = VerifierType.createArray(be);
        System.arraycopy(verifierTypes, 0, verifierTypes2, 0, be - bc);
        if (bc == 1) {
            verifierTypes2[verifierTypes2.length - 1] = VerifierType.forDescriptor(stackReturnType);
        }

        return new StackFrameState(verifierTypes2, verifierTypes1, subroutineLocalsBitSet, set1);
    }

    private StackFrameState simulateInvoke(
            VerifierType[] verifierTypes,
            int ba,
            VerifierType[] verifierTypes1,
            SubroutineLocalsBitSet subroutineLocalsBitSet,
            Set set1,
            boolean bl,
            boolean bl1,
            boolean bl2,
            CommonSuperTypeResolver commonSuperTypeResolver1,
            String text
    ) throws ZkmException, IOException {
        TruncatedStringDisplay truncatedStringDisplay = new TruncatedStringDisplay(text);
        ResolvedMethodRef resolvedMethodRef = (ResolvedMethodRef) this.constantEntry;
        List list1 = resolvedMethodRef.getStackParameterTypes();
        int list1Size = list1.size();
        String stackReturnType = resolvedMethodRef.getStackReturnType();
        int bc = stackReturnType == null ? 0 : 1;
        if (bl2) {
            for (int elementIndex = 0; elementIndex < list1Size; elementIndex++) {
                VerifierType verifierType = verifierTypes[ba - list1Size + elementIndex];
                VerifierType verifierType1 = VerifierType.forDescriptor((String) list1.get(elementIndex));

                try {
                    if (!StackFrameState.isAssignable(verifierType, verifierType1, commonSuperTypeResolver1, text)) {
                        throw new StackAnalysisException(
                                "Type mismatch at method invocation : "
                                        + verifierType.getDescriptor()
                                        + " not assignment compatible with "
                                        + verifierType1.getDescriptor()
                                        + " :  parameter "
                                        + elementIndex
                                        + " calling "
                                        + resolvedMethodRef.getDisplayString()
                                        + " in class "
                                        + resolvedMethodRef.getOwnerFilePath()
                                        + " while "
                                        + truncatedStringDisplay
                        );
                    }
                } catch (ClassFileLoadException classFileLoadException) {
                }
            }
        }

        int bg = ba - list1Size - (bl ? 1 : 0) + bc;
        if (bg - bc < 0) {
            throw new StackAnalysisException("Stack underflow : " + bg + " in class " + resolvedMethodRef.getOwnerFilePath() + " while " + truncatedStringDisplay);
        }

        VerifierType[] verifierTypes3 = VerifierType.createArray(bg);
        System.arraycopy(verifierTypes, 0, verifierTypes3, 0, bg - bc);
        if (bc == 1) {
            verifierTypes3[verifierTypes3.length - 1] = VerifierType.forDescriptor(stackReturnType);
        }

        if (bl1 && resolvedMethodRef.isConstructor()) {
            VerifierType verifierType3 = verifierTypes[ba - list1Size - 1];
            VerifierType verifierType2 = verifierType3.createCopy();
            int lengthValue = verifierTypes1.length;
            VerifierType[] verifierTypes2 = VerifierType.createArray(lengthValue);
            System.arraycopy(verifierTypes1, 0, verifierTypes2, 0, lengthValue);

            for (int verifierTypes3ElementIndex = 0; verifierTypes3ElementIndex < bg; verifierTypes3ElementIndex++) {
                if (verifierTypes3[verifierTypes3ElementIndex] == verifierType3) {
                    verifierTypes3[verifierTypes3ElementIndex] = verifierType2;
                }
            }

            for (int verifierTypes2ElementIndex = 0; verifierTypes2ElementIndex < lengthValue; verifierTypes2ElementIndex++) {
                if (verifierTypes2[verifierTypes2ElementIndex] == verifierType3) {
                    verifierTypes2[verifierTypes2ElementIndex] = verifierType2;
                }
            }

            return new StackFrameState(verifierTypes3, verifierTypes2, subroutineLocalsBitSet, set1);
        } else {
            return new StackFrameState(verifierTypes3, verifierTypes1, subroutineLocalsBitSet, set1);
        }
    }

    @Override
    public final boolean isExit() {
        return false;
    }

    @Override
    public boolean isLongConstantLoad() {
        return this.opcode == 20 && this.constantEntry instanceof ConstantLong;
    }

    public int getInvokeStackDelta() {
        ResolvedMethodRef resolvedMethodRef = (ResolvedMethodRef) this.constantEntry;
        int invokeStackDelta = 0;
        if (this.opcode != 184) {
            invokeStackDelta += -1;
        }

        String stackReturnType = resolvedMethodRef.getStackReturnType();
        invokeStackDelta += stackReturnType == null ? 0 : (ConstantPoolEntry.isWideType(stackReturnType) ? 2 : 1);
        Iterator iterator = resolvedMethodRef.getStackParameterTypes().iterator();

        while (iterator.hasNext()) {
            String string1 = (String) iterator.next();
            invokeStackDelta += ConstantPoolEntry.isWideType(string1) ? -2 : -1;
        }

        return invokeStackDelta;
    }

    @Override
    public boolean pushesWithoutPopping() {
        switch (this.opcode) {
            case 19:
            case 20:
            case 178:
            case 187:
                return true;
            case 179:
            case 180:
            case 181:
            case 182:
            case 183:
            case 185:
            case 186:
            case 189:
            case 192:
            case 193:
            case 197:
                return false;
            case 184:
                ResolvedMethodRef resolvedMethodRef = (ResolvedMethodRef) this.constantEntry;
                return resolvedMethodRef.getStackReturnType() != null && resolvedMethodRef.getStackParameterTypes().size() == 0;
            default:
                ZkmAssert.assertTrue(false, new String[]{"invalid opcode " + this.opcode});
                return false;
        }
    }

    @Override
    public boolean collectStringConstant(MultiMapTable multiMapTable, Set set1, MethodInfo methodInfo1, int rank) {
        if (this.opcode == 19 && this.constantEntry instanceof ResolvedStringConstant) {
            ResolvedStringConstant resolvedStringConstant = (ResolvedStringConstant) this.constantEntry;
            if (resolvedStringConstant.getValueUtf8().getLength() >= 2 && resolvedStringConstant.isFromClassFile() && !set1.contains(resolvedStringConstant)) {
                multiMapTable.addEntry(methodInfo1.getOwnerProgramClass(), methodInfo1, resolvedStringConstant, new RankedValue(rank, this));
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    @Override
    public void writeRemapped(DataOutputStream dataOutputStream, Map map1) throws IOException {
        super.writeTo(dataOutputStream);
        ConstantPoolEntry constantPoolEntry = (ConstantPoolEntry) map1.get(this.constantEntry);
        if (constantPoolEntry != null) {
            dataOutputStream.writeShort(constantPoolEntry.getIndex());
        } else {
            dataOutputStream.writeShort(this.constantEntry.getIndex());
        }
    }

    @Override
    public boolean loadsConstantInteger() {
        return this.opcode == 19 && this.constantEntry instanceof ConstantInteger;
    }
}

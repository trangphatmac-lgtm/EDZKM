package com.zelix.klassmaster.classfile.attribute;

import com.zelix.klassmaster.classfile.ClassFileComponent;
import com.zelix.klassmaster.classfile.ClassFileInputStream;
import com.zelix.klassmaster.classfile.constpool.UsedConstantsCollector;
import com.zelix.klassmaster.classfile.insn.InstructionUsageClearFlag;
import com.zelix.klassmaster.classfile.insn.InstructionUsageFlag;
import com.zelix.klassmaster.classfile.insn.LabelInstruction;
import com.zelix.klassmaster.classfile.insn.LabelTargetHolder;
import com.zelix.klassmaster.util.ListMultimap;
import com.zelix.klassmaster.util.SetMultiMap;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class TypeAnnotation extends ClassFileComponent implements AnnotationElementValueType, LabelTargetHolder {
    public TypeAnnotationTargetType targetType;
    public LabelInstruction offsetLabel;
    public String errorMessage;
    public boolean valid = true;
    public int targetIndex;
    public int boundIndex;
    public LocalVarTargetEntry[] localVarTargets;
    public int offset;
    public int pathLength;
    public TypePathEntry[] typePath;

    @Override
    public void bindLabel(Integer integer, LabelInstruction labelInstruction) {
        switch (TypeAnnotationTargetSwitchMap.TARGET_TYPE_SWITCH[this.targetType.ordinal()]) {
            case 14:
            case 15:
            case 16:
            case 17:
            case 18:
            case 19:
            case 20:
            case 21:
            case 22:
                this.offsetLabel = labelInstruction;
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
            case 8:
            case 9:
            case 10:
            case 11:
            case 12:
            case 13:
        }
    }

    @Override
    public void write(int bd, DataOutputStream dataOutputStream, int be, int bf) throws IOException {
        dataOutputStream.writeByte(this.targetType.getCode());
        switch (TypeAnnotationTargetSwitchMap.TARGET_TYPE_SWITCH[this.targetType.ordinal()]) {
            case 1:
            case 2:
                dataOutputStream.writeByte(this.targetIndex);
                break;
            case 3:
                dataOutputStream.writeShort(this.targetIndex);
                break;
            case 4:
            case 5:
                dataOutputStream.writeByte(this.targetIndex);
                dataOutputStream.writeByte(this.boundIndex);
            case 6:
            case 7:
            case 8:
            default:
                break;
            case 9:
                dataOutputStream.writeByte(this.targetIndex);
                break;
            case 10:
                dataOutputStream.writeShort(this.targetIndex);
                break;
            case 11:
            case 12:
                this.writeLocalVarTargets(dataOutputStream);
                break;
            case 13:
                dataOutputStream.writeShort(this.targetIndex);
                break;
            case 14:
            case 15:
            case 16:
            case 17:
                if (this.offsetLabel != null) {
                    dataOutputStream.writeShort(this.offsetLabel.getOffset());
                } else {
                    dataOutputStream.writeShort(this.offset);
                }
                break;
            case 18:
            case 19:
            case 20:
            case 21:
            case 22:
                if (this.offsetLabel != null) {
                    dataOutputStream.writeShort(this.offsetLabel.getOffset());
                } else {
                    dataOutputStream.writeShort(this.offset);
                }

                dataOutputStream.writeByte(this.targetIndex);
        }

        dataOutputStream.writeByte(this.pathLength);
        if (this.pathLength > 0) {
            int typePathElementIndex = 0;
            int typePathElementIndexValue = typePathElementIndex;

            for (int pathLengthValue = this.pathLength; typePathElementIndexValue < pathLengthValue; pathLengthValue = this.pathLength) {
                this.typePath[typePathElementIndex].writeTo(dataOutputStream);
                typePathElementIndexValue = ++typePathElementIndex;
            }
        }
    }

    @Override
    public boolean isValid() {
        return this.valid;
    }

    @Override
    public void updateAfterClassRename(Object object, Object object1) {
    }

    @Override
    public void registerLabelTargets(SetMultiMap setMultiMap) {
        if (this.valid) {
            switch (TypeAnnotationTargetSwitchMap.TARGET_TYPE_SWITCH[this.targetType.ordinal()]) {
                case 1:
                case 2:
                case 3:
                case 4:
                case 5:
                case 6:
                case 7:
                case 8:
                case 9:
                case 10:
                case 13:
                default:
                    break;
                case 11:
                case 12:
                    LocalVarTargetEntry[] localVarTargetEntrys = this.localVarTargets;
                    int lengthValue = localVarTargetEntrys.length;

                    for (int localVarTargetEntryIndex = 0; localVarTargetEntryIndex < lengthValue; localVarTargetEntryIndex++) {
                        localVarTargetEntrys[localVarTargetEntryIndex].registerLabelTargets(setMultiMap);
                    }
                    break;
                case 14:
                case 15:
                case 16:
                case 17:
                case 18:
                case 19:
                case 20:
                case 21:
                case 22:
                    setMultiMap.addValue(this.offsetLabel, this);
            }
        }
    }

    @Override
    public void updateAfterFieldRename() {
    }

    public boolean requiresSignature() {
        switch (TypeAnnotationTargetSwitchMap.TARGET_TYPE_SWITCH[this.targetType.ordinal()]) {
            case 1:
            case 2:
                return true;
            case 3:
                return hasTypeArgumentStep(this.typePath);
            case 4:
            case 5:
                return true;
            case 6:
            case 7:
            case 8:
                return hasTypeArgumentStep(this.typePath);
            case 9:
                return hasTypeArgumentStep(this.typePath);
            case 10:
                return false;
            case 11:
            case 12:
                return hasTypeArgumentStep(this.typePath);
            case 13:
                return false;
            case 14:
            case 15:
            case 16:
            case 17:
                return hasTypeArgumentStep(this.typePath);
            case 18:
            case 19:
            case 20:
            case 21:
            case 22:
                return hasTypeArgumentStep(this.typePath);
            default:
                return false;
        }
    }

    public void finishReferenceCollection() {
    }

    @Override
    public int getByteLength() {
        int byteLength = 1;
        switch (TypeAnnotationTargetSwitchMap.TARGET_TYPE_SWITCH[this.targetType.ordinal()]) {
            case 1:
            case 2:
                byteLength++;
                break;
            case 3:
                byteLength += 2;
                break;
            case 4:
            case 5:
                byteLength++;
                byteLength++;
            case 6:
            case 7:
            case 8:
            default:
                break;
            case 9:
                byteLength++;
                break;
            case 10:
                byteLength += 2;
                break;
            case 11:
            case 12:
                byteLength += 2;
                byteLength += this.localVarTargets.length * 6;
                break;
            case 13:
                byteLength += 2;
                break;
            case 14:
            case 15:
            case 16:
            case 17:
                byteLength += 2;
                break;
            case 18:
            case 19:
            case 20:
            case 21:
            case 22:
                byteLength += 3;
        }

        byteLength++;
        if (this.pathLength > 0) {
            int typePathElementIndex = 0;
            int bc = 0;

            for (int pathLengthValue = this.pathLength; bc < pathLengthValue; pathLengthValue = this.pathLength) {
                byteLength += this.typePath[typePathElementIndex].getEntrySize();
                bc = ++typePathElementIndex;
            }
        }

        return byteLength;
    }

    public boolean isThrowsTarget() {
        return this.targetType == TypeAnnotationTargetType.THROWS;
    }

    @Override
    public InstructionUsageFlag getUsageFlag() {
        return InstructionUsageFlag.SET_USAGE_ANNOTATION;
    }

    public String getErrorMessage() {
        return this.errorMessage;
    }

    public boolean clearUnsharedLabelUsage(LabelInstruction labelInstruction, SetMultiMap setMultiMap) {
        Set set1 = setMultiMap.getValues(labelInstruction);
        if (set1 != null) {
            Iterator iterator = set1.iterator();

            while (iterator.hasNext()) {
                if (((LabelTargetHolder) iterator.next()).getUsageFlag() == InstructionUsageFlag.SET_USAGE_ANNOTATION) {
                    return false;
                }
            }
        }

        labelInstruction.clearUsageFlag(InstructionUsageClearFlag.CLEAR_USAGE_ANNOTATION);
        return true;
    }

    public boolean unlinkRemovedLabels(HashSet hashSet, SetMultiMap setMultiMap) {
        boolean bl = false;
        if (this.valid) {
            switch (TypeAnnotationTargetSwitchMap.TARGET_TYPE_SWITCH[this.targetType.ordinal()]) {
                case 1:
                case 2:
                case 3:
                case 4:
                case 5:
                case 6:
                case 7:
                case 8:
                case 9:
                case 10:
                case 13:
                default:
                    break;
                case 11:
                case 12:
                    LocalVarTargetEntry[] localVarTargetEntrys = this.localVarTargets;
                    int lengthValue = localVarTargetEntrys.length;

                    for (int localVarTargetEntryIndex = 0; localVarTargetEntryIndex < lengthValue; localVarTargetEntryIndex++) {
                        if (localVarTargetEntrys[localVarTargetEntryIndex].unlinkIfLabelRemoved(hashSet, setMultiMap)) {
                            bl = true;
                        }
                    }
                    break;
                case 14:
                case 15:
                case 16:
                case 17:
                case 18:
                case 19:
                case 20:
                case 21:
                case 22:
                    if (hashSet.contains(this.offsetLabel)) {
                        setMultiMap.removeValue(this.offsetLabel, this);
                        this.clearUnsharedLabelUsage(this.offsetLabel, setMultiMap);
                        bl = true;
                    }
            }
        }

        return bl;
    }

    public void setTargetType(int targetTypeArgument) {
        switch (targetTypeArgument) {
            case 0:
                this.targetType = TypeAnnotationTargetType.CLASS_TYPE_PARAMETER;
                break;
            case 1:
                this.targetType = TypeAnnotationTargetType.METHOD_TYPE_PARAMETER;
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
            case 8:
            case 9:
            case 10:
            case 11:
            case 12:
            case 13:
            case 14:
            case 15:
            case 24:
            case 25:
            case 26:
            case 27:
            case 28:
            case 29:
            case 30:
            case 31:
            case 32:
            case 33:
            case 34:
            case 35:
            case 36:
            case 37:
            case 38:
            case 39:
            case 40:
            case 41:
            case 42:
            case 43:
            case 44:
            case 45:
            case 46:
            case 47:
            case 48:
            case 49:
            case 50:
            case 51:
            case 52:
            case 53:
            case 54:
            case 55:
            case 56:
            case 57:
            case 58:
            case 59:
            case 60:
            case 61:
            case 62:
            case 63:
            default:
                break;
            case 16:
                this.targetType = TypeAnnotationTargetType.CLASS_EXTENDS;
                break;
            case 17:
                this.targetType = TypeAnnotationTargetType.CLASS_TYPE_PARAMETER_BOUND;
                break;
            case 18:
                this.targetType = TypeAnnotationTargetType.METHOD_TYPE_PARAMETER_BOUND;
                break;
            case 19:
                this.targetType = TypeAnnotationTargetType.FIELD;
                break;
            case 20:
                this.targetType = TypeAnnotationTargetType.METHOD_RETURN;
                break;
            case 21:
                this.targetType = TypeAnnotationTargetType.METHOD_RECEIVER;
                break;
            case 22:
                this.targetType = TypeAnnotationTargetType.METHOD_FORMAL_PARAMETER;
                break;
            case 23:
                this.targetType = TypeAnnotationTargetType.THROWS;
                break;
            case 64:
                this.targetType = TypeAnnotationTargetType.LOCAL_VARIABLE;
                break;
            case 65:
                this.targetType = TypeAnnotationTargetType.RESOURCE_VARIABLE;
                break;
            case 66:
                this.targetType = TypeAnnotationTargetType.EXCEPTION_PARAMETER;
                break;
            case 67:
                this.targetType = TypeAnnotationTargetType.INSTANCEOF;
                break;
            case 68:
                this.targetType = TypeAnnotationTargetType.NEW;
                break;
            case 69:
                this.targetType = TypeAnnotationTargetType.METHOD_REFERENCE_0;
                break;
            case 70:
                this.targetType = TypeAnnotationTargetType.METHOD_REFERENCE_1;
                break;
            case 71:
                this.targetType = TypeAnnotationTargetType.CAST;
                break;
            case 72:
                this.targetType = TypeAnnotationTargetType.CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT;
                break;
            case 73:
                this.targetType = TypeAnnotationTargetType.METHOD_INVOCATION_TYPE_ARGUMENT;
                break;
            case 74:
                this.targetType = TypeAnnotationTargetType.CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT;
                break;
            case 75:
                this.targetType = TypeAnnotationTargetType.METHOD_REFERENCE_TYPE_ARGUMENT;
        }
    }

    @Override
    public void collectReferencedClasses(Object object) {
    }

    @Override
    public void writeRemapped(DataOutputStream dataOutputStream, Object object, Object object1) throws IOException {
        dataOutputStream.writeByte(this.targetType.getCode());
        switch (TypeAnnotationTargetSwitchMap.TARGET_TYPE_SWITCH[this.targetType.ordinal()]) {
            case 1:
            case 2:
                dataOutputStream.writeByte(this.targetIndex);
                break;
            case 3:
                dataOutputStream.writeShort(this.targetIndex);
                break;
            case 4:
            case 5:
                dataOutputStream.writeByte(this.targetIndex);
                dataOutputStream.writeByte(this.boundIndex);
            case 6:
            case 7:
            case 8:
            default:
                break;
            case 9:
                dataOutputStream.writeByte(this.targetIndex);
                break;
            case 10:
                dataOutputStream.writeShort(this.targetIndex);
                break;
            case 11:
            case 12:
                this.writeLocalVarTargets(dataOutputStream);
                break;
            case 13:
                dataOutputStream.writeShort(this.targetIndex);
                break;
            case 14:
            case 15:
            case 16:
            case 17:
                if (this.offsetLabel != null) {
                    dataOutputStream.writeShort(this.offsetLabel.getOffset());
                } else {
                    dataOutputStream.writeShort(this.offset);
                }
                break;
            case 18:
            case 19:
            case 20:
            case 21:
            case 22:
                if (this.offsetLabel != null) {
                    dataOutputStream.writeShort(this.offsetLabel.getOffset());
                } else {
                    dataOutputStream.writeShort(this.offset);
                }

                dataOutputStream.writeByte(this.targetIndex);
        }

        dataOutputStream.writeByte(this.pathLength);
        if (this.pathLength > 0) {
            int typePathElementIndex = 0;
            int typePathElementIndexValue = typePathElementIndex;

            for (int pathLengthValue = this.pathLength; typePathElementIndexValue < pathLengthValue; pathLengthValue = this.pathLength) {
                this.typePath[typePathElementIndex].writeTo(dataOutputStream);
                typePathElementIndexValue = ++typePathElementIndex;
            }
        }
    }

    public static boolean hasTypeArgumentStep(TypePathEntry[] typePathEntrys) {
        if (typePathEntrys != null && typePathEntrys.length != 0) {
            TypePathEntry[] typePathEntrys1 = typePathEntrys;
            int lengthValue = typePathEntrys1.length;

            for (int typePathEntrys1ElementIndex = 0; typePathEntrys1ElementIndex < lengthValue; typePathEntrys1ElementIndex++) {
                if (typePathEntrys1[typePathEntrys1ElementIndex].isTypeArgumentStep()) {
                    return true;
                }
            }

            return false;
        } else {
            return false;
        }
    }

    public static boolean clearUnsharedLabelUsageOf(TypeAnnotation typeAnnotation1, LabelInstruction labelInstruction, SetMultiMap setMultiMap) {
        return typeAnnotation1.clearUnsharedLabelUsage(labelInstruction, setMultiMap);
    }

    @Override
    public void updateAfterMethodRename() {
    }

    public TypeAnnotation(ClassFileComponent classFileComponent, ClassFileInputStream classFileInputStream, ListMultimap listMultimap) throws IOException {
        super(classFileComponent);
        this.setTargetType(classFileInputStream.readUnsignedByte());
        switch (TypeAnnotationTargetSwitchMap.TARGET_TYPE_SWITCH[this.targetType.ordinal()]) {
            case 1:
            case 2:
                this.targetIndex = classFileInputStream.readUnsignedByte();
                break;
            case 3:
                this.targetIndex = classFileInputStream.readUnsignedShort();
                break;
            case 4:
            case 5:
                this.targetIndex = classFileInputStream.readUnsignedByte();
                this.boundIndex = classFileInputStream.readUnsignedByte();
            case 6:
            case 7:
            case 8:
            default:
                break;
            case 9:
                this.targetIndex = classFileInputStream.readUnsignedByte();
                break;
            case 10:
                this.targetIndex = classFileInputStream.readUnsignedShort();
                break;
            case 11:
            case 12:
                int ba = classFileInputStream.readUnsignedShort();
                this.localVarTargets = new LocalVarTargetEntry[ba];

                for (int localVarTargetIndex = 0; localVarTargetIndex < ba; localVarTargetIndex++) {
                    LocalVarTargetEntry localVarTargetEntry = new LocalVarTargetEntry(this);
                    localVarTargetEntry.setStartPc(classFileInputStream.readUnsignedShort());
                    localVarTargetEntry.setEndPc(localVarTargetEntry.getStartPc() + classFileInputStream.readUnsignedShort());
                    localVarTargetEntry.setLocalIndex(classFileInputStream.readUnsignedShort());
                    listMultimap.addValue(localVarTargetEntry.getStartPc(), localVarTargetEntry);
                    listMultimap.addValue(localVarTargetEntry.getEndPc(), localVarTargetEntry);
                    this.localVarTargets[localVarTargetIndex] = localVarTargetEntry;
                }
                break;
            case 13:
                this.targetIndex = classFileInputStream.readUnsignedShort();
                break;
            case 14:
            case 15:
            case 16:
            case 17:
                this.offset = classFileInputStream.readUnsignedShort();
                listMultimap.addValue(this.offset, this);
                break;
            case 18:
            case 19:
            case 20:
            case 21:
            case 22:
                this.offset = classFileInputStream.readUnsignedShort();
                listMultimap.addValue(this.offset, this);
                this.targetIndex = classFileInputStream.readUnsignedByte();
        }

        this.pathLength = classFileInputStream.readUnsignedByte();
        if (this.pathLength > 0) {
            this.typePath = new TypePathEntry[this.pathLength];
            int typePathElementIndex = 0;
            int bd = 0;

            for (int pathLengthValue = this.pathLength; bd < pathLengthValue; pathLengthValue = this.pathLength) {
                this.typePath[typePathElementIndex] = new TypePathEntry(this, classFileInputStream);
                if (!this.typePath[typePathElementIndex].isValid()) {
                    this.valid = false;
                }

                bd = ++typePathElementIndex;
            }
        }
    }

    @Override
    public String getHolderTypeName(Object object, Object object1, Object object2) {
        return this.getEnclosingAttribute().getAttributeName();
    }

    public void writeLocalVarTargets(DataOutputStream dataOutputStream) throws IOException {
        dataOutputStream.writeShort(this.localVarTargets.length);

        for (LocalVarTargetEntry localVarTargetEntry : this.localVarTargets) {
            if (localVarTargetEntry.hasLabels()) {
                dataOutputStream.writeShort(localVarTargetEntry.getStartLabel().getOffset());
                dataOutputStream.writeShort(localVarTargetEntry.getLength());
            } else {
                dataOutputStream.writeShort(localVarTargetEntry.getStartPc());
                dataOutputStream.writeShort(localVarTargetEntry.getEndPc() - localVarTargetEntry.getStartPc());
            }

            dataOutputStream.writeShort(localVarTargetEntry.getLocalIndex());
        }
    }

    @Override
    public void collectUsedConstants(char ba, int bb, UsedConstantsCollector usedConstantsCollector, char bc) {
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

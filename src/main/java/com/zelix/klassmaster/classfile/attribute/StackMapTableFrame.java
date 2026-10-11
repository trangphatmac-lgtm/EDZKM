package com.zelix.klassmaster.classfile.attribute;

import com.zelix.klassmaster.classfile.ClassFileComponent;
import com.zelix.klassmaster.classfile.ClassFileInputStream;
import com.zelix.klassmaster.classfile.constpool.ConstantPool;
import com.zelix.klassmaster.classfile.insn.LabelInstruction;
import com.zelix.klassmaster.classfile.insn.VerifierType;
import com.zelix.klassmaster.config.HiddenOptionFlags;
import com.zelix.klassmaster.exceptions.ZkmException;
import com.zelix.klassmaster.util.ListMultimap;
import com.zelix.klassmaster.util.MutableInt;
import com.zelix.klassmaster.util.ObservableHolder;
import com.zelix.klassmaster.util.ZkmAssert;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class StackMapTableFrame extends StackMapFrame {
    public StackMapTableFrame(ClassFileComponent classFileComponent) {
        super(classFileComponent);
    }

    public final StackMapFrameKind getFrameKind() {
        int frameType = this.getFrameType();
        if (frameType >= 0 && frameType <= 63) {
            return StackMapFrameKind.SAME;
        } else if (frameType >= 64 && frameType <= 127) {
            return StackMapFrameKind.SAME_LOCALS_1_STACK_ITEM;
        } else if (frameType == 247) {
            return StackMapFrameKind.SAME_LOCALS_1_STACK_ITEM_EXTENDED;
        } else if (frameType >= 248 && frameType <= 250) {
            return StackMapFrameKind.CHOP;
        } else if (frameType == 251) {
            return StackMapFrameKind.SAME_EXTENDED;
        } else if (frameType >= 252 && frameType <= 254) {
            return StackMapFrameKind.APPEND;
        } else {
            return frameType == 255 ? StackMapFrameKind.FULL : null;
        }
    }

    public static StackMapTableFrame readFrame(
            StackMapTableAttribute stackMapTableAttribute,
            ClassFileInputStream classFileInputStream,
            ListMultimap listMultimap,
            ListMultimap listMultimap1,
            PrintWriter printWriter,
            MutableInt mutableInt,
            Map map1,
            Map map2
    ) throws IOException {
        StackMapTableFrame stackMapTableFrame = null;
        int frameTypeValue = classFileInputStream.readUnsignedByte();
        if (frameTypeValue == 255) {
            stackMapTableFrame = new FullStackMapFrame(
                    stackMapTableAttribute, classFileInputStream, listMultimap, listMultimap1, printWriter, mutableInt, map1, map2
            );
        } else if (frameTypeValue == 251) {
            stackMapTableFrame = new SameFrameExtended(stackMapTableAttribute, classFileInputStream, listMultimap, mutableInt);
        } else if (frameTypeValue >= 0 && frameTypeValue <= 63) {
            stackMapTableFrame = new SameFrame(frameTypeValue, stackMapTableAttribute, listMultimap, mutableInt);
        } else if (frameTypeValue >= 64 && frameTypeValue <= 127) {
            stackMapTableFrame = new SameLocalsOneStackItemFrame(
                    frameTypeValue, stackMapTableAttribute, classFileInputStream, listMultimap, listMultimap1, printWriter, mutableInt, map1, map2
            );
        } else if (frameTypeValue == 247) {
            stackMapTableFrame = new SameLocals1StackItemExtendedFrame(
                    stackMapTableAttribute, classFileInputStream, listMultimap, listMultimap1, printWriter, mutableInt, map1, map2
            );
        } else if (frameTypeValue >= 248 && frameTypeValue <= 250) {
            stackMapTableFrame = new ChopFrame(frameTypeValue, stackMapTableAttribute, classFileInputStream, listMultimap, mutableInt);
        } else if (frameTypeValue >= 252 && frameTypeValue <= 254) {
            stackMapTableFrame = new AppendFrame(
                    frameTypeValue, stackMapTableAttribute, classFileInputStream, listMultimap, listMultimap1, printWriter, mutableInt, map1, map2
            );
        } else {
            ZkmAssert.assertTrue(false, new String[]{"Invalid StackMapTable frame type '" + frameTypeValue + "' in '" + stackMapTableAttribute.getMethodDescription() + "'"});
        }

        return stackMapTableFrame;
    }

    public final void writeFrame(DataOutputStream dataOutputStream) throws IOException {
        this.writeRemapped(dataOutputStream, null);
    }

    public abstract int getFrameType();

    public static boolean areTypesEqual(VerificationTypeInfo[] verificationTypeInfos, VerificationTypeInfo[] verificationTypeInfos1) {
        if (verificationTypeInfos.length == verificationTypeInfos1.length) {
            for (int i = 0; i < verificationTypeInfos.length; i++) {
                if (!verificationTypeInfos[i].isSameType(verificationTypeInfos1[i])) {
                    return false;
                }
            }

            return true;
        } else {
            return false;
        }
    }

    public abstract void writeRemapped(DataOutputStream dataOutputStream, Map map1) throws IOException;

    public static StackMapTableFrame createFrame(
            StackMapTableAttribute stackMapTableAttribute,
            LabelInstruction labelInstruction,
            VerifierType[] stackVerifierTypes,
            VerifierType[] localVerifierTypes,
            ConstantPool constantPool1,
            Set set1,
            List list1,
            MutableInt previousFrameOffset,
            ObservableHolder stackTypesHolder,
            ObservableHolder localTypesHolder,
            Map map1,
            Map map2
    ) throws ZkmException, IOException {
        int offset = labelInstruction.getOffset();
        int value = previousFrameOffset.getValue();
        int offsetDelta;
        if (value == -1) {
            offsetDelta = offset;
        } else {
            offsetDelta = offset - value - 1;
        }

        previousFrameOffset.setValue(offset);
        VerificationTypeInfo[] previousLocals = (VerificationTypeInfo[]) localTypesHolder.getValue();
        VerificationTypeInfo[] currentLocals = VerificationTypeInfo.convertVerifierTypes(
                stackMapTableAttribute, localVerifierTypes, constantPool1, set1, list1, true, map1, map2
        );
        VerificationTypeInfo[] stackTypes = VerificationTypeInfo.convertVerifierTypes(
                stackMapTableAttribute, stackVerifierTypes, constantPool1, set1, list1, false, map1, map2
        );
        boolean localsUnchanged = areTypesEqual(previousLocals, currentLocals);
        StackMapTableFrame stackMapTableFrame = null;
        int selectedFrameType = -1;
        if (HiddenOptionFlags.FORCE_FULL_STACK_MAP_FRAMES) {
            selectedFrameType = 255;
            stackMapTableFrame = new FullStackMapFrame(stackMapTableAttribute, offsetDelta, labelInstruction, stackTypes, currentLocals);
        } else {
            if (localsUnchanged && stackTypes.length == 0) {
                if (offsetDelta >= 0 && offsetDelta <= 63) {
                    selectedFrameType = offsetDelta;
                    stackMapTableFrame = new SameFrame(stackMapTableAttribute, selectedFrameType, labelInstruction);
                } else {
                    selectedFrameType = 251;
                    stackMapTableFrame = new SameFrameExtended(stackMapTableAttribute, labelInstruction, offsetDelta);
                }
            } else {
                if (localsUnchanged && stackTypes.length == 1) {
                    if (offsetDelta >= 0 && offsetDelta <= 63) {
                        selectedFrameType = offsetDelta + 64;
                        stackMapTableFrame = new SameLocalsOneStackItemFrame(stackMapTableAttribute, selectedFrameType, labelInstruction, stackTypes[0]);
                    } else {
                        selectedFrameType = 247;
                        stackMapTableFrame = new SameLocals1StackItemExtendedFrame(stackMapTableAttribute, offsetDelta, labelInstruction, stackTypes[0]);
                    }
                } else if (stackTypes.length == 0) {
                    int removedLocalCount = previousLocals.length - currentLocals.length;
                    if (removedLocalCount >= 1 && removedLocalCount <= 3) {
                        boolean remainingLocalsUnchanged = true;

                        for (int i = 0; i < currentLocals.length; i++) {
                            if (!previousLocals[i].isSameType(currentLocals[i])) {
                                remainingLocalsUnchanged = false;
                                break;
                            }
                        }

                        if (remainingLocalsUnchanged) {
                            selectedFrameType = 251 - removedLocalCount;
                            stackMapTableFrame = new ChopFrame(stackMapTableAttribute, selectedFrameType, offsetDelta, labelInstruction);
                        }
                    }

                    if (selectedFrameType == -1) {
                        int appendedLocalCount = currentLocals.length - previousLocals.length;
                        if (appendedLocalCount >= 1 && appendedLocalCount <= 3) {
                            boolean previousLocalsUnchanged = true;

                            for (int i = 0; i < previousLocals.length; i++) {
                                if (!previousLocals[i].isSameType(currentLocals[i])) {
                                    previousLocalsUnchanged = false;
                                    break;
                                }
                            }

                            if (previousLocalsUnchanged) {
                                selectedFrameType = 251 + appendedLocalCount;
                                int appendedTypeCount = appendedLocalCount;
                                VerificationTypeInfo[] appendedLocals = new VerificationTypeInfo[appendedTypeCount];
                                System.arraycopy(currentLocals, currentLocals.length - appendedTypeCount, appendedLocals, 0, appendedTypeCount);
                                stackMapTableFrame = new AppendFrame(stackMapTableAttribute, selectedFrameType, offsetDelta, labelInstruction, appendedLocals);
                            }
                        }
                    }
                }
            }
        }

        if (selectedFrameType == -1) {
            stackMapTableFrame = new FullStackMapFrame(stackMapTableAttribute, offsetDelta, labelInstruction, stackTypes, currentLocals);
        }

        localTypesHolder.setValue(currentLocals);
        stackTypesHolder.setValue(stackTypes);
        return stackMapTableFrame;
    }

    @Override
    public final String getHolderTypeName(Object object, Object object1, Object object2) {
        return "StackMapTable";
    }
}

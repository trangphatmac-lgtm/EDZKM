package com.zelix.klassmaster.classfile.insn;

import com.zelix.klassmaster.exceptions.MethodAnalysisException;
import com.zelix.klassmaster.exceptions.ZkmException;
import com.zelix.klassmaster.util.TruncatedStringDisplay;
import com.zelix.klassmaster.util.ZkmAssert;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;

public class SimpleInstruction extends Instruction {
    private static final SimpleInstruction[] INSTANCES_BY_OPCODE = new SimpleInstruction[202];

    @Override
    public final boolean isExit() {
        switch (this.opcode) {
            case 0:
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
            case 14:
            case 15:
            case 46:
            case 47:
            case 48:
            case 49:
            case 50:
            case 51:
            case 52:
            case 53:
            case 79:
            case 80:
            case 81:
            case 82:
            case 83:
            case 84:
            case 85:
            case 86:
            case 87:
            case 88:
            case 89:
            case 90:
            case 91:
            case 92:
            case 93:
            case 94:
            case 95:
            case 96:
            case 97:
            case 98:
            case 99:
            case 100:
            case 101:
            case 102:
            case 103:
            case 104:
            case 105:
            case 106:
            case 107:
            case 108:
            case 109:
            case 110:
            case 111:
            case 112:
            case 113:
            case 114:
            case 115:
            case 116:
            case 117:
            case 118:
            case 119:
            case 120:
            case 121:
            case 122:
            case 123:
            case 124:
            case 125:
            case 126:
            case 127:
            case 128:
            case 129:
            case 130:
            case 131:
            case 133:
            case 134:
            case 135:
            case 136:
            case 137:
            case 138:
            case 139:
            case 140:
            case 141:
            case 142:
            case 143:
            case 144:
            case 145:
            case 146:
            case 147:
            case 148:
            case 149:
            case 150:
            case 151:
            case 152:
            case 190:
                return false;
            case 16:
            case 17:
            case 18:
            case 19:
            case 20:
            case 21:
            case 22:
            case 23:
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
            case 64:
            case 65:
            case 66:
            case 67:
            case 68:
            case 69:
            case 70:
            case 71:
            case 72:
            case 73:
            case 74:
            case 75:
            case 76:
            case 77:
            case 78:
            case 132:
            case 153:
            case 154:
            case 155:
            case 156:
            case 157:
            case 158:
            case 159:
            case 160:
            case 161:
            case 162:
            case 163:
            case 164:
            case 165:
            case 166:
            case 167:
            case 168:
            case 169:
            case 170:
            case 171:
            case 178:
            case 179:
            case 180:
            case 181:
            case 182:
            case 183:
            case 184:
            case 185:
            case 186:
            case 187:
            case 188:
            case 189:
            default:
                Integer integer = this.opcode;
                ZkmAssert.assertTrueWithCode(integer);
                return false;
            case 172:
            case 173:
            case 174:
            case 175:
            case 176:
            case 177:
            case 191:
                return true;
        }
    }

    @Override
    public boolean affectsStackSlot(Object object, Object object1) {
        int ba = (Integer) object1;
        VerifierType[] verifierTypes = (VerifierType[]) object;
        int verifierTypeIndex = verifierTypes.length - 1;
        switch (this.opcode) {
            case 87:
            case 88:
                return false;
            case 89:
                return ba >= verifierTypeIndex - 1;
            case 90:
                return ba >= verifierTypeIndex - 2;
            case 91:
                if (verifierTypes[verifierTypeIndex - 1].isWide()) {
                    return ba >= verifierTypeIndex - 2;
                }

                return ba >= verifierTypeIndex - 3;
            case 92:
                if (verifierTypes[verifierTypeIndex].isWide()) {
                    return ba >= verifierTypeIndex - 1;
                }

                return ba >= verifierTypeIndex - 3;
            case 93:
                if (verifierTypes[verifierTypeIndex].isWide()) {
                    return ba >= verifierTypeIndex - 2;
                }

                return ba >= verifierTypeIndex - 4;
            case 94:
                if (verifierTypes[verifierTypeIndex].isWide()) {
                    if (verifierTypes[verifierTypeIndex - 1].isWide()) {
                        return ba >= verifierTypeIndex - 2;
                    }

                    return ba >= verifierTypeIndex - 3;
                } else {
                    if (verifierTypes[verifierTypeIndex - 2].isWide()) {
                        return ba >= verifierTypeIndex - 4;
                    }

                    return ba >= verifierTypeIndex - 5;
                }
            case 95:
                return ba >= verifierTypeIndex - 1;
            default:
                ZkmAssert.assertTrue(false, new String[]{this.opcode + " " + this.getMnemonic()});
                return false;
        }
    }

    @Override
    public int getStackDelta() {
        switch (this.opcode) {
            case 0:
                return 0;
            case 1:
                return 1;
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
            case 8:
                return 1;
            case 9:
            case 10:
                return 2;
            case 11:
            case 12:
            case 13:
                return 1;
            case 14:
            case 15:
                return 2;
            case 16:
            case 17:
            case 18:
            case 19:
            case 20:
            case 21:
            case 22:
            case 23:
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
            case 64:
            case 65:
            case 66:
            case 67:
            case 68:
            case 69:
            case 70:
            case 71:
            case 72:
            case 73:
            case 74:
            case 75:
            case 76:
            case 77:
            case 78:
            case 132:
            case 153:
            case 154:
            case 155:
            case 156:
            case 157:
            case 158:
            case 159:
            case 160:
            case 161:
            case 162:
            case 163:
            case 164:
            case 165:
            case 166:
            case 167:
            case 168:
            case 169:
            case 170:
            case 171:
            case 178:
            case 179:
            case 180:
            case 181:
            case 182:
            case 183:
            case 184:
            case 185:
            case 186:
            case 187:
            case 188:
            case 189:
            default:
                return 0;
            case 46:
                return -1;
            case 47:
                return 0;
            case 48:
                return -1;
            case 49:
                return 0;
            case 50:
                return -1;
            case 51:
                return -1;
            case 52:
                return -1;
            case 53:
                return -1;
            case 79:
            case 84:
            case 85:
            case 86:
                return -3;
            case 80:
                return -4;
            case 81:
                return -3;
            case 82:
                return -4;
            case 83:
                return -3;
            case 87:
                return -1;
            case 88:
                return -2;
            case 89:
                return 1;
            case 90:
                return 1;
            case 91:
                return 1;
            case 92:
                return 2;
            case 93:
                return 2;
            case 94:
                return 2;
            case 95:
                return 0;
            case 96:
            case 100:
            case 104:
            case 108:
            case 112:
            case 120:
            case 122:
            case 124:
            case 126:
            case 128:
            case 130:
                return -1;
            case 97:
            case 101:
            case 105:
            case 109:
            case 113:
            case 127:
            case 129:
            case 131:
                return -2;
            case 98:
            case 102:
            case 106:
            case 110:
            case 114:
                return -1;
            case 99:
            case 103:
            case 107:
            case 111:
            case 115:
                return -2;
            case 116:
            case 145:
            case 146:
            case 147:
                return 0;
            case 117:
                return 0;
            case 118:
                return 0;
            case 119:
                return 0;
            case 121:
            case 123:
            case 125:
                return -1;
            case 133:
                return 1;
            case 134:
                return 0;
            case 135:
                return 1;
            case 136:
                return -1;
            case 137:
                return -1;
            case 138:
                return 1;
            case 139:
                return 0;
            case 140:
                return 1;
            case 141:
                return 1;
            case 142:
                return -1;
            case 143:
                return 0;
            case 144:
                return -1;
            case 148:
                return -3;
            case 149:
            case 150:
                return -1;
            case 151:
            case 152:
                return -3;
            case 172:
                return -1;
            case 173:
                return -2;
            case 174:
                return -1;
            case 175:
                return -2;
            case 176:
                return -1;
            case 177:
                return 0;
            case 190:
                return 0;
            case 191:
                return 0;
        }
    }

    private StackFrameState pushType(
            int copyLength, VerifierType[] verifierTypes, VerifierType[] verifierTypes1, VerifierType verifierType, SubroutineLocalsBitSet subroutineLocalsBitSet, Set set1
    ) {
        VerifierType[] verifierTypes2 = VerifierType.createArray(copyLength + 1);
        System.arraycopy(verifierTypes, 0, verifierTypes2, 0, copyLength);
        verifierTypes2[copyLength] = verifierType;
        return new StackFrameState(verifierTypes2, verifierTypes1, subroutineLocalsBitSet, set1);
    }

    private SimpleInstruction(int opcodeArgument) {
        super(opcodeArgument);
    }

    @Override
    public void printDisassembly(PrintWriter printWriter, StringBuilder stringBuilder) throws UnknownOpcodeException {
        StringBuilder stringBuilder1 = new StringBuilder(100);
        String mnemonic = this.getMnemonic();
        stringBuilder1.append(mnemonic);
        String descriptionComment = this.getDescriptionComment();
        if (descriptionComment.length() > 0) {
            stringBuilder1.append("\t" + descriptionComment);
        }

        printWriter.println(stringBuilder.toString() + stringBuilder.toString() + stringBuilder1);
    }

    @Override
    public final boolean canFallThrough() {
        switch (this.opcode) {
            case 0:
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
            case 14:
            case 15:
            case 46:
            case 47:
            case 48:
            case 49:
            case 50:
            case 51:
            case 52:
            case 53:
            case 79:
            case 80:
            case 81:
            case 82:
            case 83:
            case 84:
            case 85:
            case 86:
            case 87:
            case 88:
            case 89:
            case 90:
            case 91:
            case 92:
            case 93:
            case 94:
            case 95:
            case 96:
            case 97:
            case 98:
            case 99:
            case 100:
            case 101:
            case 102:
            case 103:
            case 104:
            case 105:
            case 106:
            case 107:
            case 108:
            case 109:
            case 110:
            case 111:
            case 112:
            case 113:
            case 114:
            case 115:
            case 116:
            case 117:
            case 118:
            case 119:
            case 120:
            case 121:
            case 122:
            case 123:
            case 124:
            case 125:
            case 126:
            case 127:
            case 128:
            case 129:
            case 130:
            case 131:
            case 133:
            case 134:
            case 135:
            case 136:
            case 137:
            case 138:
            case 139:
            case 140:
            case 141:
            case 142:
            case 143:
            case 144:
            case 145:
            case 146:
            case 147:
            case 148:
            case 149:
            case 150:
            case 151:
            case 152:
            case 190:
                return true;
            case 16:
            case 17:
            case 18:
            case 19:
            case 20:
            case 21:
            case 22:
            case 23:
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
            case 64:
            case 65:
            case 66:
            case 67:
            case 68:
            case 69:
            case 70:
            case 71:
            case 72:
            case 73:
            case 74:
            case 75:
            case 76:
            case 77:
            case 78:
            case 132:
            case 153:
            case 154:
            case 155:
            case 156:
            case 157:
            case 158:
            case 159:
            case 160:
            case 161:
            case 162:
            case 163:
            case 164:
            case 165:
            case 166:
            case 167:
            case 168:
            case 169:
            case 170:
            case 171:
            case 178:
            case 179:
            case 180:
            case 181:
            case 182:
            case 183:
            case 184:
            case 185:
            case 186:
            case 187:
            case 188:
            case 189:
            default:
                Integer integer = this.opcode;
                ZkmAssert.assertTrueWithCode(integer);
                return true;
            case 172:
            case 173:
            case 174:
            case 175:
            case 176:
            case 177:
            case 191:
                return false;
        }
    }

    @Override
    public StackFrameState computeFrameAfter(StackFrameState stackFrameState, Object object1, Object object2, Object object) throws ZkmException, IOException {
        new TruncatedStringDisplay((String) object);
        VerifierType[] verifierTypes = stackFrameState.getLocals();
        VerifierType[] verifierTypes1 = stackFrameState.getStack();
        Set set1 = stackFrameState.getHeldMonitors();
        int lengthValue = verifierTypes1.length;
        SubroutineLocalsBitSet subroutineLocalsBitSet = stackFrameState.getSubroutineLocals();
        switch (this.opcode) {
            case 0:
                return new StackFrameState(verifierTypes1, verifierTypes, subroutineLocalsBitSet, set1);
            case 1:
                VerifierType[] verifierTypes44 = VerifierType.createArray(lengthValue + 1);
                System.arraycopy(verifierTypes1, 0, verifierTypes44, 0, lengthValue);
                verifierTypes44[lengthValue] = VerifierType.NULL;
                return new StackFrameState(verifierTypes44, verifierTypes, subroutineLocalsBitSet, set1);
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
            case 8:
                return this.pushType(lengthValue, verifierTypes1, verifierTypes, VerifierType.INT, subroutineLocalsBitSet, set1);
            case 9:
            case 10:
                return this.pushType(lengthValue, verifierTypes1, verifierTypes, VerifierType.LONG, subroutineLocalsBitSet, set1);
            case 11:
            case 12:
            case 13:
                return this.pushType(lengthValue, verifierTypes1, verifierTypes, VerifierType.FLOAT, subroutineLocalsBitSet, set1);
            case 14:
            case 15:
                return this.pushType(lengthValue, verifierTypes1, verifierTypes, VerifierType.DOUBLE, subroutineLocalsBitSet, set1);
            case 16:
            case 17:
            case 18:
            case 19:
            case 20:
            case 21:
            case 22:
            case 23:
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
            case 64:
            case 65:
            case 66:
            case 67:
            case 68:
            case 69:
            case 70:
            case 71:
            case 72:
            case 73:
            case 74:
            case 75:
            case 76:
            case 77:
            case 78:
            case 132:
            case 153:
            case 154:
            case 155:
            case 156:
            case 157:
            case 158:
            case 159:
            case 160:
            case 161:
            case 162:
            case 163:
            case 164:
            case 165:
            case 166:
            case 167:
            case 168:
            case 169:
            case 170:
            case 171:
            case 178:
            case 179:
            case 180:
            case 181:
            case 182:
            case 183:
            case 184:
            case 185:
            case 186:
            case 187:
            case 188:
            case 189:
            default:
                Integer integer = this.opcode;
                ZkmAssert.assertTrueWithCode(integer);
                return null;
            case 46:
                Set set2 = set1;
                SubroutineLocalsBitSet subroutineLocalsBitSet1 = subroutineLocalsBitSet;
                VerifierType verifierType = VerifierType.INT;
                return this.popTwoPushType(lengthValue, verifierTypes1, verifierTypes, verifierType, subroutineLocalsBitSet1, set2);
            case 47:
                Set set3 = set1;
                SubroutineLocalsBitSet subroutineLocalsBitSet2 = subroutineLocalsBitSet;
                VerifierType verifierType1 = VerifierType.LONG;
                return this.popTwoPushType(lengthValue, verifierTypes1, verifierTypes, verifierType1, subroutineLocalsBitSet2, set3);
            case 48:
                Set set4 = set1;
                SubroutineLocalsBitSet subroutineLocalsBitSet3 = subroutineLocalsBitSet;
                VerifierType verifierType2 = VerifierType.FLOAT;
                return this.popTwoPushType(lengthValue, verifierTypes1, verifierTypes, verifierType2, subroutineLocalsBitSet3, set4);
            case 49:
                Set set5 = set1;
                SubroutineLocalsBitSet subroutineLocalsBitSet4 = subroutineLocalsBitSet;
                VerifierType verifierType3 = VerifierType.DOUBLE;
                return this.popTwoPushType(lengthValue, verifierTypes1, verifierTypes, verifierType3, subroutineLocalsBitSet4, set5);
            case 50:
                VerifierType[] verifierTypes43 = VerifierType.createArray(lengthValue - 1);
                System.arraycopy(verifierTypes1, 0, verifierTypes43, 0, lengthValue - 2);
                verifierTypes43[lengthValue - 2] = verifierTypes1[lengthValue - 2].getElementType();
                return new StackFrameState(verifierTypes43, verifierTypes, subroutineLocalsBitSet, set1);
            case 51:
                Set set6 = set1;
                SubroutineLocalsBitSet subroutineLocalsBitSet5 = subroutineLocalsBitSet;
                VerifierType verifierType4 = VerifierType.INT;
                return this.popTwoPushType(lengthValue, verifierTypes1, verifierTypes, verifierType4, subroutineLocalsBitSet5, set6);
            case 52:
                Set set7 = set1;
                SubroutineLocalsBitSet subroutineLocalsBitSet6 = subroutineLocalsBitSet;
                VerifierType verifierType5 = VerifierType.INT;
                return this.popTwoPushType(lengthValue, verifierTypes1, verifierTypes, verifierType5, subroutineLocalsBitSet6, set7);
            case 53:
                Set set8 = set1;
                SubroutineLocalsBitSet subroutineLocalsBitSet7 = subroutineLocalsBitSet;
                VerifierType verifierType6 = VerifierType.INT;
                return this.popTwoPushType(lengthValue, verifierTypes1, verifierTypes, verifierType6, subroutineLocalsBitSet7, set8);
            case 79:
            case 84:
            case 85:
            case 86:
                VerifierType[] verifierTypes42 = VerifierType.createArray(lengthValue - 3);
                System.arraycopy(verifierTypes1, 0, verifierTypes42, 0, lengthValue - 3);
                return new StackFrameState(verifierTypes42, verifierTypes, subroutineLocalsBitSet, set1);
            case 80:
                VerifierType[] verifierTypes41 = VerifierType.createArray(lengthValue - 3);
                System.arraycopy(verifierTypes1, 0, verifierTypes41, 0, lengthValue - 3);
                return new StackFrameState(verifierTypes41, verifierTypes, subroutineLocalsBitSet, set1);
            case 81:
                VerifierType[] verifierTypes40 = VerifierType.createArray(lengthValue - 3);
                System.arraycopy(verifierTypes1, 0, verifierTypes40, 0, lengthValue - 3);
                return new StackFrameState(verifierTypes40, verifierTypes, subroutineLocalsBitSet, set1);
            case 82:
                VerifierType[] verifierTypes39 = VerifierType.createArray(lengthValue - 3);
                System.arraycopy(verifierTypes1, 0, verifierTypes39, 0, lengthValue - 3);
                return new StackFrameState(verifierTypes39, verifierTypes, subroutineLocalsBitSet, set1);
            case 83:
                VerifierType[] verifierTypes38 = VerifierType.createArray(lengthValue - 3);
                System.arraycopy(verifierTypes1, 0, verifierTypes38, 0, lengthValue - 3);
                return new StackFrameState(verifierTypes38, verifierTypes, subroutineLocalsBitSet, set1);
            case 87:
                if (lengthValue - 1 < 0) {
                    throw new MethodAnalysisException("Method may be corrupt (A)", "Unknown");
                }

                VerifierType[] verifierTypes37 = VerifierType.createArray(lengthValue - 1);
                System.arraycopy(verifierTypes1, 0, verifierTypes37, 0, lengthValue - 1);
                return new StackFrameState(verifierTypes37, verifierTypes, subroutineLocalsBitSet, set1);
            case 88:
                if (lengthValue - 1 < 0) {
                    throw new MethodAnalysisException("Method may be corrupt (B)", "Unknown");
                } else if (StackFrameState.isWideType(verifierTypes1[lengthValue - 1])) {
                    VerifierType[] verifierTypes36 = VerifierType.createArray(lengthValue - 1);
                    System.arraycopy(verifierTypes1, 0, verifierTypes36, 0, lengthValue - 1);
                    return new StackFrameState(verifierTypes36, verifierTypes, subroutineLocalsBitSet, set1);
                } else {
                    if (lengthValue - 2 < 0) {
                        throw new MethodAnalysisException("Method may be corrupt (C)", "Unknown");
                    }

                    VerifierType[] verifierTypes35 = VerifierType.createArray(lengthValue - 2);
                    System.arraycopy(verifierTypes1, 0, verifierTypes35, 0, lengthValue - 2);
                    return new StackFrameState(verifierTypes35, verifierTypes, subroutineLocalsBitSet, set1);
                }
            case 89:
                VerifierType[] verifierTypes34 = VerifierType.createArray(lengthValue + 1);
                System.arraycopy(verifierTypes1, 0, verifierTypes34, 0, lengthValue);
                verifierTypes34[lengthValue] = verifierTypes1[lengthValue - 1];
                return new StackFrameState(verifierTypes34, verifierTypes, subroutineLocalsBitSet, set1);
            case 90:
                VerifierType[] verifierTypes33 = VerifierType.createArray(lengthValue + 1);
                int lengthValue2 = verifierTypes33.length;
                System.arraycopy(verifierTypes1, 0, verifierTypes33, 0, lengthValue - 2);
                verifierTypes33[lengthValue2 - 3] = verifierTypes1[lengthValue - 1];
                verifierTypes33[lengthValue2 - 2] = verifierTypes1[lengthValue - 2];
                verifierTypes33[lengthValue2 - 1] = verifierTypes1[lengthValue - 1];
                return new StackFrameState(verifierTypes33, verifierTypes, subroutineLocalsBitSet, set1);
            case 91:
                VerifierType[] verifierTypes32 = VerifierType.createArray(lengthValue + 1);
                int lengthValue3 = verifierTypes32.length;
                if (StackFrameState.isWideType(verifierTypes1[lengthValue - 2])) {
                    System.arraycopy(verifierTypes1, 0, verifierTypes32, 0, lengthValue - 2);
                    verifierTypes32[lengthValue3 - 3] = verifierTypes1[lengthValue - 1];
                    verifierTypes32[lengthValue3 - 2] = verifierTypes1[lengthValue - 2];
                    verifierTypes32[lengthValue3 - 1] = verifierTypes1[lengthValue - 1];
                    return new StackFrameState(verifierTypes32, verifierTypes, subroutineLocalsBitSet, set1);
                }

                System.arraycopy(verifierTypes1, 0, verifierTypes32, 0, lengthValue - 3);
                verifierTypes32[lengthValue3 - 4] = verifierTypes1[lengthValue - 1];
                verifierTypes32[lengthValue3 - 3] = verifierTypes1[lengthValue - 3];
                verifierTypes32[lengthValue3 - 2] = verifierTypes1[lengthValue - 2];
                verifierTypes32[lengthValue3 - 1] = verifierTypes1[lengthValue - 1];
                return new StackFrameState(verifierTypes32, verifierTypes, subroutineLocalsBitSet, set1);
            case 92:
                if (StackFrameState.isWideType(verifierTypes1[lengthValue - 1])) {
                    VerifierType[] verifierTypes31 = VerifierType.createArray(lengthValue + 1);
                    int lengthValue4 = verifierTypes31.length;
                    System.arraycopy(verifierTypes1, 0, verifierTypes31, 0, lengthValue);
                    verifierTypes31[lengthValue4 - 1] = verifierTypes1[lengthValue - 1];
                    return new StackFrameState(verifierTypes31, verifierTypes, subroutineLocalsBitSet, set1);
                }

                VerifierType[] verifierTypes30 = VerifierType.createArray(lengthValue + 2);
                int lengthValue5 = verifierTypes30.length;
                System.arraycopy(verifierTypes1, 0, verifierTypes30, 0, lengthValue);
                verifierTypes30[lengthValue5 - 2] = verifierTypes1[lengthValue - 2];
                verifierTypes30[lengthValue5 - 1] = verifierTypes1[lengthValue - 1];
                return new StackFrameState(verifierTypes30, verifierTypes, subroutineLocalsBitSet, set1);
            case 93:
                if (StackFrameState.isWideType(verifierTypes1[lengthValue - 1])) {
                    VerifierType[] verifierTypes29 = VerifierType.createArray(lengthValue + 1);
                    int lengthValue6 = verifierTypes29.length;
                    System.arraycopy(verifierTypes1, 0, verifierTypes29, 0, lengthValue - 2);
                    verifierTypes29[lengthValue6 - 3] = verifierTypes1[lengthValue - 1];
                    verifierTypes29[lengthValue6 - 2] = verifierTypes1[lengthValue - 2];
                    verifierTypes29[lengthValue6 - 1] = verifierTypes1[lengthValue - 1];
                    return new StackFrameState(verifierTypes29, verifierTypes, subroutineLocalsBitSet, set1);
                }

                VerifierType[] verifierTypes28 = VerifierType.createArray(lengthValue + 2);
                int lengthValue7 = verifierTypes28.length;
                System.arraycopy(verifierTypes1, 0, verifierTypes28, 0, lengthValue - 3);
                verifierTypes28[lengthValue7 - 5] = verifierTypes1[lengthValue - 2];
                verifierTypes28[lengthValue7 - 4] = verifierTypes1[lengthValue - 1];
                verifierTypes28[lengthValue7 - 3] = verifierTypes1[lengthValue - 3];
                verifierTypes28[lengthValue7 - 2] = verifierTypes1[lengthValue - 2];
                verifierTypes28[lengthValue7 - 1] = verifierTypes1[lengthValue - 1];
                return new StackFrameState(verifierTypes28, verifierTypes, subroutineLocalsBitSet, set1);
            case 94:
                if (StackFrameState.isWideType(verifierTypes1[lengthValue - 1])) {
                    if (StackFrameState.isWideType(verifierTypes1[lengthValue - 2])) {
                        VerifierType[] verifierTypes27 = VerifierType.createArray(lengthValue + 1);
                        int lengthValue8 = verifierTypes27.length;
                        System.arraycopy(verifierTypes1, 0, verifierTypes27, 0, lengthValue - 2);
                        verifierTypes27[lengthValue8 - 3] = verifierTypes1[lengthValue - 1];
                        verifierTypes27[lengthValue8 - 2] = verifierTypes1[lengthValue - 2];
                        verifierTypes27[lengthValue8 - 1] = verifierTypes1[lengthValue - 1];
                        return new StackFrameState(verifierTypes27, verifierTypes, subroutineLocalsBitSet, set1);
                    }

                    VerifierType[] verifierTypes26 = VerifierType.createArray(lengthValue + 1);
                    int lengthValue9 = verifierTypes26.length;
                    System.arraycopy(verifierTypes1, 0, verifierTypes26, 0, lengthValue - 3);
                    verifierTypes26[lengthValue9 - 4] = verifierTypes1[lengthValue - 1];
                    verifierTypes26[lengthValue9 - 3] = verifierTypes1[lengthValue - 3];
                    verifierTypes26[lengthValue9 - 2] = verifierTypes1[lengthValue - 2];
                    verifierTypes26[lengthValue9 - 1] = verifierTypes1[lengthValue - 1];
                    return new StackFrameState(verifierTypes26, verifierTypes, subroutineLocalsBitSet, set1);
                } else {
                    if (StackFrameState.isWideType(verifierTypes1[lengthValue - 3])) {
                        VerifierType[] verifierTypes25 = VerifierType.createArray(lengthValue + 2);
                        int lengthValue10 = verifierTypes25.length;
                        System.arraycopy(verifierTypes1, 0, verifierTypes25, 0, lengthValue - 3);
                        verifierTypes25[lengthValue10 - 5] = verifierTypes1[lengthValue - 2];
                        verifierTypes25[lengthValue10 - 4] = verifierTypes1[lengthValue - 1];
                        verifierTypes25[lengthValue10 - 3] = verifierTypes1[lengthValue - 3];
                        verifierTypes25[lengthValue10 - 2] = verifierTypes1[lengthValue - 2];
                        verifierTypes25[lengthValue10 - 1] = verifierTypes1[lengthValue - 1];
                        return new StackFrameState(verifierTypes25, verifierTypes, subroutineLocalsBitSet, set1);
                    }

                    VerifierType[] verifierTypes24 = VerifierType.createArray(lengthValue + 2);
                    int lengthValue11 = verifierTypes24.length;
                    System.arraycopy(verifierTypes1, 0, verifierTypes24, 0, lengthValue - 4);
                    verifierTypes24[lengthValue11 - 6] = verifierTypes1[lengthValue - 2];
                    verifierTypes24[lengthValue11 - 5] = verifierTypes1[lengthValue - 1];
                    verifierTypes24[lengthValue11 - 4] = verifierTypes1[lengthValue - 4];
                    verifierTypes24[lengthValue11 - 3] = verifierTypes1[lengthValue - 3];
                    verifierTypes24[lengthValue11 - 2] = verifierTypes1[lengthValue - 2];
                    verifierTypes24[lengthValue11 - 1] = verifierTypes1[lengthValue - 1];
                    return new StackFrameState(verifierTypes24, verifierTypes, subroutineLocalsBitSet, set1);
                }
            case 95:
                VerifierType[] verifierTypes23 = VerifierType.createArray(lengthValue);
                System.arraycopy(verifierTypes1, 0, verifierTypes23, 0, lengthValue - 2);
                verifierTypes23[lengthValue - 2] = verifierTypes1[lengthValue - 1];
                verifierTypes23[lengthValue - 1] = verifierTypes1[lengthValue - 2];
                return new StackFrameState(verifierTypes23, verifierTypes, subroutineLocalsBitSet, set1);
            case 96:
            case 100:
            case 104:
            case 108:
            case 112:
            case 120:
            case 122:
            case 124:
            case 126:
            case 128:
            case 130:
                VerifierType[] verifierTypes22 = VerifierType.createArray(lengthValue - 1);
                System.arraycopy(verifierTypes1, 0, verifierTypes22, 0, lengthValue - 2);
                verifierTypes22[lengthValue - 2] = VerifierType.INT;
                return new StackFrameState(verifierTypes22, verifierTypes, subroutineLocalsBitSet, set1);
            case 97:
            case 101:
            case 105:
            case 109:
            case 113:
            case 127:
            case 129:
            case 131:
                VerifierType[] verifierTypes21 = VerifierType.createArray(lengthValue - 1);
                System.arraycopy(verifierTypes1, 0, verifierTypes21, 0, lengthValue - 2);
                verifierTypes21[lengthValue - 2] = VerifierType.LONG;
                return new StackFrameState(verifierTypes21, verifierTypes, subroutineLocalsBitSet, set1);
            case 98:
            case 102:
            case 106:
            case 110:
            case 114:
                VerifierType[] verifierTypes20 = VerifierType.createArray(lengthValue - 1);
                System.arraycopy(verifierTypes1, 0, verifierTypes20, 0, lengthValue - 1);
                return new StackFrameState(verifierTypes20, verifierTypes, subroutineLocalsBitSet, set1);
            case 99:
            case 103:
            case 107:
            case 111:
            case 115:
                VerifierType[] verifierTypes19 = VerifierType.createArray(lengthValue - 1);
                System.arraycopy(verifierTypes1, 0, verifierTypes19, 0, lengthValue - 1);
                return new StackFrameState(verifierTypes19, verifierTypes, subroutineLocalsBitSet, set1);
            case 116:
            case 145:
            case 146:
            case 147:
                return new StackFrameState(verifierTypes1, verifierTypes, subroutineLocalsBitSet, set1);
            case 117:
                return new StackFrameState(verifierTypes1, verifierTypes, subroutineLocalsBitSet, set1);
            case 118:
                return new StackFrameState(verifierTypes1, verifierTypes, subroutineLocalsBitSet, set1);
            case 119:
                return new StackFrameState(verifierTypes1, verifierTypes, subroutineLocalsBitSet, set1);
            case 121:
            case 123:
            case 125:
                VerifierType[] verifierTypes18 = VerifierType.createArray(lengthValue - 1);
                System.arraycopy(verifierTypes1, 0, verifierTypes18, 0, lengthValue - 2);
                verifierTypes18[lengthValue - 2] = VerifierType.LONG;
                return new StackFrameState(verifierTypes18, verifierTypes, subroutineLocalsBitSet, set1);
            case 133:
                VerifierType[] verifierTypes17 = VerifierType.createArray(lengthValue);
                System.arraycopy(verifierTypes1, 0, verifierTypes17, 0, lengthValue - 1);
                verifierTypes17[lengthValue - 1] = VerifierType.LONG;
                return new StackFrameState(verifierTypes17, verifierTypes, subroutineLocalsBitSet, set1);
            case 134:
                VerifierType[] verifierTypes16 = VerifierType.createArray(lengthValue);
                System.arraycopy(verifierTypes1, 0, verifierTypes16, 0, lengthValue - 1);
                verifierTypes16[lengthValue - 1] = VerifierType.FLOAT;
                return new StackFrameState(verifierTypes16, verifierTypes, subroutineLocalsBitSet, set1);
            case 135:
                VerifierType[] verifierTypes15 = VerifierType.createArray(lengthValue);
                System.arraycopy(verifierTypes1, 0, verifierTypes15, 0, lengthValue - 1);
                verifierTypes15[lengthValue - 1] = VerifierType.DOUBLE;
                return new StackFrameState(verifierTypes15, verifierTypes, subroutineLocalsBitSet, set1);
            case 136:
                VerifierType[] verifierTypes14 = VerifierType.createArray(lengthValue);
                System.arraycopy(verifierTypes1, 0, verifierTypes14, 0, lengthValue - 1);
                verifierTypes14[lengthValue - 1] = VerifierType.INT;
                return new StackFrameState(verifierTypes14, verifierTypes, subroutineLocalsBitSet, set1);
            case 137:
                VerifierType[] verifierTypes13 = VerifierType.createArray(lengthValue);
                System.arraycopy(verifierTypes1, 0, verifierTypes13, 0, lengthValue - 1);
                verifierTypes13[lengthValue - 1] = VerifierType.FLOAT;
                return new StackFrameState(verifierTypes13, verifierTypes, subroutineLocalsBitSet, set1);
            case 138:
                VerifierType[] verifierTypes12 = VerifierType.createArray(lengthValue);
                System.arraycopy(verifierTypes1, 0, verifierTypes12, 0, lengthValue - 1);
                verifierTypes12[lengthValue - 1] = VerifierType.DOUBLE;
                return new StackFrameState(verifierTypes12, verifierTypes, subroutineLocalsBitSet, set1);
            case 139:
                VerifierType[] verifierTypes11 = VerifierType.createArray(lengthValue);
                System.arraycopy(verifierTypes1, 0, verifierTypes11, 0, lengthValue - 1);
                verifierTypes11[lengthValue - 1] = VerifierType.INT;
                return new StackFrameState(verifierTypes11, verifierTypes, subroutineLocalsBitSet, set1);
            case 140:
                VerifierType[] verifierTypes10 = VerifierType.createArray(lengthValue);
                System.arraycopy(verifierTypes1, 0, verifierTypes10, 0, lengthValue - 1);
                verifierTypes10[lengthValue - 1] = VerifierType.LONG;
                return new StackFrameState(verifierTypes10, verifierTypes, subroutineLocalsBitSet, set1);
            case 141:
                VerifierType[] verifierTypes9 = VerifierType.createArray(lengthValue);
                System.arraycopy(verifierTypes1, 0, verifierTypes9, 0, lengthValue - 1);
                verifierTypes9[lengthValue - 1] = VerifierType.DOUBLE;
                return new StackFrameState(verifierTypes9, verifierTypes, subroutineLocalsBitSet, set1);
            case 142:
                VerifierType[] verifierTypes8 = VerifierType.createArray(lengthValue);
                System.arraycopy(verifierTypes1, 0, verifierTypes8, 0, lengthValue - 1);
                verifierTypes8[lengthValue - 1] = VerifierType.INT;
                return new StackFrameState(verifierTypes8, verifierTypes, subroutineLocalsBitSet, set1);
            case 143:
                VerifierType[] verifierTypes7 = VerifierType.createArray(lengthValue);
                System.arraycopy(verifierTypes1, 0, verifierTypes7, 0, lengthValue - 1);
                verifierTypes7[lengthValue - 1] = VerifierType.LONG;
                return new StackFrameState(verifierTypes7, verifierTypes, subroutineLocalsBitSet, set1);
            case 144:
                VerifierType[] verifierTypes6 = VerifierType.createArray(lengthValue);
                System.arraycopy(verifierTypes1, 0, verifierTypes6, 0, lengthValue - 1);
                verifierTypes6[lengthValue - 1] = VerifierType.FLOAT;
                return new StackFrameState(verifierTypes6, verifierTypes, subroutineLocalsBitSet, set1);
            case 148:
                VerifierType[] verifierTypes5 = VerifierType.createArray(lengthValue - 1);
                int lengthValue12 = verifierTypes5.length;
                System.arraycopy(verifierTypes1, 0, verifierTypes5, 0, lengthValue - 2);
                verifierTypes5[lengthValue12 - 1] = VerifierType.INT;
                return new StackFrameState(verifierTypes5, verifierTypes, subroutineLocalsBitSet, set1);
            case 149:
            case 150:
                VerifierType[] verifierTypes4 = VerifierType.createArray(lengthValue - 1);
                int lengthValue13 = verifierTypes4.length;
                System.arraycopy(verifierTypes1, 0, verifierTypes4, 0, lengthValue - 2);
                verifierTypes4[lengthValue13 - 1] = VerifierType.INT;
                return new StackFrameState(verifierTypes4, verifierTypes, subroutineLocalsBitSet, set1);
            case 151:
            case 152:
                VerifierType[] verifierTypes3 = VerifierType.createArray(lengthValue - 1);
                int lengthValue14 = verifierTypes3.length;
                System.arraycopy(verifierTypes1, 0, verifierTypes3, 0, lengthValue - 2);
                verifierTypes3[lengthValue14 - 1] = VerifierType.INT;
                return new StackFrameState(verifierTypes3, verifierTypes, subroutineLocalsBitSet, set1);
            case 172:
                return new StackFrameState(verifierTypes, subroutineLocalsBitSet, set1);
            case 173:
                return new StackFrameState(verifierTypes, subroutineLocalsBitSet, set1);
            case 174:
                return new StackFrameState(verifierTypes, subroutineLocalsBitSet, set1);
            case 175:
                return new StackFrameState(verifierTypes, subroutineLocalsBitSet, set1);
            case 176:
                return new StackFrameState(verifierTypes, subroutineLocalsBitSet, set1);
            case 177:
                return new StackFrameState(verifierTypes, subroutineLocalsBitSet, set1);
            case 190:
                VerifierType[] verifierTypes2 = VerifierType.createArray(lengthValue);
                System.arraycopy(verifierTypes1, 0, verifierTypes2, 0, lengthValue - 1);
                verifierTypes2[lengthValue - 1] = VerifierType.INT;
                return new StackFrameState(verifierTypes2, verifierTypes, subroutineLocalsBitSet, set1);
            case 191:
                return new StackFrameState(verifierTypes1, verifierTypes, subroutineLocalsBitSet, set1);
        }
    }

    public StackFrameState popTwoPushType(
            int ba, Object sourceArray, VerifierType[] verifierTypes, VerifierType verifierType, SubroutineLocalsBitSet subroutineLocalsBitSet, Set set1
    ) {
        VerifierType[] verifierTypes1 = VerifierType.createArray(ba - 1);
        System.arraycopy(sourceArray, 0, verifierTypes1, 0, ba - 2);
        verifierTypes1[ba - 2] = verifierType;
        return new StackFrameState(verifierTypes1, verifierTypes, subroutineLocalsBitSet, set1);
    }

    @Override
    public int[] mapStackSlotForward(VerifierType[] verifierTypes, VerifierType[] verifierTypes1, int ba) {
        int verifierTypeIndex = verifierTypes.length - 1;
        int bc = verifierTypes1.length - 1;
        switch (this.opcode) {
            case 87:
            case 88:
                if (ba > bc) {
                    return new int[0];
                }
                break;
            case 89:
                if (ba == verifierTypeIndex) {
                    return new int[]{verifierTypeIndex, verifierTypeIndex + 1};
                }
                break;
            case 90:
                if (ba == verifierTypeIndex) {
                    return new int[]{verifierTypeIndex - 1, verifierTypeIndex + 1};
                }

                if (ba == verifierTypeIndex - 1) {
                    return new int[]{verifierTypeIndex};
                }
                break;
            case 91:
                if (verifierTypes[verifierTypeIndex - 1].isWide()) {
                    if (ba == verifierTypeIndex) {
                        return new int[]{verifierTypeIndex - 1, verifierTypeIndex + 1};
                    }

                    if (ba == verifierTypeIndex - 1) {
                        return new int[]{verifierTypeIndex};
                    }
                } else {
                    if (ba == verifierTypeIndex) {
                        return new int[]{verifierTypeIndex - 2, verifierTypeIndex + 1};
                    }

                    if (ba == verifierTypeIndex - 1 || ba == verifierTypeIndex - 2) {
                        return new int[]{ba + 1};
                    }
                }
                break;
            case 92:
                if (verifierTypes[verifierTypeIndex].isWide()) {
                    if (ba == verifierTypeIndex) {
                        return new int[]{ba, ba + 1};
                    }
                } else if (ba == verifierTypeIndex || ba == verifierTypeIndex - 1) {
                    return new int[]{ba, ba + 2};
                }
                break;
            case 93:
                if (verifierTypes[verifierTypeIndex].isWide()) {
                    if (ba == verifierTypeIndex) {
                        return new int[]{ba - 1, ba + 1};
                    }

                    if (ba == verifierTypeIndex - 1) {
                        return new int[]{ba + 1};
                    }
                } else {
                    if (ba == verifierTypeIndex || ba == verifierTypeIndex - 1) {
                        return new int[]{ba - 1, ba + 2};
                    }

                    if (ba == verifierTypeIndex - 2) {
                        return new int[]{ba + 2};
                    }
                }
                break;
            case 94:
                if (verifierTypes[verifierTypeIndex].isWide()) {
                    if (verifierTypes[verifierTypeIndex - 1].isWide()) {
                        if (ba == verifierTypeIndex) {
                            return new int[]{ba - 1, ba + 1};
                        }

                        if (ba == verifierTypeIndex - 1) {
                            return new int[]{ba + 1};
                        }
                    } else {
                        if (ba == verifierTypeIndex) {
                            return new int[]{ba - 2, ba + 1};
                        }

                        if (ba == verifierTypeIndex - 1 || ba == verifierTypeIndex - 2) {
                            return new int[]{ba + 1};
                        }
                    }
                } else if (verifierTypes[verifierTypeIndex - 2].isWide()) {
                    if (ba == verifierTypeIndex || ba == verifierTypeIndex - 1) {
                        return new int[]{ba - 1, ba + 2};
                    }

                    if (ba == verifierTypeIndex - 2) {
                        return new int[]{verifierTypeIndex};
                    }
                } else {
                    if (ba == verifierTypeIndex || ba == verifierTypeIndex - 1) {
                        return new int[]{ba - 2, ba + 2};
                    }

                    if (ba == verifierTypeIndex - 2 || ba == verifierTypeIndex - 3) {
                        return new int[]{ba + 2};
                    }
                }
                break;
            case 95:
                if (ba == verifierTypeIndex) {
                    return new int[]{verifierTypeIndex - 1};
                }

                if (ba == verifierTypeIndex - 1) {
                    return new int[]{verifierTypeIndex};
                }
                break;
            default:
                Integer integer = this.opcode;
                ZkmAssert.assertTrueWithCode(integer);
                return null;
        }

        return new int[]{ba};
    }

    public static SimpleInstruction forOpcode(int instancesByOpcodeElementIndex) {
        return INSTANCES_BY_OPCODE[instancesByOpcodeElementIndex];
    }

    @Override
    public boolean pushesValue() {
        switch (this.opcode) {
            case 0:
            case 79:
            case 80:
            case 81:
            case 82:
            case 83:
            case 84:
            case 85:
            case 86:
            case 87:
            case 88:
            case 89:
            case 90:
            case 91:
            case 92:
            case 93:
            case 94:
            case 95:
            case 172:
            case 173:
            case 174:
            case 175:
            case 176:
            case 177:
            case 191:
                return false;
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
            case 14:
            case 15:
            case 46:
            case 47:
            case 48:
            case 49:
            case 50:
            case 51:
            case 52:
            case 53:
            case 96:
            case 97:
            case 98:
            case 99:
            case 100:
            case 101:
            case 102:
            case 103:
            case 104:
            case 105:
            case 106:
            case 107:
            case 108:
            case 109:
            case 110:
            case 111:
            case 112:
            case 113:
            case 114:
            case 115:
            case 116:
            case 117:
            case 118:
            case 119:
            case 120:
            case 121:
            case 122:
            case 123:
            case 124:
            case 125:
            case 126:
            case 127:
            case 128:
            case 129:
            case 130:
            case 131:
            case 133:
            case 134:
            case 135:
            case 136:
            case 137:
            case 138:
            case 139:
            case 140:
            case 141:
            case 142:
            case 143:
            case 144:
            case 145:
            case 146:
            case 147:
            case 148:
            case 149:
            case 150:
            case 151:
            case 152:
            case 190:
                return true;
            case 16:
            case 17:
            case 18:
            case 19:
            case 20:
            case 21:
            case 22:
            case 23:
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
            case 64:
            case 65:
            case 66:
            case 67:
            case 68:
            case 69:
            case 70:
            case 71:
            case 72:
            case 73:
            case 74:
            case 75:
            case 76:
            case 77:
            case 78:
            case 132:
            case 153:
            case 154:
            case 155:
            case 156:
            case 157:
            case 158:
            case 159:
            case 160:
            case 161:
            case 162:
            case 163:
            case 164:
            case 165:
            case 166:
            case 167:
            case 168:
            case 169:
            case 170:
            case 171:
            case 178:
            case 179:
            case 180:
            case 181:
            case 182:
            case 183:
            case 184:
            case 185:
            case 186:
            case 187:
            case 188:
            case 189:
            default:
                Integer integer = this.opcode;
                ZkmAssert.assertTrueWithCode(integer);
                return false;
        }
    }

    static {
        INSTANCES_BY_OPCODE[0] = new SimpleInstruction(0);
        INSTANCES_BY_OPCODE[1] = new SimpleInstruction(1);
        INSTANCES_BY_OPCODE[2] = new SimpleInstruction(2);
        INSTANCES_BY_OPCODE[3] = new SimpleInstruction(3);
        INSTANCES_BY_OPCODE[4] = new SimpleInstruction(4);
        INSTANCES_BY_OPCODE[5] = new SimpleInstruction(5);
        INSTANCES_BY_OPCODE[6] = new SimpleInstruction(6);
        INSTANCES_BY_OPCODE[7] = new SimpleInstruction(7);
        INSTANCES_BY_OPCODE[8] = new SimpleInstruction(8);
        INSTANCES_BY_OPCODE[9] = new SimpleInstruction(9);
        INSTANCES_BY_OPCODE[10] = new SimpleInstruction(10);
        INSTANCES_BY_OPCODE[11] = new SimpleInstruction(11);
        INSTANCES_BY_OPCODE[12] = new SimpleInstruction(12);
        INSTANCES_BY_OPCODE[13] = new SimpleInstruction(13);
        INSTANCES_BY_OPCODE[14] = new SimpleInstruction(14);
        INSTANCES_BY_OPCODE[15] = new SimpleInstruction(15);
        INSTANCES_BY_OPCODE[46] = new SimpleInstruction(46);
        INSTANCES_BY_OPCODE[47] = new SimpleInstruction(47);
        INSTANCES_BY_OPCODE[48] = new SimpleInstruction(48);
        INSTANCES_BY_OPCODE[49] = new SimpleInstruction(49);
        INSTANCES_BY_OPCODE[50] = new SimpleInstruction(50);
        INSTANCES_BY_OPCODE[51] = new SimpleInstruction(51);
        INSTANCES_BY_OPCODE[52] = new SimpleInstruction(52);
        INSTANCES_BY_OPCODE[53] = new SimpleInstruction(53);
        INSTANCES_BY_OPCODE[79] = new SimpleInstruction(79);
        INSTANCES_BY_OPCODE[80] = new SimpleInstruction(80);
        INSTANCES_BY_OPCODE[81] = new SimpleInstruction(81);
        INSTANCES_BY_OPCODE[82] = new SimpleInstruction(82);
        INSTANCES_BY_OPCODE[83] = new SimpleInstruction(83);
        INSTANCES_BY_OPCODE[84] = new SimpleInstruction(84);
        INSTANCES_BY_OPCODE[85] = new SimpleInstruction(85);
        INSTANCES_BY_OPCODE[86] = new SimpleInstruction(86);
        INSTANCES_BY_OPCODE[87] = new SimpleInstruction(87);
        INSTANCES_BY_OPCODE[88] = new SimpleInstruction(88);
        INSTANCES_BY_OPCODE[89] = new SimpleInstruction(89);
        INSTANCES_BY_OPCODE[90] = new SimpleInstruction(90);
        INSTANCES_BY_OPCODE[91] = new SimpleInstruction(91);
        INSTANCES_BY_OPCODE[92] = new SimpleInstruction(92);
        INSTANCES_BY_OPCODE[93] = new SimpleInstruction(93);
        INSTANCES_BY_OPCODE[94] = new SimpleInstruction(94);
        INSTANCES_BY_OPCODE[95] = new SimpleInstruction(95);
        INSTANCES_BY_OPCODE[96] = new SimpleInstruction(96);
        INSTANCES_BY_OPCODE[97] = new SimpleInstruction(97);
        INSTANCES_BY_OPCODE[98] = new SimpleInstruction(98);
        INSTANCES_BY_OPCODE[99] = new SimpleInstruction(99);
        INSTANCES_BY_OPCODE[100] = new SimpleInstruction(100);
        INSTANCES_BY_OPCODE[101] = new SimpleInstruction(101);
        INSTANCES_BY_OPCODE[102] = new SimpleInstruction(102);
        INSTANCES_BY_OPCODE[103] = new SimpleInstruction(103);
        INSTANCES_BY_OPCODE[104] = new SimpleInstruction(104);
        INSTANCES_BY_OPCODE[105] = new SimpleInstruction(105);
        INSTANCES_BY_OPCODE[106] = new SimpleInstruction(106);
        INSTANCES_BY_OPCODE[107] = new SimpleInstruction(107);
        INSTANCES_BY_OPCODE[108] = new SimpleInstruction(108);
        INSTANCES_BY_OPCODE[109] = new SimpleInstruction(109);
        INSTANCES_BY_OPCODE[110] = new SimpleInstruction(110);
        INSTANCES_BY_OPCODE[111] = new SimpleInstruction(111);
        INSTANCES_BY_OPCODE[112] = new SimpleInstruction(112);
        INSTANCES_BY_OPCODE[113] = new SimpleInstruction(113);
        INSTANCES_BY_OPCODE[114] = new SimpleInstruction(114);
        INSTANCES_BY_OPCODE[115] = new SimpleInstruction(115);
        INSTANCES_BY_OPCODE[116] = new SimpleInstruction(116);
        INSTANCES_BY_OPCODE[117] = new SimpleInstruction(117);
        INSTANCES_BY_OPCODE[118] = new SimpleInstruction(118);
        INSTANCES_BY_OPCODE[119] = new SimpleInstruction(119);
        INSTANCES_BY_OPCODE[120] = new SimpleInstruction(120);
        INSTANCES_BY_OPCODE[121] = new SimpleInstruction(121);
        INSTANCES_BY_OPCODE[122] = new SimpleInstruction(122);
        INSTANCES_BY_OPCODE[123] = new SimpleInstruction(123);
        INSTANCES_BY_OPCODE[124] = new SimpleInstruction(124);
        INSTANCES_BY_OPCODE[125] = new SimpleInstruction(125);
        INSTANCES_BY_OPCODE[126] = new SimpleInstruction(126);
        INSTANCES_BY_OPCODE[127] = new SimpleInstruction(127);
        INSTANCES_BY_OPCODE[128] = new SimpleInstruction(128);
        INSTANCES_BY_OPCODE[129] = new SimpleInstruction(129);
        INSTANCES_BY_OPCODE[130] = new SimpleInstruction(130);
        INSTANCES_BY_OPCODE[131] = new SimpleInstruction(131);
        INSTANCES_BY_OPCODE[133] = new SimpleInstruction(133);
        INSTANCES_BY_OPCODE[134] = new SimpleInstruction(134);
        INSTANCES_BY_OPCODE[135] = new SimpleInstruction(135);
        INSTANCES_BY_OPCODE[136] = new SimpleInstruction(136);
        INSTANCES_BY_OPCODE[137] = new SimpleInstruction(137);
        INSTANCES_BY_OPCODE[138] = new SimpleInstruction(138);
        INSTANCES_BY_OPCODE[139] = new SimpleInstruction(139);
        INSTANCES_BY_OPCODE[140] = new SimpleInstruction(140);
        INSTANCES_BY_OPCODE[141] = new SimpleInstruction(141);
        INSTANCES_BY_OPCODE[142] = new SimpleInstruction(142);
        INSTANCES_BY_OPCODE[143] = new SimpleInstruction(143);
        INSTANCES_BY_OPCODE[144] = new SimpleInstruction(144);
        INSTANCES_BY_OPCODE[145] = new SimpleInstruction(145);
        INSTANCES_BY_OPCODE[146] = new SimpleInstruction(146);
        INSTANCES_BY_OPCODE[147] = new SimpleInstruction(147);
        INSTANCES_BY_OPCODE[148] = new SimpleInstruction(148);
        INSTANCES_BY_OPCODE[149] = new SimpleInstruction(149);
        INSTANCES_BY_OPCODE[150] = new SimpleInstruction(150);
        INSTANCES_BY_OPCODE[151] = new SimpleInstruction(151);
        INSTANCES_BY_OPCODE[152] = new SimpleInstruction(152);
        INSTANCES_BY_OPCODE[172] = new SimpleInstruction(172);
        INSTANCES_BY_OPCODE[173] = new SimpleInstruction(173);
        INSTANCES_BY_OPCODE[174] = new SimpleInstruction(174);
        INSTANCES_BY_OPCODE[175] = new SimpleInstruction(175);
        INSTANCES_BY_OPCODE[176] = new SimpleInstruction(176);
        INSTANCES_BY_OPCODE[177] = new SimpleInstruction(177);
        INSTANCES_BY_OPCODE[190] = new SimpleInstruction(190);
        INSTANCES_BY_OPCODE[191] = new SimpleInstruction(191);
    }

    @Override
    public final int mapStackSlotBackward(Object object, int ba) {
        VerifierType[] verifierTypes = (VerifierType[]) object;
        int verifierTypeIndex = verifierTypes.length - 1;
        switch (this.opcode) {
            case 87:
            case 88:
                return ba;
            case 89:
                if (ba == verifierTypeIndex) {
                    return ba - 1;
                }

                return ba;
            case 90:
                if (ba != verifierTypeIndex && ba != verifierTypeIndex - 1) {
                    if (ba == verifierTypeIndex - 2) {
                        return ba + 1;
                    }

                    return ba;
                }

                return ba - 1;
            case 91:
                if (verifierTypes[verifierTypeIndex - 1].isWide()) {
                    if (ba != verifierTypeIndex && ba != verifierTypeIndex - 1) {
                        if (ba == verifierTypeIndex - 2) {
                            return ba + 1;
                        }

                        return ba;
                    }

                    return ba - 1;
                } else {
                    if (ba != verifierTypeIndex && ba != verifierTypeIndex - 1 && ba != verifierTypeIndex - 2) {
                        if (ba == verifierTypeIndex - 3) {
                            return ba + 2;
                        }

                        return ba;
                    }

                    return ba - 1;
                }
            case 92:
                if (verifierTypes[verifierTypeIndex].isWide()) {
                    if (ba == verifierTypeIndex) {
                        return ba - 1;
                    }

                    return ba;
                } else {
                    if (ba != verifierTypeIndex && ba != verifierTypeIndex - 1) {
                        return ba;
                    }

                    return ba - 2;
                }
            case 93:
                if (verifierTypes[verifierTypeIndex].isWide()) {
                    if (ba != verifierTypeIndex && ba != verifierTypeIndex - 1) {
                        if (ba == verifierTypeIndex - 2) {
                            return ba + 1;
                        }

                        return ba;
                    }

                    return ba - 1;
                } else {
                    if (ba != verifierTypeIndex && ba != verifierTypeIndex - 1 && ba != verifierTypeIndex - 2) {
                        if (ba != verifierTypeIndex - 3 && ba != verifierTypeIndex - 4) {
                            return ba;
                        }

                        return ba + 1;
                    }

                    return ba - 2;
                }
            case 94:
                if (verifierTypes[verifierTypeIndex].isWide()) {
                    if (verifierTypes[verifierTypeIndex - 1].isWide()) {
                        if (ba != verifierTypeIndex && ba != verifierTypeIndex - 1) {
                            if (ba == verifierTypeIndex - 2) {
                                return ba + 1;
                            }

                            return ba;
                        }

                        return ba - 1;
                    } else {
                        if (ba != verifierTypeIndex && ba != verifierTypeIndex - 1 && ba != verifierTypeIndex - 2) {
                            if (ba == verifierTypeIndex - 3) {
                                return ba + 2;
                            }

                            return ba;
                        }

                        return ba - 1;
                    }
                } else if (verifierTypes[verifierTypeIndex - 2].isWide()) {
                    if (ba != verifierTypeIndex && ba != verifierTypeIndex - 1 && ba != verifierTypeIndex - 2) {
                        if (ba != verifierTypeIndex - 3 && ba != verifierTypeIndex - 4) {
                            return ba;
                        }

                        return ba + 1;
                    }

                    return ba - 2;
                } else {
                    if (ba != verifierTypeIndex && ba != verifierTypeIndex - 1 && ba != verifierTypeIndex - 2 && ba != verifierTypeIndex - 3) {
                        if (ba != verifierTypeIndex - 4 && ba != verifierTypeIndex - 5) {
                            return ba;
                        }

                        return ba + 2;
                    }

                    return ba - 2;
                }
            case 95:
                if (ba == verifierTypeIndex) {
                    return verifierTypeIndex - 1;
                } else {
                    if (ba == verifierTypeIndex - 1) {
                        return verifierTypeIndex;
                    }

                    return ba;
                }
            default:
                ZkmAssert.assertTrue(false, new String[]{this.opcode + " " + this.getMnemonic()});
                return ba;
        }
    }

    @Override
    public boolean isLongConstant() {
        switch (this.opcode) {
            case 9:
            case 10:
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean pushesWithoutPopping() {
        switch (this.opcode) {
            case 0:
            case 79:
            case 80:
            case 81:
            case 82:
            case 83:
            case 84:
            case 85:
            case 86:
            case 87:
            case 88:
            case 89:
            case 90:
            case 91:
            case 92:
            case 93:
            case 94:
            case 95:
            case 172:
            case 173:
            case 174:
            case 175:
            case 176:
            case 177:
            case 191:
                return false;
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
            case 14:
            case 15:
                return true;
            case 16:
            case 17:
            case 18:
            case 19:
            case 20:
            case 21:
            case 22:
            case 23:
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
            case 64:
            case 65:
            case 66:
            case 67:
            case 68:
            case 69:
            case 70:
            case 71:
            case 72:
            case 73:
            case 74:
            case 75:
            case 76:
            case 77:
            case 78:
            case 132:
            case 153:
            case 154:
            case 155:
            case 156:
            case 157:
            case 158:
            case 159:
            case 160:
            case 161:
            case 162:
            case 163:
            case 164:
            case 165:
            case 166:
            case 167:
            case 168:
            case 169:
            case 170:
            case 171:
            case 178:
            case 179:
            case 180:
            case 181:
            case 182:
            case 183:
            case 184:
            case 185:
            case 186:
            case 187:
            case 188:
            case 189:
            default:
                Integer integer = this.opcode;
                ZkmAssert.assertTrueWithCode(integer);
                return false;
            case 46:
            case 47:
            case 48:
            case 49:
            case 50:
            case 51:
            case 52:
            case 53:
            case 96:
            case 97:
            case 98:
            case 99:
            case 100:
            case 101:
            case 102:
            case 103:
            case 104:
            case 105:
            case 106:
            case 107:
            case 108:
            case 109:
            case 110:
            case 111:
            case 112:
            case 113:
            case 114:
            case 115:
            case 116:
            case 117:
            case 118:
            case 119:
            case 120:
            case 121:
            case 122:
            case 123:
            case 124:
            case 125:
            case 126:
            case 127:
            case 128:
            case 129:
            case 130:
            case 131:
            case 133:
            case 134:
            case 135:
            case 136:
            case 137:
            case 138:
            case 139:
            case 140:
            case 141:
            case 142:
            case 143:
            case 144:
            case 145:
            case 146:
            case 147:
            case 148:
            case 149:
            case 150:
            case 151:
            case 152:
            case 190:
                return false;
        }
    }

    @Override
    public String toAssembly() {
        return this.getMnemonic();
    }

    @Override
    public boolean isReturn() {
        switch (this.opcode) {
            case 172:
            case 173:
            case 174:
            case 175:
            case 176:
            case 177:
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean pushesWideValue() {
        switch (this.opcode) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
            case 8:
            case 11:
            case 12:
            case 13:
            case 46:
            case 48:
            case 50:
            case 51:
            case 52:
            case 53:
            case 79:
            case 80:
            case 81:
            case 82:
            case 83:
            case 84:
            case 85:
            case 86:
            case 87:
            case 88:
            case 89:
            case 90:
            case 91:
            case 92:
            case 93:
            case 94:
            case 95:
            case 96:
            case 98:
            case 100:
            case 102:
            case 104:
            case 106:
            case 108:
            case 110:
            case 112:
            case 114:
            case 116:
            case 118:
            case 120:
            case 122:
            case 124:
            case 126:
            case 128:
            case 130:
            case 134:
            case 136:
            case 137:
            case 139:
            case 142:
            case 144:
            case 145:
            case 146:
            case 147:
            case 148:
            case 149:
            case 150:
            case 151:
            case 152:
            case 172:
            case 173:
            case 174:
            case 175:
            case 176:
            case 177:
            case 190:
            case 191:
                return false;
            case 9:
            case 10:
            case 14:
            case 15:
            case 47:
            case 49:
            case 97:
            case 99:
            case 101:
            case 103:
            case 105:
            case 107:
            case 109:
            case 111:
            case 113:
            case 115:
            case 117:
            case 119:
            case 121:
            case 123:
            case 125:
            case 127:
            case 129:
            case 131:
            case 133:
            case 135:
            case 138:
            case 140:
            case 141:
            case 143:
                return true;
            case 16:
            case 17:
            case 18:
            case 19:
            case 20:
            case 21:
            case 22:
            case 23:
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
            case 64:
            case 65:
            case 66:
            case 67:
            case 68:
            case 69:
            case 70:
            case 71:
            case 72:
            case 73:
            case 74:
            case 75:
            case 76:
            case 77:
            case 78:
            case 132:
            case 153:
            case 154:
            case 155:
            case 156:
            case 157:
            case 158:
            case 159:
            case 160:
            case 161:
            case 162:
            case 163:
            case 164:
            case 165:
            case 166:
            case 167:
            case 168:
            case 169:
            case 170:
            case 171:
            case 178:
            case 179:
            case 180:
            case 181:
            case 182:
            case 183:
            case 184:
            case 185:
            case 186:
            case 187:
            case 188:
            case 189:
            default:
                Integer integer = this.opcode;
                ZkmAssert.assertTrueWithCode(integer);
                return false;
        }
    }

    @Override
    public final boolean requiresTypedValueAt(Object object, Object object2, Object object1) {
        int ba = (Integer) object;
        int bb = (Integer) object1;
        switch (this.opcode) {
            case 0:
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
            case 14:
            case 15:
            case 38:
            case 89:
            case 90:
            case 91:
            case 92:
            case 93:
            case 94:
            case 95:
                return false;
            case 16:
            case 17:
            case 18:
            case 19:
            case 20:
            case 21:
            case 22:
            case 23:
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
            case 39:
            case 40:
            case 41:
            case 42:
            case 43:
            case 44:
            case 45:
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
            case 64:
            case 65:
            case 66:
            case 67:
            case 68:
            case 69:
            case 70:
            case 71:
            case 72:
            case 73:
            case 74:
            case 75:
            case 76:
            case 77:
            case 78:
            case 132:
            case 153:
            case 154:
            case 155:
            case 156:
            case 157:
            case 158:
            case 159:
            case 160:
            case 161:
            case 162:
            case 163:
            case 164:
            case 165:
            case 166:
            case 167:
            case 168:
            case 169:
            case 170:
            case 171:
            case 178:
            case 179:
            case 180:
            case 181:
            case 182:
            case 183:
            case 184:
            case 185:
            case 186:
            case 187:
            case 188:
            case 189:
            default:
                Integer integer = this.opcode;
                ZkmAssert.assertTrueWithCode(integer);
                return false;
            case 46:
            case 48:
            case 50:
            case 51:
            case 52:
            case 53:
            case 96:
            case 98:
            case 100:
            case 102:
            case 104:
            case 106:
            case 108:
            case 110:
            case 112:
            case 114:
            case 116:
            case 118:
            case 120:
            case 122:
            case 124:
            case 126:
            case 128:
            case 130:
            case 134:
            case 136:
            case 137:
            case 139:
            case 142:
            case 144:
            case 145:
            case 146:
            case 147:
            case 148:
            case 149:
            case 150:
            case 151:
            case 152:
            case 190:
                return ba >= bb - 1;
            case 47:
            case 49:
            case 97:
            case 99:
            case 101:
            case 103:
            case 105:
            case 107:
            case 109:
            case 111:
            case 113:
            case 115:
            case 117:
            case 119:
            case 121:
            case 123:
            case 125:
            case 127:
            case 129:
            case 131:
            case 133:
            case 135:
            case 138:
            case 140:
            case 141:
            case 143:
                return ba >= bb - 2;
            case 79:
            case 80:
            case 81:
            case 82:
            case 83:
            case 84:
            case 85:
            case 86:
                return ba >= bb;
            case 87:
            case 88:
                return ba >= bb;
            case 172:
            case 173:
            case 174:
            case 175:
            case 176:
            case 191:
                return true;
            case 177:
                return false;
        }
    }

    @Override
    public boolean consumesStackSlot(int ba, int bb) {
        switch (this.opcode) {
            case 0:
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
            case 14:
            case 15:
            case 89:
            case 90:
            case 91:
            case 92:
            case 93:
            case 94:
            case 95:
                return false;
            case 16:
            case 17:
            case 18:
            case 19:
            case 20:
            case 21:
            case 22:
            case 23:
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
            case 64:
            case 65:
            case 66:
            case 67:
            case 68:
            case 69:
            case 70:
            case 71:
            case 72:
            case 73:
            case 74:
            case 75:
            case 76:
            case 77:
            case 78:
            case 132:
            case 153:
            case 154:
            case 155:
            case 156:
            case 157:
            case 158:
            case 159:
            case 160:
            case 161:
            case 162:
            case 163:
            case 164:
            case 165:
            case 166:
            case 167:
            case 168:
            case 169:
            case 170:
            case 171:
            case 178:
            case 179:
            case 180:
            case 181:
            case 182:
            case 183:
            case 184:
            case 185:
            case 186:
            case 187:
            case 188:
            case 189:
            default:
                Integer integer = this.opcode;
                ZkmAssert.assertTrueWithCode(integer);
                return false;
            case 46:
            case 48:
            case 50:
            case 51:
            case 52:
            case 53:
            case 96:
            case 98:
            case 100:
            case 102:
            case 104:
            case 106:
            case 108:
            case 110:
            case 112:
            case 114:
            case 116:
            case 118:
            case 120:
            case 122:
            case 124:
            case 126:
            case 128:
            case 130:
            case 134:
            case 136:
            case 137:
            case 139:
            case 142:
            case 144:
            case 145:
            case 146:
            case 147:
            case 148:
            case 149:
            case 150:
            case 151:
            case 152:
            case 190:
                return ba >= bb - 1;
            case 47:
            case 49:
            case 97:
            case 99:
            case 101:
            case 103:
            case 105:
            case 107:
            case 109:
            case 111:
            case 113:
            case 115:
            case 117:
            case 119:
            case 121:
            case 123:
            case 125:
            case 127:
            case 129:
            case 131:
            case 133:
            case 135:
            case 138:
            case 140:
            case 141:
            case 143:
                return ba >= bb - 2;
            case 79:
            case 80:
            case 81:
            case 82:
            case 83:
            case 84:
            case 85:
            case 86:
            case 87:
            case 88:
                return ba >= bb;
            case 172:
            case 173:
            case 174:
            case 175:
            case 176:
            case 177:
            case 191:
                return true;
        }
    }

    @Override
    public final boolean isIntConstantPush() {
        switch (this.opcode) {
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
            case 8:
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean isStackManipulation() {
        switch (this.opcode) {
            case 87:
            case 88:
            case 89:
            case 90:
            case 91:
            case 92:
            case 93:
            case 94:
            case 95:
                return true;
            default:
                return false;
        }
    }
}

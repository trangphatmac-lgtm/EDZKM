package com.zelix.klassmaster.classfile.insn;

import com.zelix.klassmaster.util.SoftReferenceMap;
import com.zelix.klassmaster.util.ZkmUtils;

import java.util.Map;

public class VerifierType {
    private static Map predefinedTypes = ZkmUtils.createHashMap();
    private static SoftReferenceMap typeCache = new SoftReferenceMap("TO");
    private static VerifierType[] emptyArray = new VerifierType[0];
    public static final VerifierType BYTE;
    public static final VerifierType BOOLEAN;
    public static final VerifierType CHAR;
    public static final VerifierType SHORT;
    public static final VerifierType INT;
    public static final VerifierType LONG;
    public static final VerifierType FLOAT;
    public static final VerifierType DOUBLE;
    public static final VerifierType TOP;
    public static final VerifierType NULL;
    public static final VerifierType WIDE_SECOND_SLOT;
    public static final VerifierType RETURN_ADDRESS;
    public static final VerifierType STRING;
    public static final VerifierType CLASS;
    public static final VerifierType BYTE_ARRAY;
    public static final VerifierType BOOLEAN_ARRAY;
    public static final VerifierType CHAR_ARRAY;
    public static final VerifierType SHORT_ARRAY;
    public static final VerifierType INT_ARRAY;
    public static final VerifierType LONG_ARRAY;
    public static final VerifierType FLOAT_ARRAY;
    public static final VerifierType DOUBLE_ARRAY;
    private final String descriptor;

    public boolean isInitialized() {
        return true;
    }

    public VerifierType(String descriptorArgument) {
        this.descriptor = descriptorArgument;
    }

    public static VerifierType forDescriptor(String key) {
        return forDescriptor(key, true);
    }

    public static VerifierType[] createArray(int ba) {
        return ba == 0 ? emptyArray : new VerifierType[ba];
    }

    public VerifierType getElementType() {
        if (this.descriptor.charAt(0) == '[') {
            return forDescriptor(this.descriptor.substring(1));
        } else {
            return this.descriptor.startsWith("n") ? NULL : null;
        }
    }

    public String toDisplayString() {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(this.descriptor);
        if (!this.isInitialized()) {
            if (this.isUninitializedThis()) {
                stringBuffer.append("**");
            } else {
                stringBuffer.append("*");
            }
        }

        return stringBuffer.toString();
    }

    public boolean descriptorStartsWith(String prefix) {
        return this.descriptor.startsWith(prefix);
    }

    public static VerifierType forDescriptor(String key, boolean bl) {
        return resolveType(key, bl, null);
    }

    public int getSlotSize() {
        return !this.descriptor.equals("J") && !this.descriptor.equals("D") ? 1 : 2;
    }

    public boolean isTop() {
        return this.descriptor.equals("?");
    }

    public boolean isClassType() {
        return this.descriptor.startsWith("L") && this.descriptor.endsWith(";");
    }

    public VerifierType createCopy() {
        return new VerifierType(this.descriptor);
    }

    @Override
    public int hashCode() {
        return this.descriptor.hashCode();
    }

    public boolean isIntLike() {
        return this == BYTE || this == BOOLEAN || this == CHAR || this == SHORT || this == INT;
    }

    public boolean isNull() {
        return this.descriptor.equals("n");
    }

    public boolean isArray() {
        return this.descriptor.charAt(0) == '[';
    }

    public boolean hasDescriptor(String string) {
        return this.descriptor.equals(string);
    }


    static {
        predefinedTypes.put("B", new VerifierType("B"));
        predefinedTypes.put("Z", new VerifierType("Z"));
        predefinedTypes.put("C", new VerifierType("C"));
        predefinedTypes.put("S", new VerifierType("S"));
        predefinedTypes.put("I", new VerifierType("I"));
        predefinedTypes.put("F", new VerifierType("F"));
        predefinedTypes.put("J", new VerifierType("J"));
        predefinedTypes.put("D", new VerifierType("D"));
        predefinedTypes.put("?", new VerifierType("?"));
        predefinedTypes.put("n", new VerifierType("n"));
        predefinedTypes.put("~", new VerifierType("~"));
        predefinedTypes.put("[B", new VerifierType("[B"));
        predefinedTypes.put("[Z", new VerifierType("[Z"));
        predefinedTypes.put("[C", new VerifierType("[C"));
        predefinedTypes.put("[S", new VerifierType("[S"));
        predefinedTypes.put("[I", new VerifierType("[I"));
        predefinedTypes.put("[F", new VerifierType("[F"));
        predefinedTypes.put("[J", new VerifierType("[J"));
        predefinedTypes.put("[D", new VerifierType("[D"));
        predefinedTypes.put("RETURN_ADDRESS", new VerifierType("RETURN_ADDRESS"));
        BYTE = forDescriptor("B");
        BOOLEAN = forDescriptor("Z");
        CHAR = forDescriptor("C");
        SHORT = forDescriptor("S");
        INT = forDescriptor("I");
        LONG = forDescriptor("J");
        FLOAT = forDescriptor("F");
        DOUBLE = forDescriptor("D");
        TOP = forDescriptor("?");
        NULL = forDescriptor("n");
        WIDE_SECOND_SLOT = forDescriptor("~");
        RETURN_ADDRESS = forDescriptor("RETURN_ADDRESS");
        forDescriptor("Ljava/lang/Throwable;");
        forDescriptor("Ljava/lang/Object;");
        STRING = forDescriptor("Ljava/lang/String;");
        forDescriptor("Ljava/lang/Cloneable;");
        CLASS = forDescriptor("Ljava/lang/Class;");
        BYTE_ARRAY = forDescriptor("[B");
        BOOLEAN_ARRAY = forDescriptor("[Z");
        CHAR_ARRAY = forDescriptor("[C");
        SHORT_ARRAY = forDescriptor("[S");
        INT_ARRAY = forDescriptor("[I");
        LONG_ARRAY = forDescriptor("[J");
        FLOAT_ARRAY = forDescriptor("[F");
        DOUBLE_ARRAY = forDescriptor("[D");
    }


    public boolean isWideSecondSlot() {
        return this == WIDE_SECOND_SLOT;
    }

    public boolean isWide() {
        return this == LONG || this == DOUBLE;
    }

    public boolean isUninitializedThis() {
        return !this.isInitialized();
    }

    public boolean descriptorEndsWith() {
        return this.descriptor.endsWith(";");
    }

    public static VerifierType resolveType(String key, boolean bl, TypeInstruction typeInstruction) {
        VerifierType verifierType = null;
        if (key.length() <= 2 || key.equals("RETURN_ADDRESS")) {
            verifierType = (VerifierType) predefinedTypes.get(key);
        }

        if (verifierType == null) {
            if (typeInstruction == null) {
                if (bl) {
                    verifierType = (VerifierType) typeCache.get(key);
                    if (verifierType == null) {
                        verifierType = new VerifierType(key);
                        typeCache.put(key, verifierType);
                    }
                } else {
                    verifierType = new UninitializedThisValueType(key);
                }
            } else {
                verifierType = new UninitializedType(key, typeInstruction);
            }
        }

        return verifierType;
    }

    public String getDescriptor() {
        return this.descriptor;
    }

    @Override
    public boolean equals(Object other) {
        boolean strictMergeDisabled = StackFrameState.isStrictMergeDisabled();
        boolean isVerifierType = other instanceof VerifierType;
        if (strictMergeDisabled) {
            return isVerifierType;
        }
        if (!isVerifierType) {
            return false;
        }
        VerifierType otherType = (VerifierType) other;
        if (!otherType.descriptor.equals(this.descriptor)) {
            return false;
        }
        return otherType.isInitialized() == this.isInitialized();
    }

    public static boolean isWideDescriptor(String string) {
        if (string.length() == 1) {
            switch (string.charAt(0)) {
                case 'B':
                case 'C':
                case 'F':
                case 'I':
                case 'S':
                case 'Z':
                    return false;
                case 'D':
                case 'J':
                    return true;
                case 'E':
                case 'G':
                case 'H':
                case 'K':
                case 'L':
                case 'M':
                case 'N':
                case 'O':
                case 'P':
                case 'Q':
                case 'R':
                case 'T':
                case 'U':
                case 'V':
                case 'W':
                case 'X':
                case 'Y':
                default:
                    return false;
            }
        } else {
            return false;
        }
    }
}

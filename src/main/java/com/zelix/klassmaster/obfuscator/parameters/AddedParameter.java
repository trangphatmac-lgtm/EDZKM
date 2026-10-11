package com.zelix.klassmaster.obfuscator.parameters;

public class AddedParameter {
    public static final AddedParameter[] EMPTY_ARRAY = new AddedParameter[0];
    private final int index;
    public final char typeChar;

    public int getIndex() {
        return this.index;
    }

    public AddedParameter(int index, char typeChar) {
        this.index = index;
        this.typeChar = typeChar;
    }

    @Override
    public boolean equals(Object otherArgument) {
        boolean predicateFlagClear = OpaquePredicateBase.isPredicateFlagClear();
        if (!(otherArgument instanceof AddedParameter)) {
            return false;
        }
        if (predicateFlagClear) {
            return true;
        }
        AddedParameter other = (AddedParameter) otherArgument;
        return this.index == other.index && this.typeChar == other.typeChar;
    }

    @Override
    public int hashCode() {
        return this.index + this.typeChar;
    }

    public int getBitWidth() {
        switch (this.typeChar) {
            case 'B':
                return 8;
            case 'C':
            case 'S':
                return 16;
            case 'I':
                return 32;
            default:
                return 0;
        }
    }

    public char getTypeChar() {
        return this.typeChar;
    }
}

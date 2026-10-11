package com.zelix.klassmaster.classfile.insn;

import com.zelix.klassmaster.classfile.constpool.ResolvedStringConstant;

public class StringConstantNameRef implements TrackedValue {
    private static String nullText;
    private static long separatorCode;
    public ResolvedStringConstant stringConstant;
    public boolean isFullSignature;
    public String accessorPrefix;

    @Override
    public String getCombinedValueKey() {
        return this.getStringValue();
    }

    @Override
    public boolean isConstant() {
        return true;
    }

    public StringConstantNameRef(ResolvedStringConstant resolvedStringConstant, boolean bl) {
        this(resolvedStringConstant, true, null);
    }

    private StringConstantNameRef(ResolvedStringConstant resolvedStringConstant, boolean isFullSignature, String accessorPrefixArgument) {
        if (resolvedStringConstant == null) {
            throw new IllegalArgumentException();
        }

        this.stringConstant = resolvedStringConstant;
        this.isFullSignature = isFullSignature;
        this.accessorPrefix = accessorPrefixArgument;
    }

    public final ResolvedStringConstant getStringConstant() {
        return this.stringConstant;
    }

    @Override
    public boolean equals(Object other) {
        boolean strictMergeDisabled = StackFrameState.isStrictMergeDisabled();
        if (other != null) {
            boolean bl1 = this.getClass().equals(other.getClass());
            if (strictMergeDisabled) {
                return bl1;
            }

            if (bl1) {
                StringConstantNameRef stringConstantNameRef1 = (StringConstantNameRef) other;
                StringConstantNameRef stringConstantNameRef2 = this;
                if (!strictMergeDisabled) {
                    if (this.stringConstant != null) {
                        ResolvedStringConstant resolvedStringConstant;
                        if (!strictMergeDisabled) {
                            if (stringConstantNameRef1.stringConstant == null) {
                                return false;
                            }

                            resolvedStringConstant = this.stringConstant;
                        } else {
                            resolvedStringConstant = stringConstantNameRef1.stringConstant;
                        }

                        return resolvedStringConstant.equals(stringConstantNameRef1.stringConstant);
                    }

                    stringConstantNameRef2 = stringConstantNameRef1;
                }

                return stringConstantNameRef2.stringConstant == null;
            }
        }

        return false;
    }

    @Override
    public final String getDisplayText() {
        return this.stringConstant != null ? this.getNormalizedName() : nullText;
    }

    @Override
    public int hashCode() {
        boolean strictMergeEnabled = StackFrameState.isStrictMergeEnabled();
        ResolvedStringConstant resolvedStringConstant;
        if (strictMergeEnabled) {
            if (this.stringConstant == null) {
                return 0;
            }

            resolvedStringConstant = this.stringConstant;
        } else {
            resolvedStringConstant = this.stringConstant;
        }

        return resolvedStringConstant.hashCode();
    }

    @Override
    public boolean isPending() {
        return false;
    }

    public boolean hasAccessorPrefix() {
        return this.accessorPrefix != null && this.accessorPrefix.length() > 0;
    }

    @Override
    public int getPendingInstructionIndex() {
        return -1;
    }

    public boolean isFullSignature() {
        return this.isFullSignature;
    }

    @Override
    public String getNormalizedName() {
        String stringValue = this.getStringValue();
        if (stringValue == null) {
            return null;
        }

        int ba = stringValue.lastIndexOf((int) separatorCode);
        if (ba > -1) {
            stringValue = stringValue.substring(ba + 1);
        }

        if (stringValue.startsWith("L") && stringValue.endsWith(";")) {
            stringValue = stringValue.substring(1, stringValue.length() - 1);
        }

        return stringValue;
    }

    public StringConstantNameRef(ResolvedStringConstant resolvedStringConstant) {
        this(resolvedStringConstant, false, null);
    }

    @Override
    public final String getStringValue() {
        return this.stringConstant != null ? this.stringConstant.getEditableValue() : null;
    }

    public String getAccessorPrefix() {
        return this.accessorPrefix;
    }

    @Override
    public boolean isKnown() {
        return true;
    }

    public StringConstantNameRef(ResolvedStringConstant resolvedStringConstant, String accessorPrefixArgument) {
        this(resolvedStringConstant, false, accessorPrefixArgument);
    }

    static {
        try {
            staticInit();
        } catch (Exception exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private static void staticInit() {
        nullText = "null";
        separatorCode = 8366799268467441755L;
    }
}

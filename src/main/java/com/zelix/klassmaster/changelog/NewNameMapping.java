package com.zelix.klassmaster.changelog;

public class NewNameMapping {
    public final String newName;
    public final String newParameterTypes;
    public final String parameterChangeData;
    public final boolean signatureChanged;
    public String paramChangeNodeData;
    public final boolean parametersObfuscated;
    private final boolean manufactured;

    public String getEffectiveParameterTypes() {
        return this.parametersObfuscated ? "java.lang.Object[]" : this.newParameterTypes;
    }

    public boolean hasParamChangeNodeData() {
        return this.paramChangeNodeData != null;
    }

    public boolean isSignatureChanged() {
        return this.signatureChanged;
    }

    public String getParamChangeNodeData() {
        return this.paramChangeNodeData;
    }

    public String getParameterChangeData() {
        return this.parameterChangeData;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }

        boolean bl;
        if (other instanceof NewNameMapping) {
            NewNameMapping newNameMapping1 = (NewNameMapping) other;
            if (this.newName.equals(newNameMapping1.newName) && this.newParameterTypes.equals(newNameMapping1.newParameterTypes)) {
                if ((this.parameterChangeData == null || newNameMapping1.parameterChangeData != null)
                        && (this.parameterChangeData != null || newNameMapping1.parameterChangeData == null)
                        && (
                        this.parameterChangeData == null
                                || newNameMapping1.parameterChangeData == null
                                || this.parameterChangeData.equals(newNameMapping1.parameterChangeData)
                )) {
                    bl = this.parametersObfuscated == newNameMapping1.parametersObfuscated;
                } else {
                    bl = false;
                }
            } else {
                bl = false;
            }
        } else {
            bl = false;
        }

        return bl;
    }

    public NewNameMapping(String newNameArgument, String newParameterTypesArgument, boolean parametersObfuscated, String parameterChangeDataArgument, String paramChangeNodeDataArgument, boolean signatureChanged, boolean manufactured) {
        this.newName = newNameArgument;
        this.newParameterTypes = newParameterTypesArgument;
        this.parameterChangeData = parameterChangeDataArgument;
        this.signatureChanged = signatureChanged;
        this.paramChangeNodeData = paramChangeNodeDataArgument;
        this.parametersObfuscated = parametersObfuscated;
        this.manufactured = manufactured;
    }

    public boolean isParametersObfuscated() {
        return this.parametersObfuscated;
    }

    @Override
    public int hashCode() {
        int ba = this.newName.hashCode() ^ this.newParameterTypes.hashCode();
        if (this.parameterChangeData != null) {
            ba ^= this.parameterChangeData.hashCode();
        }

        if (this.parametersObfuscated) {
            ba ^= "[Ljava/lang/Object;".hashCode();
        }

        return ba;
    }

    public NewNameMapping(String newNameArgument) {
        this(newNameArgument, "", false, null, null, false, false);
    }

    public boolean hasParameterChangeData() {
        return this.parameterChangeData != null;
    }

    public NewNameMapping(String newNameArgument, String newParameterTypesArgument) {
        this(newNameArgument, newParameterTypesArgument, false, null, null, false, false);
    }

    public final String getNameWithParameters() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(this.newName);
        if (this.hasParameterChangeData() || this.newParameterTypes != null && this.newParameterTypes.length() > 0) {
            stringBuilder.append('(');
            stringBuilder.append(this.newParameterTypes);
            stringBuilder.append(")");
        }

        return stringBuilder.toString();
    }

    public String getNewParameterTypes() {
        return this.newParameterTypes;
    }

    public String getNewName() {
        return this.newName;
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

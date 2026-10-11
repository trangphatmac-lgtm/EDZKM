package com.zelix.klassmaster.changelog;

public class MethodNameAndArgs implements Comparable {
    private static final String UNKNOWN_ARGS = "unknown";
    public String cachedDescription;
    public String methodName;
    public String[] argumentTypes;

    public String getMethodName() {
        return this.methodName;
    }

    public MethodNameAndArgs(String methodNameArgument, String[] argumentTypesArgument) {
        this.methodName = methodNameArgument;
        this.argumentTypes = argumentTypesArgument;
    }

    @Override
    public int compareTo(Object other) {
        return this.compareByDescription((MethodNameAndArgs) other);
    }

    public int compareByDescription(MethodNameAndArgs methodNameAndArgs1) {
        return this.getDescription().compareTo(methodNameAndArgs1.getDescription());
    }

    public String[] getArgumentTypes() {
        return this.argumentTypes;
    }

    public String getDescription() {
        if (this.cachedDescription == null) {
            StringBuffer stringBuffer = new StringBuffer();
            stringBuffer.append(this.methodName + "(");
            if (this.argumentTypes != null) {
                int argumentTypeIndex = 0;
                int bb = 0;

                for (String[] argumentTypesValue = this.argumentTypes; bb < argumentTypesValue.length; argumentTypesValue = this.argumentTypes) {
                    stringBuffer.append(this.argumentTypes[argumentTypeIndex]);
                    if (argumentTypeIndex < this.argumentTypes.length - 1) {
                        stringBuffer.append(",");
                    }

                    bb = ++argumentTypeIndex;
                }
            } else {
                stringBuffer.append(UNKNOWN_ARGS);
            }

            stringBuffer.append(")");
            this.cachedDescription = stringBuffer.toString();
        }

        return this.cachedDescription;
    }
}

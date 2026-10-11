package com.zelix.klassmaster.changelog;

import com.zelix.ZKMChangeLog;

public class ChangeLogMethodEntry {
    public final ZKMChangeLog changeLog;
    public String returnType;
    public String methodName;
    public String[] argumentTypes;

    public String getSignatureString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(this.returnType);
        stringBuilder.append(' ');
        stringBuilder.append(this.methodName);
        stringBuilder.append('(');
        int argumentTypeIndex = 0;
        int bb = 0;

        for (String[] argumentTypesValue = this.argumentTypes; bb < argumentTypesValue.length; argumentTypesValue = this.argumentTypes) {
            stringBuilder.append(this.argumentTypes[argumentTypeIndex]);
            if (argumentTypeIndex < this.argumentTypes.length - 1) {
                stringBuilder.append(",");
            }

            bb = ++argumentTypeIndex;
        }

        stringBuilder.append(')');
        return stringBuilder.toString();
    }

    public ChangeLogMethodEntry(ZKMChangeLog zKMChangeLog, String returnTypeArgument, String methodNameArgument, String[] argumentTypesArgument) {
        this.changeLog = zKMChangeLog;
        this.returnType = returnTypeArgument;
        this.methodName = methodNameArgument;
        this.argumentTypes = argumentTypesArgument;
    }
}

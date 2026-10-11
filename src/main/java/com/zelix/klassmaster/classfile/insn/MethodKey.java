package com.zelix.klassmaster.classfile.insn;

public class MethodKey {
    private final String className;
    private final String methodName;
    private final String descriptor;
    private final int cachedHash;

    public MethodKey(String classNameArgument, String methodNameArgument, String descriptorArgument) {
        this.className = classNameArgument;
        this.methodName = methodNameArgument;
        this.descriptor = descriptorArgument;
        this.cachedHash = classNameArgument.hashCode() ^ methodNameArgument.hashCode() ^ descriptorArgument.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof MethodKey)) {
            return false;
        }

        MethodKey methodKey1 = (MethodKey) other;
        return this.cachedHash == methodKey1.cachedHash
                && this.className.equals(methodKey1.className)
                && this.methodName.equals(methodKey1.methodName)
                && this.descriptor.equals(methodKey1.descriptor);
    }

    @Override
    public int hashCode() {
        return this.cachedHash;
    }
}

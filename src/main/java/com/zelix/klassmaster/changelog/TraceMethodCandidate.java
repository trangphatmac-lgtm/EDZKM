package com.zelix.klassmaster.changelog;

import com.zelix.klassmaster.classfile.MethodInfo;

public class TraceMethodCandidate {
    public String methodNameAndDescriptor;
    public MethodInfo method;

    public TraceMethodCandidate(String methodNameAndDescriptorArgument, MethodInfo methodInfo1) {
        this.methodNameAndDescriptor = methodNameAndDescriptorArgument;
        this.method = methodInfo1;
    }
}

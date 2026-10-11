package com.zelix.klassmaster.changelog;

import com.zelix.klassmaster.classfile.MethodInfo;

public class FrameMethodCandidate {
    public boolean callTraceBreak = false;
    public ResolvedStackTraceLine stackTraceLine;
    public MethodInfo method;
    public int hashValue;

    public boolean hasCallTraceBreak() {
        return this.callTraceBreak;
    }

    public ResolvedStackTraceLine getStackTraceLine() {
        return this.stackTraceLine;
    }

    public MethodInfo getMethod() {
        return this.method;
    }

    @Override
    public int hashCode() {
        return this.hashValue;
    }

    @Override
    public boolean equals(Object otherArgument) {
        String[] opaqueStrings = StackTraceTranslator.getOpaqueStrings();
        if (!(otherArgument instanceof FrameMethodCandidate)) {
            return false;
        }
        if (opaqueStrings == null) {
            return true;
        }

        FrameMethodCandidate other = (FrameMethodCandidate) otherArgument;
        if (this.hashValue != other.hashValue) {
            return false;
        }
        if (this.stackTraceLine == null) {
            if (other.stackTraceLine != null) {
                return false;
            }
        } else if (other.stackTraceLine == null || !this.stackTraceLine.getLineText().equals(other.stackTraceLine.getLineText())) {
            return false;
        }
        return this.method == null
                ? other.method == null
                : other.method != null && this.method.isSameMethod(other.method);
    }

    public FrameMethodCandidate(ResolvedStackTraceLine traceLine, MethodInfo resolvedMethod, boolean callTraceBreak) {
        this.stackTraceLine = traceLine;
        this.method = resolvedMethod;
        this.callTraceBreak = callTraceBreak;
        this.hashValue = traceLine.getLineText().hashCode()
                ^ resolvedMethod.getSourceNameAndDescriptor().hashCode()
                ^ resolvedMethod.getClassName().hashCode();
    }

    public FrameMethodCandidate(ResolvedStackTraceLine traceLine) {
        this.stackTraceLine = traceLine;
        this.method = null;
        this.callTraceBreak = false;
        this.hashValue = traceLine.getLineText().hashCode();
    }
}

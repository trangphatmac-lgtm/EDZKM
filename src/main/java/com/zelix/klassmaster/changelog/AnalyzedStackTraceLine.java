package com.zelix.klassmaster.changelog;

import com.zelix.klassmaster.classfile.MethodInfo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public class AnalyzedStackTraceLine extends StackTraceLine {
    public MethodNameAndArgs[] resolveOriginalMethodNames(MethodInfo methodInfo1, LoadedChangeLog loadedChangeLog) throws StackTraceTranslateException {
        StringBuilder stringBuilder = new StringBuilder();
        List list1 = methodInfo1.getParameterTypeNames();
        String[] argumentTypes = this.translateArgumentTypes(list1, stringBuilder, loadedChangeLog);
        String returnTypeName = methodInfo1.getReturnTypeName();
        String string1 = this.translateTypeName(returnTypeName, loadedChangeLog);
        MethodNameAndArgs[] methodNameAndArgs;
        if (methodInfo1.isConstructor()) {
            int ba = super.className.lastIndexOf(".");
            String classNameValue;
            if (ba == -1) {
                classNameValue = super.className;
            } else {
                classNameValue = super.className.substring(ba + 1);
            }

            methodNameAndArgs = new MethodNameAndArgs[]{new MethodNameAndArgs(classNameValue, argumentTypes)};
        } else if (methodInfo1.isStaticInitializer()) {
            methodNameAndArgs = new MethodNameAndArgs[]{new MethodNameAndArgs("static", argumentTypes)};
        } else {
            String jvmName = methodInfo1.getJvmName();
            String[] strings1 = loadedChangeLog.findOriginalMethodNames(super.className, jvmName, string1, stringBuilder.toString());
            if (strings1 != null && strings1.length != 0) {
                methodNameAndArgs = new MethodNameAndArgs[strings1.length];

                for (int methodNameAndArgIndex = 0; methodNameAndArgIndex < strings1.length; methodNameAndArgIndex++) {
                    methodNameAndArgs[methodNameAndArgIndex] = new MethodNameAndArgs(strings1[methodNameAndArgIndex], argumentTypes);
                }
            } else {
                if (loadedChangeLog.isNewClassName(super.className)) {
                    StringBuilder stringBuilder1 = new StringBuilder();

                    for (int elementIndex = 0; elementIndex < list1.size(); elementIndex++) {
                        stringBuilder1.append((String) list1.get(elementIndex));
                        if (elementIndex < list1.size() - 1) {
                            stringBuilder1.append(", ");
                        }
                    }

                    throw new StackTraceTranslateException(
                            "Method '"
                                    + returnTypeName
                                    + " "
                                    + jvmName
                                    + "("
                                    + stringBuilder1.toString()
                                    + ")' is in the bytecode of class with original name '"
                                    + super.className
                                    + "' but doesn't appear in the change log. Change log does not correspond to obfuscated bytecode."
                    );
                }

                methodNameAndArgs = new MethodNameAndArgs[]{new MethodNameAndArgs(jvmName, argumentTypes)};
            }
        }

        return methodNameAndArgs;
    }

    public AnalyzedStackTraceLine(HashSet hashSet, LoadedChangeLog loadedChangeLog) throws StackTraceTranslateException {
        boolean bl = true;
        ArrayList arrayList = null;

        for (Iterator iterator = hashSet.iterator(); iterator.hasNext(); bl = false) {
            FrameMethodCandidate frameMethodCandidate = (FrameMethodCandidate) iterator.next();
            if (bl) {
                ResolvedStackTraceLine resolvedStackTraceLine = frameMethodCandidate.getStackTraceLine();
                super.frameLine = resolvedStackTraceLine.getFrameLine();
                super.callTraceBreak = frameMethodCandidate.hasCallTraceBreak();
                if (!super.frameLine.isValidFrame()) {
                    break;
                }

                arrayList = new ArrayList(hashSet.size());
                MethodInfo methodInfo1 = frameMethodCandidate.getMethod();
                this.resolveClassAndLine(resolvedStackTraceLine, loadedChangeLog);
                MethodNameAndArgs[] methodNameAndArgs = this.resolveOriginalMethodNames(methodInfo1, loadedChangeLog);

                for (int methodNameAndArgIndex = 0; methodNameAndArgIndex < methodNameAndArgs.length; methodNameAndArgIndex++) {
                    arrayList.add(methodNameAndArgs[methodNameAndArgIndex]);
                }
            } else {
                MethodInfo methodInfo2 = frameMethodCandidate.getMethod();
                MethodNameAndArgs[] methodNameAndArgs1 = this.resolveOriginalMethodNames(methodInfo2, loadedChangeLog);

                for (int methodNameAndArgs1ElementIndex = 0; methodNameAndArgs1ElementIndex < methodNameAndArgs1.length; methodNameAndArgs1ElementIndex++) {
                    arrayList.add(methodNameAndArgs1[methodNameAndArgs1ElementIndex]);
                }
            }
        }

        int bb = 0;
        if (arrayList != null) {
            super.methodCandidates = new MethodNameAndArgs[arrayList.size()];
            Iterator iterator1 = arrayList.iterator();

            while (iterator1.hasNext()) {
                super.methodCandidates[bb++] = (MethodNameAndArgs) iterator1.next();
            }
        }
    }
}

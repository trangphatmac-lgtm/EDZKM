package com.zelix.klassmaster.changelog;

import com.zelix.klassmaster.classfile.constpool.ConstantPoolEntry;
import com.zelix.klassmaster.util.ObjectPair;

import java.util.List;
import java.util.StringTokenizer;

public class ParsedLineStackFrame extends StackTraceLine {
    public ParsedLineStackFrame(StackTraceFrameLine stackTraceFrameLine, LoadedChangeLog loadedChangeLog) throws StackTraceTranslateException {
        super.frameLine = stackTraceFrameLine;
        if (stackTraceFrameLine.isValidFrame()) {
            this.resolveClassAndLine(stackTraceFrameLine, loadedChangeLog);
            if (super.translated) {
                String methodName = stackTraceFrameLine.getMethodName();
                String methodDescriptor = stackTraceFrameLine.getMethodDescriptor();
                if (methodDescriptor != null) {
                    List list1 = ConstantPoolEntry.getParameterJavaTypes(methodDescriptor);
                    StringBuilder stringBuilder = new StringBuilder();
                    String[] argumentTypes = this.translateArgumentTypes(list1, stringBuilder, loadedChangeLog);
                    String string2 = ConstantPoolEntry.descriptorToJavaType(methodDescriptor);
                    String string3 = null;
                    if (string2 != null && string2.length() > 0) {
                        string3 = this.translateTypeName(string2, loadedChangeLog);
                    }

                    if (methodName.equals("<init>")) {
                        int ba = super.className.lastIndexOf(".");
                        String classNameValue;
                        if (ba == -1) {
                            classNameValue = super.className;
                        } else {
                            classNameValue = super.className.substring(ba + 1);
                        }

                        super.methodCandidates = new MethodNameAndArgs[1];
                        super.methodCandidates[0] = new MethodNameAndArgs(classNameValue, argumentTypes);
                    } else if (methodName.equals("<clinit>")) {
                        String methodNameValue = "static";
                        super.methodCandidates = new MethodNameAndArgs[1];
                        super.methodCandidates[0] = new MethodNameAndArgs(methodNameValue, argumentTypes);
                    } else {
                        String[] strings2 = loadedChangeLog.findOriginalMethodNames(super.className, methodName, string3, stringBuilder.toString());
                        if (strings2 != null && strings2.length != 0) {
                            super.methodCandidates = new MethodNameAndArgs[strings2.length];

                            for (int methodCandidateIndex = 0; methodCandidateIndex < strings2.length; methodCandidateIndex++) {
                                super.methodCandidates[methodCandidateIndex] = new MethodNameAndArgs(strings2[methodCandidateIndex], argumentTypes);
                            }
                        } else {
                            String methodNameValue2 = methodName;
                            super.methodCandidates = new MethodNameAndArgs[1];
                            super.methodCandidates[0] = new MethodNameAndArgs(methodNameValue2, argumentTypes);
                        }
                    }
                } else if (methodName.equals("<init>")) {
                    super.methodCandidates = new MethodNameAndArgs[1];
                    int bc = super.className.lastIndexOf(".");
                    String classNameValue2;
                    if (bc == -1) {
                        classNameValue2 = super.className;
                    } else {
                        classNameValue2 = super.className.substring(bc + 1);
                    }

                    super.methodCandidates[0] = new MethodNameAndArgs(classNameValue2, null);
                } else if (methodName.equals("<clinit>")) {
                    super.methodCandidates = new MethodNameAndArgs[1];
                    String methodNameValue3 = "static";
                    super.methodCandidates[0] = new MethodNameAndArgs(methodNameValue3, StackTraceLine.EMPTY_STRING_ARRAY);
                } else {
                    List list2 = loadedChangeLog.findOriginalMethods(super.className, methodName);
                    if (list2 != null) {
                        super.methodCandidates = new MethodNameAndArgs[list2.size()];

                        for (int elementIndex = 0; elementIndex < list2.size(); elementIndex++) {
                            ObjectPair objectPair = (ObjectPair) list2.get(elementIndex);
                            String[] emptyStringArray = StackTraceLine.EMPTY_STRING_ARRAY;
                            String second = (String) objectPair.getSecond();
                            MethodNameAndArgs[] methodNameAndArgs;
                            if (second == null) {
                                methodNameAndArgs = super.methodCandidates;
                            } else {
                                StringTokenizer stringTokenizer = new StringTokenizer(second, ", ");
                                emptyStringArray = new String[stringTokenizer.countTokens()];
                                int be = 0;

                                while (stringTokenizer.hasMoreTokens()) {
                                    emptyStringArray[be++] = stringTokenizer.nextToken();
                                }

                                methodNameAndArgs = super.methodCandidates;
                            }

                            methodNameAndArgs[elementIndex] = new MethodNameAndArgs((String) objectPair.getFirst(), emptyStringArray);
                        }
                    } else {
                        super.methodCandidates = new MethodNameAndArgs[1];
                        String methodDescriptorValue = stackTraceFrameLine.getMethodDescriptor();
                        if (methodDescriptorValue != null) {
                            List list3 = ConstantPoolEntry.getParameterJavaTypes(methodDescriptorValue);
                            super.methodCandidates[0] = new MethodNameAndArgs(methodName, ((java.lang.String[]) (list3.toArray(new String[list3.size()]))));
                        } else {
                            super.methodCandidates[0] = new MethodNameAndArgs(methodName, null);
                        }
                    }
                }
            }
        }
    }
}

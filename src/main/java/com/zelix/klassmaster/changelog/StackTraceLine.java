package com.zelix.klassmaster.changelog;

import com.zelix.klassmaster.classfile.AbstractMethodInfo;
import com.zelix.klassmaster.classfile.FieldNameTypeSignature;
import com.zelix.klassmaster.classfile.MethodSignature;
import com.zelix.klassmaster.classfile.ProgramClass;
import com.zelix.klassmaster.classfile.hierarchy.ClasspathClassResolver;
import com.zelix.klassmaster.config.HiddenOptionFlags;
import com.zelix.klassmaster.exceptions.ClassLookupException;
import com.zelix.klassmaster.exceptions.ZkmException;
import com.zelix.klassmaster.util.ZkmStringUtils;
import com.zelix.klassmaster.util.ZkmUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

public abstract class StackTraceLine {
    public static final String[] EMPTY_STRING_ARRAY = new String[0];
    public static String indent;
    public String className;
    public MethodNameAndArgs[] methodCandidates;
    public boolean callTraceBreak;
    public String sourceFileName;
    public boolean classMapped;
    public boolean translated;
    public StackTraceFrameLine frameLine;
    public Integer lineNumber;

    public String getClassName() {
        return this.className;
    }

    public void appendSourceLocation(StringBuffer stringBuffer) {
        if (this.sourceFileName != null && this.sourceFileName.length() > 0) {
            stringBuffer.append("(" + this.sourceFileName);
            if (this.lineNumber != null) {
                stringBuffer.append(":" + this.lineNumber);
            }

            stringBuffer.append(")");
        }
    }

    public boolean isStaticInitializer() {
        return this.methodCandidates != null && this.methodCandidates.length == 1 && this.methodCandidates[0].getMethodName().equals("static");
    }

    public String getErrorMessage() {
        return this.frameLine.getErrorMessage();
    }

    static {
        Integer integer = 32;
        indent = ZkmStringUtils.repeatChar(4, integer);
    }

    public boolean hasError() {
        return this.frameLine.hasError();
    }

    public String translateTypeName(String string, LoadedChangeLog loadedChangeLog) {
        String newClassNameValue = string;
        int ba = newClassNameValue.indexOf("[");
        String string2;
        if (ba != -1) {
            string2 = newClassNameValue.substring(ba);
            newClassNameValue = newClassNameValue.substring(0, ba);
        } else {
            string2 = "";
        }

        String newClassName = loadedChangeLog.getNewClassName(newClassNameValue);
        if (newClassName == null) {
            newClassName = newClassNameValue;
        }

        return newClassName + string2;
    }

    public boolean isValidFrame() {
        return this.frameLine.isValidFrame();
    }

    public String[] translateArgumentTypes(List list1, StringBuilder stringBuilder, LoadedChangeLog loadedChangeLog) {
        int list1Size = list1.size();
        String[] strings = new String[list1Size];

        for (int elementIndex = 0; elementIndex < list1Size; elementIndex++) {
            String string = (String) list1.get(elementIndex);
            strings[elementIndex] = this.translateTypeName(string, loadedChangeLog);
            stringBuilder.append(strings[elementIndex]);
            if (elementIndex < list1Size - 1) {
                stringBuilder.append(", ");
            }
        }

        return strings;
    }

    public void translateFreeText(StringBuffer stringBuffer, String string, LoadedChangeLog loadedChangeLog) {
        StringTokenizer stringTokenizer = new StringTokenizer(string, "@: ()\",", true);
        ArrayList arrayList = new ArrayList(stringTokenizer.countTokens());

        while (stringTokenizer.hasMoreTokens()) {
            String string1 = stringTokenizer.nextToken();
            arrayList.add(string1);
        }

        for (int elementIndex = 0; elementIndex < arrayList.size(); elementIndex++) {
            String keyValue2 = (String) arrayList.get(elementIndex);
            switch (keyValue2) {
                case "@":
                case ":":
                case " ":
                case ")":
                case ",":
                case "\"":
                case "(":
                    stringBuffer.append(keyValue2);
                    break;
                default:
                    boolean bl = keyValue2.indexOf("/") > -1;
                    int characterIndex = 0;
                    String key;
                    if (bl) {
                        key = keyValue2.replace('/', '.');
                    } else {
                        key = keyValue2;
                    }

                    if (key.startsWith("[") && key.endsWith(";")) {
                        while (key.charAt(characterIndex) == '[') {
                            characterIndex++;
                        }

                        if (key.charAt(characterIndex) == 'L') {
                            key = key.substring(characterIndex + 1, key.length() - 1);
                        }
                    }

                    if (key.indexOf(46) > 0 && key.indexOf(46) < key.length() - 1) {
                        boolean bl1 = false;
                        int bb = key.lastIndexOf(".");
                        if (arrayList.size() > elementIndex + 1
                                && ((String) arrayList.get(elementIndex + 1)).equals("(")
                                && arrayList.indexOf(")") > elementIndex + 1
                                && ZkmStringUtils.countChar(key, '.') > 1
                                && bb < key.length() - 1) {
                            String keyValue = key.substring(0, bb);
                            String newClassName = loadedChangeLog.getNewClassName(keyValue);
                            if (newClassName != null) {
                                String keyValue3 = key.substring(bb + 1);
                                StringBuilder stringBuilder = new StringBuilder();
                                int elementIndexValue = elementIndex + 2;
                                int elementIndexValue2 = elementIndexValue;

                                for (int j = arrayList.indexOf(")"); elementIndexValue2 < j; j = arrayList.indexOf(")")) {
                                    String keyValue5 = (String) arrayList.get(elementIndexValue);
                                    String string8 = loadedChangeLog.toNewTypeName(keyValue5);
                                    stringBuilder.append(string8);
                                    elementIndexValue2 = ++elementIndexValue;
                                }

                                String keyValue4 = stringBuilder.toString();
                                String[] strings = loadedChangeLog.findOriginalMethodNames(newClassName, keyValue3, (String) null, keyValue4);
                                String string11;
                                if (strings != null && strings.length > 0) {
                                    string11 = "." + strings[0];
                                } else {
                                    string11 = key.substring(bb);
                                }

                                if (bl) {
                                    newClassName = newClassName.replace('.', '/');
                                }

                                stringBuffer.append(newClassName);
                                stringBuffer.append(string11);
                                bl1 = true;
                            }
                        }

                        if (!bl1) {
                            if (arrayList.size() > elementIndex + 1 && ((String) arrayList.get(elementIndex + 1)).equals("(")) {
                                stringBuffer.append(keyValue2);
                            } else {
                                String newClassNameValue = loadedChangeLog.getNewClassName(key);
                                if (newClassNameValue != null) {
                                    if (bl) {
                                        newClassNameValue = newClassNameValue.replace('.', '/');
                                    }

                                    stringBuffer.append(newClassNameValue);
                                    if (characterIndex > 0) {
                                        for (int j = 0; j < characterIndex; j++) {
                                            stringBuffer.append("[]");
                                        }
                                    }
                                } else {
                                    stringBuffer.append(keyValue2);
                                }
                            }
                        }
                    } else {
                        stringBuffer.append(keyValue2);
                    }
            }
        }
    }

    public final boolean resolveClassAndLine(StackTraceLineInfo stackTraceLineInfo, LoadedChangeLog loadedChangeLog) throws StackTraceTranslateException {
        String classNameValue = stackTraceLineInfo.getClassName();
        String classNameValue2 = loadedChangeLog.getNewClassName(classNameValue);
        if (classNameValue2 != null) {
            this.classMapped = true;
            this.className = classNameValue2;
            String sourceFileNameValue = loadedChangeLog.getSourceName(classNameValue2);
            if (sourceFileNameValue != null) {
                this.sourceFileName = sourceFileNameValue;
            }

            if (stackTraceLineInfo.hasLineNumber()) {
                Long long1 = 121982683753216L;
                int lineNumberValue = stackTraceLineInfo.getLineNumber(0, long1);
                if (lineNumberValue < 0) {
                    throw new StackTraceTranslateException("Line number \"" + lineNumberValue + "\" is negative in '" + stackTraceLineInfo.getLineText() + "'.");
                }

                if (loadedChangeLog.hasLineNumberMappings(classNameValue2)) {
                    Integer integer = loadedChangeLog.lookupLineNumber(classNameValue2, lineNumberValue);
                    if (integer == null) {
                        throw new StackTraceTranslateException(
                                "Line number '"
                                        + lineNumberValue
                                        + "' in '"
                                        + stackTraceLineInfo.getLineText()
                                        + "' could not be matched. Change log does not correspond to obfuscated bytecode."
                        );
                    }

                    this.lineNumber = integer;
                    this.translated = true;
                } else {
                    this.lineNumber = lineNumberValue;
                    this.translated = true;
                }
            } else {
                this.translated = true;
            }

            return true;
        } else {
            this.className = classNameValue;
            this.classMapped = false;
            this.translated = false;
            return false;
        }
    }

    public final String formatLine(int ba, LoadedChangeLog loadedChangeLog, ClasspathClassResolver classpathClassResolver) {
        if (this.methodCandidates != null) {
            Arrays.sort(this.methodCandidates);
        }

        StringBuffer stringBuffer = new StringBuffer(64);
        if (this.frameLine.isValidFrame() && this.classMapped) {
            stringBuffer.append(indent + "at " + this.className);
            if (this.methodCandidates != null && this.methodCandidates.length != 0) {
                if (this.methodCandidates.length == 1) {
                    stringBuffer.append("." + this.methodCandidates[0].getMethodName());
                    this.appendArguments(this.methodCandidates[0], ba, stringBuffer);
                    this.appendSourceLocation(stringBuffer);
                } else {
                    int bc = stringBuffer.length() + 1;
                    Integer integer = 32;
                    String string1 = ZkmStringUtils.repeatChar(bc, integer);
                    int methodCandidateIndex = 0;
                    bc = 0;

                    for (MethodNameAndArgs[] methodNameAndArgs = this.methodCandidates; bc < methodNameAndArgs.length; methodNameAndArgs = this.methodCandidates) {
                        if (methodCandidateIndex == 0) {
                            stringBuffer.append(".");
                        } else {
                            stringBuffer.append(string1);
                        }

                        stringBuffer.append(this.methodCandidates[methodCandidateIndex].getMethodName());
                        this.appendArguments(this.methodCandidates[methodCandidateIndex], ba, stringBuffer);
                        if (methodCandidateIndex == 0) {
                            this.appendSourceLocation(stringBuffer);
                            stringBuffer.append(HiddenOptionFlags.LINE_SEPARATOR);
                        } else if (methodCandidateIndex < this.methodCandidates.length - 1) {
                            stringBuffer.append(HiddenOptionFlags.LINE_SEPARATOR);
                        }

                        bc = ++methodCandidateIndex;
                    }
                }
            }

            stringBuffer.append(HiddenOptionFlags.LINE_SEPARATOR);
        } else {
            String lineText = this.frameLine.getLineText();
            if (this.frameLine.hasError() || this.frameLine.isValidFrame() && !this.classMapped) {
                stringBuffer.append(indent);
                stringBuffer.append(lineText);
            } else if (loadedChangeLog == null) {
                stringBuffer.append(lineText);
            } else if (lineText.indexOf("Exception in ") > -1
                    && lineText.indexOf("java.lang.NullPointerException") > -1
                    && lineText.indexOf("Cannot invoke \"") > -1) {
                this.translateNullPointerMessage(stringBuffer, lineText, loadedChangeLog, classpathClassResolver);
            } else {
                this.translateFreeText(stringBuffer, lineText, loadedChangeLog);
            }

            stringBuffer.append(HiddenOptionFlags.LINE_SEPARATOR);
        }

        return stringBuffer.toString();
    }

    public boolean isClassMapped() {
        return this.classMapped;
    }

    public AbstractMethodInfo[] findMethodsInHierarchy(
            ProgramClass programClass1, FieldNameTypeSignature fieldNameTypeSignature, ClasspathClassResolver classpathClassResolver
    ) throws ZkmException, IOException {
        AbstractMethodInfo[] abstractMethodInfos = programClass1.findMethodsByNameAndType(fieldNameTypeSignature);
        if (abstractMethodInfos != null && abstractMethodInfos.length > 0) {
            return abstractMethodInfos;
        }

        String superclassName = programClass1.getSuperclassName();
        if (!superclassName.equals("java/lang/Object")) {
            ProgramClass programClass2 = classpathClassResolver.loadProgramClass(superclassName);
            abstractMethodInfos = this.findMethodsInHierarchy(programClass2, fieldNameTypeSignature, classpathClassResolver);
            if (abstractMethodInfos != null && abstractMethodInfos.length > 0) {
                return abstractMethodInfos;
            }
        }

        String[] interfaceNames = programClass1.getInterfaceNames();

        for (String string1 : interfaceNames) {
            ProgramClass programClass3 = classpathClassResolver.loadProgramClass(string1);
            abstractMethodInfos = this.findMethodsInHierarchy(programClass3, fieldNameTypeSignature, classpathClassResolver);
            if (abstractMethodInfos != null && abstractMethodInfos.length > 0) {
                return abstractMethodInfos;
            }
        }

        return null;
    }

    public boolean hasCallTraceBreak() {
        return this.callTraceBreak;
    }

    public void appendArguments(MethodNameAndArgs methodNameAndArgs, int ba, StringBuffer stringBuffer) {
        String[] argumentTypes = methodNameAndArgs.getArgumentTypes();
        if (ba != 1 && argumentTypes != null) {
            if (ba == 2 || ba == 3) {
                stringBuffer.append("(");

                for (int argumentTypeIndex = 0; argumentTypeIndex < argumentTypes.length; argumentTypeIndex++) {
                    if (ba == 3) {
                        stringBuffer.append(argumentTypes[argumentTypeIndex]);
                    } else if (ba == 2) {
                        String argumentType = argumentTypes[argumentTypeIndex];
                        int bc = argumentType.lastIndexOf(".");
                        if (bc > -1) {
                            argumentType = argumentType.substring(bc + 1);
                        }

                        stringBuffer.append(argumentType);
                    }

                    if (argumentTypeIndex < argumentTypes.length - 1) {
                        stringBuffer.append(", ");
                    }
                }

                stringBuffer.append(")");
            }
        }
    }

    public AbstractMethodInfo[] findMethods(ProgramClass programClass1, String string, String string1, ClasspathClassResolver classpathClassResolver) throws ZkmException, IOException {
        FieldNameTypeSignature fieldNameTypeSignature = new FieldNameTypeSignature(string, MethodSignature.parseParameterList(string1));
        AbstractMethodInfo[] abstractMethodInfos = programClass1.findMethodsByNameAndType(fieldNameTypeSignature);
        return abstractMethodInfos != null && abstractMethodInfos.length > 0
                ? abstractMethodInfos
                : this.findMethodsInHierarchy(programClass1, fieldNameTypeSignature, classpathClassResolver);
    }

    public void translateNullPointerMessage(
            StringBuffer stringBuffer, String string, LoadedChangeLog loadedChangeLog, ClasspathClassResolver classpathClassResolver
    ) {
        String string1 = null;
        boolean bl = false;

        try {
            int ba = string.indexOf("Cannot invoke \"");
            if (ba > 0) {
                int bb = string.indexOf("\"", ba + "Cannot invoke \"".length());
                if (bb > -1) {
                    string1 = string.substring(ba + "Cannot invoke \"".length(), bb);
                    int endIndex = string1.indexOf(40);
                    if (endIndex > 1) {
                        int endIndexValue = string1.indexOf(41);
                        if (endIndexValue == string1.length() - 1) {
                            String string2 = string1.substring(0, endIndex);
                            int endIndexValue2 = string2.lastIndexOf(".");
                            if (endIndexValue2 > 0 && endIndexValue2 < string2.length() - 1) {
                                String newClassNameValue2 = string2.substring(0, endIndexValue2);
                                String key = string2.substring(endIndexValue2 + 1);
                                String string5 = string1.substring(endIndex + 1, endIndexValue);
                                StringBuilder stringBuilder = new StringBuilder();
                                StringTokenizer stringTokenizer = new StringTokenizer(string5, " ,");

                                while (stringTokenizer.hasMoreTokens()) {
                                    String string6 = loadedChangeLog.toNewTypeName(stringTokenizer.nextToken());
                                    stringBuilder.append(string6);
                                    if (stringTokenizer.hasMoreTokens()) {
                                        stringBuilder.append(", ");
                                    }
                                }

                                String newClassName = loadedChangeLog.getNewClassName(newClassNameValue2);
                                if (newClassName == null) {
                                    newClassName = newClassNameValue2;
                                }

                                if (classpathClassResolver != null) {
                                    ProgramClass programClass1 = null;

                                    try {
                                        programClass1 = classpathClassResolver.loadProgramClass(ZkmUtils.dotsToSlashes(newClassNameValue2));
                                    } catch (ClassLookupException classLookupException) {
                                    }

                                    if (programClass1 != null) {
                                        AbstractMethodInfo[] abstractMethodInfos = this.findMethods(programClass1, key, string5, classpathClassResolver);
                                        if (abstractMethodInfos != null && abstractMethodInfos.length == 1) {
                                            AbstractMethodInfo abstractMethodInfo = abstractMethodInfos[0];
                                            if (abstractMethodInfo.isProgramMember()) {
                                                String newClassNameValue = loadedChangeLog.getNewClassName(abstractMethodInfo.getDottedClassName());
                                                String keyValue = stringBuilder.toString();
                                                String[] strings = loadedChangeLog.findOriginalMethodNames(newClassNameValue, key, (String) null, keyValue);
                                                stringBuffer.append(string.substring(0, ba + "Cannot invoke \"".length()));
                                                stringBuffer.append(newClassName);
                                                stringBuffer.append(".");
                                                stringBuffer.append(strings[0]);
                                                stringBuffer.append('(');
                                                stringBuffer.append(stringBuilder);
                                                stringBuffer.append(')');
                                                this.translateFreeText(stringBuffer, string.substring(bb), loadedChangeLog);
                                                bl = true;
                                            }
                                        }
                                    }
                                } else {
                                    String keyValue2 = stringBuilder.toString();
                                    String[] strings1 = loadedChangeLog.findOriginalMethodNames(newClassName, key, (String) null, keyValue2);
                                    if (strings1 != null && strings1.length == 1) {
                                        stringBuffer.append(string.substring(0, ba + "Cannot invoke \"".length()));
                                        stringBuffer.append(newClassName);
                                        stringBuffer.append(".");
                                        stringBuffer.append(strings1[0]);
                                        stringBuffer.append('(');
                                        stringBuffer.append(stringBuilder);
                                        stringBuffer.append(')');
                                        this.translateFreeText(stringBuffer, string.substring(bb), loadedChangeLog);
                                        bl = true;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception exception) {
        }

        if (string1 == null || !bl) {
            this.translateFreeText(stringBuffer, string, loadedChangeLog);
        }
    }
}

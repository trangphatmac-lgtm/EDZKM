package com.zelix.klassmaster.changelog;

import com.zelix.klassmaster.classfile.hierarchy.ClasspathClassResolver;
import com.zelix.klassmaster.config.HiddenOptionFlags;

public abstract class TranslatedStackTrace {
    public StackTraceLine[] lines;

    public boolean hasMappedClasses() {
        int lineIndex = 0;
        int bb = 0;

        for (StackTraceLine[] stackTraceLines = this.lines; bb < stackTraceLines.length; stackTraceLines = this.lines) {
            if (this.lines[lineIndex].isClassMapped()) {
                return true;
            }

            bb = ++lineIndex;
        }

        return false;
    }

    public String format(int ba, LoadedChangeLog loadedChangeLog, ClasspathClassResolver classpathClassResolver) {
        StringBuffer stringBuffer = new StringBuffer(1024);
        boolean callTraceBreak = false;
        StackTraceLine stackTraceLine1 = null;

        for (int lineIndex = this.lines.length - 1; lineIndex >= 0; lineIndex += -1) {
            StackTraceLine stackTraceLine2 = this.lines[lineIndex];
            if (callTraceBreak) {
                if (stackTraceLine2.isValidFrame()) {
                    String className = stackTraceLine2.getClassName();
                    if (className.startsWith("java.") || className.startsWith("sun.")) {
                        stringBuffer.append("<Break in method call trace.>" + HiddenOptionFlags.LINE_SEPARATOR);
                    } else if (stackTraceLine1.isStaticInitializer()) {
                        stringBuffer.append("<Break in method call trace due to class initializer call.>" + HiddenOptionFlags.LINE_SEPARATOR);
                    } else {
                        stringBuffer.append("<Break in method call trace. Could be due to JIT compiler inlining of method.>" + HiddenOptionFlags.LINE_SEPARATOR);
                    }
                } else {
                    stringBuffer.append("<Break in method call trace.>" + HiddenOptionFlags.LINE_SEPARATOR);
                }
            }

            stringBuffer.append(stackTraceLine2.formatLine(ba, loadedChangeLog, classpathClassResolver));
            if (stackTraceLine2.hasError()) {
                stringBuffer.append("<" + stackTraceLine2.getErrorMessage() + ">" + HiddenOptionFlags.LINE_SEPARATOR);
            }

            callTraceBreak = stackTraceLine2.hasCallTraceBreak();
            stackTraceLine1 = stackTraceLine2;
        }

        return stringBuffer.toString();
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

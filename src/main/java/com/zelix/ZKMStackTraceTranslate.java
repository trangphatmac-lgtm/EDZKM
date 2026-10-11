package com.zelix;

import com.zelix.klassmaster.archive.TempFileManager;
import com.zelix.klassmaster.changelog.StackTraceTranslator;
import com.zelix.klassmaster.classfile.hierarchy.ZkmClasspath;
import com.zelix.klassmaster.config.HiddenOptionFlags;
import com.zelix.klassmaster.engine.JvmVersionMessages;
import com.zelix.klassmaster.engine.ZkmApiBase;
import com.zelix.klassmaster.exceptions.ZkmException;
import com.zelix.klassmaster.util.JavaRuntimeVersion;
import com.zelix.klassmaster.util.ZkmAssert;
import com.zelix.klassmaster.util.ZkmUtils;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Set;
import java.util.StringTokenizer;

public class ZKMStackTraceTranslate extends JvmVersionMessages {
    public static final int NO_PARAM_TYPES = 1;
    public static final int FULL_PARAM_TYPES = 3;
    public static final int UNQUALIFIED_PARAM_TYPES = 2;
    public ArrayList changeLogFileNames;
    public Set classpathErrorPaths;
    public StackTraceTranslator translator;

    public ZKMStackTraceTranslate(String[] strings) {
        this(strings, null);
    }

    public String getTranslatedStackTrace(String stackTraceText, boolean analyzeBytecode) {
        return this.getTranslatedStackTrace(stackTraceText, analyzeBytecode, 2);
    }

    public ZKMStackTraceTranslate(String string) {
        this(string, null);
    }

    public ZKMStackTraceTranslate(String[] strings, String string) {
        this.mandatoryContruction(strings, null);
    }

    public static void launchTranslator() {
        if (HiddenOptionFlags.TEST_JVM_VERSION && JavaRuntimeVersion.getRuntimeClassMajorVersion() < 52) {
            System.err.println(JvmVersionMessages.LINE_SEPARATOR + JvmVersionMessages.JVM_VERSION_ERROR + JvmVersionMessages.LINE_SEPARATOR);
            System.exit(1);
        }

        try {
            ZkmApiBase.showStackTraceTranslate();
        } catch (IllegalAccessException illegalAccessException) {
            System.err
                    .println(
                            JvmVersionMessages.LINE_SEPARATOR
                                    + "Unexpected Error [B]. Please report the problem to "
                                    + "bugs2@zelix.com"
                                    + JvmVersionMessages.LINE_SEPARATOR
                    );
        } catch (NoClassDefFoundError noClassDefFoundError) {
            String property = System.getProperty("java.class.path");
            if (property != null) {
                if (property.indexOf("ZKMTranslate.jar") == -1) {
                    System.err.println("Startup Error. (Check that ZKMTranslate.jar is in classpath) (1)");
                } else if (isTranslateJarOnClasspath(property)) {
                    System.err
                            .println(
                                    JvmVersionMessages.LINE_SEPARATOR
                                            + "Unexpected Error [C]. Please report the problem to "
                                            + "bugs2@zelix.com"
                                            + JvmVersionMessages.LINE_SEPARATOR
                            );
                } else {
                    System.err.println("Startup Error. (ZKMTranslate.jar not found where specified by classpath)");
                }
            } else {
                System.err.println("Startup Error. (Check that ZKMTranslate.jar is in classpath) (2)");
            }

            System.err.println("Classpath is: \"" + property + "\"");
        } catch (InvocationTargetException invocationTargetException) {
            Throwable throwable = invocationTargetException.getTargetException();
            System.err
                    .println(
                            JvmVersionMessages.LINE_SEPARATOR
                                    + "Unexpected Error [D]. Please report the problem to "
                                    + "bugs2@zelix.com"
                                    + JvmVersionMessages.LINE_SEPARATOR
                    );
            throwable.printStackTrace(System.err);
        } catch (NoSuchMethodException noSuchMethodException) {
            System.err
                    .println(
                            JvmVersionMessages.LINE_SEPARATOR
                                    + "Unexpected Error [E]. Please report the problem to "
                                    + "bugs2@zelix.com"
                                    + JvmVersionMessages.LINE_SEPARATOR
                    );
        } catch (Throwable throwable1) {
            System.err
                    .println(
                            JvmVersionMessages.LINE_SEPARATOR
                                    + "Unexpected Error [G]. Please report the problem to "
                                    + "bugs2@zelix.com"
                                    + JvmVersionMessages.LINE_SEPARATOR
                    );
            throwable1.printStackTrace(System.err);
        }
    }

    public String getOldClassName(String key) {
        try {
            String oldClassName;
            try {
                oldClassName = this.translator.translateClassName(key);
            } catch (ZkmException zkmException) {
                oldClassName = "EDZKM ERROR (2): \"" + zkmException.getMessage() + "\"" + JvmVersionMessages.LINE_SEPARATOR;
            }

            return oldClassName;
        } catch (Throwable throwable) {
            throw ZkmUtils.<RuntimeException>sneakyThrow(throwable);
        }
    }

    public String getTranslatedStackTrace(String stackTraceText, boolean analyzeBytecode, int argumentFormat) {
        try {
            String translatedStackTrace;
            try {
                translatedStackTrace = this.translator.translate(stackTraceText, analyzeBytecode, argumentFormat);
            } catch (ZkmException zkmException) {
                String string2 = null;
                if (this.classpathErrorPaths != null && this.classpathErrorPaths.size() > 0) {
                    string2 = ZkmUtils.toQuotedListString(ZkmUtils.describeTempFileNames(this.classpathErrorPaths));
                }

                translatedStackTrace = "EDZKM ERROR (1): \""
                        + zkmException.getMessage()
                        + "\""
                        + JvmVersionMessages.LINE_SEPARATOR
                        + (string2 != null ? "Initial classpath paths in error : " + string2 + ZkmAssert.lineSeparator : "");
            }

            return translatedStackTrace;
        } catch (Throwable throwable) {
            throw ZkmUtils.<RuntimeException>sneakyThrow(throwable);
        }
    }

    public String getOldMethodName(String key, String string1) {
        try {
            try {
                string1 = string1.trim();
                int endIndex = string1.indexOf(" ");
                if (endIndex <= -1) {
                    return "'" + string1 + "' is not a valid method signature (3)";
                }

                String keyArgument2 = string1.substring(0, endIndex);
                String string3 = string1.substring(endIndex + 1);
                string3 = string3.trim();
                int bb = string3.indexOf("(");
                if (bb <= 0) {
                    return "'" + string1 + "' is not a valid method signature (2)";
                }

                String keyValue = string3.substring(0, bb);
                int endIndexValue = string3.indexOf(")", bb);
                if (endIndexValue <= -1) {
                    return "'" + string1 + "' is not a valid method signature (1)";
                }

                String string5 = string3.substring(bb + 1, endIndexValue);
                ArrayList arrayList = new ArrayList();
                StringTokenizer stringTokenizer = new StringTokenizer(string5, ",");

                while (stringTokenizer.hasMoreTokens()) {
                    arrayList.add(stringTokenizer.nextToken().trim());
                }

                String[] strings = new String[arrayList.size()];
                strings = ((java.lang.String[]) (arrayList.toArray(strings)));
                return this.translator.getOriginalMethodName(key, keyValue, strings, keyArgument2);
            } catch (ZkmException zkmException) {
                return "EDZKM ERROR (5): \"" + zkmException.getMessage() + "\"" + JvmVersionMessages.LINE_SEPARATOR;
            }
        } catch (Throwable throwable) {
            throw ZkmUtils.<RuntimeException>sneakyThrow(throwable);
        }
    }

    public static boolean isTranslateJarOnClasspath(String string) {
        StringTokenizer stringTokenizer = new StringTokenizer(string, JvmVersionMessages.PATH_SEPARATOR);

        while (stringTokenizer.hasMoreTokens()) {
            String string1 = stringTokenizer.nextToken();
            if (string1.endsWith("ZKMTranslate.jar") && new File(string1).exists()) {
                return true;
            }
        }

        return false;
    }

    public void close() {
        if (this.translator != null) {
            this.translator.dispose();
        }

        TempFileManager.deleteStaleTempFiles();
    }

    public void mandatoryContruction(String[] strings, String string1) {
        ZkmApiBase.isZkmJarInClasspath(null);
        this.changeLogFileNames = new ArrayList(strings.length);
        int ba = 0;

        for (String string : strings) {
            if (string == null) {
                throw new IllegalArgumentException("Change log file name " + ba + " must not be null");
            }

            this.changeLogFileNames.add(string);
            ba++;
        }

        Object object = null;
        this.translator = new StackTraceTranslator(this.changeLogFileNames, (ZkmClasspath) object);
    }

    public String getOldMethodName(String key, String keyArgument, String[] strings, String keyArgument2) {
        try {
            try {
                return this.translator.getOriginalMethodName(key, keyArgument, strings, keyArgument2);
            } catch (ZkmException zkmException) {
                return "EDZKM ERROR (4): \"" + zkmException.getMessage() + "\"" + JvmVersionMessages.LINE_SEPARATOR;
            }
        } catch (Throwable throwable) {
            throw ZkmUtils.<RuntimeException>sneakyThrow(throwable);
        }
    }

    public String getOldMethodSignatures(String string, String key) {
        try {
            String oldMethodSignatures;
            try {
                String[] originalMethodSignatures = this.translator.getOriginalMethodSignatures(string, key);
                StringBuffer stringBuffer = new StringBuffer();
                stringBuffer.append("[");

                for (int originalMethodSignatureIndex = 0; originalMethodSignatureIndex < originalMethodSignatures.length; originalMethodSignatureIndex++) {
                    stringBuffer.append(originalMethodSignatures[originalMethodSignatureIndex]);
                    if (originalMethodSignatureIndex < originalMethodSignatures.length - 2) {
                        stringBuffer.append(", ");
                    } else if (originalMethodSignatureIndex < originalMethodSignatures.length - 1) {
                        stringBuffer.append(" or ");
                    }
                }

                stringBuffer.append("]");
                oldMethodSignatures = stringBuffer.toString();
            } catch (ZkmException zkmException) {
                oldMethodSignatures = "EDZKM ERROR (3): \"" + zkmException.getMessage() + "\"" + JvmVersionMessages.LINE_SEPARATOR;
            }

            return oldMethodSignatures;
        } catch (Throwable throwable) {
            throw ZkmUtils.<RuntimeException>sneakyThrow(throwable);
        }
    }

    public static void main(String[] strings) {
        TempFileManager.deleteStaleTempFiles();
        launchTranslator();
    }

    public ZKMStackTraceTranslate(String string, String string1) {
        if (string == null) {
            throw new IllegalArgumentException("Change log file name must not be null");
        }

        String[] strings = new String[]{string};
        this.mandatoryContruction(strings, null);
    }

    public String getTranslatedStackTrace(String stackTraceText) {
        return this.getTranslatedStackTrace(stackTraceText, true, 2);
    }
}

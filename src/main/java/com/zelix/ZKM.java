package com.zelix;

import com.zelix.klassmaster.archive.ZkmFileUtils;
import com.zelix.klassmaster.config.HiddenOptionFlags;
import com.zelix.klassmaster.config.SystemPropertiesFileLoader;
import com.zelix.klassmaster.engine.StartupErrorMessages;
import com.zelix.klassmaster.engine.ZkmApiBase;
import com.zelix.klassmaster.exceptions.CorruptHierarchyException;
import com.zelix.klassmaster.license.LicenseExpiredException;
import com.zelix.klassmaster.util.JavaRuntimeVersion;
import com.zelix.klassmaster.util.ObservableHolder;
import com.zelix.klassmaster.util.ZkmUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class ZKM extends ZkmApiBase {
    public static void main(String[] stringArray) {
        System.setProperty("apple.awt.application.name", "EDZKM");
        if (com.zelix.klassmaster.web.WebLauncher.launchIfRequested(stringArray)) return;
        legacyMain(stringArray);
    }

    public static void legacyMain(String[] stringArray) {
        try {
            SystemPropertiesFileLoader.loadSystemProperties();
            if (HiddenOptionFlags.DEBUG_REDIRECT_OUT) {
                PrintStream redirectedOutput = null;
                try {
                    try {
                        File outputFile = new File(ZkmFileUtils.USER_DIR_FILE, "RedirectedOut_" + System.currentTimeMillis() + ".txt");
                        System.out.println("REDIRECTING System.out to '" + outputFile.getAbsolutePath() + "'" + '\u0007');
                        redirectedOutput = new PrintStream(new BufferedOutputStream(new FileOutputStream(outputFile), 4096), true);
                        ZkmApiBase.savedStdout = System.out;
                        System.setOut(redirectedOutput);
                        redirectedOutput.close();
                    } catch (IOException exception) {
                        System.setOut(ZkmApiBase.savedStdout);
                        exception.printStackTrace();
                        System.exit(1);
                    }
                } catch (Throwable ignored) {
                    // The original abrupt completion of finally discarded pending failures here.
                } finally {
                    if (redirectedOutput != null) {
                        redirectedOutput.close();
                    }
                }
            }
            if (HiddenOptionFlags.TEST_JVM_VERSION && JavaRuntimeVersion.getRuntimeClassMajorVersion() < 52) {
                System.err.println(StartupErrorMessages.LINE_SEPARATOR + StartupErrorMessages.JVM_VERSION_ERROR + StartupErrorMessages.LINE_SEPARATOR);
                System.exit(1);
            }
            try {
                Class.forName("java.security.MessageDigest");
            } catch (ClassNotFoundException classNotFoundException) {
                System.err.println(StartupErrorMessages.LINE_SEPARATOR + StartupErrorMessages.MESSAGE_DIGEST_MISSING_ERROR + StartupErrorMessages.LINE_SEPARATOR);
                System.exit(1);
            }
            ObservableHolder logWriterHolder = new ObservableHolder();
            try {
                Class<?>[] parameterTypes = new Class[]{String[].class, ObservableHolder.class};
                Constructor<?> constructor = Class.forName(ZkmUtils.decodeHiddenString()).getConstructor(parameterTypes);
                Object[] objectArray = new Object[]{stringArray, logWriterHolder};
                constructor.newInstance(objectArray);
            } catch (InstantiationException instantiationException) {
                System.err.println(StartupErrorMessages.LINE_SEPARATOR + "Unexpected Error (A). Please report the problem to " + "bugs2@zelix.com" + StartupErrorMessages.LINE_SEPARATOR);
                System.exit(1);
            } catch (IllegalAccessException illegalAccessException) {
                System.err.println(StartupErrorMessages.LINE_SEPARATOR + "Unexpected Error (B). Please report the problem to " + "bugs2@zelix.com" + StartupErrorMessages.LINE_SEPARATOR);
                System.exit(1);
            } catch (NoClassDefFoundError noClassDefFoundError) {
                String classPath = System.getProperty("java.class.path");
                PrintStream errorOutput;
                if (classPath == null) {
                    System.err.println("Startup Error. (Check that ZKM.jar is in classpath) (2)");
                    errorOutput = System.err;
                } else if (classPath.indexOf("ZKM.jar") == -1) {
                    System.err.println("Startup Error. (Check that ZKM.jar is in classpath) (1)");
                    errorOutput = System.err;
                } else if (ZkmApiBase.isZkmJarInClasspath(classPath)) {
                    System.err.println(StartupErrorMessages.LINE_SEPARATOR + "Unexpected Error (C). Please report the problem to " + "bugs2@zelix.com" + StartupErrorMessages.LINE_SEPARATOR);
                    errorOutput = System.err;
                } else {
                    System.err.println("Startup Error. (ZKM.jar not found where specified by classpath)");
                    errorOutput = System.err;
                }
                errorOutput.println("Classpath is: \"" + classPath + "\"");
                System.exit(1);
            } catch (InvocationTargetException invocationTargetException) {
                Throwable failure = invocationTargetException.getTargetException();
                if (failure instanceof LicenseExpiredException) {
                    System.err.println(StartupErrorMessages.LINE_SEPARATOR + "Evaluation has expired : " + failure.getMessage() + StartupErrorMessages.LINE_SEPARATOR);
                    System.exit(1);
                    System.exit(1);
                } else if (failure instanceof CorruptHierarchyException) {
                    PrintWriter logWriter = (PrintWriter) logWriterHolder.getValue();
                    System.err.println(StartupErrorMessages.LINE_SEPARATOR + "Unexpected Error (D). Please report the problem to " + "bugs2@zelix.com" + StartupErrorMessages.LINE_SEPARATOR);
                    System.err.println(StartupErrorMessages.LINE_SEPARATOR + failure.getMessage() + StartupErrorMessages.LINE_SEPARATOR);
                    logWriter.println(StartupErrorMessages.LINE_SEPARATOR + "Unexpected Error (D). Please report the problem to " + "bugs2@zelix.com");
                    logWriter.println(StartupErrorMessages.LINE_SEPARATOR + failure.getMessage() + StartupErrorMessages.LINE_SEPARATOR);
                    Throwable cause = failure.getCause();
                    if (cause != null) {
                        cause.printStackTrace(logWriter);
                    }
                    System.exit(1);
                }
                System.err.println(StartupErrorMessages.LINE_SEPARATOR + "Unexpected Error (D). Please report the problem to " + "bugs2@zelix.com" + StartupErrorMessages.LINE_SEPARATOR);
                ZkmApiBase.printTrimmedStackTrace(failure, (PrintWriter) logWriterHolder.getValue());
                System.exit(1);
            } catch (NoSuchMethodException noSuchMethodException) {
                System.err.println(StartupErrorMessages.LINE_SEPARATOR + "Unexpected Error (E). Please report the problem to " + "bugs2@zelix.com" + StartupErrorMessages.LINE_SEPARATOR);
                System.exit(1);
            } catch (Throwable throwable) {
                System.err.println(StartupErrorMessages.LINE_SEPARATOR + "Unexpected Error (G). Please report the problem to " + "bugs2@zelix.com" + StartupErrorMessages.LINE_SEPARATOR);
                ZkmApiBase.printTrimmedStackTrace(throwable, (PrintWriter) logWriterHolder.getValue());
                System.exit(1);
            }
            return;
        } catch (Throwable throwable) {
            throw ZkmUtils.<RuntimeException>sneakyThrow(throwable);
        }
    }

    private ZKM() {
    }
}

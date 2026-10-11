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
    private static String buildMarker = "W5MMsc";

    
    
    public static void main(String[] strings) {
        try {
            SystemPropertiesFileLoader.loadSystemProperties();
            if (HiddenOptionFlags.DEBUG_REDIRECT_OUT) {
                PrintStream redirectedOutput = null;
                try {
                    File outputFile = new File(ZkmFileUtils.USER_DIR_FILE, "RedirectedOut_" + System.currentTimeMillis() + ".txt");
                    System.out.println("REDIRECTING System.out to '" + outputFile.getAbsolutePath() + "'" + '\u0007');
                    redirectedOutput = new PrintStream(new BufferedOutputStream(new FileOutputStream(outputFile), 4096), true);
                    ZkmApiBase.savedStdout = System.out;
                    System.setOut(redirectedOutput);
                } catch (IOException exception) {
                    System.setOut(ZkmApiBase.savedStdout);
                    exception.printStackTrace();
                    System.exit(1);
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

            ObservableHolder observableHolder = new ObservableHolder();

            try {
                Class[] class1 = new Class[]{String[].class, ObservableHolder.class};
                Constructor<?> constructor = Class.forName(ZkmUtils.decodeHiddenString()).getConstructor(class1);
                Object[] objects = new Object[]{strings, observableHolder};
                constructor.newInstance(objects);
            } catch (InstantiationException instantiationException) {
                System.err
                        .println(
                                StartupErrorMessages.LINE_SEPARATOR
                                        + "Unexpected Error (A). Please report the problem to "
                                        + "bugs2@zelix.com"
                                        + StartupErrorMessages.LINE_SEPARATOR
                        );
                System.exit(1);
            } catch (IllegalAccessException illegalAccessException) {
                System.err
                        .println(
                                StartupErrorMessages.LINE_SEPARATOR
                                        + "Unexpected Error (B). Please report the problem to "
                                        + "bugs2@zelix.com"
                                        + StartupErrorMessages.LINE_SEPARATOR
                        );
                System.exit(1);
            } catch (NoClassDefFoundError noClassDefFoundError) {
                String property = System.getProperty("java.class.path");
                PrintStream printStream1;
                if (property != null) {
                    if (property.indexOf("ZKM.jar") == -1) {
                        System.err.println("Startup Error. (Check that ZKM.jar is in classpath) (1)");
                        printStream1 = System.err;
                    } else if (ZkmApiBase.isZkmJarInClasspath(property)) {
                        System.err
                                .println(
                                        StartupErrorMessages.LINE_SEPARATOR
                                                + "Unexpected Error (C). Please report the problem to "
                                                + "bugs2@zelix.com"
                                                + StartupErrorMessages.LINE_SEPARATOR
                                );
                        printStream1 = System.err;
                    } else {
                        System.err.println("Startup Error. (ZKM.jar not found where specified by classpath)");
                        printStream1 = System.err;
                    }
                } else {
                    System.err.println("Startup Error. (Check that ZKM.jar is in classpath) (2)");
                    printStream1 = System.err;
                }

                printStream1.println("Classpath is: \"" + property + "\"");
                System.exit(1);
            } catch (InvocationTargetException invocationTargetException) {
                Throwable throwable = invocationTargetException.getTargetException();
                if (throwable instanceof LicenseExpiredException) {
                    System.err
                            .println(StartupErrorMessages.LINE_SEPARATOR + "Evaluation has expired : " + throwable.getMessage() + StartupErrorMessages.LINE_SEPARATOR);
                    System.exit(1);
                } else if (throwable instanceof CorruptHierarchyException) {
                    PrintWriter printWriter = (PrintWriter) observableHolder.getValue();
                    System.err
                            .println(
                                    StartupErrorMessages.LINE_SEPARATOR
                                            + "Unexpected Error (D). Please report the problem to "
                                            + "bugs2@zelix.com"
                                            + StartupErrorMessages.LINE_SEPARATOR
                            );
                    System.err.println(StartupErrorMessages.LINE_SEPARATOR + throwable.getMessage() + StartupErrorMessages.LINE_SEPARATOR);
                    printWriter.println(StartupErrorMessages.LINE_SEPARATOR + "Unexpected Error (D). Please report the problem to " + "bugs2@zelix.com");
                    printWriter.println(StartupErrorMessages.LINE_SEPARATOR + throwable.getMessage() + StartupErrorMessages.LINE_SEPARATOR);
                    Throwable throwable1 = throwable.getCause();
                    if (throwable1 != null) {
                        throwable1.printStackTrace(printWriter);
                    }

                    System.exit(1);
                } else {
                    System.err
                            .println(
                                    StartupErrorMessages.LINE_SEPARATOR
                                            + "Unexpected Error (D). Please report the problem to "
                                            + "bugs2@zelix.com"
                                            + StartupErrorMessages.LINE_SEPARATOR
                            );
                    ZkmApiBase.printTrimmedStackTrace(throwable, (PrintWriter) observableHolder.getValue());
                    System.exit(1);
                }
            } catch (NoSuchMethodException noSuchMethodException) {
                System.err
                        .println(
                                StartupErrorMessages.LINE_SEPARATOR
                                        + "Unexpected Error (E). Please report the problem to "
                                        + "bugs2@zelix.com"
                                        + StartupErrorMessages.LINE_SEPARATOR
                        );
                System.exit(1);
            } catch (Throwable throwable2) {
                System.err
                        .println(
                                StartupErrorMessages.LINE_SEPARATOR
                                        + "Unexpected Error (G). Please report the problem to "
                                        + "bugs2@zelix.com"
                                        + StartupErrorMessages.LINE_SEPARATOR
                        );
                ZkmApiBase.printTrimmedStackTrace(throwable2, (PrintWriter) observableHolder.getValue());
                System.exit(1);
            }
        } catch (Throwable throwable3) {
            throw ZkmUtils.<RuntimeException>sneakyThrow(throwable3);
        }
    }

    public static void resetBuildMarker(String string) {
        buildMarker = "W5MMsc";
    }

    public static String getBuildMarker() {
        return buildMarker;
    }

    private ZKM() {
    }
}

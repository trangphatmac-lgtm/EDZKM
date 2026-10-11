import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.Permission;
import java.util.*;
import javax.tools.ToolProvider;

/** Run in a dedicated JDK 17/21 process with -Djava.security.manager=allow. */
@SuppressWarnings("removal")
public final class EntryPointEquivalenceChecks {
    private static final String TARGET = "com.zelix.klassmaster.KlassMaster";
    private static final class ExitTrap extends SecurityManager {
        private final boolean denySetIo;
        ExitTrap(boolean denySetIo) { this.denySetIo = denySetIo; }
        @Override public void checkPermission(Permission permission) {
            if (denySetIo && permission instanceof RuntimePermission && permission.getName().equals("setIO")) {
                throw new SecurityException("setIO denied by fixture");
            }
        }
        @Override public void checkExit(int status) { throw new SecurityException("exit=" + status); }
    }
    public static void main(String[] args) throws Exception {
        if (args.length < 2) throw new IllegalArgumentException("Usage: EntryPointEquivalenceChecks baseline-classes current-classes [dependency.jar ...]");
        Path temporary = Files.createTempDirectory("zkm-entry-equivalence-");
        int checked = 0;
        try {
            Path fixtureClasses = temporary.resolve("fixtures");
            Files.createDirectories(fixtureClasses);
            Path fixtureSource = temporary.resolve("KlassMaster.java");
            String source = "package com.zelix.klassmaster;\n"
                    + "import com.zelix.klassmaster.util.ObservableHolder;\n"
                    + "public class KlassMaster { public KlassMaster(String[] args, ObservableHolder holder) throws Exception {\n"
                    + "holder.setValue(new java.io.PrintWriter(System.err, true)); System.err.println(\"CONSTRUCTOR_REACHED\");\n"
                    + "if (args[0].equals(\"license\")) throw new com.zelix.klassmaster.license.LicenseExpiredException(\"fixture expiry\");\n"
                    + "if (args[0].equals(\"corrupt\")) throw new com.zelix.klassmaster.exceptions.CorruptHierarchyException(\"fixture hierarchy\", new IllegalStateException(\"fixture cause\"));\n"
                    + "if (args[0].equals(\"generic\")) throw new IllegalArgumentException(\"fixture generic\");\n"
                    + "}}\n";
            Files.write(fixtureSource, source.getBytes(StandardCharsets.UTF_8));
            StringBuilder classPath = new StringBuilder(args[0]);
            for (int i = 2; i < args.length; i++) classPath.append(File.pathSeparator).append(args[i]);
            int compileStatus = ToolProvider.getSystemJavaCompiler().run(null, null, null, "-classpath", classPath.toString(), "-d", fixtureClasses.toString(), fixtureSource.toString());
            if (compileStatus != 0) throw new AssertionError("Fixture compilation failed");
            for (String mode : new String[]{"normal", "license", "corrupt", "generic"}) {
                for (int redirect = 0; redirect < 4; redirect++) {
                    String before = run(args[0], args, fixtureClasses, temporary, mode, redirect, "fixture-classpath");
                    String after = run(args[1], args, fixtureClasses, temporary, mode, redirect, "fixture-classpath");
                    if (!before.equals(after)) throw new AssertionError(mode + "/redirect=" + redirect + "\nBASELINE:\n" + before + "\nCURRENT:\n" + after);
                    checked++;
                }
            }
            for (String classPathValue : new String[]{null, "fixture-classpath", "missing/ZKM.jar"}) {
                String before = run(args[0], args, fixtureClasses, temporary, "missing-class", 0, classPathValue);
                String after = run(args[1], args, fixtureClasses, temporary, "missing-class", 0, classPathValue);
                if (!before.equals(after)) throw new AssertionError("missing-class/" + classPathValue + "\n" + before + "\n" + after);
                checked++;
            }
            System.out.println("PASS: " + checked + " entry-point output, exit and redirection-failure comparisons");
        } finally {
            try (java.util.stream.Stream<Path> paths = Files.walk(temporary)) {
                paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                    try { Files.deleteIfExists(path); } catch (IOException exception) { throw new UncheckedIOException(exception); }
                });
            }
        }
    }
    private static void setStatic(Field field, Object value) throws Exception {
        Class<?> unsafeType = Class.forName("sun.misc.Unsafe");
        Field singleton = unsafeType.getDeclaredField("theUnsafe");
        singleton.setAccessible(true);
        Object unsafe = singleton.get(null);
        Object base = unsafeType.getMethod("staticFieldBase", Field.class).invoke(unsafe, field);
        long offset = (Long) unsafeType.getMethod("staticFieldOffset", Field.class).invoke(unsafe, field);
        if (field.getType() == boolean.class) unsafeType.getMethod("putBoolean", Object.class, long.class, boolean.class).invoke(unsafe, base, offset, value);
        else unsafeType.getMethod("putObject", Object.class, long.class, Object.class).invoke(unsafe, base, offset, value);
    }
    private static String run(String classes, String[] args, Path fixtureClasses, Path temporary, String mode, int redirect, String classPathValue) throws Exception {
        List<URL> urls = new ArrayList<>();
        urls.add(fixtureClasses.toUri().toURL());
        urls.add(Paths.get(classes).toUri().toURL());
        for (int i = 2; i < args.length; i++) urls.add(Paths.get(args[i]).toUri().toURL());
        try (URLClassLoader loader = new URLClassLoader(urls.toArray(new URL[0]), null) {
            @Override protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (mode.equals("missing-class") && name.equals(TARGET)) throw new NoClassDefFoundError("fixture missing class");
                return super.loadClass(name, resolve);
            }
        }) {
            Class<?> flags = Class.forName("com.zelix.klassmaster.config.HiddenOptionFlags", true, loader);
            Class<?> fileUtils = Class.forName("com.zelix.klassmaster.archive.ZkmFileUtils", true, loader);
            setStatic(flags.getField("DEBUG_REDIRECT_OUT"), redirect != 0);
            setStatic(flags.getField("TEST_JVM_VERSION"), false);
            setStatic(fileUtils.getField("USER_DIR_FILE"), (redirect == 3 ? temporary.resolve("absent/subdirectory") : temporary).toFile());
            Method main = Class.forName("com.zelix.ZKM", true, loader).getMethod("main", String[].class);
            PrintStream savedOut = System.out, savedErr = System.err;
            SecurityManager savedManager = System.getSecurityManager();
            String savedClassPath = System.getProperty("java.class.path");
            ByteArrayOutputStream stdout = new ByteArrayOutputStream(), stderr = new ByteArrayOutputStream();
            String failure = null;
            try {
                if (classPathValue == null) System.clearProperty("java.class.path"); else System.setProperty("java.class.path", classPathValue);
                System.setOut(new PrintStream(stdout, true, "UTF-8"));
                System.setErr(new PrintStream(stderr, true, "UTF-8"));
                System.setSecurityManager(new ExitTrap(redirect == 2));
                try { main.invoke(null, (Object) new String[]{mode}); }
                catch (InvocationTargetException exception) {
                    Throwable cause = exception.getTargetException();
                    failure = cause.getClass().getName() + ":" + cause.getMessage();
                }
            } finally {
                System.setSecurityManager(savedManager);
                System.setOut(savedOut); System.setErr(savedErr);
                if (savedClassPath == null) System.clearProperty("java.class.path"); else System.setProperty("java.class.path", savedClassPath);
            }
            String text = "OUT=" + stdout.toString("UTF-8") + ";ERR=" + stderr.toString("UTF-8") + ";FAILURE=" + failure;
            // Redirect filenames and stacktrace source lines are intentionally nondeterministic implementation details.
            return text.replaceAll("RedirectedOut_[0-9]+", "RedirectedOut_TIMESTAMP")
                    .replaceAll("\\.java:[0-9]+", ".java:LINE")
                    .replaceAll("(?m)^WARNING:.*\\R?", "");
        }
    }
}

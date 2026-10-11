import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.util.*;

/** Differential parser/translator checks. No production files are written. */
public final class ParserEquivalence {
    private static long comparisons;
    public static void main(String[] args) throws Exception {
        try (URLClassLoader before = loader(args.length > 0 ? args[0] : "build/deobfuscation/baseline/classes");
             URLClassLoader after = loader(args.length > 1 ? args[1] : "build/classes/java/main")) {
            checkConfigOptions(before, after);
            checkGrammarBranches(before, after);
            checkFileFilters(before, after);
            checkStreamReset(before, after);
            checkChangeLogs(before, after);
            checkErrorSequences(before, after);
            checkQuotedLists(before, after);
            checkFrameCandidates(before, after);
            checkScriptGeneration(before, after);
        }
        System.out.println("Parser equivalence: " + comparisons + " comparisons passed");
    }
    private static URLClassLoader loader(String classes) throws Exception {
        List<URL> urls = new ArrayList<URL>();
        urls.add(new File(classes).toURI().toURL());
        urls.add(new File("build/resources/main").toURI().toURL());
        File[] files = new File("lib").listFiles();
        if (files != null) for (File file : files) if (file.getName().endsWith(".jar")) urls.add(file.toURI().toURL());
        return new URLClassLoader(urls.toArray(new URL[0]), null);
    }
    private static void same(String name, Object left, Object right) {
        comparisons++;
        if (!Objects.equals(left, right)) throw new AssertionError(name + "\nExpected: " + left + "\nActual: " + right);
    }
    private static Field field(Class<?> type, String name) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }
    private static String failure(Throwable exception) {
        if (exception instanceof InvocationTargetException) exception = ((InvocationTargetException) exception).getCause();
        // The display name changed; parser diagnostics otherwise remain equivalent.
        String message = String.valueOf(exception.getMessage()).replace("Zelix KlassMaster", "EDZKM");
        return exception.getClass().getName() + (exception instanceof NullPointerException ? "" : ":" + message);
    }
    private static String node(Object value) throws Exception {
        if (value == null) return "null";
        Class<?> type = value.getClass();
        StringBuilder result = new StringBuilder(type.getSimpleName());
        try { result.append('=').append(type.getMethod("getValue").invoke(value)); } catch (NoSuchMethodException ignored) {}
        int children = (Integer) type.getMethod("jjtGetNumChildren").invoke(value);
        result.append('[');
        for (int i = 0; i < children; i++) result.append(node(type.getMethod("jjtGetChild", int.class).invoke(value, i))).append(',');
        return result.append(']').toString();
    }
    private static String parse(ClassLoader loader, String className, String method, String input, boolean staticParser) throws Exception {
        Class<?> type = loader.loadClass(className);
        Object parser = null;
        String outcome;
        try {
            if (staticParser && field(type, "jj_initialized_once").getBoolean(null)) {
                type.getMethod("ReInit", Reader.class).invoke(null, new StringReader(input));
            } else {
                parser = type.getConstructor(Reader.class).newInstance(new StringReader(input));
            }
            Object ast = type.getMethod(method).invoke(parser);
            if (ast == null) {
                Object state = type.getField("jjtree").get(parser);
                List<?> nodes = (List<?>) state.getClass().getField("nodes").get(state);
                ast = nodes.isEmpty() ? null : nodes.get(nodes.size() - 1);
            }
            outcome = node(ast);
        } catch (Throwable exception) {
            outcome = failure(exception);
        }
        if (staticParser || parser != null) {
            outcome += "/lookahead=" + Arrays.toString((int[]) field(type, "jj_la1").get(parser));
            outcome += "/generation=" + field(type, "jj_gen").get(parser);
        }
        return outcome;
    }
    private static void checkConfigOptions(ClassLoader before, ClassLoader after) throws Exception {
        String type = "com.zelix.klassmaster.proguard.config.parser.ProGuardConfigParser";
        String[][] options = {{"DontNoteOption", "-dontnote"}, {"AdaptResourceFileContentsOption", "-adaptresourcefilecontents"},
                {"AdaptResourceFilenamesOption", "-adaptresourcefilenames"}, {"KeepDirectoriesOption", "-keepdirectories"},
                {"AdaptClassStringsOption", "-adaptclassstrings"}, {"KeepPackageNamesOption", "-keeppackagenames"}, {"KeepAttributesOption", "-keepattributes"}};
        for (String[] option : options) {
            for (String suffix : new String[]{"", " a", " a,b", " \"a\",\"b\"", " *", " !a", " a,", " ,", " a,,b", " a\n-dontshrink", " a,\"unterminated"}) {
                String input = option[1] + suffix;
                same(input, parse(before, type, option[0], input, false), parse(after, type, option[0], input, false));
            }
        }
        String script = "-dontnote a,b\n-keepattributes Signature,*Annotation*\n-adaptclassstrings \"a\"\n-keepdirectories\n-dontshrink\n";
        same("complete config", parse(before, type, "BomClause", script, false), parse(after, type, "BomClause", script, false));
    }
    private static void checkChangeLogs(ClassLoader before, ClassLoader after) throws Exception {
        String type = "com.zelix.klassmaster.changelog.parser.ChangeLogParser";
        List<String> inputs = new ArrayList<String>(Arrays.asList("", "\ufeff", "// comment\n", "garbage", "Package:", "Module: a NameNotChanged", "Package: a => b", "Package: a NameNotChanged", "Class: a => b", "AutoReflectionClass: a", "ObfuscateReferencesClass: b", "MethodParameterChangeClasses: a b c", "ForwardClass: a", "ForwardClass: a Package: p", "TraceBackClass: a", "TraceBackClass: a ForwardClass: b", "ForwardClass: a ForwardClass: b", "MemberClass: a", "TraceBackClass: a MemberClass: b", "TraceBackClass: a TraceBackClass: b ForwardClass: c MemberClass: d"));
        String[] sections = {"Module: m NameNotChanged", "Package: p => q", "AutoReflectionClass: a", "ObfuscateReferencesClass: b", "MethodParameterChangeClasses: a b c", "TraceBackClass: t", "ForwardClass: f"};
        for (int mask = 0; mask < 128; mask++) {
            StringBuilder text = new StringBuilder();
            for (int i = 0; i < sections.length; i++) if ((mask & (1 << i)) != 0) text.append(sections[i]).append('\n');
            inputs.add(text.toString());
            inputs.add(text.toString() + "unexpected");
        }
        for (String input : inputs) same("change log: " + input, parse(before, type, "Input", input, true), parse(after, type, "Input", input, true));
    }
    private static String errorSequence(ClassLoader loader, String name, int endPosition, int savedCount, boolean match) throws Exception {
        Class<?> type = loader.loadClass(name);
        boolean isStatic = Modifier.isStatic(field(type, "jj_endpos").getModifiers());
        Object parser = isStatic ? null : type.getConstructor(Reader.class).newInstance(new StringReader(""));
        int[] tokens = new int[100];
        for (int i = 0; i < tokens.length; i++) tokens[i] = i % 7;
        int[] expected = Arrays.copyOf(tokens, Math.max(0, savedCount));
        if (!match && expected.length > 0) expected[expected.length - 1] = 1000;
        List<int[]> entries = new ArrayList<int[]>(); entries.add(new int[]{999}); entries.add(expected);
        field(type, "jj_lasttokens").set(parser, tokens);
        field(type, "jj_endpos").setInt(parser, savedCount);
        field(type, "jj_expentries").set(parser, entries);
        String outcome = "ok";
        try { type.getMethod("jj_add_error_token", int.class, int.class).invoke(parser, 42, endPosition); }
        catch (Throwable exception) { outcome = failure(exception); }
        return outcome + "/" + field(type, "jj_endpos").get(parser) + "/" + Arrays.toString(tokens) + "/" + Arrays.deepToString(entries.toArray());
    }
    private static void checkErrorSequences(ClassLoader before, ClassLoader after) throws Exception {
        for (String name : new String[]{"com.zelix.klassmaster.changelog.parser.ChangeLogParser", "com.zelix.klassmaster.proguard.mapping.parser.ProGuardMappingParser", "com.zelix.klassmaster.proguard.config.parser.ProGuardConfigParser"}) {
            for (int count : new int[]{0, 1, 2, 10, 99}) for (int end : new int[]{0, 1, 2, 3, 99, 100}) for (boolean match : new boolean[]{false, true})
                same("expected token sequence " + name, errorSequence(before, name, end, count, match), errorSequence(after, name, end, count, match));
        }
    }
    private static String quoted(ClassLoader loader, String suffix, int width, int count) throws Exception {
        Class<?> setType = loader.loadClass("com.zelix.klassmaster.util.SyncIndexedSet"); Object set = setType.getConstructor().newInstance();
        for (int i = 0; i < count; i++) setType.getMethod("add", Object.class).invoke(set, "path" + i + ".jar");
        StringBuilder output = new StringBuilder();
        loader.loadClass("com.zelix.klassmaster.proguard.ProGuardConfigTranslator").getMethod("appendQuotedListStatement", String.class, String.class, int.class, setType, StringBuilder.class).invoke(null, "classpath", suffix, width, set, output);
        return output.toString();
    }
    private static void checkQuotedLists(ClassLoader before, ClassLoader after) throws Exception {
        for (String suffix : new String[]{null, "", " // jars"}) for (int width : new int[]{0, 5, 30}) for (int count = 0; count < 4; count++) same("quoted list", quoted(before, suffix, width, count), quoted(after, suffix, width, count));
    }
    private static Object allocate(Class<?> type) throws Exception {
        Class<?> unsafe = Class.forName("sun.misc.Unsafe"); Field field = unsafe.getDeclaredField("theUnsafe"); field.setAccessible(true);
        return unsafe.getMethod("allocateInstance", Class.class).invoke(field.get(null), type);
    }
    private static List<Object> candidates(ClassLoader loader) throws Exception {
        String prefix = "com.zelix.klassmaster.changelog.";
        Class<?> candidate = loader.loadClass(prefix + "FrameMethodCandidate"); Class<?> resolved = loader.loadClass(prefix + "ResolvedStackTraceLine");
        Class<?> frame = loader.loadClass(prefix + "StackTraceFrameLine"); Class<?> resolver = loader.loadClass("com.zelix.klassmaster.classfile.hierarchy.ClasspathClassResolver");
        List<Object> result = new ArrayList<Object>();
        for (String line : new String[]{null, "at a.b(A.java:1)", "at c.d(C.java:2)"}) for (int hash : new int[]{0, 1, 2}) {
            Object value = allocate(candidate); candidate.getField("hashValue").setInt(value, hash);
            Object trace = line == null ? null : resolved.getConstructor(frame, resolver, boolean.class).newInstance(frame.getConstructor(String.class).newInstance(line), null, false);
            candidate.getField("stackTraceLine").set(value, trace); result.add(value);
        }
        result.add(null); result.add("other"); return result;
    }
    private static void checkFrameCandidates(ClassLoader before, ClassLoader after) throws Exception {
        List<Object> left = candidates(before), right = candidates(after);
        for (boolean opaqueNull : new boolean[]{true, false}) {
            for (ClassLoader loader : new ClassLoader[]{before, after}) {
                Field field = loader.loadClass("com.zelix.klassmaster.changelog.StackTraceTranslator").getDeclaredField("opaqueStrings"); field.setAccessible(true); field.set(null, opaqueNull ? null : new String[0]);
            }
            for (int i = 0; i < left.size() - 2; i++) for (int j = 0; j < left.size(); j++) same("FrameMethodCandidate.equals", left.get(i).equals(left.get(j)), right.get(i).equals(right.get(j)));
        }
    }
    private static String script(ClassLoader loader, int mask) throws Exception {
        Class<?> type = loader.loadClass("com.zelix.klassmaster.proguard.ProGuardConfigTranslator");
        Object translator = type.getConstructor(PrintWriter.class).newInstance(new PrintWriter(new StringWriter()));
        type.getField("dontShrink").setBoolean(translator, (mask & 1) != 0);
        type.getField("dontOptimize").setBoolean(translator, (mask & 2) != 0);
        if ((mask & 4) != 0) type.getField("trimExcludeStatement").set(translator, "trimExclude example.*;");
        if ((mask & 8) != 0) {
            Object set = type.getField("trimExcludes").get(translator); set.getClass().getMethod("add", Object.class).invoke(set, "example.*");
        }
        String output = (String) type.getMethod("buildZkmScript", String.class, boolean.class, boolean.class, List.class).invoke(translator, "test", false, false, new ArrayList<String>());
        // Header branding and time are presentation only; compare all statements verbatim.
        return output.replaceAll("(?m)^/\\*+/\\R[^\\r\\n]*Generated by (?:EDZKM|Zelix KlassMaster)[^\\r\\n]*\\R/\\*+/$", "<generated header>");
    }
    private static void checkScriptGeneration(ClassLoader before, ClassLoader after) throws Exception {
        for (int mask = 0; mask < 16; mask++) same("script generation mask=" + mask, script(before, mask), script(after, mask));
    }
    private static void checkGrammarBranches(ClassLoader before, ClassLoader after) throws Exception {
        String changeLog = "com.zelix.klassmaster.changelog.parser.ChangeLogParser";
        String mapping = "com.zelix.klassmaster.proguard.mapping.parser.ProGuardMappingParser";
        String config = "com.zelix.klassmaster.proguard.config.parser.ProGuardConfigParser";
        Map<String, String[]> productions = new LinkedHashMap<String, String[]>();
        productions.put("NameList", new String[]{"a", "a.b/c", "a.", "a/", "a.b/", ".a", "a b"});
        productions.put("MethodSignature", new String[]{"a()", "a(int)", "a(int,java.lang.String[])", "a(,)", "a(int,)", "a(", "a() => b()"});
        productions.put("LineNumberChange", new String[]{"1 => 2", "1 => 2 and 3", "1 => 2,3 and 4", "1 => 2,3,4 and 5", "1 => 2,", "1 => 2 and", "1 => 2,3", "1 => 2,3,", "1 => 2,3 and"});
        productions.put("MethodNameChange", new String[]{"a() NameNotChanged", "a() => b", "a() => b()", "a() => b() => c()", "a() =>", "a()", "void a(int) => b", "public void a(int) => b()", "a() NameNotChanged trailing"});
        productions.put("PackageChange", new String[]{"Package: a => b", "Package: a =>", "Package: a NameNotChanged", "Package: a => b trailing", "Package: a", "Package: a => ;"});
        productions.put("ClassChange", new String[]{"Class: a => b", "Class: a NameNotChanged", "Class: a => b FieldsOf: a", "Class: a => b MethodsOf: a", "Class: a => b LineNumbersOf: a", "Class: a => b FieldsOf: a int f => g MethodsOf: a void a() => b LineNumbersOf: a 1 => 2"});
        for (Map.Entry<String, String[]> entry : productions.entrySet()) for (String input : entry.getValue())
            same(entry.getKey() + " " + input, parse(before, changeLog, entry.getKey(), input, true), parse(after, changeLog, entry.getKey(), input, true));
        for (String newline : new String[]{"\n", "\r", "\r\n", "\n\n", ""}) for (String members : new String[]{"", "    int x -> a", "    void f() -> b", "    1:4:void f(int,java.lang.String[]) -> c", "    int x -> a\n    void f() -> b", "    ???"}) {
            String input = "example.A -> a:" + newline + members + newline;
            same("mapping " + input, parse(before, mapping, "Input", input, true), parse(after, mapping, "Input", input, true));
        }
        for (String body : new String[]{"", "public", "private static", "@example.A public", "public void f();", "int x;", "<fields>;", "*;", "public int x; private void f();", "@a public void f();", "???"}) {
            String input = "-keep class example.A { " + body + " }";
            same("keep grammar " + body, parse(before, config, "BomClause", input, false), parse(after, config, "BomClause", input, false));
        }
    }
    private static String fileFilter(ClassLoader loader, Set<String> filters) throws Exception {
        Class<?> holderType = loader.loadClass("com.zelix.klassmaster.util.ObservableHolder");
        Object holder = holderType.getConstructor().newInstance();
        Class<?> optionType = loader.loadClass("com.zelix.klassmaster.proguard.config.parser.ast.ASTInJarsOption");
        String result;
        try { result = (String) optionType.getMethod("buildFileFilterExpression", Set.class, holderType).invoke(null, filters, holder); }
        catch (Throwable exception) { result = failure(exception); }
        return result + "/warning=" + holderType.getMethod("getValue").invoke(holder);
    }
    private static void checkFileFilters(ClassLoader before, ClassLoader after) throws Exception {
        String[] choices = {"", "!", "*.class", "!*.class", "*.xml", "!*.xml", " **/*.class ", "!**/*.xml", "noExtension", null};
        same("empty filters", fileFilter(before, new LinkedHashSet<String>()), fileFilter(after, new LinkedHashSet<String>()));
        for (String first : choices) for (String second : choices) for (String third : choices) {
            Set<String> filters = new LinkedHashSet<String>(Arrays.asList(first, second, third));
            same("file filters " + filters, fileFilter(before, filters), fileFilter(after, filters));
        }
    }
    private static String streamReset(ClassLoader loader, int length, int available) throws Exception {
        Class<?> type = loader.loadClass("com.zelix.klassmaster.proguard.mapping.parser.ProGuardMappingSimpleCharStream");
        Object stream = allocate(type);
        char[] buffer = length < 0 ? null : new char[length];
        type.getField("buffer").set(null, buffer);
        type.getField("available").setInt(null, available);
        type.getField("bufsize").setInt(null, 77);
        type.getField("prevCharIsCR").setBoolean(null, true);
        type.getField("prevCharIsLF").setBoolean(null, true);
        type.getMethod("ReInit_v2", Reader.class).invoke(stream, new StringReader("abc"));
        StringBuilder result = new StringBuilder();
        result.append(type.getField("buffer").get(null) == buffer);
        for (String name : new String[]{"available", "bufsize", "prevCharIsCR", "prevCharIsLF", "maxNextCharInd", "inBuf", "tokenBegin", "bufpos", "line", "column"}) result.append('/').append(type.getField(name).get(null));
        return result.toString();
    }
    private static void checkStreamReset(ClassLoader before, ClassLoader after) throws Exception {
        for (int length : new int[]{-1, 0, 32, 4096, 8192}) for (int available : new int[]{0, 5, 4096}) same("stream reset", streamReset(before, length, available), streamReset(after, length, available));
    }

}

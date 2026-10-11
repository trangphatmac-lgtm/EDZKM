import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Differential checks for the structured control-flow cleanup, without linking either implementation. */
public final class FlowEquivalenceChecks {
    private static int checked;
    private static final String PREFIX = "com.zelix.klassmaster.";
    private interface Action { Object run(URLClassLoader loader) throws Exception; }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            throw new IllegalArgumentException("Usage: FlowEquivalenceChecks baseline-classes current-classes [dependency.jar ...]");
        }
        Path temporary = Files.createTempDirectory("zkm-flow-equivalence-");
        try (URLClassLoader baseline = loader(args[0], args); URLClassLoader current = loader(args[1], args)) {
            checkProperties(baseline, current, temporary);
            checkFileOperations(baseline, current, temporary);
            checkNameLists(baseline, current, temporary);
            checkJarIndexes(baseline, current);
            checkArchiveEffects(baseline, current, temporary);
            checkXmlNodes(baseline, current);
            checkXmlHandlers(baseline, current);
            checkXmlParser(baseline, current);
            checkAstFormatting(baseline, current);
            System.out.println("PASS: " + checked + " baseline/current control-flow comparisons");
        } finally {
            try (java.util.stream.Stream<Path> paths = Files.walk(temporary)) {
                paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                    try { Files.deleteIfExists(path); } catch (IOException exception) { throw new UncheckedIOException(exception); }
                });
            }
        }
    }

    private static URLClassLoader loader(String directory, String[] args) throws Exception {
        List<URL> urls = new ArrayList<>();
        urls.add(Paths.get(directory).toUri().toURL());
        for (int i = 2; i < args.length; i++) urls.add(Paths.get(args[i]).toUri().toURL());
        return new URLClassLoader(urls.toArray(new URL[0]), null);
    }
    private static Class<?> type(URLClassLoader loader, String name) throws Exception { return Class.forName(PREFIX + name, true, loader); }
    private static Object invoke(Object target, String name, Class<?>[] parameters, Object... arguments) throws Exception {
        return target.getClass().getMethod(name, parameters).invoke(target, arguments);
    }
    private static String outcome(URLClassLoader loader, Action action) {
        try { return "VALUE: " + String.valueOf(action.run(loader)); }
        catch (Throwable error) {
            while (error instanceof InvocationTargetException) error = ((InvocationTargetException) error).getTargetException();
            // Helpful NPE messages describe compiler local slots and are not a stable behavioral contract.
            return "THROW: " + error.getClass().getName() + (error instanceof NullPointerException ? "" : ": " + error.getMessage());
        }
    }
    private static void equal(URLClassLoader baseline, URLClassLoader current, String caseName, Action action) {
        String before = outcome(baseline, action), after = outcome(current, action);
        if (!before.equals(after)) throw new AssertionError(caseName + "\nBASELINE " + before + "\nCURRENT  " + after);
        checked++;
    }

    private static void checkProperties(URLClassLoader baseline, URLClassLoader current, Path directory) throws Exception {
        Path script = directory.resolve("empty.zkm");
        Files.write(script, new byte[0]);
        Map<URLClassLoader,Object> processors = new IdentityHashMap<>();
        Properties properties = new Properties();
        properties.setProperty("key", "local replacement");
        properties.setProperty("empty", "");
        properties.setProperty("%", "nested percent");
        String systemKey = "zkm.flow.equivalence.property";
        String previous = System.getProperty(systemKey);
        System.setProperty(systemKey, "system replacement");
        try {
            for (URLClassLoader loader : new URLClassLoader[]{baseline, current}) {
                processors.put(loader, type(loader, "script.ScriptPreprocessor").getConstructor(String.class, Properties.class)
                        .newInstance(script.toString(), properties));
            }
            List<String> inputs = new ArrayList<>(Arrays.asList("", "%", "%%", "%x%", "%key%", "%key% ", "%empty%", "%missing%",
                    "<%key%>", "x<%key%>", "%>key%", "%key%%key%", "%" + systemKey + "%", null));
            Random random = new Random(730201L);
            String alphabet = "<>% keyempty/\t\n";
            for (int sample = 0; sample < 12000; sample++) {
                StringBuilder input = new StringBuilder();
                int length = random.nextInt(60);
                for (int i = 0; i < length; i++) input.append(alphabet.charAt(random.nextInt(alphabet.length())));
                inputs.add(input.toString());
            }
            for (String input : inputs) equal(baseline, current, "property input " + input,
                    loader -> invoke(processors.get(loader), "substituteProperties", new Class<?>[]{String.class}, input));
            for (URLClassLoader loader : new URLClassLoader[]{baseline, current}) processors.get(loader).getClass().getField("properties").set(processors.get(loader), null);
            for (String input : inputs.subList(0, 15)) equal(baseline, current, "system-only property input " + input,
                    loader -> invoke(processors.get(loader), "substituteProperties", new Class<?>[]{String.class}, input));
        } finally {
            if (previous == null) System.clearProperty(systemKey); else System.setProperty(systemKey, previous);
        }
    }

    private static void checkFileOperations(URLClassLoader baseline, URLClassLoader current, Path directory) {
        for (String method : Arrays.asList("writeStringToFile", "canWriteAndReadFile")) {
            for (int scenario = 0; scenario < 4; scenario++) {
                final int variant = scenario;
                equal(baseline, current, method + " scenario " + scenario, loader -> {
                    Path target = variant < 2 ? directory.resolve("round-trip.txt") : directory.resolve("missing-parent/out.txt");
                    Files.deleteIfExists(target);
                    Class<?> holderType = type(loader, "util.ObservableHolder");
                    Object holder = variant % 2 == 0 ? holderType.getConstructor().newInstance() : null;
                    Class<?> utils = type(loader, "archive.ZkmFileUtils");
                    Object value;
                    try {
                        value = method.equals("writeStringToFile")
                                ? utils.getMethod(method, String.class, File.class, holderType).invoke(null, "alpha\nβeta", target.toFile(), holder)
                                : utils.getMethod(method, String.class, holderType).invoke(null, target.toString(), holder);
                    } catch (InvocationTargetException exception) {
                        return "exception=" + exception.getTargetException().getClass().getName() + ";exists=" + Files.exists(target);
                    }
                    String content = Files.exists(target) ? new String(Files.readAllBytes(target), StandardCharsets.UTF_8) : "absent";
                    Object error = holder == null ? null : invoke(holder, "getValue", new Class<?>[0]);
                    return value + ";content=" + content + ";error=" + error;
                });
            }
        }
    }

    private static void checkNameLists(URLClassLoader baseline, URLClassLoader current, Path directory) throws Exception {
        String[] inputs = {"", "alpha\nbeta\nalpha\n", "#comment\n/comment\na/comment\nname\n", "good\n1bad\nhas space\n",
                "  leadingAndTrailing  \n\t\n", "a\\u0042\n\\u0020\n", "a\\nB\n", "\\uZZZZ\n", "\\\\n\n"};
        for (String input : inputs) equal(baseline, current, "name-list " + input, loader -> {
            Path names = directory.resolve("names.txt");
            Files.write(names, input.getBytes(StandardCharsets.UTF_8));
            StringWriter log = new StringWriter();
            Class<?> environmentType = type(loader, "script.ScriptEnvironment");
            Object environment = environmentType.getConstructor(type(loader, "classfile.hierarchy.ClassRepository"),
                    type(loader, "classfile.hierarchy.ZkmClasspath"), String.class, PrintWriter.class)
                    .newInstance(null, null, directory.resolve("names.log").toString(), new PrintWriter(log));
            String failure = null;
            Object namesRead = null;
            try {
                namesRead = type(loader, "engine.ObfuscationEngine").getMethod("readNameListFile", String.class, String.class, environmentType)
                        .invoke(null, names.toString(), "newClassNameFile", environment);
            } catch (InvocationTargetException exception) {
                failure = exception.getTargetException().getClass().getName() + ":" + exception.getTargetException().getMessage();
            }
            return "names=" + namesRead + ";log=" + log + ";failure=" + failure;
        });
    }

    private static void checkJarIndexes(URLClassLoader baseline, URLClassLoader current) {
        for (int scenario = 0; scenario < 6; scenario++) {
            final int variant = scenario;
            equal(baseline, current, "jar index candidate scenario " + scenario, loader -> {
                Class<?> archiveType = type(loader, "archive.SourceArchive");
                Object source = archiveType.getConstructor(String.class, long.class, String.class).newInstance("main.jar", 0L, "main.jar");
                Object first = archiveType.getConstructor(String.class, long.class, String.class).newInstance("a/lib.jar", 0L, "a/lib.jar");
                Object second = archiveType.getConstructor(String.class, long.class, String.class).newInstance("b/lib.jar", 0L, "b/lib.jar");
                Class<?> multiMapType = type(loader, "util.SetMultiMap");
                Object resources = multiMapType.getConstructor().newInstance(), packages = multiMapType.getConstructor().newInstance();
                Method add = multiMapType.getMethod("addValue", Object.class, Object.class);
                add.invoke(resources, source, "main-resource");
                add.invoke(resources, first, "entry");
                add.invoke(resources, second, variant == 2 ? "other" : "entry");
                Map<String,Object> archives = new LinkedHashMap<>();
                if (variant != 0) archives.put("a/lib.jar", first);
                if (variant >= 2) archives.put("b/lib.jar", second);
                Class<?> sectionType = type(loader, "archive.JarIndexSection");
                Object section = sectionType.getConstructor(BufferedReader.class).newInstance(new BufferedReader(new StringReader("lib.jar\nentry\n\n")));
                if (variant == 4) invoke(section, "markAsFirstSection", new Class<?>[0]);
                if (variant == 5) resources = multiMapType.getConstructor().newInstance();
                return sectionType.getMethod("resolveEntries", String.class, archiveType, Map.class, multiMapType, multiMapType)
                        .invoke(section, "lib.jar", source, archives, resources, packages);
            });
        }
    }

    // Allocate a fixture without running ClassArchiveSaver's eager, whole-archive constructor.
    private static Object allocate(Class<?> type) throws Exception {
        Class<?> unsafeType = Class.forName("sun.misc.Unsafe");
        Field field = unsafeType.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return unsafeType.getMethod("allocateInstance", Class.class).invoke(field.get(null), type);
    }
    private static void checkArchiveEffects(URLClassLoader baseline, URLClassLoader current, Path directory) throws Exception {
        for (int scenario = 0; scenario < 4; scenario++) {
            final int variant = scenario;
            equal(baseline, current, "archive duplicate close/delete ordering " + scenario, loader -> {
                Class<?> saverType = type(loader, "archive.ClassArchiveSaver");
                Object saver = allocate(saverType);
                Class<?> archiveType = type(loader, "archive.SourceArchive");
                Object original = archiveType.getConstructor(String.class, long.class, String.class).newInstance("old.jar", 0L, "old.jar");
                Object incoming = archiveType.getConstructor(String.class, long.class, String.class).newInstance("new.jar", 0L, "new.jar");
                Path outputPath = directory.resolve("duplicate-output");
                if (Files.isDirectory(outputPath)) Files.deleteIfExists(outputPath.resolve("child"));
                Files.deleteIfExists(outputPath);
                if (variant == 3) {
                    Files.createDirectory(outputPath);
                    Files.write(outputPath.resolve("child"), new byte[]{1});
                } else {
                    Files.write(outputPath, new byte[]{1, 2, 3});
                }
                List<String> effects = new ArrayList<>();
                java.util.zip.ZipOutputStream output = new java.util.zip.ZipOutputStream(new ByteArrayOutputStream()) {
                    @Override public void close() throws IOException {
                        effects.add("close;file-exists=" + Files.exists(outputPath));
                        if (variant == 2) throw new IOException("synthetic close failure");
                        super.close();
                    }
                };
                saverType.getField("archiveOutputStreams").set(saver, Collections.singletonMap(original, output));
                saverType.getField("outputArchiveFiles").set(saver, Collections.singletonMap(original, outputPath.toFile()));
                Map<String,Object> registered = new HashMap<>();
                if (variant != 0) registered.put("same.jar", original);
                String failure = null;
                try {
                    saverType.getMethod("registerOutputArchivePath", String.class, archiveType, Map.class, String.class)
                            .invoke(saver, "same.jar", incoming, registered, "fixture");
                } catch (InvocationTargetException exception) { failure = exception.getTargetException().getClass().getName(); }
                // The null reporter deliberately stops after deletion; failures must preserve the file.
                return effects + ";exists=" + Files.exists(outputPath) + ";failure=" + failure + ";registered=" + registered.size();
            });
        }
        Path sourcePath = directory.resolve("source.zip");
        try (java.util.zip.ZipOutputStream output = new java.util.zip.ZipOutputStream(Files.newOutputStream(sourcePath))) {
            java.util.zip.ZipEntry entry = new java.util.zip.ZipEntry("old/path.txt");
            entry.setTime(1_600_000_000_000L);
            output.putNextEntry(entry);
            output.write("archive payload".getBytes(StandardCharsets.UTF_8));
            output.closeEntry();
        }
        for (boolean preserve : new boolean[]{false, true}) for (Boolean compressed : new Boolean[]{null, false, true}) {
            equal(baseline, current, "archive renamed entry " + preserve + "/" + compressed, loader -> {
                Class<?> saverType = type(loader, "archive.ClassArchiveSaver");
                Object saver = allocate(saverType);
                Object entries = type(loader, "util.SetMultiMap").getConstructor().newInstance();
                saverType.getField("writtenEntries").set(saver, entries);
                saverType.getField("entryTimestamp").set(saver, Long.valueOf(1_500_000_000_000L));
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                java.util.zip.ZipOutputStream output = new java.util.zip.ZipOutputStream(bytes);
                try (java.util.zip.ZipFile input = new java.util.zip.ZipFile(sourcePath.toFile())) {
                    saverType.getMethod("copyRenamedEntry", java.util.zip.ZipFile.class, java.util.zip.ZipEntry.class,
                            java.util.zip.ZipOutputStream.class, String.class, String.class, Boolean.class, boolean.class)
                            .invoke(saver, input, input.getEntry("old/path.txt"), output, "old/path.txt", "new/path.txt", compressed, preserve);
                }
                Object written = invoke(entries, "getValues", new Class<?>[]{Object.class}, output);
                output.close();
                return Arrays.toString(bytes.toByteArray()) + ";written=" + written;
            });
        }
    }

    private static void setPredicate(URLClassLoader loader, String className, boolean value) throws Exception {
        Field flag = type(loader, className).getDeclaredField("predicateFlag");
        flag.setAccessible(true);
        flag.setBoolean(null, value);
    }
    private static Object node(URLClassLoader loader, String tag, Map<String,String> attributes) throws Exception {
        Object node = type(loader, "xml.XmlElementNode").getConstructor().newInstance();
        invoke(node, "addName", new Class<?>[]{String.class}, tag);
        for (Map.Entry<String,String> entry : attributes.entrySet()) {
            invoke(node, "addRawText", new Class<?>[]{String.class}, " ");
            invoke(node, "addName", new Class<?>[]{String.class}, entry.getKey());
            invoke(node, "addRawText", new Class<?>[]{String.class}, "=\"");
            invoke(node, "addAttributeValue", new Class<?>[]{String.class}, entry.getValue());
            invoke(node, "addRawText", new Class<?>[]{String.class}, "\"");
        }
        return node;
    }
    private static void checkXmlNodes(URLClassLoader baseline, URLClassLoader current) {
        for (boolean flag : new boolean[]{false, true}) {
            for (int variant = 0; variant < 3; variant++) {
                final int scenario = variant;
                equal(baseline, current, "node predicate " + flag + " tokens " + variant, loader -> {
                    setPredicate(loader, "xml.XmlResourceParserBase", flag);
                    Object node = node(loader, "bean", Collections.singletonMap("id", "one"));
                    List<Object> tokens = (List<Object>) node.getClass().getField("tokens").get(node);
                    if (scenario == 1) tokens.clear();
                    if (scenario == 2) { tokens.clear(); tokens.add("raw-first"); tokens.add("raw-second"); }
                    // Holder.toString() uses identity; compare only its stable class when raw mode selects it.
                    String rendered = node.toString().replaceAll("@[0-9a-f]+", "@identity");
                    return rendered;
                });
            }
        }
    }
    private static void checkXmlHandlers(URLClassLoader baseline, URLClassLoader current) {
        String[] handlers = {"AndroidManifestXmlHandler", "HibernateMappingHandler", "IbatisXmlHandler", "E4XmiXmlHandler", "SpringXmlUpdater"};
        String[] tags = {"manifest", "activity", "receiver", "hibernate-mapping", "class", "id", "property", "many-to-one", "generator",
                "one-to-many", "many-to-many", "set", "list", "subclass", "joined-subclass", "unknown", "typeAlias", "registration-listener", "reference-listener", "bean"};
        for (String handlerName : handlers) for (String tag : tags) for (boolean flag : new boolean[]{false, true}) {
            equal(baseline, current, handlerName + "/" + tag + " predicate " + flag, loader -> {
                setPredicate(loader, "xml.XmlResourceParserBase", true);
                setPredicate(loader, "xml.XmlConfigFileHandler", flag);
                Class<?> mapType = type(loader, "util.EnumerableMap");
                Object renameMap = mapType.getConstructor(Map.class).newInstance(Collections.singletonMap("old/pkg", "new/pkg"));
                Object translator = type(loader, "xml.ResourcePathTranslator").getConstructor(mapType, Set.class).newInstance(renameMap, Collections.emptySet());
                Class<?> handlerType = type(loader, "xml." + handlerName);
                Object handler = handlerType.getConstructor(String.class, mapType, type(loader, "util.ReadOnlyMultiMap"), type(loader, "util.ReadOnlyMultiMap"),
                        translator.getClass(), type(loader, "classfile.hierarchy.ClassMemberLookup"), type(loader, "classfile.hierarchy.ClassResolver"), type(loader, "log.MessageReporter"))
                        .newInstance("test.xml", renameMap, null, null, translator, null, null, null);
                Map<String,String> attributes = new LinkedHashMap<>();
                attributes.put("package", "old.pkg"); attributes.put("default-access", "field"); attributes.put("sort", "natural");
                attributes.put("ref", "beanId"); attributes.put("registration-method", "register"); attributes.put("unregistration-method", "unregister");
                attributes.put("bind-method", "bind"); attributes.put("unbind-method", "unbind"); attributes.put("alias", "item"); attributes.put("type", "old.pkg.Item");
                attributes.put("path", "platform:/plugin/bundle/old/pkg/file");
                Object element = node(loader, tag, attributes);
                List<Object> output = new ArrayList<>();
                String failure = null;
                try { invoke(handler, "rewriteElement", new Class<?>[]{Object.class, Object.class}, element, output); }
                catch (InvocationTargetException exception) { failure = exception.getTargetException().getClass().getName(); }
                String state = element.toString() + ";output=" + output.size() + ";failure=" + failure;
                if (handlerName.equals("SpringXmlUpdater")) state += ";deferred=" + new TreeSet<>(((Map<?,?>)handlerType.getField("factoryMethodReferences").get(handler)).keySet());
                return state;
            });
        }
    }
    private static void checkXmlParser(URLClassLoader baseline, URLClassLoader current) {
        String[] inputs = {"<root>  <child/> tail </root>", "<root> \n x \t<child/> </root>", "<root>   ", "<root>text   ", "   ",
                "<?xml version=\"1.0\"?><root a=\"b\"><!--c--><![CDATA[x]]></root>", "<root> <unclosed", "<root/>"};
        for (String input : inputs) equal(baseline, current, "XML parser " + input, loader -> {
            setPredicate(loader, "xml.XmlResourceParserBase", true);
            Class<?> handlerType = type(loader, "xml.XmlContentHandler");
            List<String> events = new ArrayList<>();
            Object handler = java.lang.reflect.Proxy.newProxyInstance(loader, new Class<?>[]{handlerType}, (proxy, method, args) -> {
                StringBuilder event = new StringBuilder(method.getName());
                if (args != null) for (Object argument : args) if (!(argument instanceof List) && !(argument instanceof Map)) event.append('|').append(argument);
                events.add(event.toString());
                if (method.getReturnType() == boolean.class) return true;
                if (args != null && method.getName().startsWith("rewrite")) {
                    for (Object argument : args) if (argument instanceof List) ((List<Object>)argument).add(args[0]);
                }
                return null;
            });
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            String failure = null;
            try {
                type(loader, "xml.XmlResourceProcessor").getConstructor(InputStream.class, OutputStream.class, handlerType, String.class,
                        int.class, int.class, int.class, String.class, boolean.class)
                        .newInstance(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)), output, handler, "UTF-8", 0, 0, 0, "test.xml", true);
            } catch (InvocationTargetException exception) { failure = exception.getTargetException().getClass().getName() + ":" + exception.getTargetException().getMessage(); }
            return "events=" + events + ";bytes=" + Arrays.toString(output.toByteArray()) + ";failure=" + failure;
        });
    }    private static void setScriptFlow(URLClassLoader loader, int value) throws Exception {
        Field field = type(loader, "script.parser.ZkmScriptSimpleNode").getDeclaredField("flowControlKey");
        field.setAccessible(true);
        field.setInt(null, value);
    }
    private static Object spec(URLClassLoader loader, String interfaceName, String text) throws Exception {
        Class<?> specType = type(loader, "obfuscator.exclude." + interfaceName);
        Class<?> nodeType = type(loader, "script.parser.ZkmScriptNode");
        return java.lang.reflect.Proxy.newProxyInstance(loader, new Class<?>[]{specType, nodeType}, (proxy, method, args) -> {
            if (method.getName().equals("getSpecText") || method.getName().equals("getNamePattern")) return text;
            if (method.getReturnType() == boolean.class) return false;
            if (method.getReturnType() == double.class) return 0.0;
            if (method.getReturnType() == int.class) return 0;
            return null;
        });
    }
    private static Object complexSpec(URLClassLoader loader, String className, String text) throws Exception {
        Object value = type(loader, "script.parser.ast." + className).getConstructor().newInstance();
        invoke(value, "jjtAddChild", new Class<?>[]{type(loader, "script.parser.ZkmScriptNode"), int.class}, spec(loader, "DescribableSpec", text), 0);
        return value;
    }
    private static Object accessFlags(URLClassLoader loader, int bits) throws Exception {
        Object value = type(loader, "obfuscator.exclude.AccessFlagsSpec").getConstructor(int.class).newInstance(0);
        for (String name : new String[]{"packageRequired", "packageForbidden"}) {
            Field flag = value.getClass().getDeclaredField(name);
            flag.setAccessible(true);
            flag.setBoolean(value, name.equals("packageRequired") ? (bits & 1) != 0 : (bits & 2) != 0);
        }
        invoke(value, "setStaticRequired", new Class<?>[]{boolean.class}, (bits & 4) != 0);
        return value;
    }
    private static Object methodArguments(URLClassLoader loader, String descriptor) throws Exception {
        return type(loader, "obfuscator.exclude.MethodArgsPattern").getConstructor(int.class, String.class, java.util.regex.Pattern.class)
                .newInstance(2, descriptor, null);
    }
    private static void checkAstFormatting(URLClassLoader baseline, URLClassLoader current) throws Exception {
        for (int flow : new int[]{0, 110}) {
            for (String descriptor : new String[]{null, "*", "?", "I", "[I", "Ljava/lang/String;", "bad", "", "[[Ljava/util/List;"}) {
                equal(baseline, current, "Java type " + flow + "/" + descriptor, loader -> {
                    setScriptFlow(loader, flow);
                    return type(loader, "script.parser.ast.ASTRenameFilterParameter").getMethod("toJavaTypeName", String.class).invoke(null, descriptor);
                });
            }
            for (String descriptor : new String[]{"", "I", "ILjava/lang/String;", "(ILjava/lang/String;)", "[I", "*", "?"}) for (int annotationMode = 0; annotationMode < 3; annotationMode++) {
                final int mode = annotationMode;
                equal(baseline, current, "argument spec " + flow + "/" + descriptor + "/" + mode, loader -> {
                    setScriptFlow(loader, flow);
                    Class<?> annotationType = type(loader, "script.parser.ast.ASTComplexAnnotationSpecifier");
                    Object annotations = mode == 0 ? null : Array.newInstance(annotationType, mode == 1 ? 0 : 8);
                    if (mode == 2) Array.set(annotations, 0, complexSpec(loader, "ASTComplexAnnotationSpecifier", "@First"));
                    return type(loader, "script.parser.ast.ASTRenameFilterParameter").getMethod("formatArgsWithAnnotations",
                            type(loader, "obfuscator.exclude.MethodArgsPattern"), Array.newInstance(annotationType, 0).getClass())
                            .invoke(null, methodArguments(loader, descriptor), annotations);
                });
            }
            for (int combination = 0; combination < 512; combination++) {
                final int bits = combination;
                equal(baseline, current, "member spec " + flow + "/" + bits, loader -> {
                    setScriptFlow(loader, flow);
                    Class<?> annotationType = type(loader, "script.parser.ast.ASTComplexAnnotationSpecifier");
                    Class<?> describableType = type(loader, "obfuscator.exclude.DescribableSpec");
                    Method formatter = type(loader, "script.parser.ast.ASTRenameFilterParameter").getMethod("formatMemberSpec", annotationType,
                            type(loader, "obfuscator.exclude.AccessFlagsSpec"), String.class, describableType, String.class, describableType,
                            type(loader, "obfuscator.exclude.MethodArgsPattern"), Array.newInstance(annotationType, 0).getClass(), List.class, boolean.class);
                    return formatter.invoke(null, (bits & 1) == 0 ? null : complexSpec(loader, "ASTComplexAnnotationSpecifier", "@Member"),
                            (bits & 2) == 0 ? null : accessFlags(loader, (bits >> 6) & 7), (bits & 4) == 0 ? null : "Ljava/lang/String;",
                            (bits & 8) == 0 ? null : spec(loader, "DescribableSpec", "field*"), (bits & 16) == 0 ? null : "method*",
                            (bits & 32) == 0 ? null : spec(loader, "DescribableSpec", "otherMethod"), (bits & 64) == 0 ? null : methodArguments(loader, "I"),
                            null, (bits & 128) == 0 ? Collections.emptyList() : Arrays.asList("java/io/IOException", "java/lang/Exception"), (bits & 256) != 0);
                });
            }
            for (int combination = 0; combination < 256; combination++) {
                final int bits = combination;
                equal(baseline, current, "rename parameter script " + flow + "/" + bits, loader -> {
                    setScriptFlow(loader, flow);
                    Class<?> parameterType = type(loader, "script.parser.ast.ASTRenameFilterParameter");
                    Object parameter = parameterType.getConstructor().newInstance();
                    parameterType.getField("standaloneAnnotation").setBoolean(parameter, (bits & 1) != 0);
                    parameterType.getField("classNamePattern").set(parameter, (bits & 2) == 0 ? null : spec(loader, "ClassNamePattern", "Class*"));
                    parameterType.getField("packagePattern").set(parameter, (bits & 4) == 0 ? null : spec(loader, "PackagePatternSpec", "pkg."));
                    parameterType.getField("classAccessFlags").set(parameter, (bits & 8) == 0 ? null : accessFlags(loader, bits >> 5));
                    parameterType.getField("linkClassName").set(parameter, (bits & 16) == 0 ? null : "Linked<link>");
                    if ((bits & 32) != 0) {
                        parameterType.getField("referencingAnnotation").set(parameter, "pkg/Annotation");
                        parameterType.getField("referencingAnnotationComponent").set(parameter, "value");
                        parameterType.getField("archivePathPattern").set(parameter, "lib/*.jar");
                    }
                    if ((bits & 64) != 0) {
                        parameterType.getField("extendsClassName").set(parameter, "pkg/Base");
                        parameterType.getField("implementsNames").set(parameter, new ArrayList<>(Arrays.asList("pkg/First", "pkg/Second")));
                        parameterType.getField("implementsAnnotations").set(parameter, new ArrayList<>(Arrays.asList(null, null)));
                        parameterType.getField("linkPackageNames").set(parameter, new ArrayList<>(Arrays.asList("pkg/first", "pkg/second")));
                    }
                    if ((bits & 128) != 0) {
                        parameterType.getField("fieldType").set(parameter, "I");
                        parameterType.getField("fieldSpecifier").set(parameter, complexSpec(loader, "ASTComplexFieldSpecifier", "field"));
                        parameterType.getField("classAnnotation").set(parameter, complexSpec(loader, "ASTComplexAnnotationSpecifier", "@Type"));
                    }
                    return invoke(parameter, "toScriptText", new Class<?>[0]);
                });
            }
            for (String className : new String[]{"ASTAndMemberSpecifier", "ASTBracketedMemberSpecifier"}) for (int count = 0; count < 4; count++) for (boolean negated : new boolean[]{false, true}) {
                final int childCount = count;
                equal(baseline, current, "composite spec " + className + "/" + flow + "/" + count + "/" + negated, loader -> {
                    setScriptFlow(loader, flow);
                    Object node = type(loader, "script.parser.ast." + className).getConstructor().newInstance();
                    node.getClass().getField("negated").setBoolean(node, negated);
                    Class<?> nodeType = type(loader, "script.parser.ZkmScriptNode");
                    Object children = Array.newInstance(nodeType, childCount);
                    for (int i = 0; i < childCount; i++) Array.set(children, i, spec(loader, "DescribableSpec", "child" + i));
                    node.getClass().getField("children").set(node, children);
                    return invoke(node, "getSpecText", new Class<?>[0]);
                });
            }
        }
        for (String path : new String[]{"a/Foo.class", "b/Bar.class", "a/readme.txt", "outer.jar!a/Foo.class", "a\\\\Foo.class", "", null}) {
            for (int shape = 0; shape < 6; shape++) {
                final int variant = shape;
                equal(baseline, current, "file filter " + path + "/" + shape, loader -> {
                    Object positive = type(loader, "script.parser.ast.ASTFileFilterComponent").getConstructor().newInstance();
                    Object negative = type(loader, "script.parser.ast.ASTNegatedFileFilterComponent").getConstructor().newInstance();
                    for (Object term : new Object[]{positive, negative}) {
                        term.getClass().getField("directoryPattern").set(term, "*a/");
                        term.getClass().getField("fileNamePattern").set(term, "*.class");
                    }
                    Object filter = type(loader, "script.parser.ast.ASTFileFilter").getConstructor().newInstance();
                    LinkedList<List<Object>> clauses = new LinkedList<>();
                    if (variant == 1) clauses.add(Collections.emptyList());
                    if (variant == 2) clauses.add(Arrays.asList(positive));
                    if (variant == 3) clauses.add(Arrays.asList(negative));
                    if (variant == 4) { clauses.add(Arrays.asList(positive)); clauses.add(Arrays.asList(negative)); }
                    if (variant == 5) clauses.add(Arrays.asList(positive, negative));
                    filter.getClass().getField("orClauses").set(filter, clauses);
                    return invoke(filter, "acceptsPath", new Class<?>[]{Object.class}, path);
                });
            }
        }
    }

}

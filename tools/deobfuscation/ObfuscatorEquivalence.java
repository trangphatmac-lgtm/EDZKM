import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/** Differential checks against saved classes; uses independent static state per implementation. */
public final class ObfuscatorEquivalence {
    private static final String PARAMETERS = "com.zelix.klassmaster.obfuscator.parameters.";
    private static long comparisons;

    public static void main(String[] args) throws Exception {
        String baseline = args.length > 0 ? args[0] : "build/deobfuscation/baseline/classes";
        String current = args.length > 1 ? args[1] : "build/classes/java/main";
        try (URLClassLoader original = loader(baseline); URLClassLoader updated = loader(current)) {
            checkBitPermutation(original, updated);
            checkAddedParameterEquality(original, updated);
            checkInstructionReasonEquality(original, updated);
            checkReflectionValues(original, updated);
            checkParameterLayouts(original, updated);
            checkDescriptorConstruction(original, updated);
            checkHelperAssignments(original, updated);
        }
        System.out.println("Obfuscator equivalence: " + comparisons + " comparisons passed");
    }

    private static URLClassLoader loader(String classes) throws Exception {
        List<URL> urls = new ArrayList<URL>();
        urls.add(new File(classes).toURI().toURL());
        urls.add(new File("build/resources/main").toURI().toURL());
        File[] libraries = new File("lib").listFiles();
        if (libraries != null) {
            Arrays.sort(libraries);
            for (File library : libraries) {
                if (library.getName().endsWith(".jar")) urls.add(library.toURI().toURL());
            }
        }
        return new URLClassLoader(urls.toArray(new URL[0]), null);
    }

    private static void same(String caseName, Object expected, Object actual) {
        comparisons++;
        if (!java.util.Objects.equals(expected, actual)) {
            throw new AssertionError(caseName + " expected=" + expected + " actual=" + actual);
        }
    }

    private static void flag(ClassLoader loader, String className, String name, boolean value) throws Exception {
        Field field = Class.forName(className, true, loader).getDeclaredField(name);
        field.setAccessible(true);
        field.setBoolean(null, value);
    }

    private static Object call(Method method, Object receiver, Object... args) throws Exception {
        try {
            return method.invoke(receiver, args);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            // Helpful NPE diagnostics embed local-variable names and change after a rename.
            return "throws:" + cause.getClass().getName()
                    + (cause instanceof NullPointerException ? "" : ":" + cause.getMessage());
        }
    }

    private static void checkBitPermutation(ClassLoader original, ClassLoader updated) throws Exception {
        Method before = original.loadClass(PARAMETERS + "MethodParameterChanger").getMethod("permuteBits", long.class, int[].class);
        Method after = updated.loadClass(PARAMETERS + "MethodParameterChanger").getMethod("permuteBits", long.class, int[].class);
        Method rangeBefore = original.loadClass(PARAMETERS + "MethodParameterChanger").getMethod("extractBitRange", long.class, int.class, int.class);
        Method rangeAfter = updated.loadClass(PARAMETERS + "MethodParameterChanger").getMethod("extractBitRange", long.class, int.class, int.class);
        Random random = new Random(4871361L);
        for (boolean predicate : new boolean[]{false, true}) {
            flag(original, PARAMETERS + "OpaquePredicateBase", "predicateFlag", predicate);
            flag(updated, PARAMETERS + "OpaquePredicateBase", "predicateFlag", predicate);
            for (int length = 0; length <= 65; length++) {
                for (int sample = 0; sample < 128; sample++) {
                    int[] shifts = new int[length];
                    for (int i = 0; i < length; i++) shifts[i] = random.nextInt(513) - 256;
                    long value = sample == 0 ? 0 : sample == 1 ? -1L : random.nextLong();
                    same("permuteBits flag=" + predicate + " length=" + length,
                            call(before, null, value, shifts), call(after, null, value, shifts));
                }
            }
            same("permuteBits null", call(before, null, 1L, null), call(after, null, 1L, null));
            for (int firstBit = -2; firstBit <= 65; firstBit++) {
                for (int lastBit = -2; lastBit <= 65; lastBit++) {
                    long value = random.nextLong();
                    same("extractBitRange", call(rangeBefore, null, value, firstBit, lastBit), call(rangeAfter, null, value, firstBit, lastBit));
                }
            }
        }
    }

    private static void checkAddedParameterEquality(ClassLoader original, ClassLoader updated) throws Exception {
        Constructor<?> before = original.loadClass(PARAMETERS + "AddedParameter").getConstructor(int.class, char.class);
        Constructor<?> after = updated.loadClass(PARAMETERS + "AddedParameter").getConstructor(int.class, char.class);
        for (boolean predicate : new boolean[]{false, true}) {
            flag(original, PARAMETERS + "OpaquePredicateBase", "predicateFlag", predicate);
            flag(updated, PARAMETERS + "OpaquePredicateBase", "predicateFlag", predicate);
            for (int left = -1; left < 3; left++) {
                for (int right = -1; right < 3; right++) {
                    for (char first : new char[]{0, 'B', 'I', 'J'}) {
                        for (char second : new char[]{0, 'B', 'I', 'J'}) {
                            Object expected = before.newInstance(left, first);
                            Object actual = after.newInstance(left, first);
                            same("AddedParameter.equals", expected.equals(before.newInstance(right, second)), actual.equals(after.newInstance(right, second)));
                            same("AddedParameter.equals null", expected.equals(null), actual.equals(null));
                            same("AddedParameter.equals other type", expected.equals("other"), actual.equals("other"));
                        }
                    }
                }
            }
        }
    }

    private static List<Object> reasonNotes(ClassLoader loader) throws Exception {
        String instructionName = "com.zelix.klassmaster.classfile.insn.Instruction";
        Class<?> instruction = loader.loadClass(instructionName);
        Method opcode = loader.loadClass("com.zelix.klassmaster.classfile.insn.SimpleInstruction").getMethod("forOpcode", int.class);
        Constructor<?> constructor = loader.loadClass("com.zelix.klassmaster.obfuscator.reflection.InstructionReasonNote").getConstructor(instruction, String.class);
        List<Object> notes = new ArrayList<Object>();
        for (int code : new int[]{-1, 0, 3, 4}) {
            Object operation = code < 0 ? null : opcode.invoke(null, code);
            for (String reason : new String[]{null, "same", "different"}) notes.add(constructor.newInstance(operation, reason));
        }
        notes.add(null);
        notes.add("other");
        return notes;
    }

    private static void checkInstructionReasonEquality(ClassLoader original, ClassLoader updated) throws Exception {
        List<Object> before = reasonNotes(original);
        List<Object> after = reasonNotes(updated);
        for (boolean strict : new boolean[]{false, true}) {
            flag(original, "com.zelix.klassmaster.classfile.insn.StackFrameState", "strictMergeEnabled", strict);
            flag(updated, "com.zelix.klassmaster.classfile.insn.StackFrameState", "strictMergeEnabled", strict);
            for (int i = 0; i < before.size() - 2; i++) {
                for (int j = 0; j < before.size(); j++) same("InstructionReasonNote.equals strict=" + strict, before.get(i).equals(before.get(j)), after.get(i).equals(after.get(j)));
            }
        }
    }

    private static Object uninitializedFixture(Class<?> type) throws Exception {
        Class<?> unsafeType = Class.forName("sun.misc.Unsafe");
        Field singleton = unsafeType.getDeclaredField("theUnsafe");
        singleton.setAccessible(true);
        return unsafeType.getMethod("allocateInstance", Class.class).invoke(singleton.get(null), type);
    }

    private static List<Object> reflectionValues(ClassLoader loader, String name) throws Exception {
        Class<?> type = loader.loadClass("com.zelix.klassmaster.obfuscator." + name);
        List<Object> values = new ArrayList<Object>();
        if (name.endsWith("LiteralStringValue")) {
            for (String value : new String[]{null, "same", "different"}) values.add(type.getConstructor(String.class).newInstance(value));
        } else if (name.endsWith("PendingReflectionValue")) {
            Class<?> methodBytecode = loader.loadClass("com.zelix.klassmaster.classfile.insn.MethodBytecode");
            for (int index : new int[]{-1, 0, 1, 100}) values.add(type.getConstructor(int.class, methodBytecode).newInstance(index, null));
        } else if (name.endsWith("NullReflectionValue")) {
            values.add(type.getConstructor().newInstance());
            values.add(type.getConstructor().newInstance());
        } else if (name.endsWith("ClassDescriptorStringValue")) {
            Class<?> pool = loader.loadClass("com.zelix.klassmaster.classfile.constpool.AbstractConstantPool");
            Class<?> utf8 = loader.loadClass("com.zelix.klassmaster.classfile.constpool.ConstantUtf8");
            Class<?> stringConstant = loader.loadClass("com.zelix.klassmaster.classfile.constpool.ResolvedStringConstant");
            values.add(uninitializedFixture(type));
            for (String text : new String[]{"Lsame;", "Ldifferent;"}) {
                Object value = utf8.getConstructor(int.class, pool, String.class).newInstance(0, null, text);
                Object constant = stringConstant.getConstructor(pool, utf8, boolean.class).newInstance(null, value, false);
                values.add(type.getConstructor(stringConstant).newInstance(constant));
            }
        } else if (name.endsWith("OpaquePredicateField")) {
            // Create isolated value-object fixtures without needing a full parsed ProgramClass.
            for (String fieldName : new String[]{null, "first", "second"}) {
                for (String ownerName : new String[]{null, "Owner", "Other"}) {
                    Object value = uninitializedFixture(type);
                    Field field = type.getField("fieldName");
                    field.setAccessible(true);
                    field.set(value, fieldName);
                    Field owner = type.getField("ownerClassName");
                    owner.setAccessible(true);
                    owner.set(value, ownerName);
                    values.add(value);
                }
            }
        }
        values.add(null);
        values.add("other");
        return values;
    }

    private static void checkReflectionValues(ClassLoader original, ClassLoader updated) throws Exception {
        for (String name : new String[]{"reflection.LiteralStringValue", "reflection.PendingReflectionValue", "reflection.NullReflectionValue", "reflection.ClassDescriptorStringValue", "flow.OpaquePredicateField"}) {
            List<Object> before = reflectionValues(original, name);
            List<Object> after = reflectionValues(updated, name);
            Method beforeEquals = before.get(0).getClass().getMethod("equals", Object.class);
            Method afterEquals = after.get(0).getClass().getMethod("equals", Object.class);
            for (boolean strict : new boolean[]{false, true}) {
                flag(original, "com.zelix.klassmaster.classfile.insn.StackFrameState", "strictMergeEnabled", strict);
                flag(updated, "com.zelix.klassmaster.classfile.insn.StackFrameState", "strictMergeEnabled", strict);
                for (int i = 0; i < before.size() - 2; i++) {
                    for (int j = 0; j < before.size(); j++) same(name + ".equals strict=" + strict, call(beforeEquals, before.get(i), before.get(j)), call(afterEquals, after.get(i), after.get(j)));
                    if (!name.endsWith("OpaquePredicateField") && (!name.endsWith("ClassDescriptorStringValue") || i == 0)) {
                        same(name + ".hashCode strict=" + strict,
                                call(before.get(i).getClass().getMethod("hashCode"), before.get(i)),
                                call(after.get(i).getClass().getMethod("hashCode"), after.get(i)));
                    }
                    if (name.endsWith("ClassDescriptorStringValue") && i > 0) {
                        Object beforeConstant = before.get(i).getClass().getField("stringConstant").get(before.get(i));
                        Object afterConstant = after.get(i).getClass().getField("stringConstant").get(after.get(i));
                        // Constant pool entries use identity hashes, which differ across class loaders.
                        same(name + ".hashCode offset strict=" + strict,
                                before.get(i).hashCode() - beforeConstant.hashCode(),
                                after.get(i).hashCode() - afterConstant.hashCode());
                    }
                }
            }
        }
    }

    private static String snapshot(Object descriptor) throws Exception {
        if (descriptor == null) return "null";
        Class<?> type = descriptor.getClass();
        StringBuilder result = new StringBuilder((String) type.getMethod("getDescriptor").invoke(descriptor));
        int[] indices = (int[]) type.getMethod("getParamIndices").invoke(descriptor);
        result.append(Arrays.toString(indices));
        for (int i = 0; i < indices.length; i++) {
            result.append('/').append(call(type.getMethod("getKeyBitCount", int.class), descriptor, i));
            result.append('/').append(call(type.getMethod("getKeyBitOffset", int.class), descriptor, i));
        }
        for (boolean isStatic : new boolean[]{false, true}) {
            Object slots = call(type.getMethod("computeLocalIndices", type, boolean.class), null, descriptor, isStatic);
            result.append('/').append(slots instanceof int[] ? Arrays.toString((int[]) slots) : slots);
        }
        return result.toString();
    }

    private static void checkParameterLayouts(ClassLoader original, ClassLoader updated) throws Exception {
        Class<?> beforeType = original.loadClass(PARAMETERS + "ExtraParamLayoutGenerator");
        Class<?> afterType = updated.loadClass(PARAMETERS + "ExtraParamLayoutGenerator");
        Class<?> beforeDescriptor = original.loadClass(PARAMETERS + "ChangedMethodDescriptor");
        Class<?> afterDescriptor = updated.loadClass(PARAMETERS + "ChangedMethodDescriptor");
        Object beforeGenerator = beforeType.getConstructor().newInstance();
        Object afterGenerator = afterType.getConstructor().newInstance();
        Method before = beforeType.getMethod("nextLayout", boolean.class, List.class, String.class, char[].class, beforeDescriptor, boolean.class);
        Method after = afterType.getMethod("nextLayout", boolean.class, List.class, String.class, char[].class, afterDescriptor, boolean.class);
        char[][] layouts = (char[][]) beforeType.getField("PARAM_TYPE_LAYOUTS").get(null);
        {
            // Lambda and non-lambda cases exercise rightward and leftward layouts.
            for (boolean lambda : new boolean[]{false, true}) {
                for (boolean varargs : new boolean[]{false, true}) {
                    for (int count = varargs ? 1 : 0; count < 5; count++) {
                        List<String> parameters = new ArrayList<String>();
                        for (int i = 0; i < count; i++) parameters.add(i == count - 1 && varargs ? "[I" : i % 2 == 0 ? "J" : "Ljava/lang/Object;");
                        for (char[] layout : layouts) {
                            Object previousBefore = null;
                            Object previousAfter = null;
                            for (int step = 0; step < 1000; step++) {
                                previousBefore = before.invoke(beforeGenerator, lambda, parameters, "V", layout, previousBefore, varargs);
                                previousAfter = after.invoke(afterGenerator, lambda, parameters, "V", layout, previousAfter, varargs);
                                same("nextLayout", snapshot(previousBefore), snapshot(previousAfter));
                                if (previousBefore == null) break;
                                if (step == 999) throw new AssertionError("layout iteration limit exceeded");
                            }
                        }
                    }
                }
            }
        }
    }

    private static void checkDescriptorConstruction(ClassLoader original, ClassLoader updated) throws Exception {
        for (String descriptor : new String[]{"()V", "(I)I", "(JLjava/lang/Object;[I)D"}) {
            for (char[] types : new char[][]{new char[0], {'J'}, {'I','I'}, {'B','I','I'}}) {
                Object[] results = new Object[2];
                ClassLoader[] loaders = {original, updated};
                for (int side = 0; side < 2; side++) {
                    Class<?> parameter = loaders[side].loadClass(PARAMETERS + "AddedParameter");
                    Object parameters = Array.newInstance(parameter, types.length);
                    for (int i = 0; i < types.length; i++) Array.set(parameters, i, parameter.getConstructor(int.class, char.class).newInstance(i, types[i]));
                    Constructor<?> constructor = loaders[side].loadClass(PARAMETERS + "ChangedMethodDescriptor").getConstructor(String.class, parameters.getClass());
                    results[side] = constructor.newInstance(descriptor, parameters);
                }
                same("ChangedMethodDescriptor constructor", snapshot(results[0]), snapshot(results[1]));
            }
        }
    }
    private static String triple(Object value) throws Exception {
        if (value == null) return "null";
        return "[" + value.getClass().getMethod("getFirst").invoke(value) + ","
                + value.getClass().getMethod("getSecond").invoke(value) + ","
                + value.getClass().getMethod("getThird").invoke(value) + "]";
    }

    private static String helperAssignment(ClassLoader loader, int mask) throws Exception {
        String util = "com.zelix.klassmaster.util.";
        Class<?> changerType = loader.loadClass(PARAMETERS + "MethodParameterChanger");
        Class<?> holderType = loader.loadClass(util + "ObservableHolder");
        Class<?> triples = loader.loadClass(util + "ObjectTriple");
        Class<?> lists = loader.loadClass(util + "ListMultimap");
        Class<?> views = loader.loadClass(util + "ReadOnlyMultiMapView");
        Class<?> sets = loader.loadClass(util + "SetMultiMap");
        Class<?> names = loader.loadClass(util + "EnumerableMap");
        Object changer = uninitializedFixture(changerType);
        Object holder = holderType.getConstructor().newInstance();
        if ((mask & 1) != 0) holderType.getMethod("setValue", Object.class).invoke(holder,
                triples.getConstructor(Object.class, Object.class, Object.class).newInstance("oldA", (mask & 2) == 0 ? "oldB" : null, "oldC"));
        Class<?> archiveType = loader.loadClass("com.zelix.klassmaster.archive.SourceArchive");
        Object archive = archiveType.getConstructor(String.class, long.class, String.class).newInstance("fixture.jar", 0L, "fixture.jar");
        archiveType.getMethod("setModuleName", String.class).invoke(archive, "fixture.module");
        Object classLists = lists.getConstructor().newInstance();
        if ((mask & 4) != 0) lists.getMethod("putValues", Object.class, Object.class).invoke(classLists, archive, new ArrayList<Object>());
        Object view = views.getConstructor(lists).newInstance(classLists);
        Field archiveField = changerType.getField("classesByArchive");
        archiveField.setAccessible(true);
        archiveField.set(changer, view);
        java.util.Map<Object, Object> archiveHelpers = new java.util.LinkedHashMap<Object, Object>();
        if ((mask & 8) != 0) archiveHelpers.put(archive, triples.getConstructor(Object.class, Object.class, Object.class)
                .newInstance("moduleA", (mask & 16) == 0 ? "moduleB" : null, "moduleC"));
        if ((mask & 32) != 0) {
            Class<?> mappingType = loader.loadClass("com.zelix.klassmaster.changelog.ChangeLogMapping");
            Object mapping = uninitializedFixture(mappingType);
            java.util.Map<Object, Object> modules = new java.util.HashMap<Object, Object>();
            if ((mask & 64) != 0) modules.put("fixture.module", triples.getConstructor(Object.class, Object.class, Object.class).newInstance("a", "b", "c"));
            mappingType.getField("moduleMethodParameterChangeClasses").set(mapping, modules);
            Field mappingField = changerType.getField("changeLogMapping");
            mappingField.setAccessible(true);
            mappingField.set(changer, mapping);
        }
        Class<?> classpathType = loader.loadClass("com.zelix.klassmaster.classfile.hierarchy.ZkmClasspath");
        Class<?> classLoaderType = loader.loadClass("com.zelix.klassmaster.classfile.hierarchy.ClasspathClassLoader");
        Object classpath = classpathType.getConstructor(String.class).newInstance("");
        Field loaderField = changerType.getField("classpathClassLoader");
        loaderField.setAccessible(true);
        loaderField.set(changer, classLoaderType.getConstructor(classpathType, boolean.class).newInstance(classpath, true));
        Field prefixField = changerType.getField("lookupClassNamePrefix");
        prefixField.setAccessible(true);
        prefixField.set(changer, "fixture");
        Field avoidField = changerType.getField("avoidSimpleNameClash");
        avoidField.setAccessible(true);
        avoidField.setBoolean(changer, (mask & 128) != 0);
        java.util.Map<Object, Object> classHelpers = new java.util.HashMap<Object, Object>();
        Object result = call(changerType.getMethod("assignHelperClasses", java.util.Set.class, views, sets, sets, holderType,
                        java.util.Map.class, java.util.Map.class, String.class, names, names, names), changer,
                new java.util.HashSet<Object>(), view, sets.getConstructor().newInstance(), sets.getConstructor().newInstance(),
                holder, archiveHelpers, classHelpers, "", names.getConstructor().newInstance(), names.getConstructor().newInstance(), names.getConstructor().newInstance());
        StringBuilder snapshot = new StringBuilder();
        if (result instanceof List) {
            snapshot.append("success:");
            for (Object helper : (List<?>) result) snapshot.append(triple(helper));
        } else snapshot.append(result);
        snapshot.append("/default=").append(triple(holderType.getMethod("getValue").invoke(holder)));
        snapshot.append("/archive=").append(triple(archiveHelpers.get(archive)));
        snapshot.append("/classes=").append(classHelpers.size());
        return snapshot.toString();
    }

    private static void checkHelperAssignments(ClassLoader before, ClassLoader after) throws Exception {
        for (int mask = 0; mask < 256; mask++) same("helper assignment mask=" + mask, helperAssignment(before, mask), helperAssignment(after, mask));
    }

}

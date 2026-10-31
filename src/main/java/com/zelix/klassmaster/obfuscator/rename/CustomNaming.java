package com.zelix.klassmaster.obfuscator.rename;

import com.zelix.klassmaster.classfile.constpool.ConstantUtf8;
import com.zelix.klassmaster.engine.ObfuscationEngine;
import com.zelix.klassmaster.exceptions.ZkmException;
import com.zelix.klassmaster.script.ScriptEnvironment;
import com.zelix.klassmaster.util.ZkmUtils;
import java.util.*;
import java.util.function.Function;

/** Naming settings scoped to one obfuscate statement, including generated helper classes. */
public final class CustomNaming implements AutoCloseable {
    public static final List<String> KINDS = List.of("Package", "Class", "Method", "Field", "LocalVariable", "Parameter");
    private static final ThreadLocal<CustomNaming> CURRENT = new ThreadLocal<>();
    private final CustomNaming previous;
    private final Map<String, Policy> policies;
    private final Map<String, IdentityHashMap<Object, DebugNames>> debugScopes = new HashMap<>();

    private static final class DebugNames {
        final SequentialNameGenerator generator;
        final Map<String, ConstantUtf8> constants = new HashMap<>();
        DebugNames(String kind, boolean randomize) {
            generator = new SequentialNameGenerator(SequentialNameGenerator.LETTER_CHARS,
                    SequentialNameGenerator.ALPHANUMERIC_CHARS, null, randomize, kind);
        }
    }
    private final boolean randomize;

    public record Policy(String kind, int length, String characters, List<String> names) {}

    public static boolean isOption(String key) {
        return KINDS.stream().anyMatch(kind -> key.equals("new" + kind + "Names")
                || key.equals("new" + kind + "NameLength")
                || key.equals("new" + kind + "NameCharacters"))
                || key.equals("newLocalVariableNameFile") || key.equals("newParameterNameFile");
    }

    public static boolean isTextOption(String key) {
        return isOption(key) && !key.endsWith("Length") && !key.endsWith("File");
    }

    public static Map<String, Policy> read(Function<String, String> value, ScriptEnvironment env) throws ZkmException {
        Map<String, Policy> policies = new HashMap<>();
        for (String kind : KINDS) {
            String prefix = "new" + kind;
            String lengthText = value.apply(prefix + "NameLength");
            String characters = value.apply(prefix + "NameCharacters");
            String inline = value.apply(prefix + "Names");
            String file = value.apply(prefix + "NameFile");
            if (lengthText == null && characters == null && inline == null
                    && !(kind.equals("LocalVariable") || kind.equals("Parameter"))) continue;
            if (lengthText == null && characters == null && inline == null && file == null) continue;
            int length = 0;
            if (lengthText != null) {
                try { length = Integer.parseInt(lengthText); }
                catch (NumberFormatException e) { throw new IllegalArgumentException(prefix + "NameLength must be an integer from 1 to 128"); }
                if (length < 1 || length > 128) throw new IllegalArgumentException(prefix + "NameLength must be from 1 to 128");
            }
            if (characters != null) {
                characters = ZkmUtils.unescapeJavaString(characters);
                if (characters.isEmpty() || characters.chars().anyMatch(c -> Character.isSurrogate((char)c)
                        || !Character.isJavaIdentifierPart(c) || Character.isIdentifierIgnorable(c))
                        || characters.chars().noneMatch(Character::isJavaIdentifierStart))
                    throw new IllegalArgumentException(prefix + "NameCharacters must contain identifier characters and at least one letter, _ or $ (BMP characters only)");
                characters = characters.chars().distinct().collect(StringBuilder::new,
                        (s, c) -> s.append((char)c), StringBuilder::append).toString();
            }
            List<String> names = null;
            if (inline != null) {
                if (file != null && !file.isBlank()) throw new IllegalArgumentException("Choose either " + prefix + "Names or " + prefix + "NameFile");
                names = parseNames(ZkmUtils.unescapeJavaString(inline), prefix + "Names");
            } else if (file != null && !file.isBlank() && (kind.equals("LocalVariable") || kind.equals("Parameter"))) {
                names = ObfuscationEngine.readNameListFile(file, prefix + "NameFile", env);
                names = parseNames(String.join("\n", names), prefix + "NameFile");
            }
            policies.put(kind, new Policy(kind, length, characters, names));
        }
        return policies;
    }

    public static List<String> parseNames(String text, String source) {
        if (text.length() > 1_048_576) throw new IllegalArgumentException(source + " exceeds 1 MiB");
        LinkedHashSet<String> names = new LinkedHashSet<>();
        String[] lines = text.replaceFirst("^\\uFEFF", "").split("\\R", -1);
        for (int i = 0; i < lines.length; i++) {
            String name = lines[i].trim();
            if (name.isEmpty() || name.startsWith("#") || name.startsWith("//")) continue;
            int[] points = name.codePoints().toArray();
            if (points.length > 128 || !Character.isJavaIdentifierStart(points[0])
                    || Arrays.stream(points).anyMatch(c -> !Character.isJavaIdentifierPart(c) || Character.isIdentifierIgnorable(c))
                    || SequentialNameGenerator.isReservedWord(name))
                throw new IllegalArgumentException(source + ": invalid identifier at line " + (i + 1) + ": " + name);
            names.add(name);
        }
        return new ArrayList<>(names);
    }

    public CustomNaming(Map<String, Policy> policies, boolean randomize) {
        this.previous = CURRENT.get();
        this.policies = policies;
        this.randomize = randomize;
        CURRENT.set(this);
    }

    public static Policy policy(String kind) {
        CustomNaming scope = CURRENT.get();
        return scope == null || kind == null ? null : scope.policies.get(kind);
    }

    /** LVT and LVTT may use unshared UTF8 entries for the same source name.
     * Match by name within a method so both tables stay consistent. */
    public static ConstantUtf8 debugName(String kind, ConstantUtf8 original, Object method) {
        CustomNaming scope = CURRENT.get();
        if (original == null || policy(kind) == null) return null;
        DebugNames names = scope.debugScopes.computeIfAbsent(kind, k -> new IdentityHashMap<>())
                .computeIfAbsent(method, key -> new DebugNames(kind, scope.randomize));
        return names.constants.computeIfAbsent(original.getValue(), key -> {
            ConstantUtf8 copy = original.copyWithIndex(0);
            copy.setValue(names.generator.nextName());
            original.constantPool.appendEntries(List.of(copy));
            return copy;
        });
    }

    @Override public void close() {
        if (previous == null) CURRENT.remove(); else CURRENT.set(previous);
    }
}

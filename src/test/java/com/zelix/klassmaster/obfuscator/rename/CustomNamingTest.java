package com.zelix.klassmaster.obfuscator.rename;

import static org.junit.jupiter.api.Assertions.*;
import com.zelix.klassmaster.script.parser.ZkmScriptParser;
import com.zelix.klassmaster.web.BuildScripts;
import com.zelix.klassmaster.web.Models.ProjectConfig;
import java.io.StringReader;
import java.util.*;
import org.junit.jupiter.api.Test;

class CustomNamingTest {
    private SequentialNameGenerator generator(String kind) {
        return new SequentialNameGenerator("ab".toCharArray(), "ab".toCharArray(), null, false, kind);
    }

    @Test void fixedLengthExhaustionAndScopeIsolation() {
        var settings = Map.of("Class", new CustomNaming.Policy("Class", 2, "xy", List.of("迷雾")));
        try (var scope = new CustomNaming(settings, false)) {
            var g = generator("Class");
            assertEquals(List.of("迷雾", "xx", "xy", "yx", "yy"),
                    List.of(g.nextName(), g.nextName(), g.nextName(), g.nextName(), g.nextName()));
            assertThrows(IllegalStateException.class, g::nextName);
            assertEquals("a", generator("Method").nextName());
            assertEquals("a", generator(null).nextName());
        }
        assertEquals("a", generator("Class").nextName());
        assertNull(CustomNaming.policy("Class"));
    }

    @Test void identifierStartAndPrefixAndKeywordSkipping() {
        try (var scope = new CustomNaming(Map.of("Method", new CustomNaming.Policy("Method", 2, "a1", null)), false)) {
            var g = generator("Method");
            assertEquals("prefixaa", g.nextPrefixedName("prefix"));
            assertEquals("prefixa1", g.nextPrefixedName("prefix"));
            assertThrows(IllegalStateException.class, g::nextName);
        }
        try (var scope = new CustomNaming(Map.of("Field", new CustomNaming.Policy("Field", 1, "_a", null)), false)) {
            var g = generator("Field");
            assertEquals("a", g.nextName());
            assertThrows(IllegalStateException.class, g::nextName);
        }
    }

    @Test void validatesTextAndSettings() throws Exception {
        assertEquals(List.of("迷雾", "Echo"), CustomNaming.parseNames("\uFEFF迷雾\r\n# comment\n\nEcho\n迷雾\n// comment", "names"));
        for (String name : List.of("class", "1bad", "a/b", "<init>", "a.b", "a b", "ba\u0000d"))
            assertThrows(IllegalArgumentException.class, () -> CustomNaming.parseNames(name, "names"));
        for (String length : List.of("0", "129", "-1", "1.5", "999999999999"))
            assertThrows(IllegalArgumentException.class, () -> CustomNaming.read(Map.of("newClassNameLength", length)::get, null));
        for (String chars : List.of("", "123", "a b", "x/", "🙂"))
            assertThrows(IllegalArgumentException.class, () -> CustomNaming.read(Map.of("newClassNameCharacters", chars)::get, null));
        assertThrows(IllegalArgumentException.class, () -> CustomNaming.read(Map.of("newClassNames", "Name", "newClassNameFile", "names.txt")::get, null));
    }

    @Test void generatedScriptSupportsAllCategoriesAndOldScripts() throws Exception {
        var p = new ProjectConfig();
        for (String kind : CustomNaming.KINDS) {
            p.build.obfuscation.put("new" + kind + "Names", "迷雾\nEcho\n# comment");
            p.build.obfuscation.put("new" + kind + "NameLength", "8");
            p.build.obfuscation.put("new" + kind + "NameCharacters", "Il");
        }
        String script = BuildScripts.generate(p, "statements");
        assertTrue(script.contains("newClassNames=\"迷雾\\nEcho\\n# comment\""));
        new ZkmScriptParser(new StringReader(script)).Input();
        new ZkmScriptParser(new StringReader("obfuscate newLocalVariableNameFile=\"locals.txt\" newParameterNameFile=\"parameters.txt\"; obfuscate;")).Input();
        assertThrows(Exception.class, () -> new ZkmScriptParser(new StringReader("obfuscate newClasNameLength=8;")).Input());
        assertThrows(Exception.class, () -> new ZkmScriptParser(new StringReader("obfuscate newClassNameLength=1.5;")).Input());
    }
}

package com.zelix.klassmaster.web;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.zelix.klassmaster.web.Models.ProjectConfig;
import com.zelix.klassmaster.obfuscator.rename.CustomNaming;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CustomNamingIntegrationTest {
    @TempDir Path temp;

    @Test void namesReachClassFilesAndProgramsStillRun() throws Exception {
        Path source = temp.resolve("Main.java"), classes = temp.resolve("input");
        Files.createDirectories(classes);
        Files.writeString(source, """
            package fixture;
            import java.util.*;
            public class Main {
                public static void main(String[] args) { System.out.println(new Payload().calculate(5)); }
            }
            class Payload {
                private int count = 7;
                public int calculate(int input) {
                    List<String> words = new ArrayList<>();
                    words.add("value");
                    int result = input + count + words.size();
                    return result;
                }
            }
            """);
        assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, null,
                "--release", "17", "-g", "-parameters", "-d", classes.toString(), source.toString()));
        assertEquals("13", command("java", "-cp", classes.toString(), "fixture.Main").trim());
        Path jar = temp.resolve("fixture.jar");
        command("jar", "--create", "--file", jar.toString(), "--main-class", "fixture.Main", "-C", classes.toString(), ".");
        String[] kinds = {"Package", "Class", "Method", "Field", "LocalVariable", "Parameter"};
        String[] names = {"包名", "类型", "计算", "字段", "变量", "参数"};
        for (String mode : List.of("inline", "file", "generated")) {
            ProjectConfig p = new ProjectConfig();
            p.directory = temp.toString();
            p.build.inputs.add(jar.toString());
            p.build.output = temp.resolve(mode + "-output").toString();
            p.build.obfuscation.clear();
            p.build.obfuscation.putAll(Map.of("obfuscateFlow", "none", "encryptStringLiterals", "none",
                    "exceptionObfuscation", "none", "localVariables", "obfuscate", "methodParameters", "obfuscate",
                    "randomize", "false", "changeLogFileOut", mode + "-changes.txt"));
            for (int i = 0; i < kinds.length; i++) {
                String prefix = "new" + kinds[i];
                p.build.obfuscation.put(prefix + "NameLength", "6");
                p.build.obfuscation.put(prefix + "NameCharacters", "xy");
                if (mode.equals("inline")) p.build.obfuscation.put(prefix + "Names", "\uFEFF" + names[i] + "\r\n# comment\r\n" + names[i]);
                if (mode.equals("file")) {
                    Path file = temp.resolve(kinds[i] + " names.txt");
                    Files.writeString(file, "\uFEFF" + names[i] + "\r\n");
                    p.build.obfuscation.put(prefix + "NameFile", file.getFileName().toString());
                }
            }
            Path run = temp.resolve(mode + "-run");
            try (WorkerClient worker = new WorkerClient(run, temp, null)) {
                JsonNode result = worker.call(Json.map("operation", "script", "script", BuildScripts.generate(p, "build"),
                        "runDirectory", run.toString(), "workingDirectory", temp.toString(), "preview", mode.equals("generated")), null, 30);
                JsonNode manifest = Json.MAPPER.readTree(run.resolve("after/classes.json").toFile());
                String mainName = null;
                StringBuilder bytecode = new StringBuilder();
                for (JsonNode entry : manifest) {
                    String name = entry.path("name").asText();
                    if (entry.path("originalName").asText().equals("fixture/Main")) mainName = name;
                    bytecode.append(command("javap", "-v", "-p", run.resolve("after").resolve(entry.path("file").asText()).toString()));
                    if (mode.equals("generated")) assertTrue(name.matches("[xy]{6}/[xy]{6}"), name);
                }
                assertNotNull(mainName, manifest.toPrettyString());
                String outputDirectory = p.build.output;
                if (mode.equals("generated")) {
                    assertFalse(Files.exists(Path.of(p.build.output)));
                    for (JsonNode output : result.path("outputs"))
                        if (Files.isDirectory(Path.of(output.asText()))) outputDirectory = output.asText();
                }
                assertEquals("13", command("java", "-cp", Path.of(outputDirectory, "fixture.jar").toString(), mainName.replace('/', '.')).trim());
                String disassembly = bytecode.toString();
                assertTrue(disassembly.contains("LocalVariableTable:"), disassembly);
                assertTrue(disassembly.contains("LocalVariableTypeTable:"), disassembly);
                assertTrue(disassembly.contains("MethodParameters:"), disassembly);
                if (!mode.equals("generated")) {
                    for (String name : names) assertTrue(disassembly.contains(name), name + " missing from " + mode + "\n" + disassembly);
                }
                assertFalse(disassembly.matches("(?s).*\\b(?:count|calculate|words|result|input)\\b.*"), disassembly);
                assertTrue(Files.isDirectory(Path.of(result.path("afterRoot").asText())));
                if (mode.equals("generated")) {
                    p.build.obfuscation.put("newClassNameLength", "1");
                    p.build.obfuscation.put("newClassNameCharacters", "x");
                    var failure = assertThrows(WorkerClient.WorkerFailure.class, () -> worker.call(
                            Json.map("operation", "script", "script", BuildScripts.generate(p, "build"),
                                    "runDirectory", temp.resolve("exhausted").toString(), "workingDirectory", temp.toString()), null, 30));
                    assertTrue(failure.getMessage().contains("names exhausted at length 1"), failure.getMessage());
                }
                // A second statement in this worker must return to default naming.
                p.build.obfuscation.keySet().removeIf(CustomNaming::isOption);
                for (String kind : kinds) p.build.obfuscation.remove("new" + kind + "NameFile");
                p.build.output = temp.resolve(mode + "-default").toString();
                worker.call(Json.map("operation", "script", "script", BuildScripts.generate(p, "build"),
                        "runDirectory", temp.resolve(mode + "-default-run").toString(), "workingDirectory", temp.toString()), null, 30);
            }
        }
    }

    private String command(String tool, String... args) throws Exception {
        String exe = tool + (System.getProperty("os.name").startsWith("Windows") ? ".exe" : "");
        List<String> command = new ArrayList<>(List.of(Path.of(System.getProperty("java.home"), "bin", exe).toString()));
        command.addAll(List.of(args));
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(process.waitFor(15, TimeUnit.SECONDS));
        assertEquals(0, process.exitValue(), output);
        return output;
    }
}

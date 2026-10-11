package com.zelix.klassmaster.web;

import static org.junit.jupiter.api.Assertions.*;

import com.zelix.klassmaster.script.*;
import com.zelix.klassmaster.script.parser.ZkmScriptParser;
import com.zelix.klassmaster.web.Models.*;
import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

class WorkbenchTest {
  @TempDir Path temp;

  @Test
  void generatedScriptParsesAndDoesNotReopenForInteractiveActions() throws Exception {
    ProjectConfig p = new ProjectConfig();
    p.directory = temp.toString();
    p.build.inputs.add("中文 input.jar");
    p.build.classpath.add("lib/a b.jar");
    p.build.trim = true;
    new ZkmScriptParser(new StringReader(BuildScripts.generate(p, "build"))).Input();
    String statements = BuildScripts.generate(p, "statements");
    assertFalse(statements.contains("open "));
    assertFalse(statements.contains("classpath "));
    assertFalse(statements.contains("saveAll"));
    new ZkmScriptParser(new StringReader(statements)).Input();
  }

  @Test
  void frontendOptionsMatchEngineGrammar() throws Exception {
    var catalog = Json.MAPPER.readTree(getClass().getResourceAsStream("/web/options.json"));
    List<org.junit.jupiter.api.function.Executable> checks = new ArrayList<>();
    for (String group : List.of("obfuscationOptions", "trimOptions", "saveOptions")) {
      for (var option : catalog.path(group)) {
        String key = option.path("key").asText();
        List<String> values = new ArrayList<>();
        if (option.has("values")) option.path("values").forEach(v -> values.add(v.asText()));
        else values.add("\"\"");
        for (String value : values) {
          String statement =
              group.equals("saveOptions")
                  ? "saveAll " + key + "=" + value + " \"output\";"
                  : (group.equals("trimOptions") ? "trim " : "obfuscate ")
                      + key
                      + "="
                      + value
                      + ";";
          checks.add(
              () ->
                  assertDoesNotThrow(
                      () -> new ZkmScriptParser(new StringReader(statement)).Input(), statement));
        }
      }
    }
    assertTrue(checks.size() > 100);
    assertAll(checks);
  }

  @Test
  void projectRevisionAndRelocation() throws Exception {
    ProjectStore store = new ProjectStore(temp.resolve("home"));
    ProjectConfig p = store.create(temp.resolve("project").toString(), "test");
    ProjectConfig old = Json.MAPPER.convertValue(p, ProjectConfig.class);
    p.name = "updated";
    store.save(p);
    assertThrows(ProjectStore.Conflict.class, () -> store.save(old));
    assertEquals("updated", store.open(p.directory).name);
    assertEquals("scripts/a.zkm", store.portable(Path.of(p.directory, "scripts/a.zkm")));
  }

  @Test
  void previewRedirectsOutputsAndSubsequentReadsOnly() throws Exception {
    RunObserver o =
        new RunObserver(temp.resolve("run"), temp.resolve("main.zkm"), "print \"hello\";", true);
    Path output = temp.resolve("original/output");
    Path redirected = o.output(output.toFile()).toPath();
    assertTrue(redirected.startsWith(temp.resolve("run")));
    assertEquals(redirected.resolve("a.jar"), o.input(output.resolve("a.jar").toFile()).toPath());
    assertEquals(temp.resolve("input.jar").toFile(), o.input(temp.resolve("input.jar").toFile()));
    assertFalse(Files.exists(output));
  }

  @Test
  void unsavedSourceRetainsIncludeLocationAndDoesNotExecute() throws Exception {
    Files.writeString(temp.resolve("included.zkm"), "print \"included\";");
    Path source = temp.resolve("missing.zkm");
    ExecutionObserver.CURRENT.set(
        new RunObserver(
            temp.resolve("run"),
            source,
            "<%include \"included.zkm\"%>\nexecute \"do-not-execute\";",
            false));
    try {
      String text = new ScriptPreprocessor(source.toString(), new Properties()).getProcessedText();
      assertTrue(text.contains("included"));
      new ZkmScriptParser(new StringReader(text)).Input();
      assertFalse(Files.exists(source));
    } finally {
      ExecutionObserver.CURRENT.remove();
    }
  }

  @Test
  void localApiRequiresTokenAndRejectsOtherOrigins() throws Exception {
    try (WebServer server = new WebServer(0, temp.resolve("home"))) {
      server.start();
      HttpClient client = HttpClient.newHttpClient();
      URI uri = URI.create(server.base() + "/api/v1/state");
      assertEquals(
          401,
          client
              .send(HttpRequest.newBuilder(uri).build(), HttpResponse.BodyHandlers.ofString())
              .statusCode());
      String token = server.url().split("token=")[1];
      assertEquals(
          200,
          client
              .send(
                  HttpRequest.newBuilder(uri).header("X-ZKM-Token", token).build(),
                  HttpResponse.BodyHandlers.ofString())
              .statusCode());
      assertEquals(
          403,
          client
              .send(
                  HttpRequest.newBuilder(uri)
                      .header("X-ZKM-Token", token)
                      .header("Origin", "https://example.com")
                      .build(),
                  HttpResponse.BodyHandlers.ofString())
              .statusCode());
    }
  }

  @Test
  void classPairingUsesIdentityAndDoesNotGuessAmbiguousNames() throws Exception {
    Path before = temp.resolve("before"), after = temp.resolve("after");
    ClassIdentity b = identity("one", "a/A", "a/A"), a = identity("one", "b/B", "a/A");
    Json.write(before.resolve("classes.json"), List.of(b));
    Json.write(after.resolve("classes.json"), List.of(a));
    assertEquals("renamed", Comparisons.pair(before, after).get(0).get("status"));
    a.id = "two";
    ClassIdentity a2 = identity("three", "b/C", "a/A");
    Json.write(after.resolve("classes.json"), List.of(a, a2));
    assertNull(Comparisons.pair(before, after).get(0).get("after"));
  }

  @Test
  void dependencyDirectoryChangesInvalidateCache() throws Exception {
    Path dependency = temp.resolve("dependency");
    Files.createDirectories(dependency);
    Files.writeString(dependency.resolve("a.class"), "before");
    String before = FileFingerprint.of("vf", List.of(dependency));
    Files.writeString(dependency.resolve("a.class"), "after");
    assertNotEquals(before, FileFingerprint.of("vf", List.of(dependency)));
  }

  @Test
  void workerExitDoesNotAffectNewWorkers() throws Exception {
    try (WorkerClient worker = new WorkerClient(temp.resolve("worker"), temp, null)) {
      var processField = WorkerClient.class.getDeclaredField("process");
      processField.setAccessible(true);
      Process process = (Process) processField.get(worker);
      process.destroyForcibly();
      assertTrue(process.waitFor(10, java.util.concurrent.TimeUnit.SECONDS));
      assertThrows(
          IOException.class,
          () -> worker.call(Json.map("operation", "validate", "script", "print \"ok\";"), null, 2));
    }
    try (WorkerClient replacement = new WorkerClient(temp.resolve("replacement"), temp, null)) {
      assertTrue(
          replacement
              .call(Json.map("operation", "validate", "script", "print \"ok\";"), null, 10)
              .path("valid")
              .asBoolean());
    }
  }

  @Test
  void workerTimeoutKillsBlockingTask() throws Exception {
    Path marker =
        Path.of("src/test/java/com/zelix/klassmaster/web/WorkerSleeper.java").toAbsolutePath();
    javax.tools.ToolProvider.getSystemJavaCompiler()
        .run(null, null, null, "-d", temp.toString(), marker.toString());
    String command =
        Path.of(
                    System.getProperty("java.home"),
                    "bin",
                    System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java")
                .toString()
                .replace('\\', '/')
            + " -cp "
            + temp.toString().replace('\\', '/')
            + " WorkerSleeper";
    try (WorkerClient worker = new WorkerClient(temp.resolve("blocked"), temp, null)) {
      IOException error =
          assertThrows(
              IOException.class,
              () ->
                  worker.call(
                      Json.map(
                          "operation",
                          "script",
                          "script",
                          "execute " + BuildScripts.quote(command) + ";",
                          "workingDirectory",
                          temp.toString()),
                      null,
                      1));
      assertTrue(error.getMessage().contains("exceeded"));
      for (int i = 0; i < 50 && worker.alive(); i++) Thread.sleep(20);
      assertFalse(worker.alive());
    }
  }

  private ClassIdentity identity(String id, String name, String original) {
    ClassIdentity c = new ClassIdentity();
    c.id = id;
    c.name = name;
    c.originalName = original;
    c.version = "null";
    return c;
  }
}

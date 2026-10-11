package com.zelix.klassmaster.web;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.util.*;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

class ProgramRunnerTest {
  @TempDir Path temp;
  @BeforeEach void fixture() throws Exception {
    Path source = temp.resolve("OutputFixture.java");
    Files.writeString(source, """
        public class OutputFixture {
          public static void main(String[] args) throws Exception {
            if (args.length > 0 && args[0].equals("sleep")) { System.out.print("before sleep"); System.out.flush(); Thread.sleep(10000); }
            if (args.length > 0 && args[0].equals("flood")) { for (int i=0; i<2200; i++) System.out.print("x".repeat(1024)); return; }
            System.out.println("argument=" + args[0]);
            System.out.println("input=" + new String(System.in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
            System.out.println("resource=" + java.nio.file.Files.readString(java.nio.file.Path.of("data.txt")));
            System.err.println("error stream");
            System.exit(7);
          }
        }
        """);
    assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, null, "--release", "17", "-d", temp.toString(), source.toString()));
    Files.writeString(temp.resolve("data.txt"), "resource text");
  }
  Map<String, Object> request(String argument) {
    return Json.map("operation", "programRun", "runDirectory", temp.resolve("run").toString(),
        "workingDirectory", temp.toString(), "classpath", List.of(temp.toString()), "mainClass", "OutputFixture",
        "args", List.of(argument), "stdin", "中文 input\n", "timeout", 1);
  }
  @Test void capturesStreamsExitCodeArgumentsInputAndWorkingDirectory() throws Exception {
    try (WorkerClient worker = new WorkerClient(temp, temp, ignored -> {})) {
      var result = worker.call(request("a b;$(untouched)"), ignored -> {}, 10);
      assertEquals(7, result.path("exitCode").asInt());
      assertFalse(result.path("timedOut").asBoolean());
      assertTrue(result.path("stdout").asText().contains("argument=a b;$(untouched)"));
      assertTrue(result.path("stdout").asText().contains("input=中文 input\n"));
      assertTrue(result.path("stdout").asText().contains("resource=resource text"));
      assertEquals("error stream" + System.lineSeparator(), result.path("stderr").asText());
      assertTrue(Files.isRegularFile(temp.resolve("run/stdout.txt")));
    }
  }
  @Test void timeoutKeepsPartialOutputAndTerminatesTheChild() throws Exception {
    try (WorkerClient worker = new WorkerClient(temp, temp, ignored -> {})) {
      var result = worker.call(request("sleep"), ignored -> {}, 10);
      assertTrue(result.path("timedOut").asBoolean());
      assertTrue(result.path("exitCode").isNull());
      assertEquals("before sleep", result.path("stdout").asText());
      assertTrue(result.path("elapsedMs").asLong() < 8000);
    }
  }
  @Test void outputLimitStillDrainsTheProcess() throws Exception {
    try (WorkerClient worker = new WorkerClient(temp, temp, ignored -> {})) {
      var req = request("flood"); req.put("timeout", 20);
      var result = worker.call(req, ignored -> {}, 30);
      assertTrue(result.path("stdoutTruncated").asBoolean());
      assertFalse(result.path("timedOut").asBoolean());
      assertEquals(2 * 1024 * 1024, result.path("stdout").asText().length());
    }
  }
}

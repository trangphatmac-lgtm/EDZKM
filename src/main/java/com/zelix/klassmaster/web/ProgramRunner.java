package com.zelix.klassmaster.web;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.jar.JarFile;

/** Executes application code in a disposable child JVM, never inside the engine worker. */
public final class ProgramRunner {
  private static final int OUTPUT_LIMIT = 2 * 1024 * 1024;

  public static Object run(JsonNode request) throws Exception {
    List<String> classpath = strings(request.path("classpath"));
    if (classpath.isEmpty()) throw new IllegalArgumentException("Select a JAR or class directory");
    for (String value : classpath)
      if (!Files.exists(Path.of(value))) throw new FileNotFoundException(value);
    String main = request.path("mainClass").asText("").trim();
    if (main.isEmpty()) {
      for (String value : classpath) {
        if (!value.toLowerCase(Locale.ROOT).endsWith(".jar")) continue;
        try (JarFile jar = new JarFile(value)) {
          if (jar.getManifest() != null)
            main = Objects.toString(jar.getManifest().getMainAttributes().getValue("Main-Class"), "").trim();
        }
        if (!main.isEmpty()) break;
      }
    }
    if (main.isEmpty()) throw new IllegalArgumentException("Enter a main class (no JAR Main-Class found)");
    String executable = Path.of(System.getProperty("java.home"), "bin",
        System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java").toString();
    List<String> command = new ArrayList<>(List.of(executable, "-Dfile.encoding=UTF-8"));
    command.addAll(strings(request.path("jvmArgs")));
    command.addAll(List.of("-cp", String.join(File.pathSeparator, classpath), main));
    command.addAll(strings(request.path("args")));
    int timeout = Math.max(1, Math.min(3600, request.path("timeout").asInt(60)));
    Path working = Path.of(request.path("workingDirectory").asText());
    Path root = Path.of(request.path("runDirectory").asText());
    Files.createDirectories(root);
    long started = System.nanoTime();
    Process process = new ProcessBuilder(command).directory(working.toFile()).start();
    ExecutorService io = Executors.newFixedThreadPool(3);
    Thread cleanup = new Thread(() -> stop(process), "zkm-program-cleanup");
    Runtime.getRuntime().addShutdownHook(cleanup);
    try {
      Future<Capture> stdout = io.submit(() -> capture(process.getInputStream(), root.resolve("stdout.txt"), "stdout"));
      Future<Capture> stderr = io.submit(() -> capture(process.getErrorStream(), root.resolve("stderr.txt"), "stderr"));
      io.submit(() -> {
        try (OutputStream input = process.getOutputStream()) {
          input.write(request.path("stdin").asText("").getBytes(StandardCharsets.UTF_8));
        } catch (IOException ignored) { }
      });
      boolean timedOut = !process.waitFor(timeout, TimeUnit.SECONDS);
      // Also close descendants holding inherited pipes after the main program exits.
      stop(process);
      process.waitFor(5, TimeUnit.SECONDS);
      Capture out = read(stdout, process.getInputStream());
      Capture err = read(stderr, process.getErrorStream());
      return Json.map("stdout", out.text, "stderr", err.text, "stdoutTruncated", out.truncated,
          "stderrTruncated", err.truncated, "exitCode", timedOut ? null : process.exitValue(),
          "timedOut", timedOut, "elapsedMs", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started),
          "command", command, "mainClass", main, "workingDirectory", working.toString(),
          "side", request.path("side").asText(), "sourceJob", request.path("sourceJob").asText());
    } finally {
      stop(process);
      io.shutdownNow();
      Runtime.getRuntime().removeShutdownHook(cleanup);
    }
  }

  private record Capture(String text, boolean truncated) { }
  private static Capture read(Future<Capture> future, InputStream stream) throws Exception {
    try { return future.get(5, TimeUnit.SECONDS); }
    catch (TimeoutException e) { stream.close(); return future.get(5, TimeUnit.SECONDS); }
  }
  private static Capture capture(InputStream stream, Path file, String channel) throws IOException {
    StringBuilder content = new StringBuilder();
    boolean truncated = false;
    try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8);
         Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
      char[] buffer = new char[4096];
      int count;
      while ((count = reader.read(buffer)) >= 0) {
        int take = Math.min(count, OUTPUT_LIMIT - content.length());
        if (take > 0) {
          content.append(buffer, 0, take);
          writer.write(buffer, 0, take);
          System.out.println("[" + channel + "] " + new String(buffer, 0, take));
        }
        if (take < count) truncated = true;
      }
    }
    return new Capture(content.toString(), truncated);
  }
  private static void stop(Process process) {
    process.descendants().forEach(ProcessHandle::destroyForcibly);
    if (process.isAlive()) process.destroyForcibly();
  }
  private static List<String> strings(JsonNode values) {
    if (!values.isMissingNode() && !values.isArray()) throw new IllegalArgumentException("Arguments must be arrays");
    List<String> result = new ArrayList<>();
    for (JsonNode value : values) {
      if (!value.isTextual() || value.asText().indexOf('\0') >= 0) throw new IllegalArgumentException("Invalid argument");
      result.add(value.asText());
    }
    return result;
  }
}

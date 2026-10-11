package com.zelix.klassmaster.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

public final class WorkerClient implements AutoCloseable {
  private final Process process;
  private final BufferedWriter input;
  private final ConcurrentMap<String, CompletableFuture<JsonNode>> pending =
      new ConcurrentHashMap<>();
  private volatile Consumer<String> logger;
  private volatile Consumer<JsonNode> progress;

  public void onProgress(Consumer<JsonNode> listener) {
    progress = listener;
  }

  public WorkerClient(Path root, Path workingDirectory, Consumer<String> log) throws IOException {
    Files.createDirectories(root);
    logger = log;
    ProcessBuilder builder =
        new ProcessBuilder(
            javaCommand("com.zelix.klassmaster.web.WorkerMain", root.toAbsolutePath().toString()));
    builder.directory(workingDirectory.toFile());
    process = builder.start();
    input = process.outputWriter(StandardCharsets.UTF_8);
    Thread reader =
        new Thread(
            () -> {
              try (BufferedReader output = process.inputReader(StandardCharsets.UTF_8)) {
                String line;
                while ((line = output.readLine()) != null) {
                  JsonNode value = Json.MAPPER.readTree(line);
                  if (value.path("type").asText().equals("progress")) {
                    if (progress != null) progress.accept(value.path("data"));
                  } else if (value.path("type").asText().equals("log")) {
                    if (logger != null) logger.accept(value.path("text").asText());
                  } else {
                    CompletableFuture<JsonNode> future =
                        pending.remove(value.path("requestId").asText());
                    if (future != null) {
                      if (value.path("type").asText().equals("error"))
                        future.completeExceptionally(new WorkerFailure(value.path("error")));
                      else future.complete(value.path("result"));
                    }
                  }
                }
              } catch (Exception e) {
                fail(e);
              } finally {
                fail(new IOException("Engine process stopped"));
              }
            },
            "zkm-worker-output");
    reader.setDaemon(true);
    reader.start();
    Thread errors =
        new Thread(
            () -> {
              try (BufferedReader err = process.errorReader()) {
                String line;
                while ((line = err.readLine()) != null) if (logger != null) logger.accept(line);
              } catch (IOException ignored) {
              }
            },
            "zkm-worker-errors");
    errors.setDaemon(true);
    errors.start();
  }

  public static List<String> javaCommand(String main, String... args) {
    String exe =
        Path.of(
                System.getProperty("java.home"),
                "bin",
                System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java")
            .toString();
    String cp =
        String.join(
            File.pathSeparator,
            Arrays.stream(
                    System.getProperty("java.class.path")
                        .split(java.util.regex.Pattern.quote(File.pathSeparator)))
                .map(p -> Path.of(p).toAbsolutePath().toString())
                .toList());
    List<String> command =
        new ArrayList<>(List.of(exe, "-Xmx1024m", "-Dfile.encoding=UTF-8", "-cp", cp, main));
    command.addAll(List.of(args));
    return command;
  }

  public synchronized JsonNode call(Object value, Consumer<String> log, long timeoutSeconds)
      throws Exception {
    logger = log;
    ObjectNode request = (ObjectNode) Json.tree(value);
    String id = UUID.randomUUID().toString();
    request.put("requestId", id);
    CompletableFuture<JsonNode> future = new CompletableFuture<>();
    pending.put(id, future);
    try {
      input.write(Json.line(request));
      input.newLine();
      input.flush();
      return timeoutSeconds > 0 ? future.get(timeoutSeconds, TimeUnit.SECONDS) : future.get();
    } catch (TimeoutException e) {
      close();
      throw new IOException("Task exceeded " + timeoutSeconds + " seconds", e);
    } catch (ExecutionException e) {
      if (e.getCause() instanceof Exception cause) throw cause;
      throw e;
    } finally {
      pending.remove(id);
    }
  }

  public boolean alive() {
    return process.isAlive();
  }

  private void fail(Exception e) {
    pending.values().forEach(f -> f.completeExceptionally(e));
    pending.clear();
  }

  @Override
  public void close() {
    process.descendants().forEach(ProcessHandle::destroyForcibly);
    process.destroyForcibly();
    fail(new IOException("Task cancelled"));
  }

  public static class WorkerFailure extends IOException {
    public final JsonNode details;

    public WorkerFailure(JsonNode details) {
      super(details.path("message").asText());
      this.details = details;
    }
  }
}

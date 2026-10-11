package com.zelix.klassmaster.web;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Private JSON-lines protocol. Engine stdout/stderr are captured and never corrupt responses. */
public final class WorkerMain {
  public static void main(String[] args) throws Exception {
    PrintStream protocol = System.out;
    Progress.CURRENT.set(
        value -> {
          try {
            send(protocol, Json.map("type", "progress", "data", value));
          } catch (IOException ignored) {
          }
        });
    PrintStream log =
        new PrintStream(
            new OutputStream() {
              private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

              @Override
              public synchronized void write(int b) throws IOException {
                if (b == '\n' || buffer.size() >= 8192) {
                  if (buffer.size() > 0)
                    send(
                        protocol,
                        Json.map("type", "log", "text", buffer.toString(StandardCharsets.UTF_8)));
                  buffer.reset();
                }
                if (b != '\n') buffer.write(b);
              }

              @Override
              public synchronized void flush() throws IOException {
                if (buffer.size() > 0) {
                  send(
                      protocol,
                      Json.map("type", "log", "text", buffer.toString(StandardCharsets.UTF_8)));
                  buffer.reset();
                }
              }
            },
            true,
            StandardCharsets.UTF_8);
    System.setOut(log);
    System.setErr(log);
    try (EngineSession engine = new EngineSession(Path.of(args[0]));
        BufferedReader input =
            new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
      String line;
      while ((line = input.readLine()) != null) {
        JsonNode request = Json.MAPPER.readTree(line);
        String id = request.path("requestId").asText();
        try {
          Object result =
              request.path("operation").asText().equals("decompile")
                  ? Decompilers.decompile(request)
                  : request.path("operation").asText().equals("compareImport")
                      ? Comparisons.importPair(request)
                      : request.path("operation").asText().equals("programRun")
                          ? ProgramRunner.run(request)
                          : engine.execute(request);
          log.flush();
          send(protocol, Json.map("type", "result", "requestId", id, "result", result));
        } catch (Throwable error) {
          error.printStackTrace(log);
          log.flush();
          send(
              protocol,
              Json.map("type", "error", "requestId", id, "error", describe(error, request)));
        }
      }
    }
  }

  private static Map<String, Object> describe(Throwable error, JsonNode request) {
    Map<String, Object> details = Errors.describe(error);
    if (details.containsKey("line") && request.hasNonNull("sourcePath"))
      details.put("file", request.path("sourcePath").asText());
    return details;
  }

  private static synchronized void send(PrintStream out, Object message) throws IOException {
    out.println(Json.line(message));
    out.flush();
  }
}

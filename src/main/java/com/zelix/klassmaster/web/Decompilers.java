package com.zelix.klassmaster.web;

import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.*;
import java.util.*;

public final class Decompilers {
  public static Object decompile(JsonNode request) throws Exception {
    Path file = Path.of(request.path("file").asText()),
        out =
            Files.createTempDirectory(Path.of(request.path("runDirectory").asText()), "decompile-");
    String engine = request.path("engine").asText("vf");
    List<String> args = new ArrayList<>();
    if (engine.equals("cfr")) {
      args.addAll(
          List.of(
              file.toString(),
              "--outputdir",
              out.toString(),
              "--silent",
              "true",
              "--caseinsensitivefs",
              "true"));
      if (request.path("libraries").size() > 0) {
        List<String> libs = new ArrayList<>();
        request.path("libraries").forEach(v -> libs.add(v.asText()));
        args.addAll(List.of("--extraclasspath", String.join(java.io.File.pathSeparator, libs)));
      }
      org.benf.cfr.reader.Main.main(args.toArray(String[]::new));
    } else if (engine.equals("vf")) {
      args.addAll(List.of("--folder", "--log-level=ERROR", "--thread-count=1"));
      request.path("libraries").forEach(v -> args.add("--add-external=" + v.asText()));
      args.add(file.toString());
      args.add(out.toString());
      org.jetbrains.java.decompiler.main.decompiler.ConsoleDecompiler.main(
          args.toArray(String[]::new));
    } else throw new IllegalArgumentException("Unknown decompiler");
    try (var paths = Files.walk(out)) {
      List<Path> sources = paths.filter(p -> p.toString().endsWith(".java")).toList();
      if (sources.isEmpty())
        throw new IllegalStateException("Decompiler produced no Java source; see task log");
      Path primary =
          sources.stream()
              .filter(
                  p ->
                      p.getFileName()
                          .toString()
                          .equals(file.getFileName().toString().replaceAll("\\.class$", ".java")))
              .findFirst()
              .orElse(sources.get(0));
      return Json.map(
          "source",
          Files.readString(primary),
          "engine",
          engine,
          "version",
          engine.equals("vf") ? "1.12.0" : "0.152");
    } finally {
      try (var paths = Files.walk(out)) {
        for (Path p : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(p);
      }
    }
  }
}

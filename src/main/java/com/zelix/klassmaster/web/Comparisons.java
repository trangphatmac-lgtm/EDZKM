package com.zelix.klassmaster.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.zelix.klassmaster.changelog.*;
import com.zelix.klassmaster.classfile.hierarchy.ZkmClasspath;
import com.zelix.klassmaster.web.Models.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

public final class Comparisons {
  public static Object importPair(JsonNode request) throws Exception {
    Path root = Path.of(request.path("runDirectory").asText());
    Files.createDirectories(root);
    List<ClassIdentity> before =
        scan(Path.of(request.path("before").asText()), root.resolve("before"));
    List<ClassIdentity> after =
        scan(Path.of(request.path("after").asText()), root.resolve("after"));
    List<LoadedChangeLog> mappings = new ArrayList<>();
    List<String> logs = new ArrayList<>();
    request.path("changeLogs").forEach(p -> logs.add(p.asText()));
    if (!logs.isEmpty()) {
      StackTraceTranslator translator = new StackTraceTranslator(logs, new ZkmClasspath(""));
      try {
        for (String path : logs) mappings.add(translator.loadChangeLog(path));
      } finally {
        translator.dispose();
      }
    }
    for (ClassIdentity c : before) {
      String name = c.name;
      for (LoadedChangeLog mapping : mappings)
        if (mapping != null) {
          String mapped = mapping.getNewClassName(name);
          if (mapped != null) name = mapped;
        }
      for (ClassIdentity a : after) if (a.name.equals(name)) a.originalName = c.name;
    }
    Json.write(root.resolve("after/classes.json"), after);
    return Json.map(
        "beforeRoot",
        root.resolve("before").toString(),
        "afterRoot",
        root.resolve("after").toString(),
        "outputs",
        List.of(),
        "changeLogs",
        request.path("changeLogs"));
  }

  public static List<ClassIdentity> read(Path root) throws IOException {
    Path manifest = root.resolve("classes.json");
    if (!Files.exists(manifest)) return new ArrayList<>();
    List<ClassIdentity> classes =
        new ArrayList<>(Arrays.asList(Json.read(manifest, ClassIdentity[].class)));
    for (ClassIdentity c : classes)
      if (c.file != null && !Path.of(c.file).isAbsolute())
        c.file = root.resolve(c.file).toAbsolutePath().normalize().toString();
    return classes;
  }

  public static List<Map<String, Object>> pair(Path beforeRoot, Path afterRoot) throws IOException {
    List<ClassIdentity> before = read(beforeRoot), after = read(afterRoot);
    boolean nativeRun =
        Files.exists(beforeRoot.resolve("snapshot.json"))
            && Files.exists(afterRoot.resolve("snapshot.json"));
    Set<String> used = new HashSet<>();
    List<Map<String, Object>> pairs = new ArrayList<>();
    for (ClassIdentity b : before) {
      List<ClassIdentity> candidates =
          after.stream().filter(a -> !used.contains(a.id) && a.id.equals(b.id)).toList();
      if (candidates.isEmpty())
        candidates =
            after.stream()
                .filter(
                    a ->
                        !used.contains(a.id)
                            && Objects.equals(a.version, b.version)
                            && (Objects.equals(a.originalName, b.name) || a.name.equals(b.name)))
                .toList();
      ClassIdentity a = candidates.size() == 1 ? candidates.get(0) : null;
      if (a != null) used.add(a.id);
      pairs.add(
          Json.map(
              "id",
              b.id,
              "before",
              b,
              "after",
              a,
              "status",
              a == null
                  ? (candidates.isEmpty()
                      ? (nativeRun ? "removed" : "removed-or-unmapped")
                      : "ambiguous")
                  : (a.name.equals(b.name) ? "unchanged-name" : "renamed")));
    }
    for (ClassIdentity a : after)
      if (!used.contains(a.id))
        pairs.add(
            Json.map(
                "id",
                "after-" + a.id,
                "before",
                null,
                "after",
                a,
                "status",
                nativeRun ? "added" : "added-or-unmapped"));
    return pairs;
  }

  public static List<ClassIdentity> scan(Path source, Path dest) throws Exception {
    if (!Files.exists(source)) throw new FileNotFoundException(source.toString());
    Files.createDirectories(dest);
    List<ClassIdentity> list = new ArrayList<>();
    long[] budget = {512L * 1024 * 1024};
    if (Files.isDirectory(source)) {
      try (var paths = Files.walk(source)) {
        for (Path p : paths.filter(Files::isRegularFile).toList()) {
          String name = source.relativize(p).toString().replace('\\', '/');
          if (name.endsWith(".class"))
            add(Files.readAllBytes(p), source.toString(), name, dest, list, budget);
          else if (archive(name))
            try (InputStream in = Files.newInputStream(p)) {
              zip(in, p.toString(), dest, list, 0, budget);
            }
        }
      }
    } else if (source.toString().endsWith(".class"))
      add(
          Files.readAllBytes(source),
          source.getParent().toString(),
          source.getFileName().toString(),
          dest,
          list,
          budget);
    else
      try (InputStream in = Files.newInputStream(source)) {
        zip(in, source.toString(), dest, list, 0, budget);
      }
    Json.write(dest.resolve("classes.json"), list);
    return list;
  }

  private static boolean archive(String name) {
    String n = name.toLowerCase(Locale.ROOT);
    return n.endsWith(".jar") || n.endsWith(".zip") || n.endsWith(".war") || n.endsWith(".ear");
  }

  private static void zip(
      InputStream stream,
      String origin,
      Path dest,
      List<ClassIdentity> list,
      int depth,
      long[] budget)
      throws Exception {
    if (depth > 8) throw new IOException("Nested archive depth exceeds 8");
    try (ZipInputStream zip = new ZipInputStream(stream)) {
      ZipEntry entry;
      while ((entry = zip.getNextEntry()) != null) {
        if (entry.isDirectory()) continue;
        String name = entry.getName();
        if (name.endsWith(".class") || archive(name)) {
          byte[] bytes = zip.readNBytes((int) Math.min(budget[0] + 1, 64 * 1024 * 1024));
          if (bytes.length > budget[0] || bytes.length >= 64 * 1024 * 1024)
            throw new IOException("Archive expansion limit exceeded");
          if (name.endsWith(".class")) add(bytes, origin, name, dest, list, budget);
          else {
            budget[0] -= bytes.length;
            zip(
                new ByteArrayInputStream(bytes),
                origin + "!" + name,
                dest,
                list,
                depth + 1,
                budget);
          }
        }
      }
    }
  }

  private static void add(
      byte[] bytes, String origin, String entry, Path dest, List<ClassIdentity> list, long[] budget)
      throws Exception {
    budget[0] -= bytes.length;
    if (budget[0] < 0) throw new IOException("Archive expansion limit exceeded");
    ClassIdentity c = new ClassIdentity();
    c.name = className(bytes);
    c.originalName = c.name;
    c.origin = origin;
    c.entry = entry;
    c.version = entry.startsWith("META-INF/versions/") ? entry.split("/")[2] : "null";
    c.id = EngineSession.hash(origin + "!" + entry);
    Path target =
        dest.resolve("classes")
            .resolve(EngineSession.hash(origin + "@" + c.version).substring(0, 16))
            .resolve(c.name + ".class")
            .normalize();
    if (!target.startsWith(dest.normalize())) throw new IOException("Invalid class entry");
    Files.createDirectories(target.getParent());
    Files.write(target, bytes);
    c.file =
        dest.toAbsolutePath().relativize(target.toAbsolutePath()).toString().replace('\\', '/');
    list.add(c);
  }

  static String className(byte[] bytes) throws IOException {
    try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
      if (in.readInt() != 0xCAFEBABE) throw new IOException("Invalid class file");
      in.readUnsignedShort();
      in.readUnsignedShort();
      Object[] pool = new Object[in.readUnsignedShort()];
      for (int i = 1; i < pool.length; i++) {
        int tag = in.readUnsignedByte();
        switch (tag) {
          case 1 -> pool[i] = in.readUTF();
          case 7 -> pool[i] = in.readUnsignedShort();
          case 8, 16, 19, 20 -> in.skipNBytes(2);
          case 3, 4, 9, 10, 11, 12, 17, 18 -> in.skipNBytes(4);
          case 5, 6 -> {
            in.skipNBytes(8);
            i++;
          }
          case 15 -> in.skipNBytes(3);
          default -> throw new IOException("Unknown constant tag " + tag);
        }
      }
      in.readUnsignedShort();
      return (String) pool[(Integer) pool[in.readUnsignedShort()]];
    }
  }
}

package com.zelix.klassmaster.web;

import com.zelix.klassmaster.web.Models.Job;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;

/** Export only completed engine outputs, keeping existing JAR bytes intact. */
public final class OutputFiles {
  public record Candidate(String path, String name, String kind) { }
  public static List<Path> roots(Job job) {
    if (!job.status.equals("succeeded")) throw new IllegalArgumentException("Select a successful build");
    List<Path> roots = new ArrayList<>();
    for (var output : Json.tree(job.result).path("outputs")) {
      Path path = Path.of(output.asText()).toAbsolutePath().normalize();
      if (Files.isDirectory(path) || (Files.isRegularFile(path) && jar(path))) roots.add(path);
    }
    return roots.stream().distinct().toList();
  }
  public static List<Candidate> candidates(Job job) throws IOException {
    Map<String, Candidate> result = new LinkedHashMap<>();
    for (Path root : roots(job)) {
      if (Files.isRegularFile(root)) add(result, root, "jar");
      else try (var files = Files.walk(root)) {
        List<Path> paths = files.filter(Files::isRegularFile).sorted().toList();
        List<Path> jars = paths.stream().filter(OutputFiles::jar).toList();
        for (Path path : jars) add(result, path, "jar");
        if (jars.isEmpty() && paths.stream().anyMatch(p -> p.toString().endsWith(".class"))) add(result, root, "directory");
      }
    }
    return new ArrayList<>(result.values());
  }
  private static void add(Map<String, Candidate> into, Path path, String kind) {
    into.putIfAbsent(path.toString(), new Candidate(path.toString(), path.getFileName().toString(), kind));
  }
  private static boolean jar(Path path) { return path.toString().toLowerCase(Locale.ROOT).endsWith(".jar"); }

  public static void export(Candidate source, Path destination) throws IOException {
    Path from = Path.of(source.path()).toAbsolutePath().normalize();
    Path target = destination.toAbsolutePath().normalize();
    if (!jar(target)) throw new IllegalArgumentException("Export filename must end in .jar");
    if (Files.exists(target) && Files.isSameFile(from, target)) return;
    if (!Files.isDirectory(target.getParent())) throw new FileNotFoundException(target.getParent().toString());
    Path temporary = Files.createTempFile(target.getParent(), ".zkm-export-", ".jar");
    try {
      if (source.kind().equals("jar")) {
        Files.copy(from, temporary, StandardCopyOption.REPLACE_EXISTING);
      } else {
        List<Path> files;
        try (var stream = Files.walk(from)) {
          // JarInputStream recognizes the manifest only at the beginning of the archive.
          files = stream.filter(Files::isRegularFile).filter(p -> !p.equals(target) && !p.equals(temporary))
              .sorted(Comparator.<Path, Boolean>comparing(p -> !p.equals(from.resolve(JarFile.MANIFEST_NAME)))
                  .thenComparing(Comparator.naturalOrder())).toList();
        }
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(temporary))) {
          for (Path file : files) {
            JarEntry entry = new JarEntry(from.relativize(file).toString().replace('\\', '/'));
            entry.setTime(Files.getLastModifiedTime(file).toMillis());
            jar.putNextEntry(entry);
            Files.copy(file, jar);
            jar.closeEntry();
          }
        }
      }
      try { Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
      catch (AtomicMoveNotSupportedException e) { Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING); }
    } finally { Files.deleteIfExists(temporary); }
  }
}

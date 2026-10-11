package com.zelix.klassmaster.web;

import com.zelix.klassmaster.web.Models.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public final class ProjectStore {
  public final Path home;
  private ProjectConfig active;

  public ProjectStore(Path home) throws IOException {
    this.home = home.toAbsolutePath().normalize();
    Files.createDirectories(this.home);
  }

  public synchronized ProjectConfig active() {
    if (active == null) throw new IllegalStateException("Open or create a project first");
    return active;
  }

  public synchronized ProjectConfig create(String directory, String name) throws IOException {
    Path root = Path.of(directory).toAbsolutePath().normalize();
    if (Files.exists(root.resolve("project.zkm.json")))
      throw new IllegalArgumentException("Project already exists; open it instead");
    ProjectConfig p = new ProjectConfig();
    p.directory = root.toString();
    p.workingDirectory = "";
    p.name = name.isBlank() ? root.getFileName().toString() : name;
    Files.createDirectories(root.resolve("scripts"));
    Files.createDirectories(root.resolve("runs"));
    ScriptDocument s = new ScriptDocument();
    s.path = "scripts/main.zkm";
    s.name = "main.zkm";
    p.scripts.add(s);
    p.activeScript = s.id;
    Json.write(root.resolve("project.zkm.json"), p);
    active = p;
    recent(root);
    return p;
  }

  public synchronized ProjectConfig open(String directory) throws IOException {
    Path root = Path.of(directory).toAbsolutePath().normalize();
    if (Files.isRegularFile(root)) root = root.getParent();
    ProjectConfig p = Json.read(root.resolve("project.zkm.json"), ProjectConfig.class);
    if (p.schemaVersion != 1)
      throw new IllegalArgumentException("Unsupported project schema: " + p.schemaVersion);
    p.directory = root.toString();
    active = p;
    recent(root);
    return p;
  }

  public synchronized ProjectConfig save(ProjectConfig p) throws IOException {
    ProjectConfig old = active();
    if (!p.id.equals(old.id) || p.revision != old.revision)
      throw new Conflict("Project changed; reload before saving");
    p.directory = old.directory;
    p.workingDirectory = portableIn(p, p.workingDirectory);
    p.build.inputs = p.build.inputs.stream().map(v -> portableIn(p, v)).toList();
    p.build.classpath = p.build.classpath.stream().map(v -> portableIn(p, v)).toList();
    p.build.output = portableIn(p, p.build.output);
    p.scripts.forEach(v -> v.path = portableIn(p, v.path));
    p.revision++;
    Json.write(Path.of(p.directory, "project.zkm.json"), p);
    active = p;
    return p;
  }

  public synchronized void persist() throws IOException {
    active.revision++;
    Json.write(Path.of(active.directory, "project.zkm.json"), active);
  }

  private static String portableIn(ProjectConfig p, String value) {
    if (value == null || value.isEmpty()) return "";
    if (value.indexOf('*') >= 0 || value.indexOf('?') >= 0 || value.indexOf('!') >= 0) return value;
    Path path = Path.of(value).normalize(), root = Path.of(p.directory);
    return path.isAbsolute() && path.startsWith(root)
        ? root.relativize(path).toString().replace('\\', '/')
        : value;
  }

  public Path resolve(String path) {
    Path p = Path.of(path);
    return (p.isAbsolute() ? p : Path.of(active().directory).resolve(p)).normalize();
  }

  public String portable(Path path) {
    Path root = Path.of(active().directory);
    path = path.toAbsolutePath().normalize();
    return path.startsWith(root)
        ? root.relativize(path).toString().replace('\\', '/')
        : path.toString();
  }

  public synchronized List<String> recents() throws IOException {
    Path p = home.resolve("recent.json");
    return Files.exists(p) ? Json.read(p, ArrayList.class) : new ArrayList<>();
  }

  private void recent(Path root) throws IOException {
    List<String> all = recents();
    all.remove(root.toString());
    all.add(0, root.toString());
    Json.write(home.resolve("recent.json"), all.subList(0, Math.min(15, all.size())));
  }

  public static class Conflict extends RuntimeException {
    public Conflict(String message) {
      super(message);
    }
  }
}

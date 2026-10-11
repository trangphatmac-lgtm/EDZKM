package com.zelix.klassmaster.web;

import com.zelix.klassmaster.exceptions.ZkmException;
import com.zelix.klassmaster.script.*;
import com.zelix.klassmaster.script.parser.ZkmScriptNode;
import com.zelix.klassmaster.script.parser.ast.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public final class RunObserver implements ExecutionObserver {
  public final Path root, sourcePath;
  public final String text;
  public final boolean preview;
  public boolean captured;
  private int statements, completed;

  @Override
  public void start(int total) {
    statements = total;
    completed = 0;
  }

  public final Map<Path, Path> redirected = new LinkedHashMap<>();
  public final Set<String> outputs = new LinkedHashSet<>();
  public final Set<String> changeLogs = new LinkedHashSet<>();

  public RunObserver(Path root, Path sourcePath, String text, boolean preview) throws IOException {
    this.root = root;
    this.sourcePath = sourcePath.toAbsolutePath().normalize();
    this.text = text;
    this.preview = preview;
    Files.createDirectories(root);
  }

  @Override
  public String source(File file) {
    return file.toPath().toAbsolutePath().normalize().equals(sourcePath) ? text : null;
  }

  @Override
  public File includeDirectory() {
    return sourcePath.getParent().toFile();
  }

  @Override
  public File output(File file) {
    Path original = file.toPath().toAbsolutePath().normalize();
    Path result = original;
    if (preview)
      result =
          redirected.computeIfAbsent(
              original,
              p ->
                  root.resolve("preview-output")
                      .resolve(EngineSession.hash(p.toString()).substring(0, 16))
                      .resolve(p.getFileName() == null ? "output" : p.getFileName().toString()));
    outputs.add(result.toString());
    return result.toFile();
  }

  @Override
  public File input(File file) {
    Path p = file.toPath().toAbsolutePath().normalize();
    return redirected.entrySet().stream()
        .filter(e -> p.startsWith(e.getKey()))
        .max(Comparator.comparingInt(e -> e.getKey().getNameCount()))
        .map(e -> e.getValue().resolve(e.getKey().relativize(p)).toFile())
        .orElse(file);
  }

  private ZkmScriptNode unwrap(ZkmScriptNode n) {
    return n instanceof ASTNTStatement ? n.jjtGetChild(0) : n;
  }

  @Override
  public void before(ZkmScriptNode node, ScriptEnvironment env) throws ZkmException, IOException {
    ZkmScriptNode n = unwrap(node);
    Progress.report(
        n instanceof SummarizingStatementNode statement ? statement.getStatementName() : "script",
        completed++,
        statements);
    if (!captured
        && (n instanceof ObfuscateStatementBase
            || n instanceof ASTTrimStatement
            || n instanceof SaveStatementNode)
        && env.getClassRepository().hasProgramClasses()) {
      EngineSession.snapshot(env, root.resolve("before"));
      captured = true;
    }
  }

  @Override
  public void after(ZkmScriptNode node, ScriptEnvironment env) throws ZkmException, IOException {
    if (unwrap(node) instanceof ASTLoadStatement && env.getClassRepository().hasProgramClasses()) {
      EngineSession.snapshot(env, root.resolve("before"), true);
      captured = true;
    }
    if (unwrap(node) instanceof ObfuscateStatementBase)
      for (String path : outputs)
        if (Files.isRegularFile(Path.of(path)) && !path.endsWith(".jar")) changeLogs.add(path);
  }

  public Object finish(ScriptEnvironment env) throws Exception {
    if (!captured) EngineSession.snapshot(env, root.resolve("before"));
    EngineSession.snapshot(env, root.resolve("after"));
    Map<String, Object> result =
        Json.map(
            "beforeRoot",
            root.resolve("before").toString(),
            "afterRoot",
            root.resolve("after").toString(),
            "outputs",
            outputs,
            "changeLogs",
            changeLogs);
    java.util.function.Function<String, String> portable =
        value -> {
          Path p = Path.of(value);
          return p.startsWith(root.getParent().getParent())
              ? root.relativize(p).toString().replace('\\', '/')
              : value;
        };
    Json.write(
        root.resolve("artifacts.json"),
        Json.map(
            "beforeRoot",
            "before",
            "afterRoot",
            "after",
            "outputs",
            outputs.stream().map(portable).toList(),
            "changeLogs",
            changeLogs.stream().map(portable).toList()));
    return result;
  }
}

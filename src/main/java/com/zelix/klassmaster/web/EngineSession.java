package com.zelix.klassmaster.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.zelix.klassmaster.KlassMaster;
import com.zelix.klassmaster.changelog.StackTraceTranslator;
import com.zelix.klassmaster.classfile.*;
import com.zelix.klassmaster.classfile.constpool.*;
import com.zelix.klassmaster.classfile.hierarchy.*;
import com.zelix.klassmaster.config.TrimOptions;
import com.zelix.klassmaster.exceptions.ZkmException;
import com.zelix.klassmaster.proguard.ProGuardInputTranslator;
import com.zelix.klassmaster.script.*;
import com.zelix.klassmaster.script.parser.*;
import com.zelix.klassmaster.script.parser.ast.*;
import com.zelix.klassmaster.util.*;
import com.zelix.klassmaster.web.Models.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

/** Headless adapter over the original parser and mutable class repository. Owned by one worker. */
public final class EngineSession implements AutoCloseable {
  private ScriptEnvironment env;
  private ClassRepository repository;
  private final Path workspace;

  public EngineSession(Path workspace) throws IOException {
    this.workspace = workspace;
    Files.createDirectories(workspace);
  }

  public Object execute(JsonNode request) throws Exception {
    String op = request.path("operation").asText("script");
    if (op.equals("validate")) {
      Path source =
          Path.of(request.path("sourcePath").asText(workspace.resolve("script.zkm").toString()));
      ExecutionObserver.CURRENT.set(
          new RunObserver(workspace, source, request.path("script").asText(), false));
      try {
        new ZkmScriptParser(
                new ScriptPreprocessor(source.toString(), new Properties()).createReader())
            .Input();
        return Json.map("valid", true);
      } finally {
        ExecutionObserver.CURRENT.remove();
      }
    }
    if (op.equals("proguard"))
      return Json.map(
          "text",
          ProGuardInputTranslator.translateToZkmScript(request.path("text").asText(), null));
    if (op.equals("stacktrace")) {
      List<String> logs = new ArrayList<>();
      request.path("changeLogs").forEach(x -> logs.add(x.asText()));
      StackTraceTranslator t =
          new StackTraceTranslator(logs, new ZkmClasspath(request.path("classpath").asText("")));
      try {
        t.initialize();
        return Json.map(
            "text",
            t.translate(
                request.path("text").asText(), request.path("analyzeBytecode").asBoolean(false)));
      } finally {
        t.dispose();
      }
    }
    if (op.equals("gc")) {
      Runtime r = Runtime.getRuntime();
      long before = r.totalMemory() - r.freeMemory();
      System.gc();
      return Json.map(
          "before", before, "after", r.totalMemory() - r.freeMemory(), "max", r.maxMemory());
    }
    if (op.equals("bytecodeFile")) {
      StringBuilder script = new StringBuilder("classpath ");
      request
          .path("libraries")
          .forEach(v -> script.append(BuildScripts.quote(v.asText())).append(" "));
      script
          .append(";\nopen ")
          .append(BuildScripts.quote(request.path("file").asText()))
          .append(";");
      var open = (com.fasterxml.jackson.databind.node.ObjectNode) request.deepCopy();
      open.put("operation", "open");
      open.put("script", script.toString());
      run(open);
      ProgramClass c = all(repository).get(0);
      StringBuilder source = new StringBuilder(c.formatDeclaration());
      for (AbstractMethodInfo m : c.getDeclaredMethods())
        if (m instanceof MethodInfo method) source.append("\n").append(method.formatDeclaration());
      return Json.map("source", source.toString(), "engine", "bytecode");
    }
    if (op.equals("trimReport")) {
      var open = request.deepCopy();
      ((com.fasterxml.jackson.databind.node.ObjectNode) open).put("operation", "open");
      run(open);
      var tree =
          new ZkmScriptParser(new StringReader(request.path("trimStatement").asText("trim;")))
              .Input();
      ASTTrimStatement statement = (ASTTrimStatement) tree.jjtGetChild(0).jjtGetChild(0);
      for (int i = 0; i < statement.jjtGetNumChildren(); i++) {
        DelegatingParameterNode parameter = (DelegatingParameterNode) statement.jjtGetChild(i);
        parameter.execute(statement, env);
        for (int j = 0; j < parameter.getValueCount(); j++)
          statement.parameterValues.addValue(parameter.getParameterName(), parameter.getValue(j));
      }
      TrimOptions options = new TrimOptions();
      statement.applyDeleteSourceFileAttributes(options);
      statement.applyDeleteDeprecatedAttributes(options);
      statement.applyDeleteAnnotationAttributes(options);
      statement.applyDeleteUnknownAttributes(options);
      statement.applyDeleteExceptionAttributes(options);
      statement.applyDeleteDebugExtensionAttributes(options);
      StringWriter report = new StringWriter();
      repository.reportTrimPreview(
          new Vector(env.getTrimExcludeStatements()),
          new Vector(env.getTrimUnexcludeStatements()),
          new PrintWriter(report),
          NoOpCallback.getInstance(),
          options,
          env);
      Files.writeString(workspace.resolve("trim-report.txt"), report.toString());
      return Json.map("text", report.toString());
    }
    if (op.equals("script") || op.equals("open")) return run(request);
    if (repository == null) throw new IllegalStateException("Open classes first");
    if (op.equals("tree")) return tree(request);
    if (op.equals("bytecode")) {
      ProgramClass c = find(request.path("classId").asText());
      StringBuilder source = new StringBuilder(c.formatDeclaration());
      for (AbstractMethodInfo m : c.getDeclaredMethods())
        if (m instanceof MethodInfo method) source.append("\n").append(method.formatDeclaration());
      return Json.map("source", source.toString());
    }
    if (op.equals("classes"))
      return classes(
          request.path("query").asText(""),
          request.path("offset").asInt(),
          request.path("limit").asInt(150));
    if (op.equals("class")) return details(find(request.path("classId").asText()));
    if (op.equals("edit")) {
      edit(request);
      snapshot(env, workspace.resolve("checkpoint-classes"));
      return details(find(request.path("classId").asText()));
    }
    if (op.equals("statements")) {
      Path root = Path.of(request.path("runDirectory").asText(workspace.toString()));
      RunObserver observer =
          new RunObserver(
              root,
              root.resolve("statements.zkm"),
              request.path("script").asText(),
              request.path("preview").asBoolean());
      ExecutionObserver.CURRENT.set(observer);
      try {
        new ZkmScriptParser(new StringReader(request.path("script").asText()))
            .Input().execute(null, env);
        return observer.finish(env);
      } finally {
        ExecutionObserver.CURRENT.remove();
      }
    }
    if (op.equals("checkpoint")) {
      Path path = Path.of(request.path("path").asText());
      Files.createDirectories(path);
      repository.setArchiveSaveFiles(new HashMap());
      repository.setJadSaveFiles(new HashMap());
      repository.setXmlSaveFiles(new HashMap());
      repository.setYamlSaveFiles(new HashMap());
      repository.setPropertiesSaveFiles(new HashMap());
      repository.saveAll(
          3,
          false,
          false,
          null,
          path.toFile(),
          new com.zelix.klassmaster.log.LogMessageReporter(env, 0),
          env,
          NoOpCallback.getInstance());
      return Json.map("path", path.toString());
    }
    throw new IllegalArgumentException("Unknown engine operation: " + op);
  }

  private Object run(JsonNode request) throws Exception {
    close();
    Path root = Path.of(request.path("runDirectory").asText(workspace.toString())).toAbsolutePath();
    Files.createDirectories(root);
    Path source = Path.of(request.path("sourcePath").asText(root.resolve("script.zkm").toString()));
    RunObserver observer =
        new RunObserver(
            root, source, request.path("script").asText(), request.path("preview").asBoolean());
    ExecutionObserver.CURRENT.set(observer);
    ObservableHolder envHolder = new ObservableHolder(), readerHolder = new ObservableHolder();
    try {
      KlassMaster app = new KlassMaster();
      repository =
          app.executeScript(
              source.toString(),
              root.resolve("ZKM.log").toString(),
              root.resolve("Trim.log").toString(),
              null,
              null,
              null,
              null,
              request.path("workingDirectory").asText(System.getProperty("user.dir")),
              false,
              false,
              new Properties(),
              envHolder,
              readerHolder,
              true,
              false,
              false,
              new ObservableHolder(),
              false,
              null);
      env = (ScriptEnvironment) envHolder.getValue();
      if (request.path("operation").asText().equals("open")) return classes("", 0, 150);
      return observer.finish(env);
    } finally {
      env = (ScriptEnvironment) envHolder.getValue();
      if (readerHolder.getValue() instanceof Closeable r) r.close();
      if (env != null) env.getLogWriter().flush();
      ExecutionObserver.CURRENT.remove();
    }
  }

  private static List<ProgramClass> all(ClassRepository repository) {
    Set<ProgramClass> set = Collections.newSetFromMap(new IdentityHashMap<>());
    Enumeration e = repository.enumerateProgramClasses();
    while (e.hasMoreElements()) {
      ProgramClass c = (ProgramClass) e.nextElement();
      set.add(c);
      for (Object v : c.getAllVersions()) if (v instanceof ProgramClass p) set.add(p);
    }
    List<ProgramClass> list = new ArrayList<>(set);
    list.sort(
        Comparator.comparing(ProgramClass::getClassName).thenComparing(EngineSession::identity));
    return list;
  }

  public static String identity(ProgramClass c) {
    return hash(
        (c.getInputLocation() == null
                ? c.getOriginalClassName()
                : c.getInputLocation().getQualifiedName())
            + "@"
            + c.getReleaseVersion());
  }

  public static String hash(String s) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  private ProgramClass find(String id) {
    return all(repository).stream()
        .filter(c -> identity(c).equals(id))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Class not found"));
  }

  private Object classes(String query, int offset, int limit) {
    List<Object> values = new ArrayList<>();
    List<ProgramClass> all =
        all(repository).stream()
            .filter(
                c ->
                    query.startsWith("main:")
                        ? Arrays.stream(c.getDeclaredMethods())
                            .anyMatch(
                                m ->
                                    m.getJvmName().equals("main")
                                        && m.getDescriptor().equals("([Ljava/lang/String;)V")
                                        && (m.getAccessFlags() & 9) == 9)
                        : c.getClassName()
                            .replace('/', '.')
                            .toLowerCase(Locale.ROOT)
                            .contains(query.replace('/', '.').toLowerCase(Locale.ROOT)))
            .toList();
    for (ProgramClass c :
        all.subList(
            Math.min(Math.max(0, offset), all.size()),
            Math.min(Math.max(0, offset) + Math.max(1, Math.min(500, limit)), all.size())))
      values.add(summary(c));
    return Json.map("classes", values, "total", all.size());
  }

  private Object tree(JsonNode request) {
    boolean hierarchy = request.path("hierarchy").asBoolean();
    String parent = request.path("parent").asText("");
    List<ProgramClass> classes = all(repository);
    Set<String> names = new HashSet<>(), parents = new HashSet<>();
    classes.forEach(
        c -> {
          names.add(c.getClassName());
          parents.add(c.getSuperclassName());
        });
    Set<String> branches = new TreeSet<>();
    List<Object> children = new ArrayList<>();
    for (ProgramClass c : classes) {
      String name = c.getClassName();
      if (hierarchy) {
        String superclass = c.getSuperclassName();
        boolean root = superclass == null || !names.contains(superclass);
        if (parent.isEmpty() ? root : parent.equals(superclass)) {
          Map<String, Object> child = summary(c);
          child.put("children", parents.contains(name));
          child.put("branch", name);
          children.add(child);
        }
      } else {
        String prefix = parent.isEmpty() ? "" : parent + "/";
        if (!name.startsWith(prefix)) continue;
        String remaining = name.substring(prefix.length());
        int slash = remaining.indexOf('/');
        if (slash >= 0) branches.add(prefix + remaining.substring(0, slash));
        else children.add(summary(c));
      }
    }
    List<Object> result = new ArrayList<>();
    for (String branch : branches)
      result.add(
          Json.map(
              "id",
              "package:" + branch,
              "name",
              branch.replace('/', '.'),
              "branch",
              branch,
              "children",
              true,
              "package",
              true));
    result.addAll(children);
    return Json.map("nodes", result, "total", classes.size());
  }

  private static Map<String, Object> summary(ProgramClass c) {
    return Json.map(
        "id",
        identity(c),
        "name",
        c.getClassName().replace('/', '.'),
        "superclass",
        c.getSuperclassName(),
        "interfaces",
        c.getInterfaceNames(),
        "flags",
        c.getAccessFlags(),
        "origin",
        c.getLocationName(),
        "version",
        c.getReleaseVersion(),
        "majorVersion",
        c.getMajorVersion());
  }

  private Object details(ProgramClass c) throws Exception {
    Map<String, Object> result = summary(c);
    result.put("declaration", c.formatDeclaration());
    List<Object> fields = new ArrayList<>(),
        methods = new ArrayList<>(),
        constants = new ArrayList<>();
    for (AbstractFieldInfo f : c.getFields())
      fields.add(
          Json.map(
              "name",
              f.getJvmName(),
              "descriptor",
              f.getDescriptor(),
              "flags",
              f.getAccessFlags(),
              "declaration",
              f.formatDeclaration()));
    for (AbstractMethodInfo m : c.getDeclaredMethods())
      methods.add(
          Json.map(
              "name",
              m.getJvmName(),
              "descriptor",
              m.getDescriptor(),
              "flags",
              m.getAccessFlags(),
              "declaration",
              m instanceof MethodInfo ? ((MethodInfo) m).formatDeclaration() : m.toString()));
    NumericConstantEntry[] literals = ((ConstantPool) c.getConstantPool()).getLiteralConstants();
    for (int i = 0; i < literals.length; i++) {
      NumericConstantEntry v = literals[i];
      if (v instanceof ResolvedStringConstant string && string.getSourceConstant() != null)
        continue;
      constants.add(
          Json.map(
              "index",
              i,
              "poolIndex",
              v.getIndex(),
              "type",
              v.getTypeName(),
              "value",
              v.getEditableValue(),
              "editable",
              true));
    }
    result.put("fields", fields);
    result.put("methods", methods);
    result.put("constants", constants);
    result.put("multiRelease", c.hasVersionedVariants() || c.isVersionedVariant());
    return result;
  }

  private void edit(JsonNode r) throws Exception {
    ProgramClass c = find(r.path("classId").asText());
    String kind = r.path("kind").asText();
    if (c.hasVersionedVariants() || c.isVersionedVariant())
      throw new IllegalArgumentException("Cannot modify a class with multi-release versions");
    if (kind.equals("constant")) {
      NumericConstantEntry[] constants = ((ConstantPool) c.getConstantPool()).getLiteralConstants();
      int index = r.path("index").asInt(-1);
      if (index < 0 || index >= constants.length)
        throw new IllegalArgumentException("Constant not found");
      NumericConstantEntry entry = constants[index];
      entry.setValueFromString(r.path("value").asText());
      for (NumericConstantEntry other : constants)
        if (other instanceof ResolvedStringConstant string && string.getSourceConstant() == entry)
          string.setValueFromString(r.path("value").asText());
      return;
    }
    if (kind.equals("class")) {
      String name = r.path("name").asText(c.getClassSimpleName()).trim();
      if (!name.equals(c.getClassSimpleName())) {
        if (name.isEmpty() || name.indexOf('/') >= 0 || name.indexOf('.') >= 0)
          throw new IllegalArgumentException("Enter a simple class name");
        if (ClassHierarchyNode.findNode(c.getClassName()).hasEnclosingNode())
          throw new IllegalArgumentException("Cannot rename inner classes in this version");
        String full =
            (c.getPackageName().isEmpty() ? "" : c.getPackageName().replace('.', '/') + "/") + name;
        if (ClassHierarchyNode.isProgramClassName(full))
          throw new IllegalArgumentException("Class already exists");
        HashMap names = new HashMap(), refs = new HashMap();
        c.renameClass(full, names, refs);
        repository.applyNameChangesToAll(refs, null, null);
        repository.applyFieldPrefixRenames(refs);
        repository.refreshIndexes();
      }
      if (r.has("flags"))
        c.setAccessFlags((c.getAccessFlags() & ~1585) | (r.path("flags").asInt() & 1585));
      return;
    }
    MemberInfo member = null;
    if (kind.equals("field"))
      for (AbstractFieldInfo f : c.getFields())
        if (f.getJvmName().equals(r.path("member").asText())
            && f.getDescriptor().equals(r.path("descriptor").asText())) member = (MemberInfo) f;
    if (kind.equals("method"))
      for (AbstractMethodInfo m : c.getDeclaredMethods())
        if (m.getJvmName().equals(r.path("member").asText())
            && m.getDescriptor().equals(r.path("descriptor").asText())) member = (MemberInfo) m;
    if (member == null) throw new IllegalArgumentException("Member not found");
    if (r.has("name") && !r.path("name").asText().equals(member.getJvmName())) {
      if (kind.equals("method"))
        throw new IllegalArgumentException(
            "Manual method rename is not supported by the original editor");
      repository.renameField((FieldInfo) member, r.path("name").asText());
    }
    if (r.has("flags")) {
      int flags = r.path("flags").asInt();
      if (Integer.bitCount(flags & 7) > 1)
        throw new IllegalArgumentException(
            "Choose one visibility: public, protected, private or package");
      if (kind.equals("method") && (flags & 1024) != 0 && (flags & (2 | 8 | 16 | 256 | 32)) != 0)
        throw new IllegalArgumentException("Invalid abstract method modifiers");
      if (kind.equals("field") && (flags & 16) != 0 && (flags & 64) != 0)
        throw new IllegalArgumentException("A field cannot be both final and volatile");
      if (kind.equals("field") && c.isInterface() && (flags & 25) != 25)
        throw new IllegalArgumentException("Fields in interfaces must be public, static and final");
      int mask = kind.equals("method") ? 1343 : 223;
      member.setAccessFlags((member.getAccessFlags() & ~mask) | (flags & mask));
    }
  }

  public static void snapshot(ScriptEnvironment env, Path root) throws ZkmException, IOException {
    snapshot(env, root, false);
  }

  public static void snapshot(ScriptEnvironment env, Path root, boolean appendNew)
      throws ZkmException, IOException {
    Files.createDirectories(root);
    ClassRepository repo = env.getClassRepository();
    List<ClassIdentity> manifest = appendNew ? Comparisons.read(root) : new ArrayList<>();
    Set<String> existing = new HashSet<>();
    manifest.forEach(c -> existing.add(c.id));
    if (!repo.hasProgramClasses()) {
      for (ClassIdentity item : manifest)
        if (item.file != null && Path.of(item.file).isAbsolute())
          item.file =
              root.toAbsolutePath().relativize(Path.of(item.file)).toString().replace('\\', '/');
      Json.write(root.resolve("classes.json"), manifest);
      Json.write(root.resolve("snapshot.json"), Json.map("native", true));
      return;
    }
    CommonSuperTypeResolver resolver = new CommonSuperTypeResolver(repo, repo.getClassResolver());
    for (ProgramClass c : all(repo)) {
      if (existing.contains(identity(c))) continue;
      ClassIdentity item = new ClassIdentity();
      item.id = identity(c);
      item.name = c.getClassName();
      item.originalName = repo.getOriginalClassName(c.getClassName());
      item.origin =
          c.getInputLocation() == null ? "generated" : c.getInputLocation().getArchivePath();
      if (item.origin == null) item.origin = c.getLocationName();
      item.version = String.valueOf(c.getReleaseVersion());
      item.entry =
          c.getInputLocation() == null
              ? c.getClassName() + ".class"
              : c.getInputLocation().getName();
      String group = hash(item.origin + "@" + item.version).substring(0, 16);
      Path target =
          root.resolve("classes").resolve(group).resolve(c.getClassName() + ".class").normalize();
      if (!target.startsWith(root.toAbsolutePath().normalize())
          && !target.startsWith(root.normalize()))
        throw new IOException("Invalid class name: " + c.getClassName());
      Files.createDirectories(target.getParent());
      try (DataOutputStream stream = new DataOutputStream(Files.newOutputStream(target))) {
        c.writeClassFile(stream, resolver, repo, env);
      }
      item.file =
          root.toAbsolutePath().relativize(target.toAbsolutePath()).toString().replace('\\', '/');
      manifest.add(item);
    }
    for (ClassIdentity item : manifest)
      if (item.file != null && Path.of(item.file).isAbsolute())
        item.file =
            root.toAbsolutePath().relativize(Path.of(item.file)).toString().replace('\\', '/');
    Json.write(root.resolve("classes.json"), manifest);
    Json.write(root.resolve("snapshot.json"), Json.map("native", true));
  }

  @Override
  public void close() {
    if (env != null) env.closeLog();
    if (repository != null) repository.releaseResources();
    env = null;
    repository = null;
  }
}

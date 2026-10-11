package com.zelix.klassmaster.web;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.*;
import com.zelix.klassmaster.web.Models.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public final class WebServer implements AutoCloseable {
  private final HttpServer http;
  private final ExecutorService requests = Executors.newCachedThreadPool(),
      tasks = Executors.newFixedThreadPool(4);
  private final ProjectStore projects;
  private final String token;
  private final ConcurrentMap<String, Job> jobs = new ConcurrentHashMap<>();
  private final ConcurrentMap<String, WorkerClient> workers = new ConcurrentHashMap<>();
  private final List<Event> events = new ArrayList<>();
  private final AtomicLong eventId = new AtomicLong();
  private final DesktopIntegration desktop;
  private volatile boolean stopped;
  private WorkerClient session;
  private String sessionProject = "", sessionConfig = "";
  private final Object sessionLock = new Object();

  private record Event(long id, String json) {}

  public WebServer(int port, Path home) throws IOException {
    this(port, home, new DesktopIntegration());
  }

  WebServer(int port, Path home, DesktopIntegration desktop) throws IOException {
    this.desktop = desktop;
    projects = new ProjectStore(home);
    byte[] secret = new byte[32];
    new SecureRandom().nextBytes(secret);
    token = HexFormat.of().formatHex(secret);
    http = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
    http.setExecutor(requests);
    http.createContext("/", this::handle);
    for (String recent : projects.recents()) {
      if (Files.isRegularFile(Path.of(recent, "project.zkm.json"))) {
        try {
          projects.open(recent);
          loadHistory();
          break;
        } catch (IOException | IllegalArgumentException ignored) {
        }
      }
    }
  }

  public void start() {
    http.start();
  }

  public String url() {
    return base() + "/#token=" + token;
  }

  public String base() {
    return "http://127.0.0.1:" + http.getAddress().getPort();
  }

  private void handle(HttpExchange exchange) {
    try {
      String host = exchange.getRequestHeaders().getFirst("Host");
      if (!Objects.equals(host, "127.0.0.1:" + http.getAddress().getPort())) {
        send(exchange, 403, Json.map("message", "Invalid host"));
        return;
      }
      String origin = exchange.getRequestHeaders().getFirst("Origin");
      if (origin != null && !origin.equals(base())) {
        send(exchange, 403, Json.map("message", "Invalid origin"));
        return;
      }
      String path = exchange.getRequestURI().getPath();
      exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
      exchange.getResponseHeaders().set("Referrer-Policy", "no-referrer");
      if (!path.startsWith("/api/")) {
        staticFile(exchange, path);
        return;
      }
      exchange.getResponseHeaders().set("Cache-Control", "no-store");
      if (!authorized(exchange)) {
        send(
            exchange,
            401,
            Json.map("code", "UNAUTHORIZED", "message", "Open the launch URL printed by EDZKM"));
        return;
      }
      if (path.equals("/api/v1/events")) {
        if (!exchange.getRequestMethod().equals("GET"))
          throw new IllegalArgumentException("GET required");
        stream(exchange);
        return;
      }
      JsonNode body = Json.object();
      if (!exchange.getRequestMethod().equals("GET")) {
        byte[] bytes = exchange.getRequestBody().readNBytes(32 * 1024 * 1024 + 1);
        if (bytes.length > 32 * 1024 * 1024)
          throw new IllegalArgumentException("Request too large");
        if (bytes.length > 0) body = Json.MAPPER.readTree(bytes);
      }
      String route = path.substring("/api/v1".length());
      Object result = route(exchange, route, body);
      send(exchange, 200, result);
    } catch (Throwable e) {
      try {
        int status =
            e instanceof ProjectStore.Conflict
                ? 409
                : e instanceof FileNotFoundException || e instanceof NoSuchFileException
                    ? 404
                    : e instanceof IllegalArgumentException ? 400 : 500;
        send(
            exchange,
            status,
            e instanceof WorkerClient.WorkerFailure w ? w.details : Errors.describe(e));
      } catch (IOException ignored) {
        exchange.close();
      }
    }
  }

  private boolean authorized(HttpExchange e) {
    String header = e.getRequestHeaders().getFirst("X-ZKM-Token");
    if (header != null
        && MessageDigest.isEqual(
            header.getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8)))
      return true;
    String cookie = e.getRequestHeaders().getFirst("Cookie");
    return cookie != null
        && Arrays.stream(cookie.split(";"))
            .map(String::trim)
            .anyMatch(x -> x.equals("zkm-session=" + token));
  }

  private Object route(HttpExchange e, String route, JsonNode body) throws Exception {
    boolean get = e.getRequestMethod().equals("GET");
    Map<String, String> query = query(e.getRequestURI());
    if (route.equals("/session")) {
      post(e);
      e.getResponseHeaders()
          .set("Set-Cookie", "zkm-session=" + token + "; HttpOnly; SameSite=Strict; Path=/api/v1");
      return Json.map("ok", true);
    }
    if (route.equals("/state")) {
      ProjectConfig active = null;
      try {
        active = projects.active();
      } catch (IllegalStateException ignored) {
      }
      return Json.map(
          "project",
          active,
          "recent",
          projects.recents(),
          "home",
          System.getProperty("user.home"),
          "cwd",
          System.getProperty("user.dir"),
          "java",
          System.getProperty("java.version"),
          "version",
          "27.0.0",
          "jobs",
          history());
    }
    if (route.equals("/projects/create") || route.equals("/projects/open")) {
      post(e);
      if (busy())
        throw new ProjectStore.Conflict(
            "Wait for active project operations before switching projects");
      resetSession();
      ProjectConfig p =
          route.endsWith("create")
              ? projects.create(body.path("directory").asText(), body.path("name").asText())
              : projects.open(body.path("directory").asText());
      loadHistory();
      return p;
    }
    if (route.equals("/project")) {
      if (get) return projects.active();
      post(e);
      ProjectConfig next = Json.MAPPER.treeToValue(body, ProjectConfig.class);
      if (busy() && !sessionSignature(next).equals(sessionSignature(projects.active())))
        throw new ProjectStore.Conflict("Wait for the active edit before changing inputs");
      return projects.save(next);
    }
    if (route.equals("/desktop/pick")) {
      post(e);
      String mode = body.path("directory").asBoolean() ? "directory" : "file";
      boolean zh = body.path("language").asText().equals("zh");
      Path path = desktop.choose(mode, zh ? (mode.equals("directory") ? "选择目录" : "选择文件")
          : (mode.equals("directory") ? "Choose a directory" : "Choose a file"), desktopPath(body.path("start").asText()), "");
      return Json.map("path", path == null ? null : path.toString(), "cancelled", path == null);
    }
    if (route.equals("/desktop/open")) {
      post(e);
      Path path = desktopPath(body.path("path").asText());
      if (Files.isRegularFile(path)) path = path.getParent();
      desktop.openFolder(path);
      return Json.map("path", path.toString());
    }
    if (route.equals("/outputs")) {
      Job source = job(query.getOrDefault("jobId", ""));
      return Json.map("files", OutputFiles.candidates(source), "roots", OutputFiles.roots(source).stream().map(Path::toString).toList());
    }
    if (route.equals("/outputs/export")) {
      post(e);
      Job source = job(body.path("jobId").asText());
      checkExportIdle(source.projectId);
      OutputFiles.Candidate artifact = OutputFiles.candidates(source).stream()
          .filter(f -> f.path().equals(body.path("path").asText())).findFirst()
          .orElseThrow(() -> new IllegalArgumentException("Select a completed JAR or class directory"));
      String name = artifact.kind().equals("jar") ? artifact.name() : artifact.name() + ".jar";
      Path target = desktop.choose("save", body.path("language").asText().equals("zh") ? "导出成品 JAR" : "Export finished JAR",
          Path.of(artifact.path()).getParent(), name);
      if (target == null) return Json.map("cancelled", true);
      checkExportIdle(source.projectId);
      OutputFiles.export(artifact, target);
      return Json.map("path", target.toString(), "cancelled", false);
    }
    if (route.equals("/files")) {
      if (!get) throw new IllegalArgumentException("GET required");
      Path dir =
          Path.of(query.getOrDefault("path", System.getProperty("user.home")))
              .toAbsolutePath()
              .normalize();
      List<Object> list = new ArrayList<>();
      try (var entries = Files.list(dir)) {
        for (Path p :
            entries
                .sorted(
                    Comparator.comparing((Path p) -> !Files.isDirectory(p))
                        .thenComparing(p -> p.getFileName().toString().toLowerCase(Locale.ROOT)))
                .limit(3000)
                .toList())
          list.add(
              Json.map(
                  "name",
                  p.getFileName().toString(),
                  "path",
                  p.toString(),
                  "directory",
                  Files.isDirectory(p),
                  "size",
                  Files.isRegularFile(p) ? Files.size(p) : 0));
      }
      return Json.map(
          "path",
          dir.toString(),
          "parent",
          dir.getParent() == null ? null : dir.getParent().toString(),
          "entries",
          list,
          "roots",
          Arrays.stream(File.listRoots()).map(File::getPath).toList());
    }
    if (route.equals("/files/read")) {
      Path path = projects.resolve(query.get("path"));
      if (Files.size(path) > 4 * 1024 * 1024)
        throw new IllegalArgumentException("Text file exceeds 4 MB");
      return Json.map("path", path.toString(), "content", Files.readString(path));
    }
    if (route.equals("/files/write")) {
      post(e);
      Path path = projects.resolve(body.path("path").asText());
      Files.createDirectories(path.getParent());
      Files.writeString(path, body.path("content").asText());
      return Json.map("path", projects.portable(path));
    }
    if (route.equals("/files/import")) {
      post(e);
      String name = Path.of(body.path("name").asText()).getFileName().toString();
      Path target =
          Path.of(projects.active().directory, "imports", UUID.randomUUID().toString(), name);
      Files.createDirectories(target.getParent());
      Files.write(target, Base64.getDecoder().decode(body.path("base64").asText()));
      return Json.map("path", projects.portable(target));
    }
    if (route.equals("/build/script")) {
      post(e);
      ProjectConfig p =
          body.has("project")
              ? Json.MAPPER.treeToValue(body.get("project"), ProjectConfig.class)
              : projects.active();
      return Json.map("script", BuildScripts.generate(p, body.path("mode").asText("build")));
    }
    if (route.equals("/jobs")) {
      if (get) return history();
      post(e);
      return submit(body);
    }
    if (route.startsWith("/jobs/") && route.endsWith("/cancel")) {
      post(e);
      String id = route.split("/")[2];
      Job job = job(id);
      if (!Set.of("queued", "running").contains(job.status)) return job;
      synchronized (job) {
        job.status = "cancelled";
        WorkerClient worker = workers.remove(id);
        if (worker != null) worker.close();
      }
      job.finished = Instant.now().toString();
      persist(job);
      emit("job", job);
      return job;
    }
    if (route.startsWith("/jobs/")) {
      Job job = job(route.split("/")[2]);
      return job;
    }
    if (route.equals("/engine")) {
      post(e);
      String op = body.path("operation").asText();
      if (!Set.of("classes", "class", "tree", "bytecode", "gc").contains(op))
        throw new IllegalArgumentException("Use the job API for mutations");
      if (op.equals("gc")) {
        Runtime r = Runtime.getRuntime();
        long used = r.totalMemory() - r.freeMemory();
        System.gc();
        return Json.map(
            "before", used, "after", r.totalMemory() - r.freeMemory(), "max", r.maxMemory());
      }
      return session(null, null).call(body, null, 60);
    }
    if (route.equals("/comparison/manual")) {
      post(e);
      Job j = job(body.path("jobId").asText());
      if (!j.status.equals("succeeded"))
        throw new IllegalArgumentException("Select a successful run");
      JsonNode result = Json.tree(j.result);
      String pairId = body.path("pairId").asText(), afterId = body.path("afterId").asText();
      if (Comparisons.pair(
              Path.of(result.path("beforeRoot").asText()),
              Path.of(result.path("afterRoot").asText()))
          .stream()
          .noneMatch(p -> pairId.equals(p.get("id"))))
        throw new IllegalArgumentException("Pair not found");
      if (!afterId.isEmpty()
          && Comparisons.read(Path.of(result.path("afterRoot").asText())).stream()
              .noneMatch(c -> afterId.equals(c.id)))
        throw new IllegalArgumentException("Class not found");
      Path mapping = Path.of(j.directory, "manual-pairs.json");
      synchronized (j) {
        Map<String, String> pairs =
            Files.exists(mapping) ? Json.read(mapping, LinkedHashMap.class) : new LinkedHashMap<>();
        pairs.put(pairId, afterId);
        Json.write(mapping, pairs);
        return pairs;
      }
    }
    if (route.equals("/runtime/config")) {
      Job source = job(query.getOrDefault("jobId", ""));
      if (!source.status.equals("succeeded")) throw new IllegalArgumentException("Select a successful run");
      ProjectConfig snapshot = Json.read(Path.of(source.directory, "project.json"), ProjectConfig.class);
      snapshot.directory = Path.of(source.directory).getParent().getParent().toString();
      List<String> inputs = snapshot.checkpointInputs.isEmpty() ? snapshot.build.inputs : snapshot.checkpointInputs;
      List<String> before = new ArrayList<>(inputs.stream().map(p -> BuildScripts.path(snapshot, p)).toList());
      List<String> after = new ArrayList<>();
      JsonNode result = Json.tree(source.result);
      if (source.kind.equals("compareImport")) {
        JsonNode originalRequest = Json.MAPPER.readTree(Path.of(source.directory, "request.json").toFile());
        before = new ArrayList<>(List.of(BuildScripts.path(snapshot, originalRequest.path("before").asText())));
        after.add(BuildScripts.path(snapshot, originalRequest.path("after").asText()));
      } else {
        for (JsonNode output : result.path("outputs")) {
          Path path = Path.of(output.asText());
          if (Files.isDirectory(path)) {
            after.add(path.toString());
            try (var files = Files.walk(path)) {
              files.filter(Files::isRegularFile).filter(p -> p.toString().toLowerCase(Locale.ROOT).endsWith(".jar"))
                  .sorted().forEach(p -> after.add(p.toString()));
            }
          } else if (Files.isRegularFile(path) && path.toString().toLowerCase(Locale.ROOT).endsWith(".jar")) after.add(path.toString());
        }
      }
      List<String> dependencies = snapshot.build.classpath.stream().map(p -> BuildScripts.path(snapshot, p)).toList();
      if (!before.isEmpty()) before.addAll(dependencies);
      if (!after.isEmpty()) after.addAll(dependencies);
      return Json.map("before", before.stream().distinct().toList(), "after", after.stream().distinct().toList(),
          "workingDirectory", BuildScripts.path(snapshot, snapshot.workingDirectory));
    }
    if (route.equals("/comparison")) {
      Job j = job(query.get("jobId"));
      if (!j.status.equals("succeeded"))
        throw new IllegalArgumentException("Select a successful run");
      JsonNode result = Json.tree(j.result);
      return Json.map(
          "manual",
          Files.exists(Path.of(j.directory, "manual-pairs.json"))
              ? Json.read(Path.of(j.directory, "manual-pairs.json"), Map.class)
              : Map.of(),
          "classes",
          Comparisons.pair(
              Path.of(result.path("beforeRoot").asText()),
              Path.of(result.path("afterRoot").asText())),
          "before",
          Comparisons.read(Path.of(result.path("beforeRoot").asText())),
          "after",
          Comparisons.read(Path.of(result.path("afterRoot").asText())));
    }
    if (route.equals("/artifacts")) {
      Job j = job(query.get("jobId"));
      List<Artifact> list = new ArrayList<>();
      try (var paths = Files.walk(Path.of(j.directory))) {
        for (Path p :
            paths
                .filter(Files::isRegularFile)
                .filter(p -> !p.toString().contains(File.separator + "classes" + File.separator))
                .limit(3000)
                .toList())
          list.add(new Artifact(p.toString(), p.getFileName().toString(), "file", Files.size(p)));
      }
      for (JsonNode output : Json.tree(j.result).path("outputs")) {
        Path root = Path.of(output.asText());
        if (Files.exists(root))
          try (var paths = Files.walk(root)) {
            for (Path p : paths.filter(Files::isRegularFile).limit(3000).toList())
              if (list.stream().noneMatch(a -> a.path.equals(p.toString())))
                list.add(
                    new Artifact(
                        p.toString(), p.getFileName().toString(), "output", Files.size(p)));
          }
      }
      return Json.map("files", list, "outputs", Json.tree(j.result).path("outputs"));
    }
    if (route.equals("/artifact")) {
      Path path = Path.of(query.get("path")).toAbsolutePath().normalize();
      if (!downloadAllowed(path))
        throw new IllegalArgumentException("Select an artifact from project run history");
      e.getResponseHeaders().set("Content-Type", "application/octet-stream");
      e.getResponseHeaders()
          .set(
              "Content-Disposition",
              "attachment; filename*=UTF-8''"
                  + URLEncoder.encode(path.getFileName().toString(), StandardCharsets.UTF_8));
      e.sendResponseHeaders(200, Files.size(path));
      try (OutputStream out = e.getResponseBody()) {
        Files.copy(path, out);
      }
      return null;
    }
    if (route.equals("/swing")) {
      post(e);
      new ProcessBuilder(WorkerClient.javaCommand("com.zelix.ZKM", "--swing")).inheritIO().start();
      return Json.map("started", true);
    }
    if (route.equals("/shutdown")) {
      post(e);
      tasks.submit(
          () -> {
            try {
              Thread.sleep(200);
            } catch (InterruptedException ignored) {
            }
            close();
          });
      return Json.map("stopping", true);
    }
    throw new FileNotFoundException(route);
  }

  private void checkExportIdle(String projectId) {
    if (jobs.values().stream().anyMatch(j -> Objects.equals(j.projectId, projectId)
        && Set.of("queued", "running").contains(j.status)
        && Set.of("build", "script", "save").contains(j.kind)))
      throw new ProjectStore.Conflict("Wait for the active build before exporting");
  }

  private Path desktopPath(String value) {
    if (value.isBlank()) {
      try { return Path.of(projects.active().directory); }
      catch (IllegalStateException ignored) { return Path.of(System.getProperty("user.home")); }
    }
    Path path = Path.of(value);
    if (path.isAbsolute()) return path.normalize();
    try { return Path.of(projects.active().directory).resolve(path).normalize(); }
    catch (IllegalStateException ignored) { return Path.of(System.getProperty("user.home")).resolve(path).normalize(); }
  }

  private boolean downloadAllowed(Path path) {
    if (path.startsWith(Path.of(projects.active().directory))) return true;
    return history().stream()
        .filter(j -> j.status.equals("succeeded"))
        .anyMatch(
            j -> {
              for (JsonNode output : Json.tree(j.result).path("outputs")) {
                Path root = Path.of(output.asText()).toAbsolutePath().normalize();
                if (path.equals(root) || (Files.isDirectory(root) && path.startsWith(root)))
                  return true;
              }
              return false;
            });
  }

  private static void post(HttpExchange e) {
    if (!e.getRequestMethod().equals("POST")) throw new IllegalArgumentException("POST required");
  }

  private boolean busy() {
    return jobs.values().stream()
        .anyMatch(
            j ->
                Set.of("queued", "running").contains(j.status)
                    && Set.of("open", "edit", "transform", "save").contains(j.kind));
  }

  private synchronized Job submit(JsonNode body) throws Exception {
    ProjectConfig project = Json.MAPPER.convertValue(projects.active(), ProjectConfig.class);
    String kind = body.path("kind").asText("build");
    if (!Set.of(
            "build",
            "script",
            "preview",
            "validate",
            "open",
            "edit",
            "transform",
            "save",
            "proguard",
            "stacktrace",
            "compareImport",
            "decompile",
            "bytecode",
            "trimReport",
            "inspect",
            "programRun")
        .contains(kind)) throw new IllegalArgumentException("Unknown job kind");
    if (body.has("projectRevision") && body.path("projectRevision").asLong() != project.revision)
      throw new ProjectStore.Conflict("Project changed; reload before running");
    if (busy() && Set.of("open", "edit", "transform", "save").contains(kind))
      throw new ProjectStore.Conflict("An operation is already modifying the project");
    Job job = new Job();
    job.projectId = project.id;
    job.kind = kind;
    if (!body.has("script") && Set.of("build", "preview", "inspect").contains(kind))
      job.configurationKey = body.path("configurationKey").asText("");
    job.directory = Path.of(project.directory, "runs", job.id).toString();
    Files.createDirectories(Path.of(job.directory));
    Json.write(Path.of(job.directory, "project.json"), project);
    Json.write(Path.of(job.directory, "request.json"), body);
    jobs.put(job.id, job);
    persist(job);
    emit("job", job);
    tasks.submit(() -> runJob(job, project, body));
    return job;
  }

  private void runJob(Job job, ProjectConfig project, JsonNode body) {
    Consumer<String> log =
        line -> {
          try {
            Files.writeString(
                Path.of(job.directory, "console.log"),
                line + System.lineSeparator(),
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND);
          } catch (IOException ignored) {
          }
          emit("log", Json.map("jobId", job.id, "text", line));
        };
    WorkerClient worker = null;
    boolean shared = false;
    try {
      synchronized (job) {
        if (job.status.equals("cancelled")) return;
        job.status = "running";
      }
      persist(job);
      emit("job", job);
      ObjectNode request = body.deepCopy();
      request.remove("kind");
      request.put("runDirectory", job.directory);
      String working = BuildScripts.path(project, job.kind.equals("programRun")
          ? body.path("workingDirectory").asText(project.workingDirectory) : project.workingDirectory);
      request.put("workingDirectory", working);
      String kind = job.kind;
      if (kind.equals("decompile") || kind.equals("bytecode")) {
        Job sourceJob = job(body.path("jobId").asText());
        if (!sourceJob.status.equals("succeeded"))
          throw new IllegalArgumentException("Select a successful run");
        JsonNode artifact = Json.tree(sourceJob.result);
        String side = body.path("side").asText();
        if (!Set.of("before", "after").contains(side))
          throw new IllegalArgumentException("Invalid comparison side");
        Path root = Path.of(artifact.path(side + "Root").asText());
        ClassIdentity target =
            Comparisons.read(root).stream()
                .filter(c -> c.id.equals(body.path("classId").asText()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Class not found"));
        List<String> libraries = new ArrayList<>();
        Path groups = root.resolve("classes");
        if (Files.isDirectory(groups))
          try (var dirs = Files.list(groups)) {
            dirs.filter(Files::isDirectory).forEach(p -> libraries.add(p.toString()));
          }
        ProjectConfig original =
            Json.read(Path.of(sourceJob.directory, "project.json"), ProjectConfig.class);
        original.directory = Path.of(sourceJob.directory).getParent().getParent().toString();
        original.build.classpath.forEach(p -> libraries.add(BuildScripts.path(original, p)));
        Path cache = projects.home.resolve("decompiled");
        Files.createDirectories(cache);
        List<Path> fingerprintRoots = libraries.stream().map(Path::of).toList();
        String key =
            FileFingerprint.of(
                kind
                    + body.path("engine").asText("vf")
                    + "-vf1.12.0-cfr0.152-java"
                    + System.getProperty("java.version")
                    + "-"
                    + target.id,
                fingerprintRoots);
        Path cached = cache.resolve(key + ".json");
        if (Files.exists(cached)) {
          job.result = Json.read(cached, Object.class);
          synchronized (job) {
            if (!job.status.equals("cancelled")) job.status = "succeeded";
          }
          return;
        }
        request.put("operation", kind.equals("bytecode") ? "bytecodeFile" : "decompile");
        request.put("file", target.file);
        request.put("cacheDirectory", cache.toString());
        request.set("libraries", Json.tree(libraries));
        worker = new WorkerClient(Path.of(job.directory), Path.of(working), log);
        register(job, worker);
        job.result =
            worker.call(request, log, Math.max(1, Math.min(600, body.path("timeout").asInt(60))));
        if (!job.status.equals("cancelled")) Json.write(cached, job.result);
      } else {
        if (kind.equals("open")) {
          resetSession();
          shared = true;
          worker = createSession(project, log, job);
          job.result = worker.call(Json.map("operation", "classes"), log, 60);
        } else if (Set.of("edit", "transform", "save").contains(kind)) {
          shared = true;
          worker = session(job, log);
          register(job, worker);
          if (kind.equals("edit")) {
            request.put("operation", "edit");
            request.put("kind", body.path("editKind").asText());
          } else {
            request.put("operation", "statements");
            String script = body.path("script").asText();
            if (kind.equals("save"))
              script =
                  "saveAll "
                      + BuildScripts.parameters(project.build.saveOptions)
                      + " "
                      + BuildScripts.quote(BuildScripts.path(project, project.build.output))
                      + ";";
            request.put("script", script);
          }
          job.result = worker.call(request, log, 0);
          if (!kind.equals("save") && !job.status.equals("cancelled")) {
            Path checkpoint = Path.of(project.directory, "work", "checkpoint-" + job.id);
            worker.call(Json.map("operation", "checkpoint", "path", checkpoint.toString()), log, 0);
            synchronized (projects) {
              ProjectConfig current = projects.active();
              if (current.id.equals(project.id)) {
                current.checkpointInputs =
                    checkpointInputs(checkpoint).stream()
                        .map(x -> projects.portable(Path.of(x)))
                        .toList();
                projects.persist();
                sessionConfig = sessionSignature(current);
              }
            }
          }
        } else {
          if (Set.of("build", "preview", "script", "validate", "inspect").contains(kind)) {
            boolean explicit = body.has("script");
            if (!explicit && !project.checkpointInputs.isEmpty())
              project.build.inputs = project.checkpointInputs;
            String script =
                explicit ? body.path("script").asText() : BuildScripts.generate(project, kind.equals("inspect") ? "open" : "build");
            Files.writeString(Path.of(job.directory, "script.zkm"), script);
            request.put("operation", kind.equals("validate") ? "validate" : "script");
            request.put("script", script);
            request.put(
                "sourcePath",
                body.hasNonNull("sourcePath") && !body.path("sourcePath").asText().isBlank()
                    ? BuildScripts.path(project, body.path("sourcePath").asText())
                    : Path.of(project.directory, "scripts", "unsaved.zkm").toString());
            request.put("preview", kind.equals("preview"));
          } else if (kind.equals("trimReport")) {
            request.put("operation", "trimReport");
            request.put(
                "script", BuildScripts.generate(project, "open") + "\n" + project.build.trimRules);
            request.put("trimStatement", BuildScripts.statement("trim", project.build.trimOptions));
          } else request.put("operation", kind);
          if (kind.equals("programRun")) {
            var classpath = Json.MAPPER.createArrayNode();
            for (JsonNode path : body.path("classpath")) classpath.add(BuildScripts.path(project, path.asText()));
            request.set("classpath", classpath);
          }
          if (kind.equals("stacktrace") || kind.equals("compareImport")) {
            var paths = Json.MAPPER.createArrayNode();
            body.path("changeLogs").forEach(v -> paths.add(BuildScripts.path(project, v.asText())));
            request.set("changeLogs", paths);
            if (kind.equals("stacktrace")) {
              List<String> bytecodePaths = new ArrayList<>();
              if (body.path("classpath").isArray())
                body.path("classpath").forEach(v -> bytecodePaths.add(v.asText()));
              else bytecodePaths.addAll(project.build.classpath);
              request.put(
                  "classpath",
                  String.join(
                      File.pathSeparator,
                      bytecodePaths.stream().map(v -> BuildScripts.path(project, v)).toList()));
            }
          }
          if (kind.equals("compareImport")) {
            request.put("before", BuildScripts.path(project, body.path("before").asText()));
            request.put("after", BuildScripts.path(project, body.path("after").asText()));
          }
          worker = new WorkerClient(Path.of(job.directory), Path.of(working), log);
          register(job, worker);
          if (job.status.equals("cancelled")) {
            worker.close();
            return;
          }
          job.result = worker.call(request, log, 0);
        }
      }
      synchronized (job) {
        if (!job.status.equals("cancelled")) job.status = "succeeded";
      }
    } catch (Throwable error) {
      if (!job.status.equals("cancelled")) {
        job.status = "failed";
        job.error = error.getMessage() == null ? error.toString() : error.getMessage();
        job.result =
            error instanceof WorkerClient.WorkerFailure w ? w.details : Errors.describe(error);
        log.accept(job.error);
      }
      if (shared) resetSession();
    } finally {
      workers.remove(job.id);
      if (worker != null) worker.onProgress(null);
      if (job.status.equals("succeeded")) job.progress = 100;
      if (worker != null && !shared) worker.close();
      job.finished = Instant.now().toString();
      try {
        persist(job);
      } catch (IOException ignored) {
      }
      emit("job", job);
    }
  }

  private List<String> checkpointInputs(Path root) throws IOException {
    List<String> inputs = new ArrayList<>();
    boolean classes = false;
    try (var paths = Files.walk(root)) {
      for (Path p : paths.filter(Files::isRegularFile).toList()) {
        String n = p.toString().toLowerCase(Locale.ROOT);
        if (n.endsWith(".jar") || n.endsWith(".zip") || n.endsWith(".war") || n.endsWith(".ear"))
          inputs.add(p.toString());
        if (n.endsWith(".class")) classes = true;
      }
    }
    if (classes) inputs.add(root.toString());
    return inputs;
  }

  private static String sessionSignature(ProjectConfig p) throws IOException {
    return Json.line(
        Json.map(
            "project",
            p.id,
            "inputs",
            p.build.inputs,
            "classpath",
            p.build.classpath,
            "nested",
            p.build.openNestedArchives,
            "skipped",
            p.build.skipped,
            "restored",
            p.build.restored,
            "checkpoint",
            p.checkpointInputs,
            "cwd",
            p.workingDirectory));
  }

  private void register(Job job, WorkerClient worker) throws IOException {
    synchronized (job) {
      if (job.status.equals("cancelled")) {
        worker.close();
        throw new IOException("Task cancelled");
      }
      worker.onProgress(
          value -> {
            job.progress = value.path("progress").asInt();
            job.phase = value.path("phase").asText();
            emit(
                "progress",
                Json.map("jobId", job.id, "progress", job.progress, "phase", job.phase));
          });
      workers.put(job.id, worker);
    }
  }

  private WorkerClient session(Job job, Consumer<String> log) throws Exception {
    synchronized (sessionLock) {
      ProjectConfig p = projects.active();
      if (session == null || !session.alive() || !sessionConfig.equals(sessionSignature(p)))
        return createSession(p, log, job);
      if (job != null) register(job, session);
      return session;
    }
  }

  private WorkerClient createSession(ProjectConfig original, Consumer<String> log, Job job)
      throws Exception {
    synchronized (sessionLock) {
      resetSession();
      ProjectConfig p = Json.MAPPER.convertValue(original, ProjectConfig.class);
      sessionConfig = sessionSignature(p);
      if (!p.checkpointInputs.isEmpty()) p.build.inputs = p.checkpointInputs;
      Path root = Path.of(p.directory, "work", "session-" + UUID.randomUUID());
      session = new WorkerClient(root, Path.of(BuildScripts.path(p, p.workingDirectory)), log);
      sessionProject = p.id;
      try {
        if (job != null) register(job, session);
        session.call(
            Json.map(
                "operation",
                "open",
                "script",
                BuildScripts.generate(p, "open"),
                "workingDirectory",
                BuildScripts.path(p, p.workingDirectory),
                "runDirectory",
                root.toString()),
            log,
            job == null ? 60 : 0);
        return session;
      } catch (Exception e) {
        resetSession();
        throw e;
      }
    }
  }

  private void resetSession() {
    synchronized (sessionLock) {
      if (session != null) session.close();
      session = null;
      sessionProject = "";
    }
  }

  private Job job(String id) throws FileNotFoundException {
    Job j = jobs.get(id);
    if (j == null) throw new FileNotFoundException("Run not found");
    return j;
  }

  private List<Job> history() {
    String id;
    try {
      id = projects.active().id;
    } catch (IllegalStateException e) {
      return List.of();
    }
    String project = id;
    return jobs.values().stream()
        .filter(j -> Objects.equals(j.projectId, project))
        .sorted(Comparator.comparing((Job j) -> j.created).reversed())
        .toList();
  }

  private void loadHistory() throws IOException {
    Path runs = Path.of(projects.active().directory, "runs");
    if (!Files.isDirectory(runs)) return;
    try (var paths = Files.list(runs)) {
      for (Path p : paths.toList()) {
        Path f = p.resolve("job.json");
        if (Files.exists(f)) {
          Job j = Json.read(f, Job.class);
          Job existing = jobs.get(j.id);
          if (existing != null
              && (existing.directory.equals(p.toString())
                  || Set.of("queued", "running").contains(existing.status))) continue;
          jobs.remove(j.id);
          j.directory = p.toString();
          transformResultPaths(j, false);
          if (Set.of("queued", "running").contains(j.status)) {
            j.status = "failed";
            j.error = "Application stopped before this task completed";
            persist(j);
          }
          jobs.putIfAbsent(j.id, j);
        }
      }
    }
  }

  private void persist(Job job) throws IOException {
    Job stored = Json.MAPPER.convertValue(job, Job.class);
    transformResultPaths(stored, true);
    stored.directory = "runs/" + job.id;
    Json.write(Path.of(job.directory, "job.json"), stored);
  }

  private static void transformResultPaths(Job job, boolean storing) {
    if (!(Json.tree(job.result) instanceof ObjectNode result)) return;
    Path run = Path.of(job.directory).toAbsolutePath(), project = run.getParent().getParent();
    java.util.function.Function<String, String> convert =
        value -> {
          Path p = Path.of(value);
          if (storing)
            return p.isAbsolute() && p.startsWith(project)
                ? run.relativize(p).toString().replace('\\', '/')
                : value;
          return p.isAbsolute() ? value : run.resolve(p).normalize().toString();
        };
    for (String name : List.of("beforeRoot", "afterRoot"))
      if (result.hasNonNull(name)) result.put(name, convert.apply(result.path(name).asText()));
    for (String name : List.of("outputs", "changeLogs"))
      if (result.path(name).isArray()) {
        var values = Json.MAPPER.createArrayNode();
        result.path(name).forEach(v -> values.add(convert.apply(v.asText())));
        result.set(name, values);
      }
    job.result = result;
  }

  private void emit(String type, Object value) {
    try {
      synchronized (events) {
        events.add(
            new Event(eventId.incrementAndGet(), Json.line(Json.map("type", type, "data", value))));
        if (events.size() > 2000) events.remove(0);
        events.notifyAll();
      }
    } catch (IOException ignored) {
    }
  }

  private void stream(HttpExchange e) throws IOException {
    e.getResponseHeaders().set("Content-Type", "text/event-stream; charset=utf-8");
    e.sendResponseHeaders(200, 0);
    long cursor = 0;
    String last = e.getRequestHeaders().getFirst("Last-Event-ID");
    if (last != null)
      try {
        cursor = Long.parseLong(last);
      } catch (NumberFormatException ignored) {
      }
    if (cursor > eventId.get()) cursor = 0;
    try (OutputStream out = e.getResponseBody()) {
      out.write(": connected\n\n".getBytes(StandardCharsets.UTF_8));
      out.flush();
      while (!stopped) {
        List<Event> batch;
        synchronized (events) {
          long c = cursor;
          batch = events.stream().filter(v -> v.id > c).toList();
          if (batch.isEmpty())
            try {
              events.wait(10000);
            } catch (InterruptedException x) {
              Thread.currentThread().interrupt();
              break;
            }
        }
        if (batch.isEmpty()) out.write(": heartbeat\n\n".getBytes(StandardCharsets.UTF_8));
        else
          for (Event event : batch) {
            out.write(
                ("id: " + event.id + "\ndata: " + event.json + "\n\n")
                    .getBytes(StandardCharsets.UTF_8));
            cursor = event.id;
          }
        out.flush();
      }
    } catch (IOException ignored) {
    }
  }

  private static Map<String, String> query(URI uri) {
    Map<String, String> map = new HashMap<>();
    if (uri.getRawQuery() != null)
      for (String part : uri.getRawQuery().split("&")) {
        String[] pair = part.split("=", 2);
        map.put(
            URLDecoder.decode(pair[0], StandardCharsets.UTF_8),
            URLDecoder.decode(pair.length > 1 ? pair[1] : "", StandardCharsets.UTF_8));
      }
    return map;
  }

  private static void send(HttpExchange e, int status, Object value) throws IOException {
    if (e.getResponseCode() != -1) return;
    byte[] bytes = Json.MAPPER.writeValueAsBytes(value);
    e.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
    e.sendResponseHeaders(status, bytes.length);
    try (OutputStream out = e.getResponseBody()) {
      out.write(bytes);
    }
  }

  private void staticFile(HttpExchange e, String raw) throws IOException {
    if (!e.getRequestMethod().equals("GET") && !e.getRequestMethod().equals("HEAD")) {
      send(e, 405, Json.map("message", "GET required"));
      return;
    }
    if (raw.contains("..") || raw.contains("\\")) {
      send(e, 404, Json.map("message", "Not found"));
      return;
    }
    String path = raw.equals("/") ? "/index.html" : raw;
    boolean help = path.startsWith("/help/");
    String resource = help ? "/com/zelix/" + path.substring(6) : "/web" + path;
    InputStream stream = WebServer.class.getResourceAsStream(resource);
    if (stream == null && !path.contains("."))
      stream = WebServer.class.getResourceAsStream("/web/index.html");
    if (stream == null) {
      send(
          e,
          404,
          Json.map("message", "Web resources missing. Build the application with gradlew jar."));
      return;
    }
    String type =
        path.endsWith(".txt")
            ? "text/plain"
            : path.endsWith(".json")
                ? "application/json"
                : path.endsWith(".js")
                    ? "text/javascript"
                    : path.endsWith(".css")
                        ? "text/css"
                        : path.endsWith(".svg")
                            ? "image/svg+xml"
                            : path.endsWith(".png")
                                ? "image/png"
                                : path.endsWith(".gif")
                                    ? "image/gif"
                                    : path.endsWith(".ttf") ? "font/ttf" : "text/html";
    e.getResponseHeaders()
        .set("Content-Type", type + ((type.startsWith("text/")) ? "; charset=utf-8" : ""));
    e.getResponseHeaders()
        .set(
            "Content-Security-Policy",
            "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src"
                + " 'self' data:; font-src 'self' data:; worker-src 'self' blob:; connect-src"
                + " 'self'; frame-ancestors 'none'");
    try (InputStream in = stream) {
      if (e.getRequestMethod().equals("HEAD")) {
        e.sendResponseHeaders(200, -1);
        return;
      }
      e.sendResponseHeaders(200, 0);
      try (OutputStream out = e.getResponseBody()) {
        in.transferTo(out);
      }
    }
  }

  @Override
  public void close() {
    if (stopped) return;
    stopped = true;
    desktop.close();
    workers.values().forEach(WorkerClient::close);
    resetSession();
    synchronized (events) {
      events.notifyAll();
    }
    http.stop(0);
    requests.shutdownNow();
    tasks.shutdownNow();
  }
}

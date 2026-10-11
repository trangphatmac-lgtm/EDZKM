package com.zelix.klassmaster.web;

import java.util.*;

public final class Models {
  public static class ProjectConfig {
    public int schemaVersion = 1;
    public String id = UUID.randomUUID().toString();
    public long revision = 0;
    public String name = "Untitled project", directory = "", workingDirectory = "";
    public BuildConfig build = new BuildConfig();
    public List<ScriptDocument> scripts = new ArrayList<>();
    public String activeScript = "";
    public List<Map<String, Object>> edits = new ArrayList<>();
    public List<String> checkpointInputs = new ArrayList<>();
  }

  public static class BuildConfig {
    public List<String> inputs = new ArrayList<>(),
        classpath = new ArrayList<>(),
        skipped = new ArrayList<>(),
        restored = new ArrayList<>();
    public boolean openNestedArchives = true, trim = false, obfuscate = true;
    public String output = "output",
        rules = "exclude *.* public static main(java.lang.String[]);",
        trimRules = "trimExclude *.* public static main(java.lang.String[]);";
    public Map<String, String> obfuscation =
        new LinkedHashMap<>(
            Map.of(
                "obfuscateFlow",
                "light",
                "encryptStringLiterals",
                "enhanced",
                "lineNumbers",
                "delete",
                "changeLogFileOut",
                "ChangeLog.txt"));
    public Map<String, String> trimOptions = new LinkedHashMap<>();
    public Map<String, String> saveOptions = new LinkedHashMap<>();
  }

  public static class ScriptDocument {
    public String id = UUID.randomUUID().toString(),
        name = "script.zkm",
        path = "",
        content = "",
        savedContent = "";
  }

  public static class Job {
    public String id = UUID.randomUUID().toString(),
        projectId,
        kind,
        created = java.time.Instant.now().toString(),
        directory;
    public volatile String status = "queued", finished = "", error = "";
    public volatile Object result;
    public volatile int progress;
    public volatile String phase = "";
    public String configurationKey = "";
  }

  public static class Artifact {
    public String path, name, kind;
    public long size;

    public Artifact() {}

    public Artifact(String path, String name, String kind, long size) {
      this.path = path;
      this.name = name;
      this.kind = kind;
      this.size = size;
    }
  }

  public static class ClassIdentity {
    public String id, name, origin, entry, version, file, originalName;
  }

  public static class Comparison {
    public String id, beforeRoot, afterRoot;
    public List<Map<String, Object>> classes = new ArrayList<>();
  }
}

import { describe, it, expect } from "vitest";
import { configurationKey, matchingRun } from "./preview";
import type { Project, Job } from "./types";
const project = (): Project => ({
  schemaVersion: 1,
  id: "project",
  revision: 1,
  name: "Test",
  directory: "/tmp/project",
  workingDirectory: "",
  checkpointInputs: [],
  scripts: [],
  activeScript: "",
  build: {
    inputs: ["input.jar"],
    classpath: [],
    skipped: [],
    restored: [],
    output: "output",
    rules: "",
    trimRules: "",
    trim: false,
    obfuscate: true,
    openNestedArchives: true,
    obfuscation: {},
    trimOptions: {},
    saveOptions: {},
  },
});
describe("configuration preview reuse", () => {
  it("keeps a result when only project metadata, revisions, scripts or path representation change", () => {
    const p = project(),
      other = structuredClone(p);
    other.revision++;
    other.name = "Renamed";
    other.activeScript = "different";
    other.workingDirectory = "/tmp/project";
    other.build.inputs = ["/tmp/project/input.jar"];
    other.build.output = "/tmp/project/output";
    expect(configurationKey(other)).toBe(configurationKey(p));
  });
  it("invalidates transformed code for configuration changes but retains original code", () => {
    const p = project(),
      other = structuredClone(p);
    other.build.obfuscation = { randomize: "false" };
    expect(configurationKey(other)).not.toBe(configurationKey(p));
    expect(configurationKey(other, true)).toBe(configurationKey(p, true));
    other.checkpointInputs = ["work/new.jar"];
    expect(configurationKey(other, true)).not.toBe(configurationKey(p, true));
  });
  it("reuses an in-flight or successful generated run, never a failed run or script", () => {
    const key = configurationKey(project());
    const jobs = [
      {
        id: "failed",
        kind: "preview",
        status: "failed",
        configurationKey: key,
      },
      {
        id: "script",
        kind: "script",
        status: "succeeded",
        configurationKey: key,
      },
      {
        id: "active",
        kind: "preview",
        status: "running",
        configurationKey: key,
      },
    ] as Job[];
    expect(matchingRun(jobs, key, false)?.id).toBe("active");
    expect(matchingRun(jobs, key, true)).toBeUndefined();
    expect(matchingRun(jobs, key + "new", false)).toBeUndefined();
  });
});

import type { Project, Job } from "./types";

const stable = (value: any): any =>
  Array.isArray(value)
    ? value.map(stable)
    : value && typeof value === "object"
      ? Object.fromEntries(
          Object.keys(value)
            .sort()
            .map((key) => [key, stable(value[key])]),
        )
      : value;

// Project saving makes internal paths relative. Compare both forms identically.
export function configurationKey(project: Project, original = false): string {
  const path = (value: string) => {
    const root = project.directory.replaceAll("\\", "/").replace(/\/$/, "");
    let normalized = value.replaceAll("\\", "/");
    if (!normalized.startsWith("/") && !/^[A-Za-z]:\//.test(normalized))
      normalized = root + "/" + normalized;
    const parts: string[] = [];
    normalized.split("/").forEach((part) => {
      if (part === "..") parts.pop();
      else if (part !== ".") parts.push(part);
    });
    return parts.join("/").replace(/\/$/, "");
  };
  const build = {
    ...project.build,
    inputs: project.build.inputs.map(path),
    classpath: project.build.classpath.map(path),
    skipped: project.build.skipped.map(path),
    restored: project.build.restored.map(path),
    output: path(project.build.output),
  };
  const content = original
    ? {
        inputs: build.inputs,
        classpath: build.classpath,
        skipped: build.skipped,
        restored: build.restored,
        openNestedArchives: build.openNestedArchives,
      }
    : build;
  return JSON.stringify(
    stable({
      projectId: project.id,
      build: content,
      workingDirectory: path(project.workingDirectory),
      checkpointInputs: project.checkpointInputs.map(path),
    }),
  );
}

export function matchingRun(jobs: Job[], key: string, original: boolean) {
  return jobs.find(
    (job) =>
      job.configurationKey === key &&
      (original
        ? job.kind === "inspect"
        : ["build", "preview"].includes(job.kind)) &&
      ["queued", "running", "succeeded"].includes(job.status),
  );
}

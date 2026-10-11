import type { Job } from "./types";
export function completedOutputs(jobs: Job[]) {
  return jobs
    .filter(
      (j) =>
        j.status === "succeeded" &&
        ["build", "preview", "script", "save"].includes(j.kind) &&
        j.result?.outputs?.length > 0,
    )
    .sort((a, b) => b.created.localeCompare(a.created));
}
export function runtimeCandidates(jobs: Job[]) {
  return jobs
    .filter(
      (j) =>
        j.status === "succeeded" &&
        j.result?.beforeRoot &&
        (j.kind === "compareImport" || completedOutputs([j]).length > 0),
    )
    .sort((a, b) => b.created.localeCompare(a.created));
}

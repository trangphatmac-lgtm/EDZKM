export type Lang = "en" | "zh";
export interface BuildConfig {
  inputs: string[];
  skipped: string[];
  restored: string[];
  classpath: string[];
  openNestedArchives: boolean;
  trim: boolean;
  obfuscate: boolean;
  output: string;
  rules: string;
  trimRules: string;
  obfuscation: Record<string, string>;
  trimOptions: Record<string, string>;
  saveOptions: Record<string, string>;
}
export interface ScriptDocument {
  id: string;
  name: string;
  path: string;
  content: string;
  savedContent?: string;
}
export interface Project {
  schemaVersion: number;
  id: string;
  revision: number;
  name: string;
  directory: string;
  workingDirectory: string;
  build: BuildConfig;
  scripts: ScriptDocument[];
  activeScript: string;
  checkpointInputs: string[];
}
export interface Job {
  id: string;
  kind: string;
  status: string;
  created: string;
  finished: string;
  directory: string;
  error: string;
  result: any;
  projectId: string;
  progress: number;
  phase: string;
  configurationKey?: string;
}
export interface State {
  project: Project | null;
  recent: string[];
  home: string;
  cwd: string;
  java: string;
  version: string;
  jobs: Job[];
}
export interface ClassIdentity {
  id: string;
  name: string;
  origin: string;
  entry: string;
  version: string;
  file: string;
  originalName: string;
}
export interface Pair {
  id: string;
  before: ClassIdentity | null;
  after: ClassIdentity | null;
  status: string;
}

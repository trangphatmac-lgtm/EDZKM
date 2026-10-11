import { createContext, useContext } from "react";
import type { Lang, Project, Job, State } from "./types";
export interface Workbench {
  lang: Lang;
  langMode: string;
  themeMode: string;
  setLangMode: (v: string) => void;
  setThemeMode: (v: string) => void;
  theme: string;
  t: (en: string, zh: string) => string;
  project: Project | null;
  setProject: (p: Project) => void;
  acceptSaved: (p: Project) => void;
  state: State;
  refresh: () => Promise<void>;
  save: () => Promise<Project>;
  jobs: Job[];
  run: (request: any, save?: boolean) => Promise<Job>;
  notify: (message: string, error?: boolean) => void;
  choose: (options: {
    directory?: boolean;
    start?: string;
    onSelect: (path: string) => void;
  }) => Promise<void>;
  navigate: (page: string) => void;
  activePage: string;
  browserRequest: { runId: string; serial: number } | null;
  showClasses: (runId: string) => void;
  error: (e: unknown) => void;
}
export const Context = createContext<Workbench>(null!);
export const useWorkbench = () => useContext(Context);

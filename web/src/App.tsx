import { useEffect, useRef, useState, lazy, Suspense } from "react";
import {
  Layers,
  LayoutDashboard,
  Workflow,
  Boxes,
  Code2,
  GitCompareArrows,
  Wrench,
  Settings,
  Sun,
  Moon,
  ChevronDown,
  ChevronUp,
  Terminal,
  CheckCircle2,
  AlertCircle,
  X,
  PanelBottom,
  Square,
  Power,
  FolderOpen,
} from "lucide-react";
import { configurationKey } from "./preview";
import { statusLabel } from "./labels";
import { Context, type Workbench } from "./context";
import { api, connect } from "./api";
import type { State, Project, Job, Lang } from "./types";
import { Button } from "./components";
import ProjectPage from "./ProjectPage";
const BuildPage = lazy(() => import("./BuildPage")),
  ScriptsPage = lazy(() => import("./ScriptsPage")),
  BrowserPage = lazy(() => import("./BrowserPage")),
  ComparePage = lazy(() => import("./ComparePage")),
  ToolsPage = lazy(() => import("./ToolsPage"));
const initial: State = {
  project: null,
  recent: [],
  home: "",
  cwd: "",
  java: "",
  version: "27",
  jobs: [],
};
export default function App() {
  const [state, setState] = useState<State>(initial),
    [project, setProjectRaw] = useState<Project | null>(null),
    [jobs, setJobs] = useState<Job[]>([]),
    [page, setPage] = useState("project"),
    [langMode, setLangMode] = useState(
      localStorage.getItem("zkm-lang") || "system",
    ),
    [themeMode, setThemeMode] = useState(
      localStorage.getItem("zkm-theme") || "system",
    ),
    [systemDark, setSystemDark] = useState(
      matchMedia("(prefers-color-scheme: dark)").matches,
    ),
    [booted, setBooted] = useState(false),
    [connected, setConnected] = useState(false),
    [failure, setFailure] = useState(""),
    [toast, setToast] = useState<{ text: string; error: boolean } | null>(null),
    [logs, setLogs] = useState<Record<string, string[]>>({}),
    [logJob, setLogJob] = useState(""),
    [consoleOpen, setConsoleOpen] = useState(false),
    [dirty, setDirty] = useState(false);
  const [browserRequest, setBrowserRequest] = useState<{
    runId: string;
    serial: number;
  } | null>(null);
  const visited = useRef({ projectId: project?.id, pages: new Set<string>() });
  if (visited.current.projectId !== project?.id)
    visited.current = { projectId: project?.id, pages: new Set() };
  visited.current.pages.add(page);
  const showClasses = (runId: string) => {
    setBrowserRequest({ runId, serial: Date.now() });
    setPage("browser");
  };
  const lang: Lang =
    langMode === "system"
      ? navigator.language.startsWith("zh")
        ? "zh"
        : "en"
      : (langMode as Lang);
  const theme =
    themeMode === "system" ? (systemDark ? "dark" : "light") : themeMode;
  const setLang = (value: Lang) => setLangMode(value),
    setTheme = (value: string) => setThemeMode(value);
  useEffect(() => {
    const media = matchMedia("(prefers-color-scheme: dark)");
    const changed = () => setSystemDark(media.matches);
    media.addEventListener("change", changed);
    return () => media.removeEventListener("change", changed);
  }, []);
  const pRef = useRef(project);
  pRef.current = project;
  const toastTimer = useRef<number>(0);
  const t = (en: string, zh: string) => (lang === "zh" ? zh : en);
  const notify = (text: string, error = false) => {
    setToast({ text, error });
    window.clearTimeout(toastTimer.current);
    toastTimer.current = window.setTimeout(
      () => setToast(null),
      error ? 10000 : 4000,
    );
  };
  const error = (e: unknown) =>
    notify(
      t("Operation failed: ", "操作失败：") +
        (e instanceof Error ? e.message : String(e)),
      true,
    );
  const nativePending = useRef(false);
  const choose: Workbench["choose"] = async (options) => {
    if (nativePending.current) return;
    nativePending.current = true;
    const projectId = pRef.current?.id;
    try {
      const result = await api("/desktop/pick", {
        directory: options.directory || false,
        start: options.start || pRef.current?.directory || state.home,
        language: lang,
      });
      if (result.path && pRef.current?.id === projectId)
        options.onSelect(result.path);
    } catch (e) {
      error(e);
    } finally {
      nativePending.current = false;
    }
  };
  const setProject = (p: Project) => {
    setProjectRaw(p);
    pRef.current = p;
    setDirty(true);
  };
  const acceptSaved = (p: Project) => {
    setProjectRaw(p);
    pRef.current = p;
    setDirty(false);
  };
  const refresh = async () => {
    const s = await api<State>("/state");
    setState(s);
    setProjectRaw(s.project);
    setJobs(s.jobs);
    setDirty(false);
  };
  const saves = useRef<Promise<unknown>>(Promise.resolve());
  const save = () => {
    const operation = saves.current
      .catch(() => {})
      .then(async () => {
        const draft = pRef.current;
        if (!draft)
          throw new Error(
            t("Create or open a project first", "请先创建或打开项目"),
          );
        const saved = await api<Project>("/project", draft);
        if (pRef.current?.id === saved.id) {
          const changed = pRef.current !== draft;
          const current = changed
            ? { ...pRef.current!, revision: saved.revision }
            : saved;
          pRef.current = current;
          setProjectRaw(current);
          setDirty(changed);
        }
        return saved;
      });
    saves.current = operation;
    return operation;
  };
  const run = async (request: any, saveFirst = false) => {
    const snapshot = saveFirst ? await save() : pRef.current;
    const job = await api<Job>("/jobs", {
      ...request,
      configurationKey:
        snapshot &&
        !Object.hasOwn(request, "script") &&
        ["build", "preview", "inspect"].includes(request.kind)
          ? configurationKey(snapshot, request.kind === "inspect")
          : undefined,
      projectRevision: snapshot?.revision,
    });
    setJobs((old) => [job, ...old.filter((j) => j.id !== job.id)]);
    if (!["decompile", "bytecode", "inspect"].includes(request.kind)) {
      setLogJob(job.id);
      setConsoleOpen(true);
    }
    return job;
  };
  useEffect(() => {
    document.documentElement.dataset.theme = theme;
    localStorage.setItem("zkm-theme", themeMode);
  }, [theme, themeMode]);
  useEffect(() => {
    document.documentElement.lang = lang === "zh" ? "zh-CN" : "en";
    localStorage.setItem("zkm-lang", langMode);
  }, [lang, langMode]);
  useEffect(() => {
    const beforeUnload = (e: BeforeUnloadEvent) => {
      if (dirty) {
        e.preventDefault();
        e.returnValue = "";
      }
    };
    window.addEventListener("beforeunload", beforeUnload);
    return () => window.removeEventListener("beforeunload", beforeUnload);
  }, [dirty]);
  useEffect(() => {
    let stream: EventSource;
    let closed = false;
    connect()
      .then((s) => {
        if (closed) return;
        setState(s);
        setProjectRaw(s.project);
        setJobs(s.jobs);
        setConnected(true);
        setBooted(true);
        stream = new EventSource("/api/v1/events");
        stream.onopen = () => {
          setConnected(true);
          api<Job[]>("/jobs").then(setJobs).catch(error);
        };
        stream.onerror = () => setConnected(false);
        stream.onmessage = (event) => {
          const message = JSON.parse(event.data);
          if (message.type === "job") {
            const j = message.data as Job;
            if (pRef.current && j.projectId !== pRef.current.id) return;
            setJobs((old) =>
              [j, ...old.filter((v) => v.id !== j.id)].sort((a, b) =>
                b.created.localeCompare(a.created),
              ),
            );
            if (j.status === "failed" && j.kind !== "decompile")
              notify(t("Task failed: ", "任务失败：") + j.error, true);
            if (
              j.status === "succeeded" &&
              ["edit", "transform"].includes(j.kind)
            )
              api<Project>("/project")
                .then((p) => {
                  setProjectRaw((current) =>
                    current?.id === p.id
                      ? {
                          ...current,
                          revision: p.revision,
                          checkpointInputs: p.checkpointInputs,
                        }
                      : current,
                  );
                })
                .catch(error);
          } else if (message.type === "progress") {
            setJobs((old) =>
              old.map((j) =>
                j.id === message.data.jobId
                  ? {
                      ...j,
                      progress: message.data.progress,
                      phase: message.data.phase,
                    }
                  : j,
              ),
            );
          } else if (message.type === "log") {
            const { jobId, text } = message.data;
            setLogs((old) => ({
              ...old,
              [jobId]: [...(old[jobId] || []), text].slice(-2000),
            }));
          }
        };
      })
      .catch((e) => setFailure(e.message));
    return () => {
      closed = true;
      stream?.close();
    };
  }, []);
  const selectedJob = jobs.find((j) => j.id === logJob) || jobs[0];
  const active = jobs.filter((j) => ["running", "queued"].includes(j.status));
  const entries = [
    ["project", "Project", "项目", LayoutDashboard],
    ["build", "Build Helper", "构建向导", Workflow],
    ["browser", "Class browser", "类浏览器", Boxes],
    ["scripts", "Script studio", "脚本工作室", Code2],
    ["compare", "Runtime output", "运行输出对比", GitCompareArrows],
    ["tools", "Tools", "工具", Wrench],
  ] as const;
  if (failure)
    return (
      <div className="connection-failure">
        <Layers size={40} />
        <h1>EDZKM Workbench</h1>
        <p>{failure}</p>
        <p>
          {t(
            "Open the full launch URL printed in your terminal to connect.",
            "请打开终端输出的完整启动链接以连接。",
          )}
        </p>
      </div>
    );
  if (!booted)
    return (
      <div className="loading-page" style={{ height: "100vh" }}>
        <span className="spinner" />
        {t("Connecting to EDZKM…", "正在连接 EDZKM…")}
      </div>
    );
  return (
    <Context.Provider
      value={{
        lang,
        theme,
        langMode,
        themeMode,
        setLangMode,
        setThemeMode,
        t,
        project,
        setProject,
        acceptSaved,
        state,
        refresh,
        save,
        jobs,
        run,
        notify,
        choose,
        navigate: setPage,
        activePage: page,
        browserRequest,
        showClasses,
        error,
      }}
    >
      <div className="app">
        <aside className="sidebar">
          <div className="brand">
            <div className="brand-mark">
              <Layers size={25} />
            </div>
            <span>
              EDZKM<strong>WORKBENCH</strong>
            </span>
            <small>27</small>
          </div>
          <button
            className="project-switch"
            onClick={() => {
              setPage("project");
            }}
          >
            <div className="project-icon">
              {project?.name.slice(0, 1).toUpperCase() || (
                <FolderOpen size={17} />
              )}
            </div>
            <span>
              {project?.name || t("No project", "未打开项目")}
              <small>
                {project
                  ? t("Local workspace", "本地工作区")
                  : t("Get started", "开始使用")}
              </small>
            </span>
            <ChevronDown size={14} />
          </button>
          <div className="nav-label">{t("WORKSPACE", "工作区")}</div>
          <nav>
            {entries.map(([id, en, zh, Icon]) => (
              <button
                key={id}
                className={page === id ? "active" : ""}
                disabled={id !== "project" && !project}
                onClick={() => setPage(id)}
              >
                <Icon size={18} />
                {t(en, zh)}
                {id === "scripts" && project && (
                  <small>{project.scripts.length}</small>
                )}
                {id === "build" && <span className="nav-dot" />}
              </button>
            ))}
          </nav>
          <div className="sidebar-bottom">
            <div className="local-card">
              <span className="pulse" />
              <span>
                {connected
                  ? t("Running locally", "本地运行中")
                  : t("Reconnecting…", "正在重新连接…")}
                <small>127.0.0.1 · JAVA {state.java.split(".")[0]}</small>
              </span>
            </div>
            <button
              className={
                "settings-link " + (page === "settings" ? "active" : "")
              }
              onClick={() => setPage("settings")}
            >
              <Settings size={18} />
              {t("Settings & help", "设置与帮助")}
            </button>
          </div>
        </aside>
        <div className="workspace">
          <header className="topbar">
            <div className="breadcrumbs">
              <span>{project?.name || "EDZKM"}</span>
              <span>/</span>
              <strong>
                {t(
                  entries.find((e) => e[0] === page)?.[1] || "Settings",
                  entries.find((e) => e[0] === page)?.[2] || "设置",
                )}
              </strong>
              {dirty && (
                <span
                  className="unsaved-dot"
                  title={t("Unsaved changes", "未保存的更改")}
                />
              )}
            </div>
            <div className="topbar-actions">
              <button
                className="language"
                onClick={() => setLang(lang === "zh" ? "en" : "zh")}
              >
                {lang === "zh" ? "EN" : "中文"}
              </button>
              <button
                title={t("Toggle theme", "切换主题")}
                onClick={() => setTheme(theme === "dark" ? "light" : "dark")}
              >
                {theme === "dark" ? <Sun size={18} /> : <Moon size={18} />}
              </button>
              <span className="toolbar-separator" />
              {project && (
                <button
                  className="save-project"
                  onClick={() =>
                    save()
                      .then(() => notify(t("Project saved", "项目已保存")))
                      .catch(error)
                  }
                >
                  {t("Save project", "保存项目")}
                </button>
              )}
              <button
                title={t("Quit application", "退出程序")}
                onClick={() => {
                  if (
                    confirm(
                      t(
                        "Stop all tasks and quit EDZKM?",
                        "停止全部任务并退出 EDZKM？",
                      ),
                    )
                  )
                    api("/shutdown", {})
                      .then(() => {
                        setConnected(false);
                        notify(t("Application stopped", "程序已退出"));
                      })
                      .catch(error);
                }}
              >
                <Power size={16} />
              </button>
            </div>
          </header>
          <div className="page-container">
            <Suspense
              fallback={
                <div className="loading-page">
                  <span className="spinner" />
                  {t("Loading workspace…", "正在加载工作区…")}
                </div>
              }
            >
              {[...visited.current.pages].map((id) => (
                <div
                  className="retained-page"
                  key={(project?.id || "welcome") + ":" + id}
                  hidden={page !== id}
                >
                  {id === "project" ? (
                    <ProjectPage />
                  ) : id === "build" ? (
                    <BuildPage />
                  ) : id === "scripts" ? (
                    <ScriptsPage />
                  ) : id === "browser" ? (
                    <BrowserPage />
                  ) : id === "compare" ? (
                    <ComparePage />
                  ) : (
                    <ToolsPage settings={id === "settings"} />
                  )}
                </div>
              ))}
            </Suspense>
          </div>
          <div className={"console " + (consoleOpen ? "open" : "")}>
            <div className="console-bar">
              <button onClick={() => setConsoleOpen(!consoleOpen)}>
                <Terminal size={15} />
                {t("Output", "输出")}
                {active.length > 0 && (
                  <span className="badge accent">
                    {active.length} {t("running", "运行中")}
                  </span>
                )}
              </button>
              {consoleOpen && (
                <select
                  value={selectedJob?.id || ""}
                  onChange={(e) => {
                    setLogJob(e.target.value);
                    const j = jobs.find((j) => j.id === e.target.value);
                    if (j && !logs[j.id])
                      api(
                        "/files/read?path=" +
                          encodeURIComponent(j.directory + "/console.log"),
                      )
                        .then((r) =>
                          setLogs((old) => ({
                            ...old,
                            [j.id]: r.content.split("\n"),
                          })),
                        )
                        .catch(error);
                  }}
                >
                  {jobs.slice(0, 100).map((j) => (
                    <option key={j.id} value={j.id}>
                      {statusLabel(j.kind, t)} · {statusLabel(j.status, t)} ·{" "}
                      {j.id.slice(0, 8)}
                    </option>
                  ))}
                </select>
              )}
              {selectedJob?.status === "running" && (
                <span className="task-progress">
                  {statusLabel(selectedJob.phase, t)}{" "}
                  <progress max={100} value={selectedJob.progress || 0} />
                </span>
              )}
              <div className="console-spacer" />
              {selectedJob &&
                ["queued", "running"].includes(selectedJob.status) && (
                  <button
                    onClick={() =>
                      api("/jobs/" + selectedJob.id + "/cancel", {}).catch(
                        error,
                      )
                    }
                  >
                    <Square size={12} />
                    {t("Cancel task", "取消任务")}
                  </button>
                )}
              <button
                aria-label={t("Toggle output", "切换输出面板")}
                onClick={() => setConsoleOpen(!consoleOpen)}
              >
                {consoleOpen ? (
                  <ChevronDown size={16} />
                ) : (
                  <ChevronUp size={16} />
                )}
              </button>
            </div>
            {consoleOpen && (
              <pre className="console-output">
                {selectedJob
                  ? (
                      logs[selectedJob.id] || [
                        selectedJob.error ||
                          t("Waiting for output…", "等待输出…"),
                      ]
                    ).join("\n")
                  : t(
                      "Ready. Start a build or run a script to see output here.",
                      "已就绪。开始构建或运行脚本，在这里查看输出。",
                    )}
              </pre>
            )}
          </div>
          <footer className="statusbar">
            <span>
              <i className="status-led" />
              {connected
                ? t("Engine ready", "引擎就绪")
                : t("Disconnected", "连接已断开")}
            </span>
            <span>
              {t("Local processing", "本地处理")}
              <span>UTF-8</span>
              <span>EDZKM 27</span>
            </span>
          </footer>
        </div>{" "}
        {toast && (
          <div
            className={"toast " + (toast.error ? "error" : "")}
            role="status"
          >
            {toast.error ? (
              <AlertCircle size={19} />
            ) : (
              <CheckCircle2 size={19} />
            )}
            <span>{toast.text}</span>
            <button onClick={() => setToast(null)}>
              <X size={16} />
            </button>
          </div>
        )}
      </div>
    </Context.Provider>
  );
}

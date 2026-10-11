import { useState } from "react";
import {
  ArrowRight,
  FolderOpen,
  Plus,
  Layers,
  Workflow,
  Code2,
  GitCompareArrows,
  FileArchive,
  Play,
  ExternalLink,
} from "lucide-react";
import { useWorkbench } from "./context";
import { statusLabel } from "./labels";
import { api, fileUrl } from "./api";
import { Button, Field, Modal, PathField, Paths, Section } from "./components";
import ProjectActions from "./ProjectActions";
import type { Project } from "./types";
export default function ProjectPage() {
  const {
    project,
    setProject,
    state,
    t,
    refresh,
    error,
    navigate,
    jobs,
    choose,
    save,
    notify,
  } = useWorkbench();
  const [creating, setCreating] = useState(false),
    [directory, setDirectory] = useState(""),
    [name, setName] = useState(""),
    [artifacts, setArtifacts] = useState<any>(null);
  const open = async (path: string) => {
    if (project) await save();
    await api("/projects/open", { directory: path });
    await refresh();
  };
  const create = async () => {
    if (project) await save();
    await api("/projects/create", { directory, name });
    setCreating(false);
    await refresh();
  };
  const cards = [
    {
      icon: <Workflow size={22} />,
      title: "Build Helper",
      zh: "构建向导",
      detail: t(
        "From input to protected output, step by step.",
        "从输入到混淆产物，逐步完成配置。",
      ),
      page: "build",
    },
    {
      icon: <Code2 size={22} />,
      title: "Script studio",
      zh: "脚本工作室",
      detail: t(
        "Write, validate and run ZKM scripts.",
        "编写、校验和运行 ZKM Script。",
      ),
      page: "scripts",
    },
    {
      icon: <GitCompareArrows size={22} />,
      title: "Inspect the difference",
      zh: "查看混淆效果",
      detail: t(
        "Compare before and after with two decompilers.",
        "通过两种反编译器对比处理前后。",
      ),
      page: "browser",
    },
  ];
  return (
    <div className="page project-page">
      <div className="hero">
        <div className="eyebrow">
          <span className="pulse" />
          LOCAL JAVA WORKBENCH
        </div>
        <h1>
          {project
            ? t("Your next build,", "下一次构建，")
            : t("Make your code", "让你的代码")}
          <br />
          <span>
            {project
              ? t("under your control.", "尽在掌握。")
              : t("harder to unravel.", "更难被解读。")}
          </span>
        </h1>
        <p>
          {t(
            "Safeguarding source code and copyright in the JVM world, keeping the light of ingenuity alive.",
            "守护JVM世界的源代码与版权，让智慧的光芒永不熄灭。",
          )}
        </p>
        <div className="hero-actions">
          <Button
            primary
            onClick={() =>
              project
                ? navigate("build")
                : (setDirectory(state.home + "/ZKM-projects/untitled"),
                  setCreating(true))
            }
          >
            {project ? <Play size={16} /> : <Plus size={16} />}{" "}
            {project
              ? t("Configure a build", "配置构建")
              : t("Create project", "新建项目")}
          </Button>
          <Button
            onClick={() =>
              choose({
                directory: true,
                onSelect: (path) => open(path).catch(error),
              })
            }
          >
            <FolderOpen size={16} />
            {t("Open project", "打开项目")}
          </Button>
          {project && (
            <Button
              onClick={() => {
                setDirectory(state.home + "/ZKM-projects/untitled");
                setCreating(true);
              }}
            >
              <Plus size={15} />
              {t("New project", "新建项目")}
            </Button>
          )}
        </div>
        <div className="hero-art" aria-hidden="true">
          <div />
          <div />
          <div />
          <Layers size={75} strokeWidth={1} />
          <span>EDZKM / 27</span>
        </div>
      </div>
      <div className="quick-cards">
        {cards.map((card) => (
          <button
            className="quick-card"
            key={card.page}
            disabled={!project}
            onClick={() => navigate(card.page)}
          >
            <div className="card-symbol">{card.icon}</div>
            <h3>
              {t(card.title, card.zh)}
              <ArrowRight size={16} />
            </h3>
            <p>{card.detail}</p>
          </button>
        ))}
      </div>
      {project ? (
        <>
          <Section
            title={t("Project configuration", "项目配置")}
            detail={project.directory}
            action={
              <Button
                onClick={() =>
                  save()
                    .then(() => notify(t("Project saved", "项目已保存")))
                    .catch(error)
                }
              >
                {t("Save project", "保存项目")}
              </Button>
            }
          >
            <ProjectActions />
            <div className="form-grid">
              <Field label={t("Project name", "项目名称")}>
                <input
                  value={project.name}
                  onChange={(e) =>
                    setProject({ ...project, name: e.target.value })
                  }
                />
              </Field>
              <Field label={t("Working directory", "工作目录")}>
                <PathField
                  directory
                  value={project.workingDirectory}
                  onChange={(workingDirectory) =>
                    setProject({ ...project, workingDirectory })
                  }
                />
              </Field>
            </div>
            <div className="two-col">
              <Field label={t("Input classes & archives", "输入类与归档")}>
                <Paths
                  values={project.build.inputs}
                  onChange={(inputs) =>
                    setProject({
                      ...project,
                      checkpointInputs: [],
                      build: { ...project.build, inputs },
                    })
                  }
                />
              </Field>
              <Field label={t("Classpath dependencies", "Classpath 依赖")}>
                <Paths
                  values={project.build.classpath}
                  onChange={(classpath) =>
                    setProject({
                      ...project,
                      checkpointInputs: [],
                      build: { ...project.build, classpath },
                    })
                  }
                />
              </Field>
            </div>
            {project.checkpointInputs.length > 0 && (
              <div className="notice">
                {t(
                  "A saved working copy includes your class edits and transformations.",
                  "保存的工作副本包含你的类编辑和转换结果。",
                )}
                <Button
                  onClick={() =>
                    setProject({ ...project, checkpointInputs: [] })
                  }
                >
                  {t("Use original inputs", "使用原始输入")}
                </Button>
              </div>
            )}
          </Section>
          <Section
            title={t("Run history", "运行历史")}
            detail={t(
              "Every run keeps its configuration, script and logs.",
              "每次运行均保存配置、脚本和日志。",
            )}
          >
            <div className="run-table">
              {jobs
                .filter((j) => !["decompile", "validate"].includes(j.kind))
                .slice(0, 20)
                .map((j) => (
                  <div className="run-row" key={j.id}>
                    <span className={"status-dot " + j.status} />
                    <div>
                      <strong>{statusLabel(j.kind, t)}</strong>
                      <small>
                        {new Date(j.created).toLocaleString()} ·{" "}
                        {j.id.slice(0, 8)}
                      </small>
                    </div>
                    <span className={"badge " + j.status}>
                      {statusLabel(j.status, t)}
                    </span>
                    <Button
                      onClick={() =>
                        api("/artifacts?jobId=" + j.id)
                          .then(setArtifacts)
                          .catch(error)
                      }
                    >
                      <FileArchive size={14} />
                      {t("Artifacts", "产物")}
                    </Button>
                  </div>
                ))}
              {!jobs.length && (
                <p className="muted">
                  {t("Your first run starts here.", "从这里开始第一次运行。")}
                </p>
              )}
            </div>
          </Section>
        </>
      ) : (
        <Section title={t("Recent projects", "最近项目")}>
          {state.recent.length ? (
            state.recent.map((path) => (
              <button
                className="recent"
                key={path}
                onClick={() => open(path).catch(error)}
              >
                <FolderOpen size={20} />
                <span>
                  {path.split(/[\\/]/).pop()}
                  <small>{path}</small>
                </span>
                <ArrowRight size={17} />
              </button>
            ))
          ) : (
            <p className="muted">
              {t(
                "Create a project to keep inputs, scripts and run history together.",
                "创建项目，统一保存输入配置、脚本和运行历史。",
              )}
            </p>
          )}
        </Section>
      )}
      {creating && (
        <Modal
          title={t("Create project", "新建项目")}
          onClose={() => setCreating(false)}
        >
          <div className="modal-body">
            <Field label={t("Name", "名称")}>
              <input
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="My application"
              />
            </Field>
            <Field label={t("Project directory", "项目目录")}>
              <PathField directory value={directory} onChange={setDirectory} />
            </Field>
          </div>
          <footer>
            <Button onClick={() => setCreating(false)}>
              {t("Cancel", "取消")}
            </Button>
            <Button primary onClick={() => create().catch(error)}>
              {t("Create", "创建")}
            </Button>
          </footer>
        </Modal>
      )}
      {artifacts && (
        <Modal
          title={t("Run artifacts", "运行产物")}
          onClose={() => setArtifacts(null)}
        >
          <div className="artifact-list">
            {artifacts.outputs?.map((p: string) => (
              <p className="notice" key={p}>
                {p}
              </p>
            ))}
            {artifacts.files.map((f: any) => (
              <a key={f.path} href={fileUrl(f.path)} download>
                <FileArchive size={16} />
                <span>
                  {f.name}
                  <small>{f.path}</small>
                </span>
                <ExternalLink size={14} />
              </a>
            ))}
          </div>
        </Modal>
      )}
    </div>
  );
}

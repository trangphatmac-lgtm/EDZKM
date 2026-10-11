import { useState } from "react";
import {
  Languages,
  ArrowRight,
  Code2,
  Activity,
  BookOpen,
  ExternalLink,
} from "lucide-react";
import { useWorkbench } from "./context";
import { Button, Field, Paths, Section, Modal, PathField } from "./components";
import { Editor } from "./Editor";
import { api } from "./api";
export default function ToolsPage({
  settings = false,
}: {
  settings?: boolean;
}) {
  const {
    t,
    theme,
    project,
    setProject,
    run,
    jobs,
    error,
    notify,
    navigate,
    choose,
    state,
    langMode,
    themeMode,
    setLangMode,
    setThemeMode,
  } = useWorkbench();
  const [tab, setTab] = useState("stacktrace"),
    [input, setInput] = useState(""),
    [logs, setLogs] = useState<string[]>([]),
    [analyze, setAnalyze] = useState(false),
    [id, setId] = useState(""),
    [saving, setSaving] = useState(false),
    [savePath, setSavePath] = useState("scripts/translated.zkm"),
    [bytecodePaths, setBytecodePaths] = useState<string[]>([]),
    [memory, setMemory] = useState<any>(null);
  const j = jobs.find((j) => j.id === id);
  const result = j?.status === "succeeded" ? j.result.text : "";
  const start = async () => {
    const job = await run(
      {
        kind: tab,
        text: input,
        changeLogs: logs,
        analyzeBytecode: analyze,
        classpath: bytecodePaths.length
          ? bytecodePaths
          : project?.build.classpath || [],
      },
      true,
    );
    setId(job.id);
  };
  const toScript = () => {
    if (!project) return;
    const doc = {
      id: crypto.randomUUID(),
      name: "translated.zkm",
      path: "scripts/translated.zkm",
      content: result,
    };
    setProject({
      ...project,
      scripts: [...project.scripts, doc],
      activeScript: doc.id,
    });
    navigate("scripts");
  };
  if (settings)
    return (
      <div className="page">
        <div className="page-heading">
          <div className="eyebrow">WORKBENCH / SETTINGS</div>
          <h1>{t("Your workspace", "你的工作台")}</h1>
          <p>
            {t("Local by design. Ready when you are.", "本地运行，随时就绪。")}
          </p>
        </div>
        <Section title={t("Appearance & language", "外观与语言")}>
          <div className="form-grid">
            <Field label={t("Theme", "主题")}>
              <select
                value={themeMode}
                onChange={(e) => setThemeMode(e.target.value)}
              >
                <option value="system">{t("Follow system", "跟随系统")}</option>
                <option value="light">{t("Light", "浅色")}</option>
                <option value="dark">{t("Dark", "深色")}</option>
              </select>
            </Field>
            <Field label={t("Language", "语言")}>
              <select
                value={langMode}
                onChange={(e) => setLangMode(e.target.value)}
              >
                <option value="system">
                  {t("Follow browser", "跟随浏览器")}
                </option>
                <option value="zh">简体中文</option>
                <option value="en">English</option>
              </select>
            </Field>
          </div>
        </Section>
        <Section title={t("Runtime", "运行环境")}>
          <div className="stat-grid">
            <div>
              <small>Java</small>
              <strong>{state.java}</strong>
            </div>
            <div>
              <small>Zelix KlassMaster</small>
              <strong>27.0.0</strong>
            </div>
            <div>
              <small>{t("Decompiler engines", "反编译引擎")}</small>
              <strong>CFR + VF</strong>
            </div>
          </div>
          <Button
            onClick={() =>
              api("/engine", { operation: "gc" }).then(setMemory).catch(error)
            }
          >
            <Activity size={16} />
            {t("Collect garbage", "执行垃圾回收")}
          </Button>
          {memory && (
            <p className="notice">
              {t("Workbench memory", "工作台内存")} ·{" "}
              {(memory.before / 1048576).toFixed(1)} MB →{" "}
              {(memory.after / 1048576).toFixed(1)} MB
            </p>
          )}
        </Section>
        <Section
          title={t("Alternative interface", "备选界面")}
          detail={t(
            "Open the original Swing application in its own process.",
            "在独立进程中打开原 Swing 应用。",
          )}
        >
          <Button
            onClick={() =>
              api("/swing", {})
                .then(() => notify(t("Swing launched", "Swing 已启动")))
                .catch(error)
            }
          >
            <ExternalLink size={16} />
            {t("Open Swing UI", "打开 Swing UI")}
          </Button>
        </Section>
        <Section title={t("Help & reference", "帮助与参考")}>
          <div className="help-links">
            {[
              ["015", "ZKM overview", "ZKM 概览"],
              ["016", "Class browser", "类浏览器"],
              ["012", "Build Helper", "构建向导"],
              ["011", "Script Helper", "脚本向导"],
              ["025", "Obfuscation", "混淆"],
              ["020", "Trim", "裁剪"],
              ["035", "Stack trace translation", "堆栈还原"],
            ].map(([id, en, zh]) => (
              <a
                key={id}
                href={"/help/" + id + ".html"}
                target="_blank"
                rel="noreferrer"
              >
                <BookOpen size={17} />
                {t(en, zh)}
                <ExternalLink size={13} />
              </a>
            ))}
          </div>
          <p className="muted">
            {t(
              "Bundled engine documentation is provided in its original language.",
              "随附引擎文档保留原始语言。",
            )}
          </p>
        </Section>
        <Section title={t("About this workbench", "关于工作台")}>
          <p>
            {t(
              "An independent community interface for Open ZelixKlassMaster 27. Not an official Zelix product.",
              "Open ZelixKlassMaster 27 的独立社区界面，并非 Zelix 官方产品。",
            )}
          </p>
          <div className="license-pills">
            <a
              href="/third-party-licenses.txt"
              target="_blank"
              rel="noreferrer"
            >
              {t("Third-party licenses", "第三方许可")}
            </a>
            <span>React</span>
            <span>Monaco Editor</span>
            <span>CFR 0.152</span>
            <span>Vineflower 1.12.0</span>
          </div>
        </Section>
      </div>
    );
  return (
    <div className="studio-page">
      <div className="studio-toolbar">
        <div>
          <div className="eyebrow">TRANSLATE / RECOVER</div>
          <h2>{t("Developer tools", "开发工具")}</h2>
        </div>
        <div className="button-row">
          <Button
            onClick={() =>
              choose({
                onSelect: (path) =>
                  api("/files/read?path=" + encodeURIComponent(path))
                    .then((r) => setInput(r.content))
                    .catch(error),
              })
            }
          >
            {t("Open input", "打开输入")}
          </Button>
          <Button
            onClick={() =>
              navigator.clipboard.readText().then(setInput).catch(error)
            }
          >
            {t("Paste", "粘贴")}
          </Button>
          <Button primary onClick={() => start().catch(error)}>
            <Languages size={16} />
            {t("Translate", "转换")}
          </Button>
        </div>
      </div>
      <div className="tabs tools-tabs">
        <button
          className={tab === "stacktrace" ? "active" : ""}
          onClick={() => {
            setTab("stacktrace");
            setId("");
          }}
        >
          {t("Stack trace translate", "堆栈还原")}
        </button>
        <button
          className={tab === "proguard" ? "active" : ""}
          onClick={() => {
            setTab("proguard");
            setId("");
          }}
        >
          {t("ProGuard configuration", "ProGuard 配置转换")}
        </button>
      </div>
      {tab === "stacktrace" && (
        <div className="tool-options">
          <Field label={t("ChangeLogs", "ChangeLog 文件")}>
            <Paths values={logs} onChange={setLogs} />
          </Field>
          <Field label={t("Obfuscated bytecode paths", "混淆后字节码路径")}>
            <Paths values={bytecodePaths} onChange={setBytecodePaths} />
          </Field>
          <label className="toggle-row">
            <input
              type="checkbox"
              checked={analyze}
              onChange={(e) => setAnalyze(e.target.checked)}
            />
            {t(
              "Analyze bytecode (empty paths use project classpath)",
              "分析字节码（路径为空时使用项目 Classpath）",
            )}
          </label>
        </div>
      )}
      <div className="tool-editors">
        <div>
          <div className="pane-label">{t("Input", "输入")}</div>
          <Editor
            theme={theme}
            value={input}
            onChange={setInput}
            language="plaintext"
          />
        </div>
        <div>
          <div className="pane-label">
            {t("Result", "结果")}
            {result && (
              <>
                <Button
                  onClick={() =>
                    navigator.clipboard
                      .writeText(result)
                      .then(() => notify(t("Copied", "已复制")))
                      .catch(error)
                  }
                >
                  {t("Copy", "复制")}
                </Button>
                <Button onClick={() => setSaving(true)}>
                  {t("Save result", "保存结果")}
                </Button>
              </>
            )}
            {result && tab === "proguard" && (
              <Button onClick={toScript}>
                <Code2 size={14} />
                {t("Edit script", "编辑脚本")}
              </Button>
            )}
          </div>
          <Editor
            theme={theme}
            value={j?.status === "failed" ? j.error : result || ""}
            readOnly
            language={tab === "proguard" ? "zkm" : "plaintext"}
          />
        </div>
      </div>
      {saving && (
        <Modal
          title={t("Save result", "保存结果")}
          onClose={() => setSaving(false)}
        >
          <div className="modal-body">
            <Field label={t("File path", "文件路径")}>
              <PathField value={savePath} onChange={setSavePath} />
            </Field>
          </div>
          <footer>
            <Button
              primary
              onClick={() =>
                api("/files/write", { path: savePath, content: result })
                  .then(() => {
                    setSaving(false);
                    notify(t("Result saved", "结果已保存"));
                  })
                  .catch(error)
              }
            >
              {t("Save", "保存")}
            </Button>
          </footer>
        </Modal>
      )}
    </div>
  );
}

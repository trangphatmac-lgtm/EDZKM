import { useEffect, useState } from "react";
import {
  Plus,
  FolderOpen,
  Save,
  Play,
  CheckCheck,
  FlaskConical,
  X,
  FileCode2,
} from "lucide-react";
import { useWorkbench } from "./context";
import { api } from "./api";
import { Button, Field, Modal, PathField } from "./components";
import { Editor, syncScriptModels } from "./Editor";
export default function ScriptsPage() {
  const {
    project: p,
    setProject,
    acceptSaved,
    t,
    theme,
    run,
    error,
    save,
    choose,
    notify,
    jobs,
  } = useWorkbench();
  const [saving, setSaving] = useState(false),
    [savePath, setSavePath] = useState(""),
    [validation, setValidation] = useState({ id: "", doc: "", text: "" });
  useEffect(() => {
    if (p)
      syncScriptModels(
        p.id,
        p.scripts.map((s) => s.id),
      );
  }, [p?.id, p?.scripts]);
  if (!p) return null;
  const doc = p.scripts.find((s) => s.id === p.activeScript) || p.scripts[0];
  const job =
    validation.doc === doc?.id && validation.text === doc?.content
      ? jobs.find((j) => j.id === validation.id)
      : undefined;
  const patch = (change: object) =>
    setProject({
      ...p,
      scripts: p.scripts.map((s) =>
        s.id === doc.id ? { ...s, ...change } : s,
      ),
    });
  const add = () => {
    const d = {
      id: crypto.randomUUID(),
      name: "untitled.zkm",
      path: "",
      content: "// ZKM Script\n\n",
    };
    setProject({ ...p, scripts: [...p.scripts, d], activeScript: d.id });
  };
  const open = async (path: string) => {
    const result = await api("/files/read?path=" + encodeURIComponent(path));
    const d = {
      id: crypto.randomUUID(),
      name: path.split(/[\\/]/).pop()!,
      path,
      content: result.content,
      savedContent: result.content,
    };
    setProject({
      ...p,
      workingDirectory: path.replace(/[\\/][^\\/]+$/, ""),
      scripts: [...p.scripts, d],
      activeScript: d.id,
    });
  };
  const saveFile = async (path: string) => {
    await api("/files/write", { path, content: doc.content });
    const next = {
      ...p,
      scripts: p.scripts.map((s) =>
        s.id === doc.id
          ? {
              ...s,
              path,
              name: path.split(/[\\/]/).pop()!,
              savedContent: doc.content,
            }
          : s,
      ),
    };
    const saved = await api("/project", next);
    acceptSaved(saved);
    setSaving(false);
    notify(t("Script saved", "脚本已保存"));
  };
  const execute = async (kind: string) => {
    const result = await run(
      {
        kind,
        script: doc.content,
        sourcePath: doc.path || "scripts/" + doc.name,
      },
      true,
    );
    if (kind === "validate")
      setValidation({ id: result.id, doc: doc.id, text: doc.content });
  };
  return (
    <div className="studio-page">
      <div className="studio-toolbar">
        <div>
          <div className="eyebrow">SCRIPT / STUDIO</div>
          <h2>{t("Script studio", "脚本工作室")}</h2>
        </div>
        <div className="button-row">
          <Button onClick={add} title={t("New script", "新建脚本")}>
            <Plus size={16} />
          </Button>
          <Button
            onClick={() =>
              choose({ onSelect: (path) => open(path).catch(error) })
            }
            title={t("Open script", "打开脚本")}
          >
            <FolderOpen size={16} />
          </Button>
          <Button
            onClick={() =>
              doc.path
                ? saveFile(doc.path).catch(error)
                : (setSavePath("scripts/" + doc.name), setSaving(true))
            }
          >
            <Save size={15} />
            {t("Save", "保存")}
          </Button>
          <Button
            onClick={() => {
              setSavePath(doc.path || "scripts/" + doc.name);
              setSaving(true);
            }}
          >
            {t("Save as", "另存为")}
          </Button>
          <Button onClick={() => execute("validate").catch(error)}>
            <CheckCheck size={16} />
            {t("Validate", "校验")}
          </Button>
          <Button onClick={() => execute("preview").catch(error)}>
            <FlaskConical size={16} />
            {t("Preview", "试运行")}
          </Button>
          <Button primary onClick={() => execute("script").catch(error)}>
            <Play size={16} />
            {t("Run", "运行")}
          </Button>
        </div>
      </div>
      <div className="script-tabs">
        {p.scripts.map((s) => (
          <button
            key={s.id}
            className={s.id === doc?.id ? "active" : ""}
            onClick={() => setProject({ ...p, activeScript: s.id })}
          >
            <FileCode2 size={15} />
            {s.name}
            {s.content !== (s.savedContent || "") && (
              <span className="unsaved-dot" />
            )}
            {p.scripts.length > 1 && (
              <span
                className="close-tab"
                role="button"
                tabIndex={0}
                onClick={(e) => {
                  e.stopPropagation();
                  if (
                    s.content !== (s.savedContent || "") &&
                    !confirm(
                      t(
                        "Close this script tab? Save your changes first.",
                        "关闭此脚本标签？请先保存更改。",
                      ),
                    )
                  )
                    return;
                  setProject({
                    ...p,
                    scripts: p.scripts.filter((d) => d.id !== s.id),
                    activeScript: p.scripts.find((d) => d.id !== s.id)!.id,
                  });
                }}
              >
                <X size={13} />
              </span>
            )}
          </button>
        ))}
      </div>
      {doc && (
        <div className="studio-editor">
          <Editor
            key={doc.id}
            documentId={p.id + ":" + doc.id}
            value={doc.content}
            onChange={(content) => patch({ content })}
            theme={theme}
            diagnostic={job?.status === "failed" ? job.result : null}
          />
        </div>
      )}
      <div className="editor-status">
        <span>{doc?.path || t("Unsaved script", "未保存脚本")}</span>
        <span>
          {job?.status === "succeeded"
            ? t("✓ Syntax valid", "✓ 语法有效")
            : job?.status === "failed"
              ? job.error
              : ""}
        </span>
        <span>ZKM Script · UTF-8</span>
      </div>
      {saving && (
        <Modal
          title={t("Save script", "保存脚本")}
          onClose={() => setSaving(false)}
        >
          <div className="modal-body">
            <Field label={t("Script path", "脚本路径")}>
              <PathField value={savePath} onChange={setSavePath} />
            </Field>
          </div>
          <footer>
            <Button onClick={() => setSaving(false)}>
              {t("Cancel", "取消")}
            </Button>
            <Button primary onClick={() => saveFile(savePath).catch(error)}>
              {t("Save", "保存")}
            </Button>
          </footer>
        </Modal>
      )}
    </div>
  );
}

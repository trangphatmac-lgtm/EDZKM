import { useState } from "react";
import { FolderOpen, Download } from "lucide-react";
import { Button, Field, Modal } from "./components";
import { useWorkbench } from "./context";
import { api } from "./api";
import { completedOutputs } from "./outputJobs";
import { statusLabel } from "./labels";
type Output = { path: string; name: string; kind: string };
export default function ProjectActions() {
  const { project, jobs, lang, t, error, notify } = useWorkbench();
  const [pending, setPending] = useState(false);
  const [exporting, setExporting] = useState<{
    id: string;
    files: Output[];
  } | null>(null);
  const [selected, setSelected] = useState("");
  const candidates = completedOutputs(jobs),
    latest = candidates[0];
  const producing = jobs.some(
    (j) =>
      ["queued", "running"].includes(j.status) &&
      ["build", "script", "save"].includes(j.kind),
  );
  const perform = async (operation: () => Promise<void>) => {
    if (pending) return;
    setPending(true);
    try {
      await operation();
    } catch (e) {
      error(e);
    } finally {
      setPending(false);
    }
  };
  const openOutput = async () => {
    if (!project) return;
    let path = project.build.output;
    if (latest) {
      const output = await api("/outputs?jobId=" + latest.id);
      path = output.roots[0] || path;
    }
    await api("/desktop/open", { path });
  };
  const exportFile = async (id: string, path: string) => {
    const result = await api("/outputs/export", {
      jobId: id,
      path,
      language: lang,
    });
    if (!result.cancelled) {
      setExporting(null);
      notify(t("JAR exported: ", "JAR 已导出：") + result.path);
    }
  };
  const load = async (id: string, immediate: boolean) => {
    const output = await api<{ files: Output[] }>("/outputs?jobId=" + id);
    if (!output.files.length)
      throw new Error(
        t(
          "No completed JAR or class directory is available in this run.",
          "该记录没有可导出的 JAR 或类目录。",
        ),
      );
    if (immediate && output.files.length === 1)
      await exportFile(id, output.files[0].path);
    else {
      setExporting({ id, files: output.files });
      setSelected(output.files[0].path);
    }
  };
  if (!project) return null;
  return (
    <>
      <div className="button-row project-actions">
        <Button
          disabled={pending}
          onClick={() =>
            perform(async () => {
              await api("/desktop/open", { path: project.directory });
            })
          }
        >
          <FolderOpen size={15} />
          {t("Open project folder", "打开项目文件夹")}
        </Button>
        <Button disabled={pending} onClick={() => perform(openOutput)}>
          <FolderOpen size={15} />
          {t("Open output folder", "打开输出文件夹")}
        </Button>
        <Button
          disabled={pending || producing || !latest}
          onClick={() => latest && perform(() => load(latest.id, true))}
        >
          <Download size={15} />
          {t("Export finished JAR", "导出成品 JAR")}
        </Button>
        {pending && (
          <small className="muted">
            {t("Waiting for the system dialog…", "正在等待系统窗口…")}
          </small>
        )}
      </div>
      {exporting && (
        <Modal
          title={t("Export finished JAR", "导出成品 JAR")}
          onClose={() => !pending && setExporting(null)}
        >
          <div className="modal-body">
            <Field label={t("Build result", "构建结果")}>
              <select
                disabled={pending}
                value={exporting.id}
                onChange={(e) => perform(() => load(e.target.value, false))}
              >
                {candidates.map((j) => (
                  <option key={j.id} value={j.id}>
                    {statusLabel(j.kind, t)} ·{" "}
                    {new Date(j.created).toLocaleString()}
                  </option>
                ))}
              </select>
            </Field>
            <Field label={t("Artifact to export", "要导出的产物")}>
              <select
                disabled={pending}
                value={selected}
                onChange={(e) => setSelected(e.target.value)}
              >
                {exporting.files.map((f) => (
                  <option key={f.path} value={f.path}>
                    {f.name} ·{" "}
                    {f.kind === "jar"
                      ? "JAR"
                      : t("Package directory", "打包目录")}
                  </option>
                ))}
              </select>
            </Field>
            <p className="muted">{selected}</p>
          </div>
          <footer>
            <Button
              disabled={pending}
              onClick={() => perform(() => exportFile(exporting.id, selected))}
            >
              <Download size={15} />
              {t("Save as…", "另存为…")}
            </Button>
          </footer>
        </Modal>
      )}
    </>
  );
}

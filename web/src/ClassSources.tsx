import { useEffect, useState, useRef, useMemo } from "react";
import {
  GitCompareArrows,
  Upload,
  ArrowRight,
  RefreshCw,
  X,
  Clock,
  Copy,
} from "lucide-react";
import { useWorkbench } from "./context";
import { api } from "./api";
import { Button, Empty, Field, Modal, PathField, Paths } from "./components";
import { configurationKey, matchingRun } from "./preview";
import { DiffEditor, Editor } from "./Editor";
import SourceClassTree from "./SourceClassTree";
import { statusLabel } from "./labels";
import type { ClassIdentity, Pair } from "./types";
export default function ClassSources({
  active,
  onSelect,
}: {
  active: boolean;
  onSelect: (id: string) => void;
}) {
  const { t, theme, jobs, run, error, project, browserRequest } =
    useWorkbench();
  const [mode, setMode] = useState("before");
  const [appliedRequest, setAppliedRequest] = useState(0);
  const decompilations = useRef(
    new Map<string, Promise<import("./types").Job>>(),
  );
  const [retryBuild, setRetryBuild] = useState(0);
  const [launching, setLaunching] = useState(false);
  const [launchError, setLaunchError] = useState("");
  const attempts = useRef(new Map<string, string>());
  const selectedIdentity = useRef<ClassIdentity | null>(null);
  const candidates = jobs.filter(
    (j) =>
      j.status === "succeeded" && j.result?.beforeRoot && j.kind !== "inspect",
  );
  const [runId, setRunId] = useState(""),
    [data, setData] = useState<{
      classes: Pair[];
      before: ClassIdentity[];
      after: ClassIdentity[];
    }>({ classes: [], before: [], after: [] }),
    [selected, setSelected] = useState(""),
    [query, setQuery] = useState(""),
    [engine, setEngine] = useState("vf"),
    [beforeJob, setBeforeJob] = useState(""),
    [afterJob, setAfterJob] = useState(""),
    [importing, setImporting] = useState(false),
    [input, setInput] = useState(""),
    [output, setOutput] = useState(""),
    [logs, setLogs] = useState<string[]>([]),
    [manual, setManual] = useState<Record<string, string>>({}),
    [timeout, setTimeoutValue] = useState(60),
    [retry, setRetry] = useState(0),
    [scroll, setScroll] = useState(0),
    [loadedRun, setLoadedRun] = useState("");
  const [expanded, setExpanded] = useState(new Set<string>());
  const overrides = useMemo(() => {
    const classes = new Map(data.after.map((c) => [c.id, c]));
    return Object.fromEntries(
      Object.entries(manual).map(([id, target]) => [id, classes.get(target)]),
    );
  }, [manual, data.after]);
  const key = project ? configurationKey(project, mode === "before") : "";
  const current = matchingRun(jobs, key, mode === "before");
  const effective = runId || current?.id || attempts.current.get(key) || "";
  const sourceJob = jobs.find((j) => j.id === effective);
  useEffect(() => {
    if (!browserRequest) return;
    setAppliedRequest(browserRequest.serial);
    const requested = jobs.find(
      (j) => j.id === browserRequest.runId && j.projectId === project?.id,
    );
    if (!requested) return;
    setMode("compare");
    setRunId(
      requested?.configurationKey === (project && configurationKey(project))
        ? ""
        : browserRequest.runId,
    );
  }, [browserRequest?.serial]);
  useEffect(() => {
    if (browserRequest && appliedRequest !== browserRequest.serial) return;
    if (
      !active ||
      launching ||
      runId ||
      !project?.build.inputs.length ||
      current ||
      attempts.current.has(key)
    )
      return;
    attempts.current.set(key, "");
    setLaunching(true);
    setLaunchError("");
    run({ kind: mode === "before" ? "inspect" : "preview" }, true)
      .then((job) => {
        attempts.current.set(key, job.id);
      })
      .catch((e) => {
        setLaunchError(e instanceof Error ? e.message : String(e));
        error(e);
      })
      .finally(() => setLaunching(false));
  }, [active, runId, key, current?.id, retryBuild, appliedRequest, launching]);
  const pair = data.classes.find((c) => c.id === selected);
  const before = pair?.before;
  const after = Object.prototype.hasOwnProperty.call(manual, selected)
    ? data.after.find((c) => c.id === manual[selected])
    : pair?.after;
  useEffect(() => {
    let active = true;
    setLoadedRun("");
    setData({ classes: [], before: [], after: [] });
    setSelected("");
    if (effective && sourceJob?.status === "succeeded")
      api("/comparison?jobId=" + effective)
        .then((result) => {
          if (!active) return;
          setLoadedRun(effective);
          setData(result);
          const previous = selectedIdentity.current;
          const exact =
            previous &&
            result.classes.find(
              (p: Pair) =>
                p.before?.id === previous.id || p.after?.id === previous.id,
            );
          const named = result.classes.filter(
            (p: Pair) =>
              (p.before?.name || p.after?.name) === previous?.name &&
              (p.before?.version || p.after?.version) === previous?.version,
          );
          setSelected(
            exact?.id ||
              (named.length === 1 ? named[0].id : result.classes[0]?.id) ||
              "",
          );
          setManual(result.manual || {});
        })
        .catch(error);
    return () => {
      active = false;
    };
  }, [effective, sourceJob?.status]);
  useEffect(() => {
    if (pair) {
      selectedIdentity.current = pair.before || pair.after;
      onSelect(sourceJob?.configurationKey ? pair.before?.id || "" : "");
    }
  }, [pair?.id, loadedRun]);
  useEffect(() => {
    if (!active || loadedRun !== effective) return;
    let mounted = true;
    const start = (
      side: "before" | "after",
      target: ClassIdentity | null | undefined,
    ) => {
      const setter = side === "before" ? setBeforeJob : setAfterJob;
      if (
        !target ||
        (side === "before" ? mode === "after" : mode === "before")
      ) {
        setter("");
        return;
      }
      const id = [effective, side, target.id, engine, retry].join(":");
      let pending = decompilations.current.get(id);
      if (!pending) {
        setter("");
        pending = run({
          kind: "decompile",
          jobId: effective,
          side,
          classId: target.id,
          engine,
          timeout,
        });
        decompilations.current.set(id, pending);
      }
      pending
        .then((job) => {
          if (mounted) setter(job.id);
        })
        .catch((e) => {
          if (mounted) error(e);
        });
    };
    start("before", before);
    start("after", after);
    return () => {
      mounted = false;
    };
  }, [
    active,
    effective,
    loadedRun,
    selected,
    engine,
    manual[selected],
    retry,
    mode,
  ]);
  const fallback = (side: string, target: ClassIdentity | null | undefined) => {
    if (target)
      run({ kind: "bytecode", jobId: effective, side, classId: target.id })
        .then((j) =>
          side === "before" ? setBeforeJob(j.id) : setAfterJob(j.id),
        )
        .catch(error);
  };
  const left = jobs.find((j) => j.id === beforeJob),
    right = jobs.find((j) => j.id === afterJob);
  const text = (j: typeof left, exists: boolean) =>
    !exists
      ? t("// No corresponding class", "// 无对应的类")
      : j?.status === "succeeded"
        ? j.result.source
        : j?.status === "failed"
          ? "// " + j.error
          : j?.status === "cancelled"
            ? t("// Cancelled", "// 已取消")
            : t("// Decompiling…", "// 正在反编译…");
  const loadImport = async () => {
    const j = await run(
      { kind: "compareImport", before: input, after: output, changeLogs: logs },
      true,
    );
    setImporting(false);
    setRunId(j.id);
    setMode("compare");
  };
  const running =
    sourceJob && ["queued", "running"].includes(sourceJob.status)
      ? sourceJob
      : null;
  const failed =
    sourceJob && ["failed", "cancelled"].includes(sourceJob.status);
  return (
    <div className="studio-page">
      <div className="studio-toolbar">
        <div>
          <div className="eyebrow">EXPLORE / DECOMPILE</div>
          <h2>{t("Class browser", "类浏览器")}</h2>
        </div>
        <div className="button-row">
          <div
            className="view-switch"
            role="group"
            aria-label={t("Source view", "源码视图")}
          >
            {[
              ["before", "Original only", "仅原始"],
              ["after", "Obfuscated", "混淆后"],
              ["compare", "Compare", "对比"],
            ].map(([id, en, zh]) => (
              <button
                key={id}
                aria-pressed={mode === id}
                className={mode === id ? "active" : ""}
                onClick={() => setMode(id)}
              >
                {t(en, zh)}
              </button>
            ))}
          </div>
          <select
            aria-label={t("Run", "运行记录")}
            value={runId}
            onChange={(e) => setRunId(e.target.value)}
          >
            <option value="">
              {t("Current configuration · automatic", "当前配置 · 自动更新")}
            </option>
            {sourceJob && runId && !candidates.some((j) => j.id === runId) && (
              <option value={runId}>{statusLabel(sourceJob.status, t)}</option>
            )}
            {candidates.map((j) => (
              <option value={j.id} key={j.id}>
                {statusLabel(j.kind, t)} ·{" "}
                {new Date(j.created).toLocaleTimeString()} · {j.id.slice(0, 6)}
              </option>
            ))}
          </select>
          <select
            aria-label={t("Decompiler", "反编译器")}
            value={engine}
            onChange={(e) => setEngine(e.target.value)}
          >
            <option value="vf">Vineflower 1.12.0</option>
            <option value="cfr">CFR 0.152</option>
          </select>
          <Button onClick={() => setImporting(true)}>
            <Upload size={16} />
            {t("Import pair", "导入对比")}
          </Button>
        </div>
      </div>
      {(running || launching) && (
        <div className="running-banner">
          <span className="spinner" />
          {t(
            mode === "before"
              ? "Loading original classes…"
              : "Automatically previewing the current configuration…",
            mode === "before" ? "正在加载原始类…" : "正在自动试运行当前配置…",
          )}
          {running && (
            <Button
              onClick={() =>
                api("/jobs/" + running.id + "/cancel", {}).catch(error)
              }
            >
              {t("Cancel", "取消")}
            </Button>
          )}
        </div>
      )}
      {(failed || launchError) && (
        <div className="running-banner error-banner">
          {sourceJob?.error ||
            launchError ||
            t("Preview cancelled", "试运行已取消")}
          <Button
            onClick={() => {
              attempts.current.delete(key);
              setRunId("");
              setRetryBuild((v) => v + 1);
            }}
          >
            {t("Retry", "重试")}
          </Button>
        </div>
      )}
      {!effective || loadedRun !== effective ? (
        <Empty
          icon={<GitCompareArrows size={32} />}
          heading={t("Explore your classes", "探索类源码")}
          detail={t(
            "Add inputs in Build Helper. Original classes load automatically; switching to obfuscated or compare automatically previews your configuration.",
            "在 Build Helper 添加输入后自动加载原始类；切换到混淆后或对比视图时，自动试运行当前配置。",
          )}
        >
          <Button onClick={() => setImporting(true)}>
            <Upload size={15} />
            {t("Import existing results", "导入已有结果")}
          </Button>
        </Empty>
      ) : (
        <div className="comparison-layout">
          <SourceClassTree
            pairs={data.classes}
            side={mode === "after" ? "after" : "before"}
            overrides={overrides}
            selected={selected}
            onSelect={setSelected}
            query={query}
            onQuery={setQuery}
            expanded={expanded}
            setExpanded={setExpanded}
            scroll={scroll}
            setScroll={setScroll}
          />
          <main className="comparison-main">
            <div className="comparison-controls">
              <span className="badge accent">
                {statusLabel(pair?.status, t)}
              </span>
              {mode !== "before" && (
                <div className="mapping-label">
                  <code>{before?.name.replaceAll("/", ".") || "—"}</code>
                  <ArrowRight size={15} />
                  <select
                    aria-label={t("Mapped class", "对应类")}
                    value={after?.id || ""}
                    onChange={(e) => {
                      const value = e.target.value;
                      api("/comparison/manual", {
                        jobId: effective,
                        pairId: selected,
                        afterId: value,
                      })
                        .then(() => setManual({ ...manual, [selected]: value }))
                        .catch(error);
                    }}
                  >
                    <option value="">{t("No mapping", "无映射")}</option>
                    {data.after.map((c) => (
                      <option key={c.id} value={c.id}>
                        {c.name.replaceAll("/", ".")} ·{" "}
                        {c.version && c.version !== "null"
                          ? c.version
                          : t("base", "基础版本")}
                      </option>
                    ))}
                  </select>
                </div>
              )}
              <Button
                title={t("Retry", "重试")}
                onClick={() => setRetry(retry + 1)}
              >
                <RefreshCw size={15} />
              </Button>
              <label className="timeout">
                <Clock size={14} />
                <input
                  aria-label={t("Timeout seconds", "超时秒数")}
                  type="number"
                  min={1}
                  max={600}
                  value={timeout}
                  onChange={(e) => setTimeoutValue(Number(e.target.value))}
                />
                s
              </label>
            </div>
            <div
              className={
                "diff-labels " + (mode !== "compare" ? "single-side" : "")
              }
            >
              {mode !== "after" && (
                <span>
                  <i className="dot-original" />
                  {t("Original", "混淆前")}
                  <button
                    title={t("Copy original source", "复制原始代码")}
                    onClick={() =>
                      navigator.clipboard
                        .writeText(text(left, !!before))
                        .catch(error)
                    }
                  >
                    <Copy size={12} />
                  </button>
                  <button onClick={() => fallback("before", before)}>
                    {t("Bytecode", "字节码")}
                  </button>
                  <small>{statusLabel(left?.status, t)}</small>
                  {left && ["queued", "running"].includes(left.status) && (
                    <button
                      onClick={() =>
                        api("/jobs/" + left.id + "/cancel", {}).catch(error)
                      }
                    >
                      <X size={13} />
                    </button>
                  )}
                </span>
              )}
              {mode !== "before" && (
                <span>
                  <i className="dot-modified" />
                  {t("Obfuscated", "混淆后")}
                  <button
                    title={t("Copy obfuscated source", "复制混淆代码")}
                    onClick={() =>
                      navigator.clipboard
                        .writeText(text(right, !!after))
                        .catch(error)
                    }
                  >
                    <Copy size={12} />
                  </button>
                  <button onClick={() => fallback("after", after)}>
                    {t("Bytecode", "字节码")}
                  </button>
                  <small>{statusLabel(right?.status, t)}</small>
                  {right && ["queued", "running"].includes(right.status) && (
                    <button
                      onClick={() =>
                        api("/jobs/" + right.id + "/cancel", {}).catch(error)
                      }
                    >
                      <X size={13} />
                    </button>
                  )}
                </span>
              )}
            </div>
            {mode === "compare" ? (
              <DiffEditor
                theme={theme}
                before={text(left, !!before)}
                after={text(right, !!after)}
              />
            ) : (
              <Editor
                theme={theme}
                language="java"
                readOnly
                value={
                  mode === "before"
                    ? text(left, !!before)
                    : text(right, !!after)
                }
              />
            )}
            <div className="editor-status">
              <span>{engine === "vf" ? "Vineflower 1.12.0" : "CFR 0.152"}</span>
              <span>
                {mode === "compare"
                  ? t("Read-only · Synchronized diff", "只读 · 同步差异对比")
                  : t("Read-only source", "只读源码")}
              </span>
            </div>
          </main>
        </div>
      )}
      {importing && (
        <Modal
          title={t("Import a comparison", "导入对比")}
          onClose={() => setImporting(false)}
        >
          <div className="modal-body">
            <Field label={t("Original JAR / directory", "原始 JAR / 目录")}>
              <PathField allowDirectory value={input} onChange={setInput} />
            </Field>
            <Field label={t("Obfuscated JAR / directory", "混淆后 JAR / 目录")}>
              <PathField allowDirectory value={output} onChange={setOutput} />
            </Field>
            <Field
              label={t(
                "ChangeLogs (in transformation order)",
                "ChangeLog（按混淆顺序）",
              )}
            >
              <Paths values={logs} onChange={setLogs} />
            </Field>
          </div>
          <footer>
            <Button onClick={() => setImporting(false)}>
              {t("Cancel", "取消")}
            </Button>
            <Button
              primary
              disabled={!input || !output}
              onClick={() => loadImport().catch(error)}
            >
              {t("Import", "导入")}
            </Button>
          </footer>
        </Modal>
      )}
    </div>
  );
}

import { useEffect, useRef, useState } from "react";
import {
  Play,
  Square,
  Terminal,
  Copy,
  ChevronDown,
  ChevronUp,
} from "lucide-react";
import { useWorkbench } from "./context";
import { Button, Field, PathField, Paths } from "./components";
import { api } from "./api";
import { DiffEditor } from "./Editor";
import { statusLabel } from "./labels";
import type { Job } from "./types";
import { runtimeCandidates } from "./outputJobs";

const argumentsFrom = (value: string) =>
  value ? value.replaceAll("\r\n", "\n").split("\n") : [];
export default function ComparePage() {
  const { t, project, jobs, run, theme, error, activePage } = useWorkbench();
  const [sourceId, setSourceId] = useState("");
  const [before, setBefore] = useState<string[]>([]),
    [after, setAfter] = useState<string[]>([]);
  const [beforeMain, setBeforeMain] = useState(""),
    [afterMain, setAfterMain] = useState("");
  const [cwd, setCwd] = useState(
    project?.workingDirectory || project?.directory || "",
  );
  const [args, setArgs] = useState(""),
    [jvmArgs, setJvmArgs] = useState("");
  const [stdin, setStdin] = useState(""),
    [timeout, setTimeoutValue] = useState(60);
  const [leftId, setLeftId] = useState(""),
    [rightId, setRightId] = useState("");
  const [channel, setChannel] = useState("stdout"),
    [expanded, setExpanded] = useState(true);
  const [starting, setStarting] = useState(false),
    [loading, setLoading] = useState(false);
  const [launchErrors, setLaunchErrors] = useState<Record<string, string>>({});
  const [executed, setExecuted] = useState("");
  const [followLatest, setFollowLatest] = useState(true);
  const [loadError, setLoadError] = useState("");
  const loadVersion = useRef(0),
    attempted = useRef("");
  const candidates = runtimeCandidates(jobs);
  const latest =
    candidates.find((j) => j.kind !== "compareImport") || candidates[0];
  const left = jobs.find((j) => j.id === leftId),
    right = jobs.find((j) => j.id === rightId);
  const busy =
    starting ||
    [left, right].some((j) => j && ["queued", "running"].includes(j.status));
  const load = async (id: string, preserveMain = false) => {
    const version = ++loadVersion.current;
    attempted.current = id;
    setSourceId(id);
    setLoadError("");
    if (!id) {
      setLoading(false);
      return;
    }
    setLoading(true);
    try {
      const config = await api("/runtime/config?jobId=" + id);
      if (version !== loadVersion.current) return;
      setBefore(config.before);
      setAfter(config.after);
      setCwd(config.workingDirectory);
      if (!preserveMain) {
        setBeforeMain("");
        setAfterMain("");
      }
    } catch (e) {
      if (version !== loadVersion.current) return;
      setBefore([]);
      setAfter([]);
      setLoadError(String(e));
      error(e);
    } finally {
      if (version === loadVersion.current) setLoading(false);
    }
  };
  useEffect(() => {
    if (
      activePage === "compare" &&
      followLatest &&
      !busy &&
      latest &&
      attempted.current !== latest.id
    )
      load(latest.id, true);
  }, [activePage, followLatest, busy, latest?.id]);
  const start = async () => {
    setStarting(true);
    setLaunchErrors({});
    setLeftId("");
    setRightId("");
    setExecuted(sourceId);
    setExpanded(false);
    const common = {
      kind: "programRun",
      sourceJob: sourceId,
      args: argumentsFrom(args),
      jvmArgs: argumentsFrom(jvmArgs),
      stdin,
      timeout,
      workingDirectory: cwd,
    };
    await Promise.allSettled(
      (["before", "after"] as const).map(async (side) => {
        try {
          const job = await run({
            ...common,
            side,
            classpath: side === "before" ? before : after,
            mainClass: side === "before" ? beforeMain : afterMain,
          });
          (side === "before" ? setLeftId : setRightId)(job.id);
        } catch (e) {
          setLaunchErrors((old) => ({ ...old, [side]: String(e) }));
          error(e);
        }
      }),
    );
    setStarting(false);
  };
  const cancel = (j?: Job) =>
    j &&
    ["queued", "running"].includes(j.status) &&
    api("/jobs/" + j.id + "/cancel", {}).catch(error);
  const output = (j: Job | undefined, side: string) => {
    if (launchErrors[side]) return launchErrors[side];
    if (!j)
      return t(
        "Run both programs to compare their output.",
        "运行两侧程序，比较实际输出。",
      );
    if (j.status === "succeeded")
      return j.result?.[channel] || t("(no output)", "（无输出）");
    if (j.status === "failed") return j.error;
    return statusLabel(j.status, t);
  };
  const summary = (j?: Job) =>
    j?.status === "succeeded"
      ? `${j.result.timedOut ? t("Timed out", "已超时") : t("Exit ", "退出码 ") + j.result.exitCode} · ${j.result.elapsedMs} ms`
      : statusLabel(j?.status, t);
  return (
    <div className="studio-page runtime-page">
      <div className="studio-toolbar">
        <div>
          <div className="eyebrow">RUN / OBSERVE</div>
          <h2>{t("Runtime output comparison", "程序运行输出对比")}</h2>
        </div>
        <div className="button-row">
          <Button onClick={() => setExpanded(!expanded)}>
            {expanded ? <ChevronUp size={15} /> : <ChevronDown size={15} />}{" "}
            {t("Launch settings", "运行设置")}
          </Button>
          {busy ? (
            <Button
              onClick={() => {
                cancel(left);
                cancel(right);
              }}
            >
              <Square size={14} />
              {t("Stop both", "停止两侧")}
            </Button>
          ) : (
            <Button
              primary
              disabled={!project || !before.length || !after.length || loading}
              onClick={start}
            >
              <Play size={15} />
              {t("Run both", "运行两侧")}
            </Button>
          )}
        </div>
      </div>
      <div className="runtime-settings" hidden={!expanded}>
        <div className="runtime-origin">
          <Field label={t("Use paths from a build", "从构建记录填入路径")}>
            <select
              disabled={busy || loading}
              value={followLatest ? "latest" : sourceId}
              onChange={(e) => {
                const id = e.target.value;
                setFollowLatest(id === "latest");
                load(id === "latest" ? latest?.id || "" : id);
              }}
            >
              <option value="latest">
                {t(
                  "Latest successful build (automatic)",
                  "最新成功构建（自动跟随）",
                )}
                {latest
                  ? ` · ${statusLabel(latest.kind, t)} · ${latest.id.slice(0, 6)}`
                  : ""}
              </option>
              <option value="">{t("Custom paths", "自定义路径")}</option>
              {candidates.map((j) => (
                <option key={j.id} value={j.id}>
                  {statusLabel(j.kind, t)} ·{" "}
                  {new Date(j.created).toLocaleString()} · {j.id.slice(0, 6)}
                </option>
              ))}
            </select>
          </Field>
          <p>
            {t(
              "Automatically follows new successful builds. Select a historical build or edit the paths to pin them. Arguments and recorded output are preserved.",
              "默认跟随最新成功构建；选择历史记录或编辑路径后固定使用该配置。运行参数和已执行的输出会保留。",
            )}
          </p>
        </div>
        {loadError && (
          <div className="error-banner">
            <span>
              {t("Could not load build paths: ", "无法加载构建路径：")}
              {loadError}
            </span>
            <Button onClick={() => load(sourceId, true)}>
              {t("Retry", "重试")}
            </Button>
          </div>
        )}
        <fieldset disabled={busy || loading} className="runtime-form">
          <div className="runtime-columns">
            {(["before", "after"] as const).map((side) => (
              <section key={side}>
                <h3>
                  <i
                    className={
                      side === "before" ? "dot-original" : "dot-modified"
                    }
                  />
                  {side === "before"
                    ? t("Original program", "原始程序")
                    : t("Obfuscated program", "混淆后程序")}
                </h3>
                <Field
                  label={
                    side === "before"
                      ? t("Original classpath", "原始类路径")
                      : t("Obfuscated classpath", "混淆后类路径")
                  }
                  hint={t(
                    "JARs, class directories and dependencies, in resolution order.",
                    "按加载顺序填写 JAR、类目录和依赖。",
                  )}
                >
                  <Paths
                    values={side === "before" ? before : after}
                    onChange={(paths) => {
                      setFollowLatest(false);
                      (side === "before" ? setBefore : setAfter)(paths);
                    }}
                  />
                </Field>
                <Field
                  label={
                    side === "before"
                      ? t("Original main class", "原始主类")
                      : t("Obfuscated main class", "混淆后主类")
                  }
                >
                  <input
                    value={side === "before" ? beforeMain : afterMain}
                    onChange={(e) =>
                      (side === "before" ? setBeforeMain : setAfterMain)(
                        e.target.value,
                      )
                    }
                    placeholder={t(
                      "Auto: JAR Main-Class",
                      "自动读取 JAR Main-Class",
                    )}
                  />
                </Field>
              </section>
            ))}
          </div>
          <div className="runtime-columns">
            <Field
              label={t("Program arguments", "程序参数")}
              hint={t(
                "One argument per line; spaces are preserved, no shell quoting needed.",
                "每行一个参数，保留空格，无需 shell 引号。",
              )}
            >
              <textarea
                rows={3}
                value={args}
                onChange={(e) => setArgs(e.target.value)}
              />
            </Field>
            <Field
              label={t("JVM arguments", "JVM 参数")}
              hint={t(
                "One argument per line, e.g. -Xmx512m",
                "每行一个参数，例如 -Xmx512m",
              )}
            >
              <textarea
                rows={3}
                value={jvmArgs}
                onChange={(e) => setJvmArgs(e.target.value)}
              />
            </Field>
            <Field label={t("Working directory", "执行目录")}>
              <PathField directory value={cwd} onChange={setCwd} />
            </Field>
            <Field label={t("Timeout per side (seconds)", "每侧超时（秒）")}>
              <input
                type="number"
                min={1}
                max={3600}
                value={timeout}
                onChange={(e) => setTimeoutValue(Number(e.target.value))}
              />
            </Field>
          </div>
          <Field
            label={t(
              "Standard input (EOF after text)",
              "标准输入（发送后关闭输入）",
            )}
          >
            <textarea
              rows={2}
              value={stdin}
              onChange={(e) => setStdin(e.target.value)}
            />
          </Field>
        </fieldset>
      </div>
      <div className="runtime-results">
        <div className="comparison-controls">
          <Terminal size={16} />
          <div
            className="view-switch"
            role="group"
            aria-label={t("Output stream", "输出流")}
          >
            {["stdout", "stderr"].map((id) => (
              <button
                key={id}
                className={channel === id ? "active" : ""}
                aria-pressed={channel === id}
                onClick={() => setChannel(id)}
              >
                {id}
              </button>
            ))}
          </div>
          <span className="muted">
            {executed
              ? t("Source run: ", "来源记录：") + executed.slice(0, 8)
              : t("Custom execution", "自定义执行")}
          </span>
        </div>
        <div className="diff-labels">
          {([left, right] as const).map((j, index) => (
            <span key={index}>
              <i className={index === 0 ? "dot-original" : "dot-modified"} />
              {index === 0 ? t("Original", "原始") : t("Obfuscated", "混淆后")}
              <small
                className={
                  j?.result?.timedOut || j?.result?.exitCode
                    ? "runtime-failed"
                    : ""
                }
              >
                {summary(j)}
              </small>
              {j?.result?.[channel + "Truncated"] && (
                <small>{t("Output truncated", "输出过长，已截断")}</small>
              )}
              <button
                aria-label={
                  index === 0
                    ? t("Copy original output", "复制原始输出")
                    : t("Copy obfuscated output", "复制混淆后输出")
                }
                onClick={() =>
                  navigator.clipboard
                    .writeText(
                      j?.result?.[channel] ||
                        output(j, index === 0 ? "before" : "after"),
                    )
                    .catch(error)
                }
              >
                <Copy size={13} />
              </button>
              {j && ["queued", "running"].includes(j.status) && (
                <button onClick={() => cancel(j)}>{t("Cancel", "取消")}</button>
              )}
            </span>
          ))}
        </div>
        <DiffEditor
          before={output(left, "before")}
          after={output(right, "after")}
          theme={theme}
          language="plaintext"
        />
      </div>
    </div>
  );
}

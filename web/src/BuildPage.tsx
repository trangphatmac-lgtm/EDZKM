import { useState } from "react";
import {
  ArrowLeft,
  ArrowRight,
  Check,
  Play,
  FlaskConical,
  Code2,
  ShieldCheck,
  Settings2,
} from "lucide-react";
import { api } from "./api";
import { useWorkbench } from "./context";
import {
  Button,
  Field,
  OptionFields,
  PathField,
  Paths,
  Section,
  Modal,
} from "./components";
import {
  obfuscationOptions,
  trimOptions,
  saveOptions,
  ruleKeywords,
} from "./options";
import ProjectActions from "./ProjectActions";
import { Editor } from "./Editor";
export default function BuildPage() {
  const {
    project: p,
    setProject,
    t,
    theme,
    lang,
    run,
    error,
    navigate,
    showClasses,
    jobs,
  } = useWorkbench();
  const [reportId, setReportId] = useState(""),
    [step, setStep] = useState(0),
    [script, setScript] = useState(""),
    [group, setGroup] = useState("protection"),
    [keyword, setKeyword] = useState("exclude"),
    [target, setTarget] = useState(
      "*.* public static main(java.lang.String[])",
    );
  if (!p) return null;
  const report = jobs.find((j) => j.id === reportId);
  const b = p.build;
  const update = (change: object) =>
    setProject({ ...p, build: { ...b, ...change } });
  const steps = [
    t("Inputs", "输入"),
    t("Trim", "裁剪"),
    t("Rules", "规则"),
    t("Obfuscation", "混淆"),
    t("Output", "输出"),
    t("Review", "预览"),
  ];
  const generate = async () => {
    const { script } = await api("/build/script", { project: p });
    setScript(script);
    return script;
  };
  const toScript = async () => {
    const content = await generate();
    const document = {
      id: crypto.randomUUID(),
      name: "build.zkm",
      path: "scripts/build.zkm",
      content,
    };
    setProject({
      ...p,
      scripts: [...p.scripts, document],
      activeScript: document.id,
    });
    navigate("scripts");
  };
  const start = async (preview: boolean) => {
    const job = await run({ kind: preview ? "preview" : "build" }, true);
    showClasses(job.id);
  };
  return (
    <div className="page">
      <div className="page-heading">
        <div className="eyebrow">BUILD / CONFIGURE</div>
        <h1>Build Helper</h1>
        <p>
          {t(
            "A deliberate workflow. Every option, within reach.",
            "清晰的构建流程，每个选项都触手可及。",
          )}
        </p>
      </div>
      <div className="stepper">
        {steps.map((label, i) => (
          <button
            className={step === i ? "active" : step > i ? "complete" : ""}
            key={label}
            onClick={() => {
              setStep(i);
              if (i === 5) generate().catch(error);
            }}
          >
            <span>
              {step > i ? <Check size={14} /> : String(i + 1).padStart(2, "0")}
            </span>
            {label}
            {i < 5 && <i />}
          </button>
        ))}
      </div>
      <div className="build-layout">
        <div className="build-main">
          {step === 0 && (
            <>
              <Section
                title={t("What are we protecting?", "选择要保护的代码")}
                detail={t(
                  "Select classes, JARs, directories and resource files.",
                  "选择类文件、JAR、目录和资源文件。",
                )}
              >
                <Paths
                  values={b.inputs}
                  onChange={(inputs) =>
                    setProject({
                      ...p,
                      checkpointInputs: [],
                      build: { ...b, inputs },
                    })
                  }
                />
                <label className="toggle-row">
                  <input
                    type="checkbox"
                    checked={b.openNestedArchives}
                    onChange={(e) =>
                      update({ openNestedArchives: e.target.checked })
                    }
                  />
                  <span>
                    {t("Open nested archives", "打开嵌套归档")}
                    <small>
                      {t(
                        "Include classes inside embedded JARs.",
                        "同时处理内嵌 JAR 中的类。",
                      )}
                    </small>
                  </span>
                </label>
              </Section>
              <Section
                title={t("Input filters", "输入筛选")}
                detail={t(
                  "Skip or restore entries using paths, wildcards and nested archive locations.",
                  "使用路径、通配符或嵌套归档位置跳过或重新包含条目。",
                )}
              >
                <Field label={t("Skip paths", "跳过路径")}>
                  <Paths
                    values={b.skipped || []}
                    onChange={(skipped) => update({ skipped })}
                  />
                </Field>
                <Field label={t("Restore paths", "重新包含路径")}>
                  <Paths
                    values={b.restored || []}
                    onChange={(restored) => update({ restored })}
                  />
                </Field>
              </Section>
              <Section
                title={t("Dependency classpath", "依赖 Classpath")}
                detail={t(
                  "Add the libraries referenced by your application.",
                  "添加应用引用的依赖库。",
                )}
              >
                <Paths
                  values={b.classpath}
                  onChange={(classpath) => update({ classpath })}
                />
              </Section>
            </>
          )}
          {step === 1 && (
            <>
              <Section title={t("Remove unused code", "移除未使用的代码")}>
                <label className="toggle-row">
                  <input
                    type="checkbox"
                    checked={b.trim}
                    onChange={(e) => update({ trim: e.target.checked })}
                  />
                  <span>
                    {t("Enable Trim", "启用 Trim")}
                    <small>
                      {t(
                        "Keep entry points and reflective access in the exclusion rules.",
                        "请在排除规则中保留入口点与反射访问目标。",
                      )}
                    </small>
                  </span>
                </label>
              </Section>
              <Section
                title={t("Trim exclusions", "Trim 排除规则")}
                action={
                  <Button
                    onClick={() =>
                      run({ kind: "trimReport" }, true)
                        .then((j) => setReportId(j.id))
                        .catch(error)
                    }
                  >
                    {t("Test Trim report", "测试 Trim 报告")}
                  </Button>
                }
              >
                <div className="small-editor">
                  <Editor
                    theme={theme}
                    value={b.trimRules}
                    onChange={(trimRules) => update({ trimRules })}
                  />
                </div>
              </Section>
              <Section title={t("Attributes", "属性清理")}>
                <OptionFields
                  options={trimOptions}
                  value={b.trimOptions}
                  onChange={(trimOptions) => update({ trimOptions })}
                />
              </Section>
            </>
          )}
          {step === 2 && (
            <>
              <Section
                title={t("Preserve what matters", "保留必要的名称与行为")}
                detail={t(
                  "Compose rules for classes, fields, methods, annotations and references.",
                  "配置类、字段、方法、注解和引用规则。",
                )}
              >
                <div className="rule-builder">
                  <select
                    value={keyword}
                    onChange={(e) => setKeyword(e.target.value)}
                  >
                    {ruleKeywords.map((v) => (
                      <option key={v}>{v}</option>
                    ))}
                  </select>
                  <input
                    aria-label={t("Rule target", "规则目标")}
                    value={target}
                    onChange={(e) => setTarget(e.target.value)}
                  />
                  <Button
                    onClick={() =>
                      update({
                        rules:
                          b.rules +
                          "\n" +
                          keyword +
                          " " +
                          target.replace(/;\s*$/, "") +
                          ";",
                      })
                    }
                  >
                    {t("Add rule", "添加规则")}
                  </Button>
                </div>
                <div className="rules-editor">
                  <Editor
                    theme={theme}
                    value={b.rules}
                    onChange={(rules) => update({ rules })}
                  />
                </div>
                <div className="button-row">
                  <Button
                    onClick={() =>
                      run({ kind: "open" }, true)
                        .then(() => navigate("browser"))
                        .catch(error)
                    }
                  >
                    {t(
                      "Find entry points in class browser",
                      "在类浏览器中查找入口",
                    )}
                  </Button>
                  <Button onClick={() => start(true).catch(error)}>
                    <FlaskConical size={16} />
                    {t("Test rules with a preview run", "试运行并查看规则效果")}
                  </Button>
                </div>
              </Section>
              <p className="muted">
                {t(
                  "Full ZKM rule syntax is supported, including advanced inclusion and exclusion statements.",
                  "支持完整 ZKM 规则语法，包括高级包含与排除语句。",
                )}
              </p>
            </>
          )}
          {step === 3 && (
            <Section
              title={t("Protection settings", "混淆保护设置")}
              detail={t(
                "Unset options inherit the original engine defaults.",
                "未设置的选项沿用原引擎默认值。",
              )}
            >
              <div className="tabs">
                {[
                  ["protection", "Protection", "保护"],
                  ["names", "Naming", "命名"],
                  ["metadata", "Metadata", "元数据"],
                  ["compatibility", "Compatibility", "兼容"],
                  ["mapping", "ChangeLog", "映射"],
                ].map(([id, en, zh]) => (
                  <button
                    className={group === id ? "active" : ""}
                    key={id}
                    onClick={() => setGroup(id)}
                  >
                    {t(en, zh)}
                  </button>
                ))}
              </div>
              <label className="toggle-row">
                <input
                  type="checkbox"
                  checked={b.obfuscate}
                  onChange={(e) => update({ obfuscate: e.target.checked })}
                />
                {t("Enable obfuscation", "启用混淆")}
              </label>
              <OptionFields
                options={obfuscationOptions.filter((o) => o.group === group)}
                value={b.obfuscation}
                onChange={(obfuscation) => update({ obfuscation })}
              />
            </Section>
          )}
          {step === 4 && (
            <Section
              title={t("Where should the output go?", "设置产物输出位置")}
            >
              <ProjectActions />
              <Field label={t("Output directory or archive", "输出目录或归档")}>
                <PathField
                  directory
                  value={b.output}
                  onChange={(output) => update({ output })}
                />
              </Field>
              <OptionFields
                options={saveOptions}
                value={b.saveOptions}
                onChange={(saveOptions) => update({ saveOptions })}
              />
              <p className="notice">
                {t(
                  "Preview runs redirect ZKM output to the run directory. Script execute commands keep their original behavior.",
                  "试运行将 ZKM 输出重定向到运行目录；脚本 execute 命令保持原始行为。",
                )}
              </p>
            </Section>
          )}
          {step === 5 && (
            <Section
              title={t("Ready to build", "准备构建")}
              detail={t(
                "The exact ZKM script generated from your settings.",
                "根据当前配置生成的 ZKM Script。",
              )}
              action={
                <Button onClick={() => toScript().catch(error)}>
                  <Code2 size={15} />
                  {t("Edit as script", "作为脚本编辑")}
                </Button>
              }
            >
              <div className="review-editor">
                <Editor value={script} readOnly theme={theme} />
              </div>
            </Section>
          )}
          <div className="wizard-footer">
            <Button disabled={step === 0} onClick={() => setStep(step - 1)}>
              <ArrowLeft size={15} />
              {t("Back", "上一步")}
            </Button>
            {step < 5 ? (
              <Button
                primary
                onClick={() => {
                  setStep(step + 1);
                  if (step === 4) generate().catch(error);
                }}
              >
                {t("Continue", "继续")}
                <ArrowRight size={15} />
              </Button>
            ) : (
              <div className="button-row">
                <Button onClick={() => start(true).catch(error)}>
                  <FlaskConical size={16} />
                  {t("Preview run", "试运行")}
                </Button>
                <Button primary onClick={() => start(false).catch(error)}>
                  <Play size={16} />
                  {t("Run build", "运行构建")}
                </Button>
              </div>
            )}
          </div>
        </div>
        <aside className="build-summary">
          <div className="card-symbol">
            <ShieldCheck size={24} />
          </div>
          <h3>{t("Build at a glance", "构建概览")}</h3>
          <div className="summary-line">
            <span>{t("Input paths", "输入路径")}</span>
            <strong>{b.inputs.length}</strong>
          </div>
          <div className="summary-line">
            <span>{t("Dependencies", "依赖")}</span>
            <strong>{b.classpath.length}</strong>
          </div>
          <div className="summary-line">
            <span>Trim</span>
            <span className="badge">
              {b.trim ? t("Enabled", "已启用") : t("Off", "关闭")}
            </span>
          </div>
          <div className="summary-line">
            <span>{t("Obfuscation", "混淆")}</span>
            <span className="badge accent">
              {b.obfuscate ? t("Enabled", "已启用") : t("Off", "关闭")}
            </span>
          </div>
          <div className="divider" />
          <Settings2 size={17} />
          <small>{t("Output destination", "输出位置")}</small>
          <code>{b.output}</code>
          <div className="summary-note">
            {t(
              "Your settings stay with this project. Run history keeps a snapshot of every build.",
              "配置随项目保存，每次运行都保留构建快照。",
            )}
          </div>
        </aside>
      </div>
      {reportId && (
        <Modal
          title={t("Test Trim report", "测试 Trim 报告")}
          onClose={() => setReportId("")}
        >
          <div className="review-editor">
            <Editor
              theme={theme}
              readOnly
              language="plaintext"
              value={
                report?.status === "succeeded"
                  ? report.result.text
                  : report?.error || t("Generating report…", "正在生成报告…")
              }
            />
          </div>
          <footer>
            <Button onClick={() => setReportId("")}>
              {t("Close", "关闭")}
            </Button>
          </footer>
        </Modal>
      )}
    </div>
  );
}

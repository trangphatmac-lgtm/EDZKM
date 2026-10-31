import { useRef, useState } from "react";
import { Upload, Download } from "lucide-react";
import { Button, Field, PathField } from "./components";
import { useWorkbench } from "./context";
import {
  applyUnifiedNaming,
  namingDraft,
  dictionaryNames,
  namingKinds,
  readDictionary,
} from "./naming";

export default function NamingSettings({
  value,
  onChange,
}: {
  value: Record<string, string>;
  onChange: (next: Record<string, string>) => void;
}) {
  const { t, error, notify } = useWorkbench();
  const [kind, setKind] = useState<string>("Class");
  const [unifiedDraft, setUnifiedDraft] = useState<Record<string, string>>({});
  const [importing, setImporting] = useState(false);
  const upload = useRef<HTMLInputElement>(null);
  // An import can finish after the user switches tabs or edits another setting.
  const latest = useRef(value);
  latest.current = value;
  const unified = kind === "All";
  const displayed = unified ? unifiedDraft : value;
  const prefix = `new${unified ? "Class" : kind}`;
  const namesKey = prefix + "Names",
    fileKey = prefix + "NameFile";
  const mode =
    namesKey in displayed
      ? "editor"
      : fileKey in displayed
        ? "file"
        : "default";
  const metadataKey =
    kind === "LocalVariable"
      ? "localVariables"
      : kind === "Parameter"
        ? "methodParameters"
        : "";
  const update = (
    changes: Record<string, string | undefined>,
    target = kind,
  ) => {
    if (target === "All") {
      setUnifiedDraft((previous) => {
        const next = { ...previous };
        for (const [key, val] of Object.entries(changes)) {
          if (val === undefined) delete next[key];
          else next[key] = val;
        }
        return next;
      });
      return;
    }
    const next = { ...latest.current };
    for (const [key, val] of Object.entries(changes)) {
      if (val === undefined) delete next[key];
      else next[key] = val;
    }
    // Custom debug names need their tables retained and renamed by the engine.
    const metadata =
      target === "LocalVariable"
        ? "localVariables"
        : target === "Parameter"
          ? "methodParameters"
          : "";
    if (
      metadata &&
      Object.entries(changes).some(
        ([key, val]) => key.startsWith("new") && val !== undefined,
      )
    )
      next[metadata] = "obfuscate";
    latest.current = next;
    onChange(next);
  };
  const exportNames = () => {
    const url = URL.createObjectURL(
      new Blob([displayed[namesKey] || ""], {
        type: "text/plain;charset=utf-8",
      }),
    );
    const link = document.createElement("a");
    link.href = url;
    link.download = `${kind.toLowerCase()}-names.txt`;
    link.click();
    URL.revokeObjectURL(url);
  };
  return (
    <div className="naming-settings">
      <h3>{t("Custom names & length", "自定义名称与长度")}</h3>
      <p className="muted">
        {t(
          "Dictionary names are used first without padding or truncation. Once exhausted, new names use the character set and length below. Prefixes, exclusions and existing mappings still apply.",
          "优先使用字典中的完整名称，不截断、不补齐。字典用完后，按下方字符集和长度生成新名称；名称前缀、排除规则与已有映射仍然生效。",
        )}
      </p>
      <div
        className="tabs naming-tabs"
        role="tablist"
        aria-label={t("Name categories", "名称类别")}
      >
        <button
          role="tab"
          aria-selected={unified}
          className={unified ? "active" : ""}
          onClick={() => {
            if (!unified) setUnifiedDraft(namingDraft(latest.current, kind));
            setKind("All");
          }}
        >
          {t("Unified settings", "统一设置")}
        </button>
        {namingKinds.map(([id, en, zh]) => (
          <button
            key={id}
            role="tab"
            aria-selected={kind === id}
            className={kind === id ? "active" : ""}
            onClick={() => setKind(id)}
          >
            {t(en, zh)}
          </button>
        ))}
      </div>
      {unified && (
        <p className="notice">
          {t(
            "Start from the category you just viewed. Edit below, then Apply to all to replace the naming source, dictionary, character set and length for all six categories. You can still adjust each category afterwards. Local variable and parameter handling will be set to Obfuscate names.",
            "以刚才查看的类别为起点。编辑后点击“应用到全部”，统一覆盖六类名称的来源、字典、字符集和长度；之后仍可分别调整。局部变量与参数会自动设为“混淆名称”。",
          )}
        </p>
      )}
      <div className="form-grid">
        <Field label={t("Name source", "名称来源")}>
          <select
            value={mode}
            onChange={(e) =>
              update({
                [namesKey]: e.target.value === "editor" ? "" : undefined,
                [fileKey]: e.target.value === "file" ? "" : undefined,
              })
            }
          >
            <option value="default">{t("Generate names", "自动生成")}</option>
            <option value="editor">
              {t("Edit dictionary here", "在网页内编辑字典")}
            </option>
            <option value="file">
              {t("Use a TXT path", "使用 TXT 文件路径")}
            </option>
          </select>
        </Field>
        <Field
          label={t("Generated name length", "生成名称长度")}
          hint={t(
            "1–128; blank = automatic. Excludes prefix, package path and outer class name.",
            "1–128；留空为自动。不含前缀、包路径和外部类名。 ",
          )}
        >
          <input
            type="number"
            min={1}
            max={128}
            step={1}
            value={displayed[prefix + "NameLength"] || ""}
            placeholder={t("Automatic", "自动")}
            onChange={(e) =>
              update({ [prefix + "NameLength"]: e.target.value || undefined })
            }
          />
        </Field>
        <Field
          label={t("Custom character set", "自定义字符集")}
          hint={t(
            "Optional, e.g. Il or abc. Overrides the default alphabet; include a letter, _ or $.",
            "可选，例如 Il、abc 或中文汉字。覆盖默认字符集，需包含字母、汉字、_ 或 $。 ",
          )}
        >
          <input
            value={displayed[prefix + "NameCharacters"] || ""}
            placeholder={t("Engine default", "引擎默认")}
            onChange={(e) =>
              update({
                [prefix + "NameCharacters"]: e.target.value || undefined,
              })
            }
          />
        </Field>
        {metadataKey && (
          <Field label={t("Debug name handling", "调试名称处理")}>
            <select
              value={value[metadataKey] || ""}
              onChange={(e) =>
                update({ [metadataKey]: e.target.value || undefined })
              }
            >
              <option value="">{t("Engine default", "引擎默认")}</option>
              <option value="obfuscate">
                {t("Obfuscate names", "混淆名称")}
              </option>
              <option value="keep">
                {t("Keep original names", "保留原名")}
              </option>
              <option value="delete">
                {t("Delete debug names", "删除调试名称")}
              </option>
              {metadataKey === "localVariables" &&
                [
                  "keepVisibleMethodParameters",
                  "keepVisibleMethodParametersIfNotObfuscated",
                  "keepMethodParametersIfNotObfuscated",
                ].map((v) => (
                  <option key={v} value={v}>
                    {v}
                  </option>
                ))}
            </select>
          </Field>
        )}
      </div>
      {mode === "file" && (
        <Field
          label={t("TXT dictionary path", "TXT 字典路径")}
          hint={t(
            "UTF-8, one name per line. Relative paths start at the project directory.",
            "UTF-8 编码，每行一个名称。相对路径以项目目录为起点。 ",
          )}
        >
          <PathField
            value={displayed[fileKey] || ""}
            onChange={(path) => update({ [fileKey]: path })}
          />
        </Field>
      )}
      {mode === "editor" && (
        <Field
          label={t("Name dictionary", "名称字典")}
          hint={t(
            "One Java identifier per line; Chinese supported. Blank lines, duplicates, # and // comments are ignored.",
            "每行一个合法 Java 标识符，支持中文。忽略空行、重复项及 #、// 开头的注释。 ",
          )}
        >
          <textarea
            className="name-dictionary"
            spellCheck={false}
            rows={8}
            value={displayed[namesKey] || ""}
            placeholder={"迷雾\n星河\nEcho"}
            onChange={(e) => update({ [namesKey]: e.target.value })}
          />
        </Field>
      )}
      <div className="button-row">
        <Button onClick={() => upload.current?.click()}>
          <Upload size={15} />
          {t("Import TXT", "导入 TXT")}
        </Button>
        {mode === "editor" && (
          <>
            <Button onClick={exportNames}>
              <Download size={15} />
              {t("Export TXT", "导出 TXT")}
            </Button>
            <span className="muted">
              {dictionaryNames(displayed[namesKey] || "").length}{" "}
              {t("unique names", "个去重名称")}
            </span>
          </>
        )}
        <input
          ref={upload}
          type="file"
          accept=".txt,text/plain"
          hidden
          aria-label={t("Import name dictionary", "导入名称字典")}
          onChange={(e) => {
            const file = e.target.files?.[0],
              target = kind;
            e.target.value = "";
            if (file) {
              setImporting(true);
              readDictionary(file)
                .then((text) =>
                  update(
                    {
                      [`new${target === "All" ? "Class" : target}Names`]: text,
                      [`new${target === "All" ? "Class" : target}NameFile`]:
                        undefined,
                    },
                    target,
                  ),
                )
                .catch(error)
                .finally(() => setImporting(false));
            }
          }}
        />
      </div>
      {unified && (
        <div className="button-row">
          <Button
            primary
            disabled={importing}
            onClick={() => {
              const next = applyUnifiedNaming(latest.current, unifiedDraft);
              latest.current = next;
              onChange(next);
              notify(
                t(
                  "Naming settings applied to all six categories",
                  "命名设置已应用到全部六个类别",
                ),
              );
            }}
          >
            {t("Apply to all", "应用到全部")}
          </Button>
          <span className="muted">
            {t(
              "Changes here take effect only after applying.",
              "统一设置需点击应用后才会生效。",
            )}
          </span>
        </div>
      )}
      {metadataKey && value[metadataKey] !== "obfuscate" && (
        <p className="notice">
          {t(
            "Select Obfuscate names above to apply this dictionary and length.",
            "请选择上方的“混淆名称”，才能应用此字典和长度。 ",
          )}
        </p>
      )}
      {(metadataKey || unified) && (
        <p className="muted">
          {t(
            "Only debug tables present in the input can be renamed. Decompiled source may invent its own local names.",
            "仅处理输入文件中已有的调试名称表；反编译器可能自行生成局部变量名。 ",
          )}
        </p>
      )}
      <p className="muted">
        {t(
          "If the fixed length runs out of available names, the build stops with an error. Imported text is saved with the project.",
          "固定长度的可用名称耗尽时，构建会报错。导入的文本随项目保存。 ",
        )}
      </p>
    </div>
  );
}

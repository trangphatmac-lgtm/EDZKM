import { useEffect, useRef, useState, type ReactNode } from "react";
import { File, Plus, X, FolderOpen } from "lucide-react";
import { api } from "./api";
import { useWorkbench } from "./context";
import type { Option } from "./options";
export function Button({
  children,
  onClick,
  primary = false,
  disabled = false,
  title,
}: {
  children: ReactNode;
  onClick?: () => void;
  primary?: boolean;
  disabled?: boolean;
  title?: string;
}) {
  return (
    <button
      title={title}
      className={"button " + (primary ? "primary" : "")}
      onClick={onClick}
      disabled={disabled}
    >
      {children}
    </button>
  );
}
export function Empty({
  icon,
  heading,
  detail,
  children,
}: {
  icon?: ReactNode;
  heading: string;
  detail: string;
  children?: ReactNode;
}) {
  return (
    <div className="empty">
      <div className="empty-icon">{icon || <FolderOpen size={28} />}</div>
      <h2>{heading}</h2>
      <p>{detail}</p>
      {children}
    </div>
  );
}
export function Field({
  label,
  children,
  hint,
}: {
  label: string;
  children: ReactNode;
  hint?: string;
}) {
  return (
    <label className="field">
      <span>{label}</span>
      {children}
      {hint && <small>{hint}</small>}
    </label>
  );
}
export function Section({
  title,
  detail,
  children,
  action,
}: {
  title: string;
  detail?: string;
  children: ReactNode;
  action?: ReactNode;
}) {
  return (
    <section className="card">
      <div className="section-heading">
        <div>
          <h3>{title}</h3>
          {detail && <p>{detail}</p>}
        </div>
        {action}
      </div>
      {children}
    </section>
  );
}
export function PathField({
  value,
  onChange,
  directory = false,
  allowDirectory = false,
}: {
  value: string;
  onChange: (v: string) => void;
  directory?: boolean;
  allowDirectory?: boolean;
}) {
  const { choose, t } = useWorkbench();
  return (
    <div className="input-action">
      <input value={value} onChange={(e) => onChange(e.target.value)} />
      <button
        type="button"
        title={t("Browse", "浏览")}
        onClick={(e) => {
          e.preventDefault();
          choose({ directory, start: value, onSelect: onChange });
        }}
      >
        <FolderOpen size={16} />
      </button>
      {allowDirectory && !directory && (
        <button
          type="button"
          title={t("Choose directory", "选择目录")}
          onClick={(e) => {
            e.preventDefault();
            choose({ directory: true, start: value, onSelect: onChange });
          }}
        >
          <FolderOpen size={16} />
        </button>
      )}
    </div>
  );
}
export function Paths({
  values,
  onChange,
}: {
  values: string[];
  onChange: (v: string[]) => void;
}) {
  const { choose, t, notify, error } = useWorkbench();
  const [value, setValue] = useState("");
  const add = (path: string) => {
    if (path.trim() && !values.includes(path.trim()))
      onChange([...values, path.trim()]);
    setValue("");
  };
  const drop = async (e: React.DragEvent) => {
    e.preventDefault();
    const next = [...values];
    for (const file of Array.from(e.dataTransfer.files)) {
      if (file.size > 20 * 1024 * 1024) {
        notify(
          t(
            "Use Browse for files larger than 20 MB",
            "大于 20 MB 的文件请使用浏览按钮",
          ),
          true,
        );
        continue;
      }
      const buffer = new Uint8Array(await file.arrayBuffer());
      let binary = "";
      for (let i = 0; i < buffer.length; i += 16384)
        binary += String.fromCharCode(...buffer.subarray(i, i + 16384));
      const result = await api("/files/import", {
        name: file.name,
        base64: btoa(binary),
      });
      next.push(result.path);
    }
    onChange(next);
  };
  return (
    <div
      className="paths"
      onDragOver={(e) => e.preventDefault()}
      onDrop={(e) => {
        drop(e).catch(error);
      }}
    >
      <div className="path-items">
        {values.map((path, index) => (
          <div className="path-item" key={index}>
            <File size={16} />
            <code title={path}>{path}</code>
            <button
              title={t("Relocate path", "重新定位路径")}
              onClick={() =>
                choose({
                  onSelect: (next) =>
                    onChange(
                      values.map((value, i) => (i === index ? next : value)),
                    ),
                })
              }
            >
              <FolderOpen size={14} />
            </button>
            <button
              title={t("Remove", "移除")}
              onClick={() => onChange(values.filter((_, i) => i !== index))}
            >
              <X size={14} />
            </button>
          </div>
        ))}
        {!values.length && (
          <div className="drop-hint">
            <FolderOpen size={22} />
            {t(
              "Add a local path or drop files here",
              "添加本地路径，或将文件拖到这里",
            )}
          </div>
        )}
      </div>
      <div className="input-row">
        <input
          placeholder={t("File or directory path", "文件或目录路径")}
          value={value}
          onChange={(e) => setValue(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === "Enter") add(value);
          }}
        />
        <Button onClick={() => add(value)}>
          <Plus size={15} />
        </Button>
        <Button onClick={() => choose({ start: value, onSelect: add })}>
          <FolderOpen size={15} />
          {t("Browse", "浏览")}
        </Button>
        <Button
          onClick={() =>
            choose({ directory: true, start: value, onSelect: add })
          }
        >
          <FolderOpen size={15} />
          {t("Choose directory", "选择目录")}
        </Button>
      </div>
    </div>
  );
}
export function OptionFields({
  options,
  value,
  onChange,
}: {
  options: Option[];
  value: Record<string, string>;
  onChange: (v: Record<string, string>) => void;
}) {
  const { lang, t } = useWorkbench();
  return (
    <div className="form-grid">
      {options.map((o) => (
        <Field key={o.key} label={lang === "zh" ? o.zh : o.en} hint={o.key}>
          {o.values ? (
            <select
              value={value[o.key] ?? ""}
              onChange={(e) => {
                const next = { ...value };
                if (e.target.value === "") delete next[o.key];
                else next[o.key] = e.target.value;
                onChange(next);
              }}
            >
              <option value="">{t("Engine default", "引擎默认值")}</option>
              {o.values.map((v) => (
                <option key={v}>{v}</option>
              ))}
            </select>
          ) : (
            <div className="input-action">
              <input
                value={value[o.key] ?? ""}
                placeholder={t("Engine default", "引擎默认值")}
                onChange={(e) =>
                  onChange({ ...value, [o.key]: e.target.value })
                }
              />
              {o.key in value && (
                <button
                  title={t("Use default", "恢复默认")}
                  onClick={(e) => {
                    e.preventDefault();
                    const next = { ...value };
                    delete next[o.key];
                    onChange(next);
                  }}
                >
                  <X size={13} />
                </button>
              )}
            </div>
          )}
        </Field>
      ))}
    </div>
  );
}
export function Modal({
  title,
  children,
  onClose,
}: {
  title: string;
  children: ReactNode;
  onClose: () => void;
}) {
  const { t } = useWorkbench();
  const ref = useRef<HTMLDialogElement>(null);
  useEffect(() => {
    ref.current?.showModal();
  }, []);
  return (
    <dialog ref={ref} onCancel={onClose}>
      <header>
        <h3>{title}</h3>
        <button onClick={onClose} aria-label={t("Close", "关闭")}>
          <X size={20} />
        </button>
      </header>
      {children}
    </dialog>
  );
}

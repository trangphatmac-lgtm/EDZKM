import { useEffect, useState, useRef } from "react";
import {
  Search,
  Boxes,
  FolderTree,
  RefreshCw,
  Play,
  Save,
  ChevronDown,
  Hash,
  Box,
  Braces,
  Folder,
  ChevronRight,
} from "lucide-react";
import { useWorkbench } from "./context";
import { configurationKey } from "./preview";
import { api } from "./api";
import { Button, Empty, Field, Modal } from "./components";
import { Editor } from "./Editor";
import { flattenTree, type TreeNode } from "./tree";
export default function ClassInspector({
  selectedId,
  active,
}: {
  selectedId?: string;
  active: boolean;
}) {
  const { t, theme, run, error, jobs, project } = useWorkbench();
  const [query, setQuery] = useState(""),
    [items, setItems] = useState<any[]>([]),
    [total, setTotal] = useState(0),
    [selected, setSelected] = useState<any>(null),
    [tab, setTab] = useState("declaration"),
    [loading, setLoading] = useState(false),
    [edit, setEdit] = useState<any>(null),
    [operation, setOperation] = useState(""),
    [scroll, setScroll] = useState(0),
    [hierarchy, setHierarchy] = useState(false),
    [bytecode, setBytecode] = useState("");
  const [roots, setRoots] = useState<TreeNode[]>([]),
    [children, setChildren] = useState<Record<string, TreeNode[]>>({}),
    [expanded, setExpanded] = useState(new Set<string>());
  const listVersion = useRef(0),
    loadVersion = useRef(0),
    listed = useRef("");
  const inputKey = project ? configurationKey(project, true) : "";
  const list = async (append = false) => {
    if (!project?.build.inputs.length) return;
    const version = ++listVersion.current;
    if (!append) setScroll(0);
    setLoading(true);
    try {
      if (query) {
        const result = await api("/engine", {
          operation: "classes",
          query,
          offset: append ? items.length : 0,
          limit: 150,
        });
        if (version !== listVersion.current) return;
        setItems(append ? [...items, ...result.classes] : result.classes);
        setTotal(result.total);
      } else {
        const result = await api("/engine", { operation: "tree", hierarchy });
        if (version !== listVersion.current) return;
        setRoots(result.nodes);
        setChildren({});
        setExpanded(new Set());
        setTotal(result.total);
      }
    } catch (e) {
      error(e);
    } finally {
      if (version === listVersion.current) setLoading(false);
    }
  };
  const toggle = async (node: TreeNode) => {
    if (expanded.has(node.id)) {
      setExpanded((old) => {
        const value = new Set(old);
        value.delete(node.id);
        return value;
      });
      return;
    }
    if (!children[node.id]) {
      const result = await api("/engine", {
        operation: "tree",
        hierarchy,
        parent: node.branch,
      });
      setChildren((old) => ({ ...old, [node.id]: result.nodes }));
    }
    setExpanded((old) => new Set([...old, node.id]));
  };
  const load = async (id: string) => {
    const version = ++loadVersion.current;
    const result = await api("/engine", { operation: "class", classId: id });
    if (version !== loadVersion.current) return;
    setSelected(result);
    setBytecode("");
  };
  useEffect(() => {
    const key = JSON.stringify([query, hierarchy, inputKey]);
    if (!active || listed.current === key) return;
    const timer = setTimeout(() => {
      listed.current = key;
      list();
    }, 250);
    return () => clearTimeout(timer);
  }, [active, query, hierarchy, inputKey]);
  useEffect(() => {
    const j = jobs.find((j) => j.id === operation);
    if (j?.status === "succeeded") {
      list();
      if (selected) load(selected.id).catch(error);
      setOperation("");
    }
  }, [jobs]);
  useEffect(() => {
    if (active && selectedId && selected?.id !== selectedId)
      load(selectedId).catch(error);
  }, [active, selectedId]);
  const open = async () => {
    const j = await run({ kind: "open" }, true);
    setOperation(j.id);
  };
  const apply = async () => {
    const j = await run({
      ...edit,
      kind: "edit",
      editKind: edit.kind,
      operation: "edit",
      classId: selected.id,
    });
    setOperation(j.id);
    setEdit(null);
  };
  const transform = async (kind: string) => {
    if (!project) return;
    const build = {
      ...project.build,
      trim: kind === "trim",
      obfuscate: kind === "obfuscate",
    };
    const result = await api("/build/script", {
      project: { ...project, build },
      mode: "statements",
    });
    const script = result.script;
    const j = await run({ kind: "transform", script }, true);
    setOperation(j.id);
  };
  const flags = [
    ["public", 1],
    ["private", 2],
    ["protected", 4],
    ["static", 8],
    ["final", 16],
    [edit?.kind === "class" ? "super" : "synchronized", 32],
    ["volatile", 64],
    ["transient", 128],
    ["native", 256],
    ["interface", 512],
    ["abstract", 1024],
  ] as const;
  const rows = query
    ? items.map((item) => ({ ...item, depth: 0 }))
    : flattenTree(roots, children, expanded);
  const start = Math.max(0, Math.floor(scroll / 42) - 8),
    visible = rows.slice(start, start + 32);
  return (
    <div className="studio-page">
      <div className="studio-toolbar">
        <div>
          <div className="eyebrow">EXPLORE / EDIT</div>
          <h2>{t("Class browser", "类浏览器")}</h2>
        </div>
        <div className="button-row">
          <Button onClick={() => open().catch(error)}>
            <FolderTree size={16} />
            {t("Open classes", "打开类")}
          </Button>
          <Button onClick={() => transform("trim").catch(error)}>Trim</Button>
          <Button onClick={() => transform("obfuscate").catch(error)}>
            {t("Obfuscate", "混淆")}
          </Button>
          <Button
            primary
            onClick={() => run({ kind: "save" }, true).catch(error)}
          >
            <Save size={15} />
            {t("Save all", "保存全部")}
          </Button>
        </div>
      </div>
      <div className="browser-layout">
        <aside className="class-sidebar">
          <div className="search-box">
            <Search size={15} />
            <input
              placeholder={t("Search classes…", "搜索类…")}
              value={query}
              onChange={(e) => setQuery(e.target.value)}
            />
            <button onClick={() => list()} title={t("Refresh", "刷新")}>
              <RefreshCw size={13} />
            </button>
          </div>
          <div className="class-tools">
            <button
              className={hierarchy ? "active" : ""}
              onClick={() => setHierarchy(!hierarchy)}
            >
              <Boxes size={14} />
              {hierarchy ? t("Hierarchy", "继承层级") : t("Packages", "包视图")}
            </button>
            <button onClick={() => setQuery(query === "main:" ? "" : "main:")}>
              {t("Main classes", "入口类")}
            </button>
            <span>{total}</span>
          </div>
          <div
            className="virtual-list"
            onScroll={(e) => setScroll(e.currentTarget.scrollTop)}
          >
            <div style={{ height: rows.length * 42, position: "relative" }}>
              {visible.map((item, index) => (
                <button
                  style={{
                    position: "absolute",
                    top: (start + index) * 42,
                    height: 42,
                    width: "100%",
                    paddingLeft: 12 + item.depth * 15,
                  }}
                  key={item.id}
                  className={
                    "class-row " + (selected?.id === item.id ? "selected" : "")
                  }
                  onKeyDown={(e) => {
                    if (
                      item.children &&
                      ((e.key === "ArrowRight" && !expanded.has(item.id)) ||
                        (e.key === "ArrowLeft" && expanded.has(item.id)))
                    ) {
                      e.preventDefault();
                      toggle(item).catch(error);
                    }
                  }}
                  aria-expanded={
                    item.children ? expanded.has(item.id) : undefined
                  }
                  onClick={() => {
                    if (item.package) toggle(item).catch(error);
                    else {
                      load(item.id).catch(error);
                      if (item.children) toggle(item).catch(error);
                    }
                  }}
                >
                  {item.children ? (
                    expanded.has(item.id) ? (
                      <ChevronDown size={14} />
                    ) : (
                      <ChevronRight size={14} />
                    )
                  ) : (
                    <Box size={15} />
                  )}
                  <span>
                    <b>{item.name.split(".").pop()}</b>
                    <small>
                      {hierarchy
                        ? "↳ " +
                          (item.superclass || "java/lang/Object").replaceAll(
                            "/",
                            ".",
                          )
                        : item.name.split(".").slice(0, -1).join(".") ||
                          t("Default package", "默认包")}
                    </small>
                  </span>
                  {item.version && <em>{item.version}</em>}
                </button>
              ))}
            </div>
            {query && items.length < total && (
              <Button onClick={() => list(true)}>
                {t("Load more", "加载更多")}
                <ChevronDown size={13} />
              </Button>
            )}
          </div>
          {!loading && !rows.length && (
            <div className="list-loading">
              {t(
                "No matching classes. Open your inputs or adjust the search.",
                "没有匹配的类。请打开输入文件或调整搜索。",
              )}
            </div>
          )}
          {loading && (
            <div className="list-loading">{t("Loading…", "加载中…")}</div>
          )}
        </aside>
        <main className="class-detail">
          {selected ? (
            <>
              <div className="class-title">
                <div>
                  <span className="badge accent">CLASS</span>
                  <h3>{selected.name}</h3>
                  <small>{selected.origin}</small>
                </div>
                <Button
                  disabled={selected.multiRelease}
                  onClick={() =>
                    setEdit({
                      kind: "class",
                      name: selected.name.split(".").pop(),
                      flags: selected.flags,
                    })
                  }
                >
                  {t("Edit properties", "编辑属性")}
                </Button>
              </div>
              <div className="tabs">
                {[
                  ["declaration", "Declaration", "声明"],
                  ["fields", "Fields", "字段"],
                  ["methods", "Methods", "方法"],
                  ["constants", "Constants", "常量"],
                ].map(([id, en, zh]) => (
                  <button
                    className={tab === id ? "active" : ""}
                    key={id}
                    onClick={() => {
                      setTab(id);
                      setBytecode("");
                    }}
                  >
                    {t(en, zh)}
                    {Array.isArray(selected[id]) && (
                      <span>{selected[id].length}</span>
                    )}
                  </button>
                ))}
              </div>
              {tab === "declaration" ? (
                <Editor
                  theme={theme}
                  value={selected.declaration}
                  language="java"
                  readOnly
                />
              ) : bytecode ? (
                <>
                  <Button onClick={() => setBytecode("")}>
                    {t("Back to methods", "返回方法列表")}
                  </Button>
                  <Editor
                    theme={theme}
                    value={bytecode}
                    language="java"
                    readOnly
                  />
                </>
              ) : (
                <div className="member-list">
                  {selected[tab].map((member: any, index: number) => (
                    <div className="member-row" key={index}>
                      {tab === "constants" ? (
                        <Hash size={15} />
                      ) : (
                        <Braces size={15} />
                      )}
                      <div>
                        <code>
                          {tab === "constants" ? member.value : member.name}
                        </code>
                        <small>
                          {tab === "constants"
                            ? "#" + member.index + " · " + member.type
                            : member.descriptor}
                        </small>
                      </div>
                      {tab === "methods" && (
                        <Button onClick={() => setBytecode(member.declaration)}>
                          {t("Bytecode", "字节码")}
                        </Button>
                      )}
                      <Button
                        disabled={
                          selected.multiRelease ||
                          (tab === "constants" && !member.editable)
                        }
                        onClick={() =>
                          setEdit(
                            tab === "constants"
                              ? {
                                  kind: "constant",
                                  index: member.index,
                                  value: member.value,
                                }
                              : {
                                  kind: tab === "fields" ? "field" : "method",
                                  member: member.name,
                                  name: member.name,
                                  descriptor: member.descriptor,
                                  flags: member.flags,
                                },
                          )
                        }
                      >
                        {t("Edit", "编辑")}
                      </Button>
                    </div>
                  ))}
                </div>
              )}
            </>
          ) : (
            <Empty
              icon={<Boxes size={30} />}
              heading={t("Explore your classes", "探索类结构")}
              detail={t(
                "Select a class to inspect its members, constants and bytecode.",
                "选择一个类，查看成员、常量与字节码。",
              )}
            >
              <Button primary onClick={() => open().catch(error)}>
                {t("Open project classes", "打开项目类")}
              </Button>
            </Empty>
          )}
        </main>
      </div>
      {edit && (
        <Modal
          title={t("Edit properties", "编辑属性")}
          onClose={() => setEdit(null)}
        >
          <div className="modal-body">
            {edit.kind === "constant" ? (
              <Field label={t("Value", "值")}>
                <input
                  value={edit.value}
                  onChange={(e) => setEdit({ ...edit, value: e.target.value })}
                />
              </Field>
            ) : (
              <>
                <Field label={t("Name", "名称")}>
                  <input
                    readOnly={edit.kind === "method"}
                    value={edit.name}
                    onChange={(e) => setEdit({ ...edit, name: e.target.value })}
                  />
                </Field>
                <div className="flag-options">
                  {flags
                    .filter(([n]) =>
                      edit.kind === "class"
                        ? [
                            "public",
                            "final",
                            "interface",
                            "abstract",
                            "super",
                          ].includes(n)
                        : edit.kind === "method"
                          ? !["volatile", "transient", "interface"].includes(n)
                          : ![
                              "synchronized",
                              "native",
                              "interface",
                              "abstract",
                            ].includes(n),
                    )
                    .map(([name, bit]) => (
                      <label key={name}>
                        <input
                          type="checkbox"
                          checked={(edit.flags & bit) !== 0}
                          onChange={(e) =>
                            setEdit({
                              ...edit,
                              flags: e.target.checked
                                ? bit <= 4
                                  ? (edit.flags & ~7) | bit
                                  : edit.flags | bit
                                : edit.flags & ~bit,
                            })
                          }
                        />
                        {name}
                      </label>
                    ))}
                </div>
              </>
            )}
          </div>
          <footer>
            <Button onClick={() => setEdit(null)}>
              {t("Restore / cancel", "还原 / 取消")}
            </Button>
            <Button primary onClick={() => apply().catch(error)}>
              {t("Apply change", "应用修改")}
            </Button>
          </footer>
        </Modal>
      )}
    </div>
  );
}

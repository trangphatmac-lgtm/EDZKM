import {
  useEffect,
  useLayoutEffect,
  useMemo,
  useRef,
  useState,
  useId,
  type Dispatch,
  type SetStateAction,
} from "react";
import {
  Search,
  FolderTree,
  Folder,
  FolderOpen,
  ChevronRight,
  ChevronDown,
  ChevronsDownUp,
} from "lucide-react";
import { useWorkbench } from "./context";
import { flattenTree, sourceTree } from "./tree";
import { statusLabel } from "./labels";
import type { ClassIdentity, Pair } from "./types";

export default function SourceClassTree({
  pairs,
  side,
  overrides,
  selected,
  onSelect,
  query,
  onQuery,
  expanded,
  setExpanded,
  scroll,
  setScroll,
}: {
  pairs: Pair[];
  side: "before" | "after";
  overrides: Record<string, ClassIdentity | undefined>;
  selected: string;
  onSelect: (id: string) => void;
  query: string;
  onQuery: (value: string) => void;
  expanded: Set<string>;
  setExpanded: Dispatch<SetStateAction<Set<string>>>;
  scroll: number;
  setScroll: (value: number) => void;
}) {
  const { t } = useWorkbench();
  const tree = useMemo(
    () => sourceTree(pairs, side, query, overrides),
    [pairs, side, query, overrides],
  );
  const [searchClosed, setSearchClosed] = useState(new Set<string>());
  const [focused, setFocused] = useState("");
  const list = useRef<HTMLDivElement>(null);
  const prefix = useId();
  const rowId = (id: string) => prefix + encodeURIComponent(id);
  const searching = !!query.trim();
  const open = useMemo(
    () =>
      searching
        ? new Set(
            [...tree.nodes.values()]
              .filter((n) => n.package && !searchClosed.has(n.id))
              .map((n) => n.id),
          )
        : expanded,
    [tree, expanded, searching, searchClosed],
  );
  const rows = useMemo(
    () => flattenTree(tree.roots, tree.children, open),
    [tree, open],
  );
  const selectedNode = "class:" + selected;
  const selectedParent = tree.parents.get(selectedNode);
  useEffect(() => {
    if (searching || !selectedParent) return;
    setExpanded((old) => {
      const next = new Set(old);
      for (
        let parent: string | undefined = selectedParent;
        parent;
        parent = tree.parents.get(parent)
      )
        next.add(parent);
      return next.size === old.size ? old : next;
    });
  }, [selectedParent, selectedNode, searching]);
  useEffect(() => {
    setSearchClosed(new Set());
  }, [query]);
  useLayoutEffect(() => {
    if (!list.current) return;
    const max = Math.max(0, rows.length * 44 - list.current.clientHeight);
    const next = Math.min(scroll, max);
    list.current.scrollTop = next;
    if (next !== scroll) setScroll(next);
  }, [rows.length]);
  const toggle = (id: string) => {
    const update = (old: Set<string>) => {
      const next = new Set(old);
      next.has(id) ? next.delete(id) : next.add(id);
      return next;
    };
    if (searching) setSearchClosed(update);
    else setExpanded(update);
  };
  const focusRow = (index: number) => {
    const node = rows[index];
    if (!node || !list.current) return;
    setFocused(node.id);
    const top = index * 44;
    if (top < list.current.scrollTop) list.current.scrollTop = top;
    else if (top + 44 > list.current.scrollTop + list.current.clientHeight)
      list.current.scrollTop = top + 44 - list.current.clientHeight;
    setScroll(list.current.scrollTop);
    list.current.focus();
  };
  const active =
    rows.find((n) => n.id === focused)?.id ||
    rows.find((n) => n.id === selectedNode)?.id ||
    rows[0]?.id;
  const start = Math.max(0, Math.floor(scroll / 44) - 6);
  return (
    <aside className="class-sidebar">
      <div className="search-box">
        <Search size={15} />
        <input
          value={query}
          onChange={(e) => {
            onQuery(e.target.value);
            setScroll(0);
            if (list.current) list.current.scrollTop = 0;
          }}
          placeholder={t("Find a class…", "查找类…")}
        />
      </div>
      <div className="class-tools source-tree-tools">
        <span>
          <FolderTree size={14} />
          {t("Packages", "包视图")}
        </span>
        <span>
          {tree.matches.length} / {pairs.length}
        </span>
        <button
          title={t("Collapse all", "全部折叠")}
          onClick={() => {
            if (searching)
              setSearchClosed(
                new Set(
                  [...tree.nodes.values()]
                    .filter((n) => n.package)
                    .map((n) => n.id),
                ),
              );
            else setExpanded(new Set());
            setScroll(0);
            setFocused(tree.roots[0]?.id || "");
          }}
        >
          <ChevronsDownUp size={14} />
        </button>
      </div>
      <div
        ref={list}
        className="comparison-classes source-tree"
        role="tree"
        aria-label={t("Package tree", "包树")}
        tabIndex={0}
        aria-activedescendant={
          rows.slice(start, start + 35).some((n) => n.id === active)
            ? rowId(active!)
            : undefined
        }
        onScroll={(e) => setScroll(e.currentTarget.scrollTop)}
        onKeyDown={(e) => {
          const index = rows.findIndex((n) => n.id === active),
            node = rows[index];
          if (!node) return;
          if (
            ![
              "ArrowDown",
              "ArrowUp",
              "ArrowRight",
              "ArrowLeft",
              "Home",
              "End",
              "Enter",
              " ",
            ].includes(e.key)
          )
            return;
          e.preventDefault();
          if (e.key === "ArrowDown")
            focusRow(Math.min(rows.length - 1, index + 1));
          else if (e.key === "ArrowUp") focusRow(Math.max(0, index - 1));
          else if (e.key === "Home") focusRow(0);
          else if (e.key === "End") focusRow(rows.length - 1);
          else if (e.key === "ArrowRight" && node.package) {
            if (!open.has(node.id)) toggle(node.id);
            else focusRow(index + 1);
          } else if (e.key === "ArrowLeft") {
            if (node.package && open.has(node.id)) toggle(node.id);
            else
              focusRow(
                rows.findIndex((n) => n.id === tree.parents.get(node.id)),
              );
          } else if (e.key === "Enter" || e.key === " ") {
            if (node.package) toggle(node.id);
            else onSelect(node.pairId!);
          }
        }}
      >
        <div
          role="none"
          style={{ position: "relative", height: rows.length * 44 }}
        >
          {rows.slice(start, start + 35).map((node, index) => (
            <button
              key={node.id}
              id={rowId(node.id)}
              role="treeitem"
              tabIndex={-1}
              aria-label={
                node.package
                  ? node.branch?.replaceAll("/", ".") ||
                    t("Default package", "默认包")
                  : node.identity?.name.replaceAll("/", ".")
              }
              aria-level={node.depth + 1}
              aria-expanded={node.package ? open.has(node.id) : undefined}
              aria-selected={
                node.package ? undefined : selected === node.pairId
              }
              className={
                "source-tree-row " +
                (node.package ? "source-package" : "comparison-class") +
                (selected === node.pairId ? " selected" : "") +
                (active === node.id ? " tree-focused" : "")
              }
              style={{
                position: "absolute",
                top: (start + index) * 44,
                height: 44,
                paddingLeft: 10 + node.depth * 16,
              }}
              title={
                node.package
                  ? node.branch || t("Default package", "默认包")
                  : [
                      node.identity?.name.replaceAll("/", "."),
                      node.identity?.origin,
                      node.identity?.version && node.identity.version !== "null"
                        ? "Java " + node.identity.version
                        : "",
                      statusLabel(node.status, t),
                    ]
                      .filter(Boolean)
                      .join("\n")
              }
              onClick={() => {
                setFocused(node.id);
                if (node.package) toggle(node.id);
                else onSelect(node.pairId!);
                list.current?.focus();
              }}
            >
              {node.package ? (
                <>
                  {open.has(node.id) ? (
                    <ChevronDown size={13} />
                  ) : (
                    <ChevronRight size={13} />
                  )}{" "}
                  {open.has(node.id) ? (
                    <FolderOpen size={15} />
                  ) : (
                    <Folder size={15} />
                  )}
                </>
              ) : (
                <span className={"pair-symbol " + node.status}>C</span>
              )}
              <span>
                <b>{node.name || t("Default package", "默认包")}</b>
                {!node.package && (
                  <small>
                    {node.identity?.origin.split(/[\\/]/).pop()}
                    {node.identity?.version && node.identity.version !== "null"
                      ? " · Java " + node.identity.version
                      : ""}
                  </small>
                )}
              </span>
            </button>
          ))}
        </div>
        {!rows.length && (
          <div className="list-loading">
            {t("No matching classes", "没有匹配的类")}
          </div>
        )}
      </div>
    </aside>
  );
}

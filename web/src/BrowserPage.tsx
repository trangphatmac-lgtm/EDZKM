import { useState, useEffect } from "react";
import { Code2, ListTree } from "lucide-react";
import { useWorkbench } from "./context";
import ClassSources from "./ClassSources";
import ClassInspector from "./ClassInspector";
export default function BrowserPage() {
  const { t, activePage, browserRequest } = useWorkbench();
  const [panel, setPanel] = useState("source");
  const [selectedId, setSelectedId] = useState("");
  const [inspected, setInspected] = useState(false);
  useEffect(() => {
    if (browserRequest) setPanel("source");
  }, [browserRequest?.serial]);
  return (
    <div className="browser-workbench">
      <div
        className="browser-panels tabs"
        role="tablist"
        aria-label={t("Class workspace", "类工作区")}
      >
        <button
          role="tab"
          aria-selected={panel === "source"}
          className={panel === "source" ? "active" : ""}
          onClick={() => setPanel("source")}
        >
          <Code2 size={15} />
          {t("Source code", "源码")}
        </button>
        <button
          role="tab"
          aria-selected={panel === "structure"}
          className={panel === "structure" ? "active" : ""}
          onClick={() => {
            setPanel("structure");
            setInspected(true);
          }}
        >
          <ListTree size={15} />
          {t("Members & editing", "成员与编辑")}
        </button>
      </div>
      <div className="browser-panel" hidden={panel !== "source"}>
        <ClassSources
          active={activePage === "browser" && panel === "source"}
          onSelect={setSelectedId}
        />
      </div>
      {inspected && (
        <div className="browser-panel" hidden={panel !== "structure"}>
          <ClassInspector
            active={activePage === "browser" && panel === "structure"}
            selectedId={selectedId}
          />
        </div>
      )}
    </div>
  );
}

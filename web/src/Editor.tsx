/// <reference types="vite/client" />
import { namingKeys } from "./naming";
import { useEffect, useRef } from "react";
import * as monaco from "monaco-editor/esm/vs/editor/editor.api";
import "monaco-editor/esm/vs/basic-languages/java/java.contribution";
import EditorWorker from "monaco-editor/esm/vs/editor/editor.worker?worker";
import {
  keywords,
  obfuscationOptions,
  trimOptions,
  saveOptions,
} from "./options";
(self as any).MonacoEnvironment = { getWorker: () => new EditorWorker() };
monaco.languages.register({ id: "zkm" });
monaco.languages.setMonarchTokensProvider("zkm", {
  keywords,
  tokenizer: {
    root: [
      [/\/\/.*$/, "comment"],
      [/\/\*/, "comment", "@comment"],
      [/"([^"\\]|\\.)*"/, "string"],
      [
        /[a-zA-Z_][\w]*/,
        { cases: { "@keywords": "keyword", "@default": "identifier" } },
      ],
      [/[0-9]+/, "number"],
      [/[{}();=]/, "delimiter"],
    ],
    comment: [
      [/[^/*]+/, "comment"],
      [/\*\//, "comment", "@pop"],
      [/[/*]/, "comment"],
    ],
  },
});
monaco.languages.registerCompletionItemProvider("zkm", {
  provideCompletionItems: (model, position) => {
    const word = model.getWordUntilPosition(position);
    const range = {
      startLineNumber: position.lineNumber,
      endLineNumber: position.lineNumber,
      startColumn: word.startColumn,
      endColumn: word.endColumn,
    };
    return {
      suggestions: [
        ...keywords,
        ...namingKeys,
        ...obfuscationOptions.map((o) => o.key),
        ...trimOptions.map((o) => o.key),
        ...saveOptions.map((o) => o.key),
      ].map((label) => ({
        label,
        kind: monaco.languages.CompletionItemKind.Keyword,
        insertText: label,
        range,
      })),
    };
  },
});
monaco.editor.defineTheme("zkm-dark", {
  base: "vs-dark",
  inherit: true,
  rules: [
    { token: "keyword", foreground: "A99AFF" },
    { token: "string", foreground: "8DD7AD" },
    { token: "comment", foreground: "68788E" },
  ],
  colors: {
    "editor.background": "#10151f",
    "editorLineNumber.foreground": "#495569",
    "editor.lineHighlightBackground": "#171e2b",
    "editor.selectionBackground": "#50479755",
  },
});
const scriptModels = new Map<
  string,
  {
    model: monaco.editor.ITextModel;
    view: monaco.editor.ICodeEditorViewState | null;
  }
>();
export function syncScriptModels(projectId: string, ids: string[]) {
  const keep = new Set(ids.map((id) => projectId + ":" + id));
  for (const [id, entry] of scriptModels)
    if (!keep.has(id)) {
      entry.model.dispose();
      scriptModels.delete(id);
    }
}
export function Editor({
  value,
  onChange,
  language = "zkm",
  theme,
  readOnly = false,
  diagnostic,
  documentId,
}: {
  value: string;
  onChange?: (v: string) => void;
  language?: string;
  theme: string;
  readOnly?: boolean;
  diagnostic?: any;
  documentId?: string;
}) {
  const el = useRef<HTMLDivElement>(null),
    instance = useRef<monaco.editor.IStandaloneCodeEditor | null>(null),
    cb = useRef(onChange);
  cb.current = onChange;
  useEffect(() => {
    if (!el.current) return;
    const saved = documentId ? scriptModels.get(documentId) : undefined;
    const model = saved?.model || monaco.editor.createModel(value, language);
    if (documentId && !saved)
      scriptModels.set(documentId, { model, view: null });
    const editor = monaco.editor.create(el.current, {
      model,
      theme: theme === "dark" ? "zkm-dark" : "vs",
      readOnly,
      automaticLayout: true,
      fontSize: 13,
      lineHeight: 23,
      fontFamily: '"SFMono-Regular",Consolas,"Liberation Mono",monospace',
      minimap: { enabled: false },
      padding: { top: 16, bottom: 16 },
      scrollBeyondLastLine: false,
      wordWrap: "off",
      tabSize: 4,
    });
    if (saved?.view) editor.restoreViewState(saved.view);
    instance.current = editor;
    const listener = editor.onDidChangeModelContent(() =>
      cb.current?.(editor.getValue()),
    );
    return () => {
      listener.dispose();
      if (documentId)
        scriptModels.set(documentId, { model, view: editor.saveViewState() });
      editor.dispose();
      if (!documentId) model.dispose();
      instance.current = null;
    };
  }, [language, documentId]);
  useEffect(() => {
    const e = instance.current;
    if (e && e.getValue() !== value) e.setValue(value);
  }, [value]);
  useEffect(() => {
    monaco.editor.setTheme(theme === "dark" ? "zkm-dark" : "vs");
    instance.current?.updateOptions({ readOnly });
  }, [theme, readOnly]);
  useEffect(() => {
    const model = instance.current?.getModel();
    if (model)
      monaco.editor.setModelMarkers(
        model,
        "zkm",
        diagnostic?.line
          ? [
              {
                startLineNumber: diagnostic.line,
                startColumn: diagnostic.column || 1,
                endLineNumber: diagnostic.line,
                endColumn: (diagnostic.column || 1) + 1,
                severity: monaco.MarkerSeverity.Error,
                message: diagnostic.message,
              },
            ]
          : [],
      );
    if (diagnostic?.line) {
      instance.current?.revealPositionInCenter({
        lineNumber: diagnostic.line,
        column: diagnostic.column || 1,
      });
      instance.current?.setPosition({
        lineNumber: diagnostic.line,
        column: diagnostic.column || 1,
      });
    }
  }, [diagnostic]);
  return <div className="editor" ref={el} />;
}
export function DiffEditor({
  before,
  after,
  theme,
  language = "java",
}: {
  before: string;
  after: string;
  theme: string;
  language?: string;
}) {
  const el = useRef<HTMLDivElement>(null),
    instance = useRef<monaco.editor.IStandaloneDiffEditor | null>(null);
  useEffect(() => {
    const original = monaco.editor.createModel(before, language),
      modified = monaco.editor.createModel(after, language);
    const editor = monaco.editor.createDiffEditor(el.current!, {
      theme: theme === "dark" ? "zkm-dark" : "vs",
      automaticLayout: true,
      readOnly: true,
      renderSideBySide: true,
      useInlineViewWhenSpaceIsLimited: false,
      fontSize: 13,
      lineHeight: 22,
      minimap: { enabled: false },
      scrollBeyondLastLine: false,
      originalEditable: false,
    });
    editor.setModel({ original, modified });
    instance.current = editor;
    return () => {
      editor.dispose();
      original.dispose();
      modified.dispose();
      instance.current = null;
    };
  }, []);
  useEffect(() => {
    const models = instance.current?.getModel();
    if (models) {
      models.original.setValue(before);
      models.modified.setValue(after);
    }
  }, [before, after]);
  useEffect(() => {
    monaco.editor.setTheme(theme === "dark" ? "zkm-dark" : "vs");
  }, [theme]);
  return <div className="editor diff" ref={el} />;
}

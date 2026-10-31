import { describe, it, expect } from "vitest";
import {
  applyUnifiedNaming,
  namingDraft,
  dictionaryNames,
  readDictionary,
} from "./naming";

describe("name dictionaries", () => {
  it("counts BOM/CRLF text, ignores comments and deduplicates while preserving order", () => {
    expect(
      dictionaryNames(
        "\uFEFF迷雾\r\nEcho\n\n# comment\n迷雾\n// comment\n Echo ",
      ),
    ).toEqual(["迷雾", "Echo"]);
  });
  it("rejects large files and invalid UTF-8 rather than corrupting identifiers", async () => {
    await expect(readDictionary({ size: 1_048_577 } as File)).rejects.toThrow(
      "1 MiB",
    );
    await expect(
      readDictionary({
        size: 1,
        arrayBuffer: async () => new Uint8Array([0xff]).buffer,
      } as File),
    ).rejects.toThrow();
    await expect(
      readDictionary({
        size: 5,
        arrayBuffer: async () => new TextEncoder().encode("星河").buffer,
      } as File),
    ).resolves.toBe("星河");
  });
});

describe("unified naming", () => {
  it("replaces stale dictionaries across all categories without changing protection or its source", () => {
    const value = {
      newClassNames: "星河",
      newClassNameLength: "8",
      newMethodNameFile: "old.txt",
      newFieldNameCharacters: "ab",
      localVariables: "delete",
      methodParameters: "keep",
      encryptStringLiterals: "enhanced",
    };
    const draft = namingDraft(value, "Class");
    const result = applyUnifiedNaming(value, draft);
    expect(result.newMethodNameFile).toBeUndefined();
    expect(result.newFieldNameCharacters).toBeUndefined();
    expect(result.newParameterNames).toBe("星河");
    expect(result.localVariables).toBe("obfuscate");
    expect(result.methodParameters).toBe("obfuscate");
    expect(result.encryptStringLiterals).toBe("enhanced");
    expect(value.newMethodNameFile).toBe("old.txt");
    // Individual adjustments after applying must not change other categories or the draft.
    result.newFieldNameLength = "10";
    expect(result.newClassNameLength).toBe("8");
    expect(draft.newClassNameLength).toBe("8");
  });
  it("supports a shared path and clearing custom settings without retaining inline dictionaries", () => {
    const result = applyUnifiedNaming(
      { newMethodNames: "old" },
      namingDraft({ newFieldNameFile: "names.txt" }, "Field"),
    );
    expect(result.newMethodNames).toBeUndefined();
    expect(result.newLocalVariableNameFile).toBe("names.txt");
    expect(
      applyUnifiedNaming(result, {}).newLocalVariableNameFile,
    ).toBeUndefined();
  });
});

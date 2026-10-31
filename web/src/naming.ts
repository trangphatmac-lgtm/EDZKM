export const namingKinds = [
  ["Package", "Package names", "包名"],
  ["Class", "Class names", "类名"],
  ["Method", "Method names", "方法名"],
  ["Field", "Field names", "字段名（成员变量）"],
  ["LocalVariable", "Local variable names", "局部变量名"],
  ["Parameter", "Parameter names", "方法参数名"],
] as const;

export const namingKeys = namingKinds.flatMap(([kind]) =>
  ["Names", "NameFile", "NameLength", "NameCharacters"].map(
    (suffix) => `new${kind}${suffix}`,
  ),
);

export function dictionaryNames(text: string): string[] {
  return [
    ...new Set(
      text
        .replace(/^\uFEFF/, "")
        .split(/\r\n|[\n\r\u0085\u2028\u2029]/)
        .map((line) => line.trim())
        .filter(
          (line) => line && !line.startsWith("#") && !line.startsWith("//"),
        ),
    ),
  ];
}

export async function readDictionary(file: File): Promise<string> {
  if (file.size > 1_048_576)
    throw new Error("TXT must be at most 1 MiB / TXT 文件不能超过 1 MiB");
  // Fatal decoding reports a wrong encoding instead of silently replacing names.
  return new TextDecoder("utf-8", { fatal: true }).decode(
    await file.arrayBuffer(),
  );
}

/** Copy a category's naming fields into a draft; unrelated protection options stay separate. */
export function namingDraft(
  value: Record<string, string>,
  kind: string,
): Record<string, string> {
  return Object.fromEntries(
    ["Names", "NameFile", "NameLength", "NameCharacters"]
      .filter((suffix) => `new${kind}${suffix}` in value)
      .map((suffix) => [`newClass${suffix}`, value[`new${kind}${suffix}`]]),
  );
}

/** Expand the unified draft into the existing per-category settings for scripts and persistence. */
export function applyUnifiedNaming(
  value: Record<string, string>,
  draft: Record<string, string>,
): Record<string, string> {
  const next = { ...value };
  for (const [kind] of namingKinds) {
    for (const suffix of [
      "Names",
      "NameFile",
      "NameLength",
      "NameCharacters",
    ]) {
      const source = `newClass${suffix}`,
        target = `new${kind}${suffix}`;
      if (source in draft) next[target] = draft[source];
      else delete next[target];
    }
  }
  next.localVariables = "obfuscate";
  next.methodParameters = "obfuscate";
  return next;
}

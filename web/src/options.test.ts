import { describe, it, expect } from "vitest";
import { obfuscationOptions, ruleKeywords } from "./options";
describe("Workbench configuration", () => {
  it("provides unique translated options", () => {
    expect(new Set(obfuscationOptions.map((o) => o.key)).size).toBe(
      obfuscationOptions.length,
    );
    for (const o of obfuscationOptions) {
      expect(o.en.length).toBeGreaterThan(0);
      expect(o.zh.length).toBeGreaterThan(0);
    }
  });
  it("offers both exclusions and inclusions", () => {
    expect(ruleKeywords).toContain("exclude");
    expect(ruleKeywords).toContain("obfuscateReferencesInclude");
  });
});

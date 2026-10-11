import { describe, it, expect } from "vitest";
import { flattenTree, sourceTree } from "./tree";
import { statusLabel } from "./labels";
describe("class tree", () => {
  const roots = [
    { id: "p", name: "demo", package: true, children: true },
    { id: "r", name: "Root" },
  ];
  const children = {
    p: [{ id: "a", name: "demo.A", children: true }],
    a: [{ id: "b", name: "demo.B" }],
  };
  it("flattens only expanded branches and retains ancestry depth", () => {
    expect(
      flattenTree(roots, children, new Set(["p", "a"])).map((n) => [
        n.id,
        n.depth,
      ]),
    ).toEqual([
      ["p", 0],
      ["a", 1],
      ["b", 2],
      ["r", 0],
    ]);
    expect(flattenTree(roots, children, new Set()).map((n) => n.id)).toEqual([
      "p",
      "r",
    ]);
  });
  it("can expand a branch before lazy children arrive", () =>
    expect(flattenTree(roots, {}, new Set(["p"])).length).toBe(2));
  it("translates failures and ambiguous pairs", () => {
    expect(statusLabel("failed", (_, zh) => zh)).toBe("失败");
    expect(statusLabel("ambiguous", (_, zh) => zh)).toBe("映射不唯一");
  });
});

describe("source snapshot package tree", () => {
  const identity = (name: string, id = name, version = "null") => ({
    id,
    name,
    version,
    origin: id + ".jar",
    entry: name + ".class",
    file: name + ".class",
    originalName: name,
  });
  const pairs = [
    {
      id: "first",
      before: identity("demo/nested/Foo", "one"),
      after: identity("hidden/a", "one"),
      status: "renamed",
    },
    {
      id: "second",
      before: identity("demo/nested/Foo", "two", "17"),
      after: identity("hidden/b", "two", "17"),
      status: "renamed",
    },
    { id: "root", before: identity("Main"), after: null, status: "removed" },
    {
      id: "generated",
      before: null,
      after: identity("generated/Helper"),
      status: "added",
    },
  ];
  it("keeps package ancestry and distinct archive/version identities", () => {
    const tree = sourceTree(pairs, "before");
    const rows = flattenTree(
      tree.roots,
      tree.children,
      new Set(Object.keys(tree.children)),
    );
    expect(tree.parents.get("class:first")).toBe("package:before:demo/nested");
    expect(tree.parents.get("package:before:demo/nested")).toBe(
      "package:before:demo",
    );
    expect(
      rows
        .filter((n) => n.pairId)
        .map((n) => n.pairId)
        .sort(),
    ).toEqual(["first", "generated", "root", "second"]);
    expect(tree.nodes.get("class:second")?.identity?.version).toBe("17");
    expect(tree.parents.get("class:root")).toBe("package:before:");
  });
  it("searches either name while keeping only matching package branches", () => {
    const tree = sourceTree(pairs, "before", "hidden.a");
    expect(tree.matches).toEqual(["class:first"]);
    expect(tree.roots.map((n) => n.name)).toEqual(["demo"]);
    expect(tree.children["package:before:demo"].map((n) => n.name)).toEqual([
      "nested",
    ]);
    expect(sourceTree(pairs, "before", "notfound").roots).toEqual([]);
  });
  it("uses transformed packages and manual mappings without losing removed classes", () => {
    const tree = sourceTree(pairs, "after", "", {
      first: identity("manual/Replacement"),
    });
    expect(tree.parents.get("class:first")).toBe("package:after:manual");
    expect(tree.nodes.get("class:first")?.identity?.name).toBe(
      "manual/Replacement",
    );
    expect(tree.nodes.get("class:root")?.status).toBe("removed");
    expect(
      sourceTree(pairs, "after", "manual.Replacement", {
        first: identity("manual/Replacement"),
      }).matches,
    ).toEqual(["class:first"]);
  });
});

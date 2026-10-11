import type { ClassIdentity, Pair } from "./types";

export interface TreeNode {
  id: string;
  name: string;
  package?: boolean;
  branch?: string;
  children?: boolean;
  superclass?: string;
  version?: string;
}
export function flattenTree<T extends TreeNode>(
  roots: T[],
  children: Record<string, T[]>,
  expanded: Set<string>,
  depth = 0,
): (T & { depth: number })[] {
  return roots.flatMap((node) => [
    { ...node, depth },
    ...(expanded.has(node.id)
      ? flattenTree(children[node.id] || [], children, expanded, depth + 1)
      : []),
  ]);
}

export interface SourceTreeNode extends TreeNode {
  pairId?: string;
  identity?: ClassIdentity;
  status?: string;
}

/** Index the selected run, so package navigation also works for renamed/imported classes. */
export function sourceTree(
  pairs: Pair[],
  side: "before" | "after",
  query = "",
  overrides: Record<string, ClassIdentity | undefined> = {},
) {
  const roots: SourceTreeNode[] = [];
  const children: Record<string, SourceTreeNode[]> = Object.create(null);
  const nodes = new Map<string, SourceTreeNode>();
  const parents = new Map<string, string>();
  const matches: string[] = [];
  const term = query.trim().toLowerCase().replaceAll("/", ".");
  for (const pair of pairs) {
    const after = Object.hasOwn(overrides, pair.id)
      ? overrides[pair.id]
      : pair.after;
    if (
      term &&
      ![pair.before?.name, after?.name].some((name) =>
        name?.replaceAll("/", ".").toLowerCase().includes(term),
      )
    )
      continue;
    const identity =
      side === "after" ? after || pair.before : pair.before || after;
    if (!identity) continue;
    const parts = identity.name.replaceAll(".", "/").split("/");
    const name = parts.pop()!;
    if (!parts.length) parts.push(""); // Explicit default-package branch.
    let parent = "",
      path = "";
    for (const part of parts) {
      path = path ? path + "/" + part : part;
      const id = "package:" + side + ":" + path;
      if (!nodes.has(id)) {
        const node = {
          id,
          name: part,
          branch: path,
          package: true,
          children: true,
        };
        nodes.set(id, node);
        children[id] = [];
        (parent ? children[parent] : roots).push(node);
        if (parent) parents.set(id, parent);
      }
      parent = id;
    }
    const id = "class:" + pair.id;
    const node = { id, name, identity, pairId: pair.id, status: pair.status };
    nodes.set(id, node);
    children[parent].push(node);
    parents.set(id, parent);
    matches.push(id);
  }
  const sort = (siblings: SourceTreeNode[]) =>
    siblings.sort(
      (a, b) =>
        Number(!!b.package) - Number(!!a.package) ||
        a.name.localeCompare(b.name) ||
        a.id.localeCompare(b.id),
    );
  sort(roots);
  Object.values(children).forEach(sort);
  return { roots, children, nodes, parents, matches };
}

import { readFile, readdir, mkdir, writeFile } from "node:fs/promises";
import { join } from "node:path";
const names = [
  "react",
  "react-dom",
  "scheduler",
  "monaco-editor",
  "lucide-react",
];
let result = "ZKM Web Workbench — frontend third-party notices\n\n";
for (const name of names) {
  const root = join("node_modules", name),
    pkg = JSON.parse(await readFile(join(root, "package.json"), "utf8"));
  result += `\n${name} ${pkg.version} (${pkg.license})\n${"=".repeat(72)}\n`;
  for (const file of await readdir(root))
    if (/^(license|notice|thirdpartynotices)/i.test(file))
      result += (await readFile(join(root, file), "utf8")) + "\n";
}
await mkdir("public", { recursive: true });
await writeFile("public/third-party-licenses.txt", result);

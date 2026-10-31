import { test, expect } from "@playwright/test";
import { spawn, execFileSync, ChildProcess } from "node:child_process";
import { resolve } from "node:path";
import { mkdir, writeFile } from "node:fs/promises";
let server: ChildProcess, url: string, fixtureJar: string;
test.beforeAll(async () => {
  const root = resolve("..");
  const home = resolve(root, "build/playwright/home-" + Date.now());
  await mkdir(home, { recursive: true });
  fixtureJar = resolve(home, "runtime fixture.jar");
  await writeFile(
    resolve(home, "RuntimeHello.java"),
    `public class RuntimeHello {
    public static void main(String[] args) throws Exception {
      System.out.println("Hello " + (args.length > 0 ? args[0] : "world"));
      System.out.println(new String(System.in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
      System.err.println("diagnostic output");
      System.out.println(new String(RuntimeHello.class.getResourceAsStream("/data.txt").readAllBytes()));
    }
  }`,
  );
  await writeFile(resolve(home, "data.txt"), "resource preserved");
  await writeFile(
    resolve(home, "Nested.java"),
    'package demo.tree; public class Nested { public static String value() { return "package tree"; } }',
  );
  execFileSync("javac", [
    "--release",
    "17",
    "-d",
    home,
    resolve(home, "RuntimeHello.java"),
    resolve(home, "Nested.java"),
  ]);
  execFileSync("jar", [
    "cfe",
    fixtureJar,
    "RuntimeHello",
    "-C",
    home,
    "RuntimeHello.class",
    "-C",
    home,
    "data.txt",
    "-C",
    home,
    "demo",
  ]);
  server = spawn(
    "java",
    [
      "-Dzkm.web.home=" + home,
      "-jar",
      resolve(root, "build/libs/zkm27.jar"),
      "--web",
      "--no-browser",
    ],
    { cwd: root },
  );
  url = await new Promise<string>((ok, no) => {
    let output = "";
    const timer = setTimeout(() => no(new Error(output)), 20000);
    server.stdout!.on("data", (data) => {
      output += data;
      const match = output.match(/http:\/\/127\.0\.0\.1:\d+\/#token=\w+/);
      if (match) {
        clearTimeout(timer);
        ok(match[0]);
      }
    });
    server.on("error", no);
  });
});
test.afterAll(() => {
  server?.kill();
});
test("retained workspaces, automatic source previews and runtime output", async ({
  page,
}) => {
  test.setTimeout(240000);
  const errors: string[] = [];
  const nav = (name: string) =>
    page.locator("nav").getByRole("button", { name, exact: true }).click();
  const jobs = () =>
    page.evaluate(async () => (await fetch("/api/v1/jobs")).json());
  const previews = async () =>
    (await jobs()).filter((j: any) => j.kind === "preview");
  const sourceStatus = () =>
    page
      .locator(".browser-workbench .diff-labels:visible")
      .getByText("Succeeded", { exact: true });
  page.on("pageerror", (e) => errors.push(e.message));
  const outside: string[] = [];
  await page.route("**/*", (route) => {
    const target = route.request().url();
    if (target.startsWith(new URL(url).origin + "/")) return route.continue();
    outside.push(target);
    return route.abort();
  });
  const projectDirectory = resolve(
    "../build/playwright/中文 project-" + Date.now(),
  );
  let nativeSelection: string | null = null;
  const pickerRequests: any[] = [],
    folderRequests: any[] = [],
    exportRequests: any[] = [];
  await page.route("**/api/v1/desktop/pick", (route) => {
    pickerRequests.push(route.request().postDataJSON());
    return route.fulfill({
      json: { path: nativeSelection, cancelled: nativeSelection === null },
    });
  });
  await page.route("**/api/v1/desktop/open", (route) => {
    folderRequests.push(route.request().postDataJSON());
    return route.fulfill({ json: { path: folderRequests.at(-1).path } });
  });
  await page.route("**/api/v1/outputs/export", (route) => {
    exportRequests.push(route.request().postDataJSON());
    return route.fulfill({
      json: {
        path: resolve("../build/playwright/exported.jar"),
        cancelled: false,
      },
    });
  });
  await page.goto(url);
  await expect(
    page.getByRole("heading", { name: "Make your code harder to unravel." }),
  ).toBeVisible();
  await page.screenshot({
    path: "../build/playwright/welcome-dark.png",
    fullPage: true,
    animations: "disabled",
  });
  await page.getByRole("button", { name: "Create project" }).click();
  await page.getByLabel("Name", { exact: true }).fill("Workbench test");
  await page
    .getByLabel("Project directory", { exact: true })
    .fill(projectDirectory);
  await page.getByRole("dialog").getByTitle("Browse", { exact: true }).click();
  await expect.poll(() => pickerRequests.length).toBe(1);
  expect(pickerRequests[0].directory).toBe(true);
  await expect(
    page.getByLabel("Project directory", { exact: true }),
  ).toHaveValue(projectDirectory);
  nativeSelection = projectDirectory;
  await page.getByRole("dialog").getByTitle("Browse", { exact: true }).click();
  await expect.poll(() => pickerRequests.length).toBe(2);
  await expect(page.locator(".file-list")).toHaveCount(0);
  await page.getByRole("button", { name: "Create", exact: true }).click();
  await expect(
    page.getByRole("heading", { name: "Project configuration" }),
  ).toBeVisible();
  await page.getByRole("button", { name: "中文", exact: true }).click();
  await expect(page.getByRole("heading", { name: "项目配置" })).toBeVisible();
  await page.getByRole("button", { name: "切换主题" }).click();
  await expect(page.locator("html")).toHaveAttribute("data-theme", "light");
  await page.screenshot({
    path: "../build/playwright/project-light-zh.png",
    fullPage: true,
    animations: "disabled",
  });
  await page.getByRole("button", { name: "EN", exact: true }).click();
  // Visit before any build exists; it must still discover later builds.
  await nav("Runtime output");
  await expect(page.getByLabel("Use paths from a build")).toHaveValue("latest");
  await expect(
    page.getByRole("button", { name: "Run both", exact: true }),
  ).toBeDisabled();
  await nav("Build Helper");
  await expect(
    page.getByRole("heading", { name: "What are we protecting?" }),
  ).toBeVisible();
  nativeSelection = resolve("../demo/SnakeGame.jar");
  await page
    .locator(".retained-page:not([hidden]) .paths")
    .first()
    .getByRole("button", { name: "Browse", exact: true })
    .click();
  await expect(
    page.locator(".retained-page:not([hidden]) .path-item code").first(),
  ).toHaveText(nativeSelection);
  expect(pickerRequests.at(-1).directory).toBe(false);
  nativeSelection = null;
  await page
    .locator(".retained-page:not([hidden]) .paths")
    .first()
    .getByRole("button", { name: "Choose directory", exact: true })
    .click();
  await expect.poll(() => pickerRequests.at(-1).directory).toBe(true);
  await expect(
    page
      .locator(".retained-page:not([hidden]) .paths")
      .first()
      .locator(".path-item"),
  ).toHaveCount(1);
  await page.getByRole("button", { name: "Continue", exact: true }).click();
  await expect(
    page.getByRole("heading", { name: "Remove unused code" }),
  ).toBeVisible();
  await page.screenshot({
    path: "../build/playwright/build-light.png",
    fullPage: true,
    animations: "disabled",
  });
  await page
    .locator("nav")
    .getByRole("button", { name: "Script studio" })
    .click();
  await expect(page.locator(".monaco-editor:visible")).toBeVisible();
  await page.getByRole("button", { name: "Validate", exact: true }).click();
  await expect(page.getByText("✓ Syntax valid", { exact: true })).toBeVisible({
    timeout: 20000,
  });
  await nav("Build Helper");
  await expect(
    page.getByRole("heading", { name: "Remove unused code" }),
  ).toBeVisible();
  await nav("Class browser");
  await expect(page.locator(".comparison-class").first()).toBeVisible({
    timeout: 30000,
  });
  await expect(sourceStatus()).toHaveCount(1, { timeout: 30000 });
  expect(await previews()).toHaveLength(0);
  const packageTree = page.getByRole("tree", {
    name: "Package tree",
    exact: true,
  });
  const defaultPackage = page.getByRole("treeitem", {
    name: "Default package",
    exact: true,
  });
  await expect(defaultPackage).toHaveAttribute("aria-expanded", "true");
  await defaultPackage.click();
  await expect(defaultPackage).toHaveAttribute("aria-expanded", "false");
  await expect(page.locator(".comparison-class")).toHaveCount(0);
  await nav("Build Helper");
  await nav("Class browser");
  await expect(defaultPackage).toHaveAttribute("aria-expanded", "false");
  await packageTree.press("ArrowRight");
  await expect(defaultPackage).toHaveAttribute("aria-expanded", "true");
  await page.getByPlaceholder("Find a class…").fill("Main");
  await page.locator(".comparison-class").first().click();
  const selected = await page
    .locator(".comparison-class.selected b")
    .textContent();
  await page.getByRole("button", { name: "Compare", exact: true }).click();
  await expect(sourceStatus()).toHaveCount(2, { timeout: 60000 });
  expect(await previews()).toHaveLength(1);
  await expect(page.locator(".comparison-class.selected b")).toHaveText(
    selected!,
  );
  await page.screenshot({
    path: "../build/playwright/integrated-comparison.png",
    fullPage: true,
  });
  await nav("Build Helper");
  await expect(
    page.getByRole("heading", { name: "Remove unused code" }),
  ).toBeVisible();
  await page
    .locator(".stepper")
    .getByRole("button", { name: "Obfuscation" })
    .click();
  await page.getByRole("button", { name: "Naming", exact: true }).click();
  await page
    .getByLabel("Randomize names", { exact: false })
    .selectOption("false");
  // The browser remains mounted but must not preview while it is hidden.
  expect(await previews()).toHaveLength(1);
  await nav("Class browser");
  await expect(sourceStatus()).toHaveCount(2, { timeout: 60000 });
  await expect.poll(async () => (await previews()).length).toBe(2);
  await expect(page.getByPlaceholder("Find a class…")).toHaveValue("Main");
  await page.getByRole("button", { name: "Obfuscated", exact: true }).click();
  await expect(sourceStatus()).toHaveCount(1, { timeout: 60000 });
  expect(await previews()).toHaveLength(2);
  await page.getByRole("tab", { name: "Members & editing" }).click();
  await expect(page.locator(".class-title h3")).not.toBeEmpty();
  await page.getByPlaceholder("Search classes…").fill("Main");
  await page.locator(".class-row").first().click();
  await page.getByRole("button", { name: /^Methods/ }).click();
  await nav("Build Helper");
  await expect(
    page.getByRole("heading", { name: "Protection settings" }),
  ).toBeVisible();
  await nav("Class browser");
  await expect(
    page.getByRole("tab", { name: "Members & editing" }),
  ).toHaveAttribute("aria-selected", "true");
  await expect(page.getByPlaceholder("Search classes…")).toHaveValue("Main");
  await expect(page.getByRole("button", { name: /^Methods/ })).toHaveClass(
    "active",
  );
  // Change inputs through Build Helper, then let the source view generate its own preview.
  await nav("Build Helper");
  await page
    .locator(".stepper")
    .getByRole("button", { name: "Inputs" })
    .click();
  await page
    .locator(".retained-page:not([hidden]) .paths")
    .first()
    .getByTitle("Remove")
    .click();
  await page
    .getByPlaceholder("File or directory path")
    .filter({ visible: true })
    .first()
    .fill(fixtureJar);
  await page
    .getByPlaceholder("File or directory path")
    .filter({ visible: true })
    .first()
    .press("Enter");
  await nav("Class browser");
  await page.getByRole("tab", { name: "Source code" }).click();
  await page.getByPlaceholder("Find a class…").fill("RuntimeHello");
  await expect.poll(async () => (await previews()).length).toBe(3);
  await expect(sourceStatus()).toHaveCount(1, { timeout: 60000 });
  await page.getByRole("button", { name: "Compare", exact: true }).click();
  await expect(sourceStatus()).toHaveCount(2, { timeout: 60000 });
  await page.getByLabel("Decompiler").selectOption("cfr");
  await expect(sourceStatus()).toHaveCount(2, { timeout: 60000 });
  // Restored nested package tree selects the same run's decompiled class.
  await page.getByPlaceholder("Find a class…").fill("");
  const demoPackage = page.getByRole("treeitem", { name: "demo", exact: true });
  const treePackage = page.getByRole("treeitem", {
    name: "demo.tree",
    exact: true,
  });
  const nestedClass = page.getByRole("treeitem", {
    name: "demo.tree.Nested",
    exact: true,
  });
  await demoPackage.click();
  await expect(treePackage).toBeVisible();
  await packageTree.press("ArrowRight");
  await packageTree.press("ArrowRight");
  await expect(nestedClass).toBeVisible();
  await nestedClass.click();
  await expect(nestedClass).toHaveAttribute("aria-selected", "true");
  await expect(sourceStatus()).toHaveCount(2, { timeout: 60000 });
  await expect(
    page.locator(".comparison-main .editor.original .view-lines"),
  ).toContainText("class Nested");
  await page.screenshot({
    path: "../build/playwright/package-tree.png",
    fullPage: true,
  });
  await nav("Build Helper");
  await nav("Class browser");
  await expect(treePackage).toHaveAttribute("aria-expanded", "true");
  await expect(nestedClass).toHaveAttribute("aria-selected", "true");
  await demoPackage.click();
  await expect(nestedClass).toHaveCount(0);
  await page.getByPlaceholder("Find a class…").fill("demo.tree.Nested");
  await expect(nestedClass).toBeVisible();
  await expect(demoPackage).toHaveAttribute("aria-expanded", "true");
  await page.getByPlaceholder("Find a class…").fill("RuntimeHello");
  await page.locator(".comparison-class").first().click();
  await nav("Runtime output");
  await expect(
    page.getByRole("button", { name: "Run both", exact: true }),
  ).toBeEnabled();
  await page
    .getByLabel("Program arguments", { exact: false })
    .fill("a b; literal");
  await page
    .getByLabel("Standard input (EOF after text)", { exact: true })
    .fill("中文 input");
  await nav("Build Helper");
  await nav("Runtime output");
  await expect(
    page.getByLabel("Program arguments", { exact: false }),
  ).toHaveValue("a b; literal");
  const sourceSelect = page.getByLabel("Use paths from a build");
  const selectedSource = sourceSelect.locator("option:checked");
  const previousPreview = (await previews()).find(
    (j: any) => j.status === "succeeded",
  );
  await expect(selectedSource).toContainText(previousPreview.id.slice(0, 6));
  const build = async () => {
    const job = await page.evaluate(async () =>
      (
        await fetch("/api/v1/jobs", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ kind: "build" }),
        })
      ).json(),
    );
    await expect
      .poll(
        async () => (await jobs()).find((j: any) => j.id === job.id)?.status,
        { timeout: 30000 },
      )
      .toBe("succeeded");
    return job;
  };
  // New builds while away from this page must replace paths on return.
  await nav("Build Helper");
  const builtAway = await build();
  await nav("Runtime output");
  await expect(selectedSource).toContainText(builtAway.id.slice(0, 6));
  const config = await page.evaluate(
    async (id) => (await fetch("/api/v1/runtime/config?jobId=" + id)).json(),
    builtAway.id,
  );
  await expect(
    page.locator(".runtime-page .paths").nth(1).locator("code").first(),
  ).toHaveText(config.after[0]);
  // Explicit history selection is pinned until automatic mode is restored.
  await sourceSelect.selectOption(previousPreview.id);
  await expect(sourceSelect).toBeEnabled();
  const builtPinned = await build();
  await nav("Project");
  await nav("Runtime output");
  await expect(sourceSelect).toHaveValue(previousPreview.id);
  await sourceSelect.selectOption("latest");
  await expect(selectedSource).toContainText(builtPinned.id.slice(0, 6));
  await expect(sourceSelect).toBeEnabled();
  const builtVisible = await build();
  await expect(selectedSource).toContainText(builtVisible.id.slice(0, 6));
  await expect(
    page.getByLabel("Program arguments", { exact: false }),
  ).toHaveValue("a b; literal");
  await expect(
    page.getByRole("textbox", {
      name: "Standard input (EOF after text)",
      exact: true,
    }),
  ).toHaveValue("中文 input");
  // Project shortcuts use desktop APIs, and the export action selects the latest completed artifact.
  await nav("Project");
  const projectActions = page.locator(
    ".retained-page:not([hidden]) .project-actions",
  );
  await projectActions
    .getByRole("button", { name: "Open project folder", exact: true })
    .click();
  await expect.poll(() => folderRequests.at(-1)?.path).toBe(projectDirectory);
  await projectActions
    .getByRole("button", { name: "Open output folder", exact: true })
    .click();
  const outputRoots = await page.evaluate(
    async (id) => (await fetch("/api/v1/outputs?jobId=" + id)).json(),
    builtVisible.id,
  );
  await expect
    .poll(() => folderRequests.at(-1)?.path)
    .toBe(outputRoots.roots[0]);
  await projectActions
    .getByRole("button", { name: "Export finished JAR", exact: true })
    .click();
  await expect.poll(() => exportRequests.length).toBe(1);
  expect(exportRequests[0].jobId).toBe(builtVisible.id);
  expect(exportRequests[0].path).toContain("runtime fixture.jar");
  await page.screenshot({
    path: "../build/playwright/project-file-actions.png",
    fullPage: true,
  });
  await nav("Runtime output");
  await page.getByRole("button", { name: "Run both", exact: true }).click();
  await expect(
    page
      .locator(".runtime-page .diff-labels small")
      .filter({ hasText: "Exit 0" }),
  ).toHaveCount(2, { timeout: 30000 });
  await expect(
    page.locator(".runtime-results .editor.original .view-lines").first(),
  ).toContainText("Hello a b; literal");
  await expect(
    page.locator(".runtime-results .editor.original .view-lines").first(),
  ).toContainText("resource preserved");
  const programs = (await jobs()).filter((j: any) => j.kind === "programRun");
  expect(programs).toHaveLength(2);
  expect(programs[0].result.stdout).toEqual(programs[1].result.stdout);
  expect(programs[0].result.stderr).toEqual(programs[1].result.stderr);
  await page.getByRole("button", { name: "stderr", exact: true }).click();
  await expect(
    page.locator(".runtime-results .editor.original .view-lines").first(),
  ).toContainText("diagnostic output");
  await page.screenshot({
    path: "../build/playwright/runtime-output.png",
    fullPage: true,
  });
  await page
    .getByRole("button", { name: "Launch settings", exact: true })
    .click();
  await page
    .getByLabel("Original main class", { exact: true })
    .fill("MissingMain");
  await page.getByRole("button", { name: "Run both", exact: true }).click();
  await expect(
    page
      .locator(".runtime-page .diff-labels small")
      .filter({ hasText: "Exit 1" }),
  ).toHaveCount(1, { timeout: 30000 });
  await expect(
    page
      .locator(".runtime-page .diff-labels small")
      .filter({ hasText: "Exit 0" }),
  ).toHaveCount(1, { timeout: 30000 });
  await nav("Class browser");
  // A failed automatic preview must clear stale results and wait for an explicit retry.
  await nav("Build Helper");
  await page
    .locator(".retained-page:not([hidden]) .paths")
    .first()
    .getByTitle("Remove")
    .click();
  await page
    .getByPlaceholder("File or directory path")
    .filter({ visible: true })
    .first()
    .fill(fixtureJar + ".missing");
  await page
    .getByPlaceholder("File or directory path")
    .filter({ visible: true })
    .first()
    .press("Enter");
  await nav("Class browser");
  await expect(page.locator(".error-banner")).toBeVisible({ timeout: 30000 });
  await expect(sourceStatus()).toHaveCount(0);
  expect(await previews()).toHaveLength(4);
  await nav("Tools");
  await nav("Class browser");
  expect(await previews()).toHaveLength(4);
  await page
    .locator(".error-banner")
    .getByRole("button", { name: "Retry", exact: true })
    .click();
  await expect.poll(async () => (await previews()).length).toBe(5);
  await expect(page.locator(".error-banner")).toBeVisible({ timeout: 30000 });
  await nav("Build Helper");
  await page
    .locator(".retained-page:not([hidden]) .paths")
    .first()
    .getByTitle("Remove")
    .click();
  await page
    .getByPlaceholder("File or directory path")
    .filter({ visible: true })
    .first()
    .fill(fixtureJar);
  await page
    .getByPlaceholder("File or directory path")
    .filter({ visible: true })
    .first()
    .press("Enter");
  await nav("Class browser");
  await expect(sourceStatus()).toHaveCount(2, { timeout: 30000 });
  expect(await previews()).toHaveLength(5);
  await page.getByRole("button", { name: "中文", exact: true }).click();
  await page.getByRole("button", { name: "切换主题" }).click();
  await page.setViewportSize({ width: 1100, height: 850 });
  await page.screenshot({
    path: "../build/playwright/comparison-dark-zh.png",
    fullPage: true,
    animations: "disabled",
  });
  await page.getByRole("button", { name: "保存项目", exact: true }).click();
  await page.reload();
  await expect(page.getByRole("heading", { name: "项目配置" })).toBeVisible();
  expect(errors).toEqual([]);
  expect(outside).toEqual([]);
});

test("custom naming editor, TXT import, saved settings and build", async ({
  page,
}) => {
  const errors: string[] = [];
  page.on("pageerror", (e) => errors.push(e.message));
  await page.goto(url);
  await expect(
    page.locator("nav").getByRole("button", { name: "Project", exact: true }),
  ).toBeVisible();
  const directory = resolve("../build/playwright/naming-" + Date.now());
  await page.evaluate(
    async ({ directory, fixtureJar }) => {
      const project = await (
        await fetch("/api/v1/projects/create", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ directory, name: "Custom naming" }),
        })
      ).json();
      project.build.inputs = [fixtureJar];
      project.build.obfuscation = {
        obfuscateFlow: "none",
        encryptStringLiterals: "none",
        randomize: "false",
      };
      await fetch("/api/v1/project", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(project),
      });
    },
    { directory, fixtureJar },
  );
  await page.reload();
  const naming = async () => {
    await page
      .locator("nav")
      .getByRole("button", { name: "Build Helper", exact: true })
      .click();
    await page
      .locator(".stepper")
      .getByRole("button", { name: "Obfuscation" })
      .click();
    await page.getByRole("button", { name: "Naming", exact: true }).click();
  };
  await naming();
  await page
    .getByRole("combobox", { name: "Name source", exact: true })
    .selectOption("editor");
  await page
    .getByRole("textbox", { name: /^Name dictionary/ })
    .fill("迷雾\nEcho\n迷雾\n# comment");
  await expect(page.getByText("2 unique names")).toBeVisible();
  await page.getByLabel("Generated name length", { exact: false }).fill("8");
  await page.getByLabel("Custom character set", { exact: false }).fill("Il");
  await page.getByRole("tab", { name: "Method names", exact: true }).click();
  await page
    .getByLabel("Import name dictionary", { exact: true })
    .setInputFiles({
      name: "methods.txt",
      mimeType: "text/plain",
      buffer: Buffer.from("\uFEFF方法甲\r\n方法乙\r\n方法甲"),
    });
  await expect(
    page.getByRole("textbox", { name: /^Name dictionary/ }),
  ).toHaveValue("方法甲\r\n方法乙\r\n方法甲".replaceAll("\r\n", "\n"));
  await expect(page.getByText("2 unique names")).toBeVisible();
  const download = page.waitForEvent("download");
  await page.getByRole("button", { name: "Export TXT", exact: true }).click();
  expect((await download).suggestedFilename()).toBe("method-names.txt");
  await page
    .getByRole("tab", { name: "Local variable names", exact: true })
    .click();
  await page.getByLabel("Generated name length", { exact: false }).fill("6");
  await expect(
    page.getByRole("combobox", { name: "Debug name handling", exact: true }),
  ).toHaveValue("obfuscate");
  await page.getByRole("tab", { name: "Field names", exact: true }).click();
  await page
    .getByRole("combobox", { name: "Name source", exact: true })
    .selectOption("file");
  await page
    .getByLabel("TXT dictionary path", { exact: false })
    .fill("fields.txt");
  await writeFile(resolve(directory, "fields.txt"), "字段甲\n字段乙\n");
  await page.getByRole("button", { name: "Save project", exact: true }).click();
  await expect
    .poll(async () =>
      page.evaluate(
        async () =>
          (await (await fetch("/api/v1/state")).json()).project.build
            .obfuscation.newClassNameLength,
      ),
    )
    .toBe("8");
  await page.reload();
  await naming();
  await expect(
    page.getByRole("textbox", { name: /^Name dictionary/ }),
  ).toHaveValue("迷雾\nEcho\n迷雾\n# comment");
  await expect(
    page.getByLabel("Generated name length", { exact: false }),
  ).toHaveValue("8");
  await expect(
    page.getByLabel("Custom character set", { exact: false }),
  ).toHaveValue("Il");
  await page.getByRole("tab", { name: "Method names", exact: true }).click();
  await expect(
    page.getByRole("textbox", { name: /^Name dictionary/ }),
  ).toContainText("方法乙");
  await page.screenshot({
    path: "../build/playwright/custom-naming.png",
    fullPage: true,
  });
  // Opening the unified editor must leave existing category settings untouched.
  await page
    .getByRole("tab", { name: "Unified settings", exact: true })
    .click();
  await expect(
    page.getByRole("textbox", { name: /^Name dictionary/ }),
  ).toHaveValue("方法甲\n方法乙\n方法甲");
  await page.getByLabel("Generated name length", { exact: false }).fill("12");
  await page.getByRole("tab", { name: "Class names", exact: true }).click();
  await expect(
    page.getByLabel("Generated name length", { exact: false }),
  ).toHaveValue("8");
  // Applying a file source clears stale inline dictionaries.
  await page.getByRole("tab", { name: "Field names", exact: true }).click();
  await page
    .getByRole("tab", { name: "Unified settings", exact: true })
    .click();
  await expect(
    page.getByLabel("TXT dictionary path", { exact: false }),
  ).toHaveValue("fields.txt");
  await page.getByRole("button", { name: "Apply to all", exact: true }).click();
  await page.getByRole("tab", { name: "Class names", exact: true }).click();
  await expect(
    page.getByRole("combobox", { name: "Name source", exact: true }),
  ).toHaveValue("file");
  // Imported text, length and alphabet apply to all six kinds and are persisted normally.
  await page
    .getByRole("tab", { name: "Unified settings", exact: true })
    .click();
  await page
    .getByLabel("Import name dictionary", { exact: true })
    .setInputFiles({
      name: "shared.txt",
      mimeType: "text/plain",
      buffer: Buffer.from("共用甲\n共用乙"),
    });
  await expect(
    page.getByRole("textbox", { name: /^Name dictionary/ }),
  ).toHaveValue("共用甲\n共用乙");
  await page.getByLabel("Generated name length", { exact: false }).fill("12");
  await page.getByLabel("Custom character set", { exact: false }).fill("xyz");
  await page.getByRole("button", { name: "Apply to all", exact: true }).click();
  for (const name of [
    "Package names",
    "Class names",
    "Method names",
    "Field names",
    "Local variable names",
    "Parameter names",
  ]) {
    await page.getByRole("tab", { name, exact: true }).click();
    await expect(
      page.getByRole("textbox", { name: /^Name dictionary/ }),
    ).toHaveValue("共用甲\n共用乙");
    await expect(
      page.getByLabel("Generated name length", { exact: false }),
    ).toHaveValue("12");
  }
  await page.getByRole("tab", { name: "Field names", exact: true }).click();
  await page.getByLabel("Generated name length", { exact: false }).fill("10");
  const savedResponse = page.waitForResponse(
    (r) =>
      r.url().endsWith("/api/v1/project") && r.request().method() === "POST",
  );
  await page.getByRole("button", { name: "Save project", exact: true }).click();
  const saved = await (await savedResponse).json();
  for (const kind of [
    "Package",
    "Class",
    "Method",
    "Field",
    "LocalVariable",
    "Parameter",
  ]) {
    expect(saved.build.obfuscation[`new${kind}Names`]).toBe("共用甲\n共用乙");
    expect(saved.build.obfuscation[`new${kind}NameLength`]).toBe(
      kind === "Field" ? "10" : "12",
    );
    expect(saved.build.obfuscation[`new${kind}NameCharacters`]).toBe("xyz");
    expect(saved.build.obfuscation[`new${kind}NameFile`]).toBeUndefined();
  }
  expect(saved.build.obfuscation.localVariables).toBe("obfuscate");
  expect(saved.build.obfuscation.methodParameters).toBe("obfuscate");
  await page.reload();
  await naming();
  await expect(
    page.getByLabel("Generated name length", { exact: false }),
  ).toHaveValue("12");
  await page.getByRole("tab", { name: "Field names", exact: true }).click();
  await expect(
    page.getByLabel("Generated name length", { exact: false }),
  ).toHaveValue("10");
  const job = await page.evaluate(async () =>
    (
      await fetch("/api/v1/jobs", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ kind: "build" }),
      })
    ).json(),
  );
  await expect
    .poll(
      async () =>
        page.evaluate(
          async (id) =>
            (await (await fetch("/api/v1/jobs")).json()).find(
              (j: any) => j.id === id,
            )?.status,
          job.id,
        ),
      { timeout: 30000 },
    )
    .toBe("succeeded");
  await page.getByRole("button", { name: "中文", exact: true }).click();
  await expect(
    page.getByText("自定义名称与长度", { exact: true }),
  ).toBeVisible();
  await page.getByRole("button", { name: "切换主题" }).click();
  await page.getByRole("tab", { name: "统一设置", exact: true }).click();
  await expect(
    page.getByRole("button", { name: "应用到全部", exact: true }),
  ).toBeVisible();
  await page.screenshot({
    path: "../build/playwright/custom-naming-zh.png",
    fullPage: true,
  });
  expect(errors).toEqual([]);
});

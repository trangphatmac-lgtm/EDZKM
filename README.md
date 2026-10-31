<div align="center">


# EDZKM

<p><strong>An Open Source & enhanced implementation of ZelixKlassMaster™ Java obfuscation.</strong></p>
</div>

## Features

- **Project workspace** — manage JARs, class directories, dependencies, build settings, scripts, and run history in one place.
- **Build Helper** — configure trimming, name exclusions, obfuscation, and output options; preview the generated script or run the build.
- **Custom naming** — edit name dictionaries in the browser, import/export UTF-8 TXT, or use a dictionary path; set a character set and fixed generated length for packages, classes, methods, fields, local variables, and parameters.
- **Class browser** — explore package and inheritance trees, inspect bytecode, and edit supported class properties, fields, method modifiers, and constants.
- **Source comparison** — view original and obfuscated classes side by side using bundled CFR 0.152 or Vineflower 1.12.0, with ChangeLog mapping and manual pairing.
- **Script studio** — edit multiple ZKM scripts with Monaco, syntax highlighting, completion, validation, and execution logs.
- **Runtime output comparison** — run original and obfuscated programs with configurable arguments, standard input, working directory, and timeouts; compare stdout, stderr, and exit codes.
- **Developer tools** — translate stack traces with ChangeLogs and convert ProGuard configurations into scripts.
- **Local desktop integration** — browse files with native dialogs, open project/output folders, and export built JARs.
- **English and Simplified Chinese** — switch languages and choose light, dark, or system theme.

The packaged application includes the frontend, editor, decompilers, and runtime dependencies. Running EDZKM itself requires no Node.js installation or internet connection.

## Requirements

| Purpose | Requirements |
| --- | --- |
| Run the packaged application | Java 17 or newer |
| Build from source | JDK 17+, Node.js 22+, and npm |
| Use the Web interface | A modern browser on the same machine |
| Run the smoke tests | Python 3 and a JDK |
| Run browser tests | Playwright Chromium; see [Testing](#testing) |

On Linux, native file dialogs require `zenity` or `kdialog`, and opening folders requires `xdg-open`. Paths can also be entered directly.

## Build

From the repository root:

```sh
sh gradlew jar
```

On Windows:

```powershell
.\gradlew.bat jar
```

Gradle installs the locked frontend dependencies, builds the Web UI, compiles the Java code, and packages the application and its dependencies into:

```text
build/libs/zkm27.jar
```

Use this bundled JAR for distribution. The `-thin.jar` is not the standalone application. The first build needs access to the Gradle, Maven, and npm dependency downloads.

## Run

### Web workbench (default)

```sh
java -jar build/libs/zkm27.jar
```

The application starts a local server and opens the browser automatically. To use a fixed port without opening a browser:

```sh
java -jar build/libs/zkm27.jar --web --port 8270 --no-browser
```

Open the full URL printed in the terminal, including its `#token=` fragment. The server binds to `127.0.0.1` and chooses an available port by default. Closing the browser tab leaves the application running; use the workbench's quit button to stop its tasks and server.

You can also launch it from Gradle:

```sh
sh gradlew run
```

### Swing desktop interface

```sh
java -jar build/libs/zkm27.jar --swing
```

The Web settings page can also launch Swing in a separate process.

### Command-line scripts

```sh
java -jar build/libs/zkm27.jar script.zkm
```

To validate a script without executing it:

```sh
java -jar build/libs/zkm27.jar -p script.zkm
```

EDZKM retains the existing ZKM script syntax, `.zkm` files, Java package names, configuration keys, and `zkm27.jar` filename for compatibility. The user-facing application name is EDZKM.

## Typical workflow

1. Create or open a project, add your input JARs or class directories, and configure dependencies.
2. Use **Build Helper** to set trimming, inclusion/exclusion rules, obfuscation options, and the output directory.
3. Open **Class browser** to inspect the original code. Select an obfuscated or comparison view to preview the current configuration, or select an existing run.
4. Build the project, inspect its logs and mappings, and export the resulting JAR from the project page or output step.
5. Use **Runtime output** to compare the behavior of original and obfuscated programs.
6. Send the generated configuration to **Script studio** when you need more control or repeatable command-line builds.

Preview output is stored under the run directory. Script `execute` statements retain their normal behavior during both previews and regular builds.

## Custom names and length

In **Build Helper → Obfuscation → Naming → Custom names & length**, select a name category and choose automatic generation, the built-in dictionary editor, or a TXT file path. **Import TXT** copies the file contents into the editor; imported text and all naming settings are saved with the project. **Export TXT** saves the editor contents as UTF-8. File paths can be absolute or relative to the project directory.

**Unified settings** starts from the category currently selected. Edit the source, dictionary, character set and length, then click **Apply to all** to copy those settings to all six categories and enable local-variable/parameter name obfuscation. Switching to this tab alone does not change the project. You can still adjust individual categories afterwards; save the project to retain the applied settings.

Use one complete Java identifier per line, including Chinese identifiers. The editor ignores blank lines, duplicate names, and lines beginning with `#` or `//`; imported files may include a UTF-8 BOM. Invalid editor entries are reported with their line number during a build. The editor/import limit is 1 MiB and 128 characters per name.

Dictionaries are consumed first without padding or truncation. When a dictionary runs out, generated names use the optional character set and fixed length (1–128, blank for the original automatic behavior). A custom character set accepts BMP Java identifier characters and must contain a valid starting character. Length applies to the generated name segment, excluding `newNamesPrefix`, the package path, and enclosing class names. A fixed-length namespace that runs out fails with an error; increase its length or character set. Existing exclusions, mappings, and name prefixes continue to apply.

Changing a local-variable or parameter naming setting selects **Obfuscate names** automatically. These settings only rename debug tables that exist in the input (`javac -g` for local variables, `javac -parameters` for parameter metadata). A decompiler may display names it generates itself.

Generated scripts retain these settings and can run through EDZKM's command-line launcher. For example:

```text
obfuscate
    newClassNames="迷雾\n星河"
    newClassNameLength=12
    newClassNameCharacters="Il"
    newMethodNameFile="methods.txt"
    newMethodNameLength=8
    localVariables=obfuscate
    newLocalVariableNames="变量甲\n变量乙"
    methodParameters=obfuscate
    newParameterNameLength=6;
```

The new `new{Kind}Names`, `new{Kind}NameLength`, and `new{Kind}NameCharacters` options support `Package`, `Class`, `Method`, `Field`, `LocalVariable`, and `Parameter`. The latter two also support `newLocalVariableNameFile` and `newParameterNameFile`. These additions are EDZKM extensions to the original script syntax.

## Project data

Each project stores its settings in `project.zkm.json`. Supporting directories include:

```text
scripts/       Saved scripts
imports/       Copies of files imported by drag and drop
work/          Recoverable working copies after edits and transformations
runs/          Per-run settings, scripts, logs, mappings, and bytecode snapshots
```

Recent projects and decompiler caches are stored in `~/.zkm-workbench`. Override that location when needed:

```sh
java -Dzkm.web.home=/path/to/workbench-data -jar build/libs/zkm27.jar
```

Editing classes in the Web UI updates a project working copy. Files outside the project remain referenced by their paths; relocate missing inputs and dependencies when moving a project to another machine.

## Development and testing

Open the repository as a Gradle project in IntelliJ IDEA, select JDK 17 or newer, and run the included **start** configuration. It builds and starts the packaged application.

### Testing

Run the Java and frontend tests, then exercise the packaged launcher and engine:

```sh
sh gradlew test jar
npm --prefix web test
python3 tests/test_launcher.py
python3 tests/run_smoke.py
```

Run the browser integration tests after building the JAR:

```sh
cd web
npx playwright install chromium
npm run test:e2e
```

The [CI workflow](.github/workflows/web-workbench.yml) defines checks for Windows, macOS, and Linux on Java 17 and 21.


The settings page also includes bundled engine help and third-party license information. JAR dependency notices are packaged under `META-INF/licenses/`.

## Project background and credits

This project contains reconstructed and modified code derived through decompilation. Behavioral differences and bugs may remain; bug reports and contributions are welcome.

- Original software: **Zelix KlassMaster 27.0.0**.
- Community reconstruction and deobfuscation work: **NoHackClient**.
- Symbol recovery, renaming, and fixes: human-supervised work with **Claude** and **Codex**.
- Supporting tools: **Java Deobfuscator** and **Recaf**.
- Web interface and inspection tools: **React**, **Monaco Editor**, **CFR**, and **Vineflower**, with their respective license notices included in the application.

EDZKM is an independent community project. It is not an official Zelix product and is not affiliated with or endorsed by Zelix Pty Ltd. Existing copyright notices and third-party license attributions remain with their respective owners.

###### "ZELIX" "KLASSMASTER" are trademarks owned by Zelix Pty Ltd (PO BOX 6016, Narraweena NSW 2099, Australia). All trademarks belong to their original owner.
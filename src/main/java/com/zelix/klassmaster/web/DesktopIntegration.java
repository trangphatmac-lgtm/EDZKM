package com.zelix.klassmaster.web;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Native desktop dialogs. Paths are passed as arguments/environment, never as script source. */
public class DesktopIntegration implements AutoCloseable {
  private final Set<Process> processes = ConcurrentHashMap.newKeySet();
  private final AtomicBoolean choosing = new AtomicBoolean();
  private volatile boolean closed;

  static final String APPLE_PICKER = """
      on run argv
        set pickerMode to item 1 of argv
        set pickerTitle to item 2 of argv
        set initialFolder to POSIX file (item 3 of argv)
        set suggestedName to item 4 of argv
        try
          activate
          if pickerMode is "directory" then
            set chosen to choose folder with prompt pickerTitle default location initialFolder
          else if pickerMode is "save" then
            set chosen to choose file name with prompt pickerTitle default location initialFolder default name suggestedName
          else
            set chosen to choose file with prompt pickerTitle default location initialFolder
          end if
          return POSIX path of chosen
        on error number -128
          return ""
        end try
      end run
      """;

  static final String WINDOWS_PICKER = """
      $ErrorActionPreference = 'Stop'
      [Console]::OutputEncoding = New-Object System.Text.UTF8Encoding($false)
      Add-Type -AssemblyName System.Windows.Forms
      if ($env:ZKM_PICKER_MODE -eq 'directory') {
        $dialog = New-Object System.Windows.Forms.FolderBrowserDialog
        $dialog.Description = $env:ZKM_PICKER_TITLE
        $dialog.SelectedPath = $env:ZKM_PICKER_START
        $dialog.ShowNewFolderButton = $true
      } else {
        if ($env:ZKM_PICKER_MODE -eq 'save') {
          $dialog = New-Object System.Windows.Forms.SaveFileDialog
          $dialog.FileName = $env:ZKM_PICKER_NAME
          $dialog.Filter = 'JAR (*.jar)|*.jar'
          $dialog.DefaultExt = 'jar'
          $dialog.AddExtension = $true
          $dialog.OverwritePrompt = $true
        } else { $dialog = New-Object System.Windows.Forms.OpenFileDialog }
        $dialog.Title = $env:ZKM_PICKER_TITLE
        $dialog.InitialDirectory = $env:ZKM_PICKER_START
      }
      try {
        if ($dialog.ShowDialog() -eq [System.Windows.Forms.DialogResult]::OK) {
          if ($env:ZKM_PICKER_MODE -eq 'directory') { [Console]::WriteLine($dialog.SelectedPath) }
          else { [Console]::WriteLine($dialog.FileName) }
        }
      } finally { $dialog.Dispose() }
      """;

  public Path choose(String mode, String title, Path start, String name) throws Exception {
    if (!Set.of("file", "directory", "save").contains(mode))
      throw new IllegalArgumentException("Invalid picker mode");
    if (!choosing.compareAndSet(false, true))
      throw new ProjectStore.Conflict("A system file dialog is already open");
    try {
      Path directory = existingDirectory(start);
      String os = System.getProperty("os.name");
      ProcessBuilder builder;
      if (os.startsWith("Mac")) {
        builder = new ProcessBuilder("/usr/bin/osascript", "-e", APPLE_PICKER, mode, title, directory.toString(), name);
      } else if (os.startsWith("Windows")) {
        String powershell = Path.of(System.getenv().getOrDefault("SystemRoot", "C:\\Windows"),
            "System32", "WindowsPowerShell", "v1.0", "powershell.exe").toString();
        builder = new ProcessBuilder(powershell, "-NoProfile", "-STA", "-NonInteractive", "-EncodedCommand",
            Base64.getEncoder().encodeToString(WINDOWS_PICKER.getBytes(StandardCharsets.UTF_16LE)));
        builder.environment().putAll(Map.of("ZKM_PICKER_MODE", mode, "ZKM_PICKER_TITLE", title,
            "ZKM_PICKER_START", directory.toString(), "ZKM_PICKER_NAME", name));
      } else {
        boolean kde = System.getenv().getOrDefault("XDG_CURRENT_DESKTOP", "").toLowerCase(Locale.ROOT).contains("kde");
        String zenity = executable("zenity"), kdialog = executable("kdialog");
        if (kdialog != null && (kde || zenity == null)) {
          builder = new ProcessBuilder(kdialog, "--title", title,
              mode.equals("directory") ? "--getexistingdirectory" : mode.equals("save") ? "--getsavefilename" : "--getopenfilename",
              mode.equals("save") ? directory.resolve(name).toString() : directory.toString());
        } else if (zenity != null) {
          List<String> args = new ArrayList<>(List.of(zenity, "--file-selection", "--title=" + title,
              "--filename=" + (mode.equals("save") ? directory.resolve(name).toString() : directory + File.separator)));
          if (mode.equals("directory")) args.add("--directory");
          if (mode.equals("save")) args.addAll(List.of("--save", "--confirm-overwrite"));
          builder = new ProcessBuilder(args);
        } else throw new IOException("System file dialog unavailable: install zenity or kdialog, or enter a path directly");
      }
      String value = execute(builder, true);
      if (value.isEmpty()) return null;
      Path result = Path.of(value).toAbsolutePath().normalize();
      if (mode.equals("directory") && !Files.isDirectory(result)) throw new IOException("Not a directory: " + result);
      if (mode.equals("file") && !Files.isRegularFile(result)) throw new IOException("Not a file: " + result);
      return result;
    } finally { choosing.set(false); }
  }

  public void openFolder(Path path) throws Exception {
    Path directory = path.toAbsolutePath().normalize();
    if (!Files.isDirectory(directory)) throw new FileNotFoundException("Folder does not exist: " + directory);
    String os = System.getProperty("os.name");
    List<String> command;
    if (os.startsWith("Mac")) command = List.of("/usr/bin/open", directory.toString());
    else if (os.startsWith("Windows")) command = List.of(Path.of(System.getenv().getOrDefault("SystemRoot", "C:\\Windows"), "explorer.exe").toString(), directory.toString());
    else {
      String opener = executable("xdg-open");
      if (opener == null) throw new IOException("System file manager unavailable: xdg-open not found");
      command = List.of(opener, directory.toString());
    }
    // A file manager belongs to the desktop, so do not terminate it on workbench shutdown.
    Process process = new ProcessBuilder(command).redirectOutput(ProcessBuilder.Redirect.DISCARD)
        .redirectError(ProcessBuilder.Redirect.DISCARD).start();
    if (process.waitFor(2, TimeUnit.SECONDS) && process.exitValue() != 0 && !os.startsWith("Windows"))
      throw new IOException("Could not open the system file manager: " + directory);
  }

  private Process launch(ProcessBuilder builder) throws IOException {
    if (closed) throw new IOException("Application is closing");
    Process process = builder.start();
    processes.add(process);
    if (closed) { process.destroyForcibly(); throw new IOException("Application is closing"); }
    return process;
  }

  private String execute(ProcessBuilder builder, boolean dialog) throws Exception {
    Path errors = Files.createTempFile("zkm-desktop-", ".log");
    Process process = null;
    try {
      process = launch(builder.redirectError(errors.toFile()));
      String output;
      try (InputStream stream = process.getInputStream()) {
        output = new String(stream.readNBytes(65537), StandardCharsets.UTF_8);
        if (output.length() > 65536) throw new IOException("Unexpected system dialog response");
      }
      int exit = process.waitFor();
      if (exit == 1 && dialog && output.isBlank() && Files.size(errors) == 0) return "";
      if (exit != 0) throw new IOException("System dialog/file manager failed: " + Files.readString(errors));
      // Strip the protocol newline only: spaces can be part of a real filename.
      return output.replaceFirst("\\r?\\n$", "");
    } finally {
      if (process != null) { processes.remove(process); if (process.isAlive()) process.destroyForcibly(); }
      Files.deleteIfExists(errors);
    }
  }

  static Path existingDirectory(Path start) {
    Path path = start.toAbsolutePath().normalize();
    while (path != null && !Files.isDirectory(path)) path = path.getParent();
    return path != null ? path : Path.of(System.getProperty("user.home"));
  }
  private static String executable(String name) {
    for (String entry : System.getenv().getOrDefault("PATH", "").split(File.pathSeparator)) {
      Path file = Path.of(entry, name);
      if (Files.isRegularFile(file) && Files.isExecutable(file)) return file.toString();
    }
    return null;
  }
  @Override public void close() {
    closed = true;
    processes.forEach(p -> { p.descendants().forEach(ProcessHandle::destroyForcibly); p.destroyForcibly(); });
    processes.clear();
  }
}

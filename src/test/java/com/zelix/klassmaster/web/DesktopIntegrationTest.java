package com.zelix.klassmaster.web;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.zelix.klassmaster.web.Models.*;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DesktopIntegrationTest {
  @TempDir Path temp;

  private static class FakeDesktop extends DesktopIntegration {
    Path selected, start, opened;
    String mode, name;
    int dialogs;
    @Override public Path choose(String mode, String title, Path start, String name) {
      this.mode = mode; this.start = start; this.name = name; dialogs++;
      return selected;
    }
    @Override public void openFolder(Path directory) { opened = directory; }
  }

  @Test void nativeOperationsRequireSessionAndOriginAndCancelWithoutChangingFiles() throws Exception {
    FakeDesktop desktop = new FakeDesktop();
    try (WebServer server = new WebServer(0, temp.resolve("home"), desktop)) {
      server.start();
      var client = HttpClient.newHttpClient();
      for (String route : List.of("desktop/pick", "desktop/open", "outputs/export")) {
        var request = HttpRequest.newBuilder(URI.create(server.base() + "/api/v1/" + route))
            .POST(HttpRequest.BodyPublishers.ofString("{}"));
        assertEquals(401, client.send(request.build(), HttpResponse.BodyHandlers.ofString()).statusCode());
        request.header("X-ZKM-Token", server.url().split("#token=")[1]).header("Origin", "https://foreign.invalid");
        assertEquals(403, client.send(request.build(), HttpResponse.BodyHandlers.ofString()).statusCode());
      }
      assertEquals(0, desktop.dialogs);
      Path directory = Files.createDirectory(temp.resolve("中文 folder ' $test"));
      desktop.selected = directory;
      assertEquals(directory.toString(), call(server, "desktop/pick", Json.map("directory", true, "start", directory.toString())).path("path").asText());
      assertEquals("directory", desktop.mode);
      assertEquals(directory, desktop.start);
      desktop.selected = null;
      assertTrue(call(server, "desktop/pick", Json.map("directory", true)).path("cancelled").asBoolean());
      assertTrue(Files.isDirectory(directory));
      call(server, "desktop/open", Json.map("path", directory.toString()));
      assertEquals(directory, desktop.opened);
      Path file = Files.writeString(directory.resolve("file.jar"), "test");
      call(server, "desktop/open", Json.map("path", file.toString()));
      assertEquals(directory, desktop.opened);
    }
  }

  @Test void completedJarExportsByteForByteAndNativeSaveCancellationDoesNotWrite() throws Exception {
    Path home = temp.resolve("home"), directory = temp.resolve("项目");
    ProjectConfig project = new ProjectStore(home).create(directory.toString(), "Export test");
    Path output = Files.createDirectories(directory.resolve("output"));
    Path jar = makeJar(output.resolve("成品 one.jar"));
    Job job = completed(output); job.projectId = project.id;
    job.directory = directory.resolve("runs").resolve(job.id).toString();
    Json.write(Path.of(job.directory, "job.json"), job);
    FakeDesktop desktop = new FakeDesktop();
    try (WebServer server = new WebServer(0, home, desktop)) {
      server.start();
      JsonNode candidates = call(server, "outputs?jobId=" + job.id, null);
      assertEquals(1, candidates.path("files").size());
      var body = Json.map("jobId", job.id, "path", jar.toString(), "language", "zh");
      assertTrue(call(server, "outputs/export", body).path("cancelled").asBoolean());
      assertEquals("save", desktop.mode);
      assertEquals(jar.getFileName().toString(), desktop.name);
      Path destination = temp.resolve("导出 ' $ ; 成品.jar");
      desktop.selected = destination;
      assertFalse(call(server, "outputs/export", body).path("cancelled").asBoolean());
      assertArrayEquals(Files.readAllBytes(jar), Files.readAllBytes(destination));
      // An unrelated local file must never become an export candidate through a forged request.
      int opened = desktop.dialogs;
      var response = request(server, "outputs/export", Json.map("jobId", job.id, "path", temp.resolve("private.jar").toString()));
      assertEquals(400, response.statusCode());
      assertEquals(opened, desktop.dialogs);
      OutputFiles.export(OutputFiles.candidates(job).get(0), jar); // same-file destination is harmless
      assertArrayEquals(Files.readAllBytes(jar), Files.readAllBytes(destination));
    }
  }

  @Test void directoryExportPreservesManifestClassesResourcesAndExcludesItsOwnDestination() throws Exception {
    Path directory = Files.createDirectories(temp.resolve("classes/META-INF" )).getParent();
    Files.writeString(directory.resolve("META-INF/MANIFEST.MF"), "Manifest-Version: 1.0\r\nMain-Class: Demo\r\n\r\n");
    Files.write(directory.resolve("Demo.class"), new byte[] { (byte) 0xca, (byte) 0xfe, (byte) 0xba, (byte) 0xbe });
    Files.writeString(directory.resolve("中文 资源.txt"), "preserved");
    var candidate = OutputFiles.candidates(completed(directory)).get(0);
    assertEquals("directory", candidate.kind());
    Path target = directory.resolve("export.jar");
    Files.writeString(target, "old export");
    OutputFiles.export(candidate, target);
    try (JarInputStream jar = new JarInputStream(Files.newInputStream(target))) {
      assertNotNull(jar.getManifest());
      assertEquals("Demo", jar.getManifest().getMainAttributes().getValue("Main-Class"));
    }
    try (JarFile jar = new JarFile(target.toFile())) {
      assertEquals("Demo", jar.getManifest().getMainAttributes().getValue("Main-Class"));
      assertEquals(3, jar.size());
      assertNotNull(jar.getEntry("Demo.class"));
      assertEquals("preserved", new String(jar.getInputStream(jar.getEntry("中文 资源.txt")).readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
      assertNull(jar.getEntry("export.jar"));
    }
    try (var files = Files.list(directory)) { assertFalse(files.anyMatch(p -> p.getFileName().toString().startsWith(".zkm-export-"))); }
    assertThrows(IllegalArgumentException.class, () -> OutputFiles.export(candidate, temp.resolve("wrong.txt")));
  }

  @Test void onlySuccessfulOutputsAreOfferedAndPickerStartsAtAnExistingParent() throws Exception {
    Path output = Files.createDirectories(temp.resolve("output/nested"));
    Path jar = makeJar(output.resolve("app.jar"));
    Files.writeString(output.resolve("ChangeLog.txt"), "not an artifact");
    Job job = completed(output, jar, output.resolve("ChangeLog.txt"));
    assertEquals(1, OutputFiles.candidates(job).size());
    assertEquals(jar.toString(), OutputFiles.candidates(job).get(0).path());
    job.status = "failed";
    assertThrows(IllegalArgumentException.class, () -> OutputFiles.candidates(job));
    assertEquals(output, DesktopIntegration.existingDirectory(output.resolve("missing/next")));
    assertEquals(output, DesktopIntegration.existingDirectory(jar));
  }

  private Job completed(Path... outputs) {
    Job job = new Job(); job.kind = "build"; job.status = "succeeded";
    job.result = Json.map("outputs", Arrays.stream(outputs).map(Path::toString).toList());
    return job;
  }
  private Path makeJar(Path file) throws Exception {
    try (JarOutputStream stream = new JarOutputStream(Files.newOutputStream(file))) {
      stream.putNextEntry(new JarEntry("data.txt")); stream.write("resource".getBytes()); stream.closeEntry();
    }
    return file;
  }
  private JsonNode call(WebServer server, String route, Object body) throws Exception {
    var response = request(server, route, body);
    assertEquals(200, response.statusCode(), response.body());
    return Json.MAPPER.readTree(response.body());
  }
  private HttpResponse<String> request(WebServer server, String route, Object body) throws Exception {
    var request = HttpRequest.newBuilder(URI.create(server.base() + "/api/v1/" + route))
        .header("X-ZKM-Token", server.url().split("#token=")[1]);
    if (body != null) request.header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(Json.line(body)));
    return HttpClient.newHttpClient().send(request.build(), HttpResponse.BodyHandlers.ofString());
  }
}

package com.zelix.klassmaster.web;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.util.*;

/** Content-addressed cache keys include dependency directories, not only their names. */
final class FileFingerprint {
  static String of(String settings, Collection<Path> roots) throws Exception {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    digest.update(settings.getBytes(StandardCharsets.UTF_8));
    byte[] buffer = new byte[65536];
    for (Path root : roots) {
      digest.update(root.toString().getBytes(StandardCharsets.UTF_8));
      if (!Files.exists(root)) {
        digest.update((byte) 0);
        continue;
      }
      try (var files = Files.walk(root)) {
        for (Path file : files.filter(Files::isRegularFile).sorted().toList()) {
          digest.update(root.relativize(file).toString().getBytes(StandardCharsets.UTF_8));
          digest.update((byte) 0);
          try (InputStream input = Files.newInputStream(file)) {
            int count;
            while ((count = input.read(buffer)) != -1) digest.update(buffer, 0, count);
          }
        }
      }
    }
    return HexFormat.of().formatHex(digest.digest());
  }
}

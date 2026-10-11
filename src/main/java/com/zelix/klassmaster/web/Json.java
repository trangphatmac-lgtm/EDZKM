package com.zelix.klassmaster.web;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public final class Json {
  public static final ObjectMapper MAPPER =
      new ObjectMapper()
          .enable(SerializationFeature.INDENT_OUTPUT)
          .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

  public static ObjectNode object() {
    return MAPPER.createObjectNode();
  }

  public static JsonNode tree(Object value) {
    return MAPPER.valueToTree(value);
  }

  public static String line(Object value) throws IOException {
    return MAPPER.writer().without(SerializationFeature.INDENT_OUTPUT).writeValueAsString(value);
  }

  public static <T> T read(Path path, Class<T> type) throws IOException {
    return MAPPER.readValue(path.toFile(), type);
  }

  public static void write(Path path, Object value) throws IOException {
    Files.createDirectories(path.toAbsolutePath().getParent());
    Path temp = Files.createTempFile(path.toAbsolutePath().getParent(), ".zkm-", ".tmp");
    try {
      MAPPER.writeValue(temp.toFile(), value);
      try {
        Files.move(temp, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
      } catch (AtomicMoveNotSupportedException e) {
        Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
      }
    } finally {
      Files.deleteIfExists(temp);
    }
  }

  public static Map<String, Object> map(Object... values) {
    Map<String, Object> result = new LinkedHashMap<>();
    for (int i = 0; i < values.length; i += 2) result.put((String) values[i], values[i + 1]);
    return result;
  }
}

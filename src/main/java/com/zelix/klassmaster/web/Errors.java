package com.zelix.klassmaster.web;

import java.util.*;
import java.util.regex.*;

public final class Errors {
  public static Map<String, Object> describe(Throwable error) {
    String message = error.getMessage() == null ? error.toString() : error.getMessage();
    Map<String, Object> value =
        Json.map(
            "code",
            error instanceof ProjectStore.Conflict ? "CONFLICT" : error.getClass().getSimpleName(),
            "message",
            message);
    Matcher match =
        Pattern.compile("[Ll]ine\\s+(\\d+)(?:,?\\s+[Cc]olumn\\s+(\\d+))?").matcher(message);
    if (match.find()) {
      value.put("line", Integer.parseInt(match.group(1)));
      value.put("column", match.group(2) == null ? 1 : Integer.parseInt(match.group(2)));
    }
    return value;
  }
}

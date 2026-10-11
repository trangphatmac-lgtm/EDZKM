package com.zelix.klassmaster.web;

import java.util.Map;
import java.util.function.Consumer;

/** Worker-local progress channel; absent for legacy CLI/Swing execution. */
final class Progress {
  static final ThreadLocal<Consumer<Map<String, Object>>> CURRENT = new ThreadLocal<>();

  static void report(String phase, int completed, int total) {
    var listener = CURRENT.get();
    if (listener != null)
      listener.accept(
          Json.map(
              "phase", phase, "progress", total == 0 ? 0 : Math.min(99, 100 * completed / total)));
  }
}

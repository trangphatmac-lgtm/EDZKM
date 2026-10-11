package com.zelix.klassmaster.script;

import com.zelix.klassmaster.exceptions.ZkmException;
import com.zelix.klassmaster.script.parser.ZkmScriptNode;
import java.io.*;

/** Optional, thread-confined integration hooks. Legacy execution has no observer. */
public interface ExecutionObserver {
  ThreadLocal<ExecutionObserver> CURRENT = new ThreadLocal<>();

  default void start(int statements) {}

  default void before(ZkmScriptNode statement, ScriptEnvironment environment)
      throws ZkmException, IOException {}

  default void after(ZkmScriptNode statement, ScriptEnvironment environment)
      throws ZkmException, IOException {}

  default File output(File file) {
    return file;
  }

  default File input(File file) {
    return file;
  }

  default String source(File file) {
    return null;
  }

  default File includeDirectory() {
    return null;
  }

  static File outputPath(File file) {
    ExecutionObserver o = CURRENT.get();
    return o == null ? file : o.output(file);
  }

  static File inputPath(File file) {
    ExecutionObserver o = CURRENT.get();
    return o == null ? file : o.input(file);
  }
}

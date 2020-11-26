package io.harness.mongo;

import io.harness.logging.AutoLogContext;

public class ProcessTimeLogContext extends AutoLogContext {
  public static final String ID = "processTime";

  public ProcessTimeLogContext(Long processTime, OverrideBehavior behavior) {
    super(ID, processTime.toString(), behavior);
  }
}

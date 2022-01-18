package io.harness.archiver;

import io.harness.logging.AutoLogContext;

public class ArchiverLogContext extends AutoLogContext {
  public static final String ID = "accountId";

  public ArchiverLogContext(String planExecutionId, OverrideBehavior behavior) {
    super(ID, planExecutionId, behavior);
  }
}

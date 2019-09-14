package software.wings.service.impl;

import io.harness.logging.AutoLogContext;

public class ExecutionLogContext extends AutoLogContext {
  public ExecutionLogContext(String executionId) {
    super("executionId", '[' + executionId + ']');
  }
}

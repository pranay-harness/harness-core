package software.wings.service.impl;

import io.harness.logging.AutoLogContext;

public class StateExecutionInstanceLogContext extends AutoLogContext {
  public StateExecutionInstanceLogContext(String StateExecutionInstanceId) {
    super("StateExecutionInstanceId", StateExecutionInstanceId);
  }
}

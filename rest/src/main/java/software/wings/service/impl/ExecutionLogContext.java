package software.wings.service.impl;

import org.slf4j.MDC;

public class ExecutionLogContext implements AutoCloseable {
  public ExecutionLogContext(String executionId) {
    MDC.put("executionId", '[' + executionId + ']');
  }

  @Override
  public void close() throws Exception {
    MDC.remove("executionId");
  }
}

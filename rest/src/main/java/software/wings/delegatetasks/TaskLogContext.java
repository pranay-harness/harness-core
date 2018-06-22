package software.wings.delegatetasks;

import org.slf4j.MDC;

public class TaskLogContext implements AutoCloseable {
  public TaskLogContext(String taskId) {
    MDC.put("taskId", taskId);
  }

  @Override
  public void close() throws Exception {
    MDC.remove("taskId");
  }
}

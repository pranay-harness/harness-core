package io.harness.delegate.task;

import com.google.common.collect.ImmutableMap;

import io.harness.logging.AutoLogContext;
import org.slf4j.MDC;

public class TaskLogContext extends AutoLogContext {
  public static void clearTaskData() {
    MDC.remove("taskId");
    MDC.remove("taskType");
    MDC.remove("taskGroup");
  }

  public TaskLogContext(String taskId) {
    super("taskId", taskId);
  }

  public TaskLogContext(String taskId, String taskType, String taskGroup) {
    super(ImmutableMap.of("taskId", taskId, "taskType", taskType, "taskGroup", taskGroup));
  }
}

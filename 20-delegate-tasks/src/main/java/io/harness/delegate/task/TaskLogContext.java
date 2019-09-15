package io.harness.delegate.task;

import com.google.common.collect.ImmutableMap;

import io.harness.logging.AutoLogContext;

public class TaskLogContext extends AutoLogContext {
  public TaskLogContext(String taskId) {
    super("taskId", taskId);
  }

  public TaskLogContext(String taskId, String taskType) {
    super(ImmutableMap.of("taskId", taskId, "taskType", taskType));
  }
}

package io.harness.delegate.task;

import com.google.common.collect.ImmutableMap;

import io.harness.logging.AutoLogContext;

public class TaskLogContext extends AutoLogContext {
  public TaskLogContext(String taskId, OverrideBehavior behavior) {
    super("taskId", taskId, behavior);
  }

  public TaskLogContext(String taskId, String taskType, String taskGroup, OverrideBehavior behavior) {
    super(ImmutableMap.of("taskId", taskId, "taskType", taskType, "taskGroup", taskGroup), behavior);
  }
}

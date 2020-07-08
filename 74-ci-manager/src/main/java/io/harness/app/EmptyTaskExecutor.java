package io.harness.app;

import io.harness.tasks.Task;
import io.harness.tasks.TaskExecutor;

import java.util.Map;

public class EmptyTaskExecutor implements TaskExecutor {
  @Override
  public String queueTask(Map<String, String> setupAbstractions, Task task) {
    return null;
  }

  @Override
  public void expireTask(Map<String, String> setupAbstractions, String taskId) {
    // Just Placeholder
  }

  @Override
  public void abortTask(Map<String, String> setupAbstractions, String taskId) {
    // Just Placeholder
  }
}
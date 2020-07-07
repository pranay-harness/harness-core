package io.harness.app;

import io.harness.ambiance.Ambiance;
import io.harness.tasks.Task;
import io.harness.tasks.TaskExecutor;

public class EmptyTaskExecutor implements TaskExecutor {
  @Override
  public String queueTask(Ambiance ambiance, Task task) {
    return null;
  }

  @Override
  public void expireTask(Ambiance ambiance, String taskId) {
    // Just a placeholder
  }

  @Override
  public void abortTask(Ambiance ambiance, String taskId) {
    // Just a placeholder
  }
}
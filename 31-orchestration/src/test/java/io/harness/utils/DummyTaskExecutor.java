package io.harness.utils;

import io.harness.ambiance.Ambiance;
import io.harness.tasks.Task;
import io.harness.tasks.TaskExecutor;

/**
 * The type Dummy task executor.
 * This is only to provide a Dummy Binding to Guice else it complains while running tests
 */
public class DummyTaskExecutor implements TaskExecutor {
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

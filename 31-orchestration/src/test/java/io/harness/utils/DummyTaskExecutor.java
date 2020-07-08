package io.harness.utils;

import io.harness.tasks.Task;
import io.harness.tasks.TaskExecutor;

import java.util.Map;

/**
 * The type Dummy task executor.
 * This is only to provide a Dummy Binding to Guice else it complains while running tests
 */
public class DummyTaskExecutor implements TaskExecutor {
  @Override
  public String queueTask(Map<String, String> setupAbstractions, Task task) {
    return null;
  }

  @Override
  public void expireTask(Map<String, String> setupAbstractions, String taskId) {
    // Just a placeholder
  }

  @Override
  public void abortTask(Map<String, String> setupAbstractions, String taskId) {
    // Just a placeholder
  }
}

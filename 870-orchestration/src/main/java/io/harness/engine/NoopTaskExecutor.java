package io.harness.engine;

import io.harness.engine.pms.tasks.TaskExecutor;
import io.harness.pms.contracts.execution.tasks.TaskRequest;

import java.util.Map;

/**
 * The type Dummy task executor.
 * This is only to provide a Dummy Binding to Guice else it complains while running tests
 */
public class NoopTaskExecutor implements TaskExecutor {
  @Override
  public String queueTask(Map<String, String> setupAbstractions, TaskRequest task) {
    return null;
  }

  @Override
  public void expireTask(Map<String, String> setupAbstractions, String taskId) {
    // Just a placeholder
  }

  @Override
  public boolean abortTask(Map<String, String> setupAbstractions, String taskId) {
    return false;
  }
}

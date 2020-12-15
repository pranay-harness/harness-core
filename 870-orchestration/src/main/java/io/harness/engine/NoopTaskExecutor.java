package io.harness.engine;

import io.harness.delegate.task.HDelegateTask;
import io.harness.engine.pms.tasks.TaskExecutor;

import java.util.Map;

/**
 * The type Dummy task executor.
 * This is only to provide a Dummy Binding to Guice else it complains while running tests
 */
public class NoopTaskExecutor implements TaskExecutor<HDelegateTask> {
  @Override
  public String queueTask(Map<String, String> setupAbstractions, HDelegateTask task) {
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

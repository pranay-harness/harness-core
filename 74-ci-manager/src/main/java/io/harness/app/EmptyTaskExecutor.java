package io.harness.app;

import io.harness.delegate.task.HDelegateTask;
import io.harness.tasks.TaskExecutor;

import java.util.Map;

public class EmptyTaskExecutor implements TaskExecutor<HDelegateTask> {
  @Override
  public String queueTask(Map<String, String> setupAbstractions, HDelegateTask task) {
    return null;
  }

  @Override
  public void expireTask(Map<String, String> setupAbstractions, String taskId) {
    // Just Placeholder
  }

  @Override
  public boolean abortTask(Map<String, String> setupAbstractions, String taskId) {
    return false;
  }
}
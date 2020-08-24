package io.harness.tasks;

import java.util.Map;

public interface TaskExecutor<T extends Task> {
  String queueTask(Map<String, String> setupAbstractions, T task);

  void expireTask(Map<String, String> setupAbstractions, String taskId);

  boolean abortTask(Map<String, String> setupAbstractions, String taskId);
}

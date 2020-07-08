package io.harness.tasks;

import java.util.Map;

public interface TaskExecutor {
  String queueTask(Map<String, String> setupAbstractions, Task task);

  void expireTask(Map<String, String> setupAbstractions, String taskId);

  void abortTask(Map<String, String> setupAbstractions, String taskId);
}

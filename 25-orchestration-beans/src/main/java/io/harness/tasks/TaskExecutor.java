package io.harness.tasks;

import io.harness.ambiance.Ambiance;

public interface TaskExecutor {
  String queueTask(Ambiance ambiance, Task task);

  void expireTask(Ambiance ambiance, String taskId);

  void abortTask(Ambiance ambiance, String taskId);
}

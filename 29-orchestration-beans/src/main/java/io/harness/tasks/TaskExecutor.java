package io.harness.tasks;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.Task;
import io.harness.ambiance.Ambiance;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CDC)
public interface TaskExecutor {
  String queueTask(Ambiance ambiance, Task task);

  void expireTask(Ambiance ambiance, String taskId);

  void abortTask(Ambiance ambiance, String taskId);
}

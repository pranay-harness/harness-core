package io.harness.engine.pms.tasks;

import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.tasks.ResponseData;

import java.time.Duration;
import java.util.Map;

public interface TaskExecutor {
  String queueTask(Map<String, String> setupAbstractions, TaskRequest taskRequest, Duration holdFor);

  void expireTask(Map<String, String> setupAbstractions, String taskId);

  boolean abortTask(Map<String, String> setupAbstractions, String taskId);

  <T extends ResponseData> T executeTask(Map<String, String> setupAbstractions, TaskRequest taskRequest)
      throws InterruptedException;
}

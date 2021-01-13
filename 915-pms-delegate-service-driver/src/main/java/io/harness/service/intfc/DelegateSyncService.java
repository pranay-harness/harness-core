package io.harness.service.intfc;

import io.harness.tasks.ResponseData;

import java.time.Duration;

public interface DelegateSyncService extends Runnable {
  <T extends ResponseData> T waitForTask(String taskId, String description, Duration timeout);
}

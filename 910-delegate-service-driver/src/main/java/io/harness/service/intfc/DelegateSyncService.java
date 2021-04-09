package io.harness.service.intfc;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.tasks.ResponseData;

import java.time.Duration;

@OwnedBy(HarnessTeam.DEL)
public interface DelegateSyncService extends Runnable {
  <T extends ResponseData> T waitForTask(String taskId, String description, Duration timeout);
}

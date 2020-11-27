package io.harness.service.intfc;

import io.harness.delegate.beans.DelegateResponseData;

import java.time.Duration;

public interface DelegateSyncService extends Runnable {
  <T extends DelegateResponseData> T waitForTask(String taskId, String description, Duration timeout);
}

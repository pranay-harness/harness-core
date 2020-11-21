package io.harness.service.intfc;

public interface DelegateAsyncService extends Runnable {
  void setupTimeoutForTask(String taskId, long expiry);
}

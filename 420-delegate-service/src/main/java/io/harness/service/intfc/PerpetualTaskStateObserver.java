package io.harness.service.intfc;

public interface PerpetualTaskStateObserver {
  void onPerpetualTaskAssigned(String accountId, String taskId, String delegateId);
}

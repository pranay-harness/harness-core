package software.wings.service.impl;

import static io.harness.annotations.dev.HarnessTeam.DEL;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.DelegateTaskUsageInsightsEventType;

@OwnedBy(DEL)
public interface DelegateTaskStatusObserver {
  void onTaskAssigned(String accountId, String taskId, String delegateId);
  void onTaskCompleted(
      String accountId, String taskId, String delegateId, DelegateTaskUsageInsightsEventType eventType);
}

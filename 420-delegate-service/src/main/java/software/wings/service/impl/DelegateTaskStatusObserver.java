package software.wings.service.impl;

import software.wings.beans.DelegateTaskUsageInsightsEventType;

public interface DelegateTaskStatusObserver {
  void onTaskAssigned(String accountId, String taskId, String delegateId, String delegateGroupId);
  void onTaskCompleted(
      String accountId, String taskId, String delegateId, DelegateTaskUsageInsightsEventType eventType);
}

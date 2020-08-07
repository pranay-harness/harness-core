package software.wings.utils;

import io.harness.beans.DelegateTask;
import software.wings.beans.DelegateTaskPackage;

public class DelegateTaskUtils {
  public static DelegateTaskPackage getDelegateTaskPackage(DelegateTask task) {
    return DelegateTaskPackage.builder()
        .accountId(task.getAccountId())
        .delegateId(task.getDelegateId())
        .delegateTaskId(task.getUuid())
        .data(task.getData())
        .capabilityFrameworkEnabled(task.isCapabilityFrameworkEnabled())
        .executionCapabilities(task.getExecutionCapabilities())
        .build();
  }

  private DelegateTaskUtils() {}
}

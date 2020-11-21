package software.wings.utils;

import io.harness.beans.DelegateTask;
import io.harness.delegate.beans.DelegateTaskPackage;

import lombok.experimental.UtilityClass;

@UtilityClass
public class DelegateTaskUtils {
  public static DelegateTaskPackage getDelegateTaskPackage(DelegateTask task) {
    return DelegateTaskPackage.builder()
        .accountId(task.getAccountId())
        .delegateId(task.getDelegateId())
        .delegateTaskId(task.getUuid())
        .data(task.getData())
        .executionCapabilities(task.getExecutionCapabilities())
        .build();
  }
}

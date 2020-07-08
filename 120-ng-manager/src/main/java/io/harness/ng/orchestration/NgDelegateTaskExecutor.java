package io.harness.ng.orchestration;

import com.google.inject.Inject;

import io.harness.ManagerDelegateServiceDriver;
import io.harness.ambiance.Ambiance;
import io.harness.delegate.task.HDelegateTask;
import io.harness.exception.InvalidRequestException;
import io.harness.tasks.Task;
import io.harness.tasks.TaskExecutor;
import io.harness.waiter.WaitNotifyEngine;

public class NgDelegateTaskExecutor implements TaskExecutor {
  private final ManagerDelegateServiceDriver managerDelegateServiceDriver;

  @Inject
  public NgDelegateTaskExecutor(
      ManagerDelegateServiceDriver managerDelegateServiceDriver, WaitNotifyEngine waitNotifyEngine) {
    this.managerDelegateServiceDriver = managerDelegateServiceDriver;
  }

  @Override
  public String queueTask(Ambiance ambiance, Task task) {
    if (task instanceof HDelegateTask) {
      HDelegateTask hDelegateTask = (HDelegateTask) task;
      String accountId = hDelegateTask.getAccountId();
      return managerDelegateServiceDriver.sendTaskAsync(
          accountId, ambiance.getSetupAbstractions(), hDelegateTask.getData());
    }
    throw new InvalidRequestException(
        "Execution not supported for Task. TaskClass: " + task.getClass().getCanonicalName());
  }

  @Override
  public void expireTask(Ambiance ambiance, String taskId) {
    // Implement Expire task
  }

  @Override
  public void abortTask(Ambiance ambiance, String taskId) {
    // Implement Abort task
  }
}

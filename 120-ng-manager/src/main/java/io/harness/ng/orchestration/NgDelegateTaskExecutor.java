package io.harness.ng.orchestration;

import com.google.inject.Inject;

import io.harness.ManagerDelegateServiceDriver;
import io.harness.delegate.task.HDelegateTask;
import io.harness.tasks.TaskExecutor;
import lombok.NonNull;

import java.util.Map;

public class NgDelegateTaskExecutor implements TaskExecutor<HDelegateTask> {
  private final ManagerDelegateServiceDriver managerDelegateServiceDriver;

  private static final String ACCOUNT_ID_KEY = "accountId";

  @Inject
  public NgDelegateTaskExecutor(ManagerDelegateServiceDriver managerDelegateServiceDriver) {
    this.managerDelegateServiceDriver = managerDelegateServiceDriver;
  }

  @Override
  public String queueTask(@NonNull Map<String, String> setupAbstractions, @NonNull HDelegateTask task) {
    String accountId = task.getAccountId();
    return managerDelegateServiceDriver.sendTaskAsync(accountId, task.getSetupAbstractions(), task.getData());
  }

  @Override
  public void expireTask(@NonNull Map<String, String> setupAbstractions, @NonNull String taskId) {
    // Implement Expire task
  }

  @Override
  public boolean abortTask(@NonNull Map<String, String> setupAbstractions, @NonNull String taskId) {
    String accountId = setupAbstractions.get(ACCOUNT_ID_KEY);
    return managerDelegateServiceDriver.abortTask(accountId, taskId);
  }
}

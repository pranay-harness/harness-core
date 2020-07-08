package software.wings.service;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

import com.google.inject.Inject;

import io.harness.ambiance.Ambiance;
import io.harness.beans.DelegateTask;
import io.harness.tasks.Task;
import io.harness.tasks.TaskExecutor;
import lombok.NonNull;
import software.wings.service.intfc.DelegateService;

public class DelegateTaskExecutor implements TaskExecutor {
  @Inject private DelegateService delegateService;

  @Override
  public String queueTask(@NonNull Ambiance ambiance, @NonNull Task task) {
    DelegateTask delegateTask = (DelegateTask) task;
    // This is for backward compatibility as current delegate service works with wait Id
    if (delegateTask.getWaitId() == null) {
      delegateTask.setWaitId(generateUuid());
    }
    delegateTask.setUuid(delegateTask.getWaitId());
    return delegateService.queueTask(delegateTask);
  }

  @Override
  public void expireTask(@NonNull Ambiance ambiance, @NonNull String taskId) {
    String accountId = ambiance.getSetupAbstractions().get("accountId");
    delegateService.expireTask(accountId, taskId);
  }

  @Override
  public void abortTask(@NonNull Ambiance ambiance, @NonNull String taskId) {
    String accountId = ambiance.getSetupAbstractions().get("accountId");
    delegateService.abortTask(accountId, taskId);
  }
}

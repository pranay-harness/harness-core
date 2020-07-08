package software.wings.service;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

import com.google.inject.Inject;

import io.harness.beans.DelegateTask;
import io.harness.tasks.Task;
import io.harness.tasks.TaskExecutor;
import lombok.NonNull;
import software.wings.service.intfc.DelegateService;

import java.util.Map;

public class DelegateTaskExecutor implements TaskExecutor {
  @Inject private DelegateService delegateService;

  @Override
  public String queueTask(@NonNull Map<String, String> setupAbstractions, @NonNull Task task) {
    DelegateTask delegateTask = (DelegateTask) task;
    // This is for backward compatibility as current delegate service works with wait Id
    if (delegateTask.getWaitId() == null) {
      delegateTask.setWaitId(generateUuid());
    }
    delegateTask.setUuid(delegateTask.getWaitId());
    return delegateService.queueTask(delegateTask);
  }

  @Override
  public void expireTask(@NonNull Map<String, String> setupAbstractions, @NonNull String taskId) {
    String accountId = setupAbstractions.get("accountId");
    delegateService.expireTask(accountId, taskId);
  }

  @Override
  public void abortTask(@NonNull Map<String, String> setupAbstractions, @NonNull String taskId) {
    String accountId = setupAbstractions.get("accountId");
    delegateService.abortTask(accountId, taskId);
  }
}

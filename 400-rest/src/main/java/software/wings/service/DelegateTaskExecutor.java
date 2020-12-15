package software.wings.service;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

import io.harness.beans.DelegateTask;
import io.harness.engine.pms.tasks.TaskExecutor;

import software.wings.service.intfc.DelegateService;

import com.google.inject.Inject;
import java.util.Map;
import lombok.NonNull;

public class DelegateTaskExecutor implements TaskExecutor<DelegateTask> {
  @Inject private DelegateService delegateService;

  @Override
  public String queueTask(@NonNull Map<String, String> setupAbstractions, @NonNull DelegateTask task) {
    // This is for backward compatibility as current delegate service works with wait Id
    if (task.getWaitId() == null) {
      task.setWaitId(generateUuid());
    }
    task.setUuid(task.getWaitId());
    return delegateService.queueTask(task);
  }

  @Override
  public void expireTask(@NonNull Map<String, String> setupAbstractions, @NonNull String taskId) {
    String accountId = setupAbstractions.get("accountId");
    delegateService.expireTask(accountId, taskId);
  }

  @Override
  public boolean abortTask(@NonNull Map<String, String> setupAbstractions, @NonNull String taskId) {
    String accountId = setupAbstractions.get("accountId");
    DelegateTask task = delegateService.abortTask(accountId, taskId);
    return task != null;
  }
}

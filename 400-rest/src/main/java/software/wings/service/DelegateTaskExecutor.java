package software.wings.service;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

import io.harness.beans.DelegateTask;
import io.harness.delegate.TaskDetails;
import io.harness.delegate.beans.TaskData;
import io.harness.engine.pms.tasks.TaskExecutor;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.serializer.KryoSerializer;

import software.wings.service.intfc.DelegateService;

import com.google.inject.Inject;
import java.util.Map;
import lombok.NonNull;

public class DelegateTaskExecutor implements TaskExecutor {
  @Inject private DelegateService delegateService;
  @Inject private KryoSerializer kryoSerializer;

  @Override
  public String queueTask(@NonNull Map<String, String> setupAbstractions, @NonNull TaskRequest taskRequest) {
    // This is for backward compatibility as current delegate service works with wait Id
    DelegateTask task = convertRequestToTask(taskRequest);
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

  private DelegateTask convertRequestToTask(TaskRequest taskRequest) {
    TaskDetails taskDetails = taskRequest.getDelegateTaskRequest().getDetails();
    Map<String, String> abstractions = taskRequest.getDelegateTaskRequest().getSetupAbstractions().getValuesMap();
    String id = generateUuid();
    return DelegateTask.builder()
        .uuid(id)
        .accountId(taskRequest.getDelegateTaskRequest().getAccountId())
        .waitId(id)
        .data(TaskData.builder()
                  .async(taskDetails.getParked())
                  .taskType(taskDetails.getType().getType())
                  .parameters(
                      new Object[] {kryoSerializer.asInflatedObject(taskDetails.getKryoParameters().toByteArray())})
                  .timeout(taskDetails.getExecutionTimeout().getSeconds() * 1000)
                  .build())
        .setupAbstractions(abstractions)
        .build();
  }
}

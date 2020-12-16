package io.harness.engine.pms.tasks;

import io.harness.beans.DelegateTaskRequest;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.TaskDetails;
import io.harness.delegate.task.TaskParameters;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.serializer.KryoSerializer;
import io.harness.service.DelegateGrpcClientWrapper;

import com.google.inject.Inject;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

public class NgDelegate2TaskExecutor implements TaskExecutor {
  @Inject private DelegateGrpcClientWrapper delegateGrpcClientWrapper;
  @Inject private KryoSerializer kryoSerializer;

  @Override
  public String queueTask(Map<String, String> setupAbstractions, TaskRequest taskRequest) {
    String accountId = taskRequest.getDelegateTaskRequest().getAccountId();
    TaskDetails taskDetails = taskRequest.getDelegateTaskRequest().getDetails();
    final DelegateTaskRequest delegateTaskRequest =
        DelegateTaskRequest.builder()
            .accountId(accountId)
            .taskType(taskDetails.getType().getType())
            .taskParameters(extractTaskParameters(taskDetails))
            .executionTimeout(Duration.ofSeconds(taskDetails.getExecutionTimeout().getSeconds()))
            .taskSetupAbstractions(taskRequest.getDelegateTaskRequest().getSetupAbstractions().getValuesMap())
            .logStreamingAbstractions(
                new LinkedHashMap<>(taskRequest.getDelegateTaskRequest().getLogAbstractions().getValuesMap()))
            .build();
    return delegateGrpcClientWrapper.submitAsyncTask(delegateTaskRequest);
  }

  @Override
  public void expireTask(Map<String, String> setupAbstractions, String taskId) {
    // Needs to be implemented
  }

  @Override
  public boolean abortTask(Map<String, String> setupAbstractions, String taskId) {
    // Needs to be implemented
    return false;
  }

  private TaskParameters extractTaskParameters(TaskDetails taskDetails) {
    if (taskDetails == null || EmptyPredicate.isEmpty(taskDetails.getKryoParameters().toByteArray())) {
      return null;
    }
    Object params = kryoSerializer.asObject(taskDetails.getKryoParameters().toByteArray());
    if (params instanceof TaskParameters) {
      return (TaskParameters) params;
    }
    throw new InvalidRequestException("Task Execution not supported for type");
  }
}

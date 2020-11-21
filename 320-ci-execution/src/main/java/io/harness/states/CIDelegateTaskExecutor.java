package io.harness.states;

import io.harness.beans.DelegateTaskRequest;
import io.harness.callback.DelegateCallbackToken;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.task.HDelegateTask;
import io.harness.delegate.task.TaskParameters;
import io.harness.exception.InvalidRequestException;
import io.harness.grpc.DelegateServiceGrpcClient;
import io.harness.tasks.TaskExecutor;

import com.google.inject.Inject;
import java.time.Duration;
import java.util.Map;
import java.util.function.Supplier;

public class CIDelegateTaskExecutor implements TaskExecutor<HDelegateTask> {
  private final DelegateServiceGrpcClient delegateServiceGrpcClient;
  private final Supplier<DelegateCallbackToken> delegateCallbackTokenSupplier;

  @Inject
  public CIDelegateTaskExecutor(DelegateServiceGrpcClient delegateServiceGrpcClient,
      Supplier<DelegateCallbackToken> delegateCallbackTokenSupplier) {
    this.delegateServiceGrpcClient = delegateServiceGrpcClient;
    this.delegateCallbackTokenSupplier = delegateCallbackTokenSupplier;
  }

  @Override
  public String queueTask(Map<String, String> setupAbstractions, HDelegateTask task) {
    String accountId = task.getAccountId();
    TaskData taskData = task.getData();
    final DelegateTaskRequest delegateTaskRequest = DelegateTaskRequest.builder()
                                                        .parked(taskData.isParked())
                                                        .accountId(accountId)
                                                        .taskType(taskData.getTaskType())
                                                        .taskParameters(extractTaskParameters(taskData))
                                                        .executionTimeout(Duration.ofHours(12))
                                                        .taskSetupAbstractions(setupAbstractions)
                                                        .build();
    return delegateServiceGrpcClient.submitAsyncTask(delegateTaskRequest, delegateCallbackTokenSupplier.get());
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

  private TaskParameters extractTaskParameters(TaskData taskData) {
    if (taskData == null || EmptyPredicate.isEmpty(taskData.getParameters())) {
      return null;
    }
    if (taskData.getParameters()[0] instanceof TaskParameters) {
      return (TaskParameters) taskData.getParameters()[0];
    }
    throw new InvalidRequestException("Task Execution not supported for type");
  }
}

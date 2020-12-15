package io.harness.ng.orchestration;

import io.harness.beans.DelegateTaskRequest;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.task.HDelegateTask;
import io.harness.delegate.task.TaskParameters;
import io.harness.engine.pms.tasks.TaskExecutor;
import io.harness.exception.InvalidRequestException;
import io.harness.service.DelegateGrpcClientWrapper;

import com.google.inject.Inject;
import java.time.Duration;
import java.util.Map;

public class NgDelegate2TaskExecutor implements TaskExecutor<HDelegateTask> {
  @Inject private DelegateGrpcClientWrapper delegateGrpcClientWrapper;

  @Override
  public String queueTask(Map<String, String> setupAbstractions, HDelegateTask task) {
    String accountId = task.getAccountId();
    TaskData taskData = task.getData();
    final DelegateTaskRequest delegateTaskRequest = DelegateTaskRequest.builder()
                                                        .accountId(accountId)
                                                        .taskType(taskData.getTaskType())
                                                        .taskParameters(extractTaskParameters(taskData))
                                                        .executionTimeout(Duration.ofMillis(taskData.getTimeout()))
                                                        .taskSetupAbstractions(setupAbstractions)
                                                        .logStreamingAbstractions(task.getLogStreamingAbstractions())
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

package io.harness.engine.pms.tasks;

import io.harness.callback.DelegateCallbackToken;
import io.harness.delegate.AccountId;
import io.harness.delegate.CancelTaskRequest;
import io.harness.delegate.CancelTaskResponse;
import io.harness.delegate.DelegateServiceGrpc.DelegateServiceBlockingStub;
import io.harness.delegate.SubmitTaskRequest;
import io.harness.delegate.SubmitTaskResponse;
import io.harness.delegate.TaskId;
import io.harness.pms.contracts.execution.tasks.DelegateTaskRequest;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.plan.execution.SetupAbstractionKeys;

import com.google.inject.Inject;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NgDelegate2TaskExecutor implements TaskExecutor {
  @Inject private DelegateServiceBlockingStub delegateServiceBlockingStub;
  @Inject private Supplier<DelegateCallbackToken> tokenSupplier;

  @Override
  public String queueTask(Map<String, String> setupAbstractions, TaskRequest taskRequest) {
    SubmitTaskRequest submitTaskRequest = buildSubmitTaskRequest(taskRequest);
    SubmitTaskResponse submitTaskResponse = delegateServiceBlockingStub.submitTask(submitTaskRequest);
    return submitTaskResponse.getTaskId().getId();
  }

  @Override
  public void expireTask(Map<String, String> setupAbstractions, String taskId) {
    // Needs to be implemented
  }

  @Override
  public boolean abortTask(Map<String, String> setupAbstractions, String taskId) {
    try {
      CancelTaskResponse response =
          delegateServiceBlockingStub.withDeadlineAfter(30, TimeUnit.SECONDS)
              .cancelTask(
                  CancelTaskRequest.newBuilder()
                      .setAccountId(
                          AccountId.newBuilder().setId(setupAbstractions.get(SetupAbstractionKeys.accountId)).build())
                      .setTaskId(TaskId.newBuilder().setId(taskId).build())
                      .build());
      return true;
    } catch (Exception ex) {
      log.error("Failed to abort task with taskId: {}, Error : {}", taskId, ex.getMessage());
      return false;
    }
  }

  SubmitTaskRequest buildSubmitTaskRequest(TaskRequest taskRequest) {
    DelegateTaskRequest delegateTaskRequest = taskRequest.getDelegateTaskRequest();
    return SubmitTaskRequest.newBuilder()
        .setAccountId(AccountId.newBuilder().setId(delegateTaskRequest.getAccountId()).build())
        .setDetails(delegateTaskRequest.getDetails())
        .setLogAbstractions(delegateTaskRequest.getLogAbstractions())
        .setSetupAbstractions(delegateTaskRequest.getSetupAbstractions())
        .addAllSelectors(delegateTaskRequest.getSelectorsList())
        .addAllCapabilities(delegateTaskRequest.getCapabilitiesList())
        .setCallbackToken(tokenSupplier.get())
        .build();
  }
}

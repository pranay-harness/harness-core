package io.harness.engine.pms.tasks;

import io.harness.callback.DelegateCallbackToken;
import io.harness.delegate.AccountId;
import io.harness.delegate.DelegateServiceGrpc.DelegateServiceBlockingStub;
import io.harness.delegate.SubmitTaskRequest;
import io.harness.delegate.SubmitTaskResponse;
import io.harness.pms.contracts.execution.tasks.DelegateTaskRequest;
import io.harness.pms.contracts.execution.tasks.TaskRequest;

import com.google.inject.Inject;
import java.util.Map;
import java.util.function.Supplier;

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
    // Needs to be implemented
    return false;
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

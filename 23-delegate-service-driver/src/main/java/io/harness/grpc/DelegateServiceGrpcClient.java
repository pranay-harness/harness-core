package io.harness.grpc;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.stream.Collectors.toList;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.ByteString;

import io.harness.callback.DelegateCallback;
import io.harness.callback.DelegateCallbackToken;
import io.harness.delegate.AccountId;
import io.harness.delegate.CancelTaskRequest;
import io.harness.delegate.CancelTaskResponse;
import io.harness.delegate.Capability;
import io.harness.delegate.CreatePerpetualTaskRequest;
import io.harness.delegate.CreatePerpetualTaskResponse;
import io.harness.delegate.DelegateServiceGrpc.DelegateServiceBlockingStub;
import io.harness.delegate.DeletePerpetualTaskRequest;
import io.harness.delegate.ObtainDocumentRequest;
import io.harness.delegate.ObtainDocumentResponse;
import io.harness.delegate.RegisterCallbackRequest;
import io.harness.delegate.RegisterCallbackResponse;
import io.harness.delegate.ResetPerpetualTaskRequest;
import io.harness.delegate.SubmitTaskRequest;
import io.harness.delegate.SubmitTaskResponse;
import io.harness.delegate.TaskDetails;
import io.harness.delegate.TaskExecutionStage;
import io.harness.delegate.TaskId;
import io.harness.delegate.TaskProgressRequest;
import io.harness.delegate.TaskProgressResponse;
import io.harness.delegate.TaskProgressUpdatesRequest;
import io.harness.delegate.TaskProgressUpdatesResponse;
import io.harness.delegate.TaskSelector;
import io.harness.delegate.TaskSetupAbstractions;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.perpetualtask.PerpetualTaskClientContextDetails;
import io.harness.perpetualtask.PerpetualTaskExecutionBundle;
import io.harness.perpetualtask.PerpetualTaskId;
import io.harness.perpetualtask.PerpetualTaskSchedule;
import io.harness.serializer.KryoSerializer;
import lombok.extern.slf4j.Slf4j;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Singleton
@Slf4j
public class DelegateServiceGrpcClient {
  private final DelegateServiceBlockingStub delegateServiceBlockingStub;
  private final KryoSerializer kryoSerializer;

  @Inject
  public DelegateServiceGrpcClient(
      DelegateServiceBlockingStub delegateServiceBlockingStub, KryoSerializer kryoSerializer) {
    this.delegateServiceBlockingStub = delegateServiceBlockingStub;
    this.kryoSerializer = kryoSerializer;
  }

  public TaskId submitTask(DelegateCallbackToken delegateCallbackToken, AccountId accountId,
      TaskSetupAbstractions taskSetupAbstractions, TaskDetails taskDetails, List<ExecutionCapability> capabilities,
      List<String> taskSelectors) {
    SubmitTaskRequest.Builder submitTaskRequestBuilder = SubmitTaskRequest.newBuilder()
                                                             .setCallbackToken(delegateCallbackToken)
                                                             .setAccountId(accountId)
                                                             .setSetupAbstractions(taskSetupAbstractions)
                                                             .setDetails(taskDetails);

    if (isNotEmpty(capabilities)) {
      submitTaskRequestBuilder.addAllCapabilities(
          capabilities.stream()
              .map(capability
                  -> Capability.newBuilder()
                         .setKryoCapability(ByteString.copyFrom(kryoSerializer.asDeflatedBytes(capability)))
                         .build())
              .collect(toList()));
    }

    if (isNotEmpty(taskSelectors)) {
      submitTaskRequestBuilder.addAllSelectors(
          taskSelectors.stream()
              .map(selector -> TaskSelector.newBuilder().setSelector(selector).build())
              .collect(toList()));
    }

    SubmitTaskResponse response = delegateServiceBlockingStub.withDeadlineAfter(30, TimeUnit.SECONDS)
                                      .submitTask(submitTaskRequestBuilder.build());

    return response.getTaskId();
  }

  public TaskExecutionStage cancelTask(AccountId accountId, TaskId taskId) {
    CancelTaskResponse response =
        delegateServiceBlockingStub.withDeadlineAfter(30, TimeUnit.SECONDS)
            .cancelTask(CancelTaskRequest.newBuilder().setAccountId(accountId).setTaskId(taskId).build());

    return response.getCanceledAtStage();
  }

  public TaskExecutionStage taskProgress(AccountId accountId, TaskId taskId) {
    TaskProgressResponse response =
        delegateServiceBlockingStub.withDeadlineAfter(30, TimeUnit.SECONDS)
            .taskProgress(TaskProgressRequest.newBuilder().setAccountId(accountId).setTaskId(taskId).build());

    return response.getCurrentlyAtStage();
  }

  public void taskProgressUpdate(
      AccountId accountId, TaskId taskId, Consumer<TaskExecutionStage> taskExecutionStageConsumer) {
    Iterator<TaskProgressUpdatesResponse> responseIterator = delegateServiceBlockingStub.taskProgressUpdates(
        TaskProgressUpdatesRequest.newBuilder().setAccountId(accountId).setTaskId(taskId).build());

    while (responseIterator.hasNext()) {
      taskExecutionStageConsumer.accept(responseIterator.next().getCurrentlyAtStage());
    }
  }

  public PerpetualTaskId createPerpetualTask(AccountId accountId, String type, PerpetualTaskSchedule schedule,
      PerpetualTaskClientContextDetails context, boolean allowDuplicate, String taskDescription) {
    CreatePerpetualTaskResponse response = delegateServiceBlockingStub.withDeadlineAfter(30, TimeUnit.SECONDS)
                                               .createPerpetualTask(CreatePerpetualTaskRequest.newBuilder()
                                                                        .setAccountId(accountId)
                                                                        .setType(type)
                                                                        .setSchedule(schedule)
                                                                        .setContext(context)
                                                                        .setAllowDuplicate(allowDuplicate)
                                                                        .setTaskDescription(taskDescription)
                                                                        .build());

    return response.getPerpetualTaskId();
  }

  public void deletePerpetualTask(AccountId accountId, PerpetualTaskId perpetualTaskId) {
    delegateServiceBlockingStub.withDeadlineAfter(30, TimeUnit.SECONDS)
        .deletePerpetualTask(DeletePerpetualTaskRequest.newBuilder()
                                 .setAccountId(accountId)
                                 .setPerpetualTaskId(perpetualTaskId)
                                 .build());
  }

  public void resetPerpetualTask(
      AccountId accountId, PerpetualTaskId perpetualTaskId, PerpetualTaskExecutionBundle taskExecutionBundle) {
    delegateServiceBlockingStub.withDeadlineAfter(30, TimeUnit.SECONDS)
        .resetPerpetualTask(ResetPerpetualTaskRequest.newBuilder()
                                .setAccountId(accountId)
                                .setPerpetualTaskId(perpetualTaskId)
                                .setTaskExecutionBundle(taskExecutionBundle)
                                .build());
  }

  public DelegateCallbackToken registerCallback(DelegateCallback delegateCallback) {
    RegisterCallbackResponse response = delegateServiceBlockingStub.registerCallback(
        RegisterCallbackRequest.newBuilder().setCallback(delegateCallback).build());
    return response.getCallbackToken();
  }

  public ObtainDocumentResponse obtainDocument(ObtainDocumentRequest request) {
    return delegateServiceBlockingStub.obtainDocument(request);
  }
}

package io.harness.grpc;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.stream.Collectors.toList;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.ByteString;

import io.grpc.StatusRuntimeException;
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
import io.harness.delegate.TaskSelector;
import io.harness.delegate.TaskSetupAbstractions;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.exception.DelegateServiceDriverException;
import io.harness.perpetualtask.PerpetualTaskClientContextDetails;
import io.harness.perpetualtask.PerpetualTaskExecutionBundle;
import io.harness.perpetualtask.PerpetualTaskId;
import io.harness.perpetualtask.PerpetualTaskSchedule;
import io.harness.serializer.KryoSerializer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;

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
    try {
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
    } catch (StatusRuntimeException ex) {
      throw new DelegateServiceDriverException("Unexpected error occurred while submitting task.", ex);
    }
  }

  public TaskExecutionStage cancelTask(AccountId accountId, TaskId taskId) {
    try {
      CancelTaskResponse response =
          delegateServiceBlockingStub.withDeadlineAfter(30, TimeUnit.SECONDS)
              .cancelTask(CancelTaskRequest.newBuilder().setAccountId(accountId).setTaskId(taskId).build());

      return response.getCanceledAtStage();
    } catch (StatusRuntimeException ex) {
      throw new DelegateServiceDriverException("Unexpected error occurred while cancelling task.", ex);
    }
  }

  public TaskExecutionStage taskProgress(AccountId accountId, TaskId taskId) {
    try {
      TaskProgressResponse response =
          delegateServiceBlockingStub.withDeadlineAfter(30, TimeUnit.SECONDS)
              .taskProgress(TaskProgressRequest.newBuilder().setAccountId(accountId).setTaskId(taskId).build());

      return response.getCurrentlyAtStage();
    } catch (StatusRuntimeException ex) {
      throw new DelegateServiceDriverException("Unexpected error occurred while checking task progress.", ex);
    }
  }

  public void taskProgressUpdate(
      AccountId accountId, TaskId taskId, Consumer<TaskExecutionStage> taskExecutionStageConsumer) {
    throw new NotImplementedException(
        "Temporarily removed the implementation until we find more effective way of doing this.");
  }

  public PerpetualTaskId createPerpetualTask(AccountId accountId, String type, PerpetualTaskSchedule schedule,
      PerpetualTaskClientContextDetails context, boolean allowDuplicate, String taskDescription) {
    try {
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
    } catch (StatusRuntimeException ex) {
      throw new DelegateServiceDriverException("Unexpected error occurred while creating perpetual task.", ex);
    }
  }

  public void deletePerpetualTask(AccountId accountId, PerpetualTaskId perpetualTaskId) {
    try {
      delegateServiceBlockingStub.withDeadlineAfter(30, TimeUnit.SECONDS)
          .deletePerpetualTask(DeletePerpetualTaskRequest.newBuilder()
                                   .setAccountId(accountId)
                                   .setPerpetualTaskId(perpetualTaskId)
                                   .build());
    } catch (StatusRuntimeException ex) {
      throw new DelegateServiceDriverException("Unexpected error occurred while deleting perpetual task.", ex);
    }
  }

  public void resetPerpetualTask(
      AccountId accountId, PerpetualTaskId perpetualTaskId, PerpetualTaskExecutionBundle taskExecutionBundle) {
    try {
      delegateServiceBlockingStub.withDeadlineAfter(30, TimeUnit.SECONDS)
          .resetPerpetualTask(ResetPerpetualTaskRequest.newBuilder()
                                  .setAccountId(accountId)
                                  .setPerpetualTaskId(perpetualTaskId)
                                  .setTaskExecutionBundle(taskExecutionBundle)
                                  .build());
    } catch (StatusRuntimeException ex) {
      throw new DelegateServiceDriverException("Unexpected error occurred while resetting perpetual task.", ex);
    }
  }

  public DelegateCallbackToken registerCallback(DelegateCallback delegateCallback) {
    try {
      RegisterCallbackResponse response = delegateServiceBlockingStub.registerCallback(
          RegisterCallbackRequest.newBuilder().setCallback(delegateCallback).build());
      return response.getCallbackToken();
    } catch (StatusRuntimeException ex) {
      throw new DelegateServiceDriverException("Unexpected error occurred while registering callback.", ex);
    }
  }

  public ObtainDocumentResponse obtainDocument(ObtainDocumentRequest request) {
    return delegateServiceBlockingStub.obtainDocument(request);
  }
}

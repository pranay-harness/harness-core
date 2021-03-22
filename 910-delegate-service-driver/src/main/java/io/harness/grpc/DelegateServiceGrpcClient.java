package io.harness.grpc;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static java.lang.System.currentTimeMillis;
import static java.util.stream.Collectors.toList;

import io.harness.beans.DelegateTaskRequest;
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
import io.harness.delegate.TaskLogAbstractions;
import io.harness.delegate.TaskMode;
import io.harness.delegate.TaskProgressRequest;
import io.harness.delegate.TaskProgressResponse;
import io.harness.delegate.TaskSelector;
import io.harness.delegate.TaskSetupAbstractions;
import io.harness.delegate.TaskType;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.TaskParameters;
import io.harness.exception.DelegateServiceDriverException;
import io.harness.grpc.utils.HTimestamps;
import io.harness.perpetualtask.PerpetualTaskClientContextDetails;
import io.harness.perpetualtask.PerpetualTaskExecutionBundle;
import io.harness.perpetualtask.PerpetualTaskId;
import io.harness.perpetualtask.PerpetualTaskSchedule;
import io.harness.serializer.KryoSerializer;
import io.harness.service.intfc.DelegateAsyncService;
import io.harness.service.intfc.DelegateSyncService;
import io.harness.tasks.ResponseData;

import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import com.google.protobuf.util.Durations;
import com.google.protobuf.util.Timestamps;
import io.fabric8.utils.Strings;
import io.grpc.StatusRuntimeException;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.NotImplementedException;

@Slf4j
public class DelegateServiceGrpcClient {
  private final DelegateServiceBlockingStub delegateServiceBlockingStub;
  private final DelegateAsyncService delegateAsyncService;
  private final KryoSerializer kryoSerializer;
  private final DelegateSyncService delegateSyncService;

  @Inject
  public DelegateServiceGrpcClient(DelegateServiceBlockingStub delegateServiceBlockingStub,
      DelegateAsyncService delegateAsyncService, KryoSerializer kryoSerializer,
      DelegateSyncService delegateSyncService) {
    this.delegateServiceBlockingStub = delegateServiceBlockingStub;
    this.delegateAsyncService = delegateAsyncService;
    this.kryoSerializer = kryoSerializer;
    this.delegateSyncService = delegateSyncService;
  }

  public String submitAsyncTask(
      DelegateTaskRequest taskRequest, DelegateCallbackToken delegateCallbackToken, Duration holdFor) {
    final SubmitTaskResponse submitTaskResponse =
        submitTaskInternal(TaskMode.ASYNC, taskRequest, delegateCallbackToken, holdFor);
    return submitTaskResponse.getTaskId().getId();
  }

  public <T extends DelegateResponseData> T executeSyncTask(
      DelegateTaskRequest taskRequest, DelegateCallbackToken delegateCallbackToken) {
    final SubmitTaskResponse submitTaskResponse =
        submitTaskInternal(TaskMode.SYNC, taskRequest, delegateCallbackToken, Duration.ZERO);
    final String taskId = submitTaskResponse.getTaskId().getId();
    log.info("sync task id created =[{}]", taskId);
    return delegateSyncService.waitForTask(taskId,
        Strings.defaultIfEmpty(taskRequest.getTaskDescription(), taskRequest.getTaskType()),
        Duration.ofMillis(HTimestamps.toMillis(submitTaskResponse.getTotalExpiry()) - currentTimeMillis()));
  }

  /**
   * This api, doesn't deserialize delegate response into specific objects.
   * This api can just return response containing byte[] and let
   * caller deserialize it.
   *
   * So your delegateSyncService implementation doesn't need to know about
   * all DelegeteResponse objects and caller can take that responsibility.
   *
   * e.g. Please refer to PmsDelegateSyncServiceImpl
   * (PmsDelegateSyncServiceImpl.waitForTask())
   *
   * @param taskRequest
   * @param delegateCallbackToken
   * @param <T>
   * @return
   */
  public <T extends ResponseData> T executeSyncTaskReturningResponseData(
      DelegateTaskRequest taskRequest, DelegateCallbackToken delegateCallbackToken) {
    final SubmitTaskResponse submitTaskResponse =
        submitTaskInternal(TaskMode.SYNC, taskRequest, delegateCallbackToken, Duration.ZERO);
    final String taskId = submitTaskResponse.getTaskId().getId();
    log.info("ID for Sync Task =[{}]", taskId);
    return delegateSyncService.waitForTask(taskId,
        Strings.defaultIfEmpty(taskRequest.getTaskDescription(), taskRequest.getTaskType()),
        Duration.ofMillis(HTimestamps.toMillis(submitTaskResponse.getTotalExpiry()) - currentTimeMillis()));
  }

  public SubmitTaskResponse submitTask(DelegateCallbackToken delegateCallbackToken, AccountId accountId,
      TaskSetupAbstractions taskSetupAbstractions, TaskLogAbstractions taskLogAbstractions, TaskDetails taskDetails,
      List<ExecutionCapability> capabilities, List<String> taskSelectors, Duration holdFor) {
    try {
      SubmitTaskRequest.Builder submitTaskRequestBuilder = SubmitTaskRequest.newBuilder()
                                                               .setCallbackToken(delegateCallbackToken)
                                                               .setAccountId(accountId)
                                                               .setSetupAbstractions(taskSetupAbstractions)
                                                               .setLogAbstractions(taskLogAbstractions)
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

      if (taskDetails.getMode() == TaskMode.ASYNC) {
        delegateAsyncService.setupTimeoutForTask(response.getTaskId().getId(),
            Timestamps.toMillis(response.getTotalExpiry()), currentTimeMillis() + holdFor.toMillis());
      }

      return response;
    } catch (StatusRuntimeException ex) {
      throw new DelegateServiceDriverException("Unexpected error occurred while submitting task.", ex);
    }
  }

  private SubmitTaskResponse submitTaskInternal(TaskMode taskMode, DelegateTaskRequest taskRequest,
      DelegateCallbackToken delegateCallbackToken, Duration holdFor) {
    final TaskParameters taskParameters = taskRequest.getTaskParameters();

    final List<ExecutionCapability> capabilities = (taskParameters instanceof ExecutionCapabilityDemander)
        ? ListUtils.emptyIfNull(((ExecutionCapabilityDemander) taskParameters).fetchRequiredExecutionCapabilities(null))
        : Collections.emptyList();

    return submitTask(delegateCallbackToken, AccountId.newBuilder().setId(taskRequest.getAccountId()).build(),
        TaskSetupAbstractions.newBuilder()
            .putAllValues(MapUtils.emptyIfNull(taskRequest.getTaskSetupAbstractions()))
            .build(),
        TaskLogAbstractions.newBuilder()
            .putAllValues(MapUtils.emptyIfNull(taskRequest.getLogStreamingAbstractions()))
            .build(),
        TaskDetails.newBuilder()
            .setParked(taskRequest.isParked())
            .setMode(taskMode)
            .setType(TaskType.newBuilder().setType(taskRequest.getTaskType()).build())
            .setKryoParameters(ByteString.copyFrom(kryoSerializer.asDeflatedBytes(taskParameters)))
            .setExecutionTimeout(Durations.fromSeconds(taskRequest.getExecutionTimeout().getSeconds()))
            .build(),
        capabilities, taskRequest.getTaskSelectors(), holdFor);
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

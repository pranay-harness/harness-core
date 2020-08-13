package io.harness.grpc;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.util.Durations;
import com.google.protobuf.util.Timestamps;

import io.grpc.stub.StreamObserver;
import io.harness.beans.DelegateTask;
import io.harness.beans.DelegateTask.DelegateTaskKeys;
import io.harness.beans.DelegateTask.Status;
import io.harness.callback.DelegateCallbackToken;
import io.harness.delegate.CancelTaskRequest;
import io.harness.delegate.CancelTaskResponse;
import io.harness.delegate.CreatePerpetualTaskRequest;
import io.harness.delegate.CreatePerpetualTaskResponse;
import io.harness.delegate.DelegateServiceGrpc.DelegateServiceImplBase;
import io.harness.delegate.DeletePerpetualTaskRequest;
import io.harness.delegate.DeletePerpetualTaskResponse;
import io.harness.delegate.RegisterCallbackRequest;
import io.harness.delegate.RegisterCallbackResponse;
import io.harness.delegate.ResetPerpetualTaskRequest;
import io.harness.delegate.ResetPerpetualTaskResponse;
import io.harness.delegate.SubmitTaskRequest;
import io.harness.delegate.SubmitTaskResponse;
import io.harness.delegate.TaskDetails;
import io.harness.delegate.TaskExecutionStage;
import io.harness.delegate.TaskId;
import io.harness.delegate.TaskMode;
import io.harness.delegate.TaskProgressRequest;
import io.harness.delegate.TaskProgressResponse;
import io.harness.delegate.TaskProgressUpdatesRequest;
import io.harness.delegate.TaskProgressUpdatesResponse;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.perpetualtask.PerpetualTaskClientContext;
import io.harness.perpetualtask.PerpetualTaskClientContext.PerpetualTaskClientContextBuilder;
import io.harness.perpetualtask.PerpetualTaskId;
import io.harness.perpetualtask.PerpetualTaskService;
import io.harness.serializer.KryoSerializer;
import io.harness.service.intfc.DelegateCallbackRegistry;
import software.wings.service.intfc.DelegateService;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Singleton
public class DelegateServiceGrpcImpl extends DelegateServiceImplBase {
  private DelegateCallbackRegistry delegateCallbackRegistry;
  private PerpetualTaskService perpetualTaskService;
  private DelegateService delegateService;
  private KryoSerializer kryoSerializer;

  @Inject
  public DelegateServiceGrpcImpl(DelegateCallbackRegistry delegateCallbackRegistry,
      PerpetualTaskService perpetualTaskService, DelegateService delegateService, KryoSerializer kryoSerializer) {
    this.delegateCallbackRegistry = delegateCallbackRegistry;
    this.perpetualTaskService = perpetualTaskService;
    this.delegateService = delegateService;
    this.kryoSerializer = kryoSerializer;
  }

  @Override
  public void submitTask(SubmitTaskRequest request, StreamObserver<SubmitTaskResponse> responseObserver) {
    String taskId = generateUuid();
    TaskDetails taskDetails = request.getDetails();
    Map<String, String> setupAbstractions = request.getSetupAbstractions().getValuesMap();
    List<ExecutionCapability> capabilities =
        request.getCapabilitiesList()
            .stream()
            .map(capability
                -> (ExecutionCapability) kryoSerializer.asInflatedObject(capability.getKryoCapability().toByteArray()))
            .collect(Collectors.toList());
    List<String> taskSelectors =
        request.getSelectorsList().stream().map(selector -> selector.getSelector()).collect(Collectors.toList());

    DelegateTask task =
        DelegateTask.builder()
            .uuid(taskId)
            .driverId(request.hasCallbackToken() ? request.getCallbackToken().getToken() : null)
            .waitId(taskId)
            .accountId(request.getAccountId().getId())
            .setupAbstractions(setupAbstractions)
            .workflowExecutionId(setupAbstractions.get(DelegateTaskKeys.workflowExecutionId))
            .executionCapabilities(capabilities)
            .tags(taskSelectors)
            .data(TaskData.builder()
                      .async(taskDetails.getMode() == TaskMode.ASYNC)
                      .taskType(taskDetails.getType().getType())
                      .parameters(
                          new Object[] {kryoSerializer.asInflatedObject(taskDetails.getKryoParameters().toByteArray())})
                      .timeout(Durations.toMillis(taskDetails.getExecutionTimeout()))
                      .expressionFunctorToken((int) taskDetails.getExpressionFunctorToken())
                      .expressions(taskDetails.getExpressionsMap())
                      .build())
            .build();

    if (task.getData().isAsync()) {
      delegateService.queueTask(task);
    } else {
      delegateService.scheduleSyncTask(task);
    }

    responseObserver.onNext(
        SubmitTaskResponse.newBuilder().setTaskId(TaskId.newBuilder().setId(taskId).build()).build());
    responseObserver.onCompleted();
  }

  @Override
  public void cancelTask(CancelTaskRequest request, StreamObserver<CancelTaskResponse> responseObserver) {
    DelegateTask preAbortedTask =
        delegateService.abortTask(request.getAccountId().getId(), request.getTaskId().getId());
    if (preAbortedTask != null) {
      responseObserver.onNext(
          CancelTaskResponse.newBuilder()
              .setCanceledAtStage(DelegateTaskGrpcUtils.mapTaskStatusToTaskExecutionStage(preAbortedTask.getStatus()))
              .build());
      responseObserver.onCompleted();
      return;
    }

    responseObserver.onNext(
        CancelTaskResponse.newBuilder().setCanceledAtStage(TaskExecutionStage.TYPE_UNSPECIFIED).build());
    responseObserver.onCompleted();
  }

  @Override
  public void taskProgress(TaskProgressRequest request, StreamObserver<TaskProgressResponse> responseObserver) {
    Optional<DelegateTask> delegateTaskOptional =
        delegateService.fetchDelegateTask(request.getAccountId().getId(), request.getTaskId().getId());

    if (delegateTaskOptional.isPresent()) {
      responseObserver.onNext(TaskProgressResponse.newBuilder()
                                  .setCurrentlyAtStage(DelegateTaskGrpcUtils.mapTaskStatusToTaskExecutionStage(
                                      delegateTaskOptional.get().getStatus()))
                                  .build());
      responseObserver.onCompleted();
      return;
    }

    responseObserver.onNext(
        TaskProgressResponse.newBuilder().setCurrentlyAtStage(TaskExecutionStage.TYPE_UNSPECIFIED).build());
    responseObserver.onCompleted();
  }

  @Override
  public void taskProgressUpdates(
      TaskProgressUpdatesRequest request, StreamObserver<TaskProgressUpdatesResponse> responseObserver) {
    Optional<DelegateTask> delegateTaskOptional =
        delegateService.fetchDelegateTask(request.getAccountId().getId(), request.getTaskId().getId());

    while (delegateTaskOptional.isPresent()) {
      if (Status.isFinalStatus(delegateTaskOptional.get().getStatus())) {
        responseObserver.onNext(TaskProgressUpdatesResponse.newBuilder()
                                    .setCurrentlyAtStage(DelegateTaskGrpcUtils.mapTaskStatusToTaskExecutionStage(
                                        delegateTaskOptional.get().getStatus()))
                                    .build());
        responseObserver.onCompleted();
        return;
      }

      responseObserver.onNext(TaskProgressUpdatesResponse.newBuilder()
                                  .setCurrentlyAtStage(DelegateTaskGrpcUtils.mapTaskStatusToTaskExecutionStage(
                                      delegateTaskOptional.get().getStatus()))
                                  .build());

      try {
        Thread.sleep(3000L);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }

      delegateTaskOptional =
          delegateService.fetchDelegateTask(request.getAccountId().getId(), request.getTaskId().getId());
    }

    responseObserver.onNext(
        TaskProgressUpdatesResponse.newBuilder().setCurrentlyAtStage(TaskExecutionStage.TYPE_UNSPECIFIED).build());
    responseObserver.onCompleted();
  }

  @Override
  public void registerCallback(
      RegisterCallbackRequest request, StreamObserver<RegisterCallbackResponse> responseObserver) {
    String token = delegateCallbackRegistry.ensureCallback(request.getCallback());
    responseObserver.onNext(RegisterCallbackResponse.newBuilder()
                                .setCallbackToken(DelegateCallbackToken.newBuilder().setToken(token))
                                .build());
    responseObserver.onCompleted();
  }

  @Override
  public void createPerpetualTask(
      CreatePerpetualTaskRequest request, StreamObserver<CreatePerpetualTaskResponse> responseObserver) {
    String accountId = request.getAccountId().getId();

    PerpetualTaskClientContextBuilder contextBuilder = PerpetualTaskClientContext.builder();

    if (request.getContext().hasTaskClientParams()) {
      contextBuilder.clientParams(request.getContext().getTaskClientParams().getParamsMap());
    } else if (request.getContext().hasExecutionBundle()) {
      contextBuilder.executionBundle(request.getContext().getExecutionBundle().toByteArray());
    }

    if (request.getContext().getLastContextUpdated() != null) {
      contextBuilder.lastContextUpdated(Timestamps.toMillis(request.getContext().getLastContextUpdated()));
    }

    String perpetualTaskId = perpetualTaskService.createTask(request.getType(), accountId, contextBuilder.build(),
        request.getSchedule(), request.getAllowDuplicate(), request.getTaskDescription());

    responseObserver.onNext(CreatePerpetualTaskResponse.newBuilder()
                                .setPerpetualTaskId(PerpetualTaskId.newBuilder().setId(perpetualTaskId).build())
                                .build());
    responseObserver.onCompleted();
  }

  @Override
  public void deletePerpetualTask(
      DeletePerpetualTaskRequest request, StreamObserver<DeletePerpetualTaskResponse> responseObserver) {
    perpetualTaskService.deleteTask(request.getAccountId().getId(), request.getPerpetualTaskId().getId());

    responseObserver.onNext(DeletePerpetualTaskResponse.newBuilder().build());
    responseObserver.onCompleted();
  }

  @Override
  public void resetPerpetualTask(
      ResetPerpetualTaskRequest request, StreamObserver<ResetPerpetualTaskResponse> responseObserver) {
    perpetualTaskService.resetTask(
        request.getAccountId().getId(), request.getPerpetualTaskId().getId(), request.getTaskExecutionBundle());

    responseObserver.onNext(ResetPerpetualTaskResponse.newBuilder().build());
    responseObserver.onCompleted();
  }
}

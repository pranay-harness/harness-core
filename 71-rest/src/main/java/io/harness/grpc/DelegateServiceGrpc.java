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
import io.harness.delegate.CancelTaskRequest;
import io.harness.delegate.CancelTaskResponse;
import io.harness.delegate.CreatePerpetualTaskRequest;
import io.harness.delegate.CreatePerpetualTaskResponse;
import io.harness.delegate.DelegateServiceGrpc.DelegateServiceImplBase;
import io.harness.delegate.DeletePerpetualTaskRequest;
import io.harness.delegate.DeletePerpetualTaskResponse;
import io.harness.delegate.RegisterPerpetualTaskClientEntrypointRequest;
import io.harness.delegate.RegisterPerpetualTaskClientEntrypointResponse;
import io.harness.delegate.ResetPerpetualTaskRequest;
import io.harness.delegate.ResetPerpetualTaskResponse;
import io.harness.delegate.SubmitTaskRequest;
import io.harness.delegate.SubmitTaskResponse;
import io.harness.delegate.TaskDetails;
import io.harness.delegate.TaskExecutionStage;
import io.harness.delegate.TaskId;
import io.harness.delegate.TaskProgressRequest;
import io.harness.delegate.TaskProgressResponse;
import io.harness.delegate.TaskProgressUpdatesRequest;
import io.harness.delegate.TaskProgressUpdatesResponse;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.perpetualtask.HttpsPerpetualTaskServiceClient;
import io.harness.perpetualtask.HttpsPerpetualTaskServiceClientImpl;
import io.harness.perpetualtask.PerpetualTaskClientContext;
import io.harness.perpetualtask.PerpetualTaskId;
import io.harness.perpetualtask.PerpetualTaskService;
import io.harness.perpetualtask.PerpetualTaskServiceClientRegistry;
import io.harness.serializer.KryoSerializer;
import software.wings.service.intfc.DelegateService;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Singleton
public class DelegateServiceGrpc extends DelegateServiceImplBase {
  private PerpetualTaskServiceClientRegistry perpetualTaskServiceClientRegistry;
  private PerpetualTaskService perpetualTaskService;
  private DelegateService delegateService;
  private KryoSerializer kryoSerializer;

  @Inject
  public DelegateServiceGrpc(PerpetualTaskServiceClientRegistry perpetualTaskServiceClientRegistry,
      PerpetualTaskService perpetualTaskService, DelegateService delegateService, KryoSerializer kryoSerializer) {
    this.perpetualTaskServiceClientRegistry = perpetualTaskServiceClientRegistry;
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

    DelegateTask task =
        DelegateTask.builder()
            .uuid(taskId)
            .waitId(taskId)
            .accountId(request.getAccountId().getId())
            .appId(setupAbstractions.get(DelegateTaskKeys.appId))
            .envId(setupAbstractions.get(DelegateTaskKeys.envId))
            .infrastructureMappingId(setupAbstractions.get(DelegateTaskKeys.infrastructureMappingId))
            .serviceTemplateId(setupAbstractions.get(DelegateTaskKeys.serviceTemplateId))
            .artifactStreamId(setupAbstractions.get(DelegateTaskKeys.artifactStreamId))
            .workflowExecutionId(setupAbstractions.get(DelegateTaskKeys.workflowExecutionId))
            .executionCapabilities(capabilities)
            .data(TaskData.builder()
                      .taskType(taskDetails.getType().getType())
                      .parameters(
                          new Object[] {kryoSerializer.asInflatedObject(taskDetails.getKryoParameters().toByteArray())})
                      .timeout(Durations.toMillis(taskDetails.getExecutionTimeout()))
                      .expressionFunctorToken((int) taskDetails.getExpressionFunctorToken())
                      .expressions(taskDetails.getExpressionsMap())
                      .build())
            .build();

    try {
      delegateService.executeTask(task);

      responseObserver.onNext(
          SubmitTaskResponse.newBuilder().setTaskId(TaskId.newBuilder().setId(taskId).build()).build());
      responseObserver.onCompleted();
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      responseObserver.onError(ex);
    }
  }

  @Override
  public void cancelTask(CancelTaskRequest request, StreamObserver<CancelTaskResponse> responseObserver) {
    DelegateTask preAbortedTask =
        delegateService.abortTask(request.getAccountId().getId(), request.getTaskId().getId());
    if (preAbortedTask != null) {
      responseObserver.onNext(CancelTaskResponse.newBuilder()
                                  .setCanceledAtStage(mapTaskStatusToTaskExecutionStage(preAbortedTask.getStatus()))
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
      responseObserver.onNext(
          TaskProgressResponse.newBuilder()
              .setCurrentlyAtStage(mapTaskStatusToTaskExecutionStage(delegateTaskOptional.get().getStatus()))
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
        responseObserver.onNext(
            TaskProgressUpdatesResponse.newBuilder()
                .setCurrentlyAtStage(mapTaskStatusToTaskExecutionStage(delegateTaskOptional.get().getStatus()))
                .build());
        responseObserver.onCompleted();
        return;
      }

      responseObserver.onNext(
          TaskProgressUpdatesResponse.newBuilder()
              .setCurrentlyAtStage(mapTaskStatusToTaskExecutionStage(delegateTaskOptional.get().getStatus()))
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
  public void registerPerpetualTaskClientEntrypoint(RegisterPerpetualTaskClientEntrypointRequest request,
      StreamObserver<RegisterPerpetualTaskClientEntrypointResponse> responseObserver) {
    HttpsPerpetualTaskServiceClient httpsClient =
        new HttpsPerpetualTaskServiceClientImpl(request.getPerpetualTaskClientEntrypoint().getHttpsEntrypoint());

    perpetualTaskServiceClientRegistry.registerClient(request.getType(), httpsClient);

    responseObserver.onNext(RegisterPerpetualTaskClientEntrypointResponse.newBuilder().build());
    responseObserver.onCompleted();
  }

  @Override
  public void createPerpetualTask(
      CreatePerpetualTaskRequest request, StreamObserver<CreatePerpetualTaskResponse> responseObserver) {
    String accountId = request.getAccountId().getId();

    PerpetualTaskClientContext context = new PerpetualTaskClientContext(request.getContext().getTaskClientParamsMap());
    if (request.getContext().getLastContextUpdated() != null) {
      context.setLastContextUpdated(Timestamps.toMillis(request.getContext().getLastContextUpdated()));
    }

    String perpetualTaskId = perpetualTaskService.createTask(
        request.getType(), accountId, context, request.getSchedule(), request.getAllowDuplicate());

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
    perpetualTaskService.resetTask(request.getAccountId().getId(), request.getPerpetualTaskId().getId());

    responseObserver.onNext(ResetPerpetualTaskResponse.newBuilder().build());
    responseObserver.onCompleted();
  }

  private TaskExecutionStage mapTaskStatusToTaskExecutionStage(DelegateTask.Status taskStatus) {
    switch (taskStatus) {
      case QUEUED:
        return TaskExecutionStage.QUEUEING;
      case STARTED:
        return TaskExecutionStage.EXECUTING;
      case FINISHED:
        return TaskExecutionStage.FINISHED;
      case ERROR:
        return TaskExecutionStage.FAILED;
      case ABORTED:
        return TaskExecutionStage.ABORTED;
      default:
        return TaskExecutionStage.TYPE_UNSPECIFIED;
    }
  }
}

package io.harness.grpc.ng.manager;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.waiter.OrchestrationNotifyEventListener.ORCHESTRATION;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.ByteString;
import com.google.protobuf.util.Durations;
import com.google.protobuf.util.Timestamps;

import io.grpc.stub.StreamObserver;
import io.harness.beans.DelegateTask;
import io.harness.delegate.AbortTaskRequest;
import io.harness.delegate.AbortTaskResponse;
import io.harness.delegate.NgDelegateTaskServiceGrpc;
import io.harness.delegate.NgTaskDetails;
import io.harness.delegate.NgTaskExecutionStage;
import io.harness.delegate.NgTaskId;
import io.harness.delegate.SendTaskAsyncRequest;
import io.harness.delegate.SendTaskAsyncResponse;
import io.harness.delegate.SendTaskRequest;
import io.harness.delegate.SendTaskResponse;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.task.TaskParameters;
import io.harness.grpc.DelegateTaskGrpcUtils;
import io.harness.perpetualtask.CreateRemotePerpetualTaskRequest;
import io.harness.perpetualtask.CreateRemotePerpetualTaskResponse;
import io.harness.perpetualtask.DeleteRemotePerpetualTaskRequest;
import io.harness.perpetualtask.DeleteRemotePerpetualTaskResponse;
import io.harness.perpetualtask.PerpetualTaskClientContext;
import io.harness.perpetualtask.PerpetualTaskClientContext.PerpetualTaskClientContextBuilder;
import io.harness.perpetualtask.PerpetualTaskSchedule;
import io.harness.perpetualtask.PerpetualTaskService;
import io.harness.perpetualtask.RemotePerpetualTaskSchedule;
import io.harness.perpetualtask.ResetRemotePerpetualTaskRequest;
import io.harness.perpetualtask.ResetRemotePerpetualTaskResponse;
import io.harness.serializer.KryoSerializer;
import io.harness.waiter.WaitNotifyEngine;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import software.wings.service.intfc.DelegateService;

import java.util.Map;

@Singleton
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@Slf4j
public class DelegateTaskGrpcServer extends NgDelegateTaskServiceGrpc.NgDelegateTaskServiceImplBase {
  private final DelegateService delegateService;
  private final KryoSerializer kryoSerializer;
  private final WaitNotifyEngine waitNotifyEngine;
  private final PerpetualTaskService perpetualTaskService;

  @Override
  public void sendTask(SendTaskRequest request, StreamObserver<SendTaskResponse> responseObserver) {
    try {
      DelegateTask task = extractDelegateTask(request);
      ResponseData responseData = delegateService.executeTask(task);
      responseObserver.onNext(SendTaskResponse.newBuilder()
                                  .setTaskId(NgTaskId.newBuilder().setId(task.getUuid()).build())
                                  .setResponseData(ByteString.copyFrom(kryoSerializer.asDeflatedBytes(responseData)))
                                  .build());
      responseObserver.onCompleted();
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
      responseObserver.onError(ie);
    } catch (Exception ex) {
      responseObserver.onError(ex);
    }
  }

  @Override
  public void sendTaskAsync(SendTaskAsyncRequest request, StreamObserver<SendTaskAsyncResponse> responseObserver) {
    try {
      DelegateTask task = extractDelegateTask(request);
      String taskId = delegateService.queueTask(task);
      waitNotifyEngine.waitForAllOn(ORCHESTRATION, DelegateTaskResumeCallback.builder().taskId(taskId).build(), taskId);
      responseObserver.onNext(
          SendTaskAsyncResponse.newBuilder().setTaskId(NgTaskId.newBuilder().setId(taskId).build()).build());
      responseObserver.onCompleted();
    } catch (Exception ex) {
      responseObserver.onError(ex);
    }
  }

  @Override
  public void abortTask(
      AbortTaskRequest request, StreamObserver<io.harness.delegate.AbortTaskResponse> responseObserver) {
    DelegateTask task = delegateService.abortTask(request.getAccountId().getId(), request.getTaskId().getId());
    if (task != null) {
      responseObserver.onNext(
          AbortTaskResponse.newBuilder()
              .setCanceledAtStage(DelegateTaskGrpcUtils.mapTaskStatusToNgTaskExecutionStage(task.getStatus()))
              .build());
      responseObserver.onCompleted();
      return;
    }
    responseObserver.onNext(
        AbortTaskResponse.newBuilder().setCanceledAtStage(NgTaskExecutionStage.TYPE_UNSPECIFIED).build());
    responseObserver.onCompleted();
  }

  private DelegateTask extractDelegateTask(SendTaskRequest request) {
    NgTaskDetails taskDetails = request.getDetails();
    String accountId = request.getAccountId().getId();
    Map<String, String> setupAbstractions = request.getSetupAbstractions().getValuesMap();
    return extractDelegateTask(accountId, setupAbstractions, taskDetails);
  }

  private DelegateTask extractDelegateTask(SendTaskAsyncRequest request) {
    NgTaskDetails taskDetails = request.getDetails();
    String accountId = request.getAccountId().getId();
    Map<String, String> setupAbstractions = request.getSetupAbstractions().getValuesMap();
    return extractDelegateTask(accountId, setupAbstractions, taskDetails);
  }

  private DelegateTask extractDelegateTask(
      String accountId, Map<String, String> setupAbstractions, NgTaskDetails taskDetails) {
    TaskParameters parameters =
        (TaskParameters) kryoSerializer.asInflatedObject(taskDetails.getKryoParameters().toByteArray());
    String taskId = generateUuid();
    return DelegateTask.builder()
        .uuid(taskId)
        .waitId(taskId)
        .accountId(accountId)
        .setupAbstractions(setupAbstractions)
        .data(TaskData.builder()
                  .taskType(taskDetails.getType().getType())
                  .parameters(new Object[] {parameters})
                  .timeout(Durations.toMillis(taskDetails.getExecutionTimeout()))
                  .expressionFunctorToken((int) taskDetails.getExpressionFunctorToken())
                  .expressions(taskDetails.getExpressionsMap())
                  .build())
        .build();
  }

  @Override
  public void createRemotePerpetualTask(
      CreateRemotePerpetualTaskRequest request, StreamObserver<CreateRemotePerpetualTaskResponse> responseObserver) {
    final String accountId = request.getAccountId();

    final PerpetualTaskClientContextBuilder contextBuilder = PerpetualTaskClientContext.builder();

    contextBuilder.clientParams(MapUtils.emptyIfNull(request.getContext().getTaskClientParamsMap()));

    if (request.getContext().getLastContextUpdated() != null) {
      contextBuilder.lastContextUpdated(Timestamps.toMillis(request.getContext().getLastContextUpdated()));
    }
    String perpetualTaskId = perpetualTaskService.createTask(request.getTaskType(), accountId, contextBuilder.build(),
        convertSchedule(request.getSchedule()), request.getAllowDuplicate(), request.getTaskDescription());

    responseObserver.onNext(CreateRemotePerpetualTaskResponse.newBuilder().setPerpetualTaskId(perpetualTaskId).build());
    responseObserver.onCompleted();
  }

  private PerpetualTaskSchedule convertSchedule(RemotePerpetualTaskSchedule schedule) {
    return PerpetualTaskSchedule.newBuilder()
        .setInterval(schedule.getInterval())
        .setTimeout(schedule.getTimeout())
        .build();
  }

  @Override
  public void deleteRemotePerpetualTask(
      DeleteRemotePerpetualTaskRequest request, StreamObserver<DeleteRemotePerpetualTaskResponse> responseObserver) {
    final boolean success = perpetualTaskService.deleteTask(request.getAccountId(), request.getPerpetualTaskId());

    responseObserver.onNext(DeleteRemotePerpetualTaskResponse.newBuilder().setSuccess(success).build());
    responseObserver.onCompleted();
  }

  @Override
  public void resetRemotePerpetualTask(
      ResetRemotePerpetualTaskRequest request, StreamObserver<ResetRemotePerpetualTaskResponse> responseObserver) {
    final boolean success = perpetualTaskService.resetTask(request.getAccountId(), request.getPerpetualTaskId(), null);

    responseObserver.onNext(ResetRemotePerpetualTaskResponse.newBuilder().setSuccess(success).build());
    responseObserver.onCompleted();
  }
}

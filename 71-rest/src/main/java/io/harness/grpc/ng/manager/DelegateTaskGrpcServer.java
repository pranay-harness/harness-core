package io.harness.grpc.ng.manager;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.waiter.OrchestrationNotifyEventListener.ORCHESTRATION;

import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import com.google.protobuf.util.Durations;

import io.grpc.stub.StreamObserver;
import io.harness.beans.DelegateTask;
import io.harness.delegate.AbortTaskResponse;
import io.harness.delegate.NgDelegateTaskServiceGrpc;
import io.harness.delegate.SendTaskAsyncRequest;
import io.harness.delegate.SendTaskAsyncResponse;
import io.harness.delegate.SendTaskRequest;
import io.harness.delegate.SendTaskResponse;
import io.harness.delegate.TaskDetails;
import io.harness.delegate.TaskExecutionStage;
import io.harness.delegate.TaskId;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.task.TaskParameters;
import io.harness.serializer.KryoSerializer;
import io.harness.waiter.WaitNotifyEngine;
import software.wings.service.intfc.DelegateService;

import java.util.Map;

public class DelegateTaskGrpcServer extends NgDelegateTaskServiceGrpc.NgDelegateTaskServiceImplBase {
  private DelegateService delegateService;
  private KryoSerializer kryoSerializer;
  private WaitNotifyEngine waitNotifyEngine;

  @Inject
  public DelegateTaskGrpcServer(
      DelegateService delegateService, KryoSerializer kryoSerializer, WaitNotifyEngine waitNotifyEngine) {
    this.delegateService = delegateService;
    this.kryoSerializer = kryoSerializer;
    this.waitNotifyEngine = waitNotifyEngine;
  }

  @Override
  public void sendTask(SendTaskRequest request, StreamObserver<SendTaskResponse> responseObserver) {
    try {
      DelegateTask task = extractDelegateTask(request);
      ResponseData responseData = delegateService.executeTask(task);
      responseObserver.onNext(SendTaskResponse.newBuilder()
                                  .setTaskId(TaskId.newBuilder().setId(task.getUuid()).build())
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
          SendTaskAsyncResponse.newBuilder().setTaskId(TaskId.newBuilder().setId(taskId).build()).build());
      responseObserver.onCompleted();
    } catch (Exception ex) {
      responseObserver.onError(ex);
    }
  }

  @Override
  public void abortTask(io.harness.delegate.AbortTaskRequest request,
      io.grpc.stub.StreamObserver<io.harness.delegate.AbortTaskResponse> responseObserver) {
    try {
      AbortTaskResponse abortTaskResponse =
          AbortTaskResponse.newBuilder().setCanceledAtStage(TaskExecutionStage.EXECUTING).build();
      responseObserver.onNext(abortTaskResponse);
      responseObserver.onCompleted();
    } catch (Exception ex) {
      responseObserver.onError(ex);
    }
  }

  private DelegateTask extractDelegateTask(SendTaskRequest request) {
    TaskDetails taskDetails = request.getDetails();
    String accountId = request.getAccountId().getId();
    Map<String, String> setupAbstractions = request.getSetupAbstractions().getValuesMap();
    return extractDelegateTask(accountId, setupAbstractions, taskDetails);
  }

  private DelegateTask extractDelegateTask(SendTaskAsyncRequest request) {
    TaskDetails taskDetails = request.getDetails();
    String accountId = request.getAccountId().getId();
    Map<String, String> setupAbstractions = request.getSetupAbstractions().getValuesMap();
    return extractDelegateTask(accountId, setupAbstractions, taskDetails);
  }

  private DelegateTask extractDelegateTask(
      String accountId, Map<String, String> setupAbstractions, TaskDetails taskDetails) {
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
}

package io.harness.ng.core.remote.server.grpc;

import static org.apache.commons.collections4.MapUtils.emptyIfNull;

import com.google.inject.Inject;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.Duration;
import com.google.protobuf.Message;

import io.grpc.stub.StreamObserver;
import io.harness.delegate.NgDelegateTaskResponseServiceGrpc;
import io.harness.delegate.NgTaskDetails;
import io.harness.delegate.NgTaskSetupAbstractions;
import io.harness.delegate.NgTaskType;
import io.harness.delegate.SendTaskResultRequest;
import io.harness.delegate.SendTaskResultResponse;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.task.TaskParameters;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.remote.server.grpc.perpetualtask.RemotePerpetualTaskServiceClientManager;
import io.harness.perpetualtask.ObtainPerpetualTaskExecutionParamsRequest;
import io.harness.perpetualtask.ObtainPerpetualTaskExecutionParamsResponse;
import io.harness.perpetualtask.ObtainPerpetualTaskValidationDetailsRequest;
import io.harness.perpetualtask.ObtainPerpetualTaskValidationDetailsResponse;
import io.harness.perpetualtask.remote.ValidationTaskDetails;
import io.harness.serializer.KryoSerializer;
import io.harness.waiter.WaitNotifyEngine;

public class NgDelegateTaskResponseGrpcServer
    extends NgDelegateTaskResponseServiceGrpc.NgDelegateTaskResponseServiceImplBase {
  private final WaitNotifyEngine waitNotifyEngine;
  private final KryoSerializer kryoSerializer;
  private final RemotePerpetualTaskServiceClientManager pTaskServiceClientManager;

  @Inject
  public NgDelegateTaskResponseGrpcServer(WaitNotifyEngine waitNotifyEngine, KryoSerializer kryoSerializer,
      RemotePerpetualTaskServiceClientManager pTaskServiceClientManager) {
    this.waitNotifyEngine = waitNotifyEngine;
    this.kryoSerializer = kryoSerializer;
    this.pTaskServiceClientManager = pTaskServiceClientManager;
  }

  @Override
  public void sendTaskResult(SendTaskResultRequest request, StreamObserver<SendTaskResultResponse> responseObserver) {
    DelegateResponseData responseData = null;
    if (!request.getResponseData().isEmpty()) {
      responseData = (DelegateResponseData) kryoSerializer.asInflatedObject(request.getResponseData().toByteArray());
    }
    waitNotifyEngine.doneWith(request.getTaskId(), responseData);
    SendTaskResultResponse sendTaskResultResponse =
        SendTaskResultResponse.newBuilder().setAcknowledgement(true).build();
    responseObserver.onNext(sendTaskResultResponse);
    responseObserver.onCompleted();
  }

  @Override
  public void obtainPerpetualTaskValidationDetails(ObtainPerpetualTaskValidationDetailsRequest request,
      StreamObserver<ObtainPerpetualTaskValidationDetailsResponse> responseObserver) {
    final ValidationTaskDetails validationTask = pTaskServiceClientManager.getValidationTask(
        request.getTaskType(), request.getContext(), request.getAccountId());
    final ObtainPerpetualTaskValidationDetailsResponse response =
        ObtainPerpetualTaskValidationDetailsResponse.newBuilder()
            .setSetupAbstractions(NgTaskSetupAbstractions.newBuilder()
                                      .putAllValues(emptyIfNull(validationTask.getSetupAbstractions()))
                                      .build())
            .setDetails(buildTaskDetails(validationTask.getTaskData()))
            .build();
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void obtainPerpetualTaskExecutionParams(ObtainPerpetualTaskExecutionParamsRequest request,
      StreamObserver<ObtainPerpetualTaskExecutionParamsResponse> responseObserver) {
    final Message taskParams = pTaskServiceClientManager.getTaskParams(request.getTaskType(), request.getContext());
    responseObserver.onNext(
        ObtainPerpetualTaskExecutionParamsResponse.newBuilder().setCustomizedParams(Any.pack(taskParams)).build());
    responseObserver.onCompleted();
  }

  private NgTaskDetails buildTaskDetails(TaskData taskData) {
    return NgTaskDetails.newBuilder()
        .setType(NgTaskType.newBuilder().setType(taskData.getTaskType()).build())
        .putAllExpressions(emptyIfNull(taskData.getExpressions()))
        .setExecutionTimeout(Duration.newBuilder().setSeconds(taskData.getTimeout() * 1000).build())
        .setExpressionFunctorToken(taskData.getExpressionFunctorToken())
        .setKryoParameters(ByteString.copyFrom(kryoSerializer.asDeflatedBytes(getTaskParameter(taskData))))
        .build();
  }
  private TaskParameters getTaskParameter(TaskData taskData) {
    Object[] parameters = taskData.getParameters();
    if (parameters.length == 1 && parameters[0] instanceof TaskParameters) {
      return (TaskParameters) parameters[0];
    }
    throw new InvalidRequestException("Only Supported for task using task parameters");
  }
}

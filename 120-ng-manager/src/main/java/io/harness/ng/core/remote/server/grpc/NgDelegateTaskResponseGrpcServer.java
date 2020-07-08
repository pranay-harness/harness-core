package io.harness.ng.core.remote.server.grpc;

import com.google.inject.Inject;

import io.grpc.stub.StreamObserver;
import io.harness.delegate.NgDelegateTaskResponseServiceGrpc;
import io.harness.delegate.SendTaskResultRequest;
import io.harness.delegate.SendTaskResultResponse;
import io.harness.delegate.beans.ResponseData;
import io.harness.serializer.KryoSerializer;
import io.harness.waiter.WaitNotifyEngine;

public class NgDelegateTaskResponseGrpcServer
    extends NgDelegateTaskResponseServiceGrpc.NgDelegateTaskResponseServiceImplBase {
  private WaitNotifyEngine waitNotifyEngine;
  private KryoSerializer kryoSerializer;

  @Inject
  public NgDelegateTaskResponseGrpcServer(WaitNotifyEngine waitNotifyEngine, KryoSerializer kryoSerializer) {
    this.waitNotifyEngine = waitNotifyEngine;
    this.kryoSerializer = kryoSerializer;
  }

  @Override
  public void sendTaskResult(SendTaskResultRequest request, StreamObserver<SendTaskResultResponse> responseObserver) {
    ResponseData responseData = null;
    if (!request.getResponseData().isEmpty()) {
      responseData = (ResponseData) kryoSerializer.asInflatedObject(request.getResponseData().toByteArray());
    }
    waitNotifyEngine.doneWith(request.getTaskId().getId(), responseData);
    SendTaskResultResponse sendTaskResultResponse =
        SendTaskResultResponse.newBuilder().setAcknowledgement(true).build();
    responseObserver.onNext(sendTaskResultResponse);
    responseObserver.onCompleted();
  }
}

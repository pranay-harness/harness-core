package io.harness.ng.core.remote.server.grpc;

import io.grpc.stub.StreamObserver;
import io.harness.delegate.NgDelegateTaskResponseServiceGrpc;
import io.harness.delegate.SendTaskResultRequest;
import io.harness.delegate.SendTaskResultResponse;

public class NgDelegateTaskResponseGrpcServer
    extends NgDelegateTaskResponseServiceGrpc.NgDelegateTaskResponseServiceImplBase {
  @Override
  public void sendTaskResult(SendTaskResultRequest request, StreamObserver<SendTaskResultResponse> responseObserver) {
    SendTaskResultResponse sendTaskResultResponse =
        SendTaskResultResponse.newBuilder().setAcknowledgement(true).build();
    responseObserver.onNext(sendTaskResultResponse);
    responseObserver.onCompleted();
  }
}

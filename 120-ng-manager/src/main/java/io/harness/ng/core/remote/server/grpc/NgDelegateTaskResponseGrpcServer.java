package io.harness.ng.core.remote.server.grpc;

import io.harness.delegate.NgDelegateTaskResponseServiceGrpc;
import io.harness.delegate.SendTaskResultResponse;

public class NgDelegateTaskResponseGrpcServer
    extends NgDelegateTaskResponseServiceGrpc.NgDelegateTaskResponseServiceImplBase {
  @Override
  public void sendTaskResult(io.harness.delegate.SendTaskResultRequest request,
      io.grpc.stub.StreamObserver<io.harness.delegate.SendTaskResultResponse> responseObserver) {
    SendTaskResultResponse sendTaskResultResponse =
        SendTaskResultResponse.newBuilder().setTaskId(request.getTaskId()).build();
    responseObserver.onNext(sendTaskResultResponse);
    responseObserver.onCompleted();
  }
}

package io.harness.grpc.ng;

import io.harness.delegate.AbortTaskResponse;
import io.harness.delegate.NgDelegateTaskServiceGrpc;
import io.harness.delegate.SendTaskAsyncResponse;
import io.harness.delegate.SendTaskResponse;
import io.harness.delegate.TaskExecutionStage;

public class DelegateTaskGrpcServer extends NgDelegateTaskServiceGrpc.NgDelegateTaskServiceImplBase {
  @Override
  public void sendTask(io.harness.delegate.SendTaskRequest request,
      io.grpc.stub.StreamObserver<io.harness.delegate.SendTaskResponse> responseObserver) {
    SendTaskResponse sendTaskResponse = SendTaskResponse.newBuilder().setTaskId(request.getTaskId()).build();
    responseObserver.onNext(sendTaskResponse);
    responseObserver.onCompleted();
  }

  @Override
  public void sendTaskAsync(io.harness.delegate.SendTaskAsyncRequest request,
      io.grpc.stub.StreamObserver<io.harness.delegate.SendTaskAsyncResponse> responseObserver) {
    SendTaskAsyncResponse sendTaskAsyncResponse =
        SendTaskAsyncResponse.newBuilder().setTaskId(request.getTaskId()).build();
    responseObserver.onNext(sendTaskAsyncResponse);
    responseObserver.onCompleted();
  }

  @Override
  public void abortTask(io.harness.delegate.AbortTaskRequest request,
      io.grpc.stub.StreamObserver<io.harness.delegate.AbortTaskResponse> responseObserver) {
    AbortTaskResponse abortTaskResponse =
        AbortTaskResponse.newBuilder().setCanceledAtStage(TaskExecutionStage.EXECUTING).build();
    responseObserver.onNext(abortTaskResponse);
    responseObserver.onCompleted();
  }
}

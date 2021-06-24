package io.harness.grpc;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DelegateTask;
import io.harness.delegate.DelegateClassicTaskRequest;
import io.harness.delegate.DelegateTaskGrpc;
import io.harness.delegate.ExecuteTaskResponse;
import io.harness.delegate.QueueTaskResponse;
import io.harness.serializer.KryoSerializer;

import software.wings.service.intfc.DelegateService;

import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.DEL)

public class DelegateServiceClassicGrpcImpl extends DelegateTaskGrpc.DelegateTaskImplBase {
  private KryoSerializer kryoSerializer;
  private DelegateService delegateService;

  @Override
  public void queueTask(DelegateClassicTaskRequest request, StreamObserver<QueueTaskResponse> responseObserver) {
    try {
      DelegateTask task = (DelegateTask) kryoSerializer.asInflatedObject(request.getDelegateTaskKryo().toByteArray());

      delegateService.queueTask(task);

      responseObserver.onNext(QueueTaskResponse.newBuilder().setUuid(task.getUuid()).build());
      responseObserver.onCompleted();

    } catch (Exception ex) {
      log.error("Unexpected error occurred while processing queue task request.", ex);
      responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(ex.getMessage()).asRuntimeException());
    }
  }

  @Override
  public void executeTask(DelegateClassicTaskRequest request, StreamObserver<ExecuteTaskResponse> responseObserver) {
    try {
      DelegateTask task = (DelegateTask) kryoSerializer.asInflatedObject(request.getDelegateTaskKryo().toByteArray());
      ExecuteTaskResponse executeTaskResponse = delegateService.executeTask(task);
      responseObserver.onNext(
          ExecuteTaskResponse.newBuilder()
              .setDelegateTaskResponseKryo(ByteString.copyFrom(kryoSerializer.asDeflatedBytes(executeTaskResponse)))
              .build());
      responseObserver.onCompleted();
    } catch (Exception ex) {
      log.error("Unexpected error occurred while processing execute task request.", ex);
      responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(ex.getMessage()).asRuntimeException());
    }
  }
}
package io.harness.engine.interrupts;

import io.harness.pms.contracts.service.InterruptProtoServiceGrpc.InterruptProtoServiceImplBase;
import io.harness.pms.contracts.service.InterruptRequest;
import io.harness.pms.contracts.service.InterruptResponse;
import io.harness.tasks.BinaryResponseData;
import io.harness.waiter.WaitNotifyEngine;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class InterruptGrpcService extends InterruptProtoServiceImplBase {
  @Inject private WaitNotifyEngine waitNotifyEngine;

  @Override
  public void handleAbort(InterruptRequest request, StreamObserver<InterruptResponse> responseObserver) {
    log.info("InterruptGrpcService#handleAbort reached.");
    // adding a dummy response data object
    waitNotifyEngine.doneWith(request.getNotifyId(), BinaryResponseData.builder().build());
    responseObserver.onNext(InterruptResponse.newBuilder().build());
    responseObserver.onCompleted();
  }
}

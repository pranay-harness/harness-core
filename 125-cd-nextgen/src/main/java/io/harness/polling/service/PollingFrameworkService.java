package io.harness.polling.service;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.polling.contracts.PollingItem;
import io.harness.polling.contracts.service.PollingDocument;
import io.harness.polling.contracts.service.PollingFrameworkServiceGrpc;
import io.harness.polling.contracts.service.PollingResponse;

import io.grpc.stub.StreamObserver;

@OwnedBy(HarnessTeam.CDC)
public class PollingFrameworkService extends PollingFrameworkServiceGrpc.PollingFrameworkServiceImplBase {
  @Override
  public void subscribe(PollingItem request, StreamObserver<PollingDocument> responseObserver) {
    responseObserver.onNext(PollingDocument.newBuilder().build());
    responseObserver.onCompleted();
  }

  @Override
  public void unsubscribe(PollingItem request, StreamObserver<PollingResponse> responseObserver) {
    responseObserver.onNext(PollingResponse.newBuilder().build());
    responseObserver.onCompleted();
  }
}

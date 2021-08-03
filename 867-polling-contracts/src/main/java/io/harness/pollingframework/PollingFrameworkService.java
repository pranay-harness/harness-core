package io.harness.pollingframework;

import io.harness.polling.contracts.PollingItem;
import io.harness.polling.contracts.service.PollingDocument;
import io.harness.polling.contracts.service.PollingFrameworkServiceGrpc;

import io.grpc.stub.StreamObserver;

public class PollingFrameworkService extends PollingFrameworkServiceGrpc.PollingFrameworkServiceImplBase {
  @Override
  public void subscribe(PollingItem request, StreamObserver<PollingDocument> responseObserver) {
    responseObserver.onNext(PollingDocument.newBuilder().build());
    responseObserver.onCompleted();
  }
}

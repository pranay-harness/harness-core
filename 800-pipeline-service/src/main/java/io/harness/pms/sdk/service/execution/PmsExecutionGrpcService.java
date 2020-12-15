package io.harness.pms.sdk.service.execution;

import io.harness.pms.contracts.service.ExecutionSummaryRequest;
import io.harness.pms.contracts.service.ExecutionSummaryResponse;
import io.harness.pms.contracts.service.PmsExecutionServiceGrpc.PmsExecutionServiceImplBase;

import io.grpc.stub.StreamObserver;

public class PmsExecutionGrpcService extends PmsExecutionServiceImplBase {
  @Override
  public void updateExecutionSummary(
      ExecutionSummaryRequest request, StreamObserver<ExecutionSummaryResponse> responseObserver) {
    responseObserver.onNext(ExecutionSummaryResponse.newBuilder().build());
    responseObserver.onCompleted();
  }
}

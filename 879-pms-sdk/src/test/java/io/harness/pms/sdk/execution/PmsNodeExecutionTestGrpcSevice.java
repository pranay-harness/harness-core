package io.harness.pms.sdk.execution;

import io.harness.pms.contracts.plan.EventErrorRequest;
import io.harness.pms.contracts.plan.EventErrorResponse;
import io.harness.pms.contracts.plan.FacilitatorResponseRequest;
import io.harness.pms.contracts.plan.FacilitatorResponseResponse;
import io.harness.pms.contracts.plan.NodeExecutionProtoServiceGrpc.NodeExecutionProtoServiceImplBase;

import io.grpc.stub.StreamObserver;

public class PmsNodeExecutionTestGrpcSevice extends NodeExecutionProtoServiceImplBase {
  @Override
  public void handleFacilitatorResponse(
      FacilitatorResponseRequest request, StreamObserver<FacilitatorResponseResponse> responseObserver) {
    responseObserver.onNext(FacilitatorResponseResponse.newBuilder().build());
    responseObserver.onCompleted();
  }

  @Override
  public void handleEventError(EventErrorRequest request, StreamObserver<EventErrorResponse> responseObserver) {
    responseObserver.onNext(EventErrorResponse.newBuilder().build());
    responseObserver.onCompleted();
  }
}

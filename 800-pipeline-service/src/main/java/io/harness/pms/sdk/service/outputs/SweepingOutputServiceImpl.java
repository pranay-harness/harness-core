package io.harness.pms.sdk.service.outputs;

import io.harness.engine.outputs.ExecutionSweepingOutputServiceImpl;
import io.harness.pms.sdk.core.data.SweepingOutput;
import io.harness.pms.serializer.json.JsonOrchestrationUtils;
import io.harness.pms.service.SweepingOutputConsumeBlobRequest;
import io.harness.pms.service.SweepingOutputConsumeBlobResponse;
import io.harness.pms.service.SweepingOutputResolveBlobRequest;
import io.harness.pms.service.SweepingOutputResolveBlobResponse;
import io.harness.pms.service.SweepingOutputServiceGrpc.SweepingOutputServiceImplBase;

import com.google.inject.Inject;
import io.grpc.stub.StreamObserver;

public class SweepingOutputServiceImpl extends SweepingOutputServiceImplBase {
  private final ExecutionSweepingOutputServiceImpl executionSweepingOutputService;

  @Inject
  public SweepingOutputServiceImpl(ExecutionSweepingOutputServiceImpl executionSweepingOutputService) {
    this.executionSweepingOutputService = executionSweepingOutputService;
  }

  @Override
  public void resolve(
      SweepingOutputResolveBlobRequest request, StreamObserver<SweepingOutputResolveBlobResponse> responseObserver) {
    SweepingOutput resolve = executionSweepingOutputService.resolve(request.getAmbiance(), request.getRefObject());

    responseObserver.onNext(SweepingOutputResolveBlobResponse.newBuilder().setStepTransput(resolve.toJson()).build());
    responseObserver.onCompleted();
  }

  @Override
  public void consume(
      SweepingOutputConsumeBlobRequest request, StreamObserver<SweepingOutputConsumeBlobResponse> responseObserver) {
    String response = executionSweepingOutputService.consume(request.getAmbiance(), request.getName(),
        JsonOrchestrationUtils.asObject(request.getValue(), executionSweepingOutputService.getStepTransputClass()),
        request.getGroupName());
    responseObserver.onNext(SweepingOutputConsumeBlobResponse.newBuilder().setResponse(response).build());
    responseObserver.onCompleted();
  }
}

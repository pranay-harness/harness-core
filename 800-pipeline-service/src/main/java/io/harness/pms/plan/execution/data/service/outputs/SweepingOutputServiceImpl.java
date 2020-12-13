package io.harness.pms.plan.execution.data.service.outputs;

import io.harness.engine.pms.data.PmsSweepingOutputService;
import io.harness.pms.service.SweepingOutputConsumeBlobRequest;
import io.harness.pms.service.SweepingOutputConsumeBlobResponse;
import io.harness.pms.service.SweepingOutputResolveBlobRequest;
import io.harness.pms.service.SweepingOutputResolveBlobResponse;
import io.harness.pms.service.SweepingOutputServiceGrpc.SweepingOutputServiceImplBase;

import com.google.inject.Inject;
import io.grpc.stub.StreamObserver;

// TODO (prashant) : Right now this is acting just as a wrapper for #PmsSweepingOutputService. It also mark the module
// for understanding. Merge the two later
public class SweepingOutputServiceImpl extends SweepingOutputServiceImplBase {
  private final PmsSweepingOutputService pmsSweepingOutputService;

  @Inject
  public SweepingOutputServiceImpl(PmsSweepingOutputService pmsSweepingOutputService) {
    this.pmsSweepingOutputService = pmsSweepingOutputService;
  }

  @Override
  public void resolve(
      SweepingOutputResolveBlobRequest request, StreamObserver<SweepingOutputResolveBlobResponse> responseObserver) {
    String resolve = pmsSweepingOutputService.resolve(request.getAmbiance(), request.getRefObject());
    responseObserver.onNext(SweepingOutputResolveBlobResponse.newBuilder().setStepTransput(resolve).build());
    responseObserver.onCompleted();
  }

  @Override
  public void consume(
      SweepingOutputConsumeBlobRequest request, StreamObserver<SweepingOutputConsumeBlobResponse> responseObserver) {
    String response = pmsSweepingOutputService.consume(
        request.getAmbiance(), request.getName(), request.getValue(), request.getGroupName());
    responseObserver.onNext(SweepingOutputConsumeBlobResponse.newBuilder().setResponse(response).build());
    responseObserver.onCompleted();
  }
}

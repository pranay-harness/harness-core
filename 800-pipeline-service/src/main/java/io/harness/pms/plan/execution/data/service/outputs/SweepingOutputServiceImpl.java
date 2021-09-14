/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.plan.execution.data.service.outputs;

import io.harness.engine.pms.data.PmsSweepingOutputService;
import io.harness.engine.pms.data.RawOptionalSweepingOutput;
import io.harness.pms.contracts.service.OptionalSweepingOutputResolveBlobResponse;
import io.harness.pms.contracts.service.OptionalSweepingOutputResolveBlobResponse.Builder;
import io.harness.pms.contracts.service.SweepingOutputConsumeBlobRequest;
import io.harness.pms.contracts.service.SweepingOutputConsumeBlobResponse;
import io.harness.pms.contracts.service.SweepingOutputResolveBlobRequest;
import io.harness.pms.contracts.service.SweepingOutputResolveBlobResponse;
import io.harness.pms.contracts.service.SweepingOutputServiceGrpc.SweepingOutputServiceImplBase;

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
  public void resolveOptional(SweepingOutputResolveBlobRequest request,
      StreamObserver<OptionalSweepingOutputResolveBlobResponse> responseObserver) {
    RawOptionalSweepingOutput resolve =
        pmsSweepingOutputService.resolveOptional(request.getAmbiance(), request.getRefObject());

    Builder builder = OptionalSweepingOutputResolveBlobResponse.newBuilder().setFound(resolve.isFound());
    if (resolve.isFound()) {
      builder.setStepTransput(resolve.getOutput());
    }

    responseObserver.onNext(builder.build());
    responseObserver.onCompleted();
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

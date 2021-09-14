/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.pms.sdk.core.resolver.outputs;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.refobjects.RefObject;
import io.harness.pms.contracts.service.OptionalSweepingOutputResolveBlobResponse;
import io.harness.pms.contracts.service.SweepingOutputConsumeBlobRequest;
import io.harness.pms.contracts.service.SweepingOutputConsumeBlobResponse;
import io.harness.pms.contracts.service.SweepingOutputResolveBlobRequest;
import io.harness.pms.contracts.service.SweepingOutputResolveBlobResponse;
import io.harness.pms.contracts.service.SweepingOutputServiceGrpc.SweepingOutputServiceBlockingStub;
import io.harness.pms.sdk.core.data.ExecutionSweepingOutput;
import io.harness.pms.sdk.core.data.OptionalSweepingOutput;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;
import io.harness.pms.utils.PmsGrpcClientUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@OwnedBy(HarnessTeam.PIPELINE)
@Singleton
public class ExecutionSweepingGrpcOutputService implements ExecutionSweepingOutputService {
  private final SweepingOutputServiceBlockingStub sweepingOutputServiceBlockingStub;

  @Inject
  public ExecutionSweepingGrpcOutputService(SweepingOutputServiceBlockingStub sweepingOutputServiceBlockingStub) {
    this.sweepingOutputServiceBlockingStub = sweepingOutputServiceBlockingStub;
  }

  @Override
  public ExecutionSweepingOutput resolve(Ambiance ambiance, RefObject refObject) {
    SweepingOutputResolveBlobResponse resolve =
        PmsGrpcClientUtils.retryAndProcessException(sweepingOutputServiceBlockingStub::resolve,
            SweepingOutputResolveBlobRequest.newBuilder().setAmbiance(ambiance).setRefObject(refObject).build());
    return RecastOrchestrationUtils.fromJson(resolve.getStepTransput(), ExecutionSweepingOutput.class);
  }

  @Override
  public String consume(Ambiance ambiance, String name, ExecutionSweepingOutput value, String groupName) {
    SweepingOutputConsumeBlobRequest.Builder builder =
        SweepingOutputConsumeBlobRequest.newBuilder().setAmbiance(ambiance).setName(name).setValue(
            RecastOrchestrationUtils.toJson(value));
    if (EmptyPredicate.isNotEmpty(groupName)) {
      builder.setGroupName(groupName);
    }

    SweepingOutputConsumeBlobResponse sweepingOutputConsumeBlobResponse =
        PmsGrpcClientUtils.retryAndProcessException(sweepingOutputServiceBlockingStub::consume, builder.build());
    return sweepingOutputConsumeBlobResponse.getResponse();
  }

  @Override
  public OptionalSweepingOutput resolveOptional(Ambiance ambiance, RefObject refObject) {
    OptionalSweepingOutputResolveBlobResponse resolve =
        PmsGrpcClientUtils.retryAndProcessException(sweepingOutputServiceBlockingStub::resolveOptional,
            SweepingOutputResolveBlobRequest.newBuilder().setAmbiance(ambiance).setRefObject(refObject).build());
    return OptionalSweepingOutput.builder()
        .output(RecastOrchestrationUtils.fromJson(resolve.getStepTransput(), ExecutionSweepingOutput.class))
        .found(resolve.getFound())
        .build();
  }
}

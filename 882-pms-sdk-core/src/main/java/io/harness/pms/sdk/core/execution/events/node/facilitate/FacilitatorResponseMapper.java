/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.pms.sdk.core.execution.events.node.facilitate;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.facilitators.FacilitatorResponseProto;
import io.harness.pms.sdk.core.steps.io.PassThroughData;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;
import io.harness.serializer.ProtoUtils;

import lombok.experimental.UtilityClass;

@OwnedBy(CDC)
@UtilityClass
public class FacilitatorResponseMapper {
  public FacilitatorResponse fromFacilitatorResponseProto(FacilitatorResponseProto proto) {
    return FacilitatorResponse.builder()
        .initialWait(ProtoUtils.durationToJavaDuration(proto.getInitialWait()))
        .executionMode(proto.getExecutionMode())
        .passThroughData(RecastOrchestrationUtils.fromJson(proto.getPassThroughData(), PassThroughData.class))
        .build();
  }

  public FacilitatorResponseProto toFacilitatorResponseProto(FacilitatorResponse facilitatorResponse) {
    FacilitatorResponseProto.Builder builder =
        FacilitatorResponseProto.newBuilder().setExecutionMode(facilitatorResponse.getExecutionMode());
    if (facilitatorResponse.getInitialWait() != null) {
      builder.setInitialWait(ProtoUtils.javaDurationToDuration(facilitatorResponse.getInitialWait()));
    }
    if (facilitatorResponse.getPassThroughData() != null) {
      builder.setPassThroughData(RecastOrchestrationUtils.toJson(facilitatorResponse.getPassThroughData()));
    }
    builder.setIsSuccessful(true);
    return builder.build();
  }
}

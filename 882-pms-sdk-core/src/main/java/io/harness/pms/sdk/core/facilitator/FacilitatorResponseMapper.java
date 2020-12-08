package io.harness.pms.sdk.core.facilitator;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.facilitators.FacilitatorResponseProto;
import io.harness.pms.serializer.persistence.DocumentOrchestrationUtils;
import io.harness.serializer.ProtoUtils;

import lombok.experimental.UtilityClass;

@OwnedBy(CDC)
@UtilityClass
public class FacilitatorResponseMapper {
  public FacilitatorResponse fromFacilitatorResponseProto(FacilitatorResponseProto proto) {
    return FacilitatorResponse.builder()
        .initialWait(ProtoUtils.durationToJavaDuration(proto.getInitialWait()))
        .executionMode(proto.getExecutionMode())
        .passThroughData(DocumentOrchestrationUtils.convertFromDocumentJson(proto.getPassThroughData()))
        .build();
  }

  public FacilitatorResponseProto toFacilitatorResponseProto(FacilitatorResponse facilitatorResponse) {
    FacilitatorResponseProto.Builder builder =
        FacilitatorResponseProto.newBuilder().setExecutionMode(facilitatorResponse.getExecutionMode());
    if (facilitatorResponse.getInitialWait() != null) {
      builder.setInitialWait(ProtoUtils.javaDurationToDuration(facilitatorResponse.getInitialWait()));
    }
    if (facilitatorResponse.getPassThroughData() != null) {
      builder.setPassThroughData(
          DocumentOrchestrationUtils.convertToDocumentJson(facilitatorResponse.getPassThroughData()));
    }
    return builder.build();
  }
}

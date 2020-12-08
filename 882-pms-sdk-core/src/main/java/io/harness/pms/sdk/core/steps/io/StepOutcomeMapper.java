package io.harness.pms.sdk.core.steps.io;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.sdk.core.steps.io.StepResponse.StepOutcome;
import io.harness.pms.serializer.persistence.DocumentOrchestrationUtils;
import io.harness.pms.steps.io.StepOutcomeProto;

import lombok.experimental.UtilityClass;

@OwnedBy(CDC)
@UtilityClass
public class StepOutcomeMapper {
  public StepOutcome fromStepOutcomeProto(StepOutcomeProto proto) {
    return StepOutcome.builder()
        .group(proto.getGroup())
        .name(proto.getName())
        .outcome(DocumentOrchestrationUtils.convertFromDocumentJson(proto.getOutcome()))
        .build();
  }

  public StepOutcomeProto toStepOutcomeProto(StepOutcome stepOutcome) {
    StepOutcomeProto.Builder builder = StepOutcomeProto.newBuilder().setName(stepOutcome.getName());
    if (stepOutcome.getGroup() != null) {
      builder.setGroup(stepOutcome.getGroup());
    }
    if (stepOutcome.getOutcome() != null) {
      builder.setOutcome(DocumentOrchestrationUtils.convertToDocumentJson(stepOutcome.getOutcome()));
    }
    return builder.build();
  }
}

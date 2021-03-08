package io.harness.pms.sdk.core.steps.io;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.steps.io.StepOutcomeProto;
import io.harness.pms.sdk.core.data.Outcome;
import io.harness.pms.sdk.core.steps.io.StepResponse.StepOutcome;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;

import lombok.experimental.UtilityClass;

@OwnedBy(CDC)
@UtilityClass
public class StepOutcomeMapper {
  public static String GRAPH_KEY = "_graphOutcome_";

  public StepOutcome fromStepOutcomeProto(StepOutcomeProto proto) {
    return StepOutcome.builder()
        .group(proto.getGroup())
        .name(proto.getName())
        .outcome(RecastOrchestrationUtils.fromDocumentJson(proto.getOutcome(), Outcome.class))
        .build();
  }

  public StepOutcomeProto toStepOutcomeProto(StepOutcome stepOutcome) {
    StepOutcomeProto.Builder builder = StepOutcomeProto.newBuilder().setName(stepOutcome.getName());
    if (stepOutcome.getGroup() != null) {
      builder.setGroup(stepOutcome.getGroup());
    }
    if (stepOutcome.getOutcome() != null) {
      builder.setOutcome(RecastOrchestrationUtils.toDocumentJson(stepOutcome.getOutcome()));
    }
    return builder.build();
  }

  public StepOutcomeProto toGraphOutcomeProto(StepOutcome stepOutcome) {
    StepOutcomeProto.Builder builder = StepOutcomeProto.newBuilder().setName(stepOutcome.getName() + GRAPH_KEY);
    if (stepOutcome.getGroup() != null) {
      builder.setGroup(stepOutcome.getGroup());
    }
    if (stepOutcome.getOutcome() != null && stepOutcome.getOutcome().toViewJson() != null) {
      builder.setOutcome(stepOutcome.getOutcome().toViewJson());
    } else {
      return null;
    }
    return builder.build();
  }
}

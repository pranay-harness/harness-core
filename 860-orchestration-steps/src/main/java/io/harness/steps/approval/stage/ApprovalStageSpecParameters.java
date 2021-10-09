package io.harness.steps.approval.stage;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.plancreator.steps.common.SpecParameters;

import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(HarnessTeam.CDC)
@Value
@Builder
@TypeAlias("approvalStageSpecParameters")
@RecasterAlias("io.harness.steps.approval.stage.ApprovalStageSpecParameters")
public class ApprovalStageSpecParameters implements SpecParameters {
  String childNodeID;

  public static ApprovalStageSpecParameters getStepParameters(String childNodeID) {
    return ApprovalStageSpecParameters.builder().childNodeID(childNodeID).build();
  }
}

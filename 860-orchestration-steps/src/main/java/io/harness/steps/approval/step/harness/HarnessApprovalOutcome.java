package io.harness.steps.approval.step.harness;

import io.harness.pms.sdk.core.data.Outcome;
import io.harness.steps.approval.step.harness.beans.ApproverInput;
import io.harness.steps.approval.step.harness.beans.HarnessApprovalActivity;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@JsonTypeName("harnessApprovalOutcome")
@TypeAlias("harnessApprovalOutcome")
public class HarnessApprovalOutcome implements Outcome {
  List<HarnessApprovalActivity> approvalActivities;
  List<ApproverInput> approverInputs;

  @Override
  public String getType() {
    return "harnessApprovalOutcome";
  }
}

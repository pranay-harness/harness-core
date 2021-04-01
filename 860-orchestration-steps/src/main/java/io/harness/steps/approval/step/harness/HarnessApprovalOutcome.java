package io.harness.steps.approval.step.harness;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.sdk.core.data.Outcome;
import io.harness.steps.approval.step.harness.beans.HarnessApprovalActivity;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(CDC)
@Value
@Builder
@JsonTypeName("HarnessApprovalOutcome")
@TypeAlias("harnessApprovalOutcome")
public class HarnessApprovalOutcome implements Outcome {
  List<HarnessApprovalActivity> approvalActivities;
  Map<String, String> approverInputs;

  @Override
  public String getType() {
    return "harnessApprovalOutcome";
  }
}

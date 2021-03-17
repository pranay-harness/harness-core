package io.harness.steps.approval.harness;

import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.yaml.ParameterField;
import io.harness.steps.approval.harness.beans.ApproverInputInfo;
import io.harness.steps.approval.harness.beans.Approvers;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TypeAlias("harnessApprovalStepParameters")
public class HarnessApprovalStepParameters extends HarnessApprovalBaseStepInfo implements StepParameters {
  String name;
  String identifier;

  @Builder(builderMethodName = "infoBuilder")
  public HarnessApprovalStepParameters(ParameterField<String> approvalMessage,
      ParameterField<Boolean> includePipelineExecutionHistory, Approvers approvers,
      List<ApproverInputInfo> approverInputs, String name, String identifier) {
    super(approvalMessage, includePipelineExecutionHistory, approvers, approverInputs);
    this.name = name;
    this.identifier = identifier;
  }
}

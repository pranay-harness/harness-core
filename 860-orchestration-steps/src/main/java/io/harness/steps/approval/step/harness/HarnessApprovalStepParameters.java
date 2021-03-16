package io.harness.steps.approval.step.harness;

import io.harness.pms.yaml.ParameterField;
import io.harness.steps.approval.step.ApprovalStepParameters;
import io.harness.steps.approval.step.beans.ApprovalType;
import io.harness.steps.approval.step.harness.beans.ApproverInputInfo;
import io.harness.steps.approval.step.harness.beans.Approvers;

import java.util.List;
import javax.validation.constraints.NotNull;
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
public class HarnessApprovalStepParameters extends ApprovalStepParameters {
  @NotNull Approvers approvers;
  List<ApproverInputInfo> approverInputs;

  @Builder(builderMethodName = "infoBuilder")
  public HarnessApprovalStepParameters(String name, String identifier, ParameterField<String> timeout,
      ParameterField<String> approvalMessage, ParameterField<Boolean> includePipelineExecutionHistory,
      Approvers approvers, List<ApproverInputInfo> approverInputs) {
    super(name, identifier, timeout, approvalMessage, includePipelineExecutionHistory);
    this.approvers = approvers;
    this.approverInputs = approverInputs;
  }

  @Override
  public ApprovalType fetchType() {
    return ApprovalType.HARNESS_APPROVAL;
  }
}

package io.harness.steps.approval.step.harness;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.yaml.ParameterField;
import io.harness.steps.approval.step.ApprovalStepParameters;
import io.harness.steps.approval.step.beans.ApprovalType;
import io.harness.steps.approval.step.harness.beans.ApproverInputInfo;
import io.harness.steps.approval.step.harness.beans.Approvers;

import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(CDC)
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@TypeAlias("harnessApprovalStepParameters")
public class HarnessApprovalStepParameters extends ApprovalStepParameters {
  @NotNull ParameterField<String> approvalMessage;
  @NotNull ParameterField<Boolean> includePipelineExecutionHistory;

  @NotNull Approvers approvers;
  List<ApproverInputInfo> approverInputs;

  @Builder(builderMethodName = "infoBuilder")
  public HarnessApprovalStepParameters(String name, String identifier, ParameterField<String> timeout,
      ParameterField<String> approvalMessage, ParameterField<Boolean> includePipelineExecutionHistory,
      Approvers approvers, List<ApproverInputInfo> approverInputs) {
    super(name, identifier, timeout, ApprovalType.HARNESS_APPROVAL);
    this.approvalMessage = approvalMessage;
    this.includePipelineExecutionHistory = includePipelineExecutionHistory;
    this.approvers = approvers;
    this.approverInputs = approverInputs;
  }
}

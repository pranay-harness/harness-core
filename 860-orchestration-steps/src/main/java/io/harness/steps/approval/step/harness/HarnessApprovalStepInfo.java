package io.harness.steps.approval.step.harness;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.facilitator.OrchestrationFacilitatorType;
import io.harness.pms.sdk.core.steps.io.BaseStepParameterInfo;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.yaml.ParameterField;
import io.harness.steps.approval.step.ApprovalBaseStepInfo;
import io.harness.steps.approval.step.harness.beans.ApproverInputInfo;
import io.harness.steps.approval.step.harness.beans.Approvers;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(CDC)
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("HarnessApproval")
@TypeAlias("harnessApprovalStepInfo")
public class HarnessApprovalStepInfo extends ApprovalBaseStepInfo {
  @NotNull Approvers approvers;
  List<ApproverInputInfo> approverInputs;

  @Builder(builderMethodName = "infoBuilder")
  public HarnessApprovalStepInfo(String name, String identifier, ParameterField<String> approvalMessage,
      ParameterField<Boolean> includePipelineExecutionHistory, Approvers approvers,
      List<ApproverInputInfo> approverInputs) {
    super(name, identifier, approvalMessage, includePipelineExecutionHistory);
    this.approvers = approvers;
    this.approverInputs = approverInputs;
  }

  @Override
  public StepType getStepType() {
    return HarnessApprovalStep.STEP_TYPE;
  }

  @Override
  public String getFacilitatorType() {
    return OrchestrationFacilitatorType.ASYNC;
  }

  @Override
  public StepParameters getStepParametersWithRollbackInfo(BaseStepParameterInfo baseStepParameterInfo) {
    return HarnessApprovalStepParameters.infoBuilder()
        .name(getName())
        .identifier(getIdentifier())
        .timeout(baseStepParameterInfo.getTimeout())
        .approvalMessage(getApprovalMessage())
        .includePipelineExecutionHistory(getIncludePipelineExecutionHistory())
        .approvers(approvers)
        .approverInputs(approverInputs)
        .build();
  }

  @Override
  public boolean validateStageFailureStrategy() {
    return false;
  }
}

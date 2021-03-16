package io.harness.plancreator.steps.approval;

import io.harness.plancreator.steps.internal.PMSStepInfo;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.yaml.ParameterField;
import io.harness.steps.approval.HarnessApprovalStep;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
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
@JsonTypeName("HarnessApproval")
@TypeAlias("HarnessApprovalStepInfo")
public class HarnessApprovalStepInfo extends HarnessApprovalBaseStepInfo implements PMSStepInfo {
  @JsonIgnore String name;

  @JsonIgnore String identifier;

  @Builder(builderMethodName = "infoBuilder")
  public HarnessApprovalStepInfo(ParameterField<String> approvalMessage,
      ParameterField<Boolean> includePipelineExecutionHistory, Approvers approvers,
      List<ApproverInputInfo> approverInputInfo, String name, String identifier) {
    super(approvalMessage, includePipelineExecutionHistory, approvers, approverInputInfo);
    this.name = name;
    this.identifier = identifier;
  }

  @Override
  public StepType getStepType() {
    return HarnessApprovalStep.STEP_TYPE;
  }

  @Override
  public String getFacilitatorType() {
    return null;
  }
}

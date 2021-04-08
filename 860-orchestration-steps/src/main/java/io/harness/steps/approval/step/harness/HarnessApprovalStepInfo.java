package io.harness.steps.approval.step.harness;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.yaml.schema.beans.SupportedPossibleFieldTypes.string;

import io.harness.annotations.dev.OwnedBy;
import io.harness.common.SwaggerConstants;
import io.harness.plancreator.steps.StepElementConfig;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.facilitator.OrchestrationFacilitatorType;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.yaml.ParameterField;
import io.harness.steps.StepSpecTypeConstants;
import io.harness.steps.approval.step.ApprovalBaseStepInfo;
import io.harness.steps.approval.step.harness.beans.ApproverInputInfo;
import io.harness.steps.approval.step.harness.beans.Approvers;
import io.harness.yaml.YamlSchemaTypes;
import io.harness.yaml.core.timeout.TimeoutUtils;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
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
@JsonTypeName(StepSpecTypeConstants.HARNESS_APPROVAL)
@TypeAlias("harnessApprovalStepInfo")
public class HarnessApprovalStepInfo extends ApprovalBaseStepInfo {
  @NotNull @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> approvalMessage;

  @NotNull
  @YamlSchemaTypes(value = {string})
  @ApiModelProperty(dataType = SwaggerConstants.BOOLEAN_CLASSPATH)
  ParameterField<Boolean> includePipelineExecutionHistory;

  @NotNull Approvers approvers;
  List<ApproverInputInfo> approverInputs;

  @Builder(builderMethodName = "infoBuilder")
  public HarnessApprovalStepInfo(String name, String identifier, ParameterField<String> approvalMessage,
      ParameterField<Boolean> includePipelineExecutionHistory, Approvers approvers,
      List<ApproverInputInfo> approverInputs) {
    super(name, identifier);
    this.approvalMessage = approvalMessage;
    this.includePipelineExecutionHistory = includePipelineExecutionHistory;
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
  public StepParameters getStepParametersInfo(StepElementConfig stepElementConfig) {
    return HarnessApprovalStepParameters.infoBuilder()
        .name(getName())
        .identifier(getIdentifier())
        .timeout(ParameterField.createValueField(TimeoutUtils.getTimeoutString(stepElementConfig.getTimeout())))
        .approvalMessage(approvalMessage)
        .includePipelineExecutionHistory(includePipelineExecutionHistory)
        .approvers(approvers)
        .approverInputs(approverInputs)
        .build();
  }
}

package io.harness.steps.approval.step.harness;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.yaml.schema.beans.SupportedPossibleFieldTypes.string;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.ParameterField;
import io.harness.beans.SwaggerConstants;
import io.harness.plancreator.steps.common.SpecParameters;
import io.harness.plancreator.steps.internal.PMSStepInfo;
import io.harness.pms.contracts.steps.StepType;
import io.harness.steps.StepSpecTypeConstants;
import io.harness.steps.approval.ApprovalFacilitator;
import io.harness.steps.approval.step.harness.beans.ApproverInputInfo;
import io.harness.steps.approval.step.harness.beans.Approvers;
import io.harness.yaml.YamlSchemaTypes;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(CDC)
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonTypeName(StepSpecTypeConstants.HARNESS_APPROVAL)
@TypeAlias("harnessApprovalStepInfo")
public class HarnessApprovalStepInfo implements PMSStepInfo {
  @NotNull @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> approvalMessage;

  @NotNull
  @YamlSchemaTypes(value = {string})
  @ApiModelProperty(dataType = SwaggerConstants.BOOLEAN_CLASSPATH)
  ParameterField<Boolean> includePipelineExecutionHistory;

  @NotNull Approvers approvers;
  List<ApproverInputInfo> approverInputs;

  @Override
  public StepType getStepType() {
    return HarnessApprovalStep.STEP_TYPE;
  }

  @Override
  public String getFacilitatorType() {
    return ApprovalFacilitator.APPROVAL_FACILITATOR;
  }

  @Override
  public SpecParameters getSpecParameters() {
    return HarnessApprovalSpecParameters.builder()
        .approvalMessage(approvalMessage)
        .includePipelineExecutionHistory(includePipelineExecutionHistory)
        .approvers(approvers)
        .approverInputs(approverInputs)
        .build();
  }
}

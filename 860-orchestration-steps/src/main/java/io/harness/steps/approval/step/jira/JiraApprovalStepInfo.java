package io.harness.steps.approval.step.jira;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.common.SwaggerConstants;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.facilitator.OrchestrationFacilitatorType;
import io.harness.pms.sdk.core.steps.io.BaseStepParameterInfo;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.yaml.ParameterField;
import io.harness.steps.StepSpecTypeConstants;
import io.harness.steps.approval.step.ApprovalBaseStepInfo;
import io.harness.steps.approval.step.jira.beans.CriteriaSpecWrapper;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
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
@JsonTypeName(StepSpecTypeConstants.JIRA_APPROVAL)
@TypeAlias("jiraApprovalStepInfo")
public class JiraApprovalStepInfo extends ApprovalBaseStepInfo {
  @NotNull @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> connectorRef;
  @NotNull @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> issueKey;
  @NotNull CriteriaSpecWrapper approvalCriteria;
  @NotNull CriteriaSpecWrapper rejectionCriteria;

  @Builder(builderMethodName = "infoBuilder")
  public JiraApprovalStepInfo(String name, String identifier, ParameterField<String> connectorRef,
      ParameterField<String> issueKey, CriteriaSpecWrapper approvalCriteria, CriteriaSpecWrapper rejectionCriteria) {
    super(name, identifier);
    this.connectorRef = connectorRef;
    this.issueKey = issueKey;
    this.approvalCriteria = approvalCriteria;
    this.rejectionCriteria = rejectionCriteria;
  }

  @Override
  public StepType getStepType() {
    return JiraApprovalStep.STEP_TYPE;
  }

  @Override
  public String getFacilitatorType() {
    return OrchestrationFacilitatorType.ASYNC;
  }

  @Override
  public StepParameters getStepParametersWithRollbackInfo(BaseStepParameterInfo baseStepParameterInfo) {
    return JiraApprovalStepParameters.infoBuilder()
        .name(getName())
        .identifier(getIdentifier())
        .timeout(baseStepParameterInfo.getTimeout())
        .connectorRef(connectorRef)
        .issueKey(issueKey)
        .approvalCriteria(approvalCriteria)
        .rejectionCriteria(rejectionCriteria)
        .build();
  }

  @Override
  public boolean validateStageFailureStrategy() {
    return false;
  }
}

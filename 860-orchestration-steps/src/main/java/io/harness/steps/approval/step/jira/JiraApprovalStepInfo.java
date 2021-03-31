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
@JsonTypeName(StepSpecTypeConstants.JIRA_APPROVAL)
@TypeAlias("jiraApprovalStepInfo")
public class JiraApprovalStepInfo extends ApprovalBaseStepInfo {
  @NotNull @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> connectorRef;
  @NotNull @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> projectKey;
  @NotNull @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> issueKey;
  @NotNull CriteriaSpecWrapper approvalCriteria;
  @NotNull CriteriaSpecWrapper rejectionCriteria;

  @Builder(builderMethodName = "infoBuilder")
  public JiraApprovalStepInfo(String name, String identifier, ParameterField<String> approvalMessage,
      ParameterField<Boolean> includePipelineExecutionHistory, ParameterField<String> connectorRef,
      ParameterField<String> projectKey, ParameterField<String> issueKey, CriteriaSpecWrapper approvalCriteria,
      CriteriaSpecWrapper rejectionCriteria) {
    super(name, identifier, approvalMessage, includePipelineExecutionHistory);
    this.connectorRef = connectorRef;
    this.projectKey = projectKey;
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
        .approvalCriteria(approvalCriteria)
        .rejectionCriteria(rejectionCriteria)
        .issueKey(issueKey)
        .name(getName())
        .connectorRef(connectorRef)
        .approvalMessage(getApprovalMessage())
        .projectKey(projectKey)
        .includePipelineExecutionHistory(getIncludePipelineExecutionHistory())
        .build();
  }

  @Override
  public boolean validateStageFailureStrategy() {
    return false;
  }
}

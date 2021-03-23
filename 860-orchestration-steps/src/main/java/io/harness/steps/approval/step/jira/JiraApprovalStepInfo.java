package io.harness.steps.approval.step.jira;

import io.harness.common.SwaggerConstants;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.facilitator.OrchestrationFacilitatorType;
import io.harness.pms.sdk.core.steps.io.BaseStepParameterInfo;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.yaml.ParameterField;
import io.harness.steps.approval.step.ApprovalBaseStepInfo;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.annotation.TypeAlias;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("JiraApproval")
@TypeAlias("jiraApprovalStepInfo")
public class JiraApprovalStepInfo extends ApprovalBaseStepInfo {
  @NotEmpty @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> connectorRef;
  @NotEmpty @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> projectKey;
  @NotNull @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> issueId;
  @NotNull CriteriaSpecWrapper approvalCriteria;
  @NotNull CriteriaSpecWrapper rejectionCriteria;

  @Builder(builderMethodName = "infoBuilder")
  public JiraApprovalStepInfo(String name, String identifier, ParameterField<String> approvalMessage,
      ParameterField<Boolean> includePipelineExecutionHistory, ParameterField<String> connectorRef,
      ParameterField<String> projectKey, ParameterField<String> issueId, CriteriaSpecWrapper approvalCriteria,
      CriteriaSpecWrapper rejectionCriteria) {
    super(name, identifier, approvalMessage, includePipelineExecutionHistory);
    this.connectorRef = connectorRef;
    this.projectKey = projectKey;
    this.issueId = issueId;
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
        .issueId(issueId)
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

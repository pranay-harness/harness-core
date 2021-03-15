package io.harness.plancreator.steps.approval;

import io.harness.common.SwaggerConstants;
import io.harness.delegate.beans.connector.jira.JiraConnectorDTO;
import io.harness.plancreator.steps.internal.PMSStepInfo;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.yaml.ParameterField;
import io.harness.steps.approval.JiraApprovalStep;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.annotation.TypeAlias;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeName("JiraApproval")
@TypeAlias("JiraApprovalStepInfo")
public class JiraApprovalStepInfo implements PMSStepInfo {
  @NotEmpty ParameterField<String> approvalMessage;
  @NotNull ParameterField<Boolean> includePipelineExecutionHistory;
  @NotNull JiraConnectorDTO connectorRef;
  @NotEmpty ParameterField<String> project;
  @NotNull ParameterField<String> issueId;
  @NotNull Criteria approvalCriteria;
  Criteria rejectionCriteria;
  @NotEmpty String identifier;
  @NotEmpty String name;
  @Override
  public void setIdentifier(String identifier) {
    this.identifier = identifier;
  }

  @Override
  public void setName(String name) {
    this.name = name;
  }

  @Override
  public StepType getStepType() {
    return JiraApprovalStep.STEP_TYPE;
  }

  @Override
  public String getFacilitatorType() {
    return null;
  }
}

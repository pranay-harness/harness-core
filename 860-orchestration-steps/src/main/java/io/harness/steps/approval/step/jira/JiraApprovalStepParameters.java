package io.harness.steps.approval.step.jira;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.yaml.ParameterField;
import io.harness.steps.approval.step.ApprovalStepParameters;
import io.harness.steps.approval.step.beans.ApprovalType;
import io.harness.steps.approval.step.jira.beans.CriteriaSpecWrapper;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(CDC)
@Data
@EqualsAndHashCode(callSuper = true)
@TypeAlias("jiraApprovalStepParameters")
public class JiraApprovalStepParameters extends ApprovalStepParameters {
  @NotEmpty ParameterField<String> connectorRef;
  @NotEmpty ParameterField<String> projectKey;
  @NotNull ParameterField<String> issueId;
  @NotNull CriteriaSpecWrapper approvalCriteria;
  @NotNull CriteriaSpecWrapper rejectionCriteria;

  @Builder(builderMethodName = "infoBuilder")
  public JiraApprovalStepParameters(String name, String identifier, ParameterField<String> timeout,
      ParameterField<String> approvalMessage, ParameterField<Boolean> includePipelineExecutionHistory,
      ParameterField<String> connectorRef, ParameterField<String> projectKey, ParameterField<String> issueId,
      CriteriaSpecWrapper approvalCriteria, CriteriaSpecWrapper rejectionCriteria) {
    super(name, identifier, timeout, ApprovalType.JIRA_APPROVAL, approvalMessage, includePipelineExecutionHistory);
    this.connectorRef = connectorRef;
    this.projectKey = projectKey;
    this.issueId = issueId;
    this.approvalCriteria = approvalCriteria;
    this.rejectionCriteria = rejectionCriteria;
  }
}

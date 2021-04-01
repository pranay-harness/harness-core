package io.harness.steps.approval.step.jira;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.yaml.ParameterField;
import io.harness.steps.approval.step.ApprovalStepParameters;
import io.harness.steps.approval.step.beans.ApprovalType;
import io.harness.steps.approval.step.jira.beans.CriteriaSpecWrapper;

import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(CDC)
@Data
@EqualsAndHashCode(callSuper = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@TypeAlias("jiraApprovalStepParameters")
public class JiraApprovalStepParameters extends ApprovalStepParameters {
  @NotEmpty ParameterField<String> connectorRef;
  @NotEmpty ParameterField<String> issueKey;
  @NotNull CriteriaSpecWrapper approvalCriteria;
  @NotNull CriteriaSpecWrapper rejectionCriteria;

  @Builder(builderMethodName = "infoBuilder")
  public JiraApprovalStepParameters(String name, String identifier, ParameterField<String> timeout,
      ParameterField<String> connectorRef, ParameterField<String> issueKey, CriteriaSpecWrapper approvalCriteria,
      CriteriaSpecWrapper rejectionCriteria) {
    super(name, identifier, timeout, ApprovalType.JIRA_APPROVAL);
    this.connectorRef = connectorRef;
    this.issueKey = issueKey;
    this.approvalCriteria = approvalCriteria;
    this.rejectionCriteria = rejectionCriteria;
  }
}

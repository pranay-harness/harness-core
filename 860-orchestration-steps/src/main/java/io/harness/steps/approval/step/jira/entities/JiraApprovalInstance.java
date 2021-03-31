package io.harness.steps.approval.step.jira.entities;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.steps.approval.step.entities.ApprovalInstance;
import io.harness.steps.approval.step.jira.JiraApprovalOutcome;
import io.harness.steps.approval.step.jira.JiraApprovalStepParameters;
import io.harness.steps.approval.step.jira.beans.CriteriaSpecWrapperDTO;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.Persistent;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(CDC)
@Data
@Builder
@FieldNameConstants(innerTypeName = "JiraApprovalInstanceKeys")
@EqualsAndHashCode(callSuper = false)
@Entity(value = "approvalInstances", noClassnameStored = true)
@Persistent
@TypeAlias("jiraApprovalInstances")
public class JiraApprovalInstance extends ApprovalInstance {
  @NotEmpty String connectorRef;
  @NotEmpty String projectKey;
  @NotEmpty String issueKey;
  @NotNull CriteriaSpecWrapperDTO approvalCriteria;
  @NotNull CriteriaSpecWrapperDTO rejectionCriteria;

  public static JiraApprovalInstance fromStepParameters(Ambiance ambiance, JiraApprovalStepParameters stepParameters) {
    if (stepParameters == null) {
      return null;
    }

    String issueKey = stepParameters.getIssueKey().getValue();
    String projectKey = stepParameters.getProjectKey().getValue();
    String connectorRef = stepParameters.getConnectorRef().getValue();

    if (isBlank(issueKey)) {
      throw new InvalidRequestException("issueKey can't be empty");
    }
    if (isBlank(projectKey)) {
      throw new InvalidRequestException("projectKey can't be empty");
    }
    if (isBlank(connectorRef)) {
      throw new InvalidRequestException("connectorRef can't be empty");
    }

    JiraApprovalInstance instance =
        JiraApprovalInstance.builder()
            .projectKey(projectKey)
            .connectorRef(connectorRef)
            .issueKey(issueKey)
            .approvalCriteria(CriteriaSpecWrapperDTO.fromCriteriaSpecWrapper(stepParameters.getApprovalCriteria()))
            .rejectionCriteria(CriteriaSpecWrapperDTO.fromCriteriaSpecWrapper(stepParameters.getRejectionCriteria()))
            .build();
    instance.updateFromStepParameters(ambiance, stepParameters);
    return instance;
  }

  public JiraApprovalOutcome toJiraApprovalOutcome() {
    return JiraApprovalOutcome.builder().build();
  }
}

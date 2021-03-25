package io.harness.steps.approval.step.jira.entities;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.iterator.PersistentRegularIterable;
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
public class JiraApprovalInstance extends ApprovalInstance implements PersistentRegularIterable {
  @NotEmpty String connectorRef;
  @NotEmpty String projectKey;
  @NotEmpty String issueId;
  @NotNull CriteriaSpecWrapperDTO approvalCriteria;
  @NotNull CriteriaSpecWrapperDTO rejectionCriteria;
  long nextIteration;

  public static JiraApprovalInstance fromStepParameters(Ambiance ambiance, JiraApprovalStepParameters stepParameters) {
    if (stepParameters == null) {
      return null;
    }

    String issueId = (String) stepParameters.getIssueId().fetchFinalValue();
    String projectKey = (String) stepParameters.getProjectKey().fetchFinalValue();
    String connectorRef = (String) stepParameters.getConnectorRef().fetchFinalValue();

    if (isBlank(issueId)) {
      throw new InvalidRequestException("Issue Id can't be empty");
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
            .issueId(issueId)
            .approvalCriteria(CriteriaSpecWrapperDTO.fromCriteriaSpecWrapper(stepParameters.getApprovalCriteria()))
            .rejectionCriteria(CriteriaSpecWrapperDTO.fromCriteriaSpecWrapper(stepParameters.getRejectionCriteria()))
            .build();
    instance.updateFromStepParameters(ambiance, stepParameters);
    return instance;
  }

  public JiraApprovalOutcome toJiraApprovalOutcome() {
    return JiraApprovalOutcome.builder().build();
  }

  @Override
  public void updateNextIteration(String fieldName, long nextIteration) {
    this.nextIteration = nextIteration;
  }

  @Override
  public Long obtainNextIteration(String fieldName) {
    return nextIteration;
  }

  @Override
  public String getUuid() {
    return getId();
  }
}

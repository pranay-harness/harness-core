package io.harness.steps.approval.step.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.steps.StepSpecTypeConstants;
import io.harness.steps.approval.step.harness.beans.HarnessApprovalInstanceDetailsDTO;
import io.harness.steps.approval.step.jira.beans.JiraApprovalInstanceDetailsDTO;
import io.harness.steps.approval.step.servicenow.beans.ServiceNowApprovalInstanceDetailsDTO;

import com.fasterxml.jackson.annotation.JsonSubTypes;

@OwnedBy(CDC)
@JsonSubTypes({
  @JsonSubTypes.Type(value = HarnessApprovalInstanceDetailsDTO.class, name = StepSpecTypeConstants.HARNESS_APPROVAL)
  , @JsonSubTypes.Type(value = JiraApprovalInstanceDetailsDTO.class, name = StepSpecTypeConstants.JIRA_APPROVAL),
      @JsonSubTypes.Type(
          value = ServiceNowApprovalInstanceDetailsDTO.class, name = StepSpecTypeConstants.SERVICENOW_APPROVAL)
})
public interface ApprovalInstanceDetailsDTO {}

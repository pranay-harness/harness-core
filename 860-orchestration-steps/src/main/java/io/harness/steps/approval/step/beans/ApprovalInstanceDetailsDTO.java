package io.harness.steps.approval.step.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.steps.approval.step.harness.beans.HarnessApprovalInstanceDetailsDTO;
import io.harness.steps.approval.step.jira.beans.JiraApprovalInstanceDetailsDTO;

import com.fasterxml.jackson.annotation.JsonSubTypes;

@OwnedBy(CDC)
@JsonSubTypes({
  @JsonSubTypes.Type(value = HarnessApprovalInstanceDetailsDTO.class, name = "HarnessApproval")
  , @JsonSubTypes.Type(value = JiraApprovalInstanceDetailsDTO.class, name = "JiraApproval")
})
public interface ApprovalInstanceDetailsDTO {}

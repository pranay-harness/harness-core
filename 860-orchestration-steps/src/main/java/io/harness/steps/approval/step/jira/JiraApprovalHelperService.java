package io.harness.steps.approval.step.jira;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.jira.JiraConnectorDTO;
import io.harness.steps.approval.step.jira.entities.JiraApprovalInstance;

@OwnedBy(CDC)
public interface JiraApprovalHelperService {
  void handlePollingEvent(JiraApprovalInstance entity);
  JiraConnectorDTO getJiraConnector(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String connectorIdentifierRef);
}

package io.harness.cdng.jira.resources.service;

import io.harness.beans.IdentifierRef;
import io.harness.cdng.jira.resources.request.CreateJiraTicketRequest;
import io.harness.cdng.jira.resources.request.UpdateJiraTicketRequest;
import io.harness.cdng.jira.resources.response.JiraIssueDTO;

public interface JiraResourceService {
  boolean validateCredentials(IdentifierRef jiraConnectorRef, String orgIdentifier, String projectIdentifier);
  String createTicket(
      IdentifierRef jiraConnectorRef, String orgIdentifier, String projectIdentifier, CreateJiraTicketRequest request);
  String updateTicket(
      IdentifierRef jiraConnectorRef, String orgIdentifier, String projectIdentifier, UpdateJiraTicketRequest request);
  JiraIssueDTO fetchIssue(
      IdentifierRef jiraConnectorRef, String orgIdentifier, String projectIdentifier, String jiraIssueId);
}

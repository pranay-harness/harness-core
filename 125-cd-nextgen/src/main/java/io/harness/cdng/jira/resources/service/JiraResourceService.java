package io.harness.cdng.jira.resources.service;

import io.harness.beans.IdentifierRef;
import io.harness.cdng.jira.resources.request.CreateJiraTicketRequest;

public interface JiraResourceService {
  boolean validateCredentials(IdentifierRef jiraConnectorRef, String orgIdentifier, String projectIdentifier);
  String createTicket(
      IdentifierRef jiraConnectorRef, String orgIdentifier, String projectIdentifier, CreateJiraTicketRequest request);
}

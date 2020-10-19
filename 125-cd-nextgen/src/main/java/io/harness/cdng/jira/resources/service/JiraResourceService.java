package io.harness.cdng.jira.resources.service;

import io.harness.beans.IdentifierRef;

public interface JiraResourceService {
  boolean validateCredentials(IdentifierRef jiraConnectorRef, String orgIdentifier, String projectIdentifier);
}

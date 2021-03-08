package io.harness.connector.services;

import io.harness.connector.ConnectorValidationResult;

public interface GitRepoConnectorService {
  ConnectorValidationResult testGitRepoConnection(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String connectorIdentifier, String gitRepoURL);
}
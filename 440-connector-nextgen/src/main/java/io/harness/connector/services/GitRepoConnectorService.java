package io.harness.connector.services;

import io.harness.delegate.beans.connector.ConnectorValidationResult;

public interface GitRepoConnectorService {
  ConnectorValidationResult testGitRepoConnection(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String connectorIdentifier, String gitRepoURL);
}
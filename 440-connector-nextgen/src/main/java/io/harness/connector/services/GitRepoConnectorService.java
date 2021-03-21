package io.harness.connector.services;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.ConnectorValidationResult;

@OwnedBy(DX)
public interface GitRepoConnectorService {
  ConnectorValidationResult testGitRepoConnection(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String connectorIdentifier, String gitRepoURL);
}
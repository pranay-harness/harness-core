package io.harness.connector.services;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.stats.ConnectorStatistics;

@OwnedBy(DX)
public interface ConnectorService extends ConnectorCrudService, ConnectorValidationService, GitRepoConnectorService {
  boolean validateTheIdentifierIsUnique(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String connectorIdentifier);

  ConnectorStatistics getConnectorStatistics(String accountIdentifier, String orgIdentifier, String projectIdentifier);
}

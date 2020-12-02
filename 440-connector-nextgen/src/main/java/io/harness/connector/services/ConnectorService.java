package io.harness.connector.services;

import io.harness.connector.apis.dto.stats.ConnectorStatistics;
import io.harness.encryption.Scope;

public interface ConnectorService extends ConnectorCrudService, ConnectorValidationService, GitRepoConnectorService {
  boolean validateTheIdentifierIsUnique(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String connectorIdentifier);

  ConnectorStatistics getConnectorStatistics(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, Scope scope);
}

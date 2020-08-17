package io.harness.connector.services;

public interface ConnectorService extends ConnectorCrudService, ConnectorValidationService {
  boolean validateTheIdentifierIsUnique(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String connectorIdentifier);
}

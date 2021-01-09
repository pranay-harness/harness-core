package io.harness.connector.services;

import io.harness.connector.ConnectorDTO;
import io.harness.delegate.beans.connector.ConnectorValidationResult;

public interface ConnectorValidationService {
  ConnectorValidationResult validate(ConnectorDTO connector, String accountIdentifier);

  ConnectorValidationResult testConnection(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String connectorIdentifier);
}

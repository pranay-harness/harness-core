package io.harness.connector.services;

import io.harness.connector.apis.dto.ConnectorRequestDTO;
import io.harness.delegate.beans.connector.ConnectorValidationResult;

interface ConnectorValidationService {
  ConnectorValidationResult validate(ConnectorRequestDTO connector, String accountIdentifier);

  ConnectorValidationResult testConnection(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String connectorIdentifier);
}

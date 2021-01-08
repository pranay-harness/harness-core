package io.harness.connector.services;

import io.harness.delegate.beans.connector.ConnectorValidationResult;
import io.harness.delegate.beans.connector.apis.dto.ConnectorDTO;

public interface ConnectorValidationService {
  ConnectorValidationResult validate(ConnectorDTO connector, String accountIdentifier);

  ConnectorValidationResult testConnection(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String connectorIdentifier);
}

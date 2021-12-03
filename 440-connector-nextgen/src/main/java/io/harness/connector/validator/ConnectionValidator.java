package io.harness.connector.validator;

import io.harness.connector.ConnectorResponseDTO;
import io.harness.connector.ConnectorValidationResult;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;

public interface ConnectionValidator<T extends ConnectorConfigDTO> {
  ConnectorValidationResult validate(
      T connectorDTO, String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier);
  ConnectorValidationResult validate(ConnectorResponseDTO connectorResponseDTO, String accountIdentifier,
      String orgIdentifier, String projectIdentifier, String identifier);
}

package io.harness.ccm.connectors;

import io.harness.connector.ConnectorResponseDTO;
import io.harness.connector.ConnectorValidationResult;

public interface CEConnectorValidatorIntfc {
  ConnectorValidationResult validate(ConnectorResponseDTO connectorResponseDTO, String accountIdentifier);
}
package io.harness.connector.heartbeat;

import io.harness.connector.ConnectorInfoDTO;
import io.harness.delegate.beans.connector.ConnectorValidationParams;

public interface ConnectorValidationParamsProvider {
  ConnectorValidationParams getConnectorValidationParams(ConnectorInfoDTO connectorConfigDTO, String connectorName,
      String accountIdentifier, String orgIdentifier, String projectIdentifier);
}

package io.harness.connector.heartbeat;

import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.ConnectorValidationParams;

public interface ConnectorValidationParamsProvider {
  ConnectorValidationParams getConnectorValidationParams(ConnectorConfigDTO connectorConfigDTO, String connectorName,
      String accountIdentifier, String orgIdentifier, String projectIdentifier);
}

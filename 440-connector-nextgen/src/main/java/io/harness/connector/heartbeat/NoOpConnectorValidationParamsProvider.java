package io.harness.connector.heartbeat;

import io.harness.connector.ConnectorInfoDTO;
import io.harness.delegate.beans.connector.ConnectorValidationParams;
import io.harness.delegate.beans.connector.NoOpConnectorValidationParams;

import com.google.inject.Singleton;

@Singleton
public class NoOpConnectorValidationParamsProvider implements ConnectorValidationParamsProvider {
  @Override
  public ConnectorValidationParams getConnectorValidationParams(ConnectorInfoDTO connectorConfigDTO,
      String connectorName, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    return NoOpConnectorValidationParams.builder().build();
  }
}

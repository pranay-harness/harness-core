package io.harness.connector.heartbeat;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.delegate.beans.connector.ConnectorValidationParams;
import io.harness.delegate.beans.connector.NoOpConnectorValidationParams;

import com.google.inject.Singleton;

@Singleton
@OwnedBy(DX)
public class NoOpConnectorValidationParamsProvider implements ConnectorValidationParamsProvider {
  @Override
  public ConnectorValidationParams getConnectorValidationParams(ConnectorInfoDTO connectorConfigDTO,
      String connectorName, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    return NoOpConnectorValidationParams.builder().build();
  }
}

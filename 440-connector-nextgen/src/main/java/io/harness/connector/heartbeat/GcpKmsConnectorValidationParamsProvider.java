package io.harness.connector.heartbeat;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.delegate.beans.connector.ConnectorValidationParams;
import io.harness.delegate.beans.connector.gcpkmsconnector.GcpKmsConnectorDTO;
import io.harness.delegate.beans.connector.gcpkmsconnector.GcpKmsValidationParams;

@OwnedBy(DX)
public class GcpKmsConnectorValidationParamsProvider implements ConnectorValidationParamsProvider {
  @Override
  public ConnectorValidationParams getConnectorValidationParams(ConnectorInfoDTO connectorConfigDTO,
      String connectorName, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    return GcpKmsValidationParams.builder()
        .gcpKmsConnectorDTO((GcpKmsConnectorDTO) connectorConfigDTO.getConnectorConfig())
        .connectorName(connectorName)
        .build();
  }
}

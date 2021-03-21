package io.harness.connector.heartbeat;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.delegate.beans.connector.ConnectorValidationParams;
import io.harness.delegate.beans.connector.vaultconnector.VaultConnectorDTO;
import io.harness.delegate.beans.connector.vaultconnector.VaultValidationParams;

@OwnedBy(DX)
public class VaultConnectorValidationParamsProvider implements ConnectorValidationParamsProvider {
  @Override
  public ConnectorValidationParams getConnectorValidationParams(ConnectorInfoDTO connectorConfigDTO,
      String connectorName, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    return VaultValidationParams.builder()
        .vaultConnectorDTO((VaultConnectorDTO) connectorConfigDTO.getConnectorConfig())
        .connectorName(connectorName)
        .build();
  }
}

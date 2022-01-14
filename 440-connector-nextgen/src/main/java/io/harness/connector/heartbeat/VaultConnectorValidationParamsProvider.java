package io.harness.connector.heartbeat;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.ConnectorValidationParams;
import io.harness.delegate.beans.connector.vaultconnector.VaultConnectorDTO;
import io.harness.delegate.beans.connector.vaultconnector.VaultValidationParams;

@OwnedBy(PL)
public class VaultConnectorValidationParamsProvider
    extends SecretManagerConnectorValidationParamsProvider implements ConnectorValidationParamsProvider {
  @Override
  public ConnectorValidationParams getConnectorValidationParams(ConnectorInfoDTO connectorConfigDTO,
      String connectorName, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    ConnectorConfigDTO connectorConfig =
        getDecryptedConnectorConfigDTO(connectorConfigDTO, accountIdentifier, orgIdentifier, projectIdentifier);
    return VaultValidationParams.builder()
        .vaultConnectorDTO((VaultConnectorDTO) connectorConfig)
        .connectorName(connectorName)
        .build();
  }
}

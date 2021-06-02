package io.harness.connector.heartbeat;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.delegate.beans.connector.ConnectorValidationParams;
import io.harness.delegate.beans.connector.azurekeyvaultconnector.AzureKeyVaultConnectorDTO;
import io.harness.delegate.beans.connector.azurekeyvaultconnector.AzureKeyVaultValidationParams;

@OwnedBy(PL)
public class AzureKeyVaultConnectorValidationParamsProvider implements ConnectorValidationParamsProvider {
  @Override
  public ConnectorValidationParams getConnectorValidationParams(ConnectorInfoDTO connectorConfigDTO,
      String connectorName, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    return AzureKeyVaultValidationParams.builder()
        .azurekeyvaultConnectorDTO((AzureKeyVaultConnectorDTO) connectorConfigDTO.getConnectorConfig())
        .connectorName(connectorName)
        .build();
  }
}

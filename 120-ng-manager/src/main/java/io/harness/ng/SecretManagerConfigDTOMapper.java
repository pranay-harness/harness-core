package io.harness.ng;

import static io.harness.ng.GcpKmsConfigDTOMapper.getGcpKmsConfigDTO;
import static io.harness.ng.LocalConfigDTOMapper.getLocalConfigDTO;
import static io.harness.ng.VaultConfigDTOMapper.getVaultConfigDTO;

import io.harness.connector.apis.dto.ConnectorRequestDTO;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.secretmanagerclient.dto.GcpKmsConnectorDTO;
import io.harness.secretmanagerclient.dto.LocalConnectorDTO;
import io.harness.secretmanagerclient.dto.SecretManagerConfigDTO;
import io.harness.secretmanagerclient.dto.VaultConnectorDTO;
import lombok.experimental.UtilityClass;

@UtilityClass
public class SecretManagerConfigDTOMapper {
  public static SecretManagerConfigDTO fromConnectorDTO(
      String accountIdentifier, ConnectorRequestDTO connectorRequestDTO, ConnectorConfigDTO connectorConfigDTO) {
    switch (connectorRequestDTO.getConnectorType()) {
      case VAULT:
        return getVaultConfigDTO(accountIdentifier, connectorRequestDTO, (VaultConnectorDTO) connectorConfigDTO);
      case GCP_KMS:
        return getGcpKmsConfigDTO(accountIdentifier, connectorRequestDTO, (GcpKmsConnectorDTO) connectorConfigDTO);
      case LOCAL:
        return getLocalConfigDTO(accountIdentifier, connectorRequestDTO, (LocalConnectorDTO) connectorConfigDTO);
      default:
        throw new IllegalArgumentException(
            "This is not a valid secret manager type: " + connectorRequestDTO.getConnectorType());
    }
  }
}

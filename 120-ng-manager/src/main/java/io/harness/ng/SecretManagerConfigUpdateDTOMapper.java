package io.harness.ng;

import static io.harness.ng.GcpKmsConfigDTOMapper.getGcpKmsConfigUpdateDTO;
import static io.harness.ng.VaultConfigDTOMapper.getVaultConfigUpdateDTO;

import io.harness.connector.apis.dto.ConnectorRequestDTO;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.gcpkmsconnector.GcpKmsConnectorDTO;
import io.harness.delegate.beans.connector.vaultconnector.VaultConnectorDTO;
import io.harness.secretmanagerclient.dto.SecretManagerConfigUpdateDTO;
import lombok.experimental.UtilityClass;

@UtilityClass
public class SecretManagerConfigUpdateDTOMapper {
  public static SecretManagerConfigUpdateDTO fromConnectorDTO(
      ConnectorRequestDTO connectorRequestDTO, ConnectorConfigDTO connectorConfigDTO) {
    if (connectorRequestDTO.getConnectorType() == ConnectorType.VAULT) {
      return getVaultConfigUpdateDTO(connectorRequestDTO, (VaultConnectorDTO) connectorConfigDTO);
    } else if (connectorRequestDTO.getConnectorType() == ConnectorType.GCP_KMS) {
      return getGcpKmsConfigUpdateDTO(connectorRequestDTO, (GcpKmsConnectorDTO) connectorConfigDTO);
    }
    throw new IllegalArgumentException(
        "This is not a valid secret manager type: " + connectorRequestDTO.getConnectorType());
  }
}

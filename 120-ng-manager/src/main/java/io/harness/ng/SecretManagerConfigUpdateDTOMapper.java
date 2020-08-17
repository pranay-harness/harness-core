package io.harness.ng;

import io.harness.connector.apis.dto.ConnectorRequestDTO;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.secretmanagerclient.dto.SecretManagerConfigUpdateDTO;
import io.harness.secretmanagerclient.dto.VaultConfigUpdateDTO;
import io.harness.secretmanagerclient.dto.VaultConnectorDTO;
import io.harness.security.encryption.EncryptionType;
import lombok.experimental.UtilityClass;

@UtilityClass
public class SecretManagerConfigUpdateDTOMapper {
  public static SecretManagerConfigUpdateDTO fromConnectorDTO(
      ConnectorRequestDTO connectorRequestDTO, ConnectorConfigDTO connectorConfigDTO) {
    if (connectorRequestDTO.getConnectorType() == ConnectorType.VAULT) {
      VaultConnectorDTO vaultConnectorDTO = (VaultConnectorDTO) connectorConfigDTO;
      return VaultConfigUpdateDTO.builder()
          .authToken(vaultConnectorDTO.getAuthToken())
          .basePath(vaultConnectorDTO.getBasePath())
          .vaultUrl(vaultConnectorDTO.getVaultUrl())
          .isReadOnly(vaultConnectorDTO.isReadOnly())
          .renewIntervalHours(vaultConnectorDTO.getRenewIntervalHours())
          .secretEngineName(vaultConnectorDTO.getSecretEngineName())
          .appRoleId(vaultConnectorDTO.getAppRoleId())
          .secretId(vaultConnectorDTO.getSecretId())
          .isDefault(vaultConnectorDTO.isDefault())
          .encryptionType(EncryptionType.VAULT)

          .tags(connectorRequestDTO.getTags())
          .description(connectorRequestDTO.getDescription())
          .build();
    }
    throw new IllegalArgumentException(
        "This is not a valid secret manager type: " + connectorRequestDTO.getConnectorType());
  }
}

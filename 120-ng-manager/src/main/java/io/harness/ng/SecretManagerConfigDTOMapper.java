package io.harness.ng;

import io.harness.connector.apis.dto.ConnectorRequestDTO;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.secretmanagerclient.dto.SecretManagerConfigDTO;
import io.harness.secretmanagerclient.dto.VaultConfigDTO;
import io.harness.secretmanagerclient.dto.VaultConnectorDTO;
import io.harness.security.encryption.EncryptionType;
import lombok.experimental.UtilityClass;

@UtilityClass
public class SecretManagerConfigDTOMapper {
  public static SecretManagerConfigDTO fromConnectorDTO(
      String accountIdentifier, ConnectorRequestDTO connectorRequestDTO, ConnectorConfigDTO connectorConfigDTO) {
    if (connectorRequestDTO.getConnectorType() == ConnectorType.VAULT) {
      VaultConnectorDTO vaultConnectorDTO = (VaultConnectorDTO) connectorConfigDTO;
      return VaultConfigDTO.builder()
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

          .name(connectorRequestDTO.getName())
          .accountIdentifier(accountIdentifier)
          .orgIdentifier(connectorRequestDTO.getOrgIdentifier())
          .projectIdentifier(connectorRequestDTO.getProjectIdentifier())
          .tags(connectorRequestDTO.getTags())
          .identifier(connectorRequestDTO.getIdentifier())
          .description(connectorRequestDTO.getDescription())
          .build();
    }
    throw new IllegalArgumentException(
        "This is not a valid secret manager type: " + connectorRequestDTO.getConnectorType());
  }
}

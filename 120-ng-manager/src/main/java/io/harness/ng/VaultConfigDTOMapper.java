package io.harness.ng;

import io.harness.connector.apis.dto.ConnectorRequestDTO;
import io.harness.secretmanagerclient.dto.VaultConfigDTO;
import io.harness.secretmanagerclient.dto.VaultConfigUpdateDTO;
import io.harness.secretmanagerclient.dto.VaultConnectorDTO;
import io.harness.security.encryption.EncryptionType;
import lombok.experimental.UtilityClass;

@UtilityClass
public class VaultConfigDTOMapper {
  public static VaultConfigUpdateDTO getVaultConfigUpdateDTO(
      ConnectorRequestDTO connectorRequestDTO, VaultConnectorDTO vaultConnectorDTO) {
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

  public static VaultConfigDTO getVaultConfigDTO(
      String accountIdentifier, ConnectorRequestDTO connectorRequestDTO, VaultConnectorDTO vaultConnectorDTO) {
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
}

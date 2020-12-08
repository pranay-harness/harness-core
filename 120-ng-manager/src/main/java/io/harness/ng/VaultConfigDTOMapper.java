package io.harness.ng;

import io.harness.connector.apis.dto.ConnectorDTO;
import io.harness.connector.apis.dto.ConnectorInfoDTO;
import io.harness.delegate.beans.connector.vaultconnector.VaultConnectorDTO;
import io.harness.secretmanagerclient.dto.VaultConfigDTO;
import io.harness.secretmanagerclient.dto.VaultConfigUpdateDTO;
import io.harness.security.encryption.EncryptionType;

import lombok.experimental.UtilityClass;

@UtilityClass
public class VaultConfigDTOMapper {
  public static VaultConfigUpdateDTO getVaultConfigUpdateDTO(
      ConnectorDTO connectorRequestDTO, VaultConnectorDTO vaultConnectorDTO) {
    ConnectorInfoDTO connector = connectorRequestDTO.getConnectorInfo();
    return VaultConfigUpdateDTO.builder()
        .authToken(vaultConnectorDTO.getAuthToken())
        .basePath(vaultConnectorDTO.getBasePath())
        .vaultUrl(vaultConnectorDTO.getVaultUrl())
        .isReadOnly(vaultConnectorDTO.isReadOnly())
        .renewIntervalHours(vaultConnectorDTO.getRenewIntervalHours())
        .secretEngineName(vaultConnectorDTO.getSecretEngineName())
        .appRoleId(vaultConnectorDTO.getAppRoleId())
        .secretId(vaultConnectorDTO.getSecretId())
        .isDefault(false)
        .encryptionType(EncryptionType.VAULT)
        .tags(connector.getTags())
        .description(connector.getDescription())
        .build();
  }

  public static VaultConfigDTO getVaultConfigDTO(
      String accountIdentifier, ConnectorDTO connectorRequestDTO, VaultConnectorDTO vaultConnectorDTO) {
    ConnectorInfoDTO connector = connectorRequestDTO.getConnectorInfo();
    return VaultConfigDTO.builder()
        .authToken(vaultConnectorDTO.getAuthToken())
        .basePath(vaultConnectorDTO.getBasePath())
        .vaultUrl(vaultConnectorDTO.getVaultUrl())
        .isReadOnly(vaultConnectorDTO.isReadOnly())
        .renewIntervalHours(vaultConnectorDTO.getRenewIntervalHours())
        .secretEngineName(vaultConnectorDTO.getSecretEngineName())
        .appRoleId(vaultConnectorDTO.getAppRoleId())
        .secretId(vaultConnectorDTO.getSecretId())
        .isDefault(false)
        .encryptionType(EncryptionType.VAULT)
        .secretEngineVersion(vaultConnectorDTO.getSecretEngineVersion())

        .name(connector.getName())
        .accountIdentifier(accountIdentifier)
        .orgIdentifier(connector.getOrgIdentifier())
        .projectIdentifier(connector.getProjectIdentifier())
        .tags(connector.getTags())
        .identifier(connector.getIdentifier())
        .description(connector.getDescription())
        .build();
  }
}

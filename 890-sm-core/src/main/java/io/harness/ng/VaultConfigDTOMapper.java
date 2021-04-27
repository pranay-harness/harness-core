package io.harness.ng;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.ConnectorDTO;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.delegate.beans.connector.vaultconnector.VaultConnectorDTO;
import io.harness.secretmanagerclient.dto.VaultConfigDTO;
import io.harness.secretmanagerclient.dto.VaultConfigUpdateDTO;
import io.harness.security.encryption.EncryptionType;

import lombok.experimental.UtilityClass;

@OwnedBy(PL)
@UtilityClass
public class VaultConfigDTOMapper {
  public static VaultConfigUpdateDTO getVaultConfigUpdateDTO(
      ConnectorDTO connectorRequestDTO, VaultConnectorDTO vaultConnectorDTO) {
    vaultConnectorDTO.validate();
    ConnectorInfoDTO connector = connectorRequestDTO.getConnectorInfo();
    return VaultConfigUpdateDTO.builder()
        .authToken(vaultConnectorDTO.getAuthToken())
        .basePath(vaultConnectorDTO.getBasePath())
        .vaultUrl(vaultConnectorDTO.getVaultUrl())
        .isReadOnly(vaultConnectorDTO.isReadOnly())
        .renewalIntervalMinutes(vaultConnectorDTO.getRenewalIntervalMinutes())
        .secretEngineName(vaultConnectorDTO.getSecretEngineName())
        .secretEngineVersion(vaultConnectorDTO.getSecretEngineVersion())
        .appRoleId(vaultConnectorDTO.getAppRoleId())
        .secretId(vaultConnectorDTO.getSecretId())
        .isDefault(false)
        .name(connector.getName())
        .encryptionType(EncryptionType.VAULT)
        .tags(connector.getTags())
        .description(connector.getDescription())
        .build();
  }

  public static VaultConfigDTO getVaultConfigDTO(
      String accountIdentifier, ConnectorDTO connectorRequestDTO, VaultConnectorDTO vaultConnectorDTO) {
    vaultConnectorDTO.validate();
    ConnectorInfoDTO connector = connectorRequestDTO.getConnectorInfo();
    return VaultConfigDTO.builder()
        .authToken(vaultConnectorDTO.getAuthToken())
        .basePath(vaultConnectorDTO.getBasePath())
        .vaultUrl(vaultConnectorDTO.getVaultUrl())
        .isReadOnly(vaultConnectorDTO.isReadOnly())
        .renewalIntervalMinutes(vaultConnectorDTO.getRenewalIntervalMinutes())
        .secretEngineName(vaultConnectorDTO.getSecretEngineName())
        .secretEngineVersion(vaultConnectorDTO.getSecretEngineVersion())
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

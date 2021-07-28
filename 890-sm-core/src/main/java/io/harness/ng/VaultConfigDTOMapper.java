package io.harness.ng;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.secretmanagerclient.dto.VaultConfigDTO.VaultConfigDTOBuilder;
import static io.harness.secretmanagerclient.dto.VaultConfigUpdateDTO.VaultConfigUpdateDTOBuilder;

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

    VaultConfigUpdateDTOBuilder<?, ?> builder =
        VaultConfigUpdateDTO.builder()
            .basePath(vaultConnectorDTO.getBasePath())
            .vaultUrl(vaultConnectorDTO.getVaultUrl())
            .isReadOnly(vaultConnectorDTO.isReadOnly())
            .renewalIntervalMinutes(vaultConnectorDTO.getRenewalIntervalMinutes())
            .secretEngineName(vaultConnectorDTO.getSecretEngineName())
            .secretEngineVersion(vaultConnectorDTO.getSecretEngineVersion())
            .appRoleId(vaultConnectorDTO.getAppRoleId())
            .isDefault(false)
            .name(connector.getName())
            .encryptionType(EncryptionType.VAULT)
            .tags(connector.getTags())
            .description(connector.getDescription());

    if (null != vaultConnectorDTO.getAuthToken() && null != vaultConnectorDTO.getAuthToken().getDecryptedValue()) {
      builder.authToken(String.valueOf(vaultConnectorDTO.getAuthToken().getDecryptedValue()));
    }
    if (null != vaultConnectorDTO.getSecretId() && null != vaultConnectorDTO.getSecretId().getDecryptedValue()) {
      builder.secretId(String.valueOf(vaultConnectorDTO.getSecretId().getDecryptedValue()));
    }
    return builder.build();
  }

  public static VaultConfigDTO getVaultConfigDTO(
      String accountIdentifier, ConnectorDTO connectorRequestDTO, VaultConnectorDTO vaultConnectorDTO) {
    vaultConnectorDTO.validate();
    ConnectorInfoDTO connector = connectorRequestDTO.getConnectorInfo();

    VaultConfigDTOBuilder<?, ?> builder = VaultConfigDTO.builder()
                                              .basePath(vaultConnectorDTO.getBasePath())
                                              .vaultUrl(vaultConnectorDTO.getVaultUrl())
                                              .isReadOnly(vaultConnectorDTO.isReadOnly())
                                              .renewalIntervalMinutes(vaultConnectorDTO.getRenewalIntervalMinutes())
                                              .secretEngineName(vaultConnectorDTO.getSecretEngineName())
                                              .secretEngineVersion(vaultConnectorDTO.getSecretEngineVersion())
                                              .appRoleId(vaultConnectorDTO.getAppRoleId())
                                              .isDefault(false)
                                              .encryptionType(EncryptionType.VAULT)
                                              .secretEngineVersion(vaultConnectorDTO.getSecretEngineVersion())
                                              .delegateSelectors(vaultConnectorDTO.getDelegateSelectors())

                                              .name(connector.getName())
                                              .accountIdentifier(accountIdentifier)
                                              .orgIdentifier(connector.getOrgIdentifier())
                                              .projectIdentifier(connector.getProjectIdentifier())
                                              .tags(connector.getTags())
                                              .identifier(connector.getIdentifier())
                                              .description(connector.getDescription());

    if (null != vaultConnectorDTO.getAuthToken() && null != vaultConnectorDTO.getAuthToken().getDecryptedValue()) {
      builder.authToken(String.valueOf(vaultConnectorDTO.getAuthToken().getDecryptedValue()));
    }
    if (null != vaultConnectorDTO.getSecretId() && null != vaultConnectorDTO.getSecretId().getDecryptedValue()) {
      builder.secretId(String.valueOf(vaultConnectorDTO.getSecretId().getDecryptedValue()));
    }

    return builder.build();
  }
}

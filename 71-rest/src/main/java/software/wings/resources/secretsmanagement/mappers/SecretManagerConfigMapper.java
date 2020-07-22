package software.wings.resources.secretsmanagement.mappers;

import io.harness.secretmanagerclient.NGSecretMetadata;
import io.harness.secretmanagerclient.dto.NGSecretManagerConfigDTO;
import io.harness.secretmanagerclient.dto.NGSecretManagerConfigUpdateDTO;
import io.harness.secretmanagerclient.dto.NGVaultConfigDTO;
import io.harness.secretmanagerclient.dto.NGVaultConfigUpdateDTO;
import io.harness.security.encryption.EncryptionType;
import lombok.experimental.UtilityClass;
import software.wings.beans.SecretManagerConfig;
import software.wings.beans.VaultConfig;

@UtilityClass
public class SecretManagerConfigMapper {
  public static SecretManagerConfig applyUpdate(NGSecretManagerConfigDTO dto) {
    if (dto.getEncryptionType() == EncryptionType.VAULT) {
      NGVaultConfigDTO vaultConfigDTO = (NGVaultConfigDTO) dto;
      VaultConfig vaultConfig = VaultConfig.builder()
                                    .vaultUrl(vaultConfigDTO.getVaultUrl())
                                    .name(vaultConfigDTO.getName())
                                    .authToken(vaultConfigDTO.getAuthToken())
                                    .secretEngineName(vaultConfigDTO.getSecretEngineName())
                                    .basePath(vaultConfigDTO.getBasePath())
                                    .appRoleId(vaultConfigDTO.getAppRoleId())
                                    .renewIntervalHours(vaultConfigDTO.getRenewIntervalHours())
                                    .secretId(vaultConfigDTO.getSecretId())
                                    .isReadOnly(vaultConfigDTO.isReadOnly())
                                    .build();
      NGSecretMetadata ngMetadata = NGSecretMetadata.builder()
                                        .accountIdentifier(vaultConfigDTO.getAccountIdentifier())
                                        .identifier(vaultConfigDTO.getIdentifier())
                                        .orgIdentifier(vaultConfigDTO.getOrgIdentifier())
                                        .projectIdentifier(vaultConfigDTO.getProjectIdentifier())
                                        .tags(vaultConfigDTO.getTags())
                                        .build();
      vaultConfig.setNgMetadata(ngMetadata);
      vaultConfig.setUuid(vaultConfigDTO.getUuid());
      vaultConfig.setEncryptionType(vaultConfigDTO.getEncryptionType());
      vaultConfig.setDefault(vaultConfigDTO.isDefault());
      return vaultConfig;
    }
    throw new UnsupportedOperationException("Secret Manager not supported");
  }

  public static SecretManagerConfig applyUpdate(
      SecretManagerConfig secretManagerConfig, NGSecretManagerConfigUpdateDTO dto) {
    if (dto.getEncryptionType() == EncryptionType.VAULT) {
      NGVaultConfigUpdateDTO vaultConfigDTO = (NGVaultConfigUpdateDTO) dto;
      VaultConfig vaultConfig = (VaultConfig) secretManagerConfig;
      vaultConfig.setVaultUrl(vaultConfigDTO.getVaultUrl());
      vaultConfig.setName(vaultConfigDTO.getName());
      vaultConfig.setAuthToken(vaultConfigDTO.getAuthToken());
      vaultConfig.setSecretEngineName(vaultConfigDTO.getSecretEngineName());
      vaultConfig.setBasePath(vaultConfigDTO.getBasePath());
      vaultConfig.setAppRoleId(vaultConfigDTO.getAppRoleId());
      vaultConfig.setRenewIntervalHours(vaultConfigDTO.getRenewIntervalHours());
      vaultConfig.setSecretId(vaultConfigDTO.getSecretId());
      vaultConfig.setReadOnly(vaultConfigDTO.isReadOnly());
      vaultConfig.setDefault(vaultConfigDTO.isDefault());
      return vaultConfig;
    }
    throw new UnsupportedOperationException("Secret Manager not supported");
  }
}

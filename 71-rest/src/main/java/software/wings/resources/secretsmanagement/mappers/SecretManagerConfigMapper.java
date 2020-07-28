package software.wings.resources.secretsmanagement.mappers;

import io.harness.secretmanagerclient.NGSecretManagerMetadata;
import io.harness.secretmanagerclient.dto.NGSecretManagerConfigUpdateDTO;
import io.harness.secretmanagerclient.dto.SecretManagerConfigDTO;
import io.harness.secretmanagerclient.dto.VaultConfigDTO;
import io.harness.secretmanagerclient.dto.VaultConfigUpdateDTO;
import io.harness.security.encryption.EncryptionType;
import lombok.experimental.UtilityClass;
import software.wings.beans.SecretManagerConfig;
import software.wings.beans.VaultConfig;

import java.util.Optional;

@UtilityClass
public class SecretManagerConfigMapper {
  public static SecretManagerConfig fromDTO(SecretManagerConfigDTO dto) {
    if (dto.getEncryptionType() == EncryptionType.VAULT) {
      VaultConfigDTO vaultConfigDTO = (VaultConfigDTO) dto;
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
      NGSecretManagerMetadata ngMetadata = NGSecretManagerMetadata.builder()
                                               .accountIdentifier(vaultConfigDTO.getAccountIdentifier())
                                               .identifier(vaultConfigDTO.getIdentifier())
                                               .orgIdentifier(vaultConfigDTO.getOrgIdentifier())
                                               .projectIdentifier(vaultConfigDTO.getProjectIdentifier())
                                               .description(vaultConfigDTO.getDescription())
                                               .tags(vaultConfigDTO.getTags())
                                               .build();
      vaultConfig.setNgMetadata(ngMetadata);
      vaultConfig.setUuid(vaultConfigDTO.getUuid());
      vaultConfig.setAccountId(vaultConfigDTO.getAccountIdentifier());
      vaultConfig.setEncryptionType(vaultConfigDTO.getEncryptionType());
      vaultConfig.setDefault(vaultConfigDTO.isDefault());
      return vaultConfig;
    }
    throw new UnsupportedOperationException("Secret Manager not supported");
  }

  public static SecretManagerConfig applyUpdate(
      SecretManagerConfig secretManagerConfig, NGSecretManagerConfigUpdateDTO dto) {
    if (dto.getEncryptionType() == EncryptionType.VAULT) {
      VaultConfigUpdateDTO vaultConfigDTO = (VaultConfigUpdateDTO) dto;
      VaultConfig vaultConfig = (VaultConfig) secretManagerConfig;
      vaultConfig.setVaultUrl(vaultConfigDTO.getVaultUrl());
      vaultConfig.setAuthToken(vaultConfigDTO.getAuthToken());
      vaultConfig.setSecretEngineName(vaultConfigDTO.getSecretEngineName());
      vaultConfig.setBasePath(vaultConfigDTO.getBasePath());
      vaultConfig.setAppRoleId(vaultConfigDTO.getAppRoleId());
      vaultConfig.setRenewIntervalHours(vaultConfigDTO.getRenewIntervalHours());
      vaultConfig.setSecretId(vaultConfigDTO.getSecretId());
      vaultConfig.setReadOnly(vaultConfigDTO.isReadOnly());
      vaultConfig.setDefault(vaultConfigDTO.isDefault());
      if (!Optional.ofNullable(vaultConfig.getNgMetadata()).isPresent()) {
        vaultConfig.setNgMetadata(NGSecretManagerMetadata.builder().build());
      }
      vaultConfig.getNgMetadata().setTags(vaultConfigDTO.getTags());
      vaultConfig.getNgMetadata().setDescription(vaultConfigDTO.getDescription());
      return vaultConfig;
    }
    throw new UnsupportedOperationException("Secret Manager not supported");
  }
}

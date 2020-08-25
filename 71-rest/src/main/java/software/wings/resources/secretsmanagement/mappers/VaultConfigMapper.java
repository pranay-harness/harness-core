package software.wings.resources.secretsmanagement.mappers;

import static software.wings.resources.secretsmanagement.mappers.SecretManagerConfigMapper.ngMetaDataFromDto;

import io.harness.secretmanagerclient.NGSecretManagerMetadata;
import io.harness.secretmanagerclient.dto.VaultConfigDTO;
import io.harness.secretmanagerclient.dto.VaultConfigUpdateDTO;
import lombok.experimental.UtilityClass;
import software.wings.beans.VaultConfig;

import java.util.Optional;

@UtilityClass
public class VaultConfigMapper {
  public static VaultConfig fromDTO(VaultConfigDTO vaultConfigDTO) {
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
    vaultConfig.setNgMetadata(ngMetaDataFromDto(vaultConfigDTO));
    vaultConfig.setAccountId(vaultConfigDTO.getAccountIdentifier());
    vaultConfig.setEncryptionType(vaultConfigDTO.getEncryptionType());
    vaultConfig.setDefault(vaultConfigDTO.isDefault());
    return vaultConfig;
  }

  public static VaultConfig applyUpdate(VaultConfig vaultConfig, VaultConfigUpdateDTO vaultConfigDTO) {
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
}

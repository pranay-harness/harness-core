package io.harness.mappers;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.mappers.SecretManagerConfigMapper.ngMetaDataFromDto;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.mapper.TagMapper;
import io.harness.secretmanagerclient.NGSecretManagerMetadata;
import io.harness.secretmanagerclient.dto.VaultConfigDTO;
import io.harness.secretmanagerclient.dto.VaultConfigUpdateDTO;

import software.wings.beans.VaultConfig;

import java.util.Optional;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

@UtilityClass
@OwnedBy(PL)
public class VaultConfigMapper {
  public static VaultConfig fromDTO(VaultConfigDTO vaultConfigDTO) {
    VaultConfig vaultConfig = VaultConfig.builder()
                                  .vaultUrl(vaultConfigDTO.getVaultUrl())
                                  .name(vaultConfigDTO.getName())
                                  .authToken(vaultConfigDTO.getAuthToken())
                                  .secretEngineName(vaultConfigDTO.getSecretEngineName())
                                  .secretEngineVersion(vaultConfigDTO.getSecretEngineVersion())
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
    if (StringUtils.isEmpty(vaultConfigDTO.getAuthToken())) {
      vaultConfig.setAuthToken(vaultConfigDTO.getAuthToken());
    }
    if (StringUtils.isEmpty(vaultConfigDTO.getAppRoleId())) {
      vaultConfig.setAppRoleId(vaultConfigDTO.getAppRoleId());
    }
    if (StringUtils.isEmpty(vaultConfigDTO.getSecretId())) {
      vaultConfig.setSecretId(vaultConfigDTO.getSecretId());
    }
    vaultConfig.setSecretEngineName(vaultConfigDTO.getSecretEngineName());
    vaultConfig.setBasePath(vaultConfigDTO.getBasePath());
    vaultConfig.setRenewIntervalHours(vaultConfigDTO.getRenewIntervalHours());
    vaultConfig.setReadOnly(vaultConfigDTO.isReadOnly());
    vaultConfig.setDefault(vaultConfigDTO.isDefault());
    vaultConfig.setSecretEngineVersion(vaultConfigDTO.getSecretEngineVersion());
    if (!Optional.ofNullable(vaultConfig.getNgMetadata()).isPresent()) {
      vaultConfig.setNgMetadata(NGSecretManagerMetadata.builder().build());
    }
    vaultConfig.getNgMetadata().setTags(TagMapper.convertToList(vaultConfigDTO.getTags()));
    vaultConfig.getNgMetadata().setDescription(vaultConfigDTO.getDescription());
    return vaultConfig;
  }
}

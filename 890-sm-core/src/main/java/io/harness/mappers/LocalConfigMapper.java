package io.harness.mappers;

import static io.harness.mappers.SecretManagerConfigMapper.ngMetaDataFromDto;

import io.harness.secretmanagerclient.dto.LocalConfigDTO;

import software.wings.beans.LocalEncryptionConfig;

import lombok.experimental.UtilityClass;

@UtilityClass
public class LocalConfigMapper {
  public static LocalEncryptionConfig fromDTO(LocalConfigDTO localConfigDTO) {
    LocalEncryptionConfig localConfig = LocalEncryptionConfig.builder().name(localConfigDTO.getName()).build();
    localConfig.setNgMetadata(ngMetaDataFromDto(localConfigDTO));
    localConfig.setAccountId(localConfigDTO.getAccountIdentifier());
    localConfig.setEncryptionType(localConfigDTO.getEncryptionType());
    localConfig.setDefault(localConfigDTO.isDefault());
    return localConfig;
  }
}

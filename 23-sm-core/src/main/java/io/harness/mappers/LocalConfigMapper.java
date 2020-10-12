package io.harness.mappers;

import static io.harness.mappers.SecretManagerConfigMapper.ngMetaDataFromDto;

import io.harness.secretmanagerclient.dto.LocalConfigDTO;
import lombok.experimental.UtilityClass;
import software.wings.beans.LocalEncryptionConfig;

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

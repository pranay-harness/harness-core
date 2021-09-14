/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.mappers;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.mappers.SecretManagerConfigMapper.ngMetaDataFromDto;

import io.harness.annotations.dev.OwnedBy;
import io.harness.secretmanagerclient.dto.LocalConfigDTO;

import software.wings.beans.LocalEncryptionConfig;

import lombok.experimental.UtilityClass;

@OwnedBy(PL)
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

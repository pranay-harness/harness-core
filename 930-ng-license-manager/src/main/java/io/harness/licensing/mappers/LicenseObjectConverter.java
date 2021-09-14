/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.licensing.mappers;

import io.harness.ModuleType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.licensing.beans.modules.ModuleLicenseDTO;
import io.harness.licensing.entities.modules.ModuleLicense;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Map;

@OwnedBy(HarnessTeam.GTM)
@Singleton
public class LicenseObjectConverter {
  @Inject Map<ModuleType, LicenseObjectMapper> mapperMap;

  public <T extends ModuleLicenseDTO> T toDTO(ModuleLicense moduleLicense) {
    ModuleType moduleType = moduleLicense.getModuleType();
    ModuleLicenseDTO moduleLicenseDTO = mapperMap.get(moduleType).toDTO(moduleLicense);
    moduleLicenseDTO.setId(moduleLicense.getId());
    moduleLicenseDTO.setAccountIdentifier(moduleLicense.getAccountIdentifier());
    moduleLicenseDTO.setModuleType(moduleLicense.getModuleType());
    moduleLicenseDTO.setEdition(moduleLicense.getEdition());
    moduleLicenseDTO.setLicenseType(moduleLicense.getLicenseType());
    moduleLicenseDTO.setStatus(moduleLicense.getStatus());
    moduleLicenseDTO.setStartTime(moduleLicense.getStartTime());
    moduleLicenseDTO.setExpiryTime(moduleLicense.getExpiryTime());
    moduleLicenseDTO.setCreatedAt(moduleLicense.getCreatedAt());
    moduleLicenseDTO.setLastModifiedAt(moduleLicense.getLastUpdatedAt());
    return (T) moduleLicenseDTO;
  }

  public <T extends ModuleLicense> T toEntity(ModuleLicenseDTO moduleLicenseDTO) {
    ModuleType moduleType = moduleLicenseDTO.getModuleType();
    ModuleLicense moduleLicense = mapperMap.get(moduleType).toEntity(moduleLicenseDTO);
    moduleLicense.setId(moduleLicenseDTO.getId());
    moduleLicense.setAccountIdentifier(moduleLicenseDTO.getAccountIdentifier());
    moduleLicense.setModuleType(moduleLicenseDTO.getModuleType());
    moduleLicense.setEdition(moduleLicenseDTO.getEdition());
    moduleLicense.setLicenseType(moduleLicenseDTO.getLicenseType());
    moduleLicense.setStatus(moduleLicenseDTO.getStatus());
    moduleLicense.setStartTime(moduleLicenseDTO.getStartTime());
    moduleLicense.setExpiryTime(moduleLicenseDTO.getExpiryTime());
    return (T) moduleLicense;
  }
}

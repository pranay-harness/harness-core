/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.licensing.mappers.modules;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.licensing.beans.modules.CIModuleLicenseDTO;
import io.harness.licensing.entities.modules.CIModuleLicense;
import io.harness.licensing.mappers.LicenseObjectMapper;

import com.google.inject.Singleton;

@OwnedBy(HarnessTeam.GTM)
@Singleton
public class CILicenseObjectMapper implements LicenseObjectMapper<CIModuleLicense, CIModuleLicenseDTO> {
  @Override
  public CIModuleLicenseDTO toDTO(CIModuleLicense entity) {
    return CIModuleLicenseDTO.builder().numberOfCommitters(entity.getNumberOfCommitters()).build();
  }

  @Override
  public CIModuleLicense toEntity(CIModuleLicenseDTO dto) {
    return CIModuleLicense.builder().numberOfCommitters(dto.getNumberOfCommitters()).build();
  }
}

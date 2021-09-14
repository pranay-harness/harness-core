/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.licensing.mappers.modules;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.licensing.beans.modules.CVModuleLicenseDTO;
import io.harness.licensing.entities.modules.CVModuleLicense;
import io.harness.licensing.mappers.LicenseObjectMapper;

import com.google.inject.Singleton;

@OwnedBy(HarnessTeam.GTM)
@Singleton
public class CVLicenseObjectMapper implements LicenseObjectMapper<CVModuleLicense, CVModuleLicenseDTO> {
  @Override
  public CVModuleLicenseDTO toDTO(CVModuleLicense moduleLicense) {
    return CVModuleLicenseDTO.builder().build();
  }

  @Override
  public CVModuleLicense toEntity(CVModuleLicenseDTO moduleLicenseDTO) {
    return CVModuleLicense.builder().build();
  }
}

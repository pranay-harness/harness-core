/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.licensing.mappers.modules;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.licensing.beans.modules.CDModuleLicenseDTO;
import io.harness.licensing.beans.modules.types.CDLicenseType;
import io.harness.licensing.entities.modules.CDModuleLicense;
import io.harness.licensing.mappers.LicenseObjectMapper;

import com.google.inject.Singleton;

@OwnedBy(HarnessTeam.GTM)
@Singleton
public class CDLicenseObjectMapper implements LicenseObjectMapper<CDModuleLicense, CDModuleLicenseDTO> {
  @Override
  public CDModuleLicenseDTO toDTO(CDModuleLicense entity) {
    CDModuleLicenseDTO dto = CDModuleLicenseDTO.builder().build();
    if (CDLicenseType.SERVICE_INSTANCES.equals(entity.getCdLicenseType())) {
      dto.setCdLicenseType(CDLicenseType.SERVICE_INSTANCES);
      dto.setServiceInstances(entity.getServiceInstances());
    } else {
      dto.setCdLicenseType(CDLicenseType.SERVICES);
      dto.setWorkloads(entity.getWorkloads());
    }
    return dto;
  }

  @Override
  public CDModuleLicense toEntity(CDModuleLicenseDTO dto) {
    CDModuleLicense entity = CDModuleLicense.builder().build();
    if (CDLicenseType.SERVICE_INSTANCES.equals(dto.getCdLicenseType())) {
      entity.setCdLicenseType(CDLicenseType.SERVICE_INSTANCES);
      entity.setServiceInstances(dto.getServiceInstances());
    } else {
      entity.setCdLicenseType(CDLicenseType.SERVICES);
      entity.setWorkloads(dto.getWorkloads());
    }
    return entity;
  }
}

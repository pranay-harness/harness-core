/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.licensing.mappers.modules;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.licensing.beans.modules.CDModuleLicenseDTO;
import io.harness.licensing.entities.modules.CDModuleLicense;
import io.harness.licensing.mappers.LicenseObjectMapper;

import com.google.inject.Singleton;

@OwnedBy(HarnessTeam.GTM)
@Singleton
public class CDLicenseObjectMapper implements LicenseObjectMapper<CDModuleLicense, CDModuleLicenseDTO> {
  @Override
  public CDModuleLicenseDTO toDTO(CDModuleLicense entity) {
    CDModuleLicenseDTO dto = CDModuleLicenseDTO.builder().build();
    dto.setCdLicenseType(entity.getCdLicenseType());
    dto.setServiceInstances(entity.getServiceInstances());
    dto.setWorkloads(entity.getWorkloads());
    return dto;
  }

  @Override
  public CDModuleLicense toEntity(CDModuleLicenseDTO dto) {
    CDModuleLicense entity = CDModuleLicense.builder().build();
    entity.setCdLicenseType(dto.getCdLicenseType());
    entity.setServiceInstances(dto.getServiceInstances());
    entity.setWorkloads(dto.getWorkloads());
    return entity;
  }
}

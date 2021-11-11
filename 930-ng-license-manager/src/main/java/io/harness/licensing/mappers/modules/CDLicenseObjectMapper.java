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

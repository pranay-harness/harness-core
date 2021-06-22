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
    return CDModuleLicenseDTO.builder()
        .maxWorkLoads(entity.getMaxWorkLoads())
        .totalWorkLoads(entity.getTotalWorkLoads())
        .deploymentsPerDay(entity.getDeploymentsPerDay())
        .build();
  }

  @Override
  public CDModuleLicense toEntity(CDModuleLicenseDTO dto) {
    return CDModuleLicense.builder()
        .maxWorkLoads(dto.getMaxWorkLoads())
        .totalWorkLoads(dto.getTotalWorkLoads())
        .deploymentsPerDay(dto.getDeploymentsPerDay())
        .build();
  }
}

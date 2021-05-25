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
    return CIModuleLicenseDTO.builder()
        .maxDevelopers(entity.getMaxDevelopers())
        .totalDevelopers(entity.getTotalDevelopers())
        .build();
  }

  @Override
  public CIModuleLicense toEntity(CIModuleLicenseDTO dto) {
    return CIModuleLicense.builder()
        .totalDevelopers(dto.getTotalDevelopers())
        .maxDevelopers(dto.getMaxDevelopers())
        .build();
  }
}

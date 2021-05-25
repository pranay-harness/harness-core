package io.harness.licensing.mappers.modules;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.licensing.beans.modules.CFModuleLicenseDTO;
import io.harness.licensing.entities.modules.CFModuleLicense;
import io.harness.licensing.mappers.LicenseObjectMapper;

import com.google.inject.Singleton;

@OwnedBy(HarnessTeam.GTM)
@Singleton
public class CFLicenseObjectMapper implements LicenseObjectMapper<CFModuleLicense, CFModuleLicenseDTO> {
  @Override
  public CFModuleLicenseDTO toDTO(CFModuleLicense entity) {
    return CFModuleLicenseDTO.builder()
        .totalFeatureFlagUnit(entity.getTotalFeatureFlagUnit())
        .maxFeatureFlagUnit(entity.getMaxFeatureFlagUnit())
        .totalClientMAUs(entity.getTotalClientMAUs())
        .maxClientMAUs(entity.getMaxClientMAUs())
        .build();
  }

  @Override
  public CFModuleLicense toEntity(CFModuleLicenseDTO dto) {
    return CFModuleLicense.builder()
        .totalClientMAUs(dto.getTotalClientMAUs())
        .maxClientMAUs(dto.getMaxClientMAUs())
        .totalFeatureFlagUnit(dto.getTotalFeatureFlagUnit())
        .maxFeatureFlagUnit(dto.getMaxFeatureFlagUnit())
        .build();
  }
}

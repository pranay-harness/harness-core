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
        .numberOfUsers(entity.getNumberOfUsers())
        .numberOfClientMAUs(entity.getNumberOfClientMAUs())
        .build();
  }

  @Override
  public CFModuleLicense toEntity(CFModuleLicenseDTO dto) {
    return CFModuleLicense.builder()
        .numberOfClientMAUs(dto.getNumberOfClientMAUs())
        .numberOfUsers(dto.getNumberOfUsers())
        .build();
  }
}

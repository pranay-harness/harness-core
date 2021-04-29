package io.harness.licensing.mappers.modules;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.licensing.beans.modules.CFModuleLicenseDTO;
import io.harness.licensing.beans.modules.ModuleLicenseDTO;
import io.harness.licensing.entities.modules.CFModuleLicense;
import io.harness.licensing.entities.modules.ModuleLicense;
import io.harness.licensing.mappers.LicenseObjectMapper;

@OwnedBy(HarnessTeam.GTM)
public class CFLicenseObjectMapper implements LicenseObjectMapper {
  @Override
  public ModuleLicenseDTO toDTO(ModuleLicense moduleLicense) {
    CFModuleLicense entity = (CFModuleLicense) moduleLicense;
    return CFModuleLicenseDTO.builder()
        .numberOfUsers(entity.getNumberOfUsers())
        .numberOfClientMAUs(entity.getNumberOfClientMAUs())
        .updateChannels(entity.getUpdateChannels())
        .build();
  }

  @Override
  public ModuleLicense toEntity(ModuleLicenseDTO moduleLicenseDTO) {
    CFModuleLicenseDTO dto = (CFModuleLicenseDTO) moduleLicenseDTO;
    return CFModuleLicense.builder()
        .numberOfClientMAUs(dto.getNumberOfClientMAUs())
        .numberOfUsers(dto.getNumberOfUsers())
        .updateChannels(dto.getUpdateChannels())
        .build();
  }
}

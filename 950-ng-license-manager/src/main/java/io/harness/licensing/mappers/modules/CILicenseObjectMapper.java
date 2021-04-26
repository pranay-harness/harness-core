package io.harness.licensing.mappers.modules;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.licensing.beans.modules.CIModuleLicenseDTO;
import io.harness.licensing.beans.modules.ModuleLicenseDTO;
import io.harness.licensing.entities.modules.CIModuleLicense;
import io.harness.licensing.entities.modules.ModuleLicense;
import io.harness.licensing.mappers.LicenseObjectMapper;

@OwnedBy(HarnessTeam.GTM)
public class CILicenseObjectMapper implements LicenseObjectMapper {
  @Override
  public ModuleLicenseDTO toDTO(ModuleLicense moduleLicense) {
    CIModuleLicense entity = (CIModuleLicense) moduleLicense;

    return CIModuleLicenseDTO.builder().numberOfCommitters(entity.getNumberOfCommitters()).build();
  }

  @Override
  public ModuleLicense toEntity(ModuleLicenseDTO moduleLicenseDTO) {
    CIModuleLicenseDTO dto = (CIModuleLicenseDTO) moduleLicenseDTO;
    return CIModuleLicense.builder().numberOfCommitters(dto.getNumberOfCommitters()).build();
  }
}

package io.harness.licensing.mappers.modules;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.licensing.beans.modules.CEModuleLicenseDTO;
import io.harness.licensing.beans.modules.ModuleLicenseDTO;
import io.harness.licensing.entities.modules.CEModuleLicense;
import io.harness.licensing.entities.modules.ModuleLicense;
import io.harness.licensing.mappers.LicenseObjectMapper;

@OwnedBy(HarnessTeam.GTM)
public class CELicenseObjectMapper implements LicenseObjectMapper {
  @Override
  public ModuleLicenseDTO toDTO(ModuleLicense moduleLicense) {
    return CEModuleLicenseDTO.builder().build();
  }

  @Override
  public ModuleLicense toEntity(ModuleLicenseDTO moduleLicenseDTO) {
    return CEModuleLicense.builder().build();
  }
}

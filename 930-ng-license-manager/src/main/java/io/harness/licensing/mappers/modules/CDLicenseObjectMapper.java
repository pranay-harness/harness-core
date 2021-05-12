package io.harness.licensing.mappers.modules;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.licensing.beans.modules.CDModuleLicenseDTO;
import io.harness.licensing.beans.modules.ModuleLicenseDTO;
import io.harness.licensing.entities.modules.CDModuleLicense;
import io.harness.licensing.entities.modules.ModuleLicense;
import io.harness.licensing.mappers.LicenseObjectMapper;

@OwnedBy(HarnessTeam.GTM)
public class CDLicenseObjectMapper implements LicenseObjectMapper {
  @Override
  public ModuleLicenseDTO toDTO(ModuleLicense moduleLicense) {
    CDModuleLicense entity = (CDModuleLicense) moduleLicense;
    return CDModuleLicenseDTO.builder().deploymentUnits(entity.getDeploymentUnits()).build();
  }

  @Override
  public ModuleLicense toEntity(ModuleLicenseDTO moduleLicenseDTO) {
    CDModuleLicenseDTO dto = (CDModuleLicenseDTO) moduleLicenseDTO;
    return CDModuleLicense.builder().deploymentUnits(dto.getDeploymentUnits()).build();
  }
}

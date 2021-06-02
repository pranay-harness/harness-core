package io.harness.licensing.mappers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.licensing.beans.modules.ModuleLicenseDTO;
import io.harness.licensing.entities.modules.ModuleLicense;

@OwnedBy(HarnessTeam.GTM)
public interface LicenseObjectMapper {
  ModuleLicenseDTO toDTO(ModuleLicense moduleLicense);

  ModuleLicense toEntity(ModuleLicenseDTO moduleLicenseDTO);
}

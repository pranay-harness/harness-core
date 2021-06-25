package io.harness.licensing.mappers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.licensing.beans.modules.ModuleLicenseDTO;
import io.harness.licensing.entities.modules.ModuleLicense;

@OwnedBy(HarnessTeam.GTM)
public interface LicenseObjectMapper<T extends ModuleLicense, K extends ModuleLicenseDTO> {
  K toDTO(T moduleLicense);

  T toEntity(K moduleLicenseDTO);
}

package io.harness.licensing.interfaces.clients;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.licensing.Edition;
import io.harness.licensing.beans.modules.CVModuleLicenseDTO;

@OwnedBy(HarnessTeam.GTM)
public interface CVModuleLicenseClient extends ModuleLicenseClient<CVModuleLicenseDTO> {
  @Override CVModuleLicenseDTO createTrialLicense(Edition edition, String accountId);
}

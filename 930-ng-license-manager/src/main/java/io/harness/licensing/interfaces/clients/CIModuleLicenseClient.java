package io.harness.licensing.interfaces.clients;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.licensing.Edition;
import io.harness.licensing.beans.modules.CIModuleLicenseDTO;

@OwnedBy(HarnessTeam.GTM)
public interface CIModuleLicenseClient extends ModuleLicenseClient<CIModuleLicenseDTO> {
  @Override CIModuleLicenseDTO createTrialLicense(Edition edition, String accountId);
}

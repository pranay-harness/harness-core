package io.harness.licensing.interfaces.clients;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.licensing.Edition;
import io.harness.licensing.beans.modules.ModuleLicenseDTO;

@OwnedBy(HarnessTeam.GTM)
public interface ModuleLicenseClient<T extends ModuleLicenseDTO> {
  T createTrialLicense(Edition edition, String accountId);
}

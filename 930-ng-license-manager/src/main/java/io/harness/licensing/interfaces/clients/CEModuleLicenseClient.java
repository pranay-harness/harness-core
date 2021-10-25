package io.harness.licensing.interfaces.clients;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.licensing.Edition;
import io.harness.licensing.beans.modules.CEModuleLicenseDTO;

@OwnedBy(HarnessTeam.GTM)
public interface CEModuleLicenseClient extends ModuleLicenseClient<CEModuleLicenseDTO> {
  @Override CEModuleLicenseDTO createTrialLicense(Edition edition, String accountId);
}

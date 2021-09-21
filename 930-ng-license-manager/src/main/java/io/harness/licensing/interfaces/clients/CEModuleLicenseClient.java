package io.harness.licensing.interfaces.clients;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.licensing.Edition;
import io.harness.licensing.beans.modules.CEModuleLicenseDTO;
import io.harness.licensing.beans.stats.CERuntimeUsageDTO;

@OwnedBy(HarnessTeam.GTM)
public interface CEModuleLicenseClient extends ModuleLicenseClient<CEModuleLicenseDTO, CERuntimeUsageDTO> {
  @Override CEModuleLicenseDTO createTrialLicense(Edition edition, String accountId);

  @Override CERuntimeUsageDTO getRuntimeUsage(String accountId);
}

package io.harness.licensing.interfaces.clients;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.licensing.Edition;
import io.harness.licensing.beans.modules.CFModuleLicenseDTO;
import io.harness.licensing.beans.stats.CFRuntimUsageDTO;

@OwnedBy(HarnessTeam.GTM)
public interface CFModuleLicenseClient extends ModuleLicenseClient<CFModuleLicenseDTO, CFRuntimUsageDTO> {
  @Override CFModuleLicenseDTO createTrialLicense(Edition edition, String accountId);

  @Override CFRuntimUsageDTO getRuntimeUsage(String accountId);
}

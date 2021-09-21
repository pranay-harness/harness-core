package io.harness.licensing.interfaces.clients;

import io.harness.licensing.Edition;
import io.harness.licensing.beans.modules.CDModuleLicenseDTO;
import io.harness.licensing.beans.stats.CDRuntimeUsageDTO;

public interface CDModuleLicenseClient extends ModuleLicenseClient<CDModuleLicenseDTO, CDRuntimeUsageDTO> {
  @Override CDModuleLicenseDTO createTrialLicense(Edition edition, String accountId);

  @Override CDRuntimeUsageDTO getRuntimeUsage(String accountId);
}

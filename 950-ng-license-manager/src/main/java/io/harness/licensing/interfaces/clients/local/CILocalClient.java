package io.harness.licensing.interfaces.clients.local;

import io.harness.licensing.Edition;
import io.harness.licensing.LicenseType;
import io.harness.licensing.beans.modules.CIModuleLicenseDTO;
import io.harness.licensing.beans.stats.CIRuntimeUsageDTO;
import io.harness.licensing.interfaces.clients.CIModuleLicenseClient;

public class CILocalClient implements CIModuleLicenseClient {
  @Override
  public CIModuleLicenseDTO createTrialLicense(Edition edition, String accountId, LicenseType licenseType) {
    // TODO: check the trial license setting for CI
    return CIModuleLicenseDTO.builder().numberOfCommitters(10).build();
  }

  @Override
  public CIRuntimeUsageDTO getRuntimeUsage(String accountId) {
    return null;
  }
}

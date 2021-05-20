package io.harness.licensing.interfaces.clients.local;

import io.harness.licensing.Edition;
import io.harness.licensing.LicenseType;
import io.harness.licensing.beans.modules.CDModuleLicenseDTO;
import io.harness.licensing.beans.stats.CDRuntimeUsageDTO;
import io.harness.licensing.interfaces.clients.CDModuleLicenseClient;

public class CDLocalClient implements CDModuleLicenseClient {
  @Override
  public CDModuleLicenseDTO createTrialLicense(Edition edition, String accountId, LicenseType licenseType) {
    return CDModuleLicenseDTO.builder().maxWorkLoads(5).deploymentsPerDay(10).build();
  }

  @Override
  public CDRuntimeUsageDTO getRuntimeUsage(String accountId) {
    return null;
  }
}

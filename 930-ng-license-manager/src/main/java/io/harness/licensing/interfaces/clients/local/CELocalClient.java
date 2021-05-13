package io.harness.licensing.interfaces.clients.local;

import io.harness.licensing.Edition;
import io.harness.licensing.LicenseType;
import io.harness.licensing.beans.modules.CEModuleLicenseDTO;
import io.harness.licensing.beans.stats.CERuntimeUsageDTO;
import io.harness.licensing.interfaces.clients.CEModuleLicenseClient;

public class CELocalClient implements CEModuleLicenseClient {
  @Override
  public CEModuleLicenseDTO createTrialLicense(Edition edition, String accountId, LicenseType licenseType) {
    return CEModuleLicenseDTO.builder().numberOfCluster(2).spendLimit(250000).dataRetentionInDays(30).build();
  }

  @Override
  public CERuntimeUsageDTO getRuntimeUsage(String accountId) {
    return null;
  }
}

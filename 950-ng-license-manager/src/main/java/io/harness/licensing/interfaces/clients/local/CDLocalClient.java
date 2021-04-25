package io.harness.licensing.interfaces.clients.local;

import io.harness.licensing.DeploymentUnitType;
import io.harness.licensing.Edition;
import io.harness.licensing.LicenseType;
import io.harness.licensing.beans.modules.CDModuleLicenseDTO;
import io.harness.licensing.beans.stats.CDRuntimeUsageDTO;
import io.harness.licensing.interfaces.clients.CDModuleLicenseClient;

import com.google.common.collect.ImmutableMap;

public class CDLocalClient implements CDModuleLicenseClient {
  @Override
  public CDModuleLicenseDTO createTrialLicense(Edition edition, String accountId, LicenseType licenseType) {
    // TODO: check the trial license setting for CD
    return CDModuleLicenseDTO.builder()
        .deploymentUnits(ImmutableMap.of(DeploymentUnitType.SERVICE, 10, DeploymentUnitType.FUNCTION, 10))
        .build();
  }

  @Override
  public CDRuntimeUsageDTO getRuntimeUsage(String accountId) {
    return null;
  }
}

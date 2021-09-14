/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.licensing.interfaces.clients;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.licensing.Edition;
import io.harness.licensing.LicenseType;
import io.harness.licensing.beans.modules.CIModuleLicenseDTO;
import io.harness.licensing.beans.stats.CIRuntimeUsageDTO;

@OwnedBy(HarnessTeam.GTM)
public interface CIModuleLicenseClient extends ModuleLicenseClient<CIModuleLicenseDTO, CIRuntimeUsageDTO> {
  @Override CIModuleLicenseDTO createTrialLicense(Edition edition, String accountId, LicenseType licenseType);

  @Override CIRuntimeUsageDTO getRuntimeUsage(String accountId);
}

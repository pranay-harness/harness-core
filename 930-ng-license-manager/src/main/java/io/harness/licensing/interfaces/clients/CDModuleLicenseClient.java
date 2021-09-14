/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.licensing.interfaces.clients;

import io.harness.licensing.Edition;
import io.harness.licensing.LicenseType;
import io.harness.licensing.beans.modules.CDModuleLicenseDTO;
import io.harness.licensing.beans.stats.CDRuntimeUsageDTO;

public interface CDModuleLicenseClient extends ModuleLicenseClient<CDModuleLicenseDTO, CDRuntimeUsageDTO> {
  @Override CDModuleLicenseDTO createTrialLicense(Edition edition, String accountId, LicenseType licenseType);

  @Override CDRuntimeUsageDTO getRuntimeUsage(String accountId);
}

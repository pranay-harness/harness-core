/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.licensing.interfaces;

import io.harness.ModuleType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.licensing.Edition;
import io.harness.licensing.LicenseType;
import io.harness.licensing.beans.modules.ModuleLicenseDTO;

@OwnedBy(HarnessTeam.GTM)
public interface ModuleLicenseInterface {
  ModuleLicenseDTO generateTrialLicense(
      Edition edition, String accountId, LicenseType licenseType, ModuleType moduleType);
}

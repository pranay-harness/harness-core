package io.harness.licensing.interfaces;

import io.harness.ModuleType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.licensing.Edition;
import io.harness.licensing.beans.modules.ModuleLicenseDTO;

@OwnedBy(HarnessTeam.GTM)
public interface ModuleLicenseInterface {
  ModuleLicenseDTO generateFreeLicense(String accountId, ModuleType moduleType);
  ModuleLicenseDTO generateTrialLicense(Edition edition, String accountId, ModuleType moduleType);
}

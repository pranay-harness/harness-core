package io.harness.licensing.services;

import io.harness.licensing.ModuleType;
import io.harness.licensing.beans.modules.ModuleLicenseDTO;

public interface LicenseService extends LicenseCrudService {
  ModuleLicenseDTO startTrialLicense(String accountIdentifier, ModuleType moduleType);
}

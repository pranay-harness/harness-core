package io.harness.licensing.services;

import io.harness.ModuleType;
import io.harness.licensing.beans.modules.ModuleLicenseDTO;
import io.harness.licensing.beans.modules.StartTrialDTO;
import io.harness.licensing.beans.response.CheckExpiryResultDTO;

public interface LicenseService extends LicenseCrudService {
  ModuleLicenseDTO startFreeLicense(String accountIdentifier, ModuleType moduleType);
  ModuleLicenseDTO startTrialLicense(String accountIdentifier, StartTrialDTO startTrialRequestDTO);
  ModuleLicenseDTO extendTrialLicense(String accountIdentifier, StartTrialDTO startTrialRequestDTO);
  CheckExpiryResultDTO checkExpiry(String accountIdentifier);
  void softDelete(String accountIdentifier);
}

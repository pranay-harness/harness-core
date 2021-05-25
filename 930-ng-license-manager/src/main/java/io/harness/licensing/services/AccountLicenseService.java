package io.harness.licensing.services;

import io.harness.licensing.beans.modules.ModuleLicenseDTO;
import io.harness.licensing.beans.modules.StartTrialRequestDTO;

public interface AccountLicenseService extends AccountLicenseCrudService {
  ModuleLicenseDTO startTrialLicense(String accountIdentifier, StartTrialRequestDTO startTrialRequestDTO);
  ModuleLicenseDTO extendTrialLicense(String accountIdentifier, StartTrialRequestDTO startTrialRequestDTO);
  boolean shouldRemoveAccount(String accountIdentifier);
  void softDelete(String accountIdentifier);
}

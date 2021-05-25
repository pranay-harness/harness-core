package io.harness.licensing.services;

import io.harness.licensing.beans.modules.ModuleLicenseDTO;
import io.harness.licensing.beans.modules.StartTrialRequestDTO;

@Deprecated
public interface LicenseService extends LicenseCrudService {
  ModuleLicenseDTO startTrialLicense(String accountIdentifier, StartTrialRequestDTO startTrialRequestDTO);

  boolean checkNGLicensesAllInactive(String accountIdentifier);

  void softDelete(String accountIdentifier);
}

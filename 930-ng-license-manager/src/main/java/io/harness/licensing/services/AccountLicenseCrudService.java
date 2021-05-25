package io.harness.licensing.services;

import io.harness.licensing.ModuleType;
import io.harness.licensing.beans.modules.AccountLicenseDTO;
import io.harness.licensing.beans.modules.ModuleLicenseDTO;

public interface AccountLicenseCrudService {
  ModuleLicenseDTO getModuleLicense(String accountId, ModuleType moduleType);
  AccountLicenseDTO getAccountLicense(String accountIdentifier);
  AccountLicenseDTO getAccountLicenseById(String identifier);
  AccountLicenseDTO createAccountLicense(AccountLicenseDTO moduleLicense);
  AccountLicenseDTO updateAccountLicense(AccountLicenseDTO moduleLicense);
}

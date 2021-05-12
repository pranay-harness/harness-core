package io.harness.licensing.services;

import io.harness.licensing.ModuleType;
import io.harness.licensing.beans.modules.AccountLicensesDTO;
import io.harness.licensing.beans.modules.ModuleLicenseDTO;

public interface LicenseCrudService {
  ModuleLicenseDTO getModuleLicense(String accountId, ModuleType moduleType);
  AccountLicensesDTO getAccountLicense(String accountIdentifier);
  ModuleLicenseDTO getModuleLicenseById(String identifier);
  ModuleLicenseDTO createModuleLicense(ModuleLicenseDTO moduleLicense);
  ModuleLicenseDTO updateModuleLicense(ModuleLicenseDTO moduleLicense);
  ModuleLicenseDTO deleteModuleLicense(String identifier, String accountIdentifier);
}

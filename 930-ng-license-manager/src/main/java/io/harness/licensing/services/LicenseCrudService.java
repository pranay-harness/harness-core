/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.licensing.services;

import io.harness.ModuleType;
import io.harness.licensing.beans.modules.AccountLicenseDTO;
import io.harness.licensing.beans.modules.ModuleLicenseDTO;

import java.util.List;

public interface LicenseCrudService {
  ModuleLicenseDTO getModuleLicense(String accountId, ModuleType moduleType);
  List<ModuleLicenseDTO> getModuleLicenses(String accountIdentifier, ModuleType moduleType);
  AccountLicenseDTO getAccountLicense(String accountIdentifier);
  ModuleLicenseDTO getModuleLicenseById(String identifier);
  ModuleLicenseDTO createModuleLicense(ModuleLicenseDTO moduleLicense);
  ModuleLicenseDTO updateModuleLicense(ModuleLicenseDTO moduleLicense);
}

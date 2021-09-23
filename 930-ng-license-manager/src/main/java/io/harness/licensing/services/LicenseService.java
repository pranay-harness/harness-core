package io.harness.licensing.services;

import io.harness.ModuleType;
import io.harness.licensing.Edition;
import io.harness.licensing.beans.modules.ModuleLicenseDTO;
import io.harness.licensing.beans.modules.StartTrialDTO;
import io.harness.licensing.beans.response.CheckExpiryResultDTO;
import io.harness.licensing.beans.summary.LicensesWithSummaryDTO;

public interface LicenseService extends LicenseCrudService {
  ModuleLicenseDTO startFreeLicense(String accountIdentifier, ModuleType moduleType);
  ModuleLicenseDTO startTrialLicense(String accountIdentifier, StartTrialDTO startTrialRequestDTO);
  ModuleLicenseDTO extendTrialLicense(String accountIdentifier, StartTrialDTO startTrialRequestDTO);
  CheckExpiryResultDTO checkExpiry(String accountIdentifier);
  void softDelete(String accountIdentifier);
  LicensesWithSummaryDTO getLicenseSummary(String accountIdentifier, ModuleType moduleType);
  Edition calculateAccountEdition(String accountIdentifier);
  LicensesWithSummaryDTO getAccountLicenseSummary(String accountIdentifier);
}

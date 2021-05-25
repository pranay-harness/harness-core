package io.harness.licensing.scheduler.modules;

import io.harness.licensing.Edition;
import io.harness.licensing.LicenseStatus;
import io.harness.licensing.entities.modules.CEModuleLicense;
import io.harness.licensing.entities.modules.ModuleLicense;
import io.harness.licensing.entities.transactions.LicenseTransaction;
import io.harness.licensing.entities.transactions.modules.CELicenseTransaction;
import io.harness.licensing.scheduler.CheckResult;
import io.harness.licensing.scheduler.LicenseCheckProcessor;

import com.google.inject.Singleton;

@Singleton
public class CECheckProcessor implements LicenseCheckProcessor {
  @Override
  public CheckResult checkExpiry(String uuid, long currentTime, ModuleLicense moduleLicense) {
    CEModuleLicense license = (CEModuleLicense) moduleLicense;
    if (Edition.FREE.equals(license.getEdition())) {
      // free edition module license is always active and skip subscription check
      return CheckResult.builder().allInactive(true).isUpdated(false).build();
    }

    // Transaction check
    CheckResult checkResult = CheckResult.builder().allInactive(true).isUpdated(false).build();
    for (LicenseTransaction l : license.getTransactions()) {
      CELicenseTransaction transaction = (CELicenseTransaction) l;
      // transaction expiry check
      if (transaction.checkExpiry(currentTime)) {
        if (transaction.isActive()) {
          // Expire transaction if still active
          transaction.setStatus(LicenseStatus.EXPIRED);
          checkResult.setUpdated(true);
        }
      } else {
        // In case Active transaction
        checkResult.setAllInactive(false);
        // aggregation count
      }
    }

    // update aggregation info

    return checkResult;
  }
}

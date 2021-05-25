package io.harness.licensing.scheduler.modules;

import io.harness.licensing.Edition;
import io.harness.licensing.LicenseStatus;
import io.harness.licensing.entities.modules.CIModuleLicense;
import io.harness.licensing.entities.modules.ModuleLicense;
import io.harness.licensing.entities.transactions.LicenseTransaction;
import io.harness.licensing.entities.transactions.modules.CILicenseTransaction;
import io.harness.licensing.scheduler.CheckResult;
import io.harness.licensing.scheduler.LicenseCheckProcessor;
import io.harness.licensing.scheduler.utils.LicenseCheckProcessorUtils;

import com.google.inject.Singleton;

@Singleton
public class CICheckProcessor implements LicenseCheckProcessor {
  @Override
  public CheckResult checkExpiry(String uuid, long currentTime, ModuleLicense moduleLicense) {
    CIModuleLicense license = (CIModuleLicense) moduleLicense;
    if (Edition.FREE.equals(license.getEdition())) {
      // free edition module license is always active and skip subscription check
      return CheckResult.builder().allInactive(true).isUpdated(false).build();
    }

    // Transaction check
    CheckResult checkResult = CheckResult.builder().allInactive(true).isUpdated(false).build();
    int totalDevelopers = 0;
    for (LicenseTransaction l : license.getTransactions()) {
      CILicenseTransaction transaction = (CILicenseTransaction) l;
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
        totalDevelopers = LicenseCheckProcessorUtils.computeAdd(totalDevelopers, transaction.getDevelopers());
      }
    }

    // update aggregation info
    if (totalDevelopers != license.getTotalDevelopers().intValue()) {
      license.setTotalDevelopers(totalDevelopers);
      checkResult.setUpdated(true);
    }
    return checkResult;
  }
}

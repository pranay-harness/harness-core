package io.harness.licensing.scheduler.modules;

import io.harness.licensing.Edition;
import io.harness.licensing.LicenseStatus;
import io.harness.licensing.entities.modules.CFModuleLicense;
import io.harness.licensing.entities.modules.ModuleLicense;
import io.harness.licensing.entities.transactions.LicenseTransaction;
import io.harness.licensing.entities.transactions.modules.CFLicenseTransaction;
import io.harness.licensing.scheduler.CheckResult;
import io.harness.licensing.scheduler.LicenseCheckProcessor;
import io.harness.licensing.scheduler.utils.LicenseCheckProcessorUtils;

import com.google.inject.Singleton;

@Singleton
public class CFCheckProcessor implements LicenseCheckProcessor {
  @Override
  public CheckResult checkExpiry(String uuid, long currentTime, ModuleLicense moduleLicense) {
    CFModuleLicense license = (CFModuleLicense) moduleLicense;
    if (Edition.FREE.equals(license.getEdition())) {
      // free edition module license is always active and skip subscription check
      return CheckResult.builder().allInactive(true).isUpdated(false).build();
    }

    // Transaction check
    CheckResult checkResult = CheckResult.builder().allInactive(true).isUpdated(false).build();
    int totalFeatureFlagUnit = 0;
    long totalClientMAUs = 0;
    for (LicenseTransaction l : license.getTransactions()) {
      CFLicenseTransaction transaction = (CFLicenseTransaction) l;
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
        totalClientMAUs = LicenseCheckProcessorUtils.computeAdd(totalClientMAUs, transaction.getClientMAU());
        totalFeatureFlagUnit =
            LicenseCheckProcessorUtils.computeAdd(totalFeatureFlagUnit, transaction.getFeatureFlagUnit());
      }
    }

    // update aggregation info
    if (totalClientMAUs != license.getTotalClientMAUs().intValue()
        || totalFeatureFlagUnit != license.getTotalFeatureFlagUnit().longValue()) {
      license.setTotalClientMAUs(totalClientMAUs);
      license.setTotalFeatureFlagUnit(totalFeatureFlagUnit);
      checkResult.setUpdated(true);
    }

    return checkResult;
  }
}

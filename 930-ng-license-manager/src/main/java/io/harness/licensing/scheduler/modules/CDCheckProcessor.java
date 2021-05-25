package io.harness.licensing.scheduler.modules;

import io.harness.licensing.Edition;
import io.harness.licensing.LicenseStatus;
import io.harness.licensing.entities.modules.CDModuleLicense;
import io.harness.licensing.entities.modules.ModuleLicense;
import io.harness.licensing.entities.transactions.LicenseTransaction;
import io.harness.licensing.entities.transactions.modules.CDLicenseTransaction;
import io.harness.licensing.scheduler.CheckResult;
import io.harness.licensing.scheduler.LicenseCheckProcessor;
import io.harness.licensing.scheduler.utils.LicenseCheckProcessorUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AllArgsConstructor;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Singleton
public class CDCheckProcessor implements LicenseCheckProcessor {
  @Override
  public CheckResult checkExpiry(String uuid, long currentTime, ModuleLicense moduleLicense) {
    CDModuleLicense license = (CDModuleLicense) moduleLicense;
    if (Edition.FREE.equals(license.getEdition())) {
      // free edition module license is always active and skip subscription check
      return CheckResult.builder().allInactive(true).isUpdated(false).build();
    }

    // Transaction check
    CheckResult checkResult = CheckResult.builder().allInactive(true).isUpdated(false).build();
    int totalWorkLoad = 0;
    for (LicenseTransaction l : license.getTransactions()) {
      CDLicenseTransaction transaction = (CDLicenseTransaction) l;
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
        totalWorkLoad = LicenseCheckProcessorUtils.computeAdd(totalWorkLoad, transaction.getWorkload());
      }
    }

    // update aggregation info
    if (totalWorkLoad != license.getTotalWorkLoads()) {
      license.setTotalWorkLoads(totalWorkLoad);
      checkResult.setUpdated(true);
    }

    return checkResult;
  }
}

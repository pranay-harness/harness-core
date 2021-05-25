package io.harness.licensing.scheduler.modules;

import io.harness.licensing.entities.transactions.modules.CFLicenseTransaction;
import io.harness.licensing.scheduler.CheckResult;
import io.harness.licensing.scheduler.utils.LicenseCheckProcessorUtils;

import com.google.inject.Singleton;

@Singleton
public class CFAggregator implements TransactionAggregator<CheckResult, CFLicenseTransaction> {
  @Override
  public void processTransaction(CheckResult checkResult, CFLicenseTransaction licenseTransaction) {
    checkResult.setTotalClientMAU(
        LicenseCheckProcessorUtils.computeAdd(checkResult.getTotalClientMAU(), licenseTransaction.getClientMAU()));
    checkResult.setTotalFeatureFlagUnit(LicenseCheckProcessorUtils.computeAdd(
        checkResult.getTotalFeatureFlagUnit(), licenseTransaction.getFeatureFlagUnit()));
  }
}

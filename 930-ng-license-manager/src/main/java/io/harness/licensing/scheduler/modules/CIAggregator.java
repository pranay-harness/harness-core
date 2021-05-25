package io.harness.licensing.scheduler.modules;

import io.harness.licensing.entities.transactions.modules.CILicenseTransaction;
import io.harness.licensing.scheduler.CheckResult;
import io.harness.licensing.scheduler.utils.LicenseCheckProcessorUtils;

import com.google.inject.Singleton;

@Singleton
public class CIAggregator implements TransactionAggregator<CheckResult, CILicenseTransaction> {
  @Override
  public void processTransaction(CheckResult checkResult, CILicenseTransaction licenseTransaction) {
    checkResult.setTotalDevelopers(
        LicenseCheckProcessorUtils.computeAdd(checkResult.getTotalDevelopers(), licenseTransaction.getDevelopers()));
  }
}

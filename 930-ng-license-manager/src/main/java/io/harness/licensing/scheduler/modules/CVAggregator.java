package io.harness.licensing.scheduler.modules;

import io.harness.licensing.entities.transactions.modules.CILicenseTransaction;
import io.harness.licensing.scheduler.CheckResult;

import com.google.inject.Singleton;

@Singleton
public class CVAggregator implements TransactionAggregator<CheckResult, CILicenseTransaction> {
  @Override
  public void processTransaction(CheckResult checkResult, CILicenseTransaction licenseTransaction) {
    // add aggregation logic later
  }
}

package io.harness.licensing.scheduler.modules;

import io.harness.licensing.entities.transactions.modules.CDLicenseTransaction;
import io.harness.licensing.scheduler.CheckResult;

import com.google.inject.Singleton;

@Singleton
public class CEAggregator implements TransactionAggregator<CheckResult, CDLicenseTransaction> {
  @Override
  public void processTransaction(CheckResult checkResult, CDLicenseTransaction licenseTransaction) {
    // add aggregation logic later
  }
}

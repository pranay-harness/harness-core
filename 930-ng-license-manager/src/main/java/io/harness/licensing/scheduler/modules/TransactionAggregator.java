package io.harness.licensing.scheduler.modules;

import io.harness.licensing.entities.transactions.LicenseTransaction;
import io.harness.licensing.scheduler.CheckResult;

public interface TransactionAggregator<T extends CheckResult, Y extends LicenseTransaction> {
  void processTransaction(T checkResult, Y licenseTransaction);
}

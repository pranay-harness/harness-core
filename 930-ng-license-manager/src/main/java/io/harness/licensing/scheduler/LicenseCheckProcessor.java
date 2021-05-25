package io.harness.licensing.scheduler;

import io.harness.licensing.entities.modules.ModuleLicense;
import io.harness.licensing.scheduler.modules.TransactionAggregator;

public interface LicenseCheckProcessor {
  CheckResult checkExpiry(
      String uuid, long currentTime, ModuleLicense moduleLicense, TransactionAggregator transactionAggregate);
}

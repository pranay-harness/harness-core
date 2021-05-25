package io.harness.licensing.scheduler.modules;

import io.harness.licensing.entities.transactions.modules.CDLicenseTransaction;
import io.harness.licensing.scheduler.CheckResult;
import io.harness.licensing.scheduler.utils.LicenseCheckProcessorUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AllArgsConstructor;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Singleton
public class CDAggregator implements TransactionAggregator<CheckResult, CDLicenseTransaction> {
  @Override
  public void processTransaction(CheckResult checkResult, CDLicenseTransaction licenseTransaction) {
    checkResult.setTotalWorkload(
        LicenseCheckProcessorUtils.computeAdd(checkResult.getTotalWorkload(), licenseTransaction.getWorkload()));
  }
}

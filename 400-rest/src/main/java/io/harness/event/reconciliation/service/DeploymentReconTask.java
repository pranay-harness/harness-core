package io.harness.event.reconciliation.service;

import io.harness.event.reconciliation.deployment.ReconciliationStatus;

import software.wings.beans.Account;
import software.wings.service.intfc.AccountService;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DeploymentReconTask implements Runnable {
  @Inject DeploymentReconService deploymentReconService;
  @Inject AccountService accountService;
  /**
   * Fixed size threadPool to have max 5 threads only
   */
  @Inject @Named("DeploymentReconTaskExecutor") ExecutorService executorService;
  @Override
  public void run() {
    try {
      long startTime = System.currentTimeMillis();
      List<Account> accountList = accountService.listAllAccountWithDefaultsWithoutLicenseInfo();
      for (Account account : accountList) {
        executorService.submit(() -> {
          final long durationStartTs = startTime - 45 * 60 * 1000;
          final long durationEndTs = startTime - 5 * 60 * 1000;
          try {
            ReconciliationStatus reconciliationStatus =
                deploymentReconService.performReconciliation(account.getUuid(), durationStartTs, durationEndTs);
            log.info(
                "Completed reconciliation for accountID:[{}],accountName:[{}] durationStart:[{}],durationEnd:[{}],status:[{}]",
                account.getUuid(), account.getAccountName(), new Date(durationStartTs), new Date(durationEndTs),
                reconciliationStatus);
          } catch (Exception e) {
            log.error(
                "Error while performing reconciliation for accountID:[{}],accountName:[{}] durationStart:[{}],durationEnd:[{}]",
                account.getUuid(), account.getAccountName(), new Date(durationStartTs), new Date(durationEndTs), e);
          }
        });
      }
    } catch (Exception e) {
      log.error("Failed to run reconcilation", e);
    }
  }
}

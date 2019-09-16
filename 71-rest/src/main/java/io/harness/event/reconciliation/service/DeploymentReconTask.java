package io.harness.event.reconciliation.service;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.event.reconciliation.deployment.ReconciliationStatus;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Account;
import software.wings.service.intfc.AccountService;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;

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
            logger.info(
                "Completed reconciliation for accountID:[{}],accountName:[{}] durationStart:[{}],durationEnd:[{}],status:[{}]",
                account.getUuid(), account.getAccountName(), new Date(durationStartTs), new Date(durationEndTs),
                reconciliationStatus);
          } catch (Exception e) {
            logger.error(
                "Error while performing reconciliation for accountID:[{}],accountName:[{}] durationStart:[{}],durationEnd:[{}]",
                account.getUuid(), account.getAccountName(), new Date(durationStartTs), new Date(durationEndTs), e);
          }
        });
      }
    } catch (Exception e) {
      logger.error("Failed to run reconcilation", e);
    }
  }
}

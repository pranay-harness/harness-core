package io.harness.jobs.sg247.collection;

import com.google.inject.Inject;

import io.harness.mongo.iterator.MongoPersistenceIterator.Handler;
import io.harness.service.intfc.ContinuousVerificationService;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Account;

/**
 * This single cron collects data for both timeseries and log for
 * service guard 24X7 analysis for a specific account.
 */
@Slf4j
public class ServiceGuardDataCollectionJob implements Handler<Account> {
  @Inject private ContinuousVerificationService continuousVerificationService;

  @Override
  public void handle(Account account) {
    final String accountId = account.getUuid();
    logger.info("Executing APM & Logs Data collection for {}", accountId);
    long startTime = System.currentTimeMillis();
    continuousVerificationService.triggerAPMDataCollection(accountId);
    logger.info("[triggerAPMDataCollection] Total time taken to process accountId {} is {} (ms)", account,
        System.currentTimeMillis() - startTime);
    startTime = System.currentTimeMillis();
    continuousVerificationService.triggerLogDataCollection(accountId);
    logger.info("[triggerLogDataCollection] Total time taken to process accountId {} is {} (ms)", account,
        System.currentTimeMillis() - startTime);
  }
}

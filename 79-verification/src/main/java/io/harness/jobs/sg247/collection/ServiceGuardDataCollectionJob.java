package io.harness.jobs.sg247.collection;

import com.google.inject.Inject;

import io.harness.mongo.iterator.MongoPersistenceIterator.Handler;
import io.harness.service.intfc.ContinuousVerificationService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import software.wings.beans.Account;

/**
 * This single cron collects data for both timeseries and log for
 * service guard 24X7 analysis for a specific account.
 */
@Slf4j
public class ServiceGuardDataCollectionJob implements Job, Handler<Account> {
  public static final String SERVICE_GUARD_DATA_COLLECTION_CRON = "SERVICE_GUARD_DATA_COLLECTION_CRON";

  @Inject private ContinuousVerificationService continuousVerificationService;

  @Override
  public void execute(JobExecutionContext jobExecutionContext) {}

  @Override
  public void handle(Account account) {
    final String accountId = account.getUuid();
    logger.info("Executing APM & Logs Data collection for {}", accountId);
    continuousVerificationService.triggerAPMDataCollection(accountId);
    continuousVerificationService.triggerLogDataCollection(accountId);
  }
}

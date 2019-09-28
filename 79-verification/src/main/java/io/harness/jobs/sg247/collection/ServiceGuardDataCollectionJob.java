package io.harness.jobs.sg247.collection;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static software.wings.beans.FeatureName.MOVE_VERIFICATION_CRONS_TO_EXECUTORS;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import io.harness.managerclient.VerificationManagerClient;
import io.harness.managerclient.VerificationManagerClientHelper;
import io.harness.mongo.MongoPersistenceIterator.Handler;
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
  @Inject private VerificationManagerClientHelper verificationManagerClientHelper;
  @Inject private VerificationManagerClient verificationManagerClient;

  @Override
  public void execute(JobExecutionContext jobExecutionContext) {
    final String accountId = jobExecutionContext.getMergedJobDataMap().getString("accountId");
    Preconditions.checkState(isNotEmpty(accountId), "account Id not found for " + jobExecutionContext);

    if (!verificationManagerClientHelper
             .callManagerWithRetry(
                 verificationManagerClient.isFeatureEnabled(MOVE_VERIFICATION_CRONS_TO_EXECUTORS, accountId))
             .getResource()) {
      logger.info("Executing APM & Logs Data collector Job for {}", accountId);
      continuousVerificationService.triggerAPMDataCollection(accountId);
      continuousVerificationService.triggerLogDataCollection(accountId);
    } else {
      logger.info("for {} cron is disabled and data will be collected through ", accountId);
    }
  }

  @Override
  public void handle(Account account) {
    final String accountId = account.getUuid();
    if (!verificationManagerClientHelper
             .callManagerWithRetry(
                 verificationManagerClient.isFeatureEnabled(MOVE_VERIFICATION_CRONS_TO_EXECUTORS, accountId))
             .getResource()) {
      logger.info("for {} {} is disabled ", accountId, MOVE_VERIFICATION_CRONS_TO_EXECUTORS);
      return;
    }
    logger.info("Executing APM & Logs Data collection for {}", accountId);
    continuousVerificationService.triggerAPMDataCollection(accountId);
    continuousVerificationService.triggerLogDataCollection(accountId);
  }
}

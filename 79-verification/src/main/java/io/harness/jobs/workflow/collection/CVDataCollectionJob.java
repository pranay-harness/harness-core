package io.harness.jobs.workflow.collection;

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
import org.quartz.JobExecutionException;
import software.wings.beans.Account;

@Slf4j
public class CVDataCollectionJob implements Job, Handler<Account> {
  @Inject private ContinuousVerificationService continuousVerificationService;
  @Inject private VerificationManagerClientHelper verificationManagerClientHelper;
  @Inject private VerificationManagerClient verificationManagerClient;

  public static final String CV_TASK_CRON = "CV_TASK_CRON";
  @Override
  public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
    logger.info("Triggering Data collection job");

    final String accountId = jobExecutionContext.getMergedJobDataMap().getString("accountId");
    Preconditions.checkState(isNotEmpty(accountId), "account Id not found for " + jobExecutionContext);

    if (!verificationManagerClientHelper
             .callManagerWithRetry(
                 verificationManagerClient.isFeatureEnabled(MOVE_VERIFICATION_CRONS_TO_EXECUTORS, accountId))
             .getResource()) {
      logger.info("starting processing cv task for account id {}", accountId);
      continuousVerificationService.processNextCVTasks(accountId);
      continuousVerificationService.expireLongRunningCVTasks(accountId);
      continuousVerificationService.retryCVTasks(accountId);
    } else {
      logger.info("for {} cron is disabled and data will be processed through", accountId);
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

    logger.info("starting processing cv task for account id {}", accountId);
    continuousVerificationService.processNextCVTasks(accountId);
    continuousVerificationService.expireLongRunningCVTasks(accountId);
    continuousVerificationService.retryCVTasks(accountId);
  }
}

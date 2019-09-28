package io.harness.jobs.sg247.logs;

import static software.wings.beans.FeatureName.MOVE_VERIFICATION_CRONS_TO_EXECUTORS;

import com.google.inject.Inject;

import io.harness.managerclient.VerificationManagerClient;
import io.harness.managerclient.VerificationManagerClientHelper;
import io.harness.mongo.MongoPersistenceIterator.Handler;
import io.harness.service.intfc.ContinuousVerificationService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import software.wings.beans.Account;

@Slf4j
public class ServiceGuardLogAnalysisJob implements Job, Handler<Account> {
  public static final String SERVICE_GUARD_LOG_ANALYSIS_CRON = "SERVICE_GUARD_LOG_ANALYSIS_CRON";
  @Inject private ContinuousVerificationService continuousVerificationService;
  @Inject private VerificationManagerClientHelper verificationManagerClientHelper;
  @Inject private VerificationManagerClient verificationManagerClient;

  @Override
  public void execute(JobExecutionContext jobExecutionContext) {
    final String accountId = jobExecutionContext.getMergedJobDataMap().getString("accountId");
    logger.info("triggering logs analysis for account {}", accountId);
    if (!verificationManagerClientHelper
             .callManagerWithRetry(
                 verificationManagerClient.isFeatureEnabled(MOVE_VERIFICATION_CRONS_TO_EXECUTORS, accountId))
             .getResource()) {
      continuousVerificationService.triggerLogsL1Clustering(accountId);
      continuousVerificationService.triggerLogsL2Clustering(accountId);
      continuousVerificationService.triggerLogDataAnalysis(accountId);
      continuousVerificationService.triggerFeedbackAnalysis(accountId);
      logger.info("Completed Log Data Processor Job");
    } else {
      logger.info("for {} cron is disabled and data will be analyzed through", accountId);
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

    logger.info("triggering all analysis for account {}", accountId);
    continuousVerificationService.triggerServiceGuardTimeSeriesAnalysis(accountId);
    continuousVerificationService.triggerLogsL1Clustering(accountId);
    continuousVerificationService.triggerLogsL2Clustering(accountId);
    continuousVerificationService.triggerLogDataAnalysis(accountId);
    continuousVerificationService.triggerFeedbackAnalysis(accountId);
  }
}

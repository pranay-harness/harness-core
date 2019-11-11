package io.harness.jobs.sg247.logs;

import com.google.inject.Inject;

import io.harness.mongo.iterator.MongoPersistenceIterator.Handler;
import io.harness.service.intfc.ContinuousVerificationService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import software.wings.beans.Account;

@Slf4j
public class ServiceGuardLogAnalysisJob implements Job, Handler<Account> {
  public static final String SERVICE_GUARD_LOG_ANALYSIS_CRON = "SERVICE_GUARD_LOG_ANALYSIS_CRON";
  @Inject private ContinuousVerificationService continuousVerificationService;

  @Override
  public void execute(JobExecutionContext jobExecutionContext) {}

  @Override
  public void handle(Account account) {
    final String accountId = account.getUuid();
    logger.info("triggering all analysis for account {}", accountId);
    continuousVerificationService.triggerServiceGuardTimeSeriesAnalysis(accountId);
    continuousVerificationService.triggerLogsL1Clustering(accountId);
    continuousVerificationService.triggerLogsL2Clustering(accountId);
    continuousVerificationService.triggerLogDataAnalysis(accountId);
    continuousVerificationService.triggerFeedbackAnalysis(accountId);
  }
}

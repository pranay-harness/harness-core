package io.harness.jobs.sg247.logs;

import com.google.inject.Inject;

import io.harness.service.intfc.ContinuousVerificationService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;

@Slf4j
public class ServiceGuardLogAnalysisJob implements Job {
  public static final String SERVICE_GUARD_LOG_ANALYSIS_CRON = "SERVICE_GUARD_LOG_ANALYSIS_CRON";
  @Inject private ContinuousVerificationService continuousVerificationService;

  @Override
  public void execute(JobExecutionContext jobExecutionContext) {
    final String accountId = jobExecutionContext.getMergedJobDataMap().getString("accountId");
    logger.info("triggering logs analysis for account {}", accountId);
    continuousVerificationService.triggerLogsL1Clustering(accountId);
    continuousVerificationService.triggerLogsL2Clustering(accountId);
    continuousVerificationService.triggerLogDataAnalysis(accountId);
    continuousVerificationService.triggerFeedbackAnalysis(accountId);
    logger.info("Completed Log Data Processor Job");
  }
}

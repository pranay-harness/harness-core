package io.harness.jobs;

import com.google.inject.Inject;

import io.harness.service.intfc.ContinuousVerificationService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;

/**
 * Handles scheduling jobs related to APM
 * Created by Pranjal on 10/04/2018
 */
@Slf4j
public class LogDataProcessorJob implements Job {
  public static final String LOG_DATA_PROCESSOR_CRON_GROUP = "LOG_DATA_PROCESSOR_CRON_GROUP";
  @Inject private ContinuousVerificationService continuousVerificationService;

  @Override
  public void execute(JobExecutionContext jobExecutionContext) {
    final String accountId = jobExecutionContext.getMergedJobDataMap().getString("accountId");
    logger.info("triggering logs analysis for account {}", accountId);
    continuousVerificationService.triggerLogsL1Clustering(accountId);
    continuousVerificationService.triggerLogsL2Clustering(accountId);
    continuousVerificationService.triggerLogDataAnalysis(accountId);
    logger.info("Completed Log Data Processor Job");
  }
}

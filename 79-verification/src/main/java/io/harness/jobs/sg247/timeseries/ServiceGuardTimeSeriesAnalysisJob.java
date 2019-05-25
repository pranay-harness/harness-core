package io.harness.jobs.sg247.timeseries;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import io.harness.service.intfc.ContinuousVerificationService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;

@Slf4j
public class ServiceGuardTimeSeriesAnalysisJob implements Job {
  public static final String SERVICE_GUARD_TIME_SERIES_ANALYSIS_CRON = "SERVICE_GUARD_TIME_SERIES_ANALYSIS_CRON";

  @Inject private ContinuousVerificationService continuousVerificationService;

  @Override
  public void execute(JobExecutionContext jobExecutionContext) {
    final String accountId = jobExecutionContext.getMergedJobDataMap().getString("accountId");
    Preconditions.checkState(isNotEmpty(accountId), "account Id not found for " + jobExecutionContext);

    logger.info("Executing APM data analysis Job for {}", accountId);
    continuousVerificationService.triggerServiceGuardTimeSeriesAnalysis(accountId);
  }
}

package io.harness.jobs.sg247.timeseries;

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
  public void execute(JobExecutionContext jobExecutionContext) {}
}

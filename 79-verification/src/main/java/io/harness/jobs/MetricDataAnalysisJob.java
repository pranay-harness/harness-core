package io.harness.jobs;

import com.google.inject.Inject;

import io.harness.service.intfc.ContinuousVerificationService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;

/**
 *  Handles scheduling jobs related to APM
 * Created by Pranjal on 10/04/2018
 */
@Slf4j
@Deprecated
public class MetricDataAnalysisJob implements Job {
  static final String METRIC_DATA_ANALYSIS_CRON_GROUP = "METRIC_DATA_ANALYSIS_CRON_GROUP";

  @Inject private ContinuousVerificationService continuousVerificationService;

  @Override
  public void execute(JobExecutionContext jobExecutionContext) {
    logger.warn("Deprecating MetricDataAnalysisJob ...");
  }
}

package io.harness.jobs;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import io.harness.service.intfc.ContinuousVerificationService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *  Handles scheduling jobs related to APM
 * Created by Pranjal on 10/04/2018
 */
public class MetricDataAnalysisJob implements Job {
  private static final Logger logger = LoggerFactory.getLogger(MetricDataAnalysisJob.class);
  public static final String METRIC_DATA_ANALYSIS_CRON_GROUP = "METRIC_DATA_ANALYSIS_CRON_GROUP";

  @Inject private ContinuousVerificationService continuousVerificationService;

  @Override
  public void execute(JobExecutionContext jobExecutionContext) {
    final String accountId = jobExecutionContext.getMergedJobDataMap().getString("accountId");
    Preconditions.checkState(isNotEmpty(accountId), "account Id not found for " + jobExecutionContext);

    logger.info("Executing APM data analysis Job for {}", accountId);
    continuousVerificationService.triggerMetricDataAnalysis(accountId);
  }
}

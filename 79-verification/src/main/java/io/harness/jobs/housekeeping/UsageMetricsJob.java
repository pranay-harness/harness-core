package io.harness.jobs.housekeeping;

import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.PersistJobDataAfterExecution;

/**
 * Job that runs every 1 minute and records verification related metrics
 *
 * Created by Pranjal on 02/26/2019
 */
@PersistJobDataAfterExecution
@DisallowConcurrentExecution
@Slf4j
public class UsageMetricsJob implements Job {
  // Cron name to uniquely identify the cron
  public static final String VERIFICATION_METRIC_CRON_NAME = "USAGE_METRIC_CRON";
  // Cron Group name
  public static final String VERIFICATION_METRIC_CRON_GROUP = "USAGE_METRIC_CRON";

  @Override
  public void execute(JobExecutionContext JobExecutionContext) {}
}

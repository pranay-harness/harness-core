package io.harness.jobs;

import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;

/**
 * Handles scheduling jobs related to APM
 * Created by Pranjal on 10/04/2018
 */
@Slf4j
@Deprecated
public class LogDataProcessorJob implements Job {
  public static final String LOG_DATA_PROCESSOR_CRON_GROUP = "LOG_DATA_PROCESSOR_CRON_GROUP";

  @Override
  public void execute(JobExecutionContext jobExecutionContext) {
    logger.warn("Deprecating LogDataProcessorJob ...");
  }
}

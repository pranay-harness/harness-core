package io.harness.jobs;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.scheduler.QuartzScheduler;

/**
 *  Handles scheduling jobs related to APM
 * Created by Pranjal on 10/04/2018
 */
public class MetricDataProcessorJob implements Job {
  private static final Logger logger = LoggerFactory.getLogger(MetricDataProcessorJob.class);

  public static final String METRIC_DATA_PROCESSOR_CRON_GROUP = "METRIC_DATA_PROCESSOR_CRON_GROUP";
  @Inject @Named("JobScheduler") private QuartzScheduler jobScheduler;
  @Override
  public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
    logger.info("Executing APM Data Processor Job");
    logger.info("Completed APM Data Processor Job");
  }
}

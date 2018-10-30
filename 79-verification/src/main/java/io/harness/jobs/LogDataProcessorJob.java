package io.harness.jobs;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.scheduler.PersistentScheduler;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles scheduling jobs related to APM
 * Created by Pranjal on 10/04/2018
 */
public class LogDataProcessorJob implements Job {
  private static final Logger logger = LoggerFactory.getLogger(LogDataProcessorJob.class);

  public static final String LOG_DATA_PROCESSOR_CRON_GROUP = "LOG_DATA_PROCESSOR_CRON_GROUP";
  @Inject @Named("JobScheduler") private PersistentScheduler jobScheduler;
  @Override
  public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
    logger.info("Executing Log Data Processor Job");
    logger.info("Completed Log Data Processor Job");
  }
}

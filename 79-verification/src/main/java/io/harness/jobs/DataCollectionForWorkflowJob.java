package io.harness.jobs;

import com.google.inject.Inject;

import io.harness.service.intfc.ContinuousVerificationService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.SchedulerException;

/**
 * Created by Pranjal on 02/06/2019
 */
@Slf4j
@Deprecated
public class DataCollectionForWorkflowJob implements Job {
  @Inject private ContinuousVerificationService continuousVerificationService;

  @Override
  public void execute(JobExecutionContext jobExecutionContext) {
    logger.warn("Deprecating DataCollectionForWorkflowJob ...");
  }

  private void deleteJob(JobExecutionContext jobExecutionContext) {
    try {
      jobExecutionContext.getScheduler().deleteJob(jobExecutionContext.getJobDetail().getKey());
      logger.info("Deleting Data Collection job for context {}", jobExecutionContext);
    } catch (SchedulerException e) {
      logger.error("Unable to clean up cron", e);
    }
  }
}

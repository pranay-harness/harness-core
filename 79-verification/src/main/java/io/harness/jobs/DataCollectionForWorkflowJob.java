package io.harness.jobs;

import com.google.inject.Inject;

import io.harness.serializer.JsonUtils;
import io.harness.service.intfc.ContinuousVerificationService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.SchedulerException;
import software.wings.service.impl.analysis.AnalysisContext;

/**
 * Created by Pranjal on 02/06/2019
 */
@Slf4j
public class DataCollectionForWorkflowJob implements Job {
  @Inject private ContinuousVerificationService continuousVerificationService;

  @Override
  public void execute(JobExecutionContext jobExecutionContext) {
    logger.info("Triggering Data collection job");
    try {
      String params = jobExecutionContext.getMergedJobDataMap().getString("jobParams");
      AnalysisContext context = JsonUtils.asObject(params, AnalysisContext.class);
      boolean jobTriggered = continuousVerificationService.triggerLogDataCollection(context);
      if (!jobTriggered) {
        deleteJob(jobExecutionContext);
      }
      logger.info("Triggering scheduled job with params {}", params);
    } catch (Exception ex) {
      logger.error("Data Collection cron failed with error", ex);
    }
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

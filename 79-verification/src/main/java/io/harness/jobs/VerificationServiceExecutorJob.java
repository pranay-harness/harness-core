package io.harness.jobs;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.scheduler.PersistentScheduler;
import io.harness.service.intfc.LearningEngineService;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.PersistJobDataAfterExecution;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.ServiceSecretKey.ServiceApiVersion;
import software.wings.delegatetasks.SplunkDataCollectionTask;
import software.wings.service.impl.analysis.AnalysisContext;
import software.wings.utils.JsonUtils;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Delete all learning engine tasks in queue older than 7 days.
 */
@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public class VerificationServiceExecutorJob implements Job {
  private static final String VERIFICATION_CRON_NAME = "VERIFICATION_SERVICE_EXECUTOR_CRON_NAME";
  private static final String VERIFICATION_CRON_GROUP = "VERIFICATION_SERVICE_EXECUTOR_CRON_GROUP";
  @Inject @Named("BackgroundJobScheduler") private PersistentScheduler jobScheduler;
  private static final Logger logger = LoggerFactory.getLogger(VerificationServiceExecutorJob.class);

  @Inject private LearningEngineService learningEngineService;

  @Override
  public void execute(JobExecutionContext context) {
    AnalysisContext verificationAnalysisTask =
        learningEngineService.getNextVerificationAnalysisTask(ServiceApiVersion.V1);
    if (verificationAnalysisTask == null) {
      return;
    }

    logger.info("pulled analysis task {}", context);
    switch (verificationAnalysisTask.getAnalysisType()) {
      case TIME_SERIES:
        scheduleTimeSeriesAnalysisCronJob(verificationAnalysisTask);
        break;
      case LOG_ML:
        scheduleLogAnalysisCronJob(verificationAnalysisTask);
        scheduleClusterCronJob(verificationAnalysisTask);
        break;
      default:
        throw new IllegalStateException("invalid analysis type " + verificationAnalysisTask.getAnalysisType());
    }

    learningEngineService.markJobScheduled(verificationAnalysisTask);
  }

  private void scheduleTimeSeriesAnalysisCronJob(AnalysisContext context) {
    Date startDate =
        new Date(new Date().getTime() + TimeUnit.MINUTES.toMillis(SplunkDataCollectionTask.DELAY_MINUTES + 1));
    JobDetail job = JobBuilder.newJob(MetricAnalysisJob.class)
                        .withIdentity(context.getStateExecutionId(),
                            context.getStateType().name().toUpperCase() + "METRIC_VERIFY_CRON_GROUP")
                        .usingJobData("jobParams", JsonUtils.asJson(context))
                        .usingJobData("timestamp", System.currentTimeMillis())
                        .usingJobData("delegateTaskId", context.getDelegateTaskId())
                        .withDescription(context.getStateType() + "-" + context.getStateExecutionId())
                        .build();

    Trigger trigger = TriggerBuilder.newTrigger()
                          .withIdentity(context.getStateExecutionId(),
                              context.getStateType().name().toUpperCase() + "METRIC_VERIFY_CRON_GROUP")
                          .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                                            .withIntervalInSeconds(60)
                                            .withMisfireHandlingInstructionNowWithExistingCount()
                                            .repeatForever())
                          .startAt(startDate)
                          .build();

    jobScheduler.scheduleJob(job, trigger);
    logger.info("Scheduled TimeSeries Analysis Cron Job with details : {}", job);
  }

  private void scheduleLogAnalysisCronJob(AnalysisContext context) {
    Date startDate =
        new Date(new Date().getTime() + TimeUnit.MINUTES.toMillis(SplunkDataCollectionTask.DELAY_MINUTES + 1));
    JobDetail job = JobBuilder.newJob(LogAnalysisManagerJob.class)
                        .withIdentity(context.getStateExecutionId(), "LOG_VERIFY_CRON_GROUP")
                        .usingJobData("jobParams", JsonUtils.asJson(context))
                        .usingJobData("timestamp", System.currentTimeMillis())
                        .usingJobData("delegateTaskId", context.getDelegateTaskId())
                        .withDescription(context.getStateType() + "-" + context.getStateExecutionId())
                        .build();

    Trigger trigger = TriggerBuilder.newTrigger()
                          .withIdentity(context.getStateExecutionId(), "LOG_VERIFY_CRON_GROUP")
                          .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                                            .withIntervalInSeconds(10)
                                            .repeatForever()
                                            .withMisfireHandlingInstructionNowWithExistingCount())
                          .startAt(startDate)
                          .build();

    jobScheduler.scheduleJob(job, trigger);
    logger.info("Scheduled Log Analysis Cron Job with details : {}", job);
  }

  private void scheduleClusterCronJob(AnalysisContext context) {
    Date startDate = new Date(new Date().getTime() + 3 * 60000);

    JobDetail job = JobBuilder.newJob(LogClusterManagerJob.class)
                        .withIdentity(context.getStateExecutionId(), "LOG_CLUSTER_CRON_GROUP")
                        .usingJobData("jobParams", JsonUtils.asJson(context))
                        .usingJobData("timestamp", System.currentTimeMillis())
                        .usingJobData("delegateTaskId", context.getDelegateTaskId())
                        .withDescription(context.getStateType() + "-" + context.getStateExecutionId())
                        .build();

    Trigger trigger = TriggerBuilder.newTrigger()
                          .withIdentity(context.getStateExecutionId(), "LOG_CLUSTER_CRON_GROUP")
                          .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                                            .withIntervalInSeconds(10)
                                            .repeatForever()
                                            .withMisfireHandlingInstructionNowWithExistingCount())
                          .startAt(startDate)
                          .build();

    jobScheduler.scheduleJob(job, trigger);
    logger.info("Scheduled Log Analysis cluster Job with details : {}", job);
  }

  public static void addJob(PersistentScheduler jobScheduler) {
    jobScheduler.deleteJob(VERIFICATION_CRON_NAME, VERIFICATION_CRON_GROUP);
    JobDetail job = JobBuilder.newJob(VerificationServiceExecutorJob.class)
                        .withIdentity(VERIFICATION_CRON_NAME, VERIFICATION_CRON_GROUP)
                        .withDescription("Verification executor job ")
                        .build();

    Trigger trigger = TriggerBuilder.newTrigger()
                          .withIdentity(VERIFICATION_CRON_NAME, VERIFICATION_CRON_GROUP)
                          .withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(5).repeatForever())
                          .build();

    jobScheduler.scheduleJob(job, trigger);
    logger.info("Added job with details : {}", job);
  }
}

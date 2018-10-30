package io.harness.scheduler;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import io.harness.jobs.LogAnalysisManagerJob;
import io.harness.jobs.LogClusterManagerJob;
import io.harness.jobs.MetricAnalysisJob;
import io.harness.service.intfc.LearningEngineService;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
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
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Delete all learning engine tasks in queue older than 7 days.
 */
@Singleton
public class VerificationServiceExecutorService {
  private static final String VERIFICATION_CRON_NAME = "VERIFICATION_SERVICE_EXECUTOR_CRON_NAME";
  private static final String VERIFICATION_CRON_GROUP = "VERIFICATION_SERVICE_EXECUTOR_CRON_GROUP";
  private static final Logger logger = LoggerFactory.getLogger(VerificationServiceExecutorService.class);

  @Inject @Named("verificationServiceExecutor") protected ScheduledExecutorService taskPollService;
  @Inject @Named("JobScheduler") private PersistentScheduler jobScheduler;

  @Inject private LearningEngineService learningEngineService;

  public void scheduleTaskPoll() {
    taskPollService.scheduleAtFixedRate(() -> {
      AnalysisContext verificationAnalysisTask = null;
      do {
        try {
          verificationAnalysisTask = learningEngineService.getNextVerificationAnalysisTask(ServiceApiVersion.V1);
          if (verificationAnalysisTask != null) {
            logger.info("pulled analysis task {}", verificationAnalysisTask);
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
        } catch (Exception e) {
          logger.error("error scheduling verification crons", e);
        }
      } while (verificationAnalysisTask != null);
    }, 5, 5, TimeUnit.SECONDS);
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
  }
}

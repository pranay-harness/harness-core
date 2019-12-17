package io.harness.scheduler;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static software.wings.common.VerificationConstants.CV_CONFIGURATION_VALID_LIMIT_IN_DAYS;
import static software.wings.common.VerificationConstants.DEFAULT_DATA_COLLECTION_INTERVAL_IN_SECONDS;
import static software.wings.common.VerificationConstants.DELAY_MINUTES;
import static software.wings.common.VerificationConstants.GA_PER_MINUTE_CV_STATES;
import static software.wings.common.VerificationConstants.PER_MINUTE_CV_STATES;
import static software.wings.common.VerificationConstants.WORKFLOW_CV_COLLECTION_CRON_GROUP;
import static software.wings.service.impl.analysis.AnalysisComparisonStrategy.PREDICTIVE;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import io.harness.jobs.workflow.collection.WorkflowDataCollectionJob;
import io.harness.jobs.workflow.logs.WorkflowFeedbackAnalysisJob;
import io.harness.jobs.workflow.logs.WorkflowLogAnalysisJob;
import io.harness.jobs.workflow.logs.WorkflowLogClusterJob;
import io.harness.jobs.workflow.timeseries.WorkflowTimeSeriesAnalysisJob;
import io.harness.managerclient.VerificationManagerClient;
import io.harness.managerclient.VerificationManagerClientHelper;
import io.harness.serializer.JsonUtils;
import io.harness.service.intfc.LearningEngineService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import software.wings.beans.FeatureName;
import software.wings.beans.ServiceSecretKey.ServiceApiVersion;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.analysis.AnalysisContext;
import software.wings.service.impl.analysis.AnalysisTolerance;
import software.wings.service.intfc.verification.CVTaskService;
import software.wings.sm.StateExecutionInstance;
import software.wings.verification.CVConfiguration;
import software.wings.verification.log.LogsCVConfiguration;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Looks at the workflow analysis task queue for tasks and schedules them periodically.
 * Main entry - {@link #scheduleTaskPoll()}
 * Called from - {@link io.harness.app.VerificationServiceApplication#initializeServiceTaskPoll(Injector)}
 */
@Singleton
@Slf4j
public class WorkflowVerificationTaskPoller {
  // TODO - give better cron names
  private static final String VERIFICATION_CRON_NAME = "VERIFICATION_SERVICE_EXECUTOR_CRON_NAME";
  private static final String VERIFICATION_CRON_GROUP = "VERIFICATION_SERVICE_EXECUTOR_CRON_GROUP";

  @Inject @Named("verificationServiceExecutor") protected ScheduledExecutorService taskPollService;
  @Inject @Named("BackgroundJobScheduler") private PersistentScheduler jobScheduler;

  @Inject private LearningEngineService learningEngineService;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private VerificationManagerClient verificationManagerClient;
  @Inject private VerificationManagerClientHelper verificationManagerClientHelper;
  @Inject private CVTaskService cvTaskService;

  @SuppressWarnings("PMD")
  public void scheduleTaskPoll() {
    taskPollService.scheduleAtFixedRate(() -> {
      AnalysisContext verificationAnalysisTask = null;
      do {
        try {
          verificationAnalysisTask = learningEngineService.getNextVerificationAnalysisTask(ServiceApiVersion.V1);
          logger.info("pulled analysis task {}", verificationAnalysisTask);
          schedulePredictiveDataCollectionCronJob(verificationAnalysisTask);
          if (verificationAnalysisTask != null
              && !PREDICTIVE.equals(verificationAnalysisTask.getComparisonStrategy())) {
            // for both Log and Metric
            logger.info("Scheduling Data collection cron");
            scheduleDataCollection(verificationAnalysisTask);
            switch (verificationAnalysisTask.getAnalysisType()) {
              case TIME_SERIES:
                scheduleTimeSeriesAnalysisCronJob(verificationAnalysisTask);
                break;
              case LOG_ML:
                scheduleLogAnalysisCronJob(verificationAnalysisTask);
                scheduleClusterCronJob(verificationAnalysisTask);
                if (verificationManagerClientHelper
                        .callManagerWithRetry(verificationManagerClient.isFeatureEnabled(
                            FeatureName.CV_FEEDBACKS, verificationAnalysisTask.getAccountId()))
                        .getResource()) {
                  scheduleFeedbackAnalysisCronJob(verificationAnalysisTask);
                }
                break;
              default:
                throw new IllegalStateException("invalid analysis type " + verificationAnalysisTask.getAnalysisType());
            }
          }
        } catch (Throwable e) {
          logger.error("error scheduling verification crons", e);
        }
      } while (verificationAnalysisTask != null);
    }, 5, 5, TimeUnit.SECONDS);
  }

  private void scheduleDataCollection(AnalysisContext context) {
    if ((verificationManagerClientHelper
                .callManagerWithRetry(verificationManagerClient.isFeatureEnabled(
                    FeatureName.CV_DATA_COLLECTION_JOB, context.getAccountId()))
                .getResource()
            && PER_MINUTE_CV_STATES.contains(context.getStateType()))
        || GA_PER_MINUTE_CV_STATES.contains(context.getStateType())) {
      logger.info("PER MINUTE data collection will be triggered for accountId : {} and stateExecutionId : {}",
          context.getAccountId(), context.getStateExecutionId());
      // TODO: add logs here after triggering data collection.
      logger.info("Current stateType is present in PER_MINUTE_CV_STATES, creating job for context : {}", context);
      Date startDate = new Date(new Date().getTime() + TimeUnit.MINUTES.toMillis(DELAY_MINUTES));
      JobDetail job = JobBuilder.newJob(WorkflowDataCollectionJob.class)
                          .withIdentity(context.getStateExecutionId(),
                              context.getStateType().name().toUpperCase() + WORKFLOW_CV_COLLECTION_CRON_GROUP)
                          .usingJobData("jobParams", JsonUtils.asJson(context))
                          .usingJobData("timestamp", System.currentTimeMillis())
                          .withDescription(context.getStateType() + "-" + context.getStateExecutionId())
                          .build();

      Trigger trigger =
          TriggerBuilder.newTrigger()
              .withIdentity(context.getStateExecutionId(),
                  context.getStateType().name().toUpperCase() + WORKFLOW_CV_COLLECTION_CRON_GROUP)
              .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                                .withIntervalInSeconds(context.getCollectionInterval() == 0
                                        ? DEFAULT_DATA_COLLECTION_INTERVAL_IN_SECONDS
                                        : context.getCollectionInterval() * DEFAULT_DATA_COLLECTION_INTERVAL_IN_SECONDS)
                                .withMisfireHandlingInstructionNowWithExistingCount()
                                .repeatForever())
              .startAt(startDate)
              .build();
      jobScheduler.scheduleJob(job, trigger);
      logger.info("Scheduled Data Collection Cron Job with details : {}", job);
    }
  }

  private void schedulePredictiveDataCollectionCronJob(AnalysisContext context) {
    if (context != null && PREDICTIVE.equals(context.getComparisonStrategy())) {
      String cvConfigUuid = context.getPredictiveCvConfigId();
      if (isNotEmpty(cvConfigUuid)) {
        return;
      }
      logger.info("Creating CV Configuration for PREDICTIVE Analysis with context : {}", context);
      cvConfigUuid = generateUuid();
      CVConfiguration cvConfiguration;
      switch (context.getStateType()) {
        case SUMO:
        case DATA_DOG_LOG:
          cvConfiguration = createLogCVConfiguration(context, cvConfigUuid);
          break;
        default:
          throw new IllegalArgumentException("Invalid state: " + context.getStateType());
      }
      logger.info("Created Configuration for Type {}, cvConfigId {}, stateExecutionId {}",
          cvConfiguration.getStateType(), cvConfiguration.getUuid(), context.getStateExecutionId());
      context.setPredictiveCvConfigId(cvConfigUuid);
      wingsPersistence.updateField(AnalysisContext.class, context.getUuid(), "predictiveCvConfigId", cvConfigUuid);
      wingsPersistence.updateField(
          StateExecutionInstance.class, context.getStateExecutionId(), "lastUpdatedAt", System.currentTimeMillis());
    }
  }

  private LogsCVConfiguration createLogCVConfiguration(AnalysisContext context, String cvConfigUuid) {
    LogsCVConfiguration logsCVConfiguration = new LogsCVConfiguration();
    logsCVConfiguration.setAppId(context.getAppId());
    logsCVConfiguration.setQuery(context.getQuery());
    logsCVConfiguration.setExactBaselineEndMinute(context.getStartDataCollectionMinute());
    logsCVConfiguration.setExactBaselineStartMinute(
        context.getStartDataCollectionMinute() - context.getPredictiveHistoryMinutes() + 1);
    logsCVConfiguration.setUuid(cvConfigUuid);
    logsCVConfiguration.setName(cvConfigUuid);
    logsCVConfiguration.setAccountId(context.getAccountId());
    logsCVConfiguration.setConnectorId(context.getAnalysisServerConfigId());
    logsCVConfiguration.setEnvId(context.getEnvId());
    logsCVConfiguration.setServiceId(context.getServiceId());
    logsCVConfiguration.setStateType(context.getStateType());
    logsCVConfiguration.setAnalysisTolerance(AnalysisTolerance.LOW);
    logsCVConfiguration.setEnabled24x7(true);
    logsCVConfiguration.setComparisonStrategy(context.getComparisonStrategy());
    logsCVConfiguration.setWorkflowConfig(true);
    logsCVConfiguration.setContextId(context.getUuid());
    logsCVConfiguration.setValidUntil(
        Date.from(OffsetDateTime.now().plusDays(CV_CONFIGURATION_VALID_LIMIT_IN_DAYS).toInstant()));
    wingsPersistence.saveIgnoringDuplicateKeys(Collections.singletonList(logsCVConfiguration));
    return logsCVConfiguration;
  }

  private void scheduleTimeSeriesAnalysisCronJob(AnalysisContext context) {
    Date startDate = new Date(new Date().getTime() + TimeUnit.MINUTES.toMillis(DELAY_MINUTES + 1));
    JobDetail job = JobBuilder.newJob(WorkflowTimeSeriesAnalysisJob.class)
                        .withIdentity(context.getStateExecutionId(),
                            context.getStateType().name().toUpperCase() + "WORKFLOW_TIME_SERIES_VERIFY_CRON_GROUP")
                        .usingJobData("jobParams", JsonUtils.asJson(context))
                        .usingJobData("timestamp", System.currentTimeMillis())
                        .usingJobData("delegateTaskId", context.getDelegateTaskId())
                        .withDescription(context.getStateType() + "-" + context.getStateExecutionId())
                        .build();

    Trigger trigger = TriggerBuilder.newTrigger()
                          .withIdentity(context.getStateExecutionId(),
                              context.getStateType().name().toUpperCase() + "WORKFLOW_TIME_SERIES_VERIFY_CRON_GROUP")
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
    Date startDate = new Date(new Date().getTime() + TimeUnit.MINUTES.toMillis(DELAY_MINUTES + 1));
    JobDetail job = JobBuilder.newJob(WorkflowLogAnalysisJob.class)
                        .withIdentity(context.getStateExecutionId(), "WORKFLOW_LOG_ANALYSIS_CRON_GROUP")
                        .usingJobData("jobParams", JsonUtils.asJson(context))
                        .usingJobData("timestamp", System.currentTimeMillis())
                        .usingJobData("delegateTaskId", context.getDelegateTaskId())
                        .withDescription(context.getStateType() + "-" + context.getStateExecutionId())
                        .build();

    Trigger trigger = TriggerBuilder.newTrigger()
                          .withIdentity(context.getStateExecutionId(), "WORKFLOW_LOG_ANALYSIS_CRON_GROUP")
                          .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                                            .withIntervalInSeconds(60)
                                            .repeatForever()
                                            .withMisfireHandlingInstructionNowWithExistingCount())
                          .startAt(startDate)
                          .build();

    jobScheduler.scheduleJob(job, trigger);
    logger.info("Scheduled Log Analysis Cron Job with details : {}", job);
  }

  private void scheduleClusterCronJob(AnalysisContext context) {
    Date startDate = new Date(new Date().getTime() + 3 * 60000);

    JobDetail job = JobBuilder.newJob(WorkflowLogClusterJob.class)
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

  private void scheduleFeedbackAnalysisCronJob(AnalysisContext context) {
    Date startDate = new Date(new Date().getTime() + TimeUnit.MINUTES.toMillis(DELAY_MINUTES + 1));
    JobDetail job = JobBuilder.newJob(WorkflowFeedbackAnalysisJob.class)
                        .withIdentity(context.getStateExecutionId(), "WORKFLOW_FEEDBACK_ANALYSIS_CRON_GROUP")
                        .usingJobData("jobParams", JsonUtils.asJson(context))
                        .usingJobData("timestamp", System.currentTimeMillis())
                        .usingJobData("delegateTaskId", context.getDelegateTaskId())
                        .withDescription(context.getStateType() + "-" + context.getStateExecutionId())
                        .build();

    Trigger trigger = TriggerBuilder.newTrigger()
                          .withIdentity(context.getStateExecutionId(), "WORKFLOW_FEEDBACK_ANALYSIS_CRON_GROUP")
                          .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                                            .withIntervalInSeconds(60)
                                            .repeatForever()
                                            .withMisfireHandlingInstructionNowWithExistingCount())
                          .startAt(startDate)
                          .build();

    jobScheduler.scheduleJob(job, trigger);
    logger.info("Scheduled Feedback Analysis Cron Job with details : {}", job);
  }

  public static void addJob(PersistentScheduler jobScheduler) {
    jobScheduler.deleteJob(VERIFICATION_CRON_NAME, VERIFICATION_CRON_GROUP);
  }
}

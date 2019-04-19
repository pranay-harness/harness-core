package io.harness.scheduler;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static software.wings.common.VerificationConstants.CV_CONFIGURATION_VALID_LIMIT_IN_DAYS;
import static software.wings.common.VerificationConstants.DEFAULT_DATA_COLLECTION_INTERVAL_IN_SECONDS;
import static software.wings.common.VerificationConstants.DELAY_MINUTES;
import static software.wings.common.VerificationConstants.WORKFLOW_CV_COLLECTION_CRON_GROUP;
import static software.wings.service.impl.analysis.AnalysisComparisonStrategy.PREDICTIVE;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import io.harness.jobs.DataCollectionForWorkflowJob;
import io.harness.jobs.LogAnalysisManagerJob;
import io.harness.jobs.LogClusterManagerJob;
import io.harness.jobs.MetricAnalysisJob;
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
import software.wings.service.impl.elk.ElkDataCollectionInfo;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateType;
import software.wings.verification.CVConfiguration;
import software.wings.verification.log.ElkCVConfiguration;
import software.wings.verification.log.LogsCVConfiguration;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Delete all learning engine tasks in queue older than 7 days.
 */
@Singleton
@Slf4j
public class VerificationServiceExecutorService {
  private static final String VERIFICATION_CRON_NAME = "VERIFICATION_SERVICE_EXECUTOR_CRON_NAME";
  private static final String VERIFICATION_CRON_GROUP = "VERIFICATION_SERVICE_EXECUTOR_CRON_GROUP";

  @Inject @Named("verificationServiceExecutor") protected ScheduledExecutorService taskPollService;
  @Inject @Named("BackgroundJobScheduler") private PersistentScheduler jobScheduler;

  @Inject private LearningEngineService learningEngineService;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private VerificationManagerClient verificationManagerClient;
  @Inject private VerificationManagerClientHelper verificationManagerClientHelper;

  public void scheduleTaskPoll() {
    taskPollService.scheduleAtFixedRate(() -> {
      AnalysisContext verificationAnalysisTask = null;
      do {
        try {
          verificationAnalysisTask = learningEngineService.getNextVerificationAnalysisTask(ServiceApiVersion.V1);
          schedulePredictiveDataCollectionCronJob(verificationAnalysisTask);
          if (verificationAnalysisTask != null
              && !PREDICTIVE.equals(verificationAnalysisTask.getComparisonStrategy())) {
            // for both Log and Metric
            scheduleDataCollectionCronJob(verificationAnalysisTask);
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

  private void scheduleDataCollectionCronJob(AnalysisContext context) {
    if (verificationManagerClientHelper
            .callManagerWithRetry(
                verificationManagerClient.isFeatureEnabled(FeatureName.CV_DATA_COLLECTION_JOB, context.getAccountId()))
            .getResource()) {
      if (context.getStateType().equals(StateType.SUMO)) {
        Date startDate = new Date(new Date().getTime() + TimeUnit.MINUTES.toMillis(DELAY_MINUTES));
        JobDetail job = JobBuilder.newJob(DataCollectionForWorkflowJob.class)
                            .withIdentity(context.getStateExecutionId(),
                                context.getStateType().name().toUpperCase() + WORKFLOW_CV_COLLECTION_CRON_GROUP)
                            .usingJobData("jobParams", JsonUtils.asJson(context))
                            .usingJobData("timestamp", System.currentTimeMillis())
                            .withDescription(context.getStateType() + "-" + context.getStateExecutionId())
                            .build();

        Trigger trigger = TriggerBuilder.newTrigger()
                              .withIdentity(context.getStateExecutionId(),
                                  context.getStateType().name().toUpperCase() + WORKFLOW_CV_COLLECTION_CRON_GROUP)
                              .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                                                .withIntervalInSeconds(context.getCollectionInterval() == 0
                                                        ? DEFAULT_DATA_COLLECTION_INTERVAL_IN_SECONDS
                                                        : context.getCollectionInterval()
                                                            * DEFAULT_DATA_COLLECTION_INTERVAL_IN_SECONDS)
                                                .withMisfireHandlingInstructionNowWithExistingCount()
                                                .repeatForever())
                              .startAt(startDate)
                              .build();
        jobScheduler.scheduleJob(job, trigger);
        logger.info("Scheduled Data Collection Cron Job with details : {}", job);
      }
    }
  }

  private void schedulePredictiveDataCollectionCronJob(AnalysisContext context) {
    if (context != null && PREDICTIVE.equals(context.getComparisonStrategy())) {
      String cvConfigUuid = context.getPredictiveCvConfigId();
      if (isNotEmpty(cvConfigUuid)) {
        return;
      }
      cvConfigUuid = generateUuid();
      CVConfiguration cvConfiguration;
      switch (context.getStateType()) {
        case SUMO:
          cvConfiguration = createSUMOCVConfiguration(context, cvConfigUuid);
          break;
        case ELK:
          cvConfiguration = createELKCVConfiguration(context, cvConfigUuid);
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

  private LogsCVConfiguration createSUMOCVConfiguration(AnalysisContext context, String cvConfigUuid) {
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

  private ElkCVConfiguration createELKCVConfiguration(AnalysisContext context, String cvConfigUuid) {
    ElkDataCollectionInfo elkDataCollectionInfo = (ElkDataCollectionInfo) context.getDataCollectionInfo();
    ElkCVConfiguration elkCVConfiguration = new ElkCVConfiguration();

    elkCVConfiguration.setAppId(context.getAppId());
    elkCVConfiguration.setHostnameField(context.getHostNameField());
    elkCVConfiguration.setTimestampField(elkDataCollectionInfo.getTimestampField());
    elkCVConfiguration.setTimestampFormat(elkDataCollectionInfo.getTimestampFieldFormat());
    elkCVConfiguration.setIndex(elkDataCollectionInfo.getIndices());
    elkCVConfiguration.setQuery(context.getQuery());
    elkCVConfiguration.setQueryType(elkDataCollectionInfo.getQueryType());
    elkCVConfiguration.setMessageField(elkDataCollectionInfo.getMessageField());
    elkCVConfiguration.setExactBaselineEndMinute(context.getStartDataCollectionMinute());
    elkCVConfiguration.setExactBaselineStartMinute(
        context.getStartDataCollectionMinute() - context.getPredictiveHistoryMinutes() + 1);
    elkCVConfiguration.setUuid(cvConfigUuid);
    elkCVConfiguration.setName(cvConfigUuid);
    elkCVConfiguration.setAccountId(context.getAccountId());
    elkCVConfiguration.setConnectorId(context.getAnalysisServerConfigId());
    elkCVConfiguration.setEnvId(context.getEnvId());
    elkCVConfiguration.setServiceId(context.getServiceId());
    elkCVConfiguration.setStateType(context.getStateType());
    elkCVConfiguration.setAnalysisTolerance(AnalysisTolerance.LOW);
    elkCVConfiguration.setEnabled24x7(true);
    elkCVConfiguration.setComparisonStrategy(context.getComparisonStrategy());
    elkCVConfiguration.setWorkflowConfig(true);
    elkCVConfiguration.setContextId(context.getUuid());
    elkCVConfiguration.setValidUntil(
        Date.from(OffsetDateTime.now().plusDays(CV_CONFIGURATION_VALID_LIMIT_IN_DAYS).toInstant()));
    wingsPersistence.saveIgnoringDuplicateKeys(Collections.singletonList(elkCVConfiguration));
    return elkCVConfiguration;
  }

  private void scheduleTimeSeriesAnalysisCronJob(AnalysisContext context) {
    Date startDate = new Date(new Date().getTime() + TimeUnit.MINUTES.toMillis(DELAY_MINUTES + 1));
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
    Date startDate = new Date(new Date().getTime() + TimeUnit.MINUTES.toMillis(DELAY_MINUTES + 1));
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

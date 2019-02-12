package software.wings.scheduler;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.ExecutionContext.MANAGER;
import static software.wings.common.Constants.ACCOUNT_ID_KEY;
import static software.wings.common.Constants.APP_ID_KEY;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.exception.WingsException;
import io.harness.logging.ExceptionLogger;
import io.harness.scheduler.PersistentScheduler;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.TriggerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Application;
import software.wings.beans.FeatureName;
import software.wings.beans.Permit;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.service.impl.PermitServiceImpl;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactCollectionService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.PermitService;
import software.wings.service.intfc.TriggerService;

import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class ArtifactCollectionJob implements Job {
  private static final Logger logger = LoggerFactory.getLogger(ArtifactCollectionJob.class);

  public static final String GROUP = "ARTIFACT_STREAM_CRON_GROUP";
  private static final int POLL_INTERVAL = 60; // in secs

  private static final String ARTIFACT_STREAM_ID_KEY = "artifactStreamId";

  public static final Duration timeout = Duration.ofMinutes(10);

  @Inject private ArtifactStreamService artifactStreamService;
  @Inject private TriggerService triggerService;
  @Inject @Named("ArtifactCollectionService") private ArtifactCollectionService artifactCollectionService;
  @Inject @Named("AsyncArtifactCollectionService") private ArtifactCollectionService artifactCollectionServiceAsync;
  @Inject @Named("artifactCollectionExecutor") private ExecutorService artifactCollectionExecutor;
  @Inject @Named("BackgroundJobScheduler") private PersistentScheduler jobScheduler;
  @Inject private FeatureFlagService featureFlagService;
  @Inject private PermitService permitService;
  @Inject private AppService appService;

  public static void addWithDelay(
      PersistentScheduler jobScheduler, String accountId, String appId, String artifactStreamId) {
    // Add some randomness in the trigger start time to avoid overloading quartz by firing jobs at the same time.
    long startTime = System.currentTimeMillis() + new Random().nextInt((int) TimeUnit.SECONDS.toMillis(POLL_INTERVAL));
    addInternal(jobScheduler, accountId, appId, artifactStreamId, new Date(startTime));
  }

  public static void addDefaultJob(
      PersistentScheduler jobScheduler, String accountId, String appId, String artifactStreamId) {
    addInternal(jobScheduler, accountId, appId, artifactStreamId, null);
  }

  private static void addInternal(PersistentScheduler jobScheduler, String accountId, String appId,
      String artifactStreamId, Date triggerStartTime) {
    JobDetail job = JobBuilder.newJob(ArtifactCollectionJob.class)
                        .withIdentity(artifactStreamId, ArtifactCollectionJob.GROUP)
                        .usingJobData(ARTIFACT_STREAM_ID_KEY, artifactStreamId)
                        .usingJobData(APP_ID_KEY, appId)
                        .usingJobData(ACCOUNT_ID_KEY, accountId)
                        .build();

    TriggerBuilder triggerBuilder = TriggerBuilder.newTrigger()
                                        .withIdentity(artifactStreamId, ArtifactCollectionJob.GROUP)
                                        .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                                                          .withIntervalInSeconds(POLL_INTERVAL)
                                                          .repeatForever()
                                                          .withMisfireHandlingInstructionNowWithExistingCount());
    if (triggerStartTime != null) {
      triggerBuilder.startAt(triggerStartTime);
    }

    jobScheduler.ensureJob__UnderConstruction(job, triggerBuilder.build());
  }

  @Override
  public void execute(JobExecutionContext jobExecutionContext) {
    String artifactStreamId = jobExecutionContext.getMergedJobDataMap().getString(ARTIFACT_STREAM_ID_KEY);
    String appId = jobExecutionContext.getMergedJobDataMap().getString(APP_ID_KEY);
    String accountId = jobExecutionContext.getMergedJobDataMap().getString(ACCOUNT_ID_KEY);

    ArtifactStream artifactStream = artifactStreamService.get(appId, artifactStreamId);
    if (artifactStream == null) {
      jobScheduler.deleteJob(artifactStreamId, GROUP);
      return;
    }

    logger.info("Received the artifact collection for ArtifactStreamId {}. Running asynchronously", artifactStreamId);

    artifactCollectionExecutor.submit(() -> executeInternal(appId, artifactStream, accountId));
  }

  private void executeInternal(String appId, ArtifactStream artifactStream, String accountId) {
    String artifactStreamId = artifactStream.getUuid();
    if (artifactStream.getFailedCronAttempts() > 100) {
      logger.warn(
          "ASYNC_ARTIFACT_CRON: Artifact collection disabled for artifactstream:[id:{}, type:{}] due to too many failures [{}]",
          artifactStreamId, artifactStream.getArtifactStreamType(), artifactStream.getFailedCronAttempts());
      return;
    }

    if (featureFlagService.isEnabled(FeatureName.ASYNC_ARTIFACT_COLLECTION, appService.getAccountIdByAppId(appId))) {
      try {
        int leaseDuration = (int) (TimeUnit.MINUTES.toMillis(1)
            * PermitServiceImpl.getBackoffMultiplier(artifactStream.getFailedCronAttempts()));
        String permitId =
            permitService.acquirePermit(Permit.builder()
                                            .appId(appId)
                                            .group(GROUP)
                                            .key(artifactStreamId)
                                            .expireAt(new Date(System.currentTimeMillis() + leaseDuration))
                                            .leaseDuration(leaseDuration)
                                            .build());
        if (isNotEmpty(permitId)) {
          logger.info("Permit [{}] acquired for artifactStream [id: {}, failedCount: {}] for [{}] minutes", permitId,
              artifactStream.getUuid(), artifactStream.getFailedCronAttempts(),
              TimeUnit.MILLISECONDS.toMinutes(leaseDuration));
          artifactCollectionServiceAsync.collectNewArtifactsAsync(appId, artifactStream, permitId);
        } else {
          logger.info("Permit already exists for artifactStreamId[{}]", artifactStreamId);
        }
      } catch (WingsException exception) {
        logger.warn("Failed to collect artifacts for appId {}, artifact stream {}. Reason {}", appId, artifactStreamId,
            exception.getMessage());
        exception.addContext(Application.class, appId);
        exception.addContext(ArtifactStream.class, artifactStreamId);
        ExceptionLogger.logProcessedMessages(exception, MANAGER, logger);
      } catch (Exception e) {
        logger.warn(
            "Failed to collect artifacts for appId {}, artifactStream {}", appId, artifactStreamId, e.getMessage());
      }
    } else {
      List<Artifact> artifacts;
      artifacts = artifactCollectionService.collectNewArtifacts(appId, artifactStreamId);
      if (isNotEmpty(artifacts)) {
        logger.info("[{}] new artifacts collected", artifacts.size());
        artifacts.forEach(artifact -> logger.info(artifact.toString()));
        logger.info("Calling trigger service to check if any triggers set for the collected artifacts");
        triggerService.triggerExecutionPostArtifactCollectionAsync(appId, artifactStreamId, artifacts);
      }
    }

    // Old cron jobs doesn't have accountId. Will need to recreate with accountId as part of the job details
    if (isEmpty(accountId)) {
      logger.info(
          "Quartz job '{}' in group {} doesn't have accountId in job details. Will recreate with accountId included.",
          artifactStreamId, GROUP);
      accountId = appService.getAccountIdByAppId(appId);
      jobScheduler.deleteJob(artifactStreamId, GROUP);
      addDefaultJob(jobScheduler, accountId, appId, artifactStreamId);
    }
  }
}

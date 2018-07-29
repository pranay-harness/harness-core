package software.wings.scheduler;

import static software.wings.exception.WingsException.ExecutionContext.MANAGER;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.data.structure.EmptyPredicate;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.exception.WingsException;
import software.wings.service.intfc.ArtifactCollectionService;
import software.wings.service.intfc.ArtifactStreamService;

import java.time.Duration;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Created by anubhaw on 11/8/16.
 */
public class ArtifactCollectionJob implements Job {
  private static final Logger logger = LoggerFactory.getLogger(ArtifactCollectionJob.class);

  public static final String GROUP = "ARTIFACT_STREAM_CRON_GROUP";
  private static final int POLL_INTERVAL = 60; // in secs

  private static final String APP_ID_KEY = "appId";
  private static final String ARTIFACT_STREAM_ID_KEY = "artifactStreamId";

  public static final Duration timeout = Duration.ofMinutes(10);

  @Inject private ArtifactStreamService artifactStreamService;
  @Inject private ArtifactCollectionService artifactCollectionService;
  @Inject private PermitService permitService;
  @Inject @Named("JobScheduler") private QuartzScheduler jobScheduler;

  public static void addDefaultJob(QuartzScheduler jobScheduler, String appId, String artifactStreamId) {
    // If somehow this job was scheduled from before, we would like to reset it to start counting from now.
    jobScheduler.deleteJob(artifactStreamId, GROUP);

    JobDetail job = JobBuilder.newJob(ArtifactCollectionJob.class)
                        .withIdentity(artifactStreamId, ArtifactCollectionJob.GROUP)
                        .usingJobData(ARTIFACT_STREAM_ID_KEY, artifactStreamId)
                        .usingJobData(APP_ID_KEY, appId)
                        .build();

    Trigger trigger = TriggerBuilder.newTrigger()
                          .withIdentity(artifactStreamId, ArtifactCollectionJob.GROUP)
                          .startNow()
                          .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                                            .withIntervalInSeconds(POLL_INTERVAL)
                                            .repeatForever()
                                            .withMisfireHandlingInstructionNowWithExistingCount())
                          .build();

    jobScheduler.scheduleJob(job, trigger);
  }

  @Override
  public void execute(JobExecutionContext jobExecutionContext) {
    String artifactStreamId = jobExecutionContext.getMergedJobDataMap().getString(ARTIFACT_STREAM_ID_KEY);
    String appId = jobExecutionContext.getMergedJobDataMap().getString(APP_ID_KEY);
    ArtifactStream artifactStream = artifactStreamService.get(appId, artifactStreamId);
    if (artifactStream == null) {
      jobScheduler.deleteJob(artifactStreamId, GROUP);
      return;
    }

    if (artifactStream.getFailedCronAttempts() > 100) {
      logger.warn(
          "ASYNC_ARTIFACT_CRON: Artifact collection disabled for artifactstream:[id:{}, type:{}] due to too many failures [{}]",
          artifactStreamId, artifactStream.getArtifactStreamType(), artifactStream.getFailedCronAttempts());
      return;
    }

    try {
      int leaseDuration = (int) (TimeUnit.MINUTES.toMillis(1)
          * PermitServiceImpl.getBackoffMultiplier(artifactStream.getFailedCronAttempts()));
      String permitId = permitService.acquirePermit(Permit.builder()
                                                        .appId(appId)
                                                        .group(GROUP)
                                                        .key(artifactStreamId)
                                                        .expireAt(new Date(System.currentTimeMillis() + leaseDuration))
                                                        .leaseDuration(leaseDuration)
                                                        .build());
      if (EmptyPredicate.isNotEmpty(permitId)) {
        artifactCollectionService.collectNewArtifactsAsync(appId, artifactStream, permitId);
      } else {
        logger.info("Permit already exists for artifactStreamId[{}]", artifactStreamId);
      }
    } catch (WingsException exception) {
      logger.warn("Failed to collect artifacts for appId {}, artifact stream {}. Reason {}", appId, artifactStreamId,
          exception.getMessage());
      exception.logProcessedMessages(MANAGER, logger);
    } catch (Exception e) {
      logger.warn("Failed to collect artifacts for appId {}, artifact stream {}", appId, artifactStreamId,
          new WingsException(e));
    }
  }
}

package software.wings.scheduler;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.exception.WingsException;
import software.wings.exception.WingsException.ReportTarget;
import software.wings.service.intfc.ArtifactCollectionService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.TriggerService;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;

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
  @Inject private ExecutorService executorService;
  @Inject private TriggerService triggerService;
  @Inject private ArtifactCollectionService artifactCollectionService;

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
    executorService.submit(() -> executeJobAsync(appId, artifactStreamId));
  }

  private void executeJobAsync(String appId, String artifactStreamId) {
    List<Artifact> artifacts = null;
    ArtifactStream artifactStream = artifactStreamService.get(appId, artifactStreamId);
    if (artifactStream == null) {
      jobScheduler.deleteJob(artifactStreamId, GROUP);
      return;
    }
    try {
      artifacts = artifactCollectionService.collectNewArtifacts(appId, artifactStreamId);
    } catch (WingsException exception) {
      // TODO: temporary suppress the errors coming from here - they are too many:
      if (!exception.getResponseMessageList(ReportTarget.HARNESS_ENGINEER).isEmpty()) {
        log(appId, artifactStream, exception);
      }
      // This is the way we should print this after most of the cases are resolved
      // exception.logProcessedMessages();
    } catch (Exception e) {
      log(appId, artifactStream, (WingsException) e);
    }
    if (isNotEmpty(artifacts)) {
      logger.info("[{}] new artifacts collected", artifacts.size());
      artifacts.forEach(artifact -> logger.info(artifact.toString()));
      Artifact latestArtifact = artifacts.get(artifacts.size() - 1);
      logger.info("Calling trigger execution if any for new artifact id {}", latestArtifact.getUuid());
      triggerService.triggerExecutionPostArtifactCollectionAsync(latestArtifact);
    }
  }

  private void log(String appId, ArtifactStream artifactStream, WingsException exception) {
    logger.warn(
        "Failed to collect artifacts for appId {}, artifact stream {}", appId, artifactStream.getUuid(), exception);
  }
}

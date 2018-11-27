package software.wings.scheduler;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.ExecutionContext.MANAGER;

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
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Application;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactStream;
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
  @Inject private TriggerService triggerService;
  @Inject private ArtifactCollectionService artifactCollectionService;
  @Inject @Named("artifactCollectionExecutor") private ExecutorService artifactCollectionExecutor;
  @Inject @Named("BackgroundJobScheduler") private PersistentScheduler jobScheduler;

  public static void addDefaultJob(PersistentScheduler jobScheduler, String appId, String artifactStreamId) {
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
    artifactCollectionExecutor.submit(() -> executeJobAsync(appId, artifactStreamId));
  }

  private void executeJobAsync(String appId, String artifactStreamId) {
    List<Artifact> artifacts = null;
    try {
      artifacts = artifactCollectionService.collectNewArtifacts(appId, artifactStreamId);
    } catch (WingsException exception) {
      exception.addContext(Application.class, appId);
      exception.addContext(ArtifactStream.class, artifactStreamId);
      ExceptionLogger.logProcessedMessages(exception, MANAGER, logger);
    } catch (Exception e) {
      log(appId, artifactStreamId, new WingsException(e));
    }
    if (isNotEmpty(artifacts)) {
      logger.info("[{}] new artifacts collected", artifacts.size());
      artifacts.forEach(artifact -> logger.info(artifact.toString()));
      logger.info("Calling trigger service to check if any triggers set for the collected artifacts");
      triggerService.triggerExecutionPostArtifactCollectionAsync(appId, artifactStreamId, artifacts);
    }
  }

  private void log(String appId, String artifactStreamId, WingsException exception) {
    logger.warn("Failed to collect artifacts for appId {}, artifact stream {}. Reason {}", appId, artifactStreamId,
        exception.getMessage());
  }
}

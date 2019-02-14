package software.wings.scheduler;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.scheduler.PersistentScheduler;
import org.quartz.Job;
import org.quartz.JobExecutionContext;

// TODO: This class should be removed after 05/14/2019
public class ArtifactCollectionJob implements Job {
  public static final String GROUP = "ARTIFACT_STREAM_CRON_GROUP";
  private static final String ARTIFACT_STREAM_ID_KEY = "artifactStreamId";

  @Inject @Named("BackgroundJobScheduler") private PersistentScheduler jobScheduler;

  @Override
  public void execute(JobExecutionContext jobExecutionContext) {
    String artifactStreamId = jobExecutionContext.getMergedJobDataMap().getString(ARTIFACT_STREAM_ID_KEY);
    jobScheduler.deleteJob(artifactStreamId, GROUP);
  }
}

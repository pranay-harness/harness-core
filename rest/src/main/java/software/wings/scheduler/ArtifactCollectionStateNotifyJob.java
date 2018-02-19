package software.wings.scheduler;

import com.google.inject.Inject;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import software.wings.api.ArtifactCollectionExecutionData;
import software.wings.waitnotify.WaitNotifyEngine;

/**
 * Created by sgurubelli on 11/21/17.
 */
public class ArtifactCollectionStateNotifyJob implements Job {
  @Inject private WaitNotifyEngine waitNotifyEngine;

  @Override
  public void execute(JobExecutionContext jobExecutionContext) {
    String correlationId = jobExecutionContext.getMergedJobDataMap().getString("correlationId");
    String artifactStreamId = jobExecutionContext.getMergedJobDataMap().getString("artifactStreamId");
    waitNotifyEngine.notify(
        correlationId, ArtifactCollectionExecutionData.builder().artifactStreamId(artifactStreamId).build());
  }
}

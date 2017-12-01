package software.wings.scheduler;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import software.wings.api.ArtifactCollectionExecutionData;
import software.wings.waitnotify.WaitNotifyEngine;

import javax.inject.Inject;

/**
 * Created by sgurubelli on 11/21/17.
 */
public class ArtifactCollectionStateNotifyJob implements Job {
  @Inject private WaitNotifyEngine waitNotifyEngine;

  @Override
  public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
    System.out.println("In artifact collection state notify job = " + jobExecutionContext);
    String correlationId = jobExecutionContext.getMergedJobDataMap().getString("correlationId");
    String artifactStreamId = jobExecutionContext.getMergedJobDataMap().getString("artifactStreamId");
    waitNotifyEngine.notify(
        correlationId, ArtifactCollectionExecutionData.builder().artifactStreamId(artifactStreamId).build());
  }
}

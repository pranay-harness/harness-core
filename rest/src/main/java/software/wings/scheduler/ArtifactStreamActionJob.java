package software.wings.scheduler;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.service.intfc.ArtifactStreamService;

import javax.inject.Inject;

/**
 * Created by anubhaw on 10/24/16.
 */
public class ArtifactStreamActionJob implements Job {
  @Inject private ArtifactStreamService artifactStreamService;
  private final Logger logger = LoggerFactory.getLogger(ArtifactStreamActionJob.class);

  @Override
  public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
    String artifactStreamId = jobExecutionContext.getMergedJobDataMap().getString("artifactStreamId");
    String appId = jobExecutionContext.getMergedJobDataMap().getString("appId");
    String workflowId = jobExecutionContext.getMergedJobDataMap().getString("workflowId");
    String actionId = jobExecutionContext.getMergedJobDataMap().getString("actionId");
    artifactStreamService.triggerScheduledStreamAction(appId, artifactStreamId, actionId);
  }
}

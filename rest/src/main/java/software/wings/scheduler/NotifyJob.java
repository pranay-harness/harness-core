package software.wings.scheduler;

import static software.wings.sm.ExecutionStatusData.Builder.anExecutionStatusData;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import software.wings.sm.ExecutionStatus;
import software.wings.waitnotify.WaitNotifyEngine;

import javax.inject.Inject;

/**
 * Created by rishi on 3/31/17.
 */
public class NotifyJob implements Job {
  @Inject private WaitNotifyEngine waitNotifyEngine;

  @Override
  public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
    String correlationId = jobExecutionContext.getMergedJobDataMap().getString("correlationId");
    String executionStatus = jobExecutionContext.getMergedJobDataMap().getString("executionStatus");
    waitNotifyEngine.notify(
        correlationId, anExecutionStatusData().withExecutionStatus(ExecutionStatus.valueOf(executionStatus)).build());
  }
}

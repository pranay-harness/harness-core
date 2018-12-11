package software.wings.scheduler;

import static software.wings.sm.ExecutionStatusData.Builder.anExecutionStatusData;

import com.google.inject.Inject;

import io.harness.beans.ExecutionStatus;
import io.harness.waiter.WaitNotifyEngine;
import org.quartz.Job;
import org.quartz.JobExecutionContext;

/**
 * Created by rishi on 3/31/17.
 */
public class NotifyJob implements Job {
  @Inject private WaitNotifyEngine waitNotifyEngine;

  @Override
  public void execute(JobExecutionContext jobExecutionContext) {
    String correlationId = jobExecutionContext.getMergedJobDataMap().getString("correlationId");
    String executionStatus = jobExecutionContext.getMergedJobDataMap().getString("executionStatus");
    waitNotifyEngine.notify(
        correlationId, anExecutionStatusData().withExecutionStatus(ExecutionStatus.valueOf(executionStatus)).build());
  }
}

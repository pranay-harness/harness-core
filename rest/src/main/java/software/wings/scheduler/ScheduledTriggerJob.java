package software.wings.scheduler;

import com.google.inject.Inject;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import software.wings.service.intfc.TriggerService;

/**
 * Created by sgurubelli on 10/26/17.
 */
public class ScheduledTriggerJob implements Job {
  @Inject private TriggerService triggerService;

  @Override
  public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
    String triggerId = jobExecutionContext.getMergedJobDataMap().getString("triggerId");
    String appId = jobExecutionContext.getMergedJobDataMap().getString("appId");
    triggerService.triggerScheduledExecutionAsync(appId, triggerId);
  }
}

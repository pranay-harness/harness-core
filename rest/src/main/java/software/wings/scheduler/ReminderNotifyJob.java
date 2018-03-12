package software.wings.scheduler;

import com.google.inject.Inject;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import software.wings.common.Constants;
import software.wings.waitnotify.WaitNotifyEngine;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by sgurubelli on 11/21/17.
 */
public class ReminderNotifyJob implements Job {
  @Inject private WaitNotifyEngine waitNotifyEngine;

  @Override
  public void execute(JobExecutionContext jobExecutionContext) {
    String correlationId = jobExecutionContext.getMergedJobDataMap().getString(Constants.CORRELATION_ID);

    Map<String, String> parameters = new HashMap<>();
    jobExecutionContext.getMergedJobDataMap().entrySet().forEach(
        entry -> { parameters.put(entry.getKey(), String.valueOf(entry.getValue())); });
    waitNotifyEngine.notify(correlationId, ReminderNotifyResponse.builder().parameters(parameters).build());
  }
}

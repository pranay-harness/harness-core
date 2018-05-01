package software.wings.scheduler;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import software.wings.waitnotify.NotifyCallback;
import software.wings.waitnotify.NotifyResponseData;

import java.util.Map;

/**
 * Created by rsingh on 5/1/18.
 *
 * If you are reading this, then delete this class.
 */
public class NewRelicMetricNameCollectionJob implements Job {
  @Override
  public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {}

  private static class DelegateCallbackHandler implements NotifyCallback {
    @Override
    public void notify(Map<String, NotifyResponseData> response) {}

    @Override
    public void notifyError(Map<String, NotifyResponseData> response) {}
  }
}
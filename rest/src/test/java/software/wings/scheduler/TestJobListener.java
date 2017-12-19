package software.wings.scheduler;

import static software.wings.utils.WingsTestConstants.APP_ID;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;

public class TestJobListener implements JobListener {
  private String jobKey;

  public TestJobListener(String jk) {
    jobKey = jk;
  }

  @Override
  public String getName() {
    return TestJobListener.class.getName();
  }

  @Override
  public void jobToBeExecuted(JobExecutionContext context) {}

  @Override
  public void jobExecutionVetoed(JobExecutionContext context) {}

  @Override
  public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
    if (context.getTrigger().getJobKey().toString().equals(jobKey)) {
      synchronized (this) {
        this.notifyAll();
      }
    }
  }
}
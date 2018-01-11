package software.wings.scheduler;

import org.eclipse.jgit.util.time.MonotonicSystemClock;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;

import java.util.concurrent.TimeoutException;

public class TestJobListener implements JobListener {
  private String jobKey;

  public TestJobListener(String jk) {
    jobKey = jk;
  }

  private boolean satisfied;

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
        satisfied = true;
        this.notifyAll();
      }
    }
  }

  public void waitToSatisfy(int timeoutMillis) throws InterruptedException, TimeoutException {
    final MonotonicSystemClock monotonicSystemClock = new MonotonicSystemClock();
    final long end = monotonicSystemClock.propose().millis() + timeoutMillis;

    synchronized (this) {
      while (!satisfied) {
        int timeLeft = (int) (end - monotonicSystemClock.propose().millis());
        if (timeLeft <= 0) {
          throw new TimeoutException();
        }
        this.wait(timeLeft);
      }
    }
  }
}
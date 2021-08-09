package software.wings.scheduler;

import java.time.Duration;
import java.util.concurrent.TimeoutException;
import org.eclipse.jgit.util.time.MonotonicSystemClock;
import org.eclipse.jgit.util.time.ProposedTimestamp;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;

public class TestJobListener implements JobListener {
  private String jobKey;

  private static final MonotonicSystemClock monotonicSystemClock = new MonotonicSystemClock();

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

  long monotonicTimestamp() {
    try (ProposedTimestamp timestamp = monotonicSystemClock.propose()) {
      return timestamp.millis();
    }
  }

  public void waitToSatisfy(Duration timeout) throws InterruptedException, TimeoutException {
    final long end = monotonicTimestamp() + timeout.toMillis();

    synchronized (this) {
      while (!satisfied) {
        int timeLeft = (int) (end - monotonicTimestamp());
        if (timeLeft <= 0) {
          throw new TimeoutException();
        }
        this.wait(timeLeft);
      }
    }
  }
}

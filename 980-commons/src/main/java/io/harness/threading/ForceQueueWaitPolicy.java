package io.harness.threading;

import java.time.Duration;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ForceQueueWaitPolicy implements RejectedExecutionHandler {
  long queueSizeThreshold = 500;
  long sleepTimeInMilliSeconds = 200;

  public ForceQueueWaitPolicy() {}

  public ForceQueueWaitPolicy(long queueSizeThreshold, long sleepTimeInMilliSeconds) {
    this.queueSizeThreshold = queueSizeThreshold;
    this.sleepTimeInMilliSeconds = sleepTimeInMilliSeconds;
  }

  @Override
  public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
    try {
      log.info("rejectedExecution occurred - will force the thread pool to increase pool size. Current queue size is"
          + executor.getQueue().size());
      // This would sleep for some time till the queue is full.
      // Having the fully queue could lead to
      while (executor.getQueue().size() > queueSizeThreshold) {
        Morpheus.sleep(Duration.ofMillis(sleepTimeInMilliSeconds));
      }
      executor.getQueue().put(r);
    } catch (InterruptedException ex) {
      // should never happen since we never wait
      throw new RejectedExecutionException(ex);
    }
  }
}

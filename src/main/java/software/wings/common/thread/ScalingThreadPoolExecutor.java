package software.wings.common.thread;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ThreadPoolExecutor based on https://github.com/AndroidDeveloperLB/ListViewVariants
 * /blob/master/app/src/main /java/lb/listviewvariants/utils/async_task_thread_pool
 * /ScalingThreadPoolExecutor.java that forces the Java to raise the current pool size, if it has
 * still not reached the max threshold, in case existing ones are busy processing other jobs.
 *
 * @author Rishi
 */
public class ScalingThreadPoolExecutor extends ThreadPoolExecutor {
  /**
   * number of threads that are actively executing tasks
   */
  private final AtomicInteger activeCount = new AtomicInteger();

  public ScalingThreadPoolExecutor() {
    super(1, 1, 0, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(100));
  }

  public ScalingThreadPoolExecutor(
      int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue workQueue) {
    super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
  }

  @Override
  public int getActiveCount() {
    return activeCount.get();
  }

  @Override
  protected void beforeExecute(Thread t, Runnable r) {
    activeCount.incrementAndGet();
  }

  @Override
  protected void afterExecute(Runnable r, Throwable t) {
    activeCount.decrementAndGet();
  }
}

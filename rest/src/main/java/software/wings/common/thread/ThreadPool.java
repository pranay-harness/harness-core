package software.wings.common.thread;

import static io.harness.threading.Morpheus.quietSleep;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * This is a common threadpool for the entire application.
 *
 * @author Rishi
 */
public class ThreadPool {
  private static final int CORE_POOL_SIZE = 20;
  private static final int MAX_POOL_SIZE = 1000;
  private static final long IDLE_TIME = 500L;
  private static final ThreadPoolExecutor commonPool =
      create(CORE_POOL_SIZE, MAX_POOL_SIZE, IDLE_TIME, TimeUnit.MILLISECONDS);

  /**
   * Creates the thread pool executor.
   *
   * @param corePoolSize the core pool size
   * @param maxPoolSize  the max pool size
   * @param idleTime     the idle time
   * @param unit         the unit
   * @return the thread pool executor
   */
  public static ThreadPoolExecutor create(int corePoolSize, int maxPoolSize, long idleTime, TimeUnit unit) {
    return create(corePoolSize, maxPoolSize, idleTime, unit, Executors.defaultThreadFactory());
  }

  public static ThreadPoolExecutor create(
      int corePoolSize, int maxPoolSize, long idleTime, TimeUnit unit, ThreadFactory threadFactory) {
    ScalingQueue queue = new ScalingQueue();
    ThreadPoolExecutor executor =
        new ScalingThreadPoolExecutor(corePoolSize, maxPoolSize, idleTime, unit, queue, threadFactory);
    executor.setRejectedExecutionHandler(new ForceQueuePolicy());
    queue.setThreadPoolExecutor(executor);
    return executor;
  }

  /**
   * Execute.
   *
   * @param task  the task
   * @param delay the delay
   */
  public static void execute(Runnable task, int delay) {
    execute(new Delayed(task, delay));
  }

  /**
   * Execute.
   *
   * @param task the task
   */
  public static void execute(Runnable task) {
    commonPool.execute(task);
  }

  /**
   * Submit.
   *
   * @param <T>  the generic type
   * @param task the task
   * @return the future
   */
  public static <T> Future<T> submit(Callable<T> task) {
    return commonPool.submit(task);
  }

  /**
   * Shutdown.
   */
  public static void shutdown() {
    commonPool.shutdown();
  }

  /**
   * Shutdown now.
   */
  public static void shutdownNow() {
    commonPool.shutdownNow();
  }

  /**
   * Is idle boolean.
   *
   * @return the boolean
   */
  public static boolean isIdle() {
    return commonPool.getActiveCount() == 0;
  }

  private static class Delayed implements Runnable {
    private Runnable runnable;
    private int delay;

    /**
     * Instantiates a new Delayed.
     *
     * @param runnable the runnable
     * @param delay    the delay
     */
    Delayed(Runnable runnable, int delay) {
      this.runnable = runnable;
      this.delay = delay;
    }

    @Override
    public void run() {
      quietSleep(Duration.ofMillis(delay));
      runnable.run();
    }
  }
}

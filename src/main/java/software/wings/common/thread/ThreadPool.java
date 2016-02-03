package software.wings.common.thread;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import software.wings.utils.Misc;

/**
 *  This is a common threadpool for the entire application
 *
 *
 * @author Rishi
 *
 */
public class ThreadPool {
  private static final int CORE_POOL_SIZE = 20;
  private static final int MAX_POOL_SIZE = 1000;
  private static final long IDLE_TIME = 500L;
  private static ThreadPoolExecutor commonPool =
      create(CORE_POOL_SIZE, MAX_POOL_SIZE, IDLE_TIME, TimeUnit.MILLISECONDS);

  private static ThreadPoolExecutor create(int corePoolSize, int maxPoolSize, long idleTime, TimeUnit unit) {
    ScalingQueue queue = new ScalingQueue();
    ThreadPoolExecutor executor = new ScalingThreadPoolExecutor(corePoolSize, maxPoolSize, idleTime, unit, queue);
    executor.setRejectedExecutionHandler(new ForceQueuePolicy());
    queue.setThreadPoolExecutor(executor);
    return executor;
  }

  public static void execute(Runnable task) {
    commonPool.execute(task);
  }

  public static void execute(Runnable task, int delay) {
    execute(new Delayed(task, delay));
  }

  public static <T> Future<T> submit(Callable<T> task) {
    return commonPool.submit(task);
  }

  public static void shutdown() {
    commonPool.shutdown();
  }

  public static void shutdownNow() {
    commonPool.shutdownNow();
  }

  public static boolean isIdle() {
    return (commonPool.getActiveCount() == 0);
  }

  private static class Delayed implements Runnable {
    private Runnable runnable;
    private int delay;

    public Delayed(Runnable runnable, int delay) {
      this.runnable = runnable;
      this.delay = delay;
    }

    @Override
    public void run() {
      Misc.quietSleep(delay);
      runnable.run();
    }
  }
}
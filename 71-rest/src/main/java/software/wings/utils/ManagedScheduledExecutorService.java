package software.wings.utils;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by peeyushaggarwal on 4/18/16.
 */
public class ManagedScheduledExecutorService extends ManagedExecutorService implements ScheduledExecutorService {
  private static ScheduledThreadPoolExecutor createScheduledThreadPoolExecutor(String name) {
    return new ScheduledThreadPoolExecutor(1, new ThreadFactoryBuilder().setNameFormat(name).build());
  }

  /**
   * Instantiates a new managed scheduled executor service.
   *
   * @param name the name
   */
  public ManagedScheduledExecutorService(String name) {
    super(createScheduledThreadPoolExecutor(name));
  }

  /* (non-Javadoc)
   * @see java.util.concurrent.ScheduledExecutorService#schedule(java.lang.Runnable, long,
   * java.util.concurrent.TimeUnit)
   */
  @Override
  public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
    return ((ScheduledExecutorService) getExecutorService()).schedule(command, delay, unit);
  }

  /* (non-Javadoc)
   * @see java.util.concurrent.ScheduledExecutorService#schedule(java.util.concurrent.Callable, long,
   * java.util.concurrent.TimeUnit)
   */
  @Override
  public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
    return ((ScheduledExecutorService) getExecutorService()).schedule(callable, delay, unit);
  }

  /* (non-Javadoc)
   * @see java.util.concurrent.ScheduledExecutorService#scheduleAtFixedRate(java.lang.Runnable, long, long,
   * java.util.concurrent.TimeUnit)
   */
  @Override
  public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
    return ((ScheduledExecutorService) getExecutorService()).scheduleAtFixedRate(command, initialDelay, period, unit);
  }

  /* (non-Javadoc)
   * @see java.util.concurrent.ScheduledExecutorService#scheduleWithFixedDelay(java.lang.Runnable, long, long,
   * java.util.concurrent.TimeUnit)
   */
  @Override
  public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
    return ((ScheduledExecutorService) getExecutorService()).scheduleWithFixedDelay(command, initialDelay, delay, unit);
  }

  /* (non-Javadoc)
   * @see software.wings.utils.ManagedExecutorService#stop()
   */
  @Override
  public void stop() throws Exception {
    shutdown();
    awaitTermination(10000, TimeUnit.MILLISECONDS);
  }
}
